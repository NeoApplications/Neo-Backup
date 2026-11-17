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
package com.machiav3lli.backup.manager.tasks

import com.machiav3lli.backup.data.dbs.entity.Backup
import com.machiav3lli.backup.data.entity.ActionResult
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.manager.handler.BackupRestoreHelper
import com.machiav3lli.backup.manager.handler.ShellHandler
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.utils.SystemUtils

class RestoreActionTask(
    appInfo: Package, oAndBackupX: NeoActivity, shellHandler: ShellHandler, restoreMode: Int,
    private val backup: Backup, private val password: String? = null,
    setInfoBar: (String) -> Unit,
) : BaseActionTask(
    appInfo, oAndBackupX, shellHandler, restoreMode,
    BackupRestoreHelper.ActionType.RESTORE, setInfoBar
) {
    override fun onPreExecute() {
        super.onPreExecute()
        notificationId = SystemUtils.now.toInt()
    }

    override suspend fun doInBackground(vararg params: Void?): ActionResult? {
        val mainActivityX = neoActivityReference.get()?.takeIf { !it.isFinishing }
            ?: return ActionResult(app, null, "", false)

        publishProgress()
        result = BackupRestoreHelper.restore(
            mainActivityX, null, shellHandler,
            app, mode, backup, password,
        )
        return result
    }
}