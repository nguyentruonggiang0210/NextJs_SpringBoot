"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "../../lib/config";

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const PAGE_SIZE = 50;

// ---------------------------------------------------------------------------
// GraphQL query — paginated
// ---------------------------------------------------------------------------

const TELEMETRY_PAGE_QUERY = `
  query TelemetryPage($offset: Int!, $limit: Int!) {
    telemetryPage(offset: $offset, limit: $limit) {
      total
      hasMore
      items {
        deviceId vinNumber plateNumber firmwareVersion hardwareSerial
        lastLatitude lastLongitude altitude speedKmh headingDegree
        gpsSatelliteCount gpsSignalStrength currentCity currentCountry isRoaming
      engineRpm engineLoadPercent coolantTemperature fuelLevelPercent
      fuelConsumptionRate oilPressure batteryVoltage odometerKm
      tripDistanceKm engineRuntimeHours
      isEngineOn isMoving isOverspeeding brakePedalStatus seatbeltDriverLocked
      tirePressureFl tirePressureFr tirePressureRl tirePressureRr impactSensorGForce
      outsideTemp cabinTemp humidityPercent airQualityIndex rainSensorActive
      headlightStatus doorFlOpen doorFrOpen doorRlOpen doorRrOpen
      lastTransmissionAt createdAt updatedAt errorCodeCount lastErrorCode
      emergencyButtonPressed maintenanceDueKm cellularNetworkType simIccid isActiveMonitoring
      }
    }
  }
`;

// ---------------------------------------------------------------------------
// Column definitions — grouped for readability
// ---------------------------------------------------------------------------
type ColGroup = { label: string; cols: { key: string; header: string }[] };

const COLUMN_GROUPS: ColGroup[] = [
  {
    label: "Hardware Identity",
    cols: [
      { key: "deviceId", header: "Device ID" },
      { key: "vinNumber", header: "VIN" },
      { key: "plateNumber", header: "Plate" },
      { key: "firmwareVersion", header: "Firmware" },
      { key: "hardwareSerial", header: "Serial" },
    ],
  },
  {
    label: "Real-Time Location",
    cols: [
      { key: "lastLatitude", header: "Lat" },
      { key: "lastLongitude", header: "Lng" },
      { key: "altitude", header: "Alt (m)" },
      { key: "speedKmh", header: "Speed (km/h)" },
      { key: "headingDegree", header: "Heading°" },
      { key: "gpsSatelliteCount", header: "Satellites" },
      { key: "gpsSignalStrength", header: "GPS Signal" },
      { key: "currentCity", header: "City" },
      { key: "currentCountry", header: "Country" },
      { key: "isRoaming", header: "Roaming" },
    ],
  },
  {
    label: "Engine & Fuel",
    cols: [
      { key: "engineRpm", header: "RPM" },
      { key: "engineLoadPercent", header: "Engine Load%" },
      { key: "coolantTemperature", header: "Coolant °C" },
      { key: "fuelLevelPercent", header: "Fuel%" },
      { key: "fuelConsumptionRate", header: "L/100km" },
      { key: "oilPressure", header: "Oil Press" },
      { key: "batteryVoltage", header: "Battery V" },
      { key: "odometerKm", header: "Odometer" },
      { key: "tripDistanceKm", header: "Trip km" },
      { key: "engineRuntimeHours", header: "Runtime h" },
    ],
  },
  {
    label: "Safety & Sensors",
    cols: [
      { key: "isEngineOn", header: "Engine On" },
      { key: "isMoving", header: "Moving" },
      { key: "isOverspeeding", header: "Overspeed" },
      { key: "brakePedalStatus", header: "Brake" },
      { key: "seatbeltDriverLocked", header: "Seatbelt" },
      { key: "tirePressureFl", header: "Tire FL" },
      { key: "tirePressureFr", header: "Tire FR" },
      { key: "tirePressureRl", header: "Tire RL" },
      { key: "tirePressureRr", header: "Tire RR" },
      { key: "impactSensorGForce", header: "G-Force" },
    ],
  },
  {
    label: "Environment",
    cols: [
      { key: "outsideTemp", header: "Outside °C" },
      { key: "cabinTemp", header: "Cabin °C" },
      { key: "humidityPercent", header: "Humidity%" },
      { key: "airQualityIndex", header: "AQI" },
      { key: "rainSensorActive", header: "Rain" },
      { key: "headlightStatus", header: "Headlight" },
      { key: "doorFlOpen", header: "Door FL" },
      { key: "doorFrOpen", header: "Door FR" },
      { key: "doorRlOpen", header: "Door RL" },
      { key: "doorRrOpen", header: "Door RR" },
    ],
  },
  {
    label: "Diagnostics",
    cols: [
      { key: "lastTransmissionAt", header: "Last TX" },
      { key: "createdAt", header: "Created" },
      { key: "updatedAt", header: "Updated" },
      { key: "errorCodeCount", header: "Error Count" },
      { key: "lastErrorCode", header: "Error Code" },
      { key: "emergencyButtonPressed", header: "Emergency" },
      { key: "maintenanceDueKm", header: "Maint Due km" },
      { key: "cellularNetworkType", header: "Network" },
      { key: "simIccid", header: "SIM ICCID" },
      { key: "isActiveMonitoring", header: "Monitoring" },
    ],
  },
];

// ---------------------------------------------------------------------------
// Cell renderer
// ---------------------------------------------------------------------------

const FLAG_KEYS = new Set([
  "isRoaming", "isEngineOn", "isMoving", "isOverspeeding",
  "brakePedalStatus", "seatbeltDriverLocked", "rainSensorActive",
  "doorFlOpen", "doorFrOpen", "doorRlOpen", "doorRrOpen",
  "emergencyButtonPressed", "isActiveMonitoring",
]);

/**
 * Renders a table cell value: flag integers become coloured badges,
 * timestamps are shortened, nulls become a dash.
 *
 * @param key   column key
 * @param value raw value from the API
 * @returns formatted React node
 */
function renderCell(key: string, value: unknown): React.ReactNode {
  if (value === null || value === undefined)
    return <span className="text-zinc-400">—</span>;

  if (FLAG_KEYS.has(key)) {
    const on = Number(value) === 1;
    const isAlert = key === "isOverspeeding" || key === "emergencyButtonPressed";
    return (
      <span
        className={`px-1.5 py-0.5 rounded text-xs font-medium ${
          on && isAlert
            ? "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400"
            : on
            ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
            : "bg-zinc-100 text-zinc-500 dark:bg-zinc-700 dark:text-zinc-400"
        }`}
      >
        {on ? "Yes" : "No"}
      </span>
    );
  }

  if (typeof value === "string" && value.includes("T")) {
    return <span title={value}>{value.slice(0, 16).replace("T", " ")}</span>;
  }

  return String(value);
}

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TelemetryRow = Record<string, any>;

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function TelemetryPage() {
  const router = useRouter();

  const [rows, setRows] = useState<TelemetryRow[]>([]);
  const [total, setTotal] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeGroup, setActiveGroup] = useState(0);

  const offsetRef = useRef(0);
  const fetchingRef = useRef(false);
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  // Auth guard
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const raw = localStorage.getItem("user");
    if (!token || !raw) { router.push("/login"); return; }
    try {
      const user = JSON.parse(raw);
      if (user?.permissionName !== "member" && user?.permissionName !== "admin") {
        router.push("/forbidden");
      }
    } catch {
      router.push("/login");
    }
  }, [router]);

  /**
   * Fetches the next PAGE_SIZE rows from the GraphQL paginated endpoint
   * and appends them to the existing rows array.
   */
  const fetchNextPage = useCallback(async () => {
    if (fetchingRef.current) return;
    fetchingRef.current = true;
    setLoading(true);

    try {
      const res = await fetch(`${API_BASE}/graphql`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: TELEMETRY_PAGE_QUERY,
          variables: { offset: offsetRef.current, limit: PAGE_SIZE },
        }),
      });

      const json = await res.json();

      if (json.errors) {
        setError(json.errors[0]?.message ?? "GraphQL error");
        return;
      }

      const page = json.data?.telemetryPage;
      if (!page) return;

      setRows((prev) => [...prev, ...page.items]);
      setTotal(page.total);
      setHasMore(page.hasMore);
      offsetRef.current += page.items.length;
    } catch {
      setError("Không thể kết nối tới server.");
    } finally {
      setLoading(false);
      setInitialLoading(false);
      fetchingRef.current = false;
    }
  }, []);

  // Initial load
  useEffect(() => {
    fetchNextPage();
  }, [fetchNextPage]);

  // IntersectionObserver — triggers the next fetch when sentinel is visible
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !fetchingRef.current) {
          fetchNextPage();
        }
      },
      { rootMargin: "200px" }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, fetchNextPage, initialLoading]);

  const group = COLUMN_GROUPS[activeGroup];
  const groupCols = group.cols.filter(
    (c) => c.key !== "deviceId" && c.key !== "vinNumber"
  );

  return (
    <div className="min-h-screen bg-zinc-100 dark:bg-zinc-900 p-6">
      {/* Title */}
      <div className="mb-4">
        <h1 className="text-2xl font-bold text-zinc-800 dark:text-zinc-100">
          Vehicle Telemetry
        </h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
          Bảng{" "}
          <code className="bg-zinc-200 dark:bg-zinc-700 px-1 rounded text-xs">
            behicle_telemetry_master
          </code>
          {total > 0 && (
            <span className="ml-2">
              — hiển thị{" "}
              <span className="font-semibold text-zinc-700 dark:text-zinc-200">
                {rows.length.toLocaleString()}
              </span>{" "}
              /{" "}
              <span className="font-semibold text-zinc-700 dark:text-zinc-200">
                {total.toLocaleString()}
              </span>{" "}
              bản ghi
            </span>
          )}
        </p>
      </div>

      {/* Group tabs */}
      <div className="flex flex-wrap gap-2 mb-4">
        {COLUMN_GROUPS.map((g, i) => (
          <button
            key={g.label}
            onClick={() => setActiveGroup(i)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              activeGroup === i
                ? "bg-blue-600 text-white"
                : "bg-white dark:bg-zinc-800 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-200 dark:hover:bg-zinc-700 border border-zinc-200 dark:border-zinc-700"
            }`}
          >
            {g.label}
          </button>
        ))}
      </div>

      {/* Initial skeleton */}
      {initialLoading && (
        <div className="space-y-2">
          {Array.from({ length: 12 }).map((_, i) => (
            <div
              key={i}
              className="h-10 bg-zinc-200 dark:bg-zinc-700 rounded-lg animate-pulse"
            />
          ))}
        </div>
      )}

      {error && <p className="text-sm text-red-500 mb-4">{error}</p>}

      {/* Table */}
      {!initialLoading && !error && (
        <div className="bg-white dark:bg-zinc-800 rounded-2xl shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="sticky top-0 z-10">
                <tr className="bg-zinc-50 dark:bg-zinc-700 border-b border-zinc-200 dark:border-zinc-600">
                  <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-500 dark:text-zinc-400 uppercase tracking-wide whitespace-nowrap">
                    #
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-zinc-500 dark:text-zinc-400 uppercase tracking-wide whitespace-nowrap">
                    VIN / Plate
                  </th>
                  {groupCols.map((col) => (
                    <th
                      key={col.key}
                      className="px-4 py-3 text-left text-xs font-semibold text-zinc-500 dark:text-zinc-400 uppercase tracking-wide whitespace-nowrap"
                    >
                      {col.header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-700">
                {rows.length === 0 && !loading && (
                  <tr>
                    <td colSpan={groupCols.length + 2} className="px-4 py-8 text-center text-sm text-zinc-400">
                      Không có dữ liệu
                    </td>
                  </tr>
                )}
                {rows.map((row) => (
                  <tr
                    key={row.deviceId}
                    className="hover:bg-zinc-50 dark:hover:bg-zinc-700/30 transition-colors"
                  >
                    <td className="px-4 py-2 font-mono text-xs text-zinc-400 whitespace-nowrap">
                      {row.deviceId}
                    </td>
                    <td className="px-4 py-2 whitespace-nowrap">
                      <div className="text-xs font-medium text-zinc-800 dark:text-zinc-100">
                        {row.vinNumber}
                      </div>
                      <div className="text-xs text-zinc-400">{row.plateNumber ?? "—"}</div>
                    </td>
                    {groupCols.map((col) => (
                      <td
                        key={col.key}
                        className="px-4 py-2 whitespace-nowrap text-zinc-700 dark:text-zinc-300"
                      >
                        {renderCell(col.key, row[col.key])}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Sentinel + spinner */}
          <div ref={sentinelRef} className="px-4 py-4 flex items-center justify-center min-h-[48px]">
            {loading && (
              <div className="flex items-center gap-2 text-sm text-zinc-400">
                <svg
                  className="animate-spin h-4 w-4 text-blue-500"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                </svg>
                Đang tải thêm...
              </div>
            )}
            {!loading && !hasMore && rows.length > 0 && (
              <p className="text-xs text-zinc-400">
                Đã hiển thị hết {total.toLocaleString()} bản ghi
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
