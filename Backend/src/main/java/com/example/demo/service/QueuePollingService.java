package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.JobResultSummary;
import com.example.demo.dto.QueueMessage;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import jakarta.annotation.PreDestroy;

/**
 * Service với polling thread để nhận và xử lý message từ RabbitMQ queue
 * Sử dụng polling thay vì @RabbitListener để có control tốt hơn
 */
@Service
public class QueuePollingService {

    private static final Logger logger = LoggerFactory.getLogger(QueuePollingService.class);

    private final ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Connection connection;
    private Channel channel;
    private final JobResultService jobResultService;
    private final SimpMessagingTemplate messagingTemplate;

    // Cấu hình polling
    private static final long POLL_INTERVAL_MS = 1000; // 1 giây
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 5000;

    public QueuePollingService(JobResultService jobResultService, SimpMessagingTemplate messagingTemplate) {
        this.jobResultService = jobResultService;
        this.messagingTemplate = messagingTemplate;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "QueuePollingThread");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Khởi động polling thread
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            executorService.submit(this::pollingLoop);
            logger.info("Queue polling service started");
        }
    }

    /**
     * Dừng polling thread
     */
    @PreDestroy
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Đóng connection
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                if (connection != null && connection.isOpen()) {
                    connection.close();
                }
            } catch (Exception e) {
                logger.error("Error closing RabbitMQ connection", e);
            }

            logger.info("Queue polling service stopped");
        }
    }

    /**
     * Polling loop - liên tục pull message từ queue
     */
    private void pollingLoop() {
        int retryCount = 0;

        while (isRunning.get()) {
            try {
                // Kết nối đến RabbitMQ nếu chưa có
                if (connection == null || !connection.isOpen()) {
                    establishConnection();
                    retryCount = 0; // Reset retry count khi kết nối thành công
                }

                // Poll message từ queue
                if (channel != null && channel.isOpen()) {
                    pollMessages();
                }

                // Sleep trước khi poll lần tiếp theo
                Thread.sleep(POLL_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in polling loop", e);
                retryCount++;

                if (retryCount >= MAX_RETRIES) {
                    logger.error("Max retries reached, stopping polling service");
                    stop();
                    break;
                }

                // Đợi trước khi thử lại
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Thiết lập kết nối đến RabbitMQ
     */
    private void establishConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        connection = factory.newConnection();
        channel = connection.createChannel();

        // Declare queue (đảm bảo queue tồn tại)
        channel.queueDeclare(
                RabbitMQConfig.AUTO_PATTERN_QUEUE,
                true,  // durable
                false, // exclusive
                false, // auto-delete
                null   // arguments
        );

        logger.info("Connected to RabbitMQ and declared queue: {}", RabbitMQConfig.AUTO_PATTERN_QUEUE);
    }

    /**
     * Poll messages từ queue và xử lý
     */
    private void pollMessages() {
        try {
            // Lấy message từ queue (non-blocking, chỉ lấy 1 message mỗi lần)
            var response = channel.basicGet(RabbitMQConfig.AUTO_PATTERN_QUEUE, false);

            if (response != null) {
                String messageBody = new String(response.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = response.getEnvelope().getDeliveryTag();

                // Log message body để debug
                logger.info("Received message body: {}", messageBody);

                try {
                    // Parse và xử lý message
                    QueueMessage message = parseMessage(messageBody);
                    processMessage(message);

                    // Ack message sau khi xử lý thành công
                    channel.basicAck(deliveryTag, false);
                    logger.info("Processed and acknowledged message: {}", message.getMessageId());

                } catch (Exception e) {
                    logger.error("Error processing message, rejecting: {}", messageBody, e);
                    // Lưu lỗi vào job result
                    String jobId = extractJobId(messageBody);
                    if (jobId != null) {
                        jobResultService.markAsFailed(jobId, e.getMessage());
                    }
                    // Reject message (không requeue để tránh lặp vô hạn)
                    channel.basicReject(deliveryTag, false);
                }
            }

        } catch (Exception e) {
            logger.error("Error polling messages", e);
        }
    }

    /**
     * Extract jobId từ JSON message body (dùng khi parse thất bại)
     */
    private String extractJobId(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
            return node.has("messageId") ? node.get("messageId").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse message từ JSON string
     */
    private QueueMessage parseMessage(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // Đăng ký JavaTimeModule để hỗ trợ LocalDateTime
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.readValue(json, QueueMessage.class);
        } catch (Exception e) {
            logger.error("Error parsing message: {}", json, e);
            throw new RuntimeException("Failed to parse message", e);
        }
    }

    /**
     * Xử lý message - implement logic xử lý ở đây
     */
    private void processMessage(QueueMessage message) {
        String jobId = message.getMessageId();
        logger.info("Processing message - ID: {}, Type: {}, User: {}",
                jobId,
                message.getPatternType(),
                message.getUserId());

        // Tạo job result mới nếu chưa tồn tại
        if (!jobResultService.jobExists(jobId)) {
            jobResultService.createJobResult(message);
        }

        // Đánh dấu đang xử lý
        jobResultService.markAsProcessing(jobId);

        // Gửi WebSocket notification khi status = PROCESSING
        sendWebSocketNotification(jobId, message.getUserId());

        try {
            // TODO: Implement logic xử lý pattern cụ thể ở đây
            // Ví dụ:
            String result;
            switch (message.getPatternType()) {
                case "DATA_ANALYSIS":
                    result = handleDataAnalysis(message);
                    break;
                case "REPORT_GENERATION":
                    result = handleReportGeneration(message);
                    break;
                case "NOTIFICATION":
                    result = handleNotification(message);
                    break;
                case "FILE_CONVERTER":
                    result = handleFileConverter(message);
                    break;
                default:
                    logger.warn("Unknown pattern type: {}", message.getPatternType());
                    result = "Unknown pattern type: " + message.getPatternType();
            }

            // Đánh dấu hoàn thành với kết quả
            jobResultService.markAsCompleted(jobId, result);

            // Gửi WebSocket notification khi status = COMPLETED
            sendWebSocketNotification(jobId, message.getUserId());

            logger.info("Message processed successfully: {}", jobId);

        } catch (Exception e) {
            jobResultService.markAsFailed(jobId, e.getMessage());

            // Gửi WebSocket notification khi status = FAILED
            sendWebSocketNotification(jobId, message.getUserId());

            logger.error("Failed to process message: {}", jobId, e);
            throw e;
        }
    }

    /**
     * Gửi WebSocket notification khi job status thay đổi
     */
    private void sendWebSocketNotification(String jobId, Long userId) {
        try {
            jobResultService.getJobResult(jobId).ifPresent(jobResult -> {
                String destination = "/topic/job-results/" + userId;
                
                // Create summary message to avoid buffer size limit
                JobResultSummary summary = new JobResultSummary(
                    jobResult.getJobId(),
                    jobResult.getPatternType(),
                    jobResult.getStatus(),
                    jobResult.getErrorMessage(),
                    jobResult.getCreatedAt(),
                    jobResult.getStartedAt(),
                    jobResult.getCompletedAt(),
                    jobResult.getUserId()
                );
                
                // For completed jobs, include brief result info instead of full base64
                if ("COMPLETED".equals(jobResult.getStatus()) && jobResult.getResult() != null) {
                    if (jobResult.getResult().contains("File converted and saved successfully")) {
                        summary.setBriefResult("File processed successfully");
                    } else {
                        // Truncate result to 100 chars if too long
                        String result = jobResult.getResult();
                        if (result.length() > 100) {
                            summary.setBriefResult(result.substring(0, 100) + "...");
                        } else {
                            summary.setBriefResult(result);
                        }
                    }
                }
                
                messagingTemplate.convertAndSend(destination, summary);
                logger.info("Sent WebSocket notification to {}: status = {}", destination, jobResult.getStatus());
            });
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification for job: {}", jobId, e);
        }
    }

    /**
     * Xử lý pattern phân tích dữ liệu
     */
    private String handleDataAnalysis(QueueMessage message) {
        logger.info("Handling data analysis for message: {}", message.getMessageId());
        // Implement logic phân tích dữ liệu
        // Có thể gọi các service khác, xử lý payload, v.v.
        return "Data analysis completed for payload: " + message.getPayload();
    }

    /**
     * Xử lý pattern tạo báo cáo
     */
    private String handleReportGeneration(QueueMessage message) {
        logger.info("Handling report generation for message: {}", message.getMessageId());
        // Implement logic tạo báo cáo
        return "Report generated for user: " + message.getUserId();
    }

    /**
     * Xử lý pattern thông báo
     */
    private String handleNotification(QueueMessage message) {
        logger.info("Handling notification for message: {}", message.getMessageId());
        // Implement logic gửi thông báo
        return "Notification sent for user: " + message.getUserId();
    }

    /**
     * Xử lý pattern chuyển đổi file từ base64
     */
    private String handleFileConverter(QueueMessage message) {
        logger.info("Handling file converter for message: {}", message.getMessageId());
        
        try {
            // Parse payload JSON để lấy base64 data
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode payloadNode = mapper.readTree(message.getPayload());
            
            if (!payloadNode.has("data")) {
                throw new IllegalArgumentException("Payload must contain 'data' field with base64 string");
            }
            
            String base64Data = payloadNode.get("data").asText();
            
            // Decode base64 thành byte array
            byte[] fileBytes = Base64.getDecoder().decode(base64Data);
            
            // Tạo filename unique
            String filename = "file_" + UUID.randomUUID().toString() + ".bin";
            
            // Lưu file vào absolute path
            Path filesDir = Paths.get("Backend/src/main/resources/files");
            if (!Files.exists(filesDir)) {
                Files.createDirectories(filesDir);
            }
            
            Path filePath = filesDir.resolve(filename);
            Files.write(filePath, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            
            String result = String.format("File converted and saved successfully. Filename: %s, Size: %d bytes", 
                    filename, fileBytes.length);
            
            logger.info("File converter completed: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Error in file converter: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert and save file: " + e.getMessage(), e);
        }
    }
}
