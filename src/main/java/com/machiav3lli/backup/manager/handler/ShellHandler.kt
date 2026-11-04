/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.manager.handler

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.plugins.InternalShellScriptPlugin
import com.machiav3lli.backup.data.preferences.traceDebug
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.splitCommand
import com.machiav3lli.backup.manager.handler.ShellHandler.FileInfo.Companion.utilBoxInfo
import com.machiav3lli.backup.ui.pages.baseInfo
import com.machiav3lli.backup.ui.pages.pref_libsuTimeout
import com.machiav3lli.backup.ui.pages.pref_libsuUseRootShell
import com.machiav3lli.backup.ui.pages.pref_suCommand
import com.machiav3lli.backup.utils.BUFFER_SIZE
import com.machiav3lli.backup.utils.FileUtils.translatePosixPermissionToMode
import com.machiav3lli.backup.utils.extensions.Android
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.io.SuRandomAccessFile
import de.voize.semver4k.Semver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


const val verKnown = "0.8.0 - 0.8.7"
const val verBugDotDotDirHang = "0.8.3 - 0.8.6"
const val verBugDotDotDirExtract = "0.8.0 - 0.8.0"  //TODO hg42 more versions?

class FakeShellResult(
    var aCode: Int,
    val aOut: MutableList<String> = mutableListOf(),
    val aErr: MutableList<String> = mutableListOf(),
) : Shell.Result() {
    override fun getOut(): MutableList<String> {
        return aOut
    }

    override fun getErr(): MutableList<String> {
        return aErr
    }

    override fun getCode(): Int {
        return aCode
    }
}

class ShellHandler {

    class UtilBox(
        var name: String = "",
        var version: String = "0.0.0",
        var reason: String = "",
        var score: Int = 0,
    ) {
        var bugs = mutableMapOf<String, Boolean>()

        var isKnownVersion = false
        val semver: Semver

        companion object {

            fun UtilBox.fatalBug(reason: String) {
                score = score.mod(1_00_00_00_0)
            }

            fun UtilBox.haveWorkaround(reason: String) {
            }

            val bugDetectors = mapOf<String, (String, UtilBox) -> Unit>(
                "DotDotDirExtract" to { name, box ->
                    box.apply {
                        //TODO hg42 should use a real "feature" test instead of version (but it hangs)
                        if (semver.satisfies(verBugDotDotDirExtract)) {
                            bugs[name] = true
                            haveWorkaround("tar only ignores ..dir on extraction")
                        }
                    }
                },
                "DotDotDirHang" to { name, box ->
                    box.apply {
                        //TODO hg42 should use a real "feature" test instead of version (but it hangs)
                        if (semver.satisfies(verBugDotDotDirHang)) {
                            bugs[name] = true
                            haveWorkaround("we ignore directories of this kind")
                        }
                    }
                },
                "LsLNum" to { name, box ->
                    box.apply {
                        try {
                            val line = runAsRoot("${quote()} ls -ld /").out.first() ?: ""
                            val (attr, n, user, group, tail) = line.split(" ", limit = 5)
                            if (user.isDigitsOnly() || group.isDigitsOnly()) {
                                bugs[name] = true
                                haveWorkaround("now works numerically")
                            }
                        } catch (e: Throwable) {
                            bugs["unSpec"] = true
                            fatalBug("failed 'ls -ld /'") //TODO hg42 fatal? when can this happen?
                            LogsHandler.unexpectedException(e)
                        }
                    }
                }
            )

        }

        init {
            version = version.replace(Regex("""^[vV]"""), "")
            version = version.replace(Regex("""-android$"""), "")
            semver = Semver(version, Semver.SemverType.NPM)
            if (semver.satisfies("=0.0.0")) {
                score = 0
            } else {
                score += 1_00_00_0 * (semver.major ?: 0)
                score += 1_00_0 * (semver.minor ?: 0)
                score += 1_0 * (semver.patch ?: 0)

                if (!name.contains("vendor")) {
                    score += 1
                }
                if (!name.contains("stock")) {
                    score += 1
                }
                if (name.contains("ext")) {
                    score += 2
                }
                if (name.contains("local")) {   // prefer this over all others
                    score += 100_00_00_00_0
                }

                score += 1_00_00_00_0

                if (semver.satisfies(verKnown)) {
                    isKnownVersion = true
                    score += 1_00_00_00_0
                }

                bugDetectors.forEach { it.value(it.key, this) }

                if (!hasBugs()) {
                    score += 10_00_00_00_0
                }
            }
        }

        fun quote() = quote(name)

        fun hasBugs() = bugs.isNotEmpty()
        fun hasBug(name: String) = bugs[name] != null
    }

    init {
        Shell.enableVerboseLogging = !NeoApp.isRelease
        initPrivilegedShell()

        runCatching {
            baseInfo().forEach { Timber.i(it) }
        }

        utilBoxes = mutableListOf<UtilBox>()
        try {
            UTILBOX_NAMES.forEach { box ->
                var boxVersion = ""
                val reWhiteSpace = Regex("""\s+""")
                try {
                    val shellResult = runAsRoot("$box --version")
                    if (shellResult.isSuccess) {
                        boxVersion = if (shellResult.out.isNotEmpty()) {
                            val fields = shellResult.out[0].split(reWhiteSpace, 3)
                            fields[1]
                        } else {
                            ""
                        }
                        Timber.d("utilBox OK: --------------> $box $boxVersion")
                        utilBoxes.add(UtilBox(name = box, version = boxVersion))
                    } else {
                        throw Exception() // goto catch
                    }
                } catch (e: Throwable) {
                    Timber.d("utilBox FAILED: ----------> $box --version")
                    try {
                        val shellResult = runAsRoot(box)
                        if (shellResult.isSuccess) {
                            boxVersion = if (shellResult.out.isNotEmpty()) {
                                val fields = shellResult.out[0].split(reWhiteSpace, 3)
                                fields[1]
                            } else {
                                ""
                            }
                            utilBoxes.add(UtilBox(name = box, version = boxVersion))
                        } else {
                            throw Exception("failed") // goto catch
                        }
                    } catch (e: Throwable) {
                        Timber.i("utilBox FAILED: ----------> $box")
                        utilBoxes.add(UtilBox(name = box, reason = LogsHandler.message(e)))
                    }
                }
            }
        } catch (e: Throwable) {
            LogsHandler.unexpectedException(e, "utilBox detection failed miserable")
        }
        NeoApp.lastErrorCommands.clear()  // ignore fails while searching for utilBox

        utilBoxes.sortByDescending { it.score }

        utilBoxInfo().forEach { Timber.i(it) }

        utilBox = utilBoxes.first()
        if (utilBox.score <= 1) {
            Timber.e("No good utilbox found (using $utilBox)")
            //throw UtilboxNotAvailableException(message)
        }
    }

    @Throws(ShellCommandFailedException::class, UnexpectedCommandResult::class)
    fun suGetFileInfo(
        path: String,
        parent: String? = null,
        utilBoxQ: String = ShellHandler.utilBoxQ,
    ): FileInfo {
        val shellResult = runAsRoot("$utilBoxQ ls -bdAll ${quote(path)}")
        val relativeParent = parent ?: ""
        val result = shellResult.out.asSequence()
            .filter { it.isNotEmpty() }
            .filter { !it.startsWith("total") }
            .mapNotNull { FileInfo.fromLsOutput(it, relativeParent, File(path).parent!!) }
            .toList()
        if (result.isEmpty())
            throw UnexpectedCommandResult("cannot get file info for '$path'", shellResult)
        if (result.size > 1)
            Timber.w("more than one file found for '$path', taking the first", shellResult)
        return result[0]
    }

    @Throws(ShellCommandFailedException::class, UnexpectedCommandResult::class)
    fun suGetFileInfo(file: File): FileInfo {
        return suGetFileInfo(file.absolutePath, file.parent)
    }

    @Throws(ShellCommandFailedException::class)
    fun suGetDirectoryContents(path: File): Array<String> {
        val shellResult = runAsRoot("$utilBoxQ ls -bA1 ${quote(path)}")
        return shellResult.out.map { FileInfo.unescapeLsOutput(it) }.toTypedArray()
    }

    @Throws(ShellCommandFailedException::class)
    fun suGetDetailedDirectoryContents(
        path: String,
        recursive: Boolean,
        parent: String? = null,
    ): List<FileInfo> {
        val shellResult =
            if (recursive)
                runAsRoot("$utilBoxQ find ${quote(path)} -print0 | $utilBoxQ xargs -0 ls -bdAll")
            else
                runAsRoot("$utilBoxQ ls -bAll ${quote(path)}")
        val relativeParent = parent ?: ""
        return shellResult.out
            .filter { it.isNotEmpty() }
            .filter { !it.startsWith("total") }
            .mapNotNull { FileInfo.fromLsOutput(it, null, path) }
            .toMutableList()
    }

    /**
     * Uses superuser permissions to retrieve uid, gid and SELinux context of any given directory.
     *
     * @param filepath the filepath to retrieve the information from
     * @return an array with 3 fields: {uid, gid, context}
     */
    @Throws(ShellCommandFailedException::class, UnexpectedCommandResult::class)
    fun suGetOwnerGroupContext(filepath: String): Array<String> {
        // val command = "$utilBoxQuoted stat -c '%u %g %C' ${quote(filepath)}" // %C usually not supported in toybox
        // ls -Z supported as an option since landley/toybox 0.6.0 mid 2015, Android 8 starts mid 2017
        // use -dlZ instead of -dnZ, because -nZ was found (by Kostas!) with an error (with no space between group and context)
        // apparently uid/gid is less tested than names
        var shellResult: Shell.Result? = null
        val commandWithContext = "$utilBoxQ ls -bdAlZ ${quote(filepath)}"
        try {
            shellResult = runAsRoot(commandWithContext)
            if (shellResult.out.isEmpty()) throw UnexpectedCommandResult(
                "'$commandWithContext' returned no output for '$filepath'",
                shellResult,
            )

            val validLines = shellResult.out.filter {
                it.isNotEmpty() && !it.startsWith("total")
            }
            if (validLines.isEmpty()) throw UnexpectedCommandResult(
                "'$commandWithContext' returned no valid output for '$filepath'",
                shellResult,
            )

            // Expected format: drwxrwx--x 5 owner group context filepath
            // Indices:         0         1 2     3     4       5+
            val parts = validLines[0].split(Regex("""\s+"""))
            if (parts.size < 5) {
                Timber.w(
                    "Unexpected ls -bdAlZ output format for '$filepath': ${validLines[0]}"
                )
                throw Exception("Insufficient fields in ls output")
            }

            return parts.slice(2..4).toTypedArray()
        } catch (e: Throwable) {
            Timber.w(
                "Failed to get owner/group/context with ls -bdAlZ for '$filepath', trying fallback: ${e.message}"
            )

            // Fallback 1: without SELinux context (ls -bdAl)
            try {
                val commandSansContext = "$utilBoxQ ls -bdAl ${quote(filepath)}"
                shellResult = runAsRoot(commandSansContext)

                val validLines = shellResult.out.filter {
                    it.isNotEmpty() && !it.startsWith("total")
                }

                if (validLines.isEmpty()) {
                    throw Exception("No valid output from ls -bdAl")
                }

                val parts = validLines[0].split(Regex("""\s+"""))

                if (parts.size < 4) {
                    throw Exception("Insufficient fields in ls -bdAl output")
                }

                // Return with empty context since -Z didn't work
                Timber.i("Using fallback without SELinux context for '$filepath'")
                return arrayOf(parts[2], parts[3], "")

            } catch (e2: Throwable) {
                Timber.w("Fallback ls -bdAl also failed for '$filepath': ${e2.message}")
            }

            // Fallback 2: with stat command
            try {
                val statCommand = "$utilBoxQ stat -c '%u %g %C' ${quote(filepath)}"
                shellResult = runAsRoot(statCommand)

                if (shellResult.out.isNotEmpty()) {
                    val parts = shellResult.out[0].split(" ", limit = 3)
                    if (parts.size >= 2) {
                        Timber.i("Using stat command fallback for '$filepath'")
                        val context = if (parts.size >= 3) parts[2] else ""
                        return arrayOf(parts[0], parts[1], context)
                    }
                }
            } catch (e3: Throwable) {
                Timber.w("Fallback stat command also failed for '$filepath': ${e3.message}")
            }

            throw UnexpectedCommandResult(
                "'$commandWithContext' and all fallbacks failed for '$filepath'",
                shellResult,
            )
        }
    }

    class ShellCommandFailedException(
        @field:Transient val shellResult: Shell.Result,
        val command: String,
        cause: Throwable? = null,
    ) : Exception(cause)

    class UnexpectedCommandResult(message: String, val shellResult: Shell.Result?) :
        Exception(message)

    enum class FileType {
        REGULAR_FILE, BLOCK_DEVICE, CHAR_DEVICE, DIRECTORY, SYMBOLIC_LINK, NAMED_PIPE, SOCKET
    }

    class FileInfo(
        /**
         * Returns the filepath, relative to the original location
         *
         * @return relative filepath
         */
        val filePath: String,
        val fileType: FileType,
        val absoluteParent: String,
        val owner: String,
        val group: String,
        var fileMode: Int,
        var fileSize: Long,
        var fileModTime: Date,
    ) {
        val absolutePath: String = "$absoluteParent/$filePath"

        //val fileMode = fileMode
        //val fileSize = fileSize
        //val fileModTime = fileModTime
        var linkName: String? = null
            private set
        val filename: String
            get() = File(filePath).name

        override fun toString(): String {
            return "FileInfo{" +
                    "filePath='" + filePath + "'" +
                    ", fileType=" + fileType +
                    ", owner=" + owner +
                    ", group=" + group +
                    ", fileMode=" + fileMode.toString(8) +
                    ", fileSize=" + fileSize +
                    ", fileModTime=" + fileModTime +
                    ", absolutePath='" + absolutePath + "'" +
                    ", linkName='" + linkName + "'" +
                    "}"
        }

        companion object {
            private val PATTERN_LINKSPLIT = Regex(" -> ") //Pattern.compile(" -> ")
            private val FALLBACK_MODE_FOR_DIR = translatePosixPermissionToMode("rwxrwx--x")
            private val FALLBACK_MODE_FOR_FILE = translatePosixPermissionToMode("rw-rw----")
            private val FALLBACK_MODE_FOR_CACHE = translatePosixPermissionToMode("rwxrws--x")

            fun utilBoxInfo(): List<String> {
                return utilBoxes.map { box ->
                    "${box.name}: ${
                        if (box.version.isNotEmpty() and (box.version != "0.0.0"))
                            "${box.version}${
                                if (utilBox.isKnownVersion) "" else " (unknown)"
                            } -> ${box.score}${
                                if (box.hasBugs())
                                    " bugs: " + box.bugs.keys.joinToString(",")
                                else
                                    " good, no known bugs"
                            }"
                        else
                            box.reason
                    }${
                        if (box.name == utilBox.name) " (used)" else ""
                    }"
                }
            }

            /*  from toybox ls.c

                  for (i = 0; i<len; i++) {
                    *to++ = '\\';
                    if (strchr(TT.escmore, from[i])) *to++ = from[i];
                    else if (-1 != (j = stridx("\\\a\b\033\f\n\r\t\v", from[i])))
                      *to++ = "\\abefnrtv"[j];
                    else to += sprintf(to, "%03o", from[i]);
                  }
            */

            fun unescapeLsOutput(str: String): String {
                val is_escaped = Regex("""\\([\\abefnrtv ]|\d\d\d)""")
                return str.replace(
                    is_escaped
                ) { match: MatchResult ->
                    val matched =
                        match.groups[1]?.value ?: "?" // "?" cannot happen because it matched
                    when (matched) {
                        """\""" -> """\"""
                        "a"     -> "\u0007"
                        "b"     -> "\u0008"
                        "e"     -> "\u001b"
                        "f"     -> "\u000c"
                        "n"     -> "\u000a"
                        "r"     -> "\u000d"
                        "t"     -> "\u0009"
                        "v"     -> "\u000b"
                        " "     -> " "
                        else    -> (((matched[0].digitToInt() * 8) + matched[1].digitToInt()) * 8 + matched[2].digitToInt()).toChar()
                            .toString()
                    }
                }
            }

            /**
             * Create an instance of FileInfo from a line of ls output
             *
             * @param lsLine single output line from ls -bAll
             * @return an instance of FileInfo
             */
            fun fromLsOutput(
                lsLine: String,
                parentPath: String?,
                absoluteParent: String,
            ): FileInfo? {
                var parent = absoluteParent
                // Expecting something like this (with whitespace) from
                // ls -bAll /data/data/com.shazam.android/
                // field   0     1    2       3            4       5            6             7     8
                //   "drwxrwx--x 5 u0_a441 u0_a441       4096 2021-10-19 01:54:32.029625295 +0200 files"
                // links have 2 additional fields:
                //   "lrwxrwxrwx 1 root    root            61 2021-08-25 16:44:49.757000571 +0200 lib -> /data/app/com.shazam.android-I4tzgPt3Ay6mFgz4Jnb4dg==/lib/arm"
                // [0] Filemode, [1] number of directories/links inside, [2] owner [3] group [4] size
                // [5] mdate, [6] mtime, [7] mtimezone, [8] filename
                //var absoluteParent = absoluteParent
                //val tokens = lsLine.split(Regex("""\s+"""), 9).toTypedArray()
                //  val regex = Pattern.compile(
                //      """(?x)
                //          |^
                //          |(?<mode>\S+)
                //          |\s+
                //          |(?<links>\d+)
                //          |\s+
                //          |(?<owner>\S+)
                //          |\s+
                //          |(?<group>\S+)
                //          |\s+
                //          |(?<size>\d+)
                //          |\s+
                //          |(?<mdatetime>
                //          |  (?<mdate>\S+)
                //          |  \s+
                //          |  (?<mtime>\S+)(?:\.\S+)       # ignore nanoseconds part
                //          |  (\s+                         # optional
                //          |     (?<mzone>[+-]\d\d\d\d)    # old toybox on api 26 doesn't have this
                //          |  )?                           # (no -ll option or --full-time)
                //          |)
                //          |\s+
                //          |(?<name>.*)
                //          |$
                //          |""".trimMargin()
                //  )
                //  val match = regex.matcher(lsLine)
                //  match.find()
                //  var filePath: String?
                //  val modeFlags = match.group("mode") ?: return null
                //  val owner = match.group("owner") ?: return null
                //  val group = match.group("group") ?: return null
                //  val size = match.group("size") ?: return null
                //  val mdatetime = match.group("mdatetime") ?: return null
                //  val mdate = match.group("mdate") ?: return null
                //  val mtime = match.group("mtime") ?: return null
                //  val mzone = match.group("mzone")
                //  var name = match.group("name") ?: return null
                val regex = Regex(
                    """(?x)
                        |^
                        |(\S+)                       # 1 mode
                        |\s+
                        |(\d+)                       # 2 links
                        |\s+
                        |(\S+)                       # 3 owner
                        |\s+
                        |(\S+)                       # 4 group
                        |\s+
                        |(\d+)                       # 5 size
                        |\s+
                        |(                           # 6 mdatetime
                        |  ([\d-]+)                  # 7 mdate
                        |  \s+
                        |  ([\d:]+)(\.\d+)?          # 8 mtime  9 nanoseconds opt 
                        |  (\s+                      # 10 opt
                        |     ([+-]\d+)              # 11 mzone # toybox on api 26 doesn't have this TODO hg42 test 27-30
                        |  )?                                   # (no -ll option or --full-time)
                        |)
                        |\s+
                        |(.*)                        # 12 longname
                        |$
                        |""".trimMargin()
                )
                val match = regex.matchEntire(lsLine)
                    ?: throw Exception("ls output does not match expectations (regex)")
                val modeFlags = match.groupValues[1]
                val owner = match.groupValues[3]
                val group = match.groupValues[4]
                val size = match.groupValues[5]
                //val mdatetime   = match.groupValues[6]
                val mdate = match.groupValues[7]
                val mtime = match.groupValues[8]
                val mzone = match.groupValues[11]
                var name = match.groupValues[12]
                val fileModTime =
                    if (mzone.isEmpty())
                    // 2020-11-26 04:35
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .parse("$mdate $mtime")
                    else
                    // 2020-11-26 04:35:21(.543772855) +0100
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault())
                            .parse("$mdate $mtime $mzone")
                // If ls was executed with a file as parameter, the full path is echoed. This is not
                // good for processing. Removing the absolute parent and setting the parent to be the parent
                // and not the file itself (hg42: huh? a parent should be a parent and never the file itself)
                if (name.startsWith(parent)) {
                    if (name == parent) {    // don't break the case the file is it own parent :-( in case it is used
                        Timber.e("the file '$name' is it's own parent, but should not")
                        parent = File(parent).parent!!
                    }
                    name = name.substring(parent.length + 1)
                }
                val fileName = unescapeLsOutput(name)
                var filePath =
                    if (parentPath.isNullOrEmpty()) {
                        fileName
                    } else {
                        "${parentPath}/${fileName}"
                    }
                var fileMode = FALLBACK_MODE_FOR_FILE
                try {
                    fileMode = translatePosixPermissionToMode(modeFlags.substring(1))
                } catch (e: IllegalArgumentException) {
                    // Happens on cache and code_cache dir because of sticky bits
                    // drwxrws--x 2 u0_a108 u0_a108_cache 4096 2020-09-22 17:36 cache
                    // drwxrws--x 2 u0_a108 u0_a108_cache 4096 2020-09-22 17:36 code_cache
                    // These will be filtered out later, so don't print a warning here
                    // Downside: For all other directories with these names, the warning is also hidden
                    // This can be problematic for system packages, but for apps these bits do not
                    // make any sense.
                    if (filePath == "cache" || filePath == "code_cache") {
                        // Fall back to the known value of these directories
                        fileMode = FALLBACK_MODE_FOR_CACHE
                    } else {
                        fileMode =
                            if (modeFlags[0] == 'd') {
                                FALLBACK_MODE_FOR_DIR
                            } else {
                                FALLBACK_MODE_FOR_FILE
                            }
                        Timber.w(
                            String.format(
                                "Found a file with special mode (%s), which is not processable. Falling back to %s. filepath=%s ; absoluteParent=%s",
                                modeFlags, fileMode, filePath, parent
                            )
                        )
                    }
                } catch (e: Throwable) {
                    LogsHandler.unexpectedException(e, filePath)
                }
                var linkName: String? = null
                var fileSize: Long = 0
                val type: FileType
                when (modeFlags[0]) {
                    'd'  -> type = FileType.DIRECTORY
                    'l'  -> {
                        type = FileType.SYMBOLIC_LINK
                        val nameAndLink = PATTERN_LINKSPLIT.split(filePath as CharSequence)
                        //TODO hg42 what if PATTERN_LINK_SPLIT is part of a file or link path? (should be pretty rare)
                        //TODO hg42 in this case we get more parts and we could check which part combinations exist
                        if (nameAndLink.size > 2)
                            Timber.e("we got more than one 'link arrow' in '$filePath'")
                        filePath = nameAndLink[0]
                        linkName = nameAndLink[1]
                    }

                    'p'  -> type = FileType.NAMED_PIPE
                    's'  -> type = FileType.SOCKET
                    'b'  -> type = FileType.BLOCK_DEVICE
                    'c'  -> type = FileType.CHAR_DEVICE
                    else -> {
                        type = FileType.REGULAR_FILE
                        fileSize = size.toLong()
                    }
                }
                val result = FileInfo(
                    filePath,
                    type,
                    parent,
                    owner,
                    group,
                    fileMode,
                    fileSize,
                    fileModTime!!
                )
                result.linkName = linkName
                return result
            }

            fun fromLsOutput(lsLine: String, absoluteParent: String): FileInfo? {
                return fromLsOutput(lsLine, "", absoluteParent)
            }
        }
    }

    companion object {

        //private val UTILBOX_NAMES = listOf("busybox", "/sbin/.magisk/busybox/busybox", "toybox")
        private val UTILBOX_NAMES = listOf(
            // only toybox will work currently
            //"busybox",
            //"/sbin/.magisk/busybox/busybox"
            "/data/local/toybox",
            "toybox-ext",
            "toybox_vendor",
            "toybox-stock",
            "toybox",
        )

        var utilBoxes = mutableListOf<UtilBox>()
        var utilBox: UtilBox = UtilBox()
        val utilBoxQ get() = utilBox.quote()

        val libsuUsesRootShell get() = Shell.getShell().status >= Shell.ROOT_SHELL

        val hasPmBypassLowTargetSDKBlock: Boolean by lazy {
            runCatching {
                // the option is not included in usage output, instead check,
                // if the error message is complaining about the missing file
                // and not about that option
                ShellUtils.fastCmdResult(
                    "pm install --bypass-low-target-sdk-block not-existing.apk 2>&1 | grep not-existing.apk"
                )
                        &&
                        !ShellUtils.fastCmdResult(
                            "pm install --bypass-low-target-sdk-block not-existing.apk 2>&1 | grep bypass-low-target-sdk-block"
                        )
            }.getOrNull() ?: false
        }

        fun sysInfo() =
            listOf(
                "system       = Android ${Android.name} - ${ShellUtils.fastCmd("uname -r -m -o")}",
                //"has nsenter  = $hasNsEnter", //TODO wech
                "has bypass-low-target-sdk-block = $hasPmBypassLowTargetSDKBlock",
            )

        fun suInfo() =
            listOf(
                "is like root                   = $isLikeRoot",
                "libsu shell status             = ${Shell.getShell().status} = ${
                    when (Shell.getShell().status) {
                        Shell.UNKNOWN        -> "UNKNOWN"
                        Shell.NON_ROOT_SHELL -> "NON_ROOT_SHELL"
                        Shell.ROOT_SHELL     -> "ROOT_SHELL"
                        //Shell.ROOT_MOUNT_MASTER -> "ROOT_MOUNT_MASTER"     //TODO wech
                        else                 -> "unknown code ${Shell.getShell().status}"
                    }
                }",
                "libsu uses root shell          = $libsuUsesRootShell",               //TODO wech (never becasue initialized as non-root)
                "su command run to gain root    = $suCommand",
            )

        var suCommand by mutableStateOf("")

        val profileId: String get() = ShellCommands.currentProfile.toString()

        fun splitCommand(command: String): List<String> {
            val result = mutableListOf<String>()
            var current = StringBuilder()
            var inDoubleQuotes = false
            var inSingleQuotes = false
            var escapeNext = false

            for (char in command) {
                when {
                    escapeNext          -> {
                        current.append(char)
                        escapeNext = false
                    }

                    char == '\\'        -> {
                        if (inDoubleQuotes) {
                            escapeNext = true
                        } else {
                            current.append(char)
                        }
                    }

                    char == '"'         -> {
                        if (inSingleQuotes) {
                            current.append(char)
                        } else {
                            inDoubleQuotes = !inDoubleQuotes
                            if (!inDoubleQuotes) {
                                result.add(current.toString())
                                current = StringBuilder()
                            }
                        }
                    }

                    char == '\''        -> {
                        if (inDoubleQuotes) {
                            current.append(char)
                        } else {
                            inSingleQuotes = !inSingleQuotes
                            if (!inSingleQuotes) {
                                result.add(current.toString())
                                current = StringBuilder()
                            }
                        }
                    }

                    char.isWhitespace() -> {
                        if (inDoubleQuotes || inSingleQuotes) {
                            current.append(char)
                        } else {
                            if (current.isNotEmpty()) {
                                result.add(current.toString())
                                current = StringBuilder()
                            }
                        }
                    }

                    else                -> {
                        current.append(char)
                    }
                }
            }

            if (current.isNotEmpty()) {
                result.add(current.toString())
            }

            return result
        }

        private val checkRootScript
            get() = InternalShellScriptPlugin.findScript("checkroot").toString()

        fun checkRootEquivalent(): Boolean {
            val result = runAsRoot("sh '$checkRootScript'", throwFail = false)
            if (!result.isSuccess) {
                Timber.w(
                    "checkroot failed with code ${result.code}:\n" +
                            "stdout: ${result.out.joinToString("\n")}\n" +
                            "stderr: ${result.err.joinToString("\n")}"
                )
            }
            return result.isSuccess
        }

        private var checkedCommand: String = ""

        var isLikeRoot: Boolean? = null
            get() {
                // invalidate cached field if suCommand changes
                if (field == null || suCommand != checkedCommand) {
                    field = checkRootEquivalent()
                    Timber.i("suCommand = $suCommand => ${if (field == true) "IS like root" else "is NOT like root"}")
                    checkedCommand = suCommand
                }
                return field ?: false
            }

        class ShellInit : Shell.Initializer() {
            override fun onInit(context: Context, shell: Shell): Boolean {
                var command = if (suCommand == "") "su" else suCommand
                var result = shell.newJob()
                    .add(command)
                    .to(mutableListOf<String>(), mutableListOf<String>())
                    .exec()
                if (result.isSuccess) {
                    Timber.i("suCommand = $suCommand => ok")
                    return true
                }
                Timber.w(
                    "ShellInit failed with code ${result.code}: ${
                        if (result.out.isNotEmpty())
                            "\nstdout:\n${result.out.joinToString("\n") { "> $it" }}"
                        else
                            ""
                    }${
                        if (result.err.isNotEmpty())
                            "\nstderr:\n${result.err.joinToString("\n") { "! $it" }}"
                        else
                            ""
                    }"
                )
                // fallback
                command = "sh"
                result = shell.newJob()
                    .add(command)
                    .to(mutableListOf<String>(), mutableListOf<String>())
                    .exec()
                if (result.isSuccess) {
                    Timber.i("fallback shell '$command' => ok")
                    return true
                }
                return false
            }
        }

        private fun initLibSU() {
            val builder = Shell.Builder.create()
                .setTimeout(pref_libsuTimeout.value.toLong())
                .setInitializers(ShellInit::class.java)
            if (!pref_libsuUseRootShell.value)
            // we add our own suCommand to elevate privileges, so start with a simple shell
                builder.setFlags(Shell.FLAG_NON_ROOT_SHELL)
            // could be used instead of toybox, but busybox does not provide all we need
            //.setInitializers(BusyBoxInstaller::class.java)
            Shell.setDefaultBuilder(builder)
        }

        private fun tryGainAccessCommand(): Boolean {
            try {
                needFreshShell()
                if (checkRootEquivalent())
                    return true
            } catch (e: Throwable) {
                logException(e, what = "suCommand: $suCommand")
            }
            releaseShell(trace = false)
            return false
        }

        fun validateSuCommand(command: String): Boolean {
            try {
                Timber.i("----- validateSuCommand: $command")
                suCommand = command
                if (tryGainAccessCommand()) {
                    return true
                }
                Timber.w("validateSuCommand failed for: $command")
            } catch (e: Throwable) {
                logException(e, what = "suCommand: $suCommand")
            }
            return false
        }

        fun findSuCommand(command: String? = null): String {
            for (tryCommand in buildList {
                if (!command.isNullOrEmpty()) add(command)
                if (!Android.minSDK((Build.VERSION_CODES.Q))) {
                    add("su")
                    add("/system/bin/su")
                    add("sh")
                }
                add("su -c 'nsenter --mount=/proc/1/ns/mnt sh'")
                add("su --mount-master")
                add("sh -c \"exec \$(which su kp) -c 'nsenter --mount=/proc/1/ns/mnt sh'\"")  // APatch workaround
                //"sh -c \"(echo 'nsenter --mount=/proc/1/ns/mnt sh'; cat) | \$(which su kp)\"",  // TODO to be fixed: APatch workaround
                if (Android.minSDK((Build.VERSION_CODES.Q))) {
                    add("su")
                    add("/system/bin/su")
                    add("sh")
                }
            }) {
                if (validateSuCommand(tryCommand))
                    return suCommand
            }
            // fallback to simple libsu
            suCommand = ""
            return suCommand
        }

        fun initPrivilegedShell() {
            initLibSU()
            findSuCommand(pref_suCommand.value)
        }

        private fun releaseShell(trace: Boolean = true): Shell? {
            return try {
                val shell = Shell.getCachedShell()
                if (shell != null
                //&& shell.isAlive
                ) {
                    traceDebug { "previous cached shell found, trying to terminate it ($shell)" }
                    shell.waitAndClose(0L, TimeUnit.SECONDS)
                }
                return shell
            } catch (e: Throwable) {
                logException(e, what = "releaseShell")
                null
            }
        }

        fun needFreshShell() {
            try {
                val shellBefore = releaseShell()
                val shellAfter = Shell.getShell()
                if (shellBefore != null) {
                    if (shellAfter === shellBefore)
                        Timber.e("ERROR: shell not refreshed! ($shellBefore vs $shellAfter)")
                    else
                        traceDebug { "new shell created ($shellAfter)" }
                }
            } catch (e: Throwable) {
                logException(e, what = "needFreshShell")
            }
        }

        @Throws(ShellCommandFailedException::class)
        private fun runShellCommand(
            shell: Shell,
            command: String,
            throwFail: Boolean = true,
        ): Shell.Result {
            // defining stdout and stderr on our own
            // otherwise we would have to set the flag redirect stderr to stdout:
            // Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
            // stderr is used for logging, so it's better not to call an application that does that
            // and keeps quiet
            var result: Shell.Result = FakeShellResult(-1)
            try {
                Timber.d("Running Command: $command")
                val stdout: List<String> = arrayListOf()
                val stderr: List<String> = arrayListOf()
                result = Shell.cmd(command).to(stdout, stderr).exec()
                Timber.d("Command(s) $command ended with ${result.code}")
                if (!result.isSuccess) {
                    NeoApp.addErrorCommand(command)
                    if (throwFail)
                        throw ShellCommandFailedException(result, command)
                }
            } catch (e: Throwable) {
                NeoApp.addErrorCommand(command)
                if (throwFail)
                    throw ShellCommandFailedException(result, command = command, cause = e)
            }
            return result
        }

        @Throws(ShellCommandFailedException::class)
        fun runAsRoot(command: String, throwFail: Boolean = true): Shell.Result {
            return runShellCommand(Shell.getShell(), command, throwFail)
        }

        fun runAsRootPipeInCollectErr(
            inStream: InputStream,
            command: String,
        ): Pair<Int, String> {
            Timber.i("SHELL: $command")

            return runBlocking(Dispatchers.IO) {

                runCatching {

                    val process = Runtime.getRuntime().exec(splitCommand(suCommand).toTypedArray())

                    val shellIn = process.outputStream
                    //val shellOut = process.inputStream
                    val shellErr = process.errorStream

                    val errAsync = async(Dispatchers.IO) {
                        shellErr.readBytes().decodeToString()
                    }

                    shellIn.write("$command\n".encodeToByteArray())

                    inStream.copyTo(shellIn, 65536)
                    shellIn.close()

                    val err = errAsync.await()
                    withContext(Dispatchers.IO) { process.waitFor(10, TimeUnit.SECONDS) }
                    if (process.isAlive)
                        process.destroyForcibly()
                    val code = process.exitValue()

                    if (code != 0)
                        NeoApp.addErrorCommand(command)

                    (code to err)
                }.getOrElse {
                    (-1 to (it.message ?: "unknown error"))
                }
            }
        }

        fun runAsRootPipeOutCollectErr(
            outStream: OutputStream,
            command: String,
        ): Pair<Int, String> {
            Timber.i("SHELL: $command")

            return runBlocking(Dispatchers.IO) {

                runCatching {

                    val process = Runtime.getRuntime().exec(splitCommand(suCommand).toTypedArray())

                    val shellIn = process.outputStream
                    val shellOut = process.inputStream
                    val shellErr = process.errorStream

                    val errAsync = async(Dispatchers.IO) {
                        shellErr.readBytes().decodeToString()
                    }

                    shellIn.write(command.encodeToByteArray())
                    shellIn.close()

                    shellOut.copyTo(outStream, 65536)
                    outStream.flush()

                    val err = errAsync.await()
                    withContext(Dispatchers.IO) {
                        process.waitFor(10, TimeUnit.SECONDS)
                    }
                    if (process.isAlive)
                        process.destroyForcibly()
                    val code = process.exitValue()
                    if (code != 0)
                        NeoApp.addErrorCommand(command)

                    (code to err)
                }.getOrElse {
                    (-1 to (it.message ?: "unknown error"))
                }
            }
        }

        // the Android command line shell is mksh
        // mksh quoting
        //   '...'  single quotes would do well, but single quotes cannot be used
        //   $'...' dollar + single quotes need many escapes
        //   "..."  needs only a few escaped chars (backslash, dollar, double quote, back tick)
        //   from mksh man page:
        //      double quote quotes all characters,
        //          except '$' , '`' and '\' ,
        //          up to the next unquoted double quote
        private val charactersToBeEscaped =
            Regex("""[\\${'$'}"`]""")   // blacklist, only escape those that are necessary

        fun quote(parameter: String): String {
            return "\"${parameter.replace(charactersToBeEscaped) { "\\${it.value}" }}\""
            //return "\"${parameter.replace(charactersToBeEscaped) { "\\${(0xFF and it.value[0].code).toString(8).padStart(3, '0')}" }}\""
        }

        fun quote(parameter: File): String {
            return quote(parameter.absolutePath)
        }

        fun quoteMultiple(parameters: Collection<String>): String =
            parameters.joinToString(" ", transform = ::quote)

        fun isFileNotFoundException(ex: ShellCommandFailedException): Boolean {
            val err = ex.shellResult.err
            return err.isNotEmpty() && err[0].contains("no such file or directory", true)
        }

        @Throws(IOException::class)
        fun quirkLibsuReadFileWorkaround(inputFile: FileInfo, output: OutputStream) {
            quirkLibsuReadFileWorkaround(inputFile.absolutePath, inputFile.fileSize, output)
        }

        @Throws(IOException::class)
        fun quirkLibsuReadFileWorkaround(filepath: String, filesize: Long, output: OutputStream) {
            val maxRetries: Short = 10
            var stream = SuRandomAccessFile.open(filepath, "r")
            val buf = ByteArray(BUFFER_SIZE)
            var readOverall: Long = 0
            var retriesLeft = maxRetries.toInt()
            while (true) {
                val read = stream.read(buf)
                if (0 > read && filesize > readOverall) {
                    // For some reason, SuFileInputStream throws eof much to early on slightly bigger files
                    // This workaround detects the unfinished file like the tar archive does (it tracks
                    // the written amount of bytes, too because it needs to match the header)
                    // As side effect the archives slightly differ in size because of the flushing mechanism.
                    if (0 >= retriesLeft) {
                        Timber.e(
                            String.format(
                                Locale.ENGLISH,
                                "Could not recover after %d tries. Seems like there is a bigger issue. Maybe the file has changed?",
                                maxRetries
                            )
                        )
                        throw IOException(
                            String.format(
                                "Could not read expected amount of input bytes %d; stopped after %d tries at %d",
                                filesize, maxRetries, readOverall
                            )
                        )
                    }
                    Timber.w(
                        String.format(
                            Locale.ENGLISH,
                            "SuFileInputStream EOF before expected after %d bytes (%d are missing). Trying to recover. %d retries lef",
                            readOverall, filesize - readOverall, retriesLeft
                        )
                    )
                    // Reopen the file to reset eof flag
                    stream.close()
                    stream = SuRandomAccessFile.open(filepath, "r")
                    stream.seek(readOverall)
                    // Reduce the retries
                    retriesLeft--
                    continue
                }
                if (0 > read) {
                    break
                }
                output.write(buf, 0, read)
                readOverall += read.toLong()
                // successful write, resetting retries
                retriesLeft = maxRetries.toInt()
            }
        }

        fun findUserOverridableFile(subDirName: String, fileName: String): File? {
            var found: File?
            val userDir = File(
                NeoApp.activity?.getExternalFilesDir(null),
                subDirName
            )
            userDir.mkdirs()
            found = File(userDir, fileName)
            if (found?.isFile != true) {
                val assetDir = File(NeoApp.assets.directory, subDirName)
                found = File(assetDir, fileName)
            }
            return found
        }
    }
}


@Test
fun test_splitCommand() {
    val command =
        """echo "Hello\ World!\n" 'This is a test with a backslash->\' \\\\escaped\\ space"""
    val cmdArray = splitCommand(command)
    for (arg in cmdArray) {
        println("'$arg'")
    }
}

