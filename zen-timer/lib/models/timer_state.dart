class TimerState {
  final int remainingSeconds;
  final int currentRound;
  final int totalRounds;
  final bool isRunning;
  final bool isPaused;
  final bool isCompleted;
  final int? startTimestamp; // epoch ms, used for background sync

  const TimerState({
    required this.remainingSeconds,
    required this.currentRound,
    required this.totalRounds,
    this.isRunning = false,
    this.isPaused = false,
    this.isCompleted = false,
    this.startTimestamp,
  });

  factory TimerState.initial(int intervalMinutes, int totalMinutes) {
    return TimerState(
      remainingSeconds: intervalMinutes * 60,
      currentRound: 0,
      totalRounds: totalMinutes ~/ intervalMinutes,
    );
  }

  bool get isIdle => !isRunning && !isPaused && !isCompleted;

  TimerState copyWith({
    int? remainingSeconds,
    int? currentRound,
    int? totalRounds,
    bool? isRunning,
    bool? isPaused,
    bool? isCompleted,
    int? startTimestamp,
    bool clearStartTimestamp = false,
  }) {
    return TimerState(
      remainingSeconds: remainingSeconds ?? this.remainingSeconds,
      currentRound: currentRound ?? this.currentRound,
      totalRounds: totalRounds ?? this.totalRounds,
      isRunning: isRunning ?? this.isRunning,
      isPaused: isPaused ?? this.isPaused,
      isCompleted: isCompleted ?? this.isCompleted,
      startTimestamp:
          clearStartTimestamp ? null : (startTimestamp ?? this.startTimestamp),
    );
  }
}
