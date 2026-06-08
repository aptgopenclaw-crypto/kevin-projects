export interface TimerConfig {
  intervalMinutes: number; // 1–60
  totalMinutes: number;    // >= intervalMinutes, max 180
}

export interface TimerState {
  remainingSeconds: number;
  currentRound: number;        // 1-based, increments after each bell
  totalRounds: number;         // totalMinutes / intervalMinutes
  isRunning: boolean;
  isPaused: boolean;
  isCompleted: boolean;
  startTimestamp: number | null; // epoch ms when current round started (for bg sync)
  pausedAt: number | null;       // epoch ms when paused
}

export type BuiltinSoundId = 'singing_bowl' | 'wooden_fish' | 'chime';

export interface SoundSetting {
  type: 'builtin' | 'custom';
  builtinId: BuiltinSoundId;
  customUri: string | null;
  customFileName: string | null;
}

export interface PersistedSettings {
  intervalMinutes: number;
  totalMinutes: number;
  sound: SoundSetting;
}
