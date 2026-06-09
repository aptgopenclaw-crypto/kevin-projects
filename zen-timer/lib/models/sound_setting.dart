enum BuiltinSoundId { singingBowl, woodenFish, chime }

extension BuiltinSoundIdExt on BuiltinSoundId {
  String get assetPath {
    switch (this) {
      case BuiltinSoundId.singingBowl:
        return 'assets/sounds/singing_bowl.mp3';
      case BuiltinSoundId.woodenFish:
        return 'assets/sounds/wooden_fish.mp3';
      case BuiltinSoundId.chime:
        return 'assets/sounds/chime.mp3';
    }
  }

  String get displayName {
    switch (this) {
      case BuiltinSoundId.singingBowl:
        return '頌缽聲';
      case BuiltinSoundId.woodenFish:
        return '木魚聲';
      case BuiltinSoundId.chime:
        return '清脆叮聲';
    }
  }

  String get key {
    switch (this) {
      case BuiltinSoundId.singingBowl:
        return 'singing_bowl';
      case BuiltinSoundId.woodenFish:
        return 'wooden_fish';
      case BuiltinSoundId.chime:
        return 'chime';
    }
  }

  static BuiltinSoundId fromKey(String key) {
    switch (key) {
      case 'wooden_fish':
        return BuiltinSoundId.woodenFish;
      case 'chime':
        return BuiltinSoundId.chime;
      default:
        return BuiltinSoundId.singingBowl;
    }
  }
}

class SoundSetting {
  final bool isCustom;
  final BuiltinSoundId builtinId;
  final String? customUri;
  final String? customFileName;

  const SoundSetting({
    this.isCustom = false,
    this.builtinId = BuiltinSoundId.singingBowl,
    this.customUri,
    this.customFileName,
  });

  SoundSetting copyWith({
    bool? isCustom,
    BuiltinSoundId? builtinId,
    String? customUri,
    String? customFileName,
    bool clearCustom = false,
  }) {
    return SoundSetting(
      isCustom: isCustom ?? this.isCustom,
      builtinId: builtinId ?? this.builtinId,
      customUri: clearCustom ? null : (customUri ?? this.customUri),
      customFileName:
          clearCustom ? null : (customFileName ?? this.customFileName),
    );
  }

  Map<String, dynamic> toJson() => {
        'isCustom': isCustom,
        'builtinId': builtinId.key,
        'customUri': customUri,
        'customFileName': customFileName,
      };

  factory SoundSetting.fromJson(Map<String, dynamic> json) => SoundSetting(
        isCustom: (json['isCustom'] as bool?) ?? false,
        builtinId: BuiltinSoundIdExt.fromKey(
            (json['builtinId'] as String?) ?? 'singing_bowl'),
        customUri: json['customUri'] as String?,
        customFileName: json['customFileName'] as String?,
      );
}
