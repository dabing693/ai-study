import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../controllers/idea_feed_controller.dart';
import '../models/idea.dart';

class IdeaDiscoveryFeed extends StatefulWidget {
  const IdeaDiscoveryFeed({Key? key}) : super(key: key);

  @override
  _IdeaDiscoveryFeedState createState() => _IdeaDiscoveryFeedState();
}

class _IdeaDiscoveryFeedState extends State<IdeaDiscoveryFeed> {
  final IdeaFeedController controller = Get.put(IdeaFeedController());
  final List<String> categories = ['全部', '人工智能', '区块链', 'Web3', '开源硬件'];
  int selectedCategoryIndex = 0;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colorScheme),
            Expanded(
              child: Obx(() {
                if (controller.isLoading.value && controller.ideas.isEmpty) {
                  return const Center(child: CircularProgressIndicator());
                }

                if (controller.ideas.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text('暂无创意'),
                        const SizedBox(height: 16),
                        ElevatedButton(onPressed: controller.fetchIdeas, child: const Text('重试')),
                      ],
                    ),
                  );
                }

                return RefreshIndicator(
                  onRefresh: controller.fetchIdeas,
                  child: ListView.separated(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    itemCount: controller.ideas.length + 1, // +1 for the section title
                    separatorBuilder: (context, index) => const SizedBox(height: 16),
                    itemBuilder: (context, index) {
                      if (index == 0) {
                        return _buildSectionTitle(colorScheme);
                      }
                      final idea = controller.ideas[index - 1];
                      return _buildIdeaCard(idea, colorScheme);
                    },
                  ),
                );
              }),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(ColorScheme colorScheme) {
    return Container(
      color: colorScheme.surface.withOpacity(0.95),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            decoration: BoxDecoration(
              color: colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(100),
            ),
            child: Row(
              children: [
                Icon(Icons.search, color: colorScheme.onSurfaceVariant),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                      hintText: '搜索创意或项目...',
                      hintStyle: TextStyle(
                        fontSize: 14,
                        color: colorScheme.onSurfaceVariant.withOpacity(0.6),
                      ),
                      border: InputBorder.none,
                      isDense: true,
                    ),
                    style: TextStyle(fontSize: 14, color: colorScheme.onSurface),
                  ),
                ),
                IconButton(
                  icon: Icon(Icons.language, color: colorScheme.onSurfaceVariant),
                  onPressed: () {},
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  splashRadius: 20,
                ),
                const SizedBox(width: 8),
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: colorScheme.outline.withOpacity(0.2)),
                  ),
                  child: ClipOval(
                    child: Image.network(
                      'https://cravatar.cn/avatar/1?s=200&d=identicon',
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) =>
                          Icon(Icons.person, color: colorScheme.onSurfaceVariant),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            height: 36,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: categories.length,
              separatorBuilder: (context, index) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final isSelected = selectedCategoryIndex == index;
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      selectedCategoryIndex = index;
                    });
                  },
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    decoration: BoxDecoration(
                      color: isSelected ? colorScheme.primaryContainer : Colors.transparent,
                      border: isSelected ? null : Border.all(color: colorScheme.outline),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      categories[index],
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: isSelected
                            ? colorScheme.onPrimaryContainer
                            : colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(ColorScheme colorScheme) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          '创意发现流',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
            color: colorScheme.onSurface,
          ),
        ),
        TextButton(
          onPressed: () {},
          child: Text(
            '查看更多',
            style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: colorScheme.primary),
          ),
        ),
      ],
    );
  }

  Widget _buildIdeaCard(Idea idea, ColorScheme colorScheme) {
    // If it has an image, build a different style card like the second one in the original design
    if (idea.imageUrl != null && idea.imageUrl!.isNotEmpty && idea.imageUrl!.contains('http')) {
      return _buildImageCard(idea, colorScheme);
    }
    return _buildStandardCard(idea, colorScheme);
  }

  Widget _buildStandardCard(Idea idea, ColorScheme colorScheme) {
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withOpacity(0.3),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            offset: const Offset(0, 1),
            blurRadius: 3,
            spreadRadius: 1,
          ),
        ],
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: colorScheme.tertiary.withOpacity(0.1),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.psychology, color: colorScheme.tertiary),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        idea.title,
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                      ),
                      Text(
                        '${idea.author} · ${idea.timeAgo}',
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w500, color: colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: colorScheme.secondaryContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  '${idea.occupiedSeats}/${idea.totalSeats} 席位',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: colorScheme.onSecondaryContainer),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            idea.description,
            style: TextStyle(fontSize: 14, color: colorScheme.onSurfaceVariant, height: 1.5),
          ),
          const SizedBox(height: 16),
          Row(
            children: idea.tags.map((tag) => Padding(
              padding: const EdgeInsets.only(right: 8),
              child: _buildTag(tag, colorScheme),
            )).toList(),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: () {},
                  style: ElevatedButton.styleFrom(
                    backgroundColor: colorScheme.primary,
                    foregroundColor: colorScheme.onPrimary,
                    elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(100)),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                  child: const Text('申请加入', style: TextStyle(fontWeight: FontWeight.w500)),
                ),
              ),
              const SizedBox(width: 8),
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(color: colorScheme.secondaryContainer, shape: BoxShape.circle),
                child: IconButton(
                  icon: Icon(Icons.favorite_border, color: colorScheme.onSecondaryContainer),
                  onPressed: () {},
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildImageCard(Idea idea, ColorScheme colorScheme) {
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withOpacity(0.2),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: colorScheme.outline.withOpacity(0.2)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Image.network(
            idea.imageUrl!,
            height: 128,
            width: double.infinity,
            fit: BoxFit.cover,
            errorBuilder: (c, e, s) => Container(height: 128, color: colorScheme.surfaceContainerHighest, child: const Icon(Icons.image)),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      idea.title,
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                    ),
                    if (idea.rating > 0)
                      Row(
                        children: [
                          Icon(Icons.star, color: colorScheme.primary, size: 16),
                          const SizedBox(width: 4),
                          Text('${idea.rating}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                        ],
                      ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  idea.description,
                  style: TextStyle(fontSize: 14, color: colorScheme.onSurfaceVariant),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _buildAvatarPlaceholderStack(colorScheme),
                    ElevatedButton(
                      onPressed: () {},
                      style: ElevatedButton.styleFrom(
                        backgroundColor: colorScheme.surfaceContainerHighest,
                        foregroundColor: colorScheme.primary,
                        elevation: 0,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(100),
                          side: BorderSide(color: colorScheme.outline.withOpacity(0.2)),
                        ),
                      ),
                      child: const Text('申请加入'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvatarPlaceholderStack(ColorScheme colorScheme) {
    return SizedBox(
      width: 72,
      child: Stack(
        children: [
          _buildAvatarMini('A', colorScheme.primary, colorScheme.onPrimary, colorScheme),
          Positioned(left: 16, child: _buildAvatarMini('B', colorScheme.secondary, colorScheme.onSecondary, colorScheme)),
          Positioned(left: 32, child: _buildAvatarMini('+', colorScheme.surfaceContainerHighest, colorScheme.onSurfaceVariant, colorScheme)),
        ],
      ),
    );
  }

  Widget _buildAvatarMini(String label, Color bg, Color text, ColorScheme colorScheme) {
    return Container(
      width: 28,
      height: 28,
      decoration: BoxDecoration(color: bg, shape: BoxShape.circle, border: Border.all(color: colorScheme.surface, width: 2)),
      child: Center(child: Text(label, style: TextStyle(color: text, fontSize: 10, fontWeight: FontWeight.bold))),
    );
  }

  Widget _buildTag(String text, ColorScheme colorScheme) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: colorScheme.outline.withOpacity(0.1)),
      ),
      child: Text(text, style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant)),
    );
  }
}
