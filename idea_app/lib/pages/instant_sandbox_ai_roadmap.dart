import 'package:flutter/material.dart';

class InstantSandboxAiRoadmap extends StatelessWidget {
  const InstantSandboxAiRoadmap({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        title: const Text('AI 沙盒路线图', style: TextStyle(fontSize: 18, fontWeight: FontWeight.normal)),
        centerTitle: false,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildRoadmapItem(
            context,
            colorScheme,
            title: '第一阶段：数据准备',
            description: '连接并清理初始数据集，为模型训练做准备。',
            status: '已完成',
            isCompleted: true,
          ),
          _buildRoadmapItem(
            context,
            colorScheme,
            title: '第二阶段：模型训练',
            description: '配置超参数并在沙盒中启动分布式训练。',
            status: '进行中',
            isCompleted: false,
            isActive: true,
          ),
          _buildRoadmapItem(
            context,
            colorScheme,
            title: '第三阶段：评估与部署',
            description: '对各项指标进行评估，然后将模型部署到生产环境。',
            status: '未开始',
            isCompleted: false,
          ),
        ],
      ),
    );
  }

  Widget _buildRoadmapItem(
    BuildContext context,
    ColorScheme colorScheme, {
    required String title,
    required String description,
    required String status,
    bool isCompleted = false,
    bool isActive = false,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: isActive ? colorScheme.primaryContainer.withValues(alpha: 0.3) : colorScheme.surfaceContainerHighest.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: isActive ? colorScheme.primary : colorScheme.outline.withValues(alpha: 0.2)),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: colorScheme.onSurface,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: isCompleted ? Colors.green.withValues(alpha: 0.2) : (isActive ? colorScheme.primary.withValues(alpha: 0.2) : colorScheme.surfaceContainerHighest),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  status,
                  style: TextStyle(
                    fontSize: 12,
                    color: isCompleted ? Colors.green[800] : (isActive ? colorScheme.primary : colorScheme.onSurfaceVariant),
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            description,
            style: TextStyle(fontSize: 14, color: colorScheme.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          if (isActive)
            LinearProgressIndicator(
              value: 0.45,
              backgroundColor: colorScheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(colorScheme.primary),
              borderRadius: BorderRadius.circular(4),
            )
          else if (isCompleted)
             LinearProgressIndicator(
              value: 1.0,
              backgroundColor: colorScheme.surfaceContainerHighest,
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.green),
              borderRadius: BorderRadius.circular(4),
            ),
        ],
      ),
    );
  }
}
