package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.QueueMessage;
import com.example.demo.entity.JobResult;
import com.example.demo.service.JobResultService;

/**
 * Controller để trigger tạo message queue cho auto pattern
 * Khi endpoint được gọi, sẽ tạo message và gửi vào RabbitMQ queue
 */
@RestController
@RequestMapping("/api/auto-pattern")
public class AutoPatternController {

    private final RabbitTemplate rabbitTemplate;
    private final JobResultService jobResultService;

    public AutoPatternController(RabbitTemplate rabbitTemplate, JobResultService jobResultService) {
        this.rabbitTemplate = rabbitTemplate;
        this.jobResultService = jobResultService;
    }

    /**
     * POST /api/auto-pattern/trigger
     * Trigger tạo message queue để xử lý auto pattern
     *
     * @param request Request chứa patternType và payload
     * @return ResponseEntity với thông tin message đã tạo
     */
    @PostMapping("/trigger")
    public ResponseEntity<TriggerResponse> triggerAutoPattern(@RequestBody AutoPatternRequest request) {
        // Tạo message mới
        QueueMessage message = new QueueMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setPatternType(request.getPatternType());
        message.setPayload(request.getPayload());
        message.setUserId(request.getUserId());
        message.setCreatedAt(LocalDateTime.now());
        message.setPriority(request.getPriority() != null ? request.getPriority() : 2);
        message.setStatus("PENDING");
        message.setMetadata(request.getMetadata());

        // Tạo job result ngay lập tức
        jobResultService.createJobResult(message);

        // Serialize message thành JSON string trước khi gửi
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String jsonMessage = mapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.AUTO_PATTERN_QUEUE, jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }

        // Trả về jobId thay vì toàn bộ message
        TriggerResponse response = new TriggerResponse();
        response.setJobId(message.getMessageId());
        response.setStatus("PENDING");
        response.setMessage("Job created successfully. Use /status/" + message.getMessageId() + " to check progress.");

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auto-pattern/batch
     * Trigger tạo nhiều message queue cùng lúc
     *
     * @param requests Danh sách các request
     * @return Số lượng message đã gửi
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchResponse> triggerBatch(@RequestBody java.util.List<AutoPatternRequest> requests) {
        int successCount = 0;

        for (AutoPatternRequest request : requests) {
            try {
                QueueMessage message = new QueueMessage();
                message.setMessageId(UUID.randomUUID().toString());
                message.setPatternType(request.getPatternType());
                message.setPayload(request.getPayload());
                message.setUserId(request.getUserId());
                message.setCreatedAt(LocalDateTime.now());
                message.setPriority(request.getPriority() != null ? request.getPriority() : 2);
                message.setStatus("PENDING");
                message.setMetadata(request.getMetadata());

                // Serialize message thành JSON string trước khi gửi
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String jsonMessage = mapper.writeValueAsString(message);
                rabbitTemplate.convertAndSend(RabbitMQConfig.AUTO_PATTERN_QUEUE, jsonMessage);
                successCount++;
            } catch (Exception e) {
                // Log error nhưng tiếp tục với các message khác
                System.err.println("Error sending message: " + e.getMessage());
            }
        }

        BatchResponse response = new BatchResponse();
        response.setTotal(requests.size());
        response.setSuccess(successCount);
        response.setFailed(requests.size() - successCount);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auto-pattern/status/{jobId}
     * Check status và kết quả của job
     *
     * @param jobId Job ID để check status
     * @return ResponseEntity với JobResult hoặc 404 nếu không tìm thấy
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobResult> getJobStatus(@PathVariable String jobId) {
        Optional<JobResult> jobResult = jobResultService.getJobResult(jobId);
        return jobResult.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/auto-pattern/upload
     * Upload file và convert sang base64 string
     *
     * @param file File cần upload
     * @return ResponseEntity với base64 string
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new FileUploadResponse(
                        null, 
                        "File is empty", 
                        null
                ));
            }

            // Convert file sang base64
            byte[] fileBytes = file.getBytes();
            String base64String = Base64.getEncoder().encodeToString(fileBytes);

            // Tạo response với format giống payload của trigger API
            FileUploadResponse response = new FileUploadResponse(
                    base64String,
                    "File converted to base64 successfully",
                    file.getOriginalFilename()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new FileUploadResponse(
                    null,
                    "Failed to convert file: " + e.getMessage(),
                    null
            ));
        }
    }

    /**
     * POST /api/auto-pattern/base64
     * Nhận base64 string và validate
     *
     * @param request Request chứa base64 string
     * @return ResponseEntity với thông tin validation
     */
    @PostMapping("/base64")
    public ResponseEntity<Base64Response> processBase64(@RequestBody Base64Request request) {
        try {
            if (request.getBase64Data() == null || request.getBase64Data().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new Base64Response(
                        null,
                        "Base64 data is required",
                        false
                ));
            }

            // Validate base64 format
            try {
                Base64.getDecoder().decode(request.getBase64Data());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new Base64Response(
                        null,
                        "Invalid base64 format: " + e.getMessage(),
                        false
                ));
            }

            // Trả về payload format để dùng với FILE_CONVERTER
            String payload = String.format("{\"data\": \"%s\"}", request.getBase64Data());
            
            Base64Response response = new Base64Response(
                    payload,
                    "Base64 is valid. Use this payload for FILE_CONVERTER.",
                    true
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new Base64Response(
                    null,
                    "Failed to process base64: " + e.getMessage(),
                    false
            ));
        }
    }

    /**
     * DTO cho response file upload
     */
    public static class FileUploadResponse {
        private String base64Data;
        private String message;
        private String filename;

        public FileUploadResponse(String base64Data, String message, String filename) {
            this.base64Data = base64Data;
            this.message = message;
            this.filename = filename;
        }

        // Getters and Setters
        public String getBase64Data() {
            return base64Data;
        }

        public void setBase64Data(String base64Data) {
            this.base64Data = base64Data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }

    /**
     * DTO cho request base64
     */
    public static class Base64Request {
        private String base64Data;

        // Getters and Setters
        public String getBase64Data() {
            return base64Data;
        }

        public void setBase64Data(String base64Data) {
            this.base64Data = base64Data;
        }
    }

    /**
     * DTO cho response base64
     */
    public static class Base64Response {
        private String payload;
        private String message;
        private boolean valid;

        public Base64Response(String payload, String message, boolean valid) {
            this.payload = payload;
            this.message = message;
            this.valid = valid;
        }

        // Getters and Setters
        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }

    /**
     * DTO cho response trigger
     */
    public static class TriggerResponse {
        private String jobId;
        private String status;
        private String message;

        // Getters and Setters
        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * DTO cho request trigger auto pattern
     */
    public static class AutoPatternRequest {
        private String patternType;
        private String payload;
        private Long userId;
        private Integer priority;
        private String metadata;

        // Getters and Setters
        public String getPatternType() {
            return patternType;
        }

        public void setPatternType(String patternType) {
            this.patternType = patternType;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public String getMetadata() {
            return metadata;
        }

        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * DTO cho response batch
     */
    public static class BatchResponse {
        private int total;
        private int success;
        private int failed;

        // Getters and Setters
        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }
    }
}
