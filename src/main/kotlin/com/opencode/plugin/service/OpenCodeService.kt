package com.opencode.plugin.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.util.SystemInfo
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

@Service(Service.Level.PROJECT)
class OpenCodeService(private val project: Project) {

    private val log = Logger.getInstance(OpenCodeService::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    @Serializable
    data class SessionInfo(
        val id: String,
        val title: String = "",
        val directory: String = "",
        val slug: String = ""
    )

    @Serializable
    data class HealthInfo(
        val healthy: Boolean,
        val version: String = ""
    )

    data class DiscoveredServer(
        val url: String,
        val port: Int,
        val version: String,
        val projectDir: String,
        val sessionCount: Int
    )

    @Serializable
    data class TuiAppendRequest(
        val text: String
    )

    @Serializable
    data class AgentInfo(
        val name: String
    )

    fun sendToOpenCode(
        filePath: String,
        startLine: Int,
        endLine: Int,
        additionalContext: String,
        agent: String = "",
        overrideSessionId: String = ""
    ) {
        val settings = service<OpenCodeSettings>()
        
        if (settings.useTuiMode) {
            appendToTuiPrompt(filePath, startLine, endLine, additionalContext)
            return
        }

        val serverUrl = settings.serverUrl
        var sessionId = if (overrideSessionId.isNotBlank()) overrideSessionId else settings.sessionId

        if (sessionId.isBlank() && settings.autoDetectSession) {
            sessionId = discoverActiveSession() ?: run {
                showNotification("No active OpenCode session found. Please start OpenCode or configure a session ID.", NotificationType.ERROR)
                return
            }
        }

        if (sessionId.isBlank()) {
            showNotification("OpenCode session ID not configured", NotificationType.ERROR)
            return
        }

        val fileUrl = "file://$filePath?start=$startLine&end=$endLine"
        val fileName = filePath.substringAfterLast("/").substringAfterLast("\\")
        
        val textContent = if (additionalContext.isNotBlank()) {
            additionalContext
        } else {
            "Please review this code selection from $filePath (lines $startLine-$endLine)"
        }

        val agentField = if (agent.isNotBlank()) ""","agent": "$agent"""" else ""
        val requestBodyJson = """
        {
            "parts": [
                {
                    "type": "file",
                    "url": "$fileUrl",
                    "filename": "$fileName",
                    "mime": "text/plain"
                },
                {
                    "type": "text",
                    "text": ${json.encodeToString(textContent)}
                }
            ]$agentField
        }
        """.trimIndent()

        try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("$serverUrl/session/$sessionId/prompt_async"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .timeout(Duration.ofSeconds(30))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            val request = requestBuilder.build()

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    ApplicationManager.getApplication().invokeLater {
                        if (response.statusCode() in 200..299) {
                            showNotification(
                                "Code sent to OpenCode successfully",
                                NotificationType.INFORMATION
                            )
                        } else {
                            log.warn("OpenCode response: ${response.statusCode()} - ${response.body()}")
                            showNotification(
                                "Failed to send to OpenCode: ${response.statusCode()}",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
                .exceptionally { error ->
                    ApplicationManager.getApplication().invokeLater {
                        log.error("Failed to send to OpenCode", error)
                        showNotification(
                            "Connection failed: ${error.message}",
                            NotificationType.ERROR
                        )
                    }
                    null
                }
        } catch (e: Exception) {
            log.error("Failed to send to OpenCode", e)
            showNotification("Failed to send: ${e.message}", NotificationType.ERROR)
        }
    }

    fun testConnection(): Boolean {
        val settings = service<OpenCodeSettings>()
        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("${settings.serverUrl}/global/health"))
                .GET()
                .timeout(Duration.ofSeconds(5))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            log.warn("Connection test failed", e)
            false
        }
    }

    fun discoverActiveSession(): String? {
        val settings = service<OpenCodeSettings>()
        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("${settings.serverUrl}/session?roots=true&limit=1"))
                .GET()
                .timeout(Duration.ofSeconds(5))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val sessions = json.decodeFromString<List<SessionInfo>>(response.body())
                sessions.firstOrNull()?.id
            } else {
                null
            }
        } catch (e: Exception) {
            log.warn("Failed to discover active session", e)
            null
        }
    }

    fun appendToTuiPrompt(
        filePath: String,
        startLine: Int,
        endLine: Int,
        additionalContext: String
    ) {
        val settings = service<OpenCodeSettings>()
        val serverUrl = settings.serverUrl
        
        val projectBasePath = project.basePath ?: ""
        val relativePath = if (projectBasePath.isNotEmpty() && filePath.startsWith(projectBasePath)) {
            filePath.removePrefix(projectBasePath).removePrefix("/").removePrefix("\\")
        } else {
            filePath.substringAfterLast("/").substringAfterLast("\\")
        }
        
        val lineRef = if (startLine == endLine) "#L$startLine" else "#L$startLine - $endLine"
        val fileRef = "@$relativePath $lineRef"
        val text = if (additionalContext.isNotBlank()) {
            "$fileRef $additionalContext"
        } else {
            fileRef
        }

        val requestBody = json.encodeToString(TuiAppendRequest(text))

        try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("$serverUrl/tui/append-prompt"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(5))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    ApplicationManager.getApplication().invokeLater {
                        if (response.statusCode() in 200..299) {
                            if (settings.autoSubmitTui) {
                                submitTuiPrompt()
                                showNotification(
                                    "Code sent to OpenCode",
                                    NotificationType.INFORMATION
                                )
                            } else {
                                showNotification(
                                    "Code reference added to OpenCode prompt",
                                    NotificationType.INFORMATION
                                )
                            }
                        } else {
                            showNotification(
                                "Failed to append to prompt: ${response.statusCode()}",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
                .exceptionally { error ->
                    ApplicationManager.getApplication().invokeLater {
                        log.error("Failed to append to TUI prompt", error)
                        showNotification("Failed: ${error.message}", NotificationType.ERROR)
                    }
                    null
                }
        } catch (e: Exception) {
            log.error("Failed to append to TUI prompt", e)
            showNotification("Failed: ${e.message}", NotificationType.ERROR)
        }
    }

    fun submitTuiPrompt() {
        val settings = service<OpenCodeSettings>()
        val serverUrl = settings.serverUrl

        try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("$serverUrl/tui/submit-prompt"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(5))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.error("Failed to submit TUI prompt", e)
        }
    }

    fun fetchSessions(limit: Int = 10): List<SessionInfo> {
        val settings = service<OpenCodeSettings>()
        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("${settings.serverUrl}/session?limit=$limit"))
                .GET()
                .timeout(Duration.ofSeconds(5))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                json.decodeFromString<List<SessionInfo>>(response.body())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            log.warn("Failed to fetch sessions", e)
            emptyList()
        }
    }

    fun fetchAgents(): List<AgentInfo> {
        val settings = service<OpenCodeSettings>()
        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("${settings.serverUrl}/agent"))
                .GET()
                .timeout(Duration.ofSeconds(5))

            if (settings.password.isNotBlank()) {
                val credentials = Base64.getEncoder().encodeToString(
                    "${settings.username}:${settings.password}".toByteArray()
                )
                requestBuilder.header("Authorization", "Basic $credentials")
            }

            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                json.decodeFromString<List<AgentInfo>>(response.body())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            log.warn("Failed to fetch agents", e)
            emptyList()
        }
    }

    private fun showNotification(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("OpenCodeCompanion")
            .createNotification(message, type)
            .notify(project)
    }

    companion object {
        private val commonPorts = listOf(4096, 4097, 4098, 4099, 4100, 4101, 4102, 4103, 4104, 4105)
        private val log = Logger.getInstance(OpenCodeService::class.java)
        
        data class KillResult(
            val success: Boolean,
            val message: String,
            val pid: Int? = null
        )
        
        fun parsePidFromWindowsNetstat(output: String): Int? {
            // Parse Windows netstat: "  TCP    0.0.0.0:4096    0.0.0.0:0    LISTENING    12345"
            return output.lines()
                .firstOrNull { it.contains("LISTENING") }
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.lastOrNull()
                ?.toIntOrNull()
        }
        
        fun parsePidFromLsof(output: String): Int? {
            return output.trim().lines().firstOrNull()?.toIntOrNull()
        }
        
        fun findPidByPort(port: Int): Int? {
            return try {
                val command = if (SystemInfo.isWindows) {
                    GeneralCommandLine("cmd", "/c", "netstat -ano | findstr :$port | findstr LISTENING")
                } else {
                    GeneralCommandLine("lsof", "-ti:$port")
                }
                
                val output = ExecUtil.execAndGetOutput(command)
                if (output.exitCode == 0 && output.stdout.isNotBlank()) {
                    if (SystemInfo.isWindows) {
                        parsePidFromWindowsNetstat(output.stdout)
                    } else {
                        parsePidFromLsof(output.stdout)
                    }
                } else null
            } catch (e: Exception) {
                log.warn("Failed to find PID for port $port", e)
                null
            }
        }
        
        fun killServerByPort(port: Int): KillResult {
            val pid = findPidByPort(port)
                ?: return KillResult(false, "No process found listening on port $port")
            
            return try {
                val command = if (SystemInfo.isWindows) {
                    GeneralCommandLine("taskkill", "/F", "/PID", pid.toString())
                } else {
                    GeneralCommandLine("kill", "-9", pid.toString())
                }
                
                val output = ExecUtil.execAndGetOutput(command)
                if (output.exitCode == 0) {
                    KillResult(true, "Killed process $pid on port $port", pid)
                } else {
                    KillResult(false, "Failed to kill process $pid: ${output.stderr}", pid)
                }
            } catch (e: Exception) {
                log.error("Failed to kill process on port $port", e)
                KillResult(false, "Error killing process: ${e.message}", pid)
            }
        }
        
        fun discoverServers(): List<DiscoveredServer> {
            val httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
                .build()
            val json = Json { ignoreUnknownKeys = true }
            
            return commonPorts.mapNotNull { port ->
                try {
                    val healthUrl = "http://localhost:$port/global/health"
                    val healthRequest = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .GET()
                        .timeout(Duration.ofMillis(500))
                        .build()
                    
                    val healthResponse = httpClient.send(healthRequest, HttpResponse.BodyHandlers.ofString())
                    if (healthResponse.statusCode() != 200) return@mapNotNull null
                    
                    val health = json.decodeFromString<HealthInfo>(healthResponse.body())
                    if (!health.healthy) return@mapNotNull null
                    
                    val sessionUrl = "http://localhost:$port/session?limit=1"
                    val sessionRequest = HttpRequest.newBuilder()
                        .uri(URI.create(sessionUrl))
                        .GET()
                        .timeout(Duration.ofMillis(500))
                        .build()
                    
                    val sessionResponse = httpClient.send(sessionRequest, HttpResponse.BodyHandlers.ofString())
                    val sessions = if (sessionResponse.statusCode() == 200) {
                        json.decodeFromString<List<SessionInfo>>(sessionResponse.body())
                    } else {
                        emptyList()
                    }
                    
                    val projectDir = sessions.firstOrNull()?.directory ?: "Unknown"
                    
                    DiscoveredServer(
                        url = "http://localhost:$port",
                        port = port,
                        version = health.version,
                        projectDir = projectDir,
                        sessionCount = sessions.size
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}
