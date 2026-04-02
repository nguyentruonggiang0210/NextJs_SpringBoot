package com.example.demo.dto;

import com.example.demo.entity.VehicleTelemetry;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Pagination wrapper returned by the telemetryPage GraphQL query.
 * Contains the current page items, total record count, and a hasMore flag.
 */
@Data
@AllArgsConstructor
public class TelemetryPageResult {

    /** The items for the current page. */
    private List<VehicleTelemetry> items;

    /** Total number of records in the table. */
    private int total;

    /** True when there are more records beyond the current offset+limit. */
    private boolean hasMore;
}
