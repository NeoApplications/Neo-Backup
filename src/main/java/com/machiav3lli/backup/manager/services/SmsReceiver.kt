/*
 * OAndBackupX: open-source apps backup and restore app.
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
package com.machiav3lli.backup.manager.services

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.content.PermissionChecker
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.showNotification
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || intent.action == null || intent.extras == null) return

        if (intent.action != (Telephony.Sms.Intents.SMS_DELIVER_ACTION)) return

        if (
            (PermissionChecker.checkCallingOrSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PermissionChecker.PERMISSION_DENIED) ||
            (PermissionChecker.checkCallingOrSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PermissionChecker.PERMISSION_DENIED) ||
            (PermissionChecker.checkCallingOrSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PermissionChecker.PERMISSION_DENIED) ||
            (PermissionChecker.checkCallingOrSelfPermission(
                context,
                Manifest.permission.RECEIVE_MMS
            ) == PermissionChecker.PERMISSION_DENIED) ||
            (PermissionChecker.checkCallingOrSelfPermission(
                context,
                Manifest.permission.RECEIVE_WAP_PUSH
            ) == PermissionChecker.PERMISSION_DENIED)
        ) return

        val pendingResult = goAsync()
        val appScope = (context.applicationContext as NeoApp).applicationScope
        appScope.launch {
            try {
                val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (message in smsMessages) {
                    putSmsToDatabase(context, message)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun putSmsToDatabase(context: Context, sms: SmsMessage) {
        val contentResolver = context.contentResolver
        val values = ContentValues()
        val notificationId = SystemUtils.now
        val message = sms.displayMessageBody.toString()
        var sender = sms.displayOriginatingAddress ?: ""
        val threadId = Telephony.Threads.getOrCreateThreadId(context, sms.displayOriginatingAddress)

        if (PermissionChecker.checkCallingOrSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PermissionChecker.PERMISSION_DENIED
        ) {
            val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender))
            val projection = arrayOf(PhoneLookup.DISPLAY_NAME)
            try {
                val cursor = contentResolver.query(uri, projection, null, null, null)
                cursor.use {
                    if (cursor?.moveToFirst() == true) {
                        if (cursor.getString(0) != "") {
                            sender = cursor.getString(0)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }

        values.put(Telephony.Sms.THREAD_ID, threadId)
        values.put(Telephony.Sms.ADDRESS, sms.displayOriginatingAddress)
        values.put(Telephony.Sms.DATE, sms.timestampMillis)
        values.put(Telephony.Sms.READ, 0)
        values.put(Telephony.Sms.STATUS, sms.status)
        values.put(Telephony.Sms.TYPE, 1)
        values.put(Telephony.Sms.SEEN, 0)
        values.put(Telephony.Sms.LOCKED, 0)
        values.put(Telephony.Sms.BODY, message)
        values.put(Telephony.Sms.SUBJECT, sms.pseudoSubject)
        contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        context.showNotification(
            NeoActivity::class.java,
            sender, message, true, notificationId.toInt()
        )
    }
}
