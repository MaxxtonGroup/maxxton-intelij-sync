package com.maxxton.mold

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "CodeStyleConfig",
    storages = [Storage("maxxton-codestyle.xml")]
)
class CodeStyleConfig : PersistentStateComponent<CodeStyleConfig.State> {
    private var myState = State()

    data class State(
        var repoUrl: String = "ssh://git@bitbucket.yourdomain.com"
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }
}
