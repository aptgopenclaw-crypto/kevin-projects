class TimerConfig {
  final int intervalMinutes;
  final int totalMinutes;

  const TimerConfig({
    this.intervalMinutes = 5,
    this.totalMinutes = 30,
  });

  int get totalRounds => totalMinutes ~/ intervalMinutes;

  TimerConfig copyWith({int? intervalMinutes, int? totalMinutes}) {
    return TimerConfig(
      intervalMinutes: intervalMinutes ?? this.intervalMinutes,
      totalMinutes: totalMinutes ?? this.totalMinutes,
    );
  }

  Map<String, dynamic> toJson() => {
        'intervalMinutes': intervalMinutes,
        'totalMinutes': totalMinutes,
      };

  factory TimerConfig.fromJson(Map<String, dynamic> json) => TimerConfig(
        intervalMinutes: (json['intervalMinutes'] as int?) ?? 5,
        totalMinutes: (json['totalMinutes'] as int?) ?? 30,
      );
}
