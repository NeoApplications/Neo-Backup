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
package com.machiav3lli.backup.data.dbs.entity

import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import com.machiav3lli.backup.BACKUP_INSTANCE_PROPERTIES_INDIR
import com.machiav3lli.backup.BACKUP_INSTANCE_REGEX_PATTERN
import com.machiav3lli.backup.FIELD_BACKUP_DATE
import com.machiav3lli.backup.FIELD_PACKAGE_NAME
import com.machiav3lli.backup.MODE_APK
import com.machiav3lli.backup.MODE_DATA
import com.machiav3lli.backup.MODE_DATA_DE
import com.machiav3lli.backup.MODE_DATA_EXT
import com.machiav3lli.backup.MODE_DATA_MEDIA
import com.machiav3lli.backup.MODE_DATA_OBB
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.PROP_NAME
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.manager.handler.LogsHandler.Companion.logException
import com.machiav3lli.backup.manager.handler.regexPackageFolder
import com.machiav3lli.backup.ui.pages.pref_createInvalidBackups
import com.machiav3lli.backup.utils.LocalDateTimeSerializer
import com.machiav3lli.backup.utils.SystemUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime

@Entity(
    primaryKeys = [FIELD_PACKAGE_NAME, FIELD_BACKUP_DATE],
    indices = [
        Index(FIELD_PACKAGE_NAME, FIELD_BACKUP_DATE, unique = true),
        Index(FIELD_PACKAGE_NAME),
    ]
)
@Serializable
@Stable
data class Backup(
    val backupVersionCode: Int = 0,
    val packageName: String,
    val packageLabel: String,
    @ColumnInfo(defaultValue = "-")
    val versionName: String? = "-",
    val versionCode: Int = 0,
    val profileId: Int = 0,
    val sourceDir: String? = null,
    val splitSourceDirs: Array<String> = arrayOf(),
    val isSystem: Boolean = false,
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable(with = LocalDateTimeSerializer::class)
    val backupDate: LocalDateTime,
    val hasApk: Boolean = false,
    val hasAppData: Boolean = false,
    val hasDevicesProtectedData: Boolean = false,
    val hasExternalData: Boolean = false,
    val hasObbData: Boolean = false,
    val hasMediaData: Boolean = false,
    val compressionType: String? = null,
    val cipherType: String? = null,
    val iv: ByteArray? = byteArrayOf(),
    val cpuArch: String?,
    val permissions: List<String> = listOf(),
    val size: Long = 0,
    val note: String = "",
    @ColumnInfo(defaultValue = "0")
    val persistent: Boolean = false,
) {
    constructor(
        base: PackageInfo,
        backupDate: LocalDateTime,
        hasApk: Boolean,
        hasAppData: Boolean,
        hasDevicesProtectedData: Boolean,
        hasExternalData: Boolean,
        hasObbData: Boolean,
        hasMediaData: Boolean,
        compressionType: String?,
        cipherType: String?,
        iv: ByteArray?,
        cpuArch: String?,
        permissions: List<String>,
        size: Long,
        persistent: Boolean = false,
        note: String = "",
    ) : this(
        backupVersionCode = SystemUtils.backupVersionCode,
        packageName = base.packageName,
        packageLabel = base.packageLabel,
        versionName = base.versionName,
        versionCode = base.versionCode,
        profileId = base.profileId,
        sourceDir = base.sourceDir,
        splitSourceDirs = base.splitSourceDirs,
        isSystem = base.isSystem,
        backupDate = backupDate,
        hasApk = hasApk,
        hasAppData = hasAppData,
        hasDevicesProtectedData = hasDevicesProtectedData,
        hasExternalData = hasExternalData,
        hasObbData = hasObbData,
        hasMediaData = hasMediaData,
        compressionType = compressionType,
        cipherType = cipherType,
        iv = iv,
        cpuArch = cpuArch,
        permissions = permissions.sorted(),
        size = size,
        persistent = persistent,
        note = note,
    )

    val isCompressed: Boolean
        get() = !compressionType.isNullOrEmpty()

    val isEncrypted: Boolean
        get() = !cipherType.isNullOrEmpty()

    val hasData: Boolean
        get() = hasAppData || hasExternalData || hasDevicesProtectedData || hasMediaData || hasObbData

    fun hasMode(mode: Int): Boolean = when (mode) {
        MODE_APK        -> hasApk
        MODE_DATA       -> hasData
        MODE_DATA_DE    -> hasDevicesProtectedData
        MODE_DATA_EXT   -> hasExternalData
        MODE_DATA_OBB   -> hasObbData
        MODE_DATA_MEDIA -> hasMediaData
        else            -> false
    }

    fun toAppInfo() = AppInfo(
        packageName,
        packageLabel,
        versionName,
        versionCode,
        profileId,
        sourceDir,
        splitSourceDirs,
        isSystem,
        permissions
    )

    override fun toString(): String = "Backup{" +
            "packageName=" + packageName +
            ", backupDate=" + backupDate +
            ", hasApk=" + hasApk +
            ", hasAppData=" + hasAppData +
            ", hasDevicesProtectedData=" + hasDevicesProtectedData +
            ", hasExternalData=" + hasExternalData +
            ", hasObbData=" + hasObbData +
            ", hasMediaData=" + hasMediaData +
            ", persistent='" + persistent + '\'' +
            ", size=" + size +
            ", backupVersionCode='" + backupVersionCode + '\'' +
            ", cpuArch='" + cpuArch + '\'' +
            ", compressionType='" + compressionType + '\'' +
            ", cipherType='" + cipherType + '\'' +
            ", iv='" + iv + '\'' +
            ", permissions='" + permissions + '\'' +
            ", persistent=" + persistent +
            ", note='" + note + '\'' +
            '}'

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass
                || other !is Backup
                || packageName != other.packageName
                || backupDate != other.backupDate
                || backupVersionCode != other.backupVersionCode
                || packageLabel != other.packageLabel
                || versionName != other.versionName
                || versionCode != other.versionCode
                || profileId != other.profileId
                || sourceDir != other.sourceDir
                || !splitSourceDirs.contentEquals(other.splitSourceDirs)
                || isSystem != other.isSystem
                || hasApk != other.hasApk
                || hasAppData != other.hasAppData
                || hasDevicesProtectedData != other.hasDevicesProtectedData
                || hasExternalData != other.hasExternalData
                || hasObbData != other.hasObbData
                || hasMediaData != other.hasMediaData
                || compressionType != other.compressionType
                || cipherType != other.cipherType
                || iv != null && other.iv == null
                || iv != null && !iv.contentEquals(other.iv)
                || iv == null && other.iv != null
                || cpuArch != other.cpuArch
                || isEncrypted != other.isEncrypted
                || permissions != other.permissions
                || persistent != other.persistent
                || note != other.note
                || file?.path != other.file?.path
                || dir?.path != other.dir?.path
                       -> false

        else           -> true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + backupVersionCode
        result = 31 * result + (packageLabel.hashCode())
        result = 31 * result + versionName.hashCode()
        result = 31 * result + versionCode
        result = 31 * result + profileId
        result = 31 * result + sourceDir.hashCode()
        result = 31 * result + splitSourceDirs.contentHashCode()
        result = 31 * result + isSystem.hashCode()
        result = 31 * result + backupDate.hashCode()
        result = 31 * result + hasApk.hashCode()
        result = 31 * result + hasAppData.hashCode()
        result = 31 * result + hasDevicesProtectedData.hashCode()
        result = 31 * result + hasExternalData.hashCode()
        result = 31 * result + hasObbData.hashCode()
        result = 31 * result + hasMediaData.hashCode()
        result = 31 * result + compressionType.hashCode()
        result = 31 * result + cipherType.hashCode()
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + cpuArch.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + permissions.hashCode()
        result = 31 * result + persistent.hashCode()
        result = 31 * result + note.hashCode()
        result = 31 * result + file?.path.hashCode()
        result = 31 * result + dir?.path.hashCode()
        return result
    }

    fun toSerialized() = NeoApp.toSerialized(NeoApp.mainSerializer, this)

    class BrokenBackupException @JvmOverloads internal constructor(
        message: String?,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    @Ignore
    @Transient
    var file: StorageFile? = null

    @Ignore
    @Transient
    var dir: StorageFile? = null
        get() {
            if (field == null) {
                field = if (file?.name == BACKUP_INSTANCE_PROPERTIES_INDIR) {
                    file?.parent
                } else {
                    val baseName = file?.name?.removeSuffix(".$PROP_NAME")
                    baseName?.let { dirName ->
                        file?.parent?.findFile(dirName)
                    }
                }
            }
            return field
        }

    val directoryTag: String
        get() {
            val pkg = "📦" // "📁"
            return (dir?.path
                ?.replace(NeoApp.backupRoot?.path ?: "", "")
                ?.replace(packageName, pkg)
                ?.replace(Regex("""($pkg@)?$BACKUP_INSTANCE_REGEX_PATTERN"""), "")
                ?.replace(Regex("""[-:\s]+"""), "-")
                ?.replace(Regex("""/+"""), "/")
                ?.replace(Regex("""[-]+$"""), "-")
                ?.replace(Regex("""^[-/]+"""), "")
                ?: "") + if (file?.name == BACKUP_INSTANCE_PROPERTIES_INDIR) "🔹" else ""
        }

    companion object {

        fun fromSerialized(serialized: String) = NeoApp.fromSerialized<Backup>(serialized)

        fun createFrom(propertiesFile: StorageFile): Backup? {
            var serialized = ""
            try {

                serialized = propertiesFile.readText()

                val backup = if (serialized.isEmpty()) {
                    if (pref_createInvalidBackups.value)
                        createInvalidFrom(propertiesFile, why = "empty-props")
                    else
                        null
                } else {
                    fromSerialized(serialized)
                }

                //TODO bug: list serialization (jsonPretty, yaml) adds a space in front of each value
                // found older multiline json and yaml without the bug, so it was introduced lately (by lib versions)
                return backup?.copy(
                    permissions = backup.permissions.map { it.trim() }, //TODO workaround
                )?.apply {
                    file = propertiesFile
                }
            } catch (e: FileNotFoundException) {
                logException(e, "Cannot open ${propertiesFile.path}", backTrace = false)
            } catch (e: IOException) {
                logException(e, "Cannot read ${propertiesFile.path}", backTrace = false)
            } catch (e: Throwable) {
                logException(e, "file: ${propertiesFile.path} =\n$serialized", backTrace = false)
            }
            return null
        }

        fun createInvalidFrom(
            directory: StorageFile,
            propertiesFile: StorageFile? = null,
            packageName: String? = null,
            why: String? = null,
        ): Backup? {

            try {

                val packageNameFixed = packageName ?: run {
                    listOf(directory, directory.parent)
                        .mapNotNull { it?.name }
                        .firstNotNullOfOrNull { name ->
                            if (regexPackageFolder.matches(name)) {
                                name
                            } else {
                                regexPackageFolder.find(name)?.let { match ->
                                    match.groups[0]?.value
                                }
                            }
                        }
                } ?: ""

                val backup = fromSerialized(
                    "{\n" +
                            "    \"backupVersionCode\": ${
                                com.machiav3lli.backup.BuildConfig.MAJOR * 1000 +
                                        com.machiav3lli.backup.BuildConfig.MINOR
                            },\n" +
                            "    \"packageName\": \"${"..." + (why ?: "")}\",\n" +
                            "    \"packageLabel\": \"? INVALID\",\n" +
                            "    \"versionName\": \"$packageNameFixed\",\n" +
                            "    \"versionCode\": 0,\n" +
                            //"    \"sourceDir\": \"/data/app/~~oXzw9ZEl326kQh4Ay1vHJQ==/org.woheller69.weather-gWQaSUpYxRgFVvgMTqNb9A==/base.apk\",\n" +
                            "    \"splitSourceDirs\": [],\n" +
                            "    \"backupDate\": \"2000-01-01T00:00:00\",\n" +
                            "    \"hasApk\": false,\n" +
                            "    \"hasAppData\": false,\n" +
                            "    \"hasDevicesProtectedData\": false,\n" +
                            "    \"hasExternalData\": false,\n" +
                            "    \"compressionType\": \"zst\",\n" +
                            //"    \"iv\": [],\n" +
                            "    \"cpuArch\": \"\",\n" +
                            "    \"size\": 0\n" +
                            "    \"note\": \"invalid\"\n" +
                            "}"
                )

                backup.file = propertiesFile
                backup.dir = directory

                return backup

            } catch (e: Throwable) {
                logException(
                    e,
                    "creating invalid backup item also failed, for directory ${
                        directory.path
                    }${
                        if (propertiesFile != null)
                            " and file ${propertiesFile.path}"
                        else
                            ""
                    }",
                    backTrace = false
                )
            }
            return null
        }
    }
}