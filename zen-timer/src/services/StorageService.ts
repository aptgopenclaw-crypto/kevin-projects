import AsyncStorage from '@react-native-async-storage/async-storage';
import { DEFAULT_SETTINGS, STORAGE_KEY } from '../constants';
import { PersistedSettings } from '../types';

export async function loadSettings(): Promise<PersistedSettings> {
  try {
    const raw = await AsyncStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    const parsed = JSON.parse(raw) as Partial<PersistedSettings>;
    return {
      intervalMinutes: parsed.intervalMinutes ?? DEFAULT_SETTINGS.intervalMinutes,
      totalMinutes: parsed.totalMinutes ?? DEFAULT_SETTINGS.totalMinutes,
      sound: parsed.sound ?? DEFAULT_SETTINGS.sound,
    };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export async function saveSettings(settings: PersistedSettings): Promise<void> {
  try {
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  } catch (error) {
    console.warn('Failed to save settings:', error);
  }
}
