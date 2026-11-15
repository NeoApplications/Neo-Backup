package com.machiav3lli.backup.data.plugins

import androidx.compose.runtime.Composable
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.preferences.tracePlugin
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import timber.log.Timber
import java.io.File
import kotlin.reflect.KClass

interface PluginCompanion {

    fun klass(): KClass<out Plugin>
    fun name(): String = klass().simpleName?.removeSuffix("Plugin") ?: "Unknown"
    fun register(): Boolean
    fun create(file: File): Plugin?
}

const val OFF_SUFFIX = "_off"

abstract class Plugin(val name: String, var file: File) {

    val className: String? get() = this::class.simpleName
    val typeName: String get() = className?.replace("Plugin", "") ?: "Unknown"
    var enabled = true

    @Composable
    abstract fun Editor()

    abstract fun save()
    abstract fun delete()

    fun enable(enable: Boolean = true) {
        if (enable != enabled) {
            enabled = enable
            ensureEditable()
            if (enable) {
                if (file.path.endsWith(OFF_SUFFIX)) {
                    file.renameTo(File(file.path.removeSuffix(OFF_SUFFIX)))
                    scan()
                }
            } else {
                if (!file.path.endsWith(OFF_SUFFIX)) {
                    file.renameTo(File(file.path + OFF_SUFFIX))
                    scan()
                }
            }
        }
    }

    val isBuiltin get() = file.path.startsWith(builtinDir.path)

    fun ensureEditable() {
        if (isBuiltin) {
            val userFile = fileFor(
                dir = userDir!!,
                name = name,
                type = typeName
            )!!
            if (userFile.path != file.path) {
                file = File(userFile.path)
                save()
            }
        }
    }

    companion object {
        // add new plugin classes here, necessary to have all classes initialized
        val pluginCompanions
            get() = mutableListOf(
                SpecialFilesPlugin.Companion,
                InternalRegexPlugin.Companion,
                InternalShellScriptPlugin.Companion,
            )
        var pluginTypes = mutableMapOf<String, PluginCompanion>()
        var pluginExtensions = mutableMapOf<String, String>()
        var pluginExtension = mutableMapOf<String, String>()
        const val DEFAULT_TYPE = "SpecialFiles"

        fun registerType(
            type: String,
            pluginCompanion: PluginCompanion,
            extensions: List<String>,
        ): Boolean {
            tracePlugin { "register ${pluginCompanion.name()} type: $type, extensions: $extensions" }
            pluginTypes[type] = pluginCompanion
            pluginExtension[type] = extensions.first()
            extensions.forEach {
                pluginExtensions[it] = type
            }
            return true
        }

        fun createFrom(file: File): Plugin? {
            var extension = file.extension
            var off = false
            if (extension.endsWith(OFF_SUFFIX)) {
                off = true
                extension = extension.removeSuffix(OFF_SUFFIX)
            }
            return pluginExtensions[extension]?.let { type ->
                val plugin = pluginTypes[type]?.create(file)
                plugin?.enable(!off)
                plugin
            }
        }

        var scanned = false

        private var plugins = mutableMapOf<String, Plugin>()

        fun setPlugins(pluginMap: Map<String, Plugin>) {
            plugins = pluginMap.toMutableMap()
        }

        fun setPlugins(vararg args: Pair<String, Plugin>) {
            setPlugins(*args)
        }

        fun getEnabled(name: String) = plugins[name]?.takeIf { it.enabled }

        fun getAll(predicate: (Map.Entry<String, Plugin>) -> Boolean) =
            plugins.filter(predicate = predicate)

        inline fun <reified T> getAll() = getAll { it.value is T }.map { it.value as T }

        // files need to be copied from ap[k to filesDir, so use assets.directory instead of filesDir
        //        val builtinDir get() = OABX.context.filesDir?.resolve("plugin")
        val builtinDir get() = NeoApp.assets.directory.resolve("plugin")
        val userDir get() = NeoApp.context.getExternalFilesDir(null)?.resolve("plugin")

        fun loadPluginFromDir(dir: File): Plugin? {
            Timber.w("not implemented: loadPluginFromDir ${dir.name}")
            return null
        }

        fun loadPlugin(file: File): Plugin? {
            return try {
                if (file.isDirectory) {
                    loadPluginFromDir(file)
                } else {
                    createFrom(file)
                }
            } catch (e: Throwable) {
                logException(e)
                null
            }
        }

        fun loadPluginsFromDir(dir: File) {
            dir.listFiles()?.forEach {
                loadPlugin(it)?.let { plugin ->
                    plugins[plugin.name] = plugin
                }
            }
        }

        fun scan() {    // must be omnipotent

            pluginCompanions.forEach {
                it.register()
            }

            synchronized(Plugin) {
                scanned = false
                plugins.clear()
                loadPluginsFromDir(builtinDir)
                userDir?.let { loadPluginsFromDir(it) }
                scanned = true
            }
        }

        fun ensureScanned() {
            synchronized(Plugin) {
                if (!scanned) {
                    scan()
                }
            }
        }

        fun fileFor(dir: File, name: String, type: String) =
            pluginExtension[type]?.let {
                dir.resolve("$name.$it")
            }

        fun typeFor(plugin: Plugin?) = plugin?.typeName ?: "Unknown"

        const val BUILTIN = "<builtin>"
        const val USER = "<user>"

        fun displayPath(path: String): String {
            var result = path
            builtinDir.path.let { builtinDir ->
                result = result.replace(builtinDir, BUILTIN)
            }
            userDir?.path?.let { userDir ->
                result = result.replace(userDir, USER)
            }
            return result
        }
    }
}

