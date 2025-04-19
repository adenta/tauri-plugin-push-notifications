import SwiftRs
import Tauri
import UIKit
import UserNotifications
import os.log

let logger = Logger(subsystem: "com.tauri.pushnotifications", category: "Plugin")

class PushNotificationsPlugin: Plugin, UNUserNotificationCenterDelegate {
    private var originalDelegate: UIApplicationDelegate?
    private var token: String?

    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
        logger.info("PushNotificationsPlugin initialized")
    }

    @objc override public func load(webview: WKWebView) {
        super.load(webview: webview)
        logger.info("PushNotificationsPlugin loaded")

        if let app = UIApplication.value(forKey: "sharedApplication") as? UIApplication {
            self.originalDelegate = app.delegate
            // Use Tauri's proxy delegate feature instead of swizzling directly
            // Ensure 'ios-proxy-delegate' feature is enabled in Cargo.toml
            logger.info("Original AppDelegate captured")
        } else {
            logger.error("Failed to capture original AppDelegate")
        }
    }

    // This method will be called by Tauri's proxy delegate
    @objc public func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        logger.info("Received APNS token: \(tokenString)")
        self.token = tokenString

        // Send token to JavaScript
        do {
            let data = ["token": tokenString, "platform": "ios"]
            try emit("push_token", data: data)
            logger.info("Emitted push_token event")
        } catch {
            logger.error("Failed to emit push_token event: \(error)")
        }

        // Call original delegate if it implements the method
        self.originalDelegate?.application?(
            application, didRegisterForRemoteNotificationsWithDeviceToken: deviceToken)
    }

    // This method will be called by Tauri's proxy delegate
    @objc public func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        logger.error("Failed to register for remote notifications: \(error.localizedDescription)")
        // Call original delegate if it implements the method
        self.originalDelegate?.application?(
            application, didFailToRegisterForRemoteNotificationsWithError: error)
    }

    // This method will be called by Tauri's proxy delegate
    @objc public func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        logger.info("Received remote notification")
        // Send notification data to JavaScript
        do {
            try emit("push_message", data: userInfo)
            logger.info("Emitted push_message event")
        } catch {
            logger.error("Failed to emit push_message event: \(error)")
        }

        // Call original delegate if it implements the method
        if let originalMethod = self.originalDelegate?.application?(
            _:didReceiveRemoteNotification:fetchCompletionHandler:) {
            originalMethod(application, userInfo, completionHandler)
        } else {
            completionHandler(.noData)
        }
    }

    // UNUserNotificationCenterDelegate method for foreground notifications
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        logger.info("Received foreground notification")
        let userInfo = notification.request.content.userInfo
        // Send notification data to JavaScript
        do {
            try emit("push_message", data: userInfo)
            logger.info("Emitted push_message event (foreground)")
        } catch {
            logger.error("Failed to emit push_message event (foreground): \(error)")
        }

        // Determine presentation options (e.g., show alert, play sound)
        completionHandler([.alert, .sound, .badge]) // Adjust as needed
    }

    // UNUserNotificationCenterDelegate method for notification response
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        logger.info("User responded to notification")
        let userInfo = response.notification.request.content.userInfo
        // Handle notification response (e.g., navigate to a specific screen)
        // You might want to emit another event here for the response
        completionHandler()
    }

    @objc public func requestPermissions(_ invoke: Invoke) {
        logger.info("Requesting notification permissions")
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) {
            granted, error in
            DispatchQueue.main.async {
                if let error = error {
                    logger.error("Permission request error: \(error.localizedDescription)")
                    invoke.reject("Permission request failed")
                    return
                }

                if granted {
                    logger.info("Notification permissions granted")
                    UIApplication.shared.registerForRemoteNotifications()
                } else {
                    logger.warning("Notification permissions denied")
                }

                let status = granted ? "granted" : "denied"
                invoke.resolve(["status": status])
            }
        }
    }

    @objc public func getToken(_ invoke: Invoke) {
        if let currentToken = self.token {
            logger.info("Returning cached token")
            invoke.resolve(["token": currentToken, "platform": "ios"])
        } else {
            logger.warning("Token not available yet")
            invoke.reject("Token not available")
            // Optionally trigger registration again if needed
            // UIApplication.shared.registerForRemoteNotifications()
        }
    }
}

@_cdecl("init_plugin_push_notifications")
func initPlugin() -> Plugin {
    return PushNotificationsPlugin()
}
