import { BuiltinSoundId, PersistedSettings, SoundSetting } from '../types';

export const COLORS = {
  background: '#F5F5DC',
  primary: '#8A9A5B',
  primaryDark: '#5C6B3E',
  text: '#333333',
  textLight: '#666666',
  white: '#FFFFFF',
  border: '#D4D4B0',
  danger: '#C0392B',
  overlay: 'rgba(0,0,0,0.5)',
} as const;

export const BUILTIN_SOUNDS: Record<BuiltinSoundId, { label: string; file: any }> = {
  singing_bowl: {
    label: '頌缽聲',
    file: require('../assets/sounds/singing_bowl.mp3'),
  },
  wooden_fish: {
    label: '木魚聲',
    file: require('../assets/sounds/wooden_fish.mp3'),
  },
  chime: {
    label: '清脆叮聲',
    file: require('../assets/sounds/chime.mp3'),
  },
};

export const DEFAULT_SOUND: SoundSetting = {
  type: 'builtin',
  builtinId: 'singing_bowl',
  customUri: null,
  customFileName: null,
};

export const DEFAULT_SETTINGS: PersistedSettings = {
  intervalMinutes: 5,
  totalMinutes: 30,
  sound: DEFAULT_SOUND,
};

export const STORAGE_KEY = '@zentimer_settings';

export const BACKGROUND_TASK_NAME = 'ZENTIMER_BACKGROUND';
export const NOTIFICATION_CHANNEL_ID = 'timer';
export const FOREGROUND_NOTIFICATION_ID = 1;

export const MAX_SOUND_DURATION_MS = 15000; // 15 seconds
