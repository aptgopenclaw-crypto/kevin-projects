import 'dart:async';
import 'package:flutter/foundation.dart';
import '../models/timer_config.dart';
import '../models/timer_state.dart';
import '../models/sound_setting.dart';
import 'audio_service.dart';

class TimerService extends ChangeNotifier {
  final AudioService audioService;

  TimerService({required this.audioService});

  TimerConfig _config = const TimerConfig();
  late TimerState _state = TimerState.initial(
    _config.intervalMinutes,
    _config.totalMinutes,
  );

  Timer? _ticker;
  // Timestamp (epoch ms) when current round started (or unpaused)
  int? _roundStartTimestamp;
  // How many seconds were already elapsed in the current round before pausing
  int _elapsedBeforePause = 0;

  TimerConfig get config => _config;
  TimerState get state => _state;

  void updateConfig(TimerConfig config) {
    _config = config;
    if (_state.isIdle || _state.isCompleted) {
      _state = TimerState.initial(config.intervalMinutes, config.totalMinutes);
    }
    notifyListeners();
  }

  void start() {
    if (_state.isRunning) return;
    if (_state.isCompleted) {
      _state = TimerState.initial(_config.intervalMinutes, _config.totalMinutes);
    }

    _roundStartTimestamp = DateTime.now().millisecondsSinceEpoch;
    _elapsedBeforePause = 0;

    _state = _state.copyWith(
      isRunning: true,
      isPaused: false,
      isCompleted: false,
      startTimestamp: _roundStartTimestamp,
    );
    _startTicker();
    notifyListeners();
  }

  void pause() {
    if (!_state.isRunning) return;
    _elapsedBeforePause = _config.intervalMinutes * 60 - _state.remainingSeconds;
    _stopTicker();
    _state = _state.copyWith(isRunning: false, isPaused: true);
    notifyListeners();
  }

  void resume() {
    if (!_state.isPaused) return;
    _roundStartTimestamp = DateTime.now().millisecondsSinceEpoch;
    _state = _state.copyWith(
      isRunning: true,
      isPaused: false,
      startTimestamp: _roundStartTimestamp,
    );
    _startTicker();
    notifyListeners();
  }

  void stop() {
    _stopTicker();
    _elapsedBeforePause = 0;
    _roundStartTimestamp = null;
    _state = TimerState.initial(_config.intervalMinutes, _config.totalMinutes);
    notifyListeners();
  }

  /// Called when app comes back to foreground — recalculate from timestamp.
  void syncFromBackground() {
    if (!_state.isRunning || _roundStartTimestamp == null) return;
    final nowMs = DateTime.now().millisecondsSinceEpoch;
    final elapsedMs = nowMs - _roundStartTimestamp!;
    final totalElapsedSec =
        _elapsedBeforePause + (elapsedMs / 1000).floor();
    final intervalSec = _config.intervalMinutes * 60;

    int rounds = _state.currentRound + totalElapsedSec ~/ intervalSec;
    int remaining = intervalSec - (totalElapsedSec % intervalSec);

    if (rounds >= _state.totalRounds) {
      _stopTicker();
      _state = _state.copyWith(
        isRunning: false,
        isCompleted: true,
        currentRound: _state.totalRounds,
        remainingSeconds: 0,
        clearStartTimestamp: true,
      );
    } else {
      _state = _state.copyWith(
        currentRound: rounds,
        remainingSeconds: remaining,
      );
      _roundStartTimestamp = DateTime.now().millisecondsSinceEpoch;
      _elapsedBeforePause = intervalSec - remaining;
    }
    notifyListeners();
  }

  void _startTicker() {
    _stopTicker();
    _ticker = Timer.periodic(const Duration(seconds: 1), _onTick);
  }

  void _stopTicker() {
    _ticker?.cancel();
    _ticker = null;
  }

  void _onTick(Timer _) {
    if (_roundStartTimestamp == null) return;

    final nowMs = DateTime.now().millisecondsSinceEpoch;
    final elapsedMs = nowMs - _roundStartTimestamp!;
    final totalElapsedSec =
        _elapsedBeforePause + (elapsedMs / 1000).floor();
    final intervalSec = _config.intervalMinutes * 60;
    final remaining = intervalSec - (totalElapsedSec % intervalSec);
    final completedRoundsThisPeriod = totalElapsedSec ~/ intervalSec;
    final newCurrentRound =
        _state.currentRound + completedRoundsThisPeriod;

    if (completedRoundsThisPeriod > 0) {
      // New round(s) crossed — play sound and update round count
      audioService.play(_currentSoundSetting);
      _elapsedBeforePause = 0;
      _roundStartTimestamp = DateTime.now().millisecondsSinceEpoch;

      if (newCurrentRound >= _state.totalRounds) {
        _stopTicker();
        _state = _state.copyWith(
          isRunning: false,
          isCompleted: true,
          currentRound: _state.totalRounds,
          remainingSeconds: 0,
          clearStartTimestamp: true,
        );
        notifyListeners();
        return;
      }

      _state = _state.copyWith(
        currentRound: newCurrentRound,
        remainingSeconds: intervalSec,
      );
    } else {
      _state = _state.copyWith(remainingSeconds: remaining);
    }
    notifyListeners();
  }

  // Injected externally after build
  SoundSetting _currentSoundSetting = const SoundSetting();
  set soundSetting(SoundSetting s) => _currentSoundSetting = s;

  @override
  void dispose() {
    _stopTicker();
    super.dispose();
  }
}
