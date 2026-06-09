import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/timer_config.dart';
import '../models/sound_setting.dart';

class StorageService {
  static const _keyIntervalMinutes = 'intervalMinutes';
  static const _keyTotalMinutes = 'totalMinutes';
  static const _keySound = 'sound';

  Future<TimerConfig> loadTimerConfig() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final interval = prefs.getInt(_keyIntervalMinutes) ?? 5;
      final total = prefs.getInt(_keyTotalMinutes) ?? 30;
      return TimerConfig(
        intervalMinutes: interval.clamp(1, 60),
        totalMinutes: total.clamp(interval, 180),
      );
    } catch (_) {
      return const TimerConfig();
    }
  }

  Future<void> saveTimerConfig(TimerConfig config) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt(_keyIntervalMinutes, config.intervalMinutes);
    await prefs.setInt(_keyTotalMinutes, config.totalMinutes);
  }

  Future<SoundSetting> loadSoundSetting() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final json = prefs.getString(_keySound);
      if (json == null) return const SoundSetting();
      return SoundSetting.fromJson(
          jsonDecode(json) as Map<String, dynamic>);
    } catch (_) {
      return const SoundSetting();
    }
  }

  Future<void> saveSoundSetting(SoundSetting setting) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keySound, jsonEncode(setting.toJson()));
  }
}
