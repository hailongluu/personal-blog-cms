package com.blog.cms.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Dashboard summary — SPEC §8.2.3
 * Aggregates counts and recent activity for the admin landing page.
 */
@Data @Builder @AllArgsConstructor
public class DashboardResponse {
    private long totalPosts;
    private long publishedPosts;
    private long draftPosts;
    private long reviewingPosts;
    private long archivedPosts;
    private long totalTopics;
    private long totalTags;
    private long totalProjects;
    private long totalMedia;
    private long newsletterSubscribers;
    private List<RecentPostDto> recentPosts;
    private List<RecentPostDto> pendingDrafts;

    @Data @Builder @AllArgsConstructor
    public static class RecentPostDto {
        private Long id;
        private String title;
        private String slug;
        private String status;
        private String type;
        private boolean featured;
        private Instant updatedAt;
    }
}
