package com.tauri.pushnotifications // Match the namespace

import android.util.Log
import app.tauri.plugin.JSObject
import app.tauri.plugin.PluginManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCMService"
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the previous token had expired,
     * the app deletes Instance ID data, or the app is restored on a new device.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Send token to JavaScript via event
        val data = JSObject().apply {
            put("token", token)
            put("platform", "android")
        }
        // Use PluginManager to get the plugin instance and emit the event
        PluginManager.getInstance().getPlugin("push-notifications")?.emit("push_token", data)
            ?: Log.e(TAG, "PushNotificationsPlugin instance not found, cannot emit push_token event.")
    }

    /**
     * Called when a message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Prepare data payload for JavaScript
        val payload = JSObject()

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().also {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val dataMap = JSObject()
            remoteMessage.data.forEach { (key, value) -> dataMap.put(key, value) }
            payload.put("data", dataMap)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val notificationMap = JSObject().apply {
                put("title", it.title)
                put("body", it.body)
                // Add other notification properties if needed (e.g., icon, sound)
            }
            payload.put("notification", notificationMap)
        }

        // Send the combined payload to JavaScript via event
        PluginManager.getInstance().getPlugin("push-notifications")?.emit("push_message", payload)
            ?: Log.e(TAG, "PushNotificationsPlugin instance not found, cannot emit push_message event.")

        // Note: If you want the system to handle the notification display when the app is in the background,
        // the message should include a `notification` payload. If you want to handle the display yourself
        // (e.g., custom layout), you'll need to process the `data` payload here and build the notification manually.
    }
}
