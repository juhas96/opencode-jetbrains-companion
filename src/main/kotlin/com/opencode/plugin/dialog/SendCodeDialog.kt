package com.opencode.plugin.dialog

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.opencode.plugin.service.OpenCodeService
import com.opencode.plugin.service.OpenCodeSettings
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class SendCodeDialog(
    private val project: Project,
    private val filePath: String,
    private val startLine: Int,
    private val endLine: Int,
    private val selectedCode: String
) : DialogWrapper(project) {

    companion object {
        val TEMPLATES = listOf(
            "" to "(Custom prompt...)",
            "explain" to "Explain this code",
            "refactor" to "Refactor this code for better readability and maintainability",
            "tests" to "Write unit tests for this code",
            "fix" to "Find and fix any bugs in this code",
            "optimize" to "Optimize this code for better performance",
            "document" to "Add documentation and comments to this code",
            "review" to "Review this code and suggest improvements",
            "types" to "Add or improve type annotations for this code"
        )
    }

    private val settings = service<OpenCodeSettings>()
    private val agents = mutableListOf<Pair<String, String>>()
    private val sessions = mutableListOf<Pair<String, String>>()

    private val templateCombo = ComboBox(DefaultComboBoxModel(TEMPLATES.map { it.second }.toTypedArray())).apply {
        addActionListener {
            val selectedIndex = selectedIndex
            if (selectedIndex > 0) {
                val templateKey = TEMPLATES[selectedIndex].first
                contextTextArea.text = getTemplatePrompt(templateKey)
            }
        }
    }

    private val agentCombo = ComboBox<String>()
    private val sessionCombo = ComboBox<String>()

    private val contextTextArea = JBTextArea(5, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    doOKAction()
                }
            }
        })
    }

    init {
        title = "Send to OpenCode"
        setOKButtonText("Send")
        loadAgents()
        loadSessions()
        init()
    }

    private fun loadSessions() {
        sessions.clear()
        sessions.add("" to "(Auto-detect)")
        
        try {
            val fetchedSessions = project.service<OpenCodeService>().fetchSessions(10)
            fetchedSessions.forEach { session ->
                val displayName = session.title.ifBlank { session.slug.ifBlank { session.id.takeLast(8) } }
                val projectName = session.directory.substringAfterLast("/")
                sessions.add(session.id to "$displayName ($projectName)")
            }
        } catch (_: Exception) {
        }
        
        sessionCombo.model = DefaultComboBoxModel(sessions.map { it.second }.toTypedArray())
    }

    private fun loadAgents() {
        agents.clear()
        agents.add("" to "(Default)")
        
        try {
            val fetchedAgents = project.service<OpenCodeService>().fetchAgents()
            fetchedAgents.forEach { agent ->
                agents.add(agent.name to agent.name)
            }
        } catch (_: Exception) {
        }
        
        agentCombo.model = DefaultComboBoxModel(agents.map { it.second }.toTypedArray())
        
        val defaultAgent = settings.defaultAgent
        if (defaultAgent.isNotBlank()) {
            val index = agents.indexOfFirst { it.first == defaultAgent }
            if (index >= 0) agentCombo.selectedIndex = index
        }
    }

    private fun getTemplatePrompt(key: String): String {
        return when (key) {
            "explain" -> "Please explain what this code does, step by step."
            "refactor" -> "Please refactor this code for better readability and maintainability. Explain your changes."
            "tests" -> "Please write comprehensive unit tests for this code."
            "fix" -> "Please review this code for bugs and issues, and provide fixes."
            "optimize" -> "Please optimize this code for better performance. Explain the optimizations."
            "document" -> "Please add clear documentation and inline comments to this code."
            "review" -> "Please review this code and suggest improvements for code quality, patterns, and best practices."
            "types" -> "Please add or improve type annotations for this code."
            else -> ""
        }
    }

    override fun createCenterPanel(): JComponent {
        val displayPath = if (filePath.length > 60) {
            "..." + filePath.takeLast(57)
        } else {
            filePath
        }

        return panel {
            row {
                label("File: $displayPath")
            }
            row {
                label("Lines: $startLine - $endLine")
            }
            row {
                label("Selected code:")
            }
            row {
                val codePreview = JBTextArea(selectedCode).apply {
                    isEditable = false
                    lineWrap = true
                    wrapStyleWord = true
                    rows = minOf(8, selectedCode.lines().size.coerceAtLeast(3))
                }
                cell(JBScrollPane(codePreview))
                    .align(AlignX.FILL)
            }
            separator()
            row("Template:") {
                cell(templateCombo)
                    .align(AlignX.FILL)
            }
            row("Agent:") {
                cell(agentCombo)
                    .align(AlignX.FILL)
            }
            if (!settings.useTuiMode) {
                row("Session:") {
                    cell(sessionCombo)
                        .align(AlignX.FILL)
                }
            }
            row {
                label("Context (Enter to send, Shift+Enter for new line):")
            }
            row {
                cell(JBScrollPane(contextTextArea))
                    .align(AlignX.FILL)
            }
        }.apply {
            preferredSize = Dimension(600, 500)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = contextTextArea

    fun getAdditionalContext(): String = contextTextArea.text.trim()

    fun getSelectedAgent(): String {
        val selectedIndex = agentCombo.selectedIndex
        return if (selectedIndex > 0 && selectedIndex < agents.size) agents[selectedIndex].first else ""
    }

    fun getSelectedSession(): String {
        val selectedIndex = sessionCombo.selectedIndex
        return if (selectedIndex > 0 && selectedIndex < sessions.size) sessions[selectedIndex].first else ""
    }
}
