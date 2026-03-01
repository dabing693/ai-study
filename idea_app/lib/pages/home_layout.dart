import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'idea_discovery_feed.dart';
import 'ai_human_mixed_pulse_feed.dart';
import 'instant_sandbox_ai_roadmap.dart';
import 'project_workspace_dashboard.dart';

class HomeController extends GetxController {
  var currentIndex = 0.obs;

  void changePage(int index) {
    currentIndex.value = index;
  }
}

class HomeLayout extends StatelessWidget {
  const HomeLayout({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final HomeController controller = Get.put(HomeController());
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    final List<Widget> pages = [
      const IdeaDiscoveryFeed(),
      const InstantSandboxAiRoadmap(),
      const Center(child: Text('快速添加')), // Placeholder for FAB action
      const AiHumanMixedPulseFeed(),
      const ProjectWorkspaceDashboard(),
    ];

    return Scaffold(
      body: Obx(() => IndexedStack(
            index: controller.currentIndex.value,
            children: pages,
          )),
      bottomNavigationBar: _buildBottomNav(context, controller, colorScheme),
    );
  }

  Widget _buildBottomNav(BuildContext context, HomeController controller, ColorScheme colorScheme) {
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surface.withOpacity(0.9),
        border: Border(top: BorderSide(color: colorScheme.outline.withOpacity(0.1))),
      ),
      padding: const EdgeInsets.only(bottom: 24, top: 12, left: 24, right: 24),
      child: Obx(
        () => Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            _buildNavItem(0, Icons.explore, Icons.explore_outlined, '探索', controller, colorScheme),
            _buildNavItem(1, Icons.biotech, Icons.biotech_outlined, '实验室', controller, colorScheme),
            // Center Floating Action Button like item
            GestureDetector(
              onTap: () => controller.changePage(2),
              child: Transform.translate(
                offset: const Offset(0, -16),
                child: Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: colorScheme.primary,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: colorScheme.primary.withOpacity(0.4),
                        blurRadius: 8,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Icon(Icons.add, color: colorScheme.onPrimary, size: 28),
                ),
              ),
            ),
            _buildNavItem(3, Icons.forum, Icons.forum_outlined, '消息', controller, colorScheme),
            _buildNavItem(4, Icons.account_circle, Icons.account_circle_outlined, '我的', controller, colorScheme),
          ],
        ),
      ),
    );
  }

  Widget _buildNavItem(int index, IconData activeIcon, IconData inactiveIcon, String label, HomeController controller, ColorScheme colorScheme) {
    final bool isSelected = controller.currentIndex.value == index;

    return GestureDetector(
      onTap: () => controller.changePage(index),
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
            decoration: BoxDecoration(
              color: isSelected ? colorScheme.secondaryContainer : Colors.transparent,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Icon(
              isSelected ? activeIcon : inactiveIcon,
              color: isSelected ? colorScheme.onSecondaryContainer : colorScheme.onSurfaceVariant,
              size: 24,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w500,
              color: isSelected ? colorScheme.onSurface : colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
