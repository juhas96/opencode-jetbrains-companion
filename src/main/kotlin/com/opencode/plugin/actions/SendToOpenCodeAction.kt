package com.opencode.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.opencode.plugin.dialog.SendCodeDialog
import com.opencode.plugin.service.OpenCodeService

class SendToOpenCodeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return
        val document = editor.document

        val startLine = document.getLineNumber(selectionModel.selectionStart) + 1
        val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1

        val dialog = SendCodeDialog(project, file.path, startLine, endLine, selectedText)
        if (dialog.showAndGet()) {
            val additionalContext = dialog.getAdditionalContext()
            val agent = dialog.getSelectedAgent()
            val sessionId = dialog.getSelectedSession()
            project.service<OpenCodeService>().sendToOpenCode(
                filePath = file.path,
                startLine = startLine,
                endLine = endLine,
                additionalContext = additionalContext,
                agent = agent,
                overrideSessionId = sessionId
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = hasSelection
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
