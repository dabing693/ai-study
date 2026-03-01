import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../controllers/dashboard_controller.dart';

class ProjectWorkspaceDashboard extends StatelessWidget {
  const ProjectWorkspaceDashboard({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final DashboardController controller = Get.put(DashboardController());
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        title: const Text('项目工作台', style: TextStyle(fontSize: 20, fontWeight: FontWeight.normal)),
        centerTitle: false,
        leading: IconButton(
          icon: const Icon(Icons.menu),
          onPressed: () {},
        ),
        actions: [
          OutlinedButton(
            onPressed: () => controller.fetchStats(), // Pull to refresh simulation
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              minimumSize: const Size(0, 32),
              side: BorderSide(color: colorScheme.outline),
            ),
            child: Text(
              '刷新',
              style: TextStyle(color: colorScheme.primary, fontSize: 12),
            ),
          ),
          Stack(
            children: [
              IconButton(icon: const Icon(Icons.notifications), onPressed: () {}),
              Positioned(
                top: 10,
                right: 10,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: Colors.red,
                    shape: BoxShape.circle,
                    border: Border.all(color: colorScheme.surface, width: 2),
                  ),
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: CircleAvatar(
              radius: 16,
              backgroundImage: const NetworkImage('https://cravatar.cn/avatar/2?s=200&d=identicon'),
            ),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Column(
            children: [
              Container(color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5), height: 1),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: Row(
                  children: [
                    _buildTab(colorScheme, '概览', isActive: true),
                    _buildTab(colorScheme, '文档'),
                    _buildTab(colorScheme, '代码'),
                    _buildTab(colorScheme, '路线图'),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      body: Obx(() {
        if (controller.isLoading.value && controller.stats.value == null) {
          return const Center(child: CircularProgressIndicator());
        }

        final stats = controller.stats.value;
        if (stats == null) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text('无法连接到服务器'),
                const SizedBox(height: 16),
                ElevatedButton(onPressed: controller.fetchStats, child: const Text('重试')),
              ],
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: controller.fetchStats,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _buildHeroSection(colorScheme, stats),
              const SizedBox(height: 24),
              _buildGridFeatures(colorScheme),
              const SizedBox(height: 24),
              _buildOverallProgress(colorScheme, stats),
              const SizedBox(height: 24),
              _buildActivityUpdates(colorScheme),
              const SizedBox(height: 100),
            ],
          ),
        );
      }),
    );
  }

  Widget _buildTab(ColorScheme colorScheme, String label, {bool isActive = false}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
      decoration: BoxDecoration(
        border: isActive ? Border(bottom: BorderSide(color: colorScheme.primary, width: 3)) : null,
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: isActive ? colorScheme.primary : colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }

  Widget _buildHeroSection(ColorScheme colorScheme, dynamic stats) {
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(24),
      ),
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(Icons.rocket_launch, size: 32, color: colorScheme.onPrimaryContainer),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(stats.projectName, style: TextStyle(fontSize: 24, fontWeight: FontWeight.w600, color: colorScheme.onSurface)),
                    Text(stats.status, style: TextStyle(fontSize: 14, color: colorScheme.onSurfaceVariant)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              SizedBox(
                width: 72,
                child: Stack(
                  children: [
                    _buildAvatar('https://cravatar.cn/avatar/a?s=200&d=identicon', colorScheme),
                    Positioned(left: 16, child: _buildAvatar('https://cravatar.cn/avatar/b?s=200&d=identicon', colorScheme)),
                    Positioned(left: 32, child: _buildPlusAvatar('+${stats.onlineMembers}', colorScheme)),
                  ],
                ),
              ),
              Container(height: 16, width: 1, color: colorScheme.outline.withValues(alpha: 0.2), margin: const EdgeInsets.symmetric(horizontal: 4)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: Colors.green.shade100.withValues(alpha: 0.5), borderRadius: BorderRadius.circular(16)),
                child: Row(
                  children: [
                    Container(width: 8, height: 8, decoration: BoxDecoration(color: Colors.green.shade500, shape: BoxShape.circle)),
                    const SizedBox(width: 6),
                    Text('${stats.onlineMembers}人 在线', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: Colors.green.shade800)),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: colorScheme.primary.withValues(alpha: 0.05), borderRadius: BorderRadius.circular(16)),
                child: Row(
                  children: [
                    Icon(Icons.forum, size: 16, color: colorScheme.primary),
                    const SizedBox(width: 6),
                    Text('${stats.discussions}条 讨论', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: colorScheme.primary)),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAvatar(String url, ColorScheme colorScheme) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: colorScheme.surfaceContainerHighest, width: 2),
        image: DecorationImage(image: NetworkImage(url), fit: BoxFit.cover),
      ),
    );
  }

  Widget _buildPlusAvatar(String text, ColorScheme colorScheme) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        color: colorScheme.secondaryContainer,
        shape: BoxShape.circle,
        border: Border.all(color: colorScheme.surfaceContainerHighest, width: 2),
      ),
      child: Center(
        child: Text(text, style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: colorScheme.onSecondaryContainer)),
      ),
    );
  }

  Widget _buildGridFeatures(ColorScheme colorScheme) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      childAspectRatio: 1.5,
      children: [
        _buildGridItem(Icons.description, '文档', '14份 新指南', colorScheme.primary, colorScheme.surfaceContainerHighest.withValues(alpha: 0.5), colorScheme),
        _buildGridItem(Icons.terminal, '代码', 'v1.2 稳定版', colorScheme.primary, colorScheme.surfaceContainerHighest.withValues(alpha: 0.5), colorScheme),
        _buildGridItem(Icons.map, '路线图', 'Q3 目标', colorScheme.primary, colorScheme.surfaceContainerHighest.withValues(alpha: 0.5), colorScheme),
        _buildGridItem(Icons.bolt, '智能工具', 'Agent 助手就绪', colorScheme.onPrimaryContainer, colorScheme.primaryContainer, colorScheme, isDarkText: true),
      ],
    );
  }

  Widget _buildGridItem(IconData icon, String title, String subTitle, Color iconColor, Color bgColor, ColorScheme colorScheme, {bool isDarkText = false}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: iconColor, size: 24),
          const Spacer(),
          Text(title, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: isDarkText ? colorScheme.onPrimaryContainer : colorScheme.onSurface)),
          Text(subTitle, style: TextStyle(fontSize: 12, color: isDarkText ? colorScheme.onPrimaryContainer.withValues(alpha: 0.7) : colorScheme.onSurfaceVariant)),
        ],
      ),
    );
  }

  Widget _buildOverallProgress(ColorScheme colorScheme, dynamic stats) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: colorScheme.surfaceContainerHighest),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('总进度', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500, color: colorScheme.onSurface)),
              Text('${stats.progress}%', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: colorScheme.primary)),
            ],
          ),
          const SizedBox(height: 16),
          LinearProgressIndicator(
            value: stats.progress / 100.0,
            backgroundColor: colorScheme.surfaceContainerHighest,
            valueColor: AlwaysStoppedAnimation<Color>(colorScheme.primary),
            borderRadius: BorderRadius.circular(4),
            minHeight: 8,
          ),
          const SizedBox(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _buildStatItem('${stats.commits}', '提交', colorScheme),
              Container(height: 40, width: 1, color: colorScheme.surfaceContainerHighest),
              _buildStatItem('${stats.ideas}', '创意', colorScheme),
              Container(height: 40, width: 1, color: colorScheme.surfaceContainerHighest),
              _buildStatItem('${stats.cycles}', '周期', colorScheme),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String value, String label, ColorScheme colorScheme) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colorScheme.onSurface)),
        Text(label, style: TextStyle(fontSize: 10, fontWeight: FontWeight.w500, color: colorScheme.onSurfaceVariant, letterSpacing: 1.2)),
      ],
    );
  }

  Widget _buildActivityUpdates(ColorScheme colorScheme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: Text('动态更新', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: colorScheme.onSurfaceVariant)),
        ),
        Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: colorScheme.surfaceContainerHighest),
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            children: [
              _buildUpdateRow('小明', ' 更新了 ', '品牌资产.fig', '2分钟前', Icons.palette, 'https://cravatar.cn/avatar/xm?s=200&d=identicon', colorScheme),
              _buildUpdateRow('架构师', ' 合并了 ', 'feat/ai-chat', '45分钟前', Icons.settings_suggest, 'https://cravatar.cn/avatar/jgs?s=200&d=identicon', colorScheme),
              _buildUpdateRowWithIcon('匿名用户_9', ' 编辑了 ', 'README.md', '1小时前', colorScheme),
              TextButton(
                onPressed: () {},
                style: TextButton.styleFrom(
                  minimumSize: const Size(double.infinity, 48),
                  shape: const RoundedRectangleBorder(),
                ),
                child: Text('查看全部更新', style: TextStyle(color: colorScheme.primary)),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildUpdateRow(String name, String action, String file, String time, IconData icon, String avatarUrl, ColorScheme colorScheme) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5))),
      ),
      child: Row(
        children: [
          CircleAvatar(radius: 20, backgroundImage: NetworkImage(avatarUrl)),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                RichText(
                  text: TextSpan(
                    style: TextStyle(color: colorScheme.onSurface, fontSize: 14),
                    children: [
                      TextSpan(text: name, style: const TextStyle(fontWeight: FontWeight.bold)),
                      TextSpan(text: action),
                      TextSpan(text: file, style: TextStyle(color: colorScheme.primary)),
                    ],
                  ),
                ),
                Text(time, style: TextStyle(color: colorScheme.onSurfaceVariant.withValues(alpha: 0.7), fontSize: 12)),
              ],
            ),
          ),
          Icon(icon, color: colorScheme.onSurfaceVariant.withValues(alpha: 0.4)),
        ],
      ),
    );
  }

  Widget _buildUpdateRowWithIcon(String name, String action, String file, String time, ColorScheme colorScheme) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5))),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(shape: BoxShape.circle, color: colorScheme.surfaceContainerHighest),
            child: Icon(Icons.person, color: colorScheme.onSurfaceVariant),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                RichText(
                  text: TextSpan(
                    style: TextStyle(color: colorScheme.onSurface, fontSize: 14),
                    children: [
                      TextSpan(text: name, style: const TextStyle(fontWeight: FontWeight.bold)),
                      TextSpan(text: action),
                      TextSpan(text: file, style: TextStyle(color: colorScheme.primary)),
                    ],
                  ),
                ),
                Text(time, style: TextStyle(color: colorScheme.onSurfaceVariant.withValues(alpha: 0.7), fontSize: 12)),
              ],
            ),
          ),
          Icon(Icons.edit_note, color: colorScheme.onSurfaceVariant.withValues(alpha: 0.4)),
        ],
      ),
    );
  }
}
