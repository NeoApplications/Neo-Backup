package com.machiav3lli.backup.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.SystemClock
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.LogsHandler
import com.machiav3lli.backup.manager.handler.ShellCommands
import com.machiav3lli.backup.utils.extensions.Android
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


object SystemUtils {

    private fun Context.getApplicationInfos(what: Int = 0): PackageInfo? =
        packageManager.getPackageInfo(packageName, what)

    @Suppress("DEPRECATION")
    private fun Context.getIssuer(): String? {
        runCatching {
            val signatures = if (Android.minSDK(Build.VERSION_CODES.P)) {
                val packageInfo = getApplicationInfos(PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo?.signingInfo
                signingInfo?.signingCertificateHistory ?: arrayOf()
            } else {
                val packageInfo = getApplicationInfos(PackageManager.GET_SIGNATURES)
                packageInfo?.signatures ?: arrayOf()
            }
            if (signatures.isEmpty())
                return null
            val signature = signatures[0]
            val signatureBytes = signature.toByteArray()
            val cf = CertificateFactory.getInstance("X509")
            val x509Certificate: X509Certificate =
                cf.generateCertificate(ByteArrayInputStream(signatureBytes)) as X509Certificate
            val dnName = x509Certificate.issuerDN.name
            val names = dnName.split(",").associate {
                val (field, value) = it.split("=", limit = 2)
                field to value
            }
            var issuer = names["CN"]
            names["O"]?.let { if (issuer != it) issuer = "$issuer / $it" }
            return issuer ?: dnName
        }
        return null
    }

    val packageName get() = com.machiav3lli.backup.BuildConfig.APPLICATION_ID
    val versionCode get() = com.machiav3lli.backup.BuildConfig.VERSION_CODE
    val versionName get() = com.machiav3lli.backup.BuildConfig.VERSION_NAME
    val Context.updateId get() = "${getApplicationInfos()?.lastUpdateTime?.toString()}-${versionName}"
    val backupVersionCode get() = com.machiav3lli.backup.BuildConfig.MAJOR * 1000 + com.machiav3lli.backup.BuildConfig.MINOR

    val Context.applicationIssuer get() = getIssuer() ?: "UNKNOWN ISSUER"

    val numCores get() = Runtime.getRuntime().availableProcessors()

    suspend fun <T> runParallel(
        items: List<T>,
        scope: CoroutineScope = MainScope(),
        pool: CoroutineDispatcher = Dispatchers.IO,
        todo: suspend (item: T) -> Unit
    ) {
        items.map { scope.async(pool) {
            todo(it) }
        }.awaitAll()
    }

    fun Context.share(text: String, subject: String? = null, asFile: Boolean = true) {
        if (asFile) {
            getExternalFilesDir(DIRECTORY_DOWNLOADS)
                ?.resolve("NeoBackup-share.txt")
                ?.also { it.writeText(text) }
                ?.let { share(StorageFile(it)) }
            return
        }
        MainScope().launch(Dispatchers.IO) {
            try {
                if (text.isEmpty())
                    throw Exception("text is empty")
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    if (subject.isNullOrEmpty())
                        putExtra(Intent.EXTRA_SUBJECT, "[NeoBackup]")
                    else
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                val shareIntent = Intent.createChooser(sendIntent, subject ?: "NeoBackup")
                NeoApp.activity?.startActivity(shareIntent)
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e)
            }
        }
    }

    fun share(file: StorageFile, asFile: Boolean = true) {
        //if (asFile && file.file == null) {
        //    OABX.context.getExternalFilesDir(DIRECTORY_DOWNLOADS)
        //        ?.resolve(file.name ?: "NeoBackup-share.txt")
        //        ?.also { it.writeText(file.readText()) }
        //        ?.let { share(StorageFile(it)) }
        //    return
        //}
        MainScope().launch(Dispatchers.IO) {
            try {
                val text = if (asFile) "" else file.readText()
                if (!asFile and text.isEmpty())
                    throw Exception("${file.name} is empty or cannot be read")
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(Intent.EXTRA_SUBJECT, "[NeoBackup] ${file.name}")
                    if (asFile)
                        putExtra(Intent.EXTRA_STREAM, file.uri)  // send as file
                    else
                        putExtra(Intent.EXTRA_TEXT, text)       // send as text
                }
                val shareIntent = Intent.createChooser(sendIntent, file.name)
                NeoApp.activity?.startActivity(shareIntent)
            } catch (e: Throwable) {
                LogsHandler.unexpectedException(e)
            }
        }
    }

    fun isWritablePath(file: RootFile?): Boolean =
        file?.let { file.exists() && file.canRead() && file.canWrite() } ?: false

    fun isReadablePath(file: RootFile?): Boolean =
        file?.let { file.exists() && file.canRead() } ?: false

    val storagePath = mutableMapOf<String, RootFile?>()

    fun getShadowPath(
        user: String,
        storage: String,
        subPath: String,
        isUseablePath: (file: RootFile?) -> Boolean = ::isWritablePath
    ): RootFile? {
        // check final path for shadow
        val key = "shadow:$user:$storage:$subPath"
        return storagePath.getOrElse(key) {
            val possiblePaths = listOf(
                "/mnt/media_rw/$storage/$subPath",
                "/mnt/pass_through/$user/$storage/$subPath",
                "/mnt/runtime/full/$storage/$subPath",
                "/mnt/runtime/default/$storage/$subPath",

                // NOTE: lockups occur in emulator (or A12?) for certain paths
                // e.g. /storage/emulated/$user
                //
                // lockups! primary links to /storage/emulated/$user and all self etc.
                //"/storage/$storage/$subpath",
                //"/storage/self/$storage/$subpath",
                //"/mnt/runtime/default/self/$storage/$subpath"
                //"/mnt/user/$user/$storage/$subpath",
                //"/mnt/user/$user/self/$storage/$subpath",
                //"/mnt/androidwritable/$user/self/$storage/$subpath",
            )
            possiblePaths.forEach { path ->
                val file = RootFile(path)
                if (isUseablePath(file)) {
                    Timber.i("found $key at $file")
                    storagePath[path] = file
                    return file
                }
            }
            return null
        }
    }

    fun getAndroidFolder(
        subPath: String,
        user: String = ShellCommands.currentProfile.toString(),
        isUseablePath: (file: RootFile?) -> Boolean = ::isWritablePath
    ): RootFile? {
        // only check access to Android folder and add subFolder even if it does not exist
        val key = "Android:$user:$subPath"
        return storagePath.getOrElse(key) {
            val baseKey = "Android:$user:"
            storagePath.getOrElse(baseKey) {
                val possiblePaths = listOf(
                    "/data/media/$user/Android",
                    "/mnt/pass_through/$user/emulated/$user/Android",
                    "/mnt/user/$user/emulated/$user/Android",
                )
                possiblePaths.forEach { path ->
                    val file = RootFile(path)
                    if (isUseablePath(file)) {
                        Timber.i("found $key at$file")
                        storagePath[baseKey] = file
                        val targetFile = RootFile(file, subPath)
                        storagePath[key] = targetFile
                        return targetFile
                    }
                }
                return null
            }?.let {
                val targetFile = RootFile(it, subPath)
                storagePath[key] = targetFile
                return targetFile
            }
            return null
        }
    }

    val msSinceBoot get() = SystemClock.elapsedRealtime()
    val now get() = System.currentTimeMillis()
}
