package com.example.demo.dto;

import java.time.LocalDateTime;

/**
 * DTO cho WebSocket notification để tránh buffer size limit
 * Chỉ gửi thông tin summary thay vì toàn bộ JobResult với base64 data
 */
public class JobResultSummary {
    private String jobId;
    private String patternType;
    private String status;
    private String errorMessage;
    private String briefResult;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long userId;

    public JobResultSummary() {}

    public JobResultSummary(String jobId, String patternType, String status, String errorMessage,
                          LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime completedAt, Long userId) {
        this.jobId = jobId;
        this.patternType = patternType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.userId = userId;
    }

    // Getters and Setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPatternType() {
        return patternType;
    }

    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getBriefResult() {
        return briefResult;
    }

    public void setBriefResult(String briefResult) {
        this.briefResult = briefResult;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
