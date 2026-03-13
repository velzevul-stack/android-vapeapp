class PaymentCard {
  final int id;
  final String label;
  final bool isArchived;
  final int createdAt;

  const PaymentCard({
    this.id = 0,
    required this.label,
    this.isArchived = false,
    required this.createdAt,
  });

  Map<String, dynamic> toInsertMap() {
    final m = toMap();
    m.remove('id');
    return m;
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'label': label,
      'isArchived': isArchived ? 1 : 0,
      'createdAt': createdAt,
    };
  }

  factory PaymentCard.fromMap(Map<String, dynamic> map) {
    return PaymentCard(
      id: map['id'] as int? ?? 0,
      label: map['label'] as String,
      isArchived: (map['isArchived'] as int?) == 1,
      createdAt: map['createdAt'] as int,
    );
  }

  Map<String, dynamic> toJson() => toMap();

  factory PaymentCard.fromJson(Map<String, dynamic> json) => PaymentCard.fromMap(json);
}
