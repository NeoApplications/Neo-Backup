package com.machiav3lli.backup.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.machiav3lli.backup.NOTIFICATION_CHANNEL_HANDLER
import com.machiav3lli.backup.NOTIFICATION_CHANNEL_REFRESH
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.activities.BaseActivity
import com.machiav3lli.backup.ui.activities.NeoActivity
import kotlin.random.Random

val Context.notificationManager: NotificationManagerCompat
    get() = NotificationManagerCompat.from(this)

fun Context.reportRefreshFail(
    message: String,
) {
    val title = getString(R.string.refresh_failed)

    val builder = NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_REFRESH)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setContentTitle(title)
        .setContentText(message)
        .setTicker(title)
        .setOngoing(false)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, NeoActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) notificationManager.notify(
        (title + message).hashCode(),
        builder.build()
    )
}

fun Context.showNotification(
    parentActivity: Class<out BaseActivity?>?,
    title: String?, text: String?,
    autoCancel: Boolean,
    id: Int = uniqueNotificationId,
) {
    showNotification(
        parentActivity = parentActivity,
        title = title,
        text = text,
        bigText = "",
        autoCancel = autoCancel,
        id = id,
    )
}

private val notificationIdGenerator by lazy { Random(SystemUtils.now.toInt()) }

val uniqueNotificationId: Int
    get() = notificationIdGenerator.nextInt()

@SuppressLint("MissingPermission")
fun Context.showNotification(
    parentActivity: Class<out BaseActivity?>?,
    title: String?, text: String?,
    bigText: String,
    autoCancel: Boolean,
    id: Int,
) {
    val resultIntent = Intent(this, parentActivity)
    resultIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    val resultPendingIntent = PendingIntent.getActivity(
        this, 0, resultIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_HANDLER)
        .setGroup(SystemUtils.packageName)
        .setSortKey("9")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setStyle(
            if (bigText.isEmpty()) null
            else NotificationCompat.BigTextStyle().bigText(bigText)
        )
        .setContentText(if (text.isNullOrEmpty()) null else text)
        .setAutoCancel(autoCancel)
        .setContentIntent(resultPendingIntent)
        .build()
    notificationManager.notify(id, notification)
}