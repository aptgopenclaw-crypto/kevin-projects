import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class IntervalSettings extends StatelessWidget {
  final int intervalMinutes;
  final int totalMinutes;
  final ValueChanged<int> onIntervalChanged;
  final ValueChanged<int> onTotalChanged;

  const IntervalSettings({
    super.key,
    required this.intervalMinutes,
    required this.totalMinutes,
    required this.onIntervalChanged,
    required this.onTotalChanged,
  });

  static const _intervalPresets = [1, 3, 5, 10];
  static const _totalPresets = [15, 30, 45, 60];

  int get _totalRounds => totalMinutes ~/ intervalMinutes;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildRow(
          label: '間隔時間',
          unit: '分鐘',
          value: intervalMinutes,
          min: 1,
          max: 60,
          presets: _intervalPresets,
          onChanged: (v) {
            onIntervalChanged(v);
            // Ensure total >= interval
            if (totalMinutes < v) onTotalChanged(v);
          },
        ),
        const SizedBox(height: 16),
        _buildRow(
          label: '總練習時長',
          unit: '分鐘',
          value: totalMinutes,
          min: intervalMinutes,
          max: 180,
          presets: _totalPresets,
          onChanged: onTotalChanged,
        ),
        const SizedBox(height: 8),
        Text(
          '預計播放 $_totalRounds 次提示音',
          style: const TextStyle(
            fontSize: 14,
            color: AppTheme.accent,
          ),
        ),
      ],
    );
  }

  Widget _buildRow({
    required String label,
    required String unit,
    required int value,
    required int min,
    required int max,
    required List<int> presets,
    required ValueChanged<int> onChanged,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppTheme.textDark,
          ),
        ),
        const SizedBox(height: 6),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _StepButton(
              icon: Icons.remove,
              onTap: value > min ? () => onChanged(value - 1) : null,
            ),
            const SizedBox(width: 12),
            SizedBox(
              width: 80,
              child: Text(
                '$value $unit',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.textDark,
                ),
              ),
            ),
            const SizedBox(width: 12),
            _StepButton(
              icon: Icons.add,
              onTap: value < max ? () => onChanged(value + 1) : null,
            ),
          ],
        ),
        const SizedBox(height: 8),
        Wrap(
          alignment: WrapAlignment.center,
          spacing: 8,
          children: presets.map((p) {
            final selected = value == p;
            return GestureDetector(
              onTap: () {
                if (p >= min && p <= max) onChanged(p);
              },
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: selected ? AppTheme.primary : AppTheme.surface,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: selected ? AppTheme.accent : AppTheme.disabled,
                  ),
                ),
                child: Text(
                  '$p分',
                  style: TextStyle(
                    fontSize: 13,
                    color: selected ? Colors.white : AppTheme.textDark,
                    fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _StepButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback? onTap;

  const _StepButton({required this.icon, this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(24),
      child: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: onTap != null ? AppTheme.primary : AppTheme.disabled,
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: Colors.white, size: 24),
      ),
    );
  }
}
