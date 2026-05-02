package com.example.demo.service;

import com.example.demo.dto.QueueMessage;
import com.example.demo.entity.JobResult;
import com.example.demo.repository.JobResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service để quản lý JobResult - lưu và truy vấn kết quả xử lý job
 */
@Service
public class JobResultService {

    private final JobResultRepository jobResultRepository;

    public JobResultService(JobResultRepository jobResultRepository) {
        this.jobResultRepository = jobResultRepository;
    }

    /**
     * Tạo job result mới khi nhận message từ queue
     */
    @Transactional
    public JobResult createJobResult(QueueMessage message) {
        JobResult jobResult = new JobResult();
        jobResult.setJobId(message.getMessageId());
        jobResult.setPatternType(message.getPatternType());
        jobResult.setPayload(message.getPayload());
        jobResult.setUserId(message.getUserId());
        jobResult.setStatus("PENDING");
        jobResult.setCreatedAt(LocalDateTime.now());
        jobResult.setMetadata(message.getMetadata());

        return jobResultRepository.save(jobResult);
    }

    /**
     * Cập nhật status khi bắt đầu xử lý
     */
    @Transactional
    public void markAsProcessing(String jobId) {
        jobResultRepository.findByJobId(jobId).ifPresent(jobResult -> {
            jobResult.setStatus("PROCESSING");
            jobResult.setStartedAt(LocalDateTime.now());
            jobResultRepository.save(jobResult);
        });
    }

    /**
     * Cập nhật kết quả khi xử lý thành công
     */
    @Transactional
    public void markAsCompleted(String jobId, String result) {
        jobResultRepository.findByJobId(jobId).ifPresent(jobResult -> {
            jobResult.setStatus("COMPLETED");
            jobResult.setResult(result);
            jobResult.setCompletedAt(LocalDateTime.now());
            jobResultRepository.save(jobResult);
        });
    }

    /**
     * Cập nhật lỗi khi xử lý thất bại
     */
    @Transactional
    public void markAsFailed(String jobId, String errorMessage) {
        jobResultRepository.findByJobId(jobId).ifPresent(jobResult -> {
            jobResult.setStatus("FAILED");
            jobResult.setErrorMessage(errorMessage);
            jobResult.setCompletedAt(LocalDateTime.now());
            jobResultRepository.save(jobResult);
        });
    }

    /**
     * Lấy job result theo jobId
     */
    public Optional<JobResult> getJobResult(String jobId) {
        return jobResultRepository.findByJobId(jobId);
    }

    /**
     * Kiểm tra job tồn tại
     */
    public boolean jobExists(String jobId) {
        return jobResultRepository.existsByJobId(jobId);
    }
}
