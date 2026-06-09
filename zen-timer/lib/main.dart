import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'services/foreground_task_service.dart';
import 'screens/home_screen.dart';
import 'theme/app_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  ForegroundTaskService.init();
  runApp(const ZenTimerApp());
}

class ZenTimerApp extends StatelessWidget {
  const ZenTimerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return WithForegroundTask(
      child: MaterialApp(
        title: 'ZenTimer',
        theme: AppTheme.theme,
        debugShowCheckedModeBanner: false,
        home: const HomeScreen(),
      ),
    );
  }
}
