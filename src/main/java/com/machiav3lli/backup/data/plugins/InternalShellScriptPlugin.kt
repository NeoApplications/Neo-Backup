package com.machiav3lli.backup.data.plugins

import com.machiav3lli.backup.data.preferences.tracePlugin
import java.io.File

class InternalShellScriptPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { "${this::class.simpleName} $name <- ${file.name}" }
    }

    companion object : PluginCompanion {

        override fun klass() = InternalShellScriptPlugin::class
        override fun register() = registerType(name(), Companion, listOf("internal_sh"))
        override fun create(file: File): Plugin? = InternalShellScriptPlugin(file)

        fun findScript(name: String) =
            try {
                (getEnabled(name) as InternalShellScriptPlugin).file
            } catch (e: Throwable) {
                null
            }
    }
}
