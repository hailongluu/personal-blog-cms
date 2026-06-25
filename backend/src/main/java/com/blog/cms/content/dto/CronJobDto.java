package com.blog.cms.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * One Hermes cron job — parsed from `hermes cron list` text output.
 * Fields mirror what the CLI prints.
 */
@Data @Builder @AllArgsConstructor
public class CronJobDto {
    private String id;
    private String name;
    private String schedule;
    private String state;      // "active" | "paused"
    private String nextRun;    // ISO timestamp or null
    private String lastRun;    // ISO timestamp or null
    private String lastStatus; // "ok" | "error" | null
    private String deliver;    // "telegram" | "local" | etc.
    private String mode;       // "agent" | "no-agent"
    private String script;     // script path if any
}
