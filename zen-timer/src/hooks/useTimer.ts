import { useCallback, useEffect, useRef, useState } from 'react';
import { AppState, AppStateStatus } from 'react-native';
import { activateKeepAwakeAsync, deactivateKeepAwake } from 'expo-keep-awake';
import {
  dismissForegroundNotification,
  showCompletionNotification,
  showForegroundNotification,
} from '../services/NotificationService';
import { playAlertSound } from '../services/SoundService';
import { SoundSetting, TimerState } from '../types';

const TICK_INTERVAL_MS = 500; // tick every 500ms, derive seconds from timestamps

interface UseTimerOptions {
  intervalMinutes: number;
  totalMinutes: number;
  sound: SoundSetting;
}

interface UseTimerReturn {
  state: TimerState;
  start: () => void;
  pause: () => void;
  stop: () => void;
}

export function useTimer({
  intervalMinutes,
  totalMinutes,
  sound,
}: UseTimerOptions): UseTimerReturn {
  const totalRounds = Math.floor(totalMinutes / intervalMinutes);
  const intervalSeconds = intervalMinutes * 60;

  const [state, setState] = useState<TimerState>({
    remainingSeconds: intervalSeconds,
    currentRound: 1,
    totalRounds,
    isRunning: false,
    isPaused: false,
    isCompleted: false,
    startTimestamp: null,
    pausedAt: null,
  });

  // Keep refs for values used inside the interval callback
  const stateRef = useRef(state);
  stateRef.current = state;

  const soundRef = useRef(sound);
  soundRef.current = sound;

  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const appStateRef = useRef<AppStateStatus>(AppState.currentState);

  // Sync totalRounds when config changes (only when stopped)
  useEffect(() => {
    if (!stateRef.current.isRunning && !stateRef.current.isPaused) {
      setState((prev) => ({
        ...prev,
        remainingSeconds: intervalMinutes * 60,
        totalRounds: Math.floor(totalMinutes / intervalMinutes),
        currentRound: 1,
        isCompleted: false,
      }));
    }
  }, [intervalMinutes, totalMinutes]);

  const stopInterval = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const tick = useCallback(() => {
    const s = stateRef.current;
    if (!s.isRunning || s.isPaused || s.startTimestamp === null) return;

    // Calculate remaining from timestamp for accuracy
    const elapsed = Math.floor((Date.now() - s.startTimestamp) / 1000);
    const configIntervalSecs = intervalMinutes * 60;
    const remaining = Math.max(0, configIntervalSecs - elapsed);

    if (remaining > 0) {
      setState((prev) => ({ ...prev, remainingSeconds: remaining }));
      // Update notification every ~5 seconds to reduce overhead
      if (elapsed % 5 === 0) {
        showForegroundNotification(
          remaining,
          s.currentRound,
          s.totalRounds
        ).catch(() => {});
      }
      return;
    }

    // Round complete
    const nextRound = s.currentRound + 1;
    playAlertSound(soundRef.current).catch(() => {});

    if (nextRound > s.totalRounds) {
      // All rounds done
      stopInterval();
      deactivateKeepAwake().catch(() => {});
      dismissForegroundNotification().catch(() => {});
      showCompletionNotification().catch(() => {});
      setState((prev) => ({
        ...prev,
        remainingSeconds: 0,
        isRunning: false,
        isPaused: false,
        isCompleted: true,
        startTimestamp: null,
      }));
    } else {
      // Start next round
      const newStart = Date.now();
      setState((prev) => ({
        ...prev,
        remainingSeconds: configIntervalSecs,
        currentRound: nextRound,
        startTimestamp: newStart,
      }));
    }
  }, [intervalMinutes, stopInterval]);

  const startInterval = useCallback(() => {
    stopInterval();
    intervalRef.current = setInterval(tick, TICK_INTERVAL_MS);
  }, [tick, stopInterval]);

  const start = useCallback(() => {
    const now = Date.now();
    activateKeepAwakeAsync().catch(() => {});
    setState((prev) => ({
      ...prev,
      isRunning: true,
      isPaused: false,
      isCompleted: false,
      startTimestamp: now,
      pausedAt: null,
    }));
    startInterval();
  }, [startInterval]);

  const pause = useCallback(() => {
    stopInterval();
    deactivateKeepAwake().catch(() => {});
    setState((prev) => ({
      ...prev,
      isPaused: true,
      isRunning: false,
      pausedAt: Date.now(),
    }));
    dismissForegroundNotification().catch(() => {});
  }, [stopInterval]);

  const stop = useCallback(() => {
    stopInterval();
    deactivateKeepAwake().catch(() => {});
    dismissForegroundNotification().catch(() => {});
    setState({
      remainingSeconds: intervalMinutes * 60,
      currentRound: 1,
      totalRounds: Math.floor(totalMinutes / intervalMinutes),
      isRunning: false,
      isPaused: false,
      isCompleted: false,
      startTimestamp: null,
      pausedAt: null,
    });
  }, [stopInterval, intervalMinutes, totalMinutes]);

  // Handle app going to background/foreground
  useEffect(() => {
    const subscription = AppState.addEventListener(
      'change',
      (nextState: AppStateStatus) => {
        const prevState = appStateRef.current;
        appStateRef.current = nextState;

        const s = stateRef.current;

        if (
          prevState === 'background' &&
          nextState === 'active' &&
          s.isRunning &&
          s.startTimestamp !== null
        ) {
          // Recalculate how many rounds may have passed while in background
          const configIntervalSecs = intervalMinutes * 60;
          const elapsed = Math.floor((Date.now() - s.startTimestamp) / 1000);
          const roundsElapsed = Math.floor(elapsed / configIntervalSecs);
          const remainingInRound =
            configIntervalSecs - (elapsed % configIntervalSecs);
          const newRound = Math.min(
            s.currentRound + roundsElapsed,
            s.totalRounds
          );

          if (s.currentRound + roundsElapsed > s.totalRounds) {
            // Completed while in background
            stopInterval();
            deactivateKeepAwake().catch(() => {});
            dismissForegroundNotification().catch(() => {});
            setState((prev) => ({
              ...prev,
              remainingSeconds: 0,
              isRunning: false,
              isPaused: false,
              isCompleted: true,
              startTimestamp: null,
            }));
          } else {
            // Sync state and restart interval with corrected timestamp
            const newStart = Date.now() - (elapsed % configIntervalSecs) * 1000;
            setState((prev) => ({
              ...prev,
              currentRound: newRound,
              remainingSeconds: remainingInRound,
              startTimestamp: newStart,
            }));
            startInterval();
          }
        }

        if (nextState === 'background' && s.isRunning) {
          // Keep interval running; Android Foreground Service notification keeps process alive
          showForegroundNotification(
            s.remainingSeconds,
            s.currentRound,
            s.totalRounds
          ).catch(() => {});
        }
      }
    );

    return () => subscription.remove();
  }, [intervalMinutes, startInterval, stopInterval]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopInterval();
      deactivateKeepAwake().catch(() => {});
    };
  }, [stopInterval]);

  return { state, start, pause, stop };
}
