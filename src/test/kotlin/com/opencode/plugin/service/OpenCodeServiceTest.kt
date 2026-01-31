package com.opencode.plugin.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class OpenCodeServiceTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Nested
    inner class PidParsingTests {
        
        @Test
        fun `parsePidFromWindowsNetstat extracts PID from standard output`() {
            val output = "  TCP    0.0.0.0:4096           0.0.0.0:0              LISTENING       12345"
            
            val pid = OpenCodeService.parsePidFromWindowsNetstat(output)
            
            assertEquals(12345, pid)
        }
        
        @Test
        fun `parsePidFromWindowsNetstat handles multiple lines`() {
            val output = """
                  TCP    0.0.0.0:4096           0.0.0.0:0              LISTENING       12345
                  TCP    127.0.0.1:4096         127.0.0.1:52341        ESTABLISHED     12345
            """.trimIndent()
            
            val pid = OpenCodeService.parsePidFromWindowsNetstat(output)
            
            assertEquals(12345, pid)
        }
        
        @Test
        fun `parsePidFromWindowsNetstat returns null for empty output`() {
            val pid = OpenCodeService.parsePidFromWindowsNetstat("")
            
            assertNull(pid)
        }
        
        @Test
        fun `parsePidFromWindowsNetstat returns null when no LISTENING line`() {
            val output = "  TCP    127.0.0.1:4096         127.0.0.1:52341        ESTABLISHED     12345"
            
            val pid = OpenCodeService.parsePidFromWindowsNetstat(output)
            
            assertNull(pid)
        }
        
        @Test
        fun `parsePidFromWindowsNetstat handles IPv6 addresses`() {
            val output = "  TCP    [::]:4096              [::]:0                 LISTENING       54321"
            
            val pid = OpenCodeService.parsePidFromWindowsNetstat(output)
            
            assertEquals(54321, pid)
        }
        
        @Test
        fun `parsePidFromLsof extracts PID from simple output`() {
            val output = "12345"
            
            val pid = OpenCodeService.parsePidFromLsof(output)
            
            assertEquals(12345, pid)
        }
        
        @Test
        fun `parsePidFromLsof handles output with newline`() {
            val output = "12345\n"
            
            val pid = OpenCodeService.parsePidFromLsof(output)
            
            assertEquals(12345, pid)
        }
        
        @Test
        fun `parsePidFromLsof handles multiple PIDs returns first`() {
            val output = """
                12345
                67890
            """.trimIndent()
            
            val pid = OpenCodeService.parsePidFromLsof(output)
            
            assertEquals(12345, pid)
        }
        
        @Test
        fun `parsePidFromLsof returns null for empty output`() {
            val pid = OpenCodeService.parsePidFromLsof("")
            
            assertNull(pid)
        }
        
        @Test
        fun `parsePidFromLsof returns null for whitespace only`() {
            val pid = OpenCodeService.parsePidFromLsof("   \n   ")
            
            assertNull(pid)
        }
        
        @Test
        fun `parsePidFromLsof returns null for non-numeric output`() {
            val pid = OpenCodeService.parsePidFromLsof("not-a-pid")
            
            assertNull(pid)
        }
    }
    
    @Nested
    inner class KillResultTests {
        
        @Test
        fun `KillResult success state`() {
            val result = OpenCodeService.Companion.KillResult(
                success = true,
                message = "Killed process 12345 on port 4096",
                pid = 12345
            )
            
            assertTrue(result.success)
            assertEquals("Killed process 12345 on port 4096", result.message)
            assertEquals(12345, result.pid)
        }
        
        @Test
        fun `KillResult failure state without PID`() {
            val result = OpenCodeService.Companion.KillResult(
                success = false,
                message = "No process found listening on port 4096"
            )
            
            assertFalse(result.success)
            assertEquals("No process found listening on port 4096", result.message)
            assertNull(result.pid)
        }
        
        @Test
        fun `KillResult failure state with PID`() {
            val result = OpenCodeService.Companion.KillResult(
                success = false,
                message = "Failed to kill process 12345: Access denied",
                pid = 12345
            )
            
            assertFalse(result.success)
            assertEquals(12345, result.pid)
        }
    }
    
    @Nested
    inner class SessionInfoSerializationTests {
        
        @Test
        fun `SessionInfo deserializes with all fields`() {
            val jsonString = """{"id":"ses_123","title":"My Session","directory":"/path/to/project","slug":"my-session"}"""
            
            val session = json.decodeFromString<OpenCodeService.SessionInfo>(jsonString)
            
            assertEquals("ses_123", session.id)
            assertEquals("My Session", session.title)
            assertEquals("/path/to/project", session.directory)
            assertEquals("my-session", session.slug)
        }
        
        @Test
        fun `SessionInfo deserializes with minimal fields`() {
            val jsonString = """{"id":"ses_456"}"""
            
            val session = json.decodeFromString<OpenCodeService.SessionInfo>(jsonString)
            
            assertEquals("ses_456", session.id)
            assertEquals("", session.title)
            assertEquals("", session.directory)
            assertEquals("", session.slug)
        }
        
        @Test
        fun `SessionInfo ignores unknown fields`() {
            val jsonString = """{"id":"ses_789","title":"Test","unknownField":"ignored","anotherUnknown":123}"""
            
            val session = json.decodeFromString<OpenCodeService.SessionInfo>(jsonString)
            
            assertEquals("ses_789", session.id)
            assertEquals("Test", session.title)
        }
    }
    
    @Nested
    inner class HealthInfoSerializationTests {
        
        @Test
        fun `HealthInfo deserializes healthy response`() {
            val jsonString = """{"healthy":true,"version":"1.2.3"}"""
            
            val health = json.decodeFromString<OpenCodeService.HealthInfo>(jsonString)
            
            assertTrue(health.healthy)
            assertEquals("1.2.3", health.version)
        }
        
        @Test
        fun `HealthInfo deserializes unhealthy response`() {
            val jsonString = """{"healthy":false}"""
            
            val health = json.decodeFromString<OpenCodeService.HealthInfo>(jsonString)
            
            assertFalse(health.healthy)
            assertEquals("", health.version)
        }
    }
    
    @Nested
    inner class AgentInfoSerializationTests {
        
        @Test
        fun `AgentInfo deserializes correctly`() {
            val jsonString = """{"name":"oracle"}"""
            
            val agent = json.decodeFromString<OpenCodeService.AgentInfo>(jsonString)
            
            assertEquals("oracle", agent.name)
        }
        
        @Test
        fun `AgentInfo list deserializes correctly`() {
            val jsonString = """[{"name":"oracle"},{"name":"explore"},{"name":"librarian"}]"""
            
            val agents = json.decodeFromString<List<OpenCodeService.AgentInfo>>(jsonString)
            
            assertEquals(3, agents.size)
            assertEquals("oracle", agents[0].name)
            assertEquals("explore", agents[1].name)
            assertEquals("librarian", agents[2].name)
        }
    }
    
    @Nested
    inner class TuiAppendRequestSerializationTests {
        
        @Test
        fun `TuiAppendRequest serializes correctly`() {
            val request = OpenCodeService.TuiAppendRequest(text = "@file.kt #L10 explain this")
            
            val jsonString = json.encodeToString(request)
            
            assertEquals("""{"text":"@file.kt #L10 explain this"}""", jsonString)
        }
        
        @Test
        fun `TuiAppendRequest handles special characters`() {
            val request = OpenCodeService.TuiAppendRequest(text = "text with \"quotes\" and\nnewlines")
            
            val jsonString = json.encodeToString(request)
            val decoded = json.decodeFromString<OpenCodeService.TuiAppendRequest>(jsonString)
            
            assertEquals(request.text, decoded.text)
        }
    }
    
    @Nested
    inner class DiscoveredServerTests {
        
        @Test
        fun `DiscoveredServer data class holds correct values`() {
            val server = OpenCodeService.DiscoveredServer(
                url = "http://localhost:4096",
                port = 4096,
                version = "1.0.0",
                projectDir = "/home/user/project",
                sessionCount = 3
            )
            
            assertEquals("http://localhost:4096", server.url)
            assertEquals(4096, server.port)
            assertEquals("1.0.0", server.version)
            assertEquals("/home/user/project", server.projectDir)
            assertEquals(3, server.sessionCount)
        }
    }
}
