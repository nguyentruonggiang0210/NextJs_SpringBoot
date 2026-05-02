package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO cho message trong RabbitMQ queue
 * Sử dụng để truyền dữ liệu giữa AutoPatternController và polling thread
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessage {

    /**
     * ID của message (tự động sinh)
     */
    private String messageId;

    /**
     * Loại pattern/auto task cần xử lý
     */
    private String patternType;

    /**
     * Payload dữ liệu cần xử lý (có thể là JSON string)
     */
    private String payload;

    /**
     * ID của user tạo request
     */
    private Long userId;

    /**
     * Thời gian tạo message
     */
    private LocalDateTime createdAt;

    /**
     * Priority của message (1=high, 2=medium, 3=low)
     */
    private Integer priority;

    /**
     * Trạng thái xử lý (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    private String status;

    /**
     * Thông tin thêm (metadata)
     */
    private String metadata;
}
