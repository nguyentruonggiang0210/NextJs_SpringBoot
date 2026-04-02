package com.example.demo.controller;

import com.example.demo.dto.TelemetryPageResult;
import com.example.demo.entity.VehicleTelemetry;
import com.example.demo.repository.VehicleTelemetryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL resolver for vehicle telemetry queries.
 * Exposes the behicle_telemetry_master table via the GraphQL endpoint at /graphql.
 */
@Controller
public class TelemetryGraphQLController {

    private static final int MAX_LIMIT = 200;

    private final VehicleTelemetryRepository repository;

    public TelemetryGraphQLController(VehicleTelemetryRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns all vehicle telemetry records (kept for backward compatibility).
     *
     * @return list of all VehicleTelemetry entities
     */
    @QueryMapping
    public List<VehicleTelemetry> allTelemetry() {
        return repository.findAll();
    }

    /**
     * Returns a single telemetry record by its device ID.
     *
     * @param deviceId the primary key of the device
     * @return the matching VehicleTelemetry, or null if not found
     */
    @QueryMapping
    public VehicleTelemetry telemetryById(@Argument Long deviceId) {
        return repository.findById(deviceId).orElse(null);
    }

    /**
     * Returns a paginated slice of telemetry records ordered by deviceId ascending.
     * Supports infinite scroll on the frontend.
     *
     * @param offset number of records to skip
     * @param limit  number of records to return (capped at MAX_LIMIT)
     * @return TelemetryPageResult containing items, total count, and hasMore flag
     */
    @QueryMapping
    public TelemetryPageResult telemetryPage(@Argument int offset, @Argument int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        int safeOffset = Math.max(offset, 0);

        int page = safeOffset / safeLimit;
        PageRequest pageRequest = PageRequest.of(page, safeLimit, Sort.by("deviceId").ascending());

        List<VehicleTelemetry> items = repository.findAll(pageRequest).getContent();
        long total = repository.count();
        boolean hasMore = (long) safeOffset + items.size() < total;

        return new TelemetryPageResult(items, (int) total, hasMore);
    }
}
