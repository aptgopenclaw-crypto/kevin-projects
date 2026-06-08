import React, { useEffect, useRef, useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { CircularProgress } from './src/components/CircularProgress';
import { CompletionOverlay } from './src/components/CompletionOverlay';
import { SoundSettingsModal } from './src/components/SoundSettingsModal';
import { COLORS, DEFAULT_SETTINGS } from './src/constants';
import { useTimer } from './src/hooks/useTimer';
import {
  requestNotificationPermissions,
  setupNotifications,
} from './src/services/NotificationService';
import { initAudio } from './src/services/SoundService';
import { loadSettings, saveSettings } from './src/services/StorageService';
import { PersistedSettings, SoundSetting } from './src/types';

export default function App() {
  const [intervalMinutes, setIntervalMinutes] = useState(
    DEFAULT_SETTINGS.intervalMinutes
  );
  const [totalMinutes, setTotalMinutes] = useState(
    DEFAULT_SETTINGS.totalMinutes
  );
  const [sound, setSound] = useState<SoundSetting>(DEFAULT_SETTINGS.sound);
  const [soundModalVisible, setSoundModalVisible] = useState(false);
  const [settingsLoaded, setSettingsLoaded] = useState(false);

  const { state, start, pause, stop } = useTimer({
    intervalMinutes,
    totalMinutes,
    sound,
  });

  const isActive = state.isRunning || state.isPaused;
  const totalRounds = Math.floor(totalMinutes / intervalMinutes);
  const progress = state.remainingSeconds / (intervalMinutes * 60);

  const mm = String(Math.floor(state.remainingSeconds / 60)).padStart(2, '0');
  const ss = String(state.remainingSeconds % 60).padStart(2, '0');

  // Load persisted settings on mount
  useEffect(() => {
    async function init() {
      await setupNotifications();
      await requestNotificationPermissions();
      await initAudio();
      const saved = await loadSettings();
      setIntervalMinutes(saved.intervalMinutes);
      setTotalMinutes(saved.totalMinutes);
      setSound(saved.sound);
      setSettingsLoaded(true);
    }
    init();
  }, []);

  // Save settings whenever they change (debounced)
  const saveTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    if (!settingsLoaded) return;
    if (saveTimeout.current) clearTimeout(saveTimeout.current);
    saveTimeout.current = setTimeout(() => {
      const settings: PersistedSettings = { intervalMinutes, totalMinutes, sound };
      saveSettings(settings);
    }, 500);
    return () => {
      if (saveTimeout.current) clearTimeout(saveTimeout.current);
    };
  }, [intervalMinutes, totalMinutes, sound, settingsLoaded]);

  function adjustInterval(delta: number) {
    if (isActive) return;
    setIntervalMinutes((prev) => {
      const next = Math.max(1, Math.min(60, prev + delta));
      setTotalMinutes((t) => Math.max(t, next));
      return next;
    });
  }

  function adjustTotal(delta: number) {
    if (isActive) return;
    setTotalMinutes((prev) =>
      Math.max(intervalMinutes, Math.min(180, prev + delta))
    );
  }

  function handleStop() {
    stop();
  }

  function handleDismissCompletion() {
    stop();
  }

  function handleSoundChange(newSetting: SoundSetting) {
    setSound(newSetting);
  }

  if (!settingsLoaded) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>ZenTimer</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor={COLORS.background} />

      <ScrollView
        contentContainerStyle={styles.scroll}
        scrollEnabled={!isActive}
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.appTitle}>ZenTimer</Text>
          <TouchableOpacity
            style={styles.gearButton}
            onPress={() => setSoundModalVisible(true)}
            disabled={state.isRunning}
            accessibilityLabel="音效設定"
          >
            <Text style={styles.gearIcon}>⚙️</Text>
          </TouchableOpacity>
        </View>

        {/* Round counter */}
        <View style={styles.roundContainer}>
          <Text style={styles.roundText}>
            {state.isCompleted
              ? `已完成 ${state.totalRounds} 次`
              : `第 ${state.currentRound} / ${totalRounds} 次`}
          </Text>
        </View>

        {/* Circular progress + timer display */}
        <View style={styles.timerWrapper}>
          <CircularProgress
            size={260}
            strokeWidth={14}
            progress={progress}
          />
          <View style={styles.timerOverlay}>
            <Text style={styles.timerText}>{mm}:{ss}</Text>
            <Text style={styles.timerSubText}>
              間隔 {intervalMinutes} 分鐘
            </Text>
          </View>
        </View>

        {/* Settings area (hidden while running) */}
        {!isActive && (
          <View style={styles.settingsSection}>
            {/* Interval time */}
            <View style={styles.settingRow}>
              <Text style={styles.settingLabel}>間隔時間（分鐘）</Text>
              <View style={styles.quickButtons}>
                {[1, 3, 5, 10].map((v) => (
                  <TouchableOpacity
                    key={v}
                    style={[
                      styles.quickBtn,
                      intervalMinutes === v && styles.quickBtnActive,
                    ]}
                    onPress={() => {
                      setIntervalMinutes(v);
                      setTotalMinutes((t) => Math.max(t, v));
                    }}
                    accessibilityLabel={`間隔 ${v} 分鐘`}
                  >
                    <Text
                      style={[
                        styles.quickBtnText,
                        intervalMinutes === v && styles.quickBtnTextActive,
                      ]}
                    >
                      {v}分
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
              <View style={styles.stepper}>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => adjustInterval(-1)}
                  accessibilityLabel="減少間隔時間"
                >
                  <Text style={styles.stepBtnText}>−</Text>
                </TouchableOpacity>
                <Text style={styles.stepValue}>{intervalMinutes} 分</Text>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => adjustInterval(1)}
                  accessibilityLabel="增加間隔時間"
                >
                  <Text style={styles.stepBtnText}>+</Text>
                </TouchableOpacity>
              </View>
            </View>

            {/* Total time */}
            <View style={styles.settingRow}>
              <Text style={styles.settingLabel}>
                總練習時長（預計 {totalRounds} 次提示音）
              </Text>
              <View style={styles.quickButtons}>
                {[15, 30, 45, 60].map((v) => (
                  <TouchableOpacity
                    key={v}
                    style={[
                      styles.quickBtn,
                      totalMinutes === v && styles.quickBtnActive,
                    ]}
                    onPress={() =>
                      setTotalMinutes(Math.max(intervalMinutes, v))
                    }
                    accessibilityLabel={`總時長 ${v} 分鐘`}
                  >
                    <Text
                      style={[
                        styles.quickBtnText,
                        totalMinutes === v && styles.quickBtnTextActive,
                      ]}
                    >
                      {v}分
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
              <View style={styles.stepper}>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => adjustTotal(-1)}
                  accessibilityLabel="減少總時長"
                >
                  <Text style={styles.stepBtnText}>−</Text>
                </TouchableOpacity>
                <Text style={styles.stepValue}>{totalMinutes} 分</Text>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => adjustTotal(1)}
                  accessibilityLabel="增加總時長"
                >
                  <Text style={styles.stepBtnText}>+</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        )}
      </ScrollView>

      {/* Control buttons (always visible) */}
      <View style={styles.controls}>
        {!state.isRunning && !state.isPaused && (
          <TouchableOpacity
            style={styles.startButton}
            onPress={start}
            accessibilityLabel="開始計時"
          >
            <Text style={styles.startButtonText}>▶ 開始</Text>
          </TouchableOpacity>
        )}
        {state.isRunning && (
          <>
            <TouchableOpacity
              style={styles.pauseButton}
              onPress={pause}
              accessibilityLabel="暫停"
            >
              <Text style={styles.pauseButtonText}>⏸ 暫停</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.stopButton}
              onPress={handleStop}
              accessibilityLabel="停止"
            >
              <Text style={styles.stopButtonText}>⏹ 停止</Text>
            </TouchableOpacity>
          </>
        )}
        {state.isPaused && (
          <>
            <TouchableOpacity
              style={styles.startButton}
              onPress={start}
              accessibilityLabel="繼續計時"
            >
              <Text style={styles.startButtonText}>▶ 繼續</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.stopButton}
              onPress={handleStop}
              accessibilityLabel="停止"
            >
              <Text style={styles.stopButtonText}>⏹ 停止</Text>
            </TouchableOpacity>
          </>
        )}
      </View>

      {/* Modals */}
      <SoundSettingsModal
        visible={soundModalVisible}
        setting={sound}
        onClose={() => setSoundModalVisible(false)}
        onChange={handleSoundChange}
      />

      <CompletionOverlay
        visible={state.isCompleted}
        totalRounds={state.totalRounds}
        onDismiss={handleDismissCompletion}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    backgroundColor: COLORS.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingText: {
    fontSize: 32,
    fontWeight: '800',
    color: COLORS.primary,
    letterSpacing: 2,
  },
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
  },
  scroll: {
    flexGrow: 1,
    paddingBottom: 12,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 20,
    paddingHorizontal: 24,
    marginBottom: 8,
  },
  appTitle: {
    fontSize: 26,
    fontWeight: '800',
    color: COLORS.primaryDark,
    letterSpacing: 2,
    flex: 1,
    textAlign: 'center',
  },
  gearButton: {
    position: 'absolute',
    right: 24,
    padding: 8,
  },
  gearIcon: {
    fontSize: 24,
  },
  roundContainer: {
    alignItems: 'center',
    marginBottom: 16,
  },
  roundText: {
    fontSize: 18,
    fontWeight: '600',
    color: COLORS.primaryDark,
    letterSpacing: 0.5,
  },
  timerWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 8,
  },
  timerOverlay: {
    position: 'absolute',
    alignItems: 'center',
  },
  timerText: {
    fontSize: 62,
    fontWeight: '800',
    color: COLORS.text,
    letterSpacing: 4,
  },
  timerSubText: {
    fontSize: 14,
    color: COLORS.textLight,
    marginTop: 4,
  },
  settingsSection: {
    paddingHorizontal: 20,
    marginTop: 16,
    gap: 16,
  },
  settingRow: {
    backgroundColor: COLORS.white,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: COLORS.border,
    gap: 12,
  },
  settingLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: COLORS.textLight,
  },
  quickButtons: {
    flexDirection: 'row',
    gap: 8,
  },
  quickBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: COLORS.border,
    backgroundColor: COLORS.white,
  },
  quickBtnActive: {
    borderColor: COLORS.primary,
    backgroundColor: '#F0F4E8',
  },
  quickBtnText: {
    fontSize: 14,
    fontWeight: '600',
    color: COLORS.textLight,
  },
  quickBtnTextActive: {
    color: COLORS.primaryDark,
  },
  stepper: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  stepBtn: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: COLORS.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepBtnText: {
    fontSize: 24,
    fontWeight: '700',
    color: COLORS.white,
    lineHeight: 28,
  },
  stepValue: {
    fontSize: 20,
    fontWeight: '700',
    color: COLORS.text,
    minWidth: 80,
    textAlign: 'center',
  },
  controls: {
    flexDirection: 'row',
    gap: 12,
    paddingHorizontal: 20,
    paddingVertical: 16,
    paddingBottom: 24,
    backgroundColor: COLORS.background,
    borderTopWidth: 1,
    borderTopColor: COLORS.border,
  },
  startButton: {
    flex: 1,
    backgroundColor: COLORS.primary,
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
  },
  startButtonText: {
    color: COLORS.white,
    fontSize: 20,
    fontWeight: '800',
    letterSpacing: 1,
  },
  pauseButton: {
    flex: 1,
    backgroundColor: COLORS.white,
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: COLORS.primary,
  },
  pauseButtonText: {
    color: COLORS.primaryDark,
    fontSize: 18,
    fontWeight: '700',
  },
  stopButton: {
    width: 72,
    backgroundColor: COLORS.white,
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: COLORS.border,
  },
  stopButtonText: {
    color: COLORS.textLight,
    fontSize: 16,
    fontWeight: '700',
  },
});
