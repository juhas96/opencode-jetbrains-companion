package com.opencode.plugin.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "OpenCodeSettings",
    storages = [Storage("opencode.xml")]
)
class OpenCodeSettings : PersistentStateComponent<OpenCodeSettings.State> {

    data class State(
        var serverUrl: String = "http://localhost:4096",
        var sessionId: String = "",
        var autoDetectSession: Boolean = true,
        var username: String = "opencode",
        var password: String = "",
        var useTuiMode: Boolean = true,
        var autoSubmitTui: Boolean = false,
        var defaultAgent: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var serverUrl: String
        get() = myState.serverUrl
        set(value) { myState.serverUrl = value }

    var sessionId: String
        get() = myState.sessionId
        set(value) { myState.sessionId = value }

    var autoDetectSession: Boolean
        get() = myState.autoDetectSession
        set(value) { myState.autoDetectSession = value }

    var username: String
        get() = myState.username
        set(value) { myState.username = value }

    var password: String
        get() = myState.password
        set(value) { myState.password = value }

    var useTuiMode: Boolean
        get() = myState.useTuiMode
        set(value) { myState.useTuiMode = value }

    var autoSubmitTui: Boolean
        get() = myState.autoSubmitTui
        set(value) { myState.autoSubmitTui = value }

    var defaultAgent: String
        get() = myState.defaultAgent
        set(value) { myState.defaultAgent = value }
}
