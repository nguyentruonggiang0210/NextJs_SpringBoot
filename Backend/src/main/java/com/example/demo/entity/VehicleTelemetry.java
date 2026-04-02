package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity that maps to the behicle_telemetry_master table.
 * Contains 55 columns across 6 groups: hardware identity, real-time location,
 * engine/fuel metrics, safety/sensors, environmental data, and logging/diagnostics.
 */
@Entity
@Table(name = "behicle_telemetry_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTelemetry {

    // -------------------------------------------------------------------------
    // GROUP 1: HARDWARE IDENTITY
    // -------------------------------------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;

    @Column(nullable = false, unique = true)
    private String vinNumber;

    private String plateNumber;
    private String firmwareVersion;
    private String hardwareSerial;

    // -------------------------------------------------------------------------
    // GROUP 2: REAL-TIME LOCATION
    // -------------------------------------------------------------------------

    private Double lastLatitude;
    private Double lastLongitude;
    private Double altitude;
    private Double speedKmh;
    private Integer headingDegree;
    private Integer gpsSatelliteCount;
    private Integer gpsSignalStrength;
    private String currentCity;
    private String currentCountry;
    private Integer isRoaming;

    // -------------------------------------------------------------------------
    // GROUP 3: ENGINE & FUEL METRICS
    // -------------------------------------------------------------------------

    private Integer engineRpm;
    private Double engineLoadPercent;
    private Double coolantTemperature;
    private Double fuelLevelPercent;
    private Double fuelConsumptionRate;
    private Double oilPressure;
    private Double batteryVoltage;
    private Double odometerKm;
    private Double tripDistanceKm;
    private Double engineRuntimeHours;

    // -------------------------------------------------------------------------
    // GROUP 4: SAFETY & SENSORS
    // -------------------------------------------------------------------------

    private Integer isEngineOn;
    private Integer isMoving;
    private Integer isOverspeeding;
    private Integer brakePedalStatus;
    private Integer seatbeltDriverLocked;
    private Double tirePressureFl;
    private Double tirePressureFr;
    private Double tirePressureRl;
    private Double tirePressureRr;
    private Double impactSensorGForce;

    // -------------------------------------------------------------------------
    // GROUP 5: ENVIRONMENTAL DATA
    // -------------------------------------------------------------------------

    private Double outsideTemp;
    private Double cabinTemp;
    private Double humidityPercent;
    private Integer airQualityIndex;
    private Integer rainSensorActive;
    private String headlightStatus;
    private Integer doorFlOpen;
    private Integer doorFrOpen;
    private Integer doorRlOpen;
    private Integer doorRrOpen;

    // -------------------------------------------------------------------------
    // GROUP 6: LOGGING & DIAGNOSTICS
    // -------------------------------------------------------------------------

    private LocalDateTime lastTransmissionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer errorCodeCount;
    private String lastErrorCode;
    private Integer emergencyButtonPressed;
    private Double maintenanceDueKm;
    private String cellularNetworkType;
    private String simIccid;
    private Integer isActiveMonitoring;
}
