package com.opencode.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.opencode.plugin.service.OpenCodeService
import com.opencode.plugin.service.OpenCodeSettings
import java.awt.Color
import java.net.URI
import java.awt.Dimension
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class OpenCodeConfigurable : Configurable {

    private val settings = service<OpenCodeSettings>()
    private val agents = mutableListOf<Pair<String, String>>()
    
    private lateinit var serverUrlField: JBTextField
    private lateinit var sessionIdField: JBTextField
    private lateinit var usernameField: JBTextField
    private lateinit var passwordField: JBPasswordField
    private lateinit var autoDetectCheckbox: JBCheckBox
    private lateinit var tuiModeCheckbox: JBCheckBox
    private lateinit var autoSubmitTuiCheckbox: JBCheckBox
    private lateinit var defaultAgentCombo: ComboBox<String>
    private lateinit var statusLabel: JBLabel
    private lateinit var testButton: JButton
    private lateinit var discoverButton: JButton
    private lateinit var killServerButton: JButton
    private lateinit var killStatusLabel: JBLabel
    private lateinit var serverListModel: DefaultListModel<String>
    private lateinit var serverList: JBList<String>
    private lateinit var discoverStatusLabel: JBLabel
    
    private val discoveredServers = mutableListOf<OpenCodeService.DiscoveredServer>()

    override fun getDisplayName(): String = "OpenCode"

    private fun loadAgents() {
        agents.clear()
        agents.add("" to "(None)")
        
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val fetchedAgents = project.service<OpenCodeService>().fetchAgents()
                fetchedAgents.forEach { agent ->
                    agents.add(agent.name to agent.name)
                }
            }
        } catch (_: Exception) {
        }
        
        defaultAgentCombo.model = DefaultComboBoxModel(agents.map { it.second }.toTypedArray())
        
        val currentAgent = settings.defaultAgent
        val index = agents.indexOfFirst { it.first == currentAgent }
        if (index >= 0) defaultAgentCombo.selectedIndex = index
    }

    override fun createComponent(): JComponent {
        serverUrlField = JBTextField(settings.serverUrl)
        sessionIdField = JBTextField(settings.sessionId)
        usernameField = JBTextField(settings.username)
        passwordField = JBPasswordField().apply { 
            text = settings.password 
        }
        autoDetectCheckbox = JBCheckBox("Auto-detect active session", settings.autoDetectSession)
        tuiModeCheckbox = JBCheckBox("Use TUI mode (append to prompt instead of sending)", settings.useTuiMode)
        autoSubmitTuiCheckbox = JBCheckBox("Auto-submit after appending (TUI mode)", settings.autoSubmitTui)
        defaultAgentCombo = ComboBox<String>()
        loadAgents()
        statusLabel = JBLabel("")
        testButton = JButton("Test Connection")
        discoverButton = JButton("Discover Servers")
        discoverStatusLabel = JBLabel("")
        killServerButton = JButton("Kill Server")
        killStatusLabel = JBLabel("")
        serverListModel = DefaultListModel()
        serverList = JBList(serverListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            visibleRowCount = 4
        }

        serverList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting && serverList.selectedIndex >= 0 && serverList.selectedIndex < discoveredServers.size) {
                val selected = discoveredServers[serverList.selectedIndex]
                serverUrlField.text = selected.url
            }
        }

        discoverButton.addActionListener {
            discoverStatusLabel.text = "Scanning..."
            discoverStatusLabel.foreground = Color.GRAY
            serverListModel.clear()
            discoveredServers.clear()
            
            Thread {
                val servers = OpenCodeService.discoverServers()
                
                javax.swing.SwingUtilities.invokeLater {
                    discoveredServers.addAll(servers)
                    servers.forEach { server ->
                        val projectName = server.projectDir.substringAfterLast("/")
                        serverListModel.addElement("${server.url} - $projectName (v${server.version})")
                    }
                    
                    discoverStatusLabel.text = if (servers.isEmpty()) {
                        "No servers found"
                    } else {
                        "Found ${servers.size} server(s)"
                    }
                    discoverStatusLabel.foreground = if (servers.isEmpty()) Color.ORANGE else Color(0, 128, 0)
                }
            }.start()
        }

        killServerButton.addActionListener {
            killStatusLabel.text = "Killing..."
            killStatusLabel.foreground = Color.GRAY
            
            Thread {
                val url = serverUrlField.text
                val port = try {
                    URI.create(url).port.takeIf { it > 0 } ?: 4096
                } catch (_: Exception) {
                    4096
                }
                
                val result = OpenCodeService.killServerByPort(port)
                
                javax.swing.SwingUtilities.invokeLater {
                    if (result.success) {
                        killStatusLabel.text = result.message
                        killStatusLabel.foreground = Color(0, 128, 0)
                    } else {
                        killStatusLabel.text = result.message
                        killStatusLabel.foreground = Color.RED
                    }
                }
            }.start()
        }

        testButton.addActionListener {
            statusLabel.text = "Testing..."
            statusLabel.foreground = Color.GRAY
            
            Thread {
                val project = ProjectManager.getInstance().openProjects.firstOrNull()
                val connected = if (project != null) {
                    project.service<OpenCodeService>().testConnection()
                } else {
                    false
                }
                
                javax.swing.SwingUtilities.invokeLater {
                    if (connected) {
                        statusLabel.text = "Connected"
                        statusLabel.foreground = Color(0, 128, 0)
                    } else {
                        statusLabel.text = "Connection failed"
                        statusLabel.foreground = Color.RED
                    }
                }
            }.start()
        }

        return panel {
            group("Server Discovery") {
                row {
                    cell(discoverButton)
                    cell(discoverStatusLabel)
                }
                row {
                    val scrollPane = JBScrollPane(serverList).apply {
                        preferredSize = Dimension(400, 100)
                    }
                    cell(scrollPane)
                        .align(AlignX.FILL)
                        .comment("Click a server to select it")
                }
            }
            group("Server Configuration") {
                row("Server URL:") {
                    cell(serverUrlField)
                        .align(AlignX.FILL)
                        .comment("Default: http://localhost:4096")
                }
                row("Session ID:") {
                    cell(sessionIdField)
                        .align(AlignX.FILL)
                        .comment("Leave empty to auto-detect active session")
                }
                row {
                    cell(autoDetectCheckbox)
                }
            }
            group("Authentication (Optional)") {
                row("Username:") {
                    cell(usernameField)
                        .align(AlignX.FILL)
                        .comment("Default: opencode")
                }
                row("Password:") {
                    cell(passwordField)
                        .align(AlignX.FILL)
                        .comment("Set if OPENCODE_SERVER_PASSWORD is configured")
                }
            }
            group("Mode") {
                row {
                    cell(tuiModeCheckbox)
                        .comment("In TUI mode, code references are appended to the TUI prompt for review")
                }
                row {
                    cell(autoSubmitTuiCheckbox)
                        .comment("Automatically submit the prompt after appending (requires TUI mode)")
                }
                row("Default Agent:") {
                    cell(defaultAgentCombo)
                    button("Refresh") { loadAgents() }
                }
            }
            group("Server Management") {
                row {
                    cell(testButton)
                    cell(statusLabel)
                }
                row {
                    cell(killServerButton)
                    cell(killStatusLabel)
                }.comment("Kill stuck OpenCode server process on the configured port")
            }
        }
    }

    override fun isModified(): Boolean {
        val selectedAgentIndex = defaultAgentCombo.selectedIndex
        val selectedAgent = if (selectedAgentIndex >= 0 && selectedAgentIndex < agents.size) agents[selectedAgentIndex].first else ""
        
        return serverUrlField.text != settings.serverUrl ||
                sessionIdField.text != settings.sessionId ||
                usernameField.text != settings.username ||
                String(passwordField.password) != settings.password ||
                autoDetectCheckbox.isSelected != settings.autoDetectSession ||
                tuiModeCheckbox.isSelected != settings.useTuiMode ||
                autoSubmitTuiCheckbox.isSelected != settings.autoSubmitTui ||
                selectedAgent != settings.defaultAgent
    }

    override fun apply() {
        settings.serverUrl = serverUrlField.text
        settings.sessionId = sessionIdField.text
        settings.username = usernameField.text
        settings.password = String(passwordField.password)
        settings.autoDetectSession = autoDetectCheckbox.isSelected
        settings.useTuiMode = tuiModeCheckbox.isSelected
        settings.autoSubmitTui = autoSubmitTuiCheckbox.isSelected
        val selectedAgentIndex = defaultAgentCombo.selectedIndex
        settings.defaultAgent = if (selectedAgentIndex >= 0 && selectedAgentIndex < agents.size) agents[selectedAgentIndex].first else ""
    }

    override fun reset() {
        serverUrlField.text = settings.serverUrl
        sessionIdField.text = settings.sessionId
        usernameField.text = settings.username
        passwordField.text = settings.password
        autoDetectCheckbox.isSelected = settings.autoDetectSession
        tuiModeCheckbox.isSelected = settings.useTuiMode
        autoSubmitTuiCheckbox.isSelected = settings.autoSubmitTui
        loadAgents()
        statusLabel.text = ""
    }
}
