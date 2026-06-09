import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class CountdownDisplay extends StatelessWidget {
  final int remainingSeconds;
  final int intervalSeconds;
  final int currentRound;
  final int totalRounds;

  const CountdownDisplay({
    super.key,
    required this.remainingSeconds,
    required this.intervalSeconds,
    required this.currentRound,
    required this.totalRounds,
  });

  String get _timeLabel {
    final m = remainingSeconds ~/ 60;
    final s = remainingSeconds % 60;
    return '${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}';
  }

  double get _progress =>
      intervalSeconds > 0 ? remainingSeconds / intervalSeconds : 0.0;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          '已播放 $currentRound / 總 $totalRounds 次',
          style: const TextStyle(
            fontSize: 18,
            color: AppTheme.textDark,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 24),
        SizedBox(
          width: 220,
          height: 220,
          child: Stack(
            alignment: Alignment.center,
            children: [
              CustomPaint(
                size: const Size(220, 220),
                painter: _RingPainter(progress: _progress),
              ),
              Text(
                _timeLabel,
                style: const TextStyle(
                  fontSize: 52,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.textDark,
                  letterSpacing: 2,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _RingPainter extends CustomPainter {
  final double progress;

  _RingPainter({required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (size.width / 2) - 10;
    const strokeWidth = 12.0;

    // Track
    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..color = AppTheme.surface
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth,
    );

    // Progress arc
    final sweepAngle = 2 * math.pi * progress;
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -math.pi / 2,
      sweepAngle,
      false,
      Paint()
        ..color = AppTheme.primary
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round,
    );
  }

  @override
  bool shouldRepaint(_RingPainter old) => old.progress != progress;
}
