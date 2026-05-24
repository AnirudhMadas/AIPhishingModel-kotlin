package com.example.aiphishingmodel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class NotificationService : NotificationListenerService() {

    private val client = OkHttpClient()

    // =========================
    // APPS TO MONITOR
    // =========================
    private val allowedPackages = setOf(

        "com.google.android.apps.messaging",
        "com.android.messaging",

        "com.whatsapp",
        "com.whatsapp.w4b",

        "org.telegram.messenger",

        "com.facebook.orca",

        "com.instagram.android",

        "com.truecaller",

        "com.google.android.gm"
    )

    // =========================
    // PREVENT DUPLICATES
    // =========================
    private var lastMessage = ""

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        try {

            Log.d(
                "NOTIFICATION_TEST",
                "Notification Received"
            )

            val packageName = sbn.packageName

            // =========================
            // ALLOW ONLY SELECTED APPS
            // =========================
            if (!allowedPackages.contains(packageName)) {

                Log.d(
                    "NOTIFICATION_TEST",
                    "Ignored App: $packageName"
                )

                return
            }

            val notification = sbn.notification

            val extras = notification.extras

            // =========================
            // ALLOW MESSAGE + EMAIL
            // =========================
            val category = notification.category

            if (
                category != Notification.CATEGORY_MESSAGE &&
                category != Notification.CATEGORY_EMAIL &&
                category != null
            ) {

                Log.d(
                    "NOTIFICATION_TEST",
                    "Ignored Category: $category"
                )

                return
            }

            val title =
                extras.getCharSequence(
                    Notification.EXTRA_TITLE
                )?.toString() ?: ""

            val text =
                extras.getCharSequence(
                    Notification.EXTRA_TEXT
                )?.toString() ?: ""

            val bigText =
                extras.getCharSequence(
                    Notification.EXTRA_BIG_TEXT
                )?.toString() ?: ""

            val summaryText =
                extras.getCharSequence(
                    Notification.EXTRA_SUMMARY_TEXT
                )?.toString() ?: ""

            // =========================
            // EXTRACT BEST MESSAGE
            // =========================
            val finalMessage = when {

                bigText.isNotBlank() -> bigText

                text.isNotBlank() -> text

                summaryText.isNotBlank() -> summaryText

                else -> ""
            }

            // =========================
            // IGNORE EMPTY
            // =========================
            if (finalMessage.isBlank()) {
                return
            }

            // =========================
            // IGNORE VERY SHORT
            // =========================
            if (finalMessage.length < 8) {
                return
            }

            if (
                finalMessage.contains("new messages", true) ||
                finalMessage.contains("messages", true)
            ) {
                return
            }

            // =========================
            // IGNORE DUPLICATES
            // =========================
            if (finalMessage == lastMessage) {

                Log.d(
                    "NOTIFICATION_TEST",
                    "Duplicate Ignored"
                )

                return
            }

            lastMessage = finalMessage

            // =========================
            // IGNORE MEDIA
            // =========================
            if (
                notification.category ==
                Notification.CATEGORY_TRANSPORT
            ) {
                return
            }

            // =========================
            // IGNORE ONGOING SERVICES
            // =========================
            if (
                notification.flags and
                Notification.FLAG_ONGOING_EVENT != 0
            ) {
                return
            }

            // =========================
            // IGNORE SIMPLE OTPS
            // =========================
            val otpRegex =
                Regex(".*\\b\\d{4,8}\\b.*")

            if (
                otpRegex.matches(finalMessage) &&
                finalMessage.length < 25
            ) {
                return
            }

            // =========================
            // URL EXTRACTION
            // =========================
            val urlRegex =
                Regex("https?://\\S+")

            val urls = urlRegex.findAll(finalMessage)
                .map { it.value }
                .toList()

            Log.d(
                "NOTIFICATION_TEST",
                "====================="
            )

            Log.d(
                "NOTIFICATION_TEST",
                "App: $packageName"
            )

            Log.d(
                "NOTIFICATION_TEST",
                "Title: $title"
            )

            Log.d(
                "NOTIFICATION_TEST",
                "Message: $finalMessage"
            )

            Log.d(
                "NOTIFICATION_TEST",
                "URLs: $urls"
            )

            Log.d(
                "NOTIFICATION_TEST",
                "====================="
            )

            // =========================
            // SEND TO BACKEND
            // =========================
            sendToBackend(finalMessage)

        } catch (e: Exception) {

            Log.e(
                "NOTIFICATION_TEST",
                "Error: ${e.message}"
            )
        }
    }

    // =========================
    // SEND MESSAGE TO FASTAPI
    // =========================
    private fun sendToBackend(
        message: String
    ) {

        try {

            val json = JSONObject()

            json.put("text", message)

            val body = RequestBody.create(
                "application/json"
                    .toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()

                // CHANGE IP IF NEEDED
                .url("http://192.168.29.36:8000/predict")

                .post(body)

                .build()

            client.newCall(request)
                .enqueue(object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        Log.e(
                            "API_TEST",
                            "Failed: ${e.message}"
                        )
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        try {

                            val result =
                                response.body?.string()

                            Log.d(
                                "API_TEST",
                                "Backend Response: $result"
                            )

                            if (result == null) {
                                return
                            }

                            val jsonResponse =
                                JSONObject(result)

                            val prediction =
                                jsonResponse.getString(
                                    "prediction"
                                )

                            val confidence =
                                jsonResponse.getDouble(
                                    "confidence"
                                )

                            Log.d(
                                "PREDICTION_CHECK",
                                prediction
                            )

                            // =========================
                            // ALERT ONLY IF NOT HAM
                            // =========================
                            if (
                                !prediction.equals(
                                    "ham",
                                    true
                                )
                            ) {

                                Log.d(
                                    "PHISHING_ALERT",
                                    "Dangerous message detected!"
                                )

                                showWarningNotification(
                                    message,
                                    confidence
                                )
                            }

                        } catch (e: Exception) {

                            Log.e(
                                "API_TEST",
                                "Response Parse Error: ${e.message}"
                            )
                        }
                    }
                })

        } catch (e: Exception) {

            Log.e(
                "API_TEST",
                "Error: ${e.message}"
            )
        }
    }

    // =========================
    // SHOW ALERT NOTIFICATION
    // =========================
    private fun showWarningNotification(
        message: String,
        confidence: Double
    ) {

        try {

            Log.d(
                "ALERT_TEST",
                "Showing Alert Notification"
            )

            val channelId = "phishing_alerts"

            val manager =
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager

            // =========================
            // CREATE CHANNEL
            // =========================
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                val channel =
                    NotificationChannel(
                        channelId,
                        "Phishing Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    )

                channel.description =
                    "Alerts for spam/phishing messages"

                manager.createNotificationChannel(
                    channel
                )
            }

            // =========================
            // OPEN APP WHEN CLICKED
            // =========================
            val intent = Intent(
                this,
                MainActivity::class.java
            )

            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            // =========================
            // BUILD NOTIFICATION
            // =========================
            val notification =
                NotificationCompat.Builder(
                    this,
                    channelId
                )

                    .setSmallIcon(
                        R.mipmap.ic_launcher
                    )

                    .setContentTitle(
                        "⚠️ Spam/Phishing Detected"
                    )

                    .setContentText(
                        message.take(60)
                    )

                    .setStyle(
                        NotificationCompat
                            .BigTextStyle()
                            .bigText(
                                "Suspicious message detected\n\n" +
                                        "Confidence: ${
                                            String.format(
                                                "%.2f",
                                                confidence
                                            )
                                        }%\n\n$message"
                            )
                    )

                    .setPriority(
                        NotificationCompat.PRIORITY_HIGH
                    )

                    .setAutoCancel(true)

                    .setContentIntent(
                        pendingIntent
                    )

                    .build()

            val handler = android.os.Handler(mainLooper)

            handler.post {

                manager.notify(
                    System.currentTimeMillis().toInt(),
                    notification
                )
            }

        } catch (e: Exception) {

            Log.e(
                "ALERT_TEST",
                "Notification Error: ${e.message}"
            )
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {

        Log.d(
            "NOTIFICATION_TEST",
            "Notification Removed"
        )
    }
}