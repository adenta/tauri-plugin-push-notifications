use serde::Serialize;
use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime, AppHandle, command,
};

#[cfg(target_os = "ios")]
mod ios;
#[cfg(target_os = "android")]
mod android;

#[derive(Clone, Serialize)]
struct PushToken {
    token: String,
    platform: String,
}

#[command]
async fn get_token<R: Runtime>(app: AppHandle<R>) -> Result<PushToken, String> {
    #[cfg(target_os = "ios")]
    {
        // TODO: Implement iOS token retrieval
        Err("iOS token retrieval not implemented".to_string())
    }
    #[cfg(target_os = "android")]
    {
        android::get_token(app).await
    }
    #[cfg(not(any(target_os = "ios", target_os = "android")))]
    {
        Err("Unsupported platform".to_string())
    }
}

#[command]
async fn request_permissions<R: Runtime>(app: AppHandle<R>) -> Result<String, String> {
     #[cfg(target_os = "ios")]
    {
        ios::request_permissions(app).await
    }
    #[cfg(target_os = "android")]
    {
        android::request_permissions(app).await
    }
    #[cfg(not(any(target_os = "ios", target_os = "android")))]
    {
        Err("Unsupported platform".to_string())
    }
}


/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("push-notifications")
        .invoke_handler(tauri::generate_handler![
            get_token,
            request_permissions,
            // TODO: Add on_token_refresh handler if needed
        ])
        .setup(|app, _api| {
            #[cfg(target_os = "ios")]
            ios::init(app)?;
            #[cfg(target_os = "android")]
            android::init(app)?;
            Ok(())
        })
        .build()
}
