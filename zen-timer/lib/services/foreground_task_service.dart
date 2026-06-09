import 'package:flutter_foreground_task/flutter_foreground_task.dart';

class ForegroundTaskService {
  static void init() {
    FlutterForegroundTask.init(
      androidNotificationOptions: AndroidNotificationOptions(
        channelId: 'zen_timer_channel',
        channelName: 'ZenTimer',
        channelDescription: '瑜伽計時器背景運行',
        channelImportance: NotificationChannelImportance.LOW,
        priority: NotificationPriority.LOW,
      ),
      iosNotificationOptions: const IOSNotificationOptions(
        showNotification: false,
        playSound: false,
      ),
      foregroundTaskOptions: ForegroundTaskOptions(
        eventAction: ForegroundTaskEventAction.repeat(1000),
        autoRunOnBoot: false,
        allowWifiLock: false,
      ),
    );
  }

  static Future<void> start({
    required String title,
    required String body,
  }) async {
    if (await FlutterForegroundTask.isRunningService) {
      await FlutterForegroundTask.updateService(
        notificationTitle: title,
        notificationText: body,
      );
    } else {
      await FlutterForegroundTask.startService(
        notificationTitle: title,
        notificationText: body,
        callback: _startTaskCallback,
      );
    }
  }

  static Future<void> update({
    required String title,
    required String body,
  }) async {
    await FlutterForegroundTask.updateService(
      notificationTitle: title,
      notificationText: body,
    );
  }

  static Future<void> stop() async {
    await FlutterForegroundTask.stopService();
  }

  static Future<bool> get isRunning =>
      FlutterForegroundTask.isRunningService;
}

@pragma('vm:entry-point')
void _startTaskCallback() {
  FlutterForegroundTask.setTaskHandler(_ZenTimerTaskHandler());
}

class _ZenTimerTaskHandler extends TaskHandler {
  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {}

  @override
  void onRepeatEvent(DateTime timestamp) {}

  @override
  Future<void> onDestroy(DateTime timestamp) async {}
}
