import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../controllers/pulse_controller.dart';
import '../models/pulse_message.dart';

class AiHumanMixedPulseFeed extends StatelessWidget {
  const AiHumanMixedPulseFeed({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final PulseController controller = Get.put(PulseController());
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        title: const Text('人机协作脉搏', style: TextStyle(fontSize: 18, fontWeight: FontWeight.normal)),
        centerTitle: false,
        actions: [
          IconButton(icon: const Icon(Icons.tune), onPressed: () {}),
          IconButton(icon: const Icon(Icons.more_vert), onPressed: () {}),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(60),
          child: _buildDeploymentStatusBar(colorScheme),
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: Obx(() {
              if (controller.isLoading.value && controller.messages.isEmpty) {
                return const Center(child: CircularProgressIndicator());
              }

              return RefreshIndicator(
                onRefresh: controller.fetchMessages,
                child: ListView.separated(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
                  itemCount: controller.messages.length,
                  separatorBuilder: (context, index) => const SizedBox(height: 24),
                  itemBuilder: (context, index) {
                    final msg = controller.messages[index];
                    return _buildMessageBubble(msg, colorScheme);
                  },
                ),
              );
            }),
          ),
          _buildMessageInput(colorScheme),
        ],
      ),
    );
  }

  Widget _buildDeploymentStatusBar(ColorScheme colorScheme) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        border: Border(top: BorderSide(color: colorScheme.outline.withValues(alpha: 0.1))),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(color: colorScheme.primary, borderRadius: BorderRadius.circular(6)),
            child: Text('部署中', style: TextStyle(color: colorScheme.onPrimary, fontSize: 11, fontWeight: FontWeight.bold)),
          ),
          const SizedBox(width: 12),
          Text('v2.4.0 核心引擎', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: colorScheme.onSurface)),
          const Spacer(),
          Text('85%', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: colorScheme.primary)),
          const SizedBox(width: 12),
          SizedBox(
            width: 60,
            child: LinearProgressIndicator(
              value: 0.85,
              backgroundColor: colorScheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(colorScheme.primary),
              minHeight: 4,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(PulseMessage msg, ColorScheme colorScheme) {
    bool isAgent = msg.type == 'AGENT';
    bool isExpert = msg.type == 'EXPERT';

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (!isAgent && !isExpert) const Spacer(),
        if (isAgent || isExpert) ...[
          _buildAvatar(msg.icon, isAgent ? colorScheme.primary : colorScheme.tertiary, colorScheme),
          const SizedBox(width: 12),
        ],
        Flexible(
          flex: 4,
          child: Column(
            crossAxisAlignment: (isAgent || isExpert) ? CrossAxisAlignment.start : CrossAxisAlignment.end,
            children: [
              Text(
                msg.senderName,
                style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: isAgent
                      ? colorScheme.primaryContainer.withValues(alpha: 0.4)
                      : (isExpert ? colorScheme.tertiaryContainer.withValues(alpha: 0.4) : colorScheme.surfaceContainerHighest.withValues(alpha: 0.8)),
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(isAgent || isExpert ? 4 : 16),
                    topRight: Radius.circular(isAgent || isExpert ? 16 : 4),
                    bottomLeft: const Radius.circular(16),
                    bottomRight: const Radius.circular(16),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      msg.content,
                      style: TextStyle(fontSize: 14, color: colorScheme.onSurface, height: 1.4),
                    ),
                    if (msg.codeSnippet != null) ...[
                      const SizedBox(height: 8),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(color: Colors.black87, borderRadius: BorderRadius.circular(8)),
                        child: Text(
                          msg.codeSnippet!,
                          style: const TextStyle(color: Colors.greenAccent, fontSize: 12, fontFamily: 'monospace'),
                        ),
                      ),
                    ],
                    if (msg.status != null) ...[
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Icon(Icons.sync, size: 14, color: colorScheme.tertiary),
                          const SizedBox(width: 6),
                          Text(msg.status!, style: TextStyle(fontSize: 11, color: colorScheme.tertiary, fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
        if (!isAgent && !isExpert) ...[
          const SizedBox(width: 12),
          _buildAvatar('person', colorScheme.secondary, colorScheme),
        ],
        if (isAgent || isExpert) const Spacer(),
      ],
    );
  }

  Widget _buildAvatar(String iconName, Color color, ColorScheme colorScheme) {
    IconData iconData = Icons.person;
    if (iconName == 'architecture') iconData = Icons.architecture;
    if (iconName == 'biotech') iconData = Icons.biotech;

    return Container(
      width: 36,
      height: 36,
      decoration: BoxDecoration(color: color.withValues(alpha: 0.1), shape: BoxShape.circle),
      child: Icon(iconData, color: color, size: 20),
    );
  }

  Widget _buildMessageInput(ColorScheme colorScheme) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        border: Border(top: BorderSide(color: colorScheme.outline.withValues(alpha: 0.1))),
      ),
      child: Row(
        children: [
          Container(
            decoration: BoxDecoration(color: colorScheme.surfaceContainerHighest, shape: BoxShape.circle),
            child: IconButton(icon: const Icon(Icons.add), onPressed: () {}),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                color: colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(24),
              ),
              child: const TextField(
                decoration: InputDecoration(hintText: '输入消息...', border: InputBorder.none, isDense: true),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Container(
            decoration: BoxDecoration(color: colorScheme.primary, shape: BoxShape.circle),
            child: IconButton(icon: const Icon(Icons.send, color: Colors.white, size: 20), onPressed: () {}),
          ),
        ],
      ),
    );
  }
}
