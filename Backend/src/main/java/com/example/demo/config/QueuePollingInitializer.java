package com.example.demo.config;

import com.example.demo.service.QueuePollingService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Component để tự động khởi động QueuePollingService khi ứng dụng ready
 * Đảm bảo polling thread bắt đầu chạy ngay sau khi Spring Boot khởi động xong
 */
@Component
public class QueuePollingInitializer {

    private final QueuePollingService queuePollingService;

    public QueuePollingInitializer(QueuePollingService queuePollingService) {
        this.queuePollingService = queuePollingService;
    }

    /**
     * Khởi động polling service khi ứng dụng ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        queuePollingService.start();
    }
}
