import { Audio } from 'expo-av';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { Alert } from 'react-native';
import { BUILTIN_SOUNDS, MAX_SOUND_DURATION_MS } from '../constants';
import { SoundSetting } from '../types';

let currentSound: Audio.Sound | null = null;

export async function initAudio(): Promise<void> {
  await Audio.setAudioModeAsync({
    allowsRecordingIOS: false,
    staysActiveInBackground: true,
    playsInSilentModeIOS: false,
    shouldDuckAndroid: false,
    playThroughEarpieceAndroid: false,
  });
}

export async function playAlertSound(setting: SoundSetting): Promise<void> {
  try {
    await stopAlertSound();

    let soundObject: Audio.Sound;

    if (setting.type === 'builtin') {
      const { sound } = await Audio.Sound.createAsync(
        BUILTIN_SOUNDS[setting.builtinId].file
      );
      soundObject = sound;
    } else if (setting.customUri) {
      const { sound } = await Audio.Sound.createAsync(
        { uri: setting.customUri },
        {},
        (status) => {
          if (status.isLoaded && status.didJustFinish) {
            soundObject.unloadAsync();
          }
        }
      );
      soundObject = sound;
    } else {
      // Fallback to default builtin
      const { sound } = await Audio.Sound.createAsync(
        BUILTIN_SOUNDS['singing_bowl'].file
      );
      soundObject = sound;
    }

    currentSound = soundObject;
    await soundObject.playAsync();
  } catch (error) {
    console.warn('Failed to play alert sound:', error);
    // Silently fail — do not crash the timer
  }
}

export async function stopAlertSound(): Promise<void> {
  if (currentSound) {
    try {
      await currentSound.stopAsync();
      await currentSound.unloadAsync();
    } catch (_) {
      // ignore
    }
    currentSound = null;
  }
}

/**
 * Opens document picker, validates duration ≤ 15s, copies to app storage.
 * Returns updated SoundSetting or null if cancelled/invalid.
 */
export async function pickCustomSound(
  currentSetting: SoundSetting
): Promise<SoundSetting | null> {
  try {
    const result = await DocumentPicker.getDocumentAsync({
      type: ['audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/*'],
      copyToCacheDirectory: true,
    });

    if (result.canceled || !result.assets || result.assets.length === 0) {
      return null;
    }

    const asset = result.assets[0];
    const tempUri = asset.uri;
    const fileName = asset.name ?? 'custom_sound';

    // Validate duration by loading the sound
    const durationMs = await getSoundDurationMs(tempUri);
    if (durationMs !== null && durationMs > MAX_SOUND_DURATION_MS) {
      Alert.alert('音效檔案過長', '音效檔案須在 15 秒以內，請重新選擇。');
      return null;
    }

    // Copy to permanent app storage
    const destDir = FileSystem.documentDirectory + 'sounds/';
    await FileSystem.makeDirectoryAsync(destDir, { intermediates: true });
    const destUri = destDir + fileName;
    await FileSystem.copyAsync({ from: tempUri, to: destUri });

    // Remove previously saved custom file if different
    if (
      currentSetting.type === 'custom' &&
      currentSetting.customUri &&
      currentSetting.customUri !== destUri
    ) {
      try {
        await FileSystem.deleteAsync(currentSetting.customUri, {
          idempotent: true,
        });
      } catch (_) {}
    }

    return {
      type: 'custom',
      builtinId: currentSetting.builtinId,
      customUri: destUri,
      customFileName: fileName,
    };
  } catch (error) {
    console.warn('pickCustomSound error:', error);
    Alert.alert('錯誤', '無法選擇音效檔案，請再試一次。');
    return null;
  }
}

async function getSoundDurationMs(uri: string): Promise<number | null> {
  try {
    const { sound, status } = await Audio.Sound.createAsync({ uri });
    let durationMs: number | null = null;
    if (status.isLoaded && status.durationMillis != null) {
      durationMs = status.durationMillis;
    }
    await sound.unloadAsync();
    return durationMs;
  } catch {
    return null;
  }
}
