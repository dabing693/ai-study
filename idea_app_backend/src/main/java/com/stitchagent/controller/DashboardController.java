package com.stitchagent.controller;

import com.stitchagent.model.ProjectStats;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    @GetMapping("/stats")
    public ProjectStats getStats() {
        return ProjectStats.builder()
            .projectName("以太项目 (Aether)")
            .status("创新工坊 · 运行中")
            .progress(68)
            .commits(482)
            .ideas(24)
            .cycles(8)
            .onlineMembers(12)
            .discussions(28)
            .build();
    }
}
