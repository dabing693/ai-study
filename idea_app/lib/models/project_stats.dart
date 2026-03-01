class ProjectStats {
  final String projectName;
  final String status;
  final int progress;
  final int commits;
  final int ideas;
  final int cycles;
  final int onlineMembers;
  final int discussions;

  ProjectStats({
    required this.projectName,
    required this.status,
    required this.progress,
    required this.commits,
    required this.ideas,
    required this.cycles,
    required this.onlineMembers,
    required this.discussions,
  });

  factory ProjectStats.fromJson(Map<String, dynamic> json) {
    return ProjectStats(
      projectName: json['projectName'] ?? '',
      status: json['status'] ?? '',
      progress: json['progress'] ?? 0,
      commits: json['commits'] ?? 0,
      ideas: json['ideas'] ?? 0,
      cycles: json['cycles'] ?? 0,
      onlineMembers: json['onlineMembers'] ?? 0,
      discussions: json['discussions'] ?? 0,
    );
  }
}
