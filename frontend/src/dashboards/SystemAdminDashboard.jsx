
import { useCallback, useEffect, useState } from "react";
import {
    Users,
    ShieldCheck,
    Activity,
    Clock3,
    UserCog,
    Settings,
    MonitorCheck,
    ClipboardList,
    ArrowUpRight,
    Server,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import api from "../api";
import DashboardLayout from "./DashboardLayout";
import "./SystemAdminDashboard.css";

const adminActions = [
    {
        title: "Manage Users",
        description: "Review user accounts and manage platform access.",
        icon: UserCog,
    },
    {
        title: "Manage Roles",
        description: "Review the six roles used across the platform.",
        icon: ShieldCheck,
    },
    {
        title: "Monitor Activity",
        description: "Monitor platform activity and operational status.",
        icon: MonitorCheck,
    },
    {
        title: "View Audit Logs",
        description: "Review system events and administration records.",
        icon: ClipboardList,
    },
];

export default function SystemAdminDashboard() {
    const navigate = useNavigate();

    const [requests, setRequests] = useState([]);
    const [loadingRequests, setLoadingRequests] = useState(true);
    const [requestError, setRequestError] = useState("");

    const fetchPendingRequests = useCallback(async () => {
        try {
            setLoadingRequests(true);
            setRequestError("");

            const response = await api.get("/role-requests/pending");

            setRequests(
                Array.isArray(response.data) ? response.data : []
            );
        } catch (error) {
            setRequestError(
                error.response?.data?.message ||
                "Unable to load pending role requests."
            );
        } finally {
            setLoadingRequests(false);
        }
    }, []);

    useEffect(() => {
        fetchPendingRequests();
    }, [fetchPendingRequests]);

    const statistics = [
        {
            title: "Total Users",
            value: "—",
            description: "Registered platform users",
            icon: Users,
            color: "blue",
        },
        {
            title: "System Roles",
            value: "6",
            description: "Defined user roles",
            icon: ShieldCheck,
            color: "purple",
        },
        {
            title: "Active Sessions",
            value: "—",
            description: "Current user sessions",
            icon: Activity,
            color: "green",
        },
        {
            title: "Pending Requests",
            value: loadingRequests ? "…" : String(requests.length),
            description: "Requests awaiting review",
            icon: Clock3,
            color: "orange",
        },
    ];

    const openRoleRequests = () => {
        navigate("/role-requests");
    };

    return (
        <div className="system-admin-dashboard">
            <DashboardLayout title="System Administrator">
                <div className="sysadmin-page">
                    <section className="sysadmin-hero">
                        <div className="sysadmin-hero-content">
                            <span className="sysadmin-eyebrow">
                                <ShieldCheck size={15} />
                                ADMINISTRATION OVERVIEW
                            </span>

                            <h2>System Control Center</h2>

                            <p>
                                Manage platform access, oversee system
                                operations, and maintain visibility across your
                                laboratory resource management platform.
                            </p>
                        </div>

                        <div className="sysadmin-hero-icon">
                            <Server size={43} strokeWidth={1.5} />
                            <span className="sysadmin-status-dot" />
                        </div>
                    </section>

                    <section className="sysadmin-overview">
                        <div className="sysadmin-section-heading">
                            <div>
                                <h2>Platform Overview</h2>
                                <p>Key system statistics at a glance</p>
                            </div>

                            <span className="sysadmin-live-label">
                                <span />
                                Overview
                            </span>
                        </div>

                        <div className="sysadmin-stats-grid">
                            {statistics.map((stat) => {
                                const Icon = stat.icon;
                                const isPendingCard =
                                    stat.title === "Pending Requests";

                                return (
                                    <article
                                        className={`sysadmin-stat-card ${
                                            isPendingCard
                                                ? "sysadmin-clickable-card"
                                                : ""
                                        }`}
                                        key={stat.title}
                                        onClick={
                                            isPendingCard
                                                ? openRoleRequests
                                                : undefined
                                        }
                                        onKeyDown={
                                            isPendingCard
                                                ? (event) => {
                                                    if (
                                                        event.key ===
                                                        "Enter" ||
                                                        event.key === " "
                                                    ) {
                                                        event.preventDefault();
                                                        openRoleRequests();
                                                    }
                                                }
                                                : undefined
                                        }
                                        role={
                                            isPendingCard
                                                ? "button"
                                                : undefined
                                        }
                                        tabIndex={
                                            isPendingCard ? 0 : undefined
                                        }
                                        aria-label={
                                            isPendingCard
                                                ? `View ${requests.length} pending role requests`
                                                : undefined
                                        }
                                    >
                                        <div className="sysadmin-stat-top">
                                            <div
                                                className={`sysadmin-stat-icon ${stat.color}`}
                                            >
                                                <Icon size={22} />
                                            </div>

                                            <ArrowUpRight
                                                className="sysadmin-stat-arrow"
                                                size={18}
                                            />
                                        </div>

                                        <p className="sysadmin-stat-title">
                                            {stat.title}
                                        </p>

                                        <h3>{stat.value}</h3>

                                        <p className="sysadmin-stat-description">
                                            {stat.description}
                                        </p>
                                    </article>
                                );
                            })}
                        </div>

                        {requestError && (
                            <div
                                className="sysadmin-request-error"
                                role="alert"
                            >
                                <span>{requestError}</span>

                                <button
                                    type="button"
                                    onClick={fetchPendingRequests}
                                    disabled={loadingRequests}
                                >
                                    Try again
                                </button>
                            </div>
                        )}
                    </section>

                    <section className="sysadmin-administration">
                        <div className="sysadmin-section-heading">
                            <div>
                                <h2>System Administration</h2>
                                <p>
                                    Quick access to administrative functions
                                </p>
                            </div>
                        </div>

                        <div className="sysadmin-actions-grid">
                            {adminActions.map((action) => {
                                const Icon = action.icon;

                                return (
                                    <article
                                        className="sysadmin-action-card"
                                        key={action.title}
                                    >
                                        <div className="sysadmin-action-icon">
                                            <Icon size={23} />
                                        </div>

                                        <div className="sysadmin-action-content">
                                            <h3>{action.title}</h3>
                                            <p>{action.description}</p>
                                        </div>

                                        <ArrowUpRight
                                            className="sysadmin-action-arrow"
                                            size={19}
                                        />
                                    </article>
                                );
                            })}
                        </div>
                    </section>

                    <section className="sysadmin-activity">
                        <div className="sysadmin-activity-header">
                            <div className="sysadmin-activity-icon">
                                <Activity size={22} />
                            </div>

                            <div>
                                <h2>System Activity</h2>
                                <p>
                                    System events and administrative records
                                </p>
                            </div>
                        </div>

                        <div className="sysadmin-activity-empty">
                            <div className="sysadmin-empty-icon">
                                <ClipboardList size={27} />
                            </div>

                            <h3>No activity to display yet</h3>

                            <p>
                                User activity, access events, and audit records
                                will appear here when connected to the backend.
                            </p>
                        </div>
                    </section>

                    <footer className="sysadmin-footer">
                        <Settings size={15} />
                        <span>
                            Laboratory Resource Management Platform
                        </span>
                        <span className="sysadmin-footer-separator">•</span>
                        <span>System Administration</span>
                    </footer>
                </div>
            </DashboardLayout>
        </div>
    );
}
