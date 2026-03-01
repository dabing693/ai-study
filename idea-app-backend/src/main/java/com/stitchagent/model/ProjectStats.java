package com.stitchagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStats {
    private String projectName;
    private String status;
    private int progress;
    private int commits;
    private int ideas;
    private int cycles;
    private int onlineMembers;
    private int discussions;
}
