package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity để lưu kết quả xử lý job từ queue
 * Client có thể check status và kết quả bằng jobId
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_results")
public class JobResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Job ID (tương ứng với messageId từ QueueMessage)
     */
    @Column(unique = true, nullable = false)
    private String jobId;

    /**
     * Loại pattern được xử lý
     */
    @Column(nullable = false)
    private String patternType;

    /**
     * Payload ban đầu
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * User ID tạo job
     */
    private Long userId;

    /**
     * Trạng thái: PENDING, PROCESSING, COMPLETED, FAILED
     */
    @Column(nullable = false)
    private String status;

    /**
     * Kết quả xử lý (JSON string)
     */
    @Column(columnDefinition = "TEXT")
    private String result;

    /**
     * Error message nếu thất bại
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Thời gian tạo job
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Thời gian bắt đầu xử lý
     */
    private LocalDateTime startedAt;

    /**
     * Thời gian hoàn thành
     */
    private LocalDateTime completedAt;

    /**
     * Metadata bổ sung
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
