import React, { useState } from 'react';
import {
  ActivityIndicator,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { BUILTIN_SOUNDS, COLORS } from '../constants';
import { pickCustomSound, playAlertSound, stopAlertSound } from '../services/SoundService';
import { BuiltinSoundId, SoundSetting } from '../types';

interface SoundSettingsModalProps {
  visible: boolean;
  setting: SoundSetting;
  onClose: () => void;
  onChange: (setting: SoundSetting) => void;
}

export function SoundSettingsModal({
  visible,
  setting,
  onClose,
  onChange,
}: SoundSettingsModalProps) {
  const [isPreviewing, setIsPreviewing] = useState(false);
  const [isPicking, setIsPicking] = useState(false);

  const builtinIds: BuiltinSoundId[] = ['singing_bowl', 'wooden_fish', 'chime'];

  async function handlePreview() {
    if (isPreviewing) {
      await stopAlertSound();
      setIsPreviewing(false);
      return;
    }
    setIsPreviewing(true);
    await playAlertSound(setting);
    setIsPreviewing(false);
  }

  async function handlePickCustom() {
    setIsPicking(true);
    const newSetting = await pickCustomSound(setting);
    setIsPicking(false);
    if (newSetting) {
      onChange(newSetting);
    }
  }

  function selectBuiltin(id: BuiltinSoundId) {
    onChange({
      ...setting,
      type: 'builtin',
      builtinId: id,
    });
  }

  const selectedLabel =
    setting.type === 'custom'
      ? setting.customFileName ?? '自定義音效'
      : BUILTIN_SOUNDS[setting.builtinId].label;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          <Text style={styles.title}>音效設定</Text>

          <Text style={styles.sectionLabel}>內建音效</Text>
          {builtinIds.map((id) => {
            const isSelected = setting.type === 'builtin' && setting.builtinId === id;
            return (
              <TouchableOpacity
                key={id}
                style={[styles.option, isSelected && styles.optionSelected]}
                onPress={() => selectBuiltin(id)}
                accessibilityLabel={BUILTIN_SOUNDS[id].label}
              >
                <View style={[styles.radio, isSelected && styles.radioSelected]} />
                <Text style={[styles.optionText, isSelected && styles.optionTextSelected]}>
                  {BUILTIN_SOUNDS[id].label}
                </Text>
              </TouchableOpacity>
            );
          })}

          <Text style={styles.sectionLabel}>自定義音效</Text>
          <TouchableOpacity
            style={styles.pickButton}
            onPress={handlePickCustom}
            disabled={isPicking}
            accessibilityLabel="選擇本地音檔"
          >
            {isPicking ? (
              <ActivityIndicator color={COLORS.white} />
            ) : (
              <Text style={styles.pickButtonText}>選擇本地音檔（≤ 15 秒）</Text>
            )}
          </TouchableOpacity>
          {setting.type === 'custom' && setting.customFileName && (
            <View style={[styles.option, styles.optionSelected]}>
              <View style={[styles.radio, styles.radioSelected]} />
              <Text style={[styles.optionText, styles.optionTextSelected]} numberOfLines={1}>
                {setting.customFileName}
              </Text>
            </View>
          )}

          <View style={styles.divider} />

          <Text style={styles.currentLabel}>
            目前選擇：<Text style={styles.currentValue}>{selectedLabel}</Text>
          </Text>

          <View style={styles.actions}>
            <TouchableOpacity
              style={styles.previewButton}
              onPress={handlePreview}
              accessibilityLabel={isPreviewing ? '停止試聽' : '試聽'}
            >
              <Text style={styles.previewButtonText}>
                {isPreviewing ? '⏹ 停止試聽' : '▶ 試聽'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.closeButton}
              onPress={onClose}
              accessibilityLabel="關閉"
            >
              <Text style={styles.closeButtonText}>完成</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: COLORS.overlay,
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: COLORS.background,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
    paddingBottom: 40,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: COLORS.text,
    marginBottom: 20,
    textAlign: 'center',
  },
  sectionLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: COLORS.textLight,
    marginTop: 12,
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: COLORS.border,
    marginBottom: 8,
    backgroundColor: COLORS.white,
  },
  optionSelected: {
    borderColor: COLORS.primary,
    backgroundColor: '#F0F4E8',
  },
  radio: {
    width: 18,
    height: 18,
    borderRadius: 9,
    borderWidth: 2,
    borderColor: COLORS.border,
    marginRight: 12,
  },
  radioSelected: {
    borderColor: COLORS.primary,
    backgroundColor: COLORS.primary,
  },
  optionText: {
    fontSize: 16,
    color: COLORS.text,
    flex: 1,
  },
  optionTextSelected: {
    color: COLORS.primaryDark,
    fontWeight: '600',
  },
  pickButton: {
    backgroundColor: COLORS.primaryDark,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 8,
  },
  pickButtonText: {
    color: COLORS.white,
    fontSize: 16,
    fontWeight: '600',
  },
  divider: {
    height: 1,
    backgroundColor: COLORS.border,
    marginVertical: 16,
  },
  currentLabel: {
    fontSize: 14,
    color: COLORS.textLight,
    textAlign: 'center',
    marginBottom: 20,
  },
  currentValue: {
    color: COLORS.primaryDark,
    fontWeight: '700',
  },
  actions: {
    flexDirection: 'row',
    gap: 12,
  },
  previewButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: COLORS.primary,
  },
  previewButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.primaryDark,
  },
  closeButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    backgroundColor: COLORS.primary,
  },
  closeButtonText: {
    fontSize: 16,
    fontWeight: '700',
    color: COLORS.white,
  },
});
