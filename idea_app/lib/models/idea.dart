class Idea {
  final String id;
  final String title;
  final String author;
  final String timeAgo;
  final String description;
  final List<String> tags;
  final int totalSeats;
  final int occupiedSeats;
  final String? imageUrl;
  final double rating;

  Idea({
    required this.id,
    required this.title,
    required this.author,
    required this.timeAgo,
    required this.description,
    required this.tags,
    required this.totalSeats,
    required this.occupiedSeats,
    this.imageUrl,
    this.rating = 0.0,
  });

  factory Idea.fromJson(Map<String, dynamic> json) {
    return Idea(
      id: json['id']?.toString() ?? '',
      title: json['title'] ?? '',
      author: json['author'] ?? '',
      timeAgo: json['timeAgo'] ?? '',
      description: json['description'] ?? '',
      tags: List<String>.from(json['tags'] ?? []),
      totalSeats: json['totalSeats'] ?? 0,
      occupiedSeats: json['occupiedSeats'] ?? 0,
      imageUrl: json['imageUrl'],
      rating: (json['rating'] ?? 0.0).toDouble(),
    );
  }
}
