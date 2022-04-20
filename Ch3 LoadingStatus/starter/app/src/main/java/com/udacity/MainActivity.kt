package com.udacity

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private val notificationId = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initializeNotificationManager()
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        custom_button.setOnClickListener {
            getRadioButtonCurrentChoice()
        }
    }

    private fun getRadioButtonCurrentChoice() {
        val radioButtonGroup = findViewById<RadioGroup>(R.id.radio_button_group)

        val currentChoice = when (radioButtonGroup.checkedRadioButtonId) {
            R.id.glide_radio_button -> FileType.Glide
            R.id.load_app_radio_button -> FileType.LoadApp
            R.id.retrofit_radio_button -> FileType.Retrofit
            else -> {
                displayToast(getString(R.string.no_button_selected_toast))
                null
            }
        }
        currentChoice ?: return

        custom_button.changeButtonState()
        download(currentChoice)
    }

    private fun displayToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    //region Download
    private fun download(fileType: FileType) {
        val request =
            DownloadManager.Request(Uri.parse(fileType.url))
                .setTitle(fileType.name)
                .setDescription(fileType.description)
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        // enqueue() puts the download request in the queue.
        downloadID = downloadManager.enqueue(request)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != downloadID) return

            custom_button.changeButtonState()
            displayToast(getString(R.string.download_finished_toast))
            parseDownloadResult()
        }
    }

    private fun parseDownloadResult() {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadID)

        // Make sure to close cursor / catch reading error by using .use{}
        // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html
        downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                // Use the full description as file name
                val columnDescription = cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)
                val fileName = cursor.getString(columnDescription)

                val columnStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(columnStatus)

                // There are many other status possible (error, paused, pending etc),
                // but we just consider success/failure to simplify
                val statusString = if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    getString(R.string.download_status_success)
                } else {
                    getString(R.string.download_status_failure)
                }

                buildNotification(fileName, statusString)
            }
        }
    }
    //endregion

    //region Notification
    private fun buildNotification(fileName: String, resultStatus: String) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra(KEY_FILE_NAME, fileName)
        intent.putExtra(KEY_DOWNLOAD_STATUS, resultStatus)

        pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Don't display an icon for the action (first param as 0)
        action =
            NotificationCompat.Action(0, getString(R.string.notification_button), pendingIntent)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            // Open DetailActivity upon click
            .setContentIntent(pendingIntent)
            // Dismiss the notification upon click
            .setAutoCancel(true)
            .addAction(action)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun initializeNotificationManager() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
    //endregion

    companion object {
        private const val CHANNEL_ID = "channelId"
        const val KEY_FILE_NAME = "keyFileName"
        const val KEY_DOWNLOAD_STATUS = "downloadStatus"
    }

}
