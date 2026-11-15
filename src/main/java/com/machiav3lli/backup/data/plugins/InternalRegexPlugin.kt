package com.machiav3lli.backup.data.plugins

import com.machiav3lli.backup.data.preferences.tracePlugin
import java.io.File

class InternalRegexPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { "${this.javaClass.simpleName} $name <- ${file.name}" }
    }

    fun getExtendedLineRegex(replacements: Map<String, String> = mapOf()) = Regex(
        """(?x)(^("""
                + replaceVars(text, replacements)
                + """)$)"""
    )

    companion object : PluginCompanion {

        override fun klass() = InternalRegexPlugin::class
        override fun register() = registerType(name(), Companion, listOf("internal_regex"))
        override fun create(file: File): Plugin? = InternalRegexPlugin(file)

        fun findRegex(name: String, replacements: Map<String, String> = mapOf()) =
            try {
                (getEnabled(name) as InternalRegexPlugin).getExtendedLineRegex(replacements)
            } catch (e: Throwable) {
                Regex("T-h-I-s--S-h-O-u-L-d--N-e-V-e-R--m-A-t-C-h")
            }

        fun replaceVars(text: String, replacements: Map<String, String> = mapOf()): String {
            var result = text
            replacements.forEach { replacement ->
                result = result.replace(replacement.key, replacement.value)
            }
            return result
        }
    }
}
