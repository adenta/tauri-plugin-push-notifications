import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

export interface PushToken {
  token: string;
  platform: 'ios' | 'android';
}

export interface PushNotification {
  data: Record<string, string>;
  notification?: {
    title?: string;
    body?: string;
  };
}

/**
 * Requests permission to receive push notifications.
 * @returns A promise resolving to an object indicating whether permission was granted.
 */
export async function requestPermissions(): Promise<{ granted: boolean }> {
  const result = await invoke<{ status: string }>('plugin:push-notifications|request_permissions');
  return { granted: result.status === 'granted' };
}

/**
 * Gets the current device token for push notifications.
 * @returns A promise resolving to the push token object.
 */
export async function getToken(): Promise<PushToken> {
  return await invoke<PushToken>('plugin:push-notifications|get_token');
}

/**
 * Listens for push notification token refresh events.
 * @param callback The function to call when the token is refreshed.
 * @returns A function to unlisten the event.
 */
export async function onTokenRefresh(callback: (token: PushToken) => void): Promise<() => void> {
  return await listen<PushToken>('push_token', (event) => {
    callback(event.payload);
  });
}

/**
 * Listens for incoming push notifications.
 * @param callback The function to call when a notification is received.
 * @returns A function to unlisten the event.
 */
export async function onNotificationReceived(callback: (notification: PushNotification) => void): Promise<() => void> {
  return await listen<PushNotification>('push_message', (event) => {
    callback(event.payload);
  });
}
