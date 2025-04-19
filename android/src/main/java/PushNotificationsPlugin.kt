package com.tauri.pushnotifications // Match the namespace

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.Permission
import app.tauri.annotation.PermissionCallback
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import app.tauri.PermissionState
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Define permissions required by the plugin
@TauriPlugin(
    permissions = [
        Permission(strings = [Manifest.permission.POST_NOTIFICATIONS], alias = "postNotification")
    ]
)
class PushNotificationsPlugin(private val activity: Activity) : Plugin(activity) {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001 // Unique code for permission request
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Command
    suspend fun getToken(invoke: Invoke) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val result = JSObject().apply {
                put("token", token)
                put("platform", "android")
            }
            invoke.resolve(result)
        } catch (e: Exception) {
            invoke.reject("Failed to get FCM token: ${e.localizedMessage}")
        }
    }

    @Command
    fun requestPermissions(invoke: Invoke) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires runtime permission for notifications
            val currentStatus = getPermissionState("postNotification")
            if (currentStatus != PermissionState.GRANTED) {
                // Use Tauri's permission request system
                requestPermissionForAlias("postNotification", invoke, "permissionCallback")
            } else {
                // Already granted
                invoke.resolve(JSObject().put("status", "granted"))
            }
        } else {
            // Permissions are implicitly granted on older versions
            invoke.resolve(JSObject().put("status", "granted"))
        }
    }

    // Callback for the permission request
    @PermissionCallback
    fun permissionCallback(invoke: Invoke) {
        val status = getPermissionState("postNotification")
        invoke.resolve(JSObject().put("status", status.toString().lowercase()))
    }

    // Helper to get permission state (needed by Tauri's permission system)
    override fun getPermissionState(permission: String): PermissionState {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                        PermissionState.GRANTED
                    } else {
                        // Check if rationale should be shown (optional, depends on UX)
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            PermissionState.PROMPT_WITH_RATIONALE
                        } else {
                            PermissionState.PROMPT
                        }
                    }
                } else {
                    PermissionState.GRANTED // Granted by default below Android 13
                }
            }
            else -> super.getPermissionState(permission)
        }
    }
}
