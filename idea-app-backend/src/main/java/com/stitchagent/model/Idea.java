package com.stitchagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Idea {
    private String id;
    private String title;
    private String author;
    private String timeAgo;
    private String description;
    private List<String> tags;
    private int totalSeats;
    private int occupiedSeats;
    private String imageUrl;
    private double rating;
}
