package com.stitchagent.controller;

import com.stitchagent.model.Idea;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/ideas")
public class IdeaController {

    @GetMapping
    public List<Idea> getIdeas() {
        return Arrays.asList(
            Idea.builder()
                .id("1")
                .title("神经合成器")
                .author("匿名发布")
                .timeAgo("2小时前")
                .description("正在构建一个自主智能体，根据可穿戴设备的实时生物特征数据生成高保真音景。")
                .tags(Arrays.asList("人工智能", "Rust"))
                .totalSeats(5)
                .occupiedSeats(3)
                .build(),
            Idea.builder()
                .id("2")
                .title("AgentOS 操作系统")
                .author("核心团队")
                .timeAgo("5小时前")
                .description("首个专为多智能体大模型协作设计的本地化操作系统。无云端依赖，无延迟。")
                .tags(Arrays.asList("OS", "AI"))
                .totalSeats(10)
                .occupiedSeats(8)
                .rating(4.9)
                .imageUrl("https://lh3.googleusercontent.com/...")
                .build()
        );
    }
}
