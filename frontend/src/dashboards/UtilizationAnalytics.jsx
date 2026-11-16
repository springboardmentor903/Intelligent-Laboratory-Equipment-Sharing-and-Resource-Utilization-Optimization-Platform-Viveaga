
import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";
import "./UtilizationAnalytics.css";
import DashboardLayout from "./DashboardLayout";

// ==========================================
// DATE CONFIGURATION
// ==========================================

const today = new Date();

const todayString = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, "0"),
    String(today.getDate()).padStart(2, "0"),
].join("-");

const firstDay = `${today.getFullYear()}-${String(
    today.getMonth() + 1
).padStart(2, "0")}-01`;

// ==========================================
// ROLE CONFIGURATION
// ==========================================

const ANALYTICS_ROLES = [
    "SYSTEM_ADMINISTRATOR",
    "INSTITUTION_ADMINISTRATOR",
    "LAB_MANAGER",
    "DEPARTMENT_HEAD",
];

const ADMIN_ROLES = [
    "SYSTEM_ADMINISTRATOR",
    "INSTITUTION_ADMINISTRATOR",
];

const ROLE_LABELS = {
    SYSTEM_ADMINISTRATOR: "System Administrator",
    INSTITUTION_ADMINISTRATOR: "Institution Administrator",
    LAB_MANAGER: "Lab Manager",
    DEPARTMENT_HEAD: "Department Head",
};

// ==========================================
// INITIAL DATA
// ==========================================

const initialData = {
    equipment: [],
    department: [],
    institution: [],
    usageMode: [],
    demand: [],
    benchmark: [],
    idle: [],
    hourlyPeak: [],
    dailyPeak: [],
    topFive: [],
    heatmap: [],
};

// ==========================================
// HELPER FUNCTIONS
// ==========================================

function formatNumber(value, digits = 2) {
    const number = Number(value);

    return Number.isFinite(number)
        ? number.toFixed(digits)
        : "0.00";
}

function formatHours(value) {
    return `${formatNumber(value)} hrs`;
}

function getPercentage(value) {
    const number = Number(value);

    return Number.isFinite(number)
        ? Math.max(0, Math.min(100, number))
        : 0;
}

// ==========================================
// PERCENTAGE BAR
// ==========================================

function PercentageBar({ value }) {
    const percentage = getPercentage(value);

    return (
        <div className="util-progress">
            <div
                className="util-progress-fill"
                style={{ width: `${percentage}%` }}
            />

            <span>{formatNumber(value)}%</span>
        </div>
    );
}

// ==========================================
// SECTION
// ==========================================

function Section({ title, description, children }) {
    return (
        <section className="util-section">
            <div className="util-section-heading">
                <div>
                    <h2>{title}</h2>

                    {description && <p>{description}</p>}
                </div>
            </div>

            {children}
        </section>
    );
}

// ==========================================
// EMPTY STATE
// ==========================================

function EmptyState({
                        message = "No data available for this period.",
                    }) {
    return (
        <div className="util-empty">
            {message}
        </div>
    );
}

// ==========================================
// MAIN COMPONENT
// ==========================================

function UtilizationAnalytics() {
    const navigate = useNavigate();

    // ==========================================
    // LOGGED-IN USER ROLE
    // ==========================================

    const role = sessionStorage.getItem("userRole");

    const userName = sessionStorage.getItem("userName");

    const hasAccess = ANALYTICS_ROLES.includes(role);

    const isAdmin = ADMIN_ROLES.includes(role);

    const roleLabel = ROLE_LABELS[role] || "User";

    // ==========================================
    // STATE
    // ==========================================

    const [startDate, setStartDate] = useState(firstDay);

    const [endDate, setEndDate] = useState(todayString);

    const [data, setData] = useState(initialData);

    const [loading, setLoading] = useState(false);

    const [errors, setErrors] = useState([]);

    // ==========================================
    // PROTECT ANALYTICS PAGE
    // ==========================================

    useEffect(() => {
        const token = sessionStorage.getItem("token");

        if (!token || !hasAccess) {
            const dashboardRoutes = {
                RESEARCHER_STUDENT: "/student-dashboard",
                LAB_TECHNICIAN: "/technician-dashboard",
            };

            const redirectPath = token
                ? dashboardRoutes[role] || "/"
                : "/";

            navigate(redirectPath, { replace: true });
        }
    }, [hasAccess, navigate, role]);

    // ==========================================
    // LOAD ANALYTICS
    // ==========================================

    const loadAnalytics = useCallback(async () => {
        if (!hasAccess) {
            return;
        }

        if (!startDate || !endDate) {
            setErrors(["Please select both dates."]);
            return;
        }

        if (startDate > endDate) {
            setErrors([
                "Start date cannot be after end date.",
            ]);
            return;
        }

        setLoading(true);
        setErrors([]);

        const params = {
            startDate,
            endDate,
        };

        // All requests are scoped by the backend
        // according to the authenticated user's role.

        const requests = [
            [
                "equipment",
                "/analytics/utilization/equipment",
                params,
            ],
            [
                "department",
                "/analytics/utilization/department",
                params,
            ],
            [
                "institution",
                "/analytics/utilization/institution",
                params,
            ],
            [
                "usageMode",
                "/analytics/utilization/usage-mode",
                params,
            ],
            [
                "demand",
                "/analytics/utilization/demand",
                params,
            ],
            [
                "benchmark",
                "/analytics/utilization/historical-benchmark",
                params,
            ],
            [
                "idle",
                "/analytics/utilization/idle-time",
                undefined,
            ],
            [
                "hourlyPeak",
                "/analytics/utilization/peak-usage/hourly",
                params,
            ],
            [
                "dailyPeak",
                "/analytics/utilization/peak-usage/daily",
                params,
            ],
            [
                "topFive",
                "/analytics/utilization/peak-usage/top-five",
                params,
            ],
            [
                "heatmap",
                "/analytics/utilization/heatmap",
                params,
            ],
        ];

        try {
            const results = await Promise.allSettled(
                requests.map(([, url, requestParams]) =>
                    api.get(
                        url,
                        requestParams
                            ? { params: requestParams }
                            : {}
                    )
                )
            );

            const newData = { ...initialData };

            const failedRequests = [];

            results.forEach((result, index) => {
                const key = requests[index][0];

                if (result.status === "fulfilled") {
                    newData[key] = Array.isArray(
                        result.value.data
                    )
                        ? result.value.data.filter(Boolean)
                        : [];

                } else {
                    newData[key] = [];

                    const status =
                        result.reason?.response?.status;

                    const message =
                        status === 403
                            ? "Access denied for this analytics section"
                            : status === 401
                                ? "Session expired. Please log in again."
                                : result.reason?.message ||
                                "Request failed";

                    failedRequests.push(
                        `${key}: ${message}`
                    );
                }
            });

            setData(newData);

            setErrors(failedRequests);

            // If the session has expired, return to login.
            if (
                failedRequests.some((error) =>
                    error.includes("Session expired")
                )
            ) {
                sessionStorage.clear();

                navigate("/", { replace: true });
            }

        } catch (error) {
            console.error(
                "Analytics loading error:",
                error
            );

            setErrors([
                "Unable to load analytics. Please try again.",
            ]);

        } finally {
            setLoading(false);
        }
    }, [startDate, endDate, hasAccess, navigate]);

    // ==========================================
    // INITIAL LOAD
    // ==========================================

    useEffect(() => {
        if (hasAccess) {
            loadAnalytics();
        }
    }, [hasAccess, loadAnalytics]);

    // ==========================================
    // SUMMARY CALCULATIONS
    // ==========================================

    const equipmentCount = data.equipment.length;

    const averageUtilization =
        equipmentCount > 0
            ? data.equipment.reduce(
            (total, item) =>
                total +
                Number(
                    item.utilizationPercentage || 0
                ),
            0
        ) / equipmentCount
            : 0;

    const totalUsage = data.equipment.reduce(
        (total, item) =>
            total +
            Number(item.actualUsageHours || 0),
        0
    );

    const totalAvailable = data.equipment.reduce(
        (total, item) =>
            total +
            Number(item.availableHours || 0),
        0
    );

    const totalIdle = data.equipment.reduce(
        (total, item) =>
            total +
            Number(item.idleHours || 0),
        0
    );

    // ==========================================
    // UNAUTHORIZED PAGE
    // ==========================================

    if (!hasAccess) {
        return null;
    }

    // ==========================================
    // UI
    // ==========================================

    return (
        <DashboardLayout>
            <div className="util-page">

                {/* ==================================
                    HEADER
                ================================== */}

                <header className="util-header">
                    <div>
                        <p className="util-eyebrow">
                            LAB RESOURCE MANAGEMENT
                        </p>

                        <h1>Utilization Analytics</h1>

                        <p>
                            Monitor equipment usage, demand,
                            idle time and laboratory performance.
                        </p>

                        <div className="util-role-info">
                            <span>
                                Logged in as:
                            </span>

                            <strong>
                                {userName || roleLabel}
                            </strong>

                            <span>
                                ({roleLabel})
                            </span>
                        </div>
                    </div>

                    <button
                        className="util-back-button"
                        onClick={() => navigate(-1)}
                    >
                        ← Go Back
                    </button>
                </header>

                {/* ==================================
                    DATE FILTER
                ================================== */}

                <div className="util-filter">

                    <div className="util-filter-field">
                        <label htmlFor="util-start-date">
                            Start Date
                        </label>

                        <input
                            id="util-start-date"
                            type="date"
                            value={startDate}
                            max={endDate}
                            onChange={(event) =>
                                setStartDate(event.target.value)
                            }
                        />
                    </div>

                    <div className="util-filter-field">
                        <label htmlFor="util-end-date">
                            End Date
                        </label>

                        <input
                            id="util-end-date"
                            type="date"
                            value={endDate}
                            min={startDate}
                            max={todayString}
                            onChange={(event) =>
                                setEndDate(event.target.value)
                            }
                        />
                    </div>

                    <button
                        className="util-apply-button"
                        onClick={loadAnalytics}
                        disabled={loading}
                    >
                        {loading
                            ? "Loading..."
                            : "Apply Filter"}
                    </button>

                </div>

                {/* ==================================
                    ERROR MESSAGE
                ================================== */}

                {errors.length > 0 && (
                    <div className="util-error">

                        <strong>
                            Some analytics requests failed:
                        </strong>

                        <ul>
                            {errors.map((error) => (
                                <li key={error}>
                                    {error}
                                </li>
                            ))}
                        </ul>

                        <p>
                            Check your backend server,
                            authentication and analytics permissions.
                        </p>

                    </div>
                )}

                {/* ==================================
                    LOADING
                ================================== */}

                {loading && (
                    <div className="util-loading">
                        Loading utilization analytics...
                    </div>
                )}

                {/* ==================================
                    SUMMARY CARDS
                ================================== */}

                <div className="util-summary-grid">

                    <div className="util-summary-card">
                        <span>Total Equipment</span>
                        <strong>{equipmentCount}</strong>
                    </div>

                    <div className="util-summary-card">
                        <span>Average Utilization</span>

                        <strong>
                            {formatNumber(averageUtilization)}%
                        </strong>
                    </div>

                    <div className="util-summary-card">
                        <span>Total Usage</span>

                        <strong>
                            {formatHours(totalUsage)}
                        </strong>
                    </div>

                    <div className="util-summary-card">
                        <span>Available Hours</span>

                        <strong>
                            {formatHours(totalAvailable)}
                        </strong>
                    </div>

                    <div className="util-summary-card">
                        <span>Idle Hours</span>

                        <strong>
                            {formatHours(totalIdle)}
                        </strong>
                    </div>

                </div>

                {/* ==================================
                    EQUIPMENT UTILIZATION
                ================================== */}

                <Section
                    title="Equipment Utilization"
                    description="Usage and available operating hours for each equipment."
                >
                    {data.equipment.length === 0 ? (
                        <EmptyState />
                    ) : (
                        <div className="util-table-wrapper">

                            <table className="util-table">

                                <thead>
                                <tr>
                                    <th>Equipment</th>
                                    <th>Asset Tag</th>
                                    <th>Usage Hours</th>
                                    <th>Available Hours</th>
                                    <th>Idle Hours</th>
                                    <th>Utilization</th>
                                </tr>
                                </thead>

                                <tbody>
                                {data.equipment.map((item) => (
                                    <tr key={item.equipmentId}>

                                        <td>
                                            {item.equipmentName || "—"}
                                        </td>

                                        <td>
                                            {item.assetTag || "—"}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.actualUsageHours
                                            )}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.availableHours
                                            )}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.idleHours
                                            )}
                                        </td>

                                        <td>
                                            <PercentageBar
                                                value={
                                                    item.utilizationPercentage
                                                }
                                            />
                                        </td>

                                    </tr>
                                ))}
                                </tbody>

                            </table>

                        </div>
                    )}
                </Section>

                {/* ==================================
                    DEPARTMENT UTILIZATION
                ================================== */}

                <div className="util-two-column">

                    <Section
                        title="Department Utilization"
                        description="Compare usage across accessible departments."
                    >
                        {data.department.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="util-list">

                                {data.department.map((item) => (
                                    <div
                                        className="util-list-item"
                                        key={item.departmentId}
                                    >

                                        <div className="util-list-top">
                                            <strong>
                                                {item.departmentName || "—"}
                                            </strong>

                                            <span>
                                                {formatNumber(
                                                    item.utilizationPercentage
                                                )}%
                                            </span>
                                        </div>

                                        <PercentageBar
                                            value={
                                                item.utilizationPercentage
                                            }
                                        />

                                        <small>
                                            {formatNumber(
                                                item.actualUsageHours
                                            )}
                                            {" usage hours / "}
                                            {formatNumber(
                                                item.availableHours
                                            )}
                                            {" available hours"}
                                        </small>

                                    </div>
                                ))}

                            </div>
                        )}
                    </Section>

                    {/* Institution-level comparison is admin-only */}

                    {isAdmin && (
                        <Section
                            title="Institution Utilization"
                            description={
                                role === "SYSTEM_ADMINISTRATOR"
                                    ? "Institution-level resource performance across all institutions."
                                    : "Institution-level resource performance within your institution."
                            }
                        >
                            {data.institution.length === 0 ? (
                                <EmptyState />
                            ) : (
                                <div className="util-list">

                                    {data.institution.map((item) => (
                                        <div
                                            className="util-list-item"
                                            key={item.institutionId}
                                        >

                                            <div className="util-list-top">
                                                <strong>
                                                    {item.institutionName || "—"}
                                                </strong>

                                                <span>
                                                    {formatNumber(
                                                        item.utilizationPercentage
                                                    )}%
                                                </span>
                                            </div>

                                            <PercentageBar
                                                value={
                                                    item.utilizationPercentage
                                                }
                                            />

                                            <small>
                                                {formatNumber(
                                                    item.actualUsageHours
                                                )}
                                                {" usage hours / "}
                                                {formatNumber(
                                                    item.availableHours
                                                )}
                                                {" available hours"}
                                            </small>

                                        </div>
                                    ))}

                                </div>
                            )}
                        </Section>
                    )}

                </div>

                {/* ==================================
                    SHARED VS EXCLUSIVE
                ================================== */}

                <Section
                    title="Shared vs Exclusive Usage"
                    description="Compare shared and exclusive equipment usage."
                >
                    {data.usageMode.length === 0 ? (
                        <EmptyState />
                    ) : (
                        <div className="util-table-wrapper">

                            <table className="util-table">

                                <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Shared Hours</th>
                                    <th>Shared %</th>
                                    <th>Exclusive Hours</th>
                                    <th>Exclusive %</th>
                                </tr>
                                </thead>

                                <tbody>
                                {data.usageMode.map((item, index) => (
                                    <tr key={item.id ?? index}>

                                        <td>
                                            {item.name || "—"}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.sharedUsageHours
                                            )}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.sharedUsagePercentage
                                            )}%
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.exclusiveUsageHours
                                            )}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.exclusiveUsagePercentage
                                            )}%
                                        </td>

                                    </tr>
                                ))}
                                </tbody>

                            </table>

                        </div>
                    )}
                </Section>

                {/* ==================================
                    DEMAND AND BENCHMARK
                ================================== */}

                <div className="util-two-column">

                    <Section
                        title="Equipment Demand Analysis"
                        description="Booking demand level for each equipment."
                    >
                        {data.demand.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="util-table-wrapper">

                                <table className="util-table">

                                    <thead>
                                    <tr>
                                        <th>Equipment</th>
                                        <th>Bookings</th>
                                        <th>Demand Level</th>
                                    </tr>
                                    </thead>

                                    <tbody>
                                    {data.demand.map((item) => (
                                        <tr key={item.equipmentId}>

                                            <td>
                                                {item.equipmentName || "—"}
                                            </td>

                                            <td>
                                                {item.totalBookings ?? 0}
                                            </td>

                                            <td>
                                                    <span className="util-status">
                                                        {item.demandLevel || "—"}
                                                    </span>
                                            </td>

                                        </tr>
                                    ))}
                                    </tbody>

                                </table>

                            </div>
                        )}
                    </Section>

                    <Section
                        title="Historical Benchmark"
                        description="Compare current and previous utilization."
                    >
                        {data.benchmark.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="util-table-wrapper">

                                <table className="util-table">

                                    <thead>
                                    <tr>
                                        <th>Equipment</th>
                                        <th>Current</th>
                                        <th>Previous</th>
                                        <th>Change</th>
                                        <th>Trend</th>
                                    </tr>
                                    </thead>

                                    <tbody>
                                    {data.benchmark.map((item) => (
                                        <tr key={item.equipmentId}>

                                            <td>
                                                {item.equipmentName || "—"}
                                            </td>

                                            <td>
                                                {formatNumber(
                                                    item.currentUtilization
                                                )}%
                                            </td>

                                            <td>
                                                {formatNumber(
                                                    item.previousUtilization
                                                )}%
                                            </td>

                                            <td>
                                                {formatNumber(
                                                    item.utilizationChange
                                                )}%
                                            </td>

                                            <td>
                                                {item.performanceTrend || "—"}
                                            </td>

                                        </tr>
                                    ))}
                                    </tbody>

                                </table>

                            </div>
                        )}
                    </Section>

                </div>

                {/* ==================================
                    IDLE TIME
                ================================== */}

                <Section
                    title="Equipment Idle Time"
                    description="Equipment with idle time and last booking information."
                >
                    {data.idle.length === 0 ? (
                        <EmptyState />
                    ) : (
                        <div className="util-table-wrapper">

                            <table className="util-table">

                                <thead>
                                <tr>
                                    <th>Equipment</th>
                                    <th>Asset Tag</th>
                                    <th>Department</th>
                                    <th>Idle Hours</th>
                                    <th>Idle %</th>
                                    <th>Last Booking Ended</th>
                                </tr>
                                </thead>

                                <tbody>
                                {data.idle.map((item) => (
                                    <tr key={item.equipmentId}>

                                        <td>
                                            {item.equipmentName || "—"}
                                        </td>

                                        <td>
                                            {item.assetTag || "—"}
                                        </td>

                                        <td>
                                            {item.departmentName || "—"}
                                        </td>

                                        <td>
                                            {formatNumber(item.idleHours)}
                                        </td>

                                        <td>
                                            {formatNumber(item.idlePercentage)}%
                                        </td>

                                        <td>
                                            {item.lastBookingEndedAt
                                                ? new Date(
                                                    item.lastBookingEndedAt
                                                ).toLocaleString()
                                                : "No previous booking"}
                                        </td>

                                    </tr>
                                ))}
                                </tbody>

                            </table>

                        </div>
                    )}
                </Section>

                {/* ==================================
                    HOURLY AND DAILY PEAK USAGE
                ================================== */}

                <div className="util-two-column">

                    <Section
                        title="Hourly Peak Usage"
                        description="Usage minutes grouped by hour."
                    >
                        {data.hourlyPeak.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="util-bar-list">

                                {data.hourlyPeak.map((item, index) => {

                                    const maximum = Math.max(
                                        ...data.hourlyPeak.map((row) =>
                                            Number(row?.usageMinutes || 0)
                                        ),
                                        1
                                    );

                                    return (
                                        <div
                                            className="util-bar-item"
                                            key={`${item.period}-${index}`}
                                        >

                                            <div className="util-list-top">
                                                <strong>
                                                    {item.period || "—"}
                                                </strong>

                                                <span>
                                                    {formatNumber(
                                                        item.usageMinutes
                                                    )} min
                                                </span>
                                            </div>

                                            <div className="util-chart-track">
                                                <div
                                                    className="util-chart-fill"
                                                    style={{
                                                        width: `${
                                                            (Number(
                                                                item.usageMinutes || 0
                                                            ) / maximum) * 100
                                                        }%`,
                                                    }}
                                                />
                                            </div>

                                            {item.peakPeriod && (
                                                <small className="util-peak-label">
                                                    Peak period
                                                </small>
                                            )}

                                        </div>
                                    );
                                })}

                            </div>
                        )}
                    </Section>

                    <Section
                        title="Daily Peak Usage"
                        description="Usage minutes grouped by day."
                    >
                        {data.dailyPeak.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="util-bar-list">

                                {data.dailyPeak.map((item, index) => {

                                    const maximum = Math.max(
                                        ...data.dailyPeak.map((row) =>
                                            Number(row?.usageMinutes || 0)
                                        ),
                                        1
                                    );

                                    return (
                                        <div
                                            className="util-bar-item"
                                            key={`${item.period}-${index}`}
                                        >

                                            <div className="util-list-top">
                                                <strong>
                                                    {item.period || "—"}
                                                </strong>

                                                <span>
                                                    {formatNumber(
                                                        item.usageMinutes
                                                    )} min
                                                </span>
                                            </div>

                                            <div className="util-chart-track">
                                                <div
                                                    className="util-chart-fill"
                                                    style={{
                                                        width: `${
                                                            (Number(
                                                                item.usageMinutes || 0
                                                            ) / maximum) * 100
                                                        }%`,
                                                    }}
                                                />
                                            </div>

                                            {item.peakPeriod && (
                                                <small className="util-peak-label">
                                                    Peak period
                                                </small>
                                            )}

                                        </div>
                                    );
                                })}

                            </div>
                        )}
                    </Section>

                </div>

                {/* ==================================
                    TOP FIVE BUSIEST SLOTS
                ================================== */}

                <Section
                    title="Top Five Busiest Day-Hour Slots"
                    description="The five day and hour combinations with the highest utilization."
                >
                    {data.topFive.length === 0 ? (
                        <EmptyState />
                    ) : (
                        <div className="util-table-wrapper">

                            <table className="util-table">

                                <thead>
                                <tr>
                                    <th>Day</th>
                                    <th>Hour</th>
                                    <th>Usage Minutes</th>
                                    <th>Utilization</th>
                                </tr>
                                </thead>

                                <tbody>
                                {data.topFive.map((item, index) => (
                                    <tr
                                        key={`${item.day}-${item.hour}-${index}`}
                                    >

                                        <td>
                                            {item.day || "—"}
                                        </td>

                                        <td>
                                            {item.hour || "—"}
                                        </td>

                                        <td>
                                            {formatNumber(
                                                item.usageMinutes
                                            )}
                                        </td>

                                        <td>
                                            <PercentageBar
                                                value={
                                                    item.utilizationPercentage
                                                }
                                            />
                                        </td>

                                    </tr>
                                ))}
                                </tbody>

                            </table>

                        </div>
                    )}
                </Section>

                {/* ==================================
                    EQUIPMENT HEATMAP
                ================================== */}

                <Section
                    title="Equipment Usage Heatmap"
                    description="Hourly availability, usage and utilization for each equipment."
                >
                    {data.heatmap.length === 0 ? (
                        <EmptyState />
                    ) : (
                        <div className="util-heatmap-list">

                            {data.heatmap.map((equipment) => (
                                <div
                                    className="util-heatmap-card"
                                    key={equipment.equipmentId}
                                >

                                    <h3>
                                        {equipment.equipmentName ||
                                            "Unnamed Equipment"}
                                    </h3>

                                    {Object.entries(
                                        equipment.hourlyUsage || {}
                                    ).map(([day, hours]) => {

                                        if (
                                            !hours ||
                                            typeof hours !== "object"
                                        ) {
                                            return null;
                                        }

                                        return (
                                            <div
                                                className="util-heatmap-day"
                                                key={day}
                                            >

                                                <strong>{day}</strong>

                                                <div className="util-heatmap-hours">

                                                    {Object.entries(hours).map(
                                                        ([hour, slot]) => {

                                                            if (
                                                                !slot ||
                                                                typeof slot !== "object"
                                                            ) {
                                                                return null;
                                                            }

                                                            const percentage =
                                                                getPercentage(
                                                                    slot.utilizationPercentage
                                                                );

                                                            return (
                                                                <div
                                                                    className="util-heatmap-cell"
                                                                    key={hour}
                                                                    title={`${day} ${hour}: ${
                                                                        slot.usedMinutes ?? 0
                                                                    } used minutes / ${
                                                                        slot.availableMinutes ?? 0
                                                                    } available minutes`}
                                                                    style={{
                                                                        backgroundColor:
                                                                            percentage >= 75
                                                                                ? "#16865d"
                                                                                : percentage >= 40
                                                                                    ? "#75b99a"
                                                                                    : percentage > 0
                                                                                        ? "#c9e8d8"
                                                                                        : "#edf1ef",
                                                                    }}
                                                                >

                                                                    <span>
                                                                        {hour}
                                                                    </span>

                                                                    <small>
                                                                        {formatNumber(
                                                                            percentage,
                                                                            0
                                                                        )}%
                                                                    </small>

                                                                </div>
                                                            );
                                                        }
                                                    )}

                                                </div>

                                            </div>
                                        );
                                    })}

                                </div>
                            ))}

                        </div>
                    )}
                </Section>

                {/* ==================================
                    FOOTER
                ================================== */}

                <footer className="util-footer">
                    Data shown for {startDate} to {endDate}
                </footer>

            </div>
        </DashboardLayout>
    );
}

export default UtilizationAnalytics;
