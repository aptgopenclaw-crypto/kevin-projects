import 'dart:io';
import 'package:audioplayers/audioplayers.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import '../models/sound_setting.dart';

class AudioService {
  final AudioPlayer _player = AudioPlayer();

  Future<void> play(SoundSetting setting) async {
    try {
      await _player.stop();
      if (setting.isCustom && setting.customUri != null) {
        final file = File(setting.customUri!);
        if (await file.exists()) {
          await _player.play(DeviceFileSource(setting.customUri!));
          return;
        }
        // Fall back to default
      }
      await _player.play(AssetSource(
        setting.builtinId.assetPath.replaceFirst('assets/', ''),
      ));
    } catch (_) {
      // Fall back to singing bowl
      await _player.play(
          AssetSource('sounds/singing_bowl.mp3'));
    }
  }

  Future<void> stop() async {
    await _player.stop();
  }

  void dispose() {
    _player.dispose();
  }

  /// Copies a picked file into app internal storage.
  /// Returns the internal file path, or null on failure.
  Future<String?> copyToInternalStorage(String sourcePath) async {
    try {
      final dir = await getApplicationDocumentsDirectory();
      final filename = p.basename(sourcePath);
      final dest = p.join(dir.path, 'custom_sounds', filename);
      await Directory(p.dirname(dest)).create(recursive: true);
      await File(sourcePath).copy(dest);
      return dest;
    } catch (_) {
      return null;
    }
  }

  /// Returns duration of a file in seconds, or null on error.
  Future<double?> getFileDuration(String filePath) async {
    final probe = AudioPlayer();
    try {
      await probe.setSource(DeviceFileSource(filePath));
      final duration = await probe.getDuration();
      return duration?.inMilliseconds.toDouble() != null
          ? duration!.inMilliseconds / 1000.0
          : null;
    } catch (_) {
      return null;
    } finally {
      await probe.dispose();
    }
  }
}
