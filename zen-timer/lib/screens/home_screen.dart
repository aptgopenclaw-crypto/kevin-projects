import 'package:flutter/material.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../models/sound_setting.dart';
import '../services/audio_service.dart';
import '../services/foreground_task_service.dart';
import '../services/storage_service.dart';
import '../services/timer_service.dart';
import '../theme/app_theme.dart';
import '../widgets/countdown_display.dart';
import '../widgets/interval_settings.dart';
import '../widgets/sound_settings_modal.dart';
import '../widgets/timer_controls.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  final _storageService = StorageService();
  final _audioService = AudioService();
  late final TimerService _timerService;

  SoundSetting _soundSetting = const SoundSetting();

  @override
  void initState() {
    super.initState();
    _timerService = TimerService(audioService: _audioService);
    WidgetsBinding.instance.addObserver(this);
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final config = await _storageService.loadTimerConfig();
    final sound = await _storageService.loadSoundSetting();
    setState(() {
      _soundSetting = sound;
    });
    _timerService.updateConfig(config);
    _timerService.soundSetting = sound;
    _timerService.addListener(_onTimerChanged);
  }

  void _onTimerChanged() {
    final state = _timerService.state;

    // Wakelock
    if (state.isRunning) {
      WakelockPlus.enable();
    } else {
      WakelockPlus.disable();
    }

    // Foreground service notification
    if (state.isRunning) {
      final m = state.remainingSeconds ~/ 60;
      final s = state.remainingSeconds % 60;
      final timeStr =
          '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
      ForegroundTaskService.update(
        title: 'ZenTimer 運行中',
        body: '倒數 $timeStr  第 ${state.currentRound}/${state.totalRounds} 輪',
      );
    } else if (state.isCompleted) {
      ForegroundTaskService.stop();
      WakelockPlus.disable();
    }

    setState(() {});
  }

  Future<void> _onStart() async {
    final state = _timerService.state;
    if (state.isPaused) {
      _timerService.resume();
    } else {
      _timerService.start();
      await ForegroundTaskService.start(
        title: 'ZenTimer 運行中',
        body: '計時器已啟動',
      );
    }
  }

  void _onPause() => _timerService.pause();

  Future<void> _onStop() async {
    _timerService.stop();
    await ForegroundTaskService.stop();
  }

  void _onIntervalChanged(int v) {
    final newConfig = _timerService.config.copyWith(intervalMinutes: v);
    _timerService.updateConfig(newConfig);
    _storageService.saveTimerConfig(newConfig);
  }

  void _onTotalChanged(int v) {
    final newConfig = _timerService.config.copyWith(totalMinutes: v);
    _timerService.updateConfig(newConfig);
    _storageService.saveTimerConfig(newConfig);
  }

  void _openSoundSettings() {
    SoundSettingsModal.show(
      context,
      current: _soundSetting,
      audioService: _audioService,
      onChanged: (s) {
        setState(() => _soundSetting = s);
        _timerService.soundSetting = s;
        _storageService.saveSoundSetting(s);
      },
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _timerService.syncFromBackground();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timerService.removeListener(_onTimerChanged);
    _timerService.dispose();
    _audioService.dispose();
    WakelockPlus.disable();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final timerState = _timerService.state;
    final config = _timerService.config;
    final showSettings = timerState.isIdle || timerState.isCompleted;

    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Title
              const Text(
                'ZenTimer',
                style: TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.accent,
                  letterSpacing: 2,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                '瑜伽循環計時器',
                style: TextStyle(fontSize: 14, color: AppTheme.textDark),
              ),
              const SizedBox(height: 32),
              // Countdown ring
              CountdownDisplay(
                remainingSeconds: timerState.remainingSeconds,
                intervalSeconds: config.intervalMinutes * 60,
                currentRound: timerState.currentRound,
                totalRounds: timerState.totalRounds,
              ),
              const SizedBox(height: 32),
              // Completed message
              if (timerState.isCompleted)
                Container(
                  padding: const EdgeInsets.all(16),
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(
                    color: AppTheme.primary.withAlpha(30),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: AppTheme.primary),
                  ),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.self_improvement_rounded,
                          color: AppTheme.accent, size: 28),
                      SizedBox(width: 8),
                      Text(
                        '練習完成！',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.accent,
                        ),
                      ),
                    ],
                  ),
                ),
              // Settings (hidden while running/paused)
              if (showSettings) ...[
                IntervalSettings(
                  intervalMinutes: config.intervalMinutes,
                  totalMinutes: config.totalMinutes,
                  onIntervalChanged: _onIntervalChanged,
                  onTotalChanged: _onTotalChanged,
                ),
                const SizedBox(height: 24),
              ],
              // Controls
              TimerControls(
                isRunning: timerState.isRunning,
                isPaused: timerState.isPaused,
                isCompleted: timerState.isCompleted,
                onStart: _onStart,
                onPause: _onPause,
                onStop: _onStop,
                onOpenSound: _openSoundSettings,
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}
