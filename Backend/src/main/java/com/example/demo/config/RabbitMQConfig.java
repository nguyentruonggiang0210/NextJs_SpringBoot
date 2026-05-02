package com.example.demo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ cho message queue
 * Định nghĩa queue, exchange, và message converter
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Tên queue cho auto pattern messages
     */
    public static final String AUTO_PATTERN_QUEUE = "auto.pattern.queue";

    /**
     * Tên exchange (topic exchange)
     */
    public static final String AUTO_PATTERN_EXCHANGE = "auto.pattern.exchange";

    /**
     * Routing key
     */
    public static final String AUTO_PATTERN_ROUTING_KEY = "auto.pattern.routing.key";

    /**
     * Tạo queue cho auto pattern messages
     */
    @Bean
    public Queue autoPatternQueue() {
        // durable: true - queue tồn tại sau khi restart RabbitMQ
        return new Queue(AUTO_PATTERN_QUEUE, true);
    }

    /**
     * RabbitTemplate với default message converter của Spring
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}
