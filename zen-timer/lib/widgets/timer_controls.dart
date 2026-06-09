import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class TimerControls extends StatelessWidget {
  final bool isRunning;
  final bool isPaused;
  final bool isCompleted;
  final VoidCallback onStart;
  final VoidCallback onPause;
  final VoidCallback onStop;
  final VoidCallback onOpenSound;

  const TimerControls({
    super.key,
    required this.isRunning,
    required this.isPaused,
    required this.isCompleted,
    required this.onStart,
    required this.onPause,
    required this.onStop,
    required this.onOpenSound,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        // Stop button (visible when running or paused)
        if (isRunning || isPaused) ...[
          _ControlButton(
            icon: Icons.stop_rounded,
            label: '停止',
            color: Colors.redAccent,
            onTap: onStop,
          ),
          const SizedBox(width: 16),
        ],
        // Main action button
        if (!isRunning && !isPaused)
          _ControlButton(
            icon: Icons.play_arrow_rounded,
            label: isCompleted ? '重新開始' : '開始',
            color: AppTheme.primary,
            onTap: onStart,
            large: true,
          )
        else if (isRunning)
          _ControlButton(
            icon: Icons.pause_rounded,
            label: '暫停',
            color: AppTheme.accent,
            onTap: onPause,
            large: true,
          )
        else if (isPaused)
          _ControlButton(
            icon: Icons.play_arrow_rounded,
            label: '繼續',
            color: AppTheme.primary,
            onTap: onStart,
            large: true,
          ),
        // Sound settings gear (visible when not running)
        if (!isRunning) ...[
          const SizedBox(width: 16),
          IconButton(
            onPressed: onOpenSound,
            icon: const Icon(Icons.settings_rounded),
            color: AppTheme.accent,
            iconSize: 32,
            tooltip: '音效設定',
          ),
        ],
      ],
    );
  }
}

class _ControlButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  final bool large;

  const _ControlButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
    this.large = false,
  });

  @override
  Widget build(BuildContext context) {
    final size = large ? 72.0 : 56.0;
    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: size,
            height: size,
            decoration: BoxDecoration(shape: BoxShape.circle, color: color),
            child: Icon(icon, color: Colors.white, size: large ? 36 : 28),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              color: color,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
