package com.machiav3lli.backup.data.entity

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.storage.StorageManager
import android.os.storage.StorageManager.UUID_DEFAULT
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.machiav3lli.backup.MIME_TYPE_DIR
import com.machiav3lli.backup.MIME_TYPE_FILE
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PROP_NAME
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.unexpectedException
import com.machiav3lli.backup.manager.handler.ShellCommands
import com.machiav3lli.backup.ui.pages.pref_cacheFileLists
import com.machiav3lli.backup.ui.pages.pref_cacheUris
import com.machiav3lli.backup.ui.pages.pref_shadowRootFile
import com.machiav3lli.backup.utils.SystemUtils.getShadowPath
import com.machiav3lli.backup.utils.SystemUtils.isWritablePath
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder


fun getCursorString(cursor: Cursor, columnName: String): String? {
    val index = cursor.getColumnIndex(columnName)
    return cursor.getStringOrNull(index)
}

fun getCursorLong(cursor: Cursor, columnName: String): Long? {
    val index = cursor.getColumnIndex(columnName)
    return cursor.getLongOrNull(index)
}

/* hg42: keep in case it's needed later, and yes, I know it could be excluded automatically

fun getCursorInt(cursor: Cursor, columnName: String): Int? {
    val index = cursor.getColumnIndex(columnName)
    return cursor.getIntOrNull(index)
}

private fun Uri.getRawType(context: Context): String? = try {
    context.contentResolver.query(this, null, null, null, null)?.let { cursor ->
        cursor.run {
            if (moveToFirst())
                //context.contentResolver.getType(this@getRawType)
                getCursorString(this, DocumentsContract.Document.COLUMN_MIME_TYPE)
            else
                null
        }.also { cursor.close() }
    }
} catch (e: Throwable) {
    unhandledException(e, this)
    null
}

fun Uri.canRead(context: Context): Boolean = when {
    context.checkCallingOrSelfUriPermission(
        this,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    ) // Ignore if grant doesn't allow read
            != PackageManager.PERMISSION_GRANTED -> false
    else                                         -> !TextUtils.isEmpty(getRawType(context))
} // Ignore documents without MIME

fun Uri.canWrite(context: Context): Boolean {
    // Ignore if grant doesn't allow write
    if (context.checkCallingOrSelfUriPermission(this, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val type = getRawType(context)
    val flags = queryForLong(context, DocumentsContract.Document.COLUMN_FLAGS)?.toInt() ?: 0
    // Ignore documents without MIME
    if (TextUtils.isEmpty(type)) {
        return false
    }
    // Deletable documents considered writable
    if (flags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) {
        return true
    }
    //return if (MIME_TYPE_DIR == type && flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0) {
    return if (MIME_TYPE_DIR.equals(type) && flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0) {
        // Directories that allow create considered writable
        true
    } else !TextUtils.isEmpty(type)
            && flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0
    // Writable normal files considered writable
}

private fun Uri.queryForLong(context: Context, column: String): Long? {
    val resolver = context.contentResolver
    var cursor: Cursor? = null
    try {
        cursor = resolver.query(this, null, null, null, null)
        if (cursor!!.moveToFirst()) {
            return getCursorLong(cursor, column)
        }
    } catch (e: Throwable) {
        unhandledException(e, "$this column: $column")
    } finally {
        closeQuietly(cursor)
    }
    return 0
}

*/

fun Uri.exists(context: Context): Boolean {
    val resolver = context.contentResolver
    var cursor: Cursor? = null
    return try {
        cursor = resolver.query(
            this, arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
            ), null, null, null
        )
        (cursor?.count ?: 0) > 0
    } catch (_: IllegalArgumentException) {
        false
    } catch (e: Throwable) {
        unexpectedException(e, this)
        false
    } finally {
        closeQuietly(cursor)
    }
}

private fun closeQuietly(closeable: AutoCloseable?) {
    if (closeable != null) {
        try {
            closeable.close()
        } catch (rethrown: RuntimeException) {
            throw rethrown
        } catch (e: Throwable) {
            unexpectedException(e)
        }
    }
}

fun Context.uriFromFile(file: File): Uri =
    FileProvider.getUriForFile(
        this,
        "${packageName}.provider",
        file
    )


/*
 TODO migrate at some point to
   FuckSAF https://github.com/K1rakishou/Fuck-Storage-Access-Framework
   OR SimpleStorage https://github.com/anggrayudi/SimpleStorage

    so instead of the current dirty code
        file.doSomeThing() ?: DocumentContract.doSomething(_uri)
    there should be an implementation member
    but it is not implemented like this, because we sometimes need the uri even if
    implementation is via file (mainly for Android UI interactions, backupDir and share)
    so we probably need a conversion function
        or caching the relation
        or having a second implementation for some objects (that may be added later)
 */

class FileDuplicationException(message: String) : IOException(message)
class FileDuplicationHandlingException(message: String, cause: Throwable) :
    IOException(message, cause)

open class StorageFile {
    var parent: StorageFile? = null
    val context: Context
        get() = NeoApp.context

    private var file: RootFile? = null
    private var _uri: Uri? = null
    val uri: Uri?
        get() = _uri ?: file?.let { f ->
            parent?.let { p ->
                name?.let { n ->
                    _uri = p.findUri(n)
                    _uri
                }
            } ?: context.uriFromFile(f)
        }

    data class DocumentInfo(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val lastModified: Long,
    )

    private var documentInfo: DocumentInfo? = null
        get() {
            if (field == null)
                field = retrieveDocumentInfo()
            return field
        }

    constructor(
        parent: StorageFile?,
        uri: Uri?,
        name: String? = null,
        allowShadowing: Boolean = false, // mandatory usage of SAF is less used (e.g. for sharing)
    ) {
        initializeFromUri(parent, uri, name, allowShadowing)
    }

    constructor(file: RootFile) {
        this.file = file
    }

    constructor(file: File) {
        this.file = RootFile(file)
    }

    constructor(parent: StorageFile, file: RootFile) {
        this.parent = parent
        this.file = file
    }

    constructor(parentFile: RootFile, subPath: String) {
        this.parent = StorageFile(parentFile)
        this.file = RootFile(parentFile, subPath)
    }

    constructor(parentFile: File, subPath: String) {
        this.parent = StorageFile(parentFile)
        this.file = RootFile(parentFile, subPath)
    }

    private fun retrieveDocumentInfo(cursor: Cursor): DocumentInfo {
        val id = getCursorString(
            cursor,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        ) ?: "???"
        val name = getCursorString(
            cursor,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        ) ?: "???"
        val mimeType = getCursorString(
            cursor,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        ) ?: ""
        val size = getCursorLong(
            cursor,
            DocumentsContract.Document.COLUMN_SIZE
        )?.coerceAtLeast(0) ?: 0
        var lastModified = getCursorLong(
            cursor,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        ) ?: 0
        if (lastModified < 0)
            lastModified = 0
        //flags = getCursorInt(cursor, Document.COLUMN_FLAGS),
        //summary = getCursorString(cursor, Document.COLUMN_SUMMARY),
        //icon = getCursorInt(cursor, Document.COLUMN_ICON),
        return DocumentInfo(
            id = id,
            name = name,
            mimeType = mimeType,
            size = size,
            lastModified = lastModified
        )
    }

    private fun retrieveDocumentInfo(): DocumentInfo? {
        val resolver = context.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(
                _uri!!,
                null /*documentColumns*/, null, null, null
            )
            cursor?.let {
                if (it.moveToFirst()) {
                    return retrieveDocumentInfo(it)
                }
            }
        } catch (_: Throwable) {
            //logException(e, "$this")
            // ignore, file is not there
        } finally {
            //cursor?.let { closeQuietly(it) }
            cursor?.close()
        }
        return null
    }

    fun initializeFromUri(
        parent: StorageFile?,
        uri: Uri? = null,
        name: String? = null,
        allowShadowing: Boolean = true, // mandatory usage of SAF is less used (e.g. for sharing)
    ) {
        this.parent = parent
        this._uri = uri
        name?.let { this.name = it }
        if (parent == null && uri != null) {
            if (pref_shadowRootFile.value && allowShadowing) {
                try {
                    if (uri.scheme == "file" || uri.scheme == null) {
                        val checkFile = RootFile(
                            uri.path
                                ?: Uri.decode(uri.toString())  // paranoid fallback in case it is not
                        )
                        if (isWritablePath(checkFile)) {
                            Timber.i("found direct RootFile shadow at '$checkFile'")
                            file = checkFile
                        } else
                            throw Exception("cannot use RootFile '$checkFile'")
                    } else {
                        val last =
                        //uri.lastPathSegment // docs say: last segment of the decoded(!) path, not the encoded one
                            // because this is not correct = not reliable, we make it explicit:
                            URLDecoder.decode(uri.encodedPath?.split("/")?.last() ?: "", "UTF-8")
                        Timber.i("StorageFile: last=$last uri=$uri")
                        var (storage, subPath) = last.split(":", limit = 2)
                        //val user = ShellCommands.currentProfile
                        val userProvider = (uri.authority ?: "").split("@", limit = 2)
                        val user =
                            if (userProvider.size > 1)
                                userProvider[0]
                            else
                                ShellCommands.currentProfile.toString()
                        if (storage == "primary")
                            storage = "emulated/$user"
                        file = getShadowPath(
                            user,
                            storage,
                            subPath,
                            ::isWritablePath
                        )
                        if (file == null)
                            throw Exception("cannot find RootFile shadow at $last")
                    }
                } catch (_: Throwable) {
                    file = null
                    Timber.i("using access via SAF")
                }
            }
        }
        cacheSetUri(uri.toString(), this)
    }

    var name: String? = null
        get() {
            if (field == null) {
                field = file?.name ?: run {
                    uri?.lastPathSegment?.substringAfterLast("/")
                }
            }
            return field
        }
        private set

    val path: String?
        get() = file?.path ?: uri?.path

    override fun toString(): String {
        return path ?: "null"
    }

    val isFile: Boolean
        get() = !isDirectory

    val isDirectory: Boolean
        get() = file?.isDirectory
            ?: (documentInfo?.mimeType == MIME_TYPE_DIR)

    val isPropertyFile: Boolean
        get() = name?.endsWith(".$PROP_NAME") ?: false

    fun exists(): Boolean =
        file?.exists()
            ?: ((documentInfo?.id?.isNotEmpty()) ?: false)

    val size: Long
        get() = (
                if (file != null)
                    (file?.length() ?: 0L)
                else
                    (documentInfo?.size ?: 0)
                )

    val freeSpace: Long
        get() = file?.usableSpace ?: run {
            // NO access to RootFile
            try {
                _uri?.let { uri ->
                    val storageManager =
                        context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                            ?: return@run 0L

                    // get current storage UUID; Fallback to internal storage
                    val uuid = storageManager.getStorageVolume(File(uri.path ?: ""))
                        ?.uuid?.let { java.util.UUID.fromString(it) }
                        ?: UUID_DEFAULT

                    val storageStatsManager =
                        context.getSystemService(Context.STORAGE_STATS_SERVICE) as? android.app.usage.StorageStatsManager

                    storageStatsManager?.getFreeBytes(uuid)
                        ?: android.os.StatFs(uri.path ?: "/").availableBytes
                } ?: 0L
            } catch (e: Throwable) {
                logException(e, "freeSpace", backTrace = false)
                // 1GB as safe Fallback
                1024L * 1024L * 1024L
            }
        }

    fun inputStream(): InputStream? {
        return file?.inputStream() ?: uri?.let { uri ->
            context.contentResolver?.openInputStream(uri)
        }
    }

    fun outputStream(): OutputStream? {
        documentInfo = null
        return file?.outputStream() ?: uri?.let { uri ->
            context.contentResolver?.openOutputStream(uri, "w")
        }
    }

    fun createDirectory(): StorageFile? {
        parent?.let { dir ->
            name?.let { name ->
                return dir.createDirectory(name)
            }
        }
        return null
    }

    fun createFile(): StorageFile? {
        parent?.let { dir ->
            name?.let { name ->
                return dir.createFile(name)
            }
        }
        return null
    }

    fun createDirectory(displayName: String): StorageFile =
        createFile(displayName, MIME_TYPE_DIR)

    fun createFile(
        displayName: String,
        mimeType: String = MIME_TYPE_FILE,
    ): StorageFile {
        var newFile =
            file?.let {
                if (mimeType == MIME_TYPE_DIR) {
                    val newDir = RootFile(it, displayName)
                    newDir.mkdirs()
                    StorageFile(this, newDir)
                } else {
                    val newFile = RootFile(it, displayName)
                    newFile.createNewFile()
                    StorageFile(this, newFile)
                }
            } ?: run {
                if (mimeType == MIME_TYPE_DIR) {
                    // if the directory already exists, just use it
                    findFile(displayName)
                    // otherwise a new one
                        ?: StorageFile(
                            this,
                            createDocument(context, uri!!, mimeType, displayName),
                            //context,
                            displayName
                        )
                } else {
                    // if the file already exists, delete it, because we want to start it again
                    findFile(displayName)?.delete()
                    // always use the new one
                    StorageFile(
                        this,
                        createDocument(context, uri!!, mimeType, displayName),
                        //context,
                        displayName
                    )
                }
            }
        if ((newFile.path?.endsWith(displayName) ?: true).not()) {
            // should now be paranoid
            // it would mean, the first findFile didn't work (note, it was missing, so stay safe here)
            var message =
                "(SAF) file duplication: '$displayName' is not last part of '${newFile.path}'"
            try {
                val found = findFile(displayName)
                if (mimeType == MIME_TYPE_DIR) {
                    found?.let {
                        // the directory (probably) exists
                        message += ", delete ${newFile.path}, use ${it.path}"
                        if (newFile.exists()) // do it safe, rumors are, it would delete the parent !!!
                            newFile.delete()
                        // use the found directory
                        newFile = it
                    } ?: run {
                        // wow, even more paranoid
                        // SAF added a new duplicate, but the wanted name doesn't exist !!!
                        // so why would it create a duplicate?
                        // but one has already seen horses puking
                        message += ", $displayName DOES NOT EXIST, rename new directory"
                        newFile.renameTo(displayName)
                    }
                } else {
                    // the file (probably) exists
                    found?.let {
                        message += ", delete ${it.path}"
                        try {
                            it.delete()
                        } catch (e: Throwable) {
                            unexpectedException(
                                e,
                                "file duplication happened, but original file cannot be deleted"
                            )
                        }
                    }
                    message += ", rename new file"
                    newFile.renameTo(displayName)
                }
                throw FileDuplicationException(message)
            } catch (e: FileDuplicationException) {
                unexpectedException(e)
            } catch (e: Throwable) {
                unexpectedException(
                    FileDuplicationHandlingException(message, e)
                )
            }
        }
        // creates a subobject inside this
        path?.let { cacheFilesAdd(it, newFile) }
        return newFile
    }

    fun delete(): Boolean { // delete only empty directories, full is a task for deleteRecursive
        traceDebug { "########## delete $path" }
        val ok = try {
            file?.delete()
                ?: run {
                    // don't delete if any file inside
                    if (isDirectory && listFiles(maxFiles = 1, useCache = false).isNotEmpty())
                        false
                    else {
                        traceDebug { "########## deleteDocument ######################### $_uri" }
                        DocumentsContract.deleteDocument(context.contentResolver, _uri!!)
                    }
                }
        } catch (_: FileNotFoundException) {
            false
        } catch (_: IllegalArgumentException) { // can also happen with file not found
            false
        } catch (e: Throwable) {
            logException(e, path, backTrace = false)
            false
        }
        if (ok) {
            // removes this, so need to change parent
            parent?.path?.let { cacheFilesRemove(it, this) }
        }
        documentInfo = null
        return ok
    }

    fun renameTo(displayName: String): Boolean {
        if (name == displayName)
            return true
        // removes this, so need to change parent
        parent?.path?.let { cacheFilesRemove(it, this) }
        var ok = false
        file?.let { oldFile ->
            val newFile = RootFile(oldFile.parent, displayName)
            ok = oldFile.renameTo(newFile)
            file = newFile
        } ?: try {
            val result =
                context.let { context ->
                    uri?.let { uri ->
                        DocumentsContract.renameDocument(
                            context.contentResolver, uri, displayName
                        )
                    }
                }
            if (result != null) {
                _uri = result
                ok = true
            } else {
                ok = false
            }
        } catch (e: Throwable) {
            logException(e, path, backTrace = false)
            ok = false
        }
        // adds this, so need to change parent
        parent?.path?.let { cacheFilesAdd(it, this) }
        if (ok) name = displayName
        return ok
    }

    fun readText(): String {
        return try {
            file?.readText()
                ?: run {
                    inputStream()?.reader()?.use {
                        it.readText()
                    } ?: ""
                }
        } catch (e: FileNotFoundException) {
            logException(e, path, backTrace = false)
            ""
        } catch (e: Throwable) {
            logException(e, path, backTrace = false)
            ""
        }
    }

    fun writeText(text: String): Boolean {
        val ok = try {
            // cache handled in createFile
            createFile()?.outputStream()?.writer()?.use {
                it.write(text)
                true
            } ?: false
        } catch (e: Throwable) {
            logException(e, path, backTrace = false)
            false
        }
        documentInfo = null
        return ok
    }

    fun findUri(displayName: String): Uri? {
        // recurse down, uri?.run { ... } prevents optimizing-away (and null test makes sense anyway)
        uri?.run {
            try {
                for (file in listFiles(forceUri = true, useCache = false)) {
                    if (displayName == file.name) {
                        return file._uri
                    }
                }
            } catch (_: FileNotFoundException) {
                // ignore
            } catch (e: Throwable) {
                logException(e, path, backTrace = false)
            }
        }
        return null
    }

    fun findFile(displayName: String): StorageFile? {
        try {
            file?.let {
                val found = StorageFile(this.file!!, displayName)
                return if (found.exists()) found else null
            } ?: run {
                for (f in listFiles()) {
                    if (displayName == f.name) {
                        return f
                    }
                }
            }
        } catch (_: FileNotFoundException) {
            // ignore
        } catch (e: Throwable) {
            logException(e, path, backTrace = false)
        }
        return null
    }

    @Throws(FileNotFoundException::class)
    fun listFiles(
        forceUri: Boolean = false,
        maxFiles: Int = 0,
        useCache: Boolean = true,
    ): List<StorageFile> {

        try {
            exists()
        } catch (_: Throwable) {
            throw FileNotFoundException("File $_uri does not exist")
        }

        path?.let { path ->

            val files = (if (useCache) cacheGetFiles(path) else null) ?: run {
                val results = mutableListOf<StorageFile>()
                if ((file != null) and !forceUri) {
                    file!!.listFiles()?.forEach { child ->
                        results.add(StorageFile(this, child))
                    }
                } else {
                    context.contentResolver?.let { resolver ->
                        val childrenUri = try {
                            DocumentsContract.buildChildDocumentsUriUsingTree(
                                this.uri,
                                DocumentsContract.getDocumentId(this.uri)
                            )
                        } catch (_: IllegalArgumentException) {
                            this.uri
                            //return listOf()
                        } ?: run {
                            return listOf()
                        }
                        var cursor: Cursor? = null
                        try {
                            cursor = resolver.query(
                                // "For type TreeDocumentFile, getName() is implemented as
                                // a query to DocumentsProvider which is.... slow."
                                // so avoid getName by using COLUMN_DISPLAY_NAME
                                // (also add name to constructor and allow setting the name field)
                                childrenUri, null /*documentColumns*/,
                                null, null, null
                            )
                            var documentUri: Uri
                            while (cursor?.moveToNext() == true) {
                                try {
                                    val docInfo = retrieveDocumentInfo(cursor)
                                    documentUri =
                                        DocumentsContract.buildDocumentUriUsingTree(
                                            this.uri,
                                            docInfo.id
                                        )
                                    val file =
                                        StorageFile(this, documentUri, docInfo.name)
                                    file.documentInfo = docInfo
                                    results.add(file)
                                    if (maxFiles > 0 && maxFiles >= results.size)
                                        break
                                } catch (e: Throwable) {
                                    logException(e, path, backTrace = false)
                                }
                            }
                        } catch (_: IllegalArgumentException) {
                            // ignore
                        } catch (e: Throwable) {
                            logException(e, path, backTrace = false)
                        } finally {
                            closeQuietly(cursor)
                        }
                    }
                }
                cacheSetFiles(path, results)
                results
            }
            return files
        } ?: return listOf()
    }

    fun ensureDirectory(dirName: String): StorageFile {
        //val dir = findFile(dirName) ?: createDirectory(dirName)   // findFile not necessary any more
        val dir = createDirectory(dirName)
        return dir
    }

    fun deleteRecursive(): Boolean {
        traceDebug { "########## deleteRecursive $path" }
        return when {
            isFile      ->
                delete()

            isDirectory ->
                try {
                    val contents = listFiles()
                    var result = true
                    contents.forEach { subFile ->
                        result = result && subFile.deleteRecursive()
                    }
                    result && delete()
                } catch (_: FileNotFoundException) {
                    false
                } catch (e: Throwable) {
                    logException(e, path, backTrace = false)
                    false
                }

            else        ->
                false
        }
    }

    companion object {
        // TODO revamp whole caching logic
        //TODO hg42 manage file trees instead of single files and let StorageFile and caches use them
        //TODO hg42 access should automatically checkCache
        private var fileListCache = mutableMapOf<String, MutableList<StorageFile>>()
        private var uriStorageFileCache = mutableMapOf<String, StorageFile>()
        private var invalidateFilters = mutableListOf<(String) -> Boolean>()

        /* unused, using null instead seems to be faster
        val documentColumns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        */

        fun fromUri(uri: Uri): StorageFile {
            if (pref_cacheUris.value) {
                cacheCheck()
                return cacheGetUri(uri.toString())
                    ?: StorageFile(null, uri)
            } else {
                return StorageFile(null, uri)
            }
        }

        fun fromUri(uri: String): StorageFile {
            return fromUri(uri.toUri())
        }

        fun createDocument(
            context: Context,
            uri: Uri,
            mimeType: String,
            displayName: String,
        ): Uri? {
            return try {
                DocumentsContract.createDocument(
                    context.contentResolver,
                    uri,
                    mimeType,
                    displayName
                )
            } catch (_: FileNotFoundException) {
                null
            } catch (e: Throwable) {
                logException(e, "$uri / $displayName", backTrace = false)
                null
            }
        }

        fun invalidateCache(filter: (String) -> Boolean) {
            try {
                synchronized(invalidateFilters) {
                    invalidateFilters.add(filter)
                }
            } catch (_: ArrayIndexOutOfBoundsException) {
                // ignore
            }
            cacheCheck()    // non-lazy seems to be better
        }

        fun invalidateCache() {
            synchronized(invalidateFilters) {
                invalidateFilters = mutableListOf({ true })
            }
            cacheCheck()    // non-lazy seems to be better
        }

        fun invalidateCache(storageFile: StorageFile) {
            storageFile.path?.let { path -> invalidateCache { it.startsWith(path) } }
        }

        private fun cacheCheck() {
            try {
                synchronized(invalidateFilters) {
                    while (invalidateFilters.isNotEmpty()) {
                        invalidateFilters.removeFirstOrNull()?.let { isInvalid ->
                            synchronized(fileListCache) {
                                fileListCache = fileListCache
                                    .toMap()
                                    .filterNot { isInvalid(it.key) }
                                    .toMutableMap()
                            }
                            synchronized(uriStorageFileCache) {
                                uriStorageFileCache = uriStorageFileCache
                                    .toMap()
                                    .filterNot { isInvalid(it.key) }
                                    .toMutableMap()
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                logException(e, backTrace = true)
            }
        }

        private fun cacheGetFiles(id: String): List<StorageFile>? {
            if (pref_cacheFileLists.value) {
                cacheCheck()
                return synchronized(fileListCache) {
                    fileListCache[id]?.drop(0)
                }
            }
            return null
        }

        private fun cacheSetFiles(id: String, files: List<StorageFile>) {
            synchronized(fileListCache) {
                fileListCache[id] = files.toMutableList()
            }
        }

        private fun cacheGetUri(id: String): StorageFile? {
            cacheCheck()
            synchronized(uriStorageFileCache) {
                return uriStorageFileCache[id]
            }
        }

        private fun cacheSetUri(id: String, file: StorageFile) {
            synchronized(uriStorageFileCache) {
                uriStorageFileCache[id] = file
            }
        }

        private fun cacheFilesAdd(path: String, file: StorageFile) {
            synchronized(fileListCache) {
                fileListCache[path]?.apply {
                    //removeAll { it.name == file.name }
                    find { it.name == file.name }?.let { remove(it) }
                    add(file)
                } ?: run {
                    fileListCache[path] = mutableListOf(file)
                }
            }
        }

        private fun cacheFilesRemove(path: String, file: StorageFile?) {
            synchronized(fileListCache) {
                file?.let {
                    fileListCache[path]?.apply {
                        removeAll { it.name == file.name }
                    }
                } ?: fileListCache.remove(path)
            }
        }

        private fun closeQuietly(closeable: AutoCloseable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (rethrown: RuntimeException) {
                    // noinspection ProhibitedExceptionThrown
                    throw rethrown
                } catch (e: Throwable) {
                    logException(e, backTrace = true)
                }
            }
        }
    }
}

class UndeterminedStorageFile(val parent: StorageFile, val subPath: String) {

    val path: String get() = parent.path + "/" + subPath

    fun findFile(): StorageFile? {
        var dir: StorageFile? = parent
        val components = subPath.split('/').toMutableList()
        while (dir != null && components.size > 1) {
            components.removeFirstOrNull()?.let { component ->
                dir = dir.findFile(component)
            }
        }
        return dir?.findFile(components.first())
    }

    fun exists() = findFile() != null

    val size: Long get() = findFile()?.size ?: 0

    fun createFile(): StorageFile {
        var dir = parent
        val components = subPath.split('/').toMutableList()
        while (components.size > 1) {
            components.removeFirstOrNull()?.let { component ->
                dir = dir.createDirectory(component)
            }
        }
        return dir.createFile(components.first())
    }

    fun writeText(text: String): StorageFile? {
        val file = createFile()
        if (file.writeText(text))
            return file
        return null
    }

    fun readText() = findFile()?.readText() ?: ""

    fun delete() = findFile()?.delete()
}
