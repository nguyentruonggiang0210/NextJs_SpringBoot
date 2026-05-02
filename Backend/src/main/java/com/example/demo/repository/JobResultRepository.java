package com.example.demo.repository;

import com.example.demo.entity.JobResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho JobResult entity
 */
@Repository
public interface JobResultRepository extends JpaRepository<JobResult, Long> {

    /**
     * Tìm job result theo jobId
     */
    Optional<JobResult> findByJobId(String jobId);

    /**
     * Kiểm tra job có tồn tại theo jobId
     */
    boolean existsByJobId(String jobId);
}
