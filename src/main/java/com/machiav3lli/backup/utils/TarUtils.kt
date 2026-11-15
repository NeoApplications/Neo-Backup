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
package com.machiav3lli.backup.utils

import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.data.entity.RootFile
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRoot
import com.machiav3lli.backup.manager.handler.ShellHandler.FileType
import com.machiav3lli.backup.ui.pages.pref_strictHardLinks
import com.topjohnwu.superuser.io.SuFileOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Objects

const val BUFFER_SIZE = 8 * 1024 * 1024

// octal

// #define	__S_IFDIR	0040000	// Directory
// #define	__S_IFCHR	0020000	// Character device
// #define	__S_IFBLK	0060000	// Block device
// #define	__S_IFREG	0100000	// Regular file
// #define	__S_IFIFO	0010000	// FIFO
// #define	__S_IFLNK	0120000	// Symbolic link
// #define	__S_IFSOCK	0140000	// Socket

// #define	__S_ISUID	04000	// Set user ID on execution
// #define	__S_ISGID	02000	// Set group ID on execution
// #define	__S_ISVTX	01000	// Save swapped text after use (sticky)
// #define	__S_IREAD	0400	// Read by owner
// #define	__S_IWRITE	0200	// Write by owner
// #define	__S_IEXEC	0100	// Execute by owner

// NOTE: underscores separate octal digits not hex digits!
private const val DIR_MODE_OR_MASK = 0b0_000_100_000_000_000_000
private const val FILE_MODE_OR_MASK = 0b0_001_000_000_000_000_000
private const val FIFO_MODE_OR_MASK = 0b0_000_001_000_000_000_000
private const val SYMLINK_MODE_OR_MASK = 0b0_001_010_000_000_000_000

/**
 * Adds a filepath to the given archive.
 * If it's a directory, it'll be added cursively
 *
 * @param inputFilepath the filepath to add to the archive
 * @param parent        the parent directory in the archive, use "" to add it to the root directory
 * @throws IOException on IO related errors such as out of disk space or missing files
 */
@Throws(IOException::class)
fun TarArchiveOutputStream.addFilepath(inputFilepath: File, parent: String) {
    val entryName = parent + inputFilepath.name
    val archiveEntry = TarArchiveEntry(inputFilepath, entryName)
    // Interject for symlinks
    if (FileUtils.isSymlink(inputFilepath)) {
        archiveEntry.linkName = inputFilepath.canonicalPath
    }
    putArchiveEntry(archiveEntry)
    when {
        inputFilepath.isFile &&
                !FileUtils.isSymlink(inputFilepath) -> {
            val bis = BufferedInputStream(FileInputStream(inputFilepath))
            IOUtils.copy(bis, this)
        }

        inputFilepath.isDirectory                   -> {
            closeArchiveEntry()
            Objects
                .requireNonNull(inputFilepath.listFiles(), "Directory listing returned null!")
                .forEach {
                    addFilepath(it, entryName + File.separator)
                }
        }

        else /* symlink etc. */                     -> {
            closeArchiveEntry()
        }
    }
}

@Throws(IOException::class)
fun TarArchiveOutputStream.suAddFiles(allFiles: List<ShellHandler.FileInfo>) {
    for (file in allFiles) {
        Timber.d("Adding ${file.filePath} to archive (filesize: ${file.fileSize})")
        var entry: TarArchiveEntry
        when (file.fileType) {
            FileType.REGULAR_FILE  -> {
                Timber.d("Adding regular file ${file.filePath} to archive (filesize: ${file.fileSize})")
                entry = TarArchiveEntry(file.filePath)
                entry.size = file.fileSize
                entry.setNames(file.owner, file.group)
                entry.mode = FILE_MODE_OR_MASK or file.fileMode
                entry.modTime = file.fileModTime
                putArchiveEntry(entry)
                try {
                    ShellHandler.quirkLibsuReadFileWorkaround(file, this)
                } finally {
                    closeArchiveEntry()
                }
            }

            FileType.DIRECTORY     -> {
                Timber.d("Adding directory ${file.filePath} to archive (filesize: ${file.fileSize})")
                entry = TarArchiveEntry(file.filePath, TarConstants.LF_DIR)
                entry.setNames(file.owner, file.group)
                entry.mode = DIR_MODE_OR_MASK or file.fileMode
                putArchiveEntry(entry)
                closeArchiveEntry()
            }

            FileType.SYMBOLIC_LINK -> {
                Timber.d("Adding symbolic link ${file.filePath} to archive (filesize: ${file.fileSize})")
                entry = TarArchiveEntry(file.filePath, TarConstants.LF_SYMLINK)
                entry.linkName = file.linkName
                entry.setNames(file.owner, file.group)
                entry.mode = SYMLINK_MODE_OR_MASK or file.fileMode
                putArchiveEntry(entry)
                closeArchiveEntry()
            }

            FileType.NAMED_PIPE    -> {
                Timber.d("Adding named pipe ${file.filePath} to archive (filesize: ${file.fileSize})")
                entry = TarArchiveEntry(file.filePath, TarConstants.LF_FIFO)
                entry.setNames(file.owner, file.group)
                entry.mode = FIFO_MODE_OR_MASK or file.fileMode
                putArchiveEntry(entry)
                closeArchiveEntry()
            }
            //TODO else: add to errors? can we backup these?
            FileType.BLOCK_DEVICE  -> Timber.w("Block devices should not occur: {$file.filePath}")
            FileType.CHAR_DEVICE   -> Timber.w("Char devices should not occur: {$file.filePath}")
            FileType.SOCKET        -> Timber.w("It does not make sense to backup sockets: {$file.filePath}") // not necessary
        }
    }
}

@Throws(IOException::class)
fun setAttributes(targetFile: RootFile, tarEntry: TarArchiveEntry) {
    val qUtilBox = ShellHandler.utilBoxQ
    val path = targetFile.absolutePath
    val mode = tarEntry.mode and 0b0_111_111_111_111
    try {
        runAsRoot(
            "$qUtilBox chmod ${
                String.format("%03o", mode)
            } ${quote(path)}"
        )
    } catch (e: Throwable) {
        throw IOException(
            "Unable to chmod $path to ${String.format("%03o", mode)}: $e"
        )
    }
    val (user, group) = listOf(tarEntry.userName, tarEntry.groupName)
    try {
        runAsRoot("$qUtilBox chown $user:$group ${quote(path)}")
    } catch (e: Throwable) {
        throw IOException("Unable to chown $path to $user:$group: $e")
    }
    val timeStr =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:SS",
            Locale.getDefault(Locale.Category.FORMAT)
        ).format(tarEntry.modTime.time)
    try {
        //targetPath.setLastModified(tarEntry.modTime.time)   YYYY-MM-DDThh:mm:SS[.frac][tz]
        runAsRoot(
            "$qUtilBox touch -m -d $timeStr ${quote(path)}"
        )
    } catch (e: Throwable) {
        throw IOException("Unable to set modification time on $path to $timeStr: $e")
    }
}

@Throws(IOException::class)
fun TarArchiveInputStream.suUnpackTo(targetDir: RootFile, forceOldVersion: Boolean = false) {
    val qUtilBox = ShellHandler.utilBoxQ
    val strictHardLinks = pref_strictHardLinks.value && !forceOldVersion
    val postponeInfo = mutableMapOf<String, TarArchiveEntry>()
    generateSequence { nextEntry }.forEach { tarEntry ->
        val targetFile = RootFile(targetDir, tarEntry.name)
        Timber.d("Extracting ${tarEntry.name} (filesize: ${tarEntry.realSize})")
        targetFile.parentFile?.let {
            if (!it.exists() and !it.mkdirs()) {
                throw IOException("Unable to create parent folder ${it.absolutePath}")
            }
        } ?: throw IOException("No parent folder for ${targetFile.absolutePath}")
        var doAttribs = true
        var postponeAttribs = false
        val relPath = targetFile.relativeTo(targetDir).toString()
        when {
            relPath.isEmpty() ||
                    relPath in NeoApp.assets.DATA_RESTORE_EXCLUDED_BASENAMES ||
                    relPath in NeoApp.assets.DATA_EXCLUDED_CACHE_DIRS -> {
                return@forEach
            }

            tarEntry.isDirectory                                      -> {
                if (!targetFile.mkdirs()) {
                    throw IOException("Unable to create folder ${targetFile.absolutePath}")
                }
                // write protection would prevent creating files inside, so chmod at end
                postponeAttribs = true
            }

            tarEntry.isLink                                           -> {
                // OABX v7 tarapi implementation stores all links as hard links (bug)
                // and extracts all links as symlinks (repair)
                if (strictHardLinks)
                    try {
                        runAsRoot(
                            "$qUtilBox ln ${quote(tarEntry.linkName)} ${quote(targetFile)}"
                        )
                    } catch (e: Throwable) {
                        throw IOException("Unable to create hardlink: ${tarEntry.linkName} -> ${targetFile.absolutePath} : $e")
                    }
                else
                    try {
                        runAsRoot(
                            // OABX v7 implementation stroes hard links and extracts all links as symlinks
                            "$qUtilBox ln -s ${quote(tarEntry.linkName)} ${quote(targetFile)}"
                        )
                    } catch (e: Throwable) {
                        throw IOException("Unable to create symlink: ${tarEntry.linkName} -> ${targetFile.absolutePath} : $e")
                    }
                doAttribs = false
            }

            tarEntry.isSymbolicLink                                   -> {
                try {
                    runAsRoot(
                        "$qUtilBox ln -s ${quote(tarEntry.linkName)} ${quote(targetFile)}"
                    )
                } catch (e: Throwable) {
                    throw IOException("Unable to create symlink: ${tarEntry.linkName} -> ${targetFile.absolutePath} : $e")
                }
                doAttribs = false
            }

            tarEntry.isFIFO                                           -> {
                try {
                    runAsRoot("$qUtilBox mkfifo ${quote(targetFile)}")
                } catch (e: Throwable) {
                    throw IOException("Unable to create fifo ${targetFile.absolutePath}: $e")
                }
            }

            else                                                      -> {
                try {
                    SuFileOutputStream.open(RootFile.open(targetDir, tarEntry.name))
                        .use { fos -> IOUtils.copy(this, fos, BUFFER_SIZE) }
                } catch (e: Throwable) {
                    throw IOException("Unable to create file ${targetFile.absolutePath}: $e")
                }
            }
        }
        if (doAttribs) {
            if (postponeAttribs) {
                postponeInfo[targetFile.absolutePath] = tarEntry
            } else {
                setAttributes(targetFile, tarEntry)
            }
        }

        postponeInfo.forEach { (targetFile, tarEntry) ->
            try {
                setAttributes(RootFile(targetFile), tarEntry)
            } catch (e: Throwable) {
                throw IOException("Unable to set security attributes on $targetFile: $e")
            }
        }
    }
}

/*
@Throws(IOException::class)
fun TarArchiveInputStream.unpackTo(targetDir: File?, strictHardLinks: Boolean = false) {
    targetDir?.let {
        val postponeModes = mutableMapOf<String, Int>()
        generateSequence { nextTarEntry }.forEach { tarEntry ->
            val targetFile = File(it, tarEntry.name)
            Timber.d("Extracting ${tarEntry.name} (filesize: ${tarEntry.realSize})")
            targetFile.parentFile?.let {
                if (!it.exists() and !it.mkdirs()) {
                    throw IOException("Unable to create parent folder ${it.absolutePath}")
                }
            } ?: throw IOException("No parent folder for ${targetFile.absolutePath}")
            var doChmod = true
            var postponeChmod = false
            var relPath = targetFile.relativeTo(it).toString()
            val mode = tarEntry.mode and 0b111_111_111_111
            when {
                relPath.isEmpty() -> return@forEach
                relPath in OABX.shellHandler!!.assets.DATA_EXCLUDED_BASENAMES -> return@forEach
                relPath in OABX.shellHandler!!.assets.DATA_EXCLUDED_CACHE_DIRS -> return@forEach
                tarEntry.isDirectory -> {
                    if (!targetFile.mkdirs()) {
                        throw IOException("Unable to create folder ${targetFile.absolutePath}")
                    }
                    // write protection would prevent creating files inside, so chmod at end
                    postponeChmod = true
                }
                tarEntry.isLink -> {
                    // OABX v7 implementation stores hard links and extracts all links as symlinks
                    if(strictHardLinks)
                        try {
                            Os.link(tarEntry.linkName, targetFile.absolutePath)
                        } catch (e: Throwable) {
                            throw IOException("Unable to create hardlink: ${tarEntry.linkName} -> ${targetFile.absolutePath} : $e")
                        }
                    else
                        try {
                            Os.symlink(tarEntry.linkName, targetFile.absolutePath)
                        } catch (e: Throwable) {
                            throw IOException("Unable to create symlink: ${tarEntry.linkName} -> ${targetFile.absolutePath} : $e")
                        }
                    doChmod = false
                }
                tarEntry.isSymbolicLink -> {
                    try {
                        Os.symlink(tarEntry.linkName, targetFile.absolutePath)
                    } catch (e: Throwable) {
                        throw IOException("Unable to create symlink: ${tarEntry.linkName} -> ${targetFile.absolutePath} : $e")
                    }
                    doChmod = false
                }
                tarEntry.isFIFO -> {
                    try {
                        Os.mkfifo(targetFile.absolutePath, tarEntry.mode)
                    } catch (e: Throwable) {
                        throw IOException("Unable to create fifo ${targetFile.absolutePath}: $e")
                    }
                }
                else -> {
                    try {
                        FileOutputStream(targetFile).use { fos -> IOUtils.copy(this, fos) }
                    } catch (e: Throwable) {
                        throw IOException("Unable to create file ${targetFile.absolutePath}: $e")
                    }
                }
            }
            if (doChmod) {
                if (postponeChmod) {
                    postponeModes[targetFile.absolutePath] = mode
                } else {
                    try {
                        Os.chmod(targetFile.absolutePath, mode)
                    } catch (e: Throwable) {
                        throw IOException("Unable to chmod ${targetFile.absolutePath} to ${String.format("%03o", mode)}: $e")
                    }
                }

            }

            try {
                targetFile.setLastModified(tarEntry.modTime.time)
            } catch (e: Throwable) {
                throw IOException("Unable to set modification time on $targetFile to ${tarEntry.modTime}: $e")
            }
        }
        postponeModes.forEach { fileMode ->
            try {
                Os.chmod(fileMode.key, fileMode.value)
            } catch (e: Throwable) {
                throw IOException("Unable to chmod ${fileMode.key} to ${String.format("%03o", fileMode.value)}: $e")
            }
        }
    }
}
*/
