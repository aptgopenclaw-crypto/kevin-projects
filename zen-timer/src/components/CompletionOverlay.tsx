import React from 'react';
import { Modal, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { COLORS } from '../constants';

interface CompletionOverlayProps {
  visible: boolean;
  totalRounds: number;
  onDismiss: () => void;
}

export function CompletionOverlay({
  visible,
  totalRounds,
  onDismiss,
}: CompletionOverlayProps) {
  return (
    <Modal visible={visible} transparent animationType="fade">
      <View style={styles.overlay}>
        <View style={styles.card}>
          <Text style={styles.emoji}>🧘</Text>
          <Text style={styles.title}>練習完成！</Text>
          <Text style={styles.subtitle}>
            已完成 {totalRounds} 次提示音{'\n'}辛苦了，好好休息。
          </Text>
          <TouchableOpacity
            style={styles.button}
            onPress={onDismiss}
            accessibilityLabel="返回"
          >
            <Text style={styles.buttonText}>返回設定</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: COLORS.overlay,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  card: {
    backgroundColor: COLORS.background,
    borderRadius: 24,
    padding: 40,
    alignItems: 'center',
    width: '100%',
  },
  emoji: {
    fontSize: 64,
    marginBottom: 16,
  },
  title: {
    fontSize: 28,
    fontWeight: '800',
    color: COLORS.primaryDark,
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 18,
    color: COLORS.textLight,
    textAlign: 'center',
    lineHeight: 26,
    marginBottom: 32,
  },
  button: {
    backgroundColor: COLORS.primary,
    paddingVertical: 16,
    paddingHorizontal: 40,
    borderRadius: 14,
    width: '100%',
    alignItems: 'center',
  },
  buttonText: {
    color: COLORS.white,
    fontSize: 18,
    fontWeight: '700',
  },
});
