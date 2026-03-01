class PulseMessage {
  final String id;
  final String senderName;
  final String content;
  final String type; // USER, AGENT, EXPERT
  final String icon;
  final String? codeSnippet;
  final String? status;

  PulseMessage({
    required this.id,
    required this.senderName,
    required this.content,
    required this.type,
    required this.icon,
    this.codeSnippet,
    this.status,
  });

  factory PulseMessage.fromJson(Map<String, dynamic> json) {
    return PulseMessage(
      id: json['id']?.toString() ?? '',
      senderName: json['senderName'] ?? '',
      content: json['content'] ?? '',
      type: json['type'] ?? 'USER',
      icon: json['icon'] ?? 'person',
      codeSnippet: json['codeSnippet'],
      status: json['status'],
    );
  }
}
