package com.stitchagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PulseMessage {
    private String id;
    private String senderName;
    private String content;
    private String type; // USER, AGENT, EXPERT
    private String icon;
    private String codeSnippet;
    private String status; // For expert/agent progress
}
