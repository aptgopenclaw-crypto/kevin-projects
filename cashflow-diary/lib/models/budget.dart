class Budget {
  final int? id;
  final String month; // YYYY-MM
  final double amount;

  const Budget({
    this.id,
    required this.month,
    required this.amount,
  });

  Budget copyWith({
    int? id,
    String? month,
    double? amount,
  }) {
    return Budget(
      id: id ?? this.id,
      month: month ?? this.month,
      amount: amount ?? this.amount,
    );
  }

  Map<String, dynamic> toMap() => {
    if (id != null) 'id': id,
    'month': month,
    'amount': amount,
  };

  factory Budget.fromMap(Map<String, dynamic> map) => Budget(
    id: map['id'] as int?,
    month: map['month'] as String,
    amount: (map['amount'] as num).toDouble(),
  );
}
