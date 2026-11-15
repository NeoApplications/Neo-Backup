package com.machiav3lli.backup.utils

import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.quote
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRootPipeInCollectErr
import com.machiav3lli.backup.manager.handler.ShellHandler.Companion.runAsRootPipeOutCollectErr
import java.io.IOException

fun copyDocumentToRootFile(sourceFile: StorageFile, targetPath: String) {
    sourceFile.inputStream()?.use { inputStream ->
        runAsRootPipeInCollectErr(inputStream, "cat >${quote(targetPath)}")
    } ?: throw IOException("cannot read ${sourceFile.path}")
}

fun copyRootFileToDocument(sourcePath: String, targetDir: StorageFile, name: String) {
    targetDir.createFile(name).let { newFile ->
        newFile.outputStream()?.use { outputStream ->
            runAsRootPipeOutCollectErr(outputStream, "cat ${quote(sourcePath)}")
        } ?: throw IOException("cannot write to ${newFile.path}")
    }
}
