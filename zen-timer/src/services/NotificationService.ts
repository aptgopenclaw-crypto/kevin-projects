import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import {
  FOREGROUND_NOTIFICATION_ID,
  NOTIFICATION_CHANNEL_ID,
} from '../constants';

export async function setupNotifications(): Promise<void> {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: false,
      shouldSetBadge: false,
    }),
  });

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync(NOTIFICATION_CHANNEL_ID, {
      name: 'ZenTimer',
      importance: Notifications.AndroidImportance.LOW,
      sound: null,
      vibrationPattern: null,
      enableVibrate: false,
      showBadge: false,
    });
  }
}

export async function requestNotificationPermissions(): Promise<boolean> {
  const { status } = await Notifications.requestPermissionsAsync();
  return status === 'granted';
}

export async function showForegroundNotification(
  remainingSeconds: number,
  currentRound: number,
  totalRounds: number
): Promise<void> {
  const mm = String(Math.floor(remainingSeconds / 60)).padStart(2, '0');
  const ss = String(remainingSeconds % 60).padStart(2, '0');

  await Notifications.scheduleNotificationAsync({
    identifier: String(FOREGROUND_NOTIFICATION_ID),
    content: {
      title: 'ZenTimer 運行中',
      body: `第 ${currentRound} / ${totalRounds} 次 — ${mm}:${ss}`,
      sticky: true,
      autoDismiss: false,
      data: {},
    },
    trigger: null,
  });
}

export async function dismissForegroundNotification(): Promise<void> {
  await Notifications.dismissNotificationAsync(
    String(FOREGROUND_NOTIFICATION_ID)
  );
  await Notifications.dismissAllNotificationsAsync();
}

export async function showCompletionNotification(): Promise<void> {
  await Notifications.scheduleNotificationAsync({
    content: {
      title: '🧘 練習完成！',
      body: '所有輪次已結束，休息一下吧。',
      data: {},
    },
    trigger: null,
  });
}
