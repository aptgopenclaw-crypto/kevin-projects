import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import '../models/sound_setting.dart';
import '../services/audio_service.dart';
import '../theme/app_theme.dart';

class SoundSettingsModal extends StatefulWidget {
  final SoundSetting current;
  final AudioService audioService;
  final ValueChanged<SoundSetting> onChanged;

  const SoundSettingsModal({
    super.key,
    required this.current,
    required this.audioService,
    required this.onChanged,
  });

  static Future<void> show(
    BuildContext context, {
    required SoundSetting current,
    required AudioService audioService,
    required ValueChanged<SoundSetting> onChanged,
  }) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.background,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => SoundSettingsModal(
        current: current,
        audioService: audioService,
        onChanged: onChanged,
      ),
    );
  }

  @override
  State<SoundSettingsModal> createState() => _SoundSettingsModalState();
}

class _SoundSettingsModalState extends State<SoundSettingsModal> {
  late SoundSetting _setting;
  String? _statusMessage;

  @override
  void initState() {
    super.initState();
    _setting = widget.current;
  }

  void _save() {
    widget.onChanged(_setting);
    Navigator.of(context).pop();
  }

  Future<void> _pickFile() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['mp3', 'wav'],
    );
    if (result == null || result.files.isEmpty) return;
    final file = result.files.first;
    if (file.path == null) return;

    // Check duration ≤ 15s
    final duration = await widget.audioService.getFileDuration(file.path!);
    if (duration != null && duration > 15.0) {
      setState(() => _statusMessage = '音效檔案須在 15 秒以內（目前 ${duration.toStringAsFixed(1)} 秒）');
      return;
    }

    final internalPath =
        await widget.audioService.copyToInternalStorage(file.path!);
    if (internalPath == null) {
      setState(() => _statusMessage = '複製音效失敗，請重試');
      return;
    }

    setState(() {
      _statusMessage = null;
      _setting = _setting.copyWith(
        isCustom: true,
        customUri: internalPath,
        customFileName: file.name,
      );
    });
  }

  Future<void> _preview() async {
    await widget.audioService.play(_setting);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 24,
        right: 24,
        top: 24,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '音效設定',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: AppTheme.textDark,
            ),
          ),
          const SizedBox(height: 16),
          // Built-in options
          RadioGroup<String>(
            groupValue: _setting.isCustom ? '__custom__' : _setting.builtinId.key,
            onChanged: (val) {
              if (val == '__custom__') {
                _pickFile();
                return;
              }
              setState(() {
                _setting = _setting.copyWith(
                  isCustom: false,
                  builtinId: BuiltinSoundIdExt.fromKey(val!),
                );
              });
            },
            child: Column(
              children: [
                ...BuiltinSoundId.values.map((id) => RadioListTile<String>(
                      value: id.key,
                      title: Text(id.displayName),
                      activeColor: AppTheme.primary,
                    )),
                // Custom option
                RadioListTile<String>(
                  value: '__custom__',
                  title: Text(
                    _setting.isCustom && _setting.customFileName != null
                        ? _setting.customFileName!
                        : '自定義音效…',
                    style: TextStyle(
                      color: _setting.isCustom
                          ? AppTheme.accent
                          : AppTheme.textDark,
                    ),
                  ),
                  activeColor: AppTheme.primary,
                ),
              ],
            ),
          ),
          if (_statusMessage != null)
            Padding(
              padding: const EdgeInsets.only(left: 16, top: 4),
              child: Text(
                _statusMessage!,
                style:
                    const TextStyle(color: Colors.redAccent, fontSize: 13),
              ),
            ),
          const SizedBox(height: 16),
          Row(
            children: [
              OutlinedButton.icon(
                onPressed: _preview,
                icon: const Icon(Icons.volume_up_rounded),
                label: const Text('試聽'),
              ),
              const Spacer(),
              ElevatedButton(
                onPressed: _save,
                child: const Text('確認'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
