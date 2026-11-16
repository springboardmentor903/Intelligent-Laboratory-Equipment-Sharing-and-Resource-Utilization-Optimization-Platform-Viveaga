
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
    CalendarCheck,
    Clock,
    Microscope,
    CheckCircle,
    Search,
    CalendarPlus,
    ClipboardList,
    History,
    ChevronRight,
    GraduationCap,
    Activity,
} from "lucide-react";

import DashboardLayout from "./DashboardLayout";
import api from "../api";
import "./StudentDashboard.css";

const quickActions = [
    {
        title: "Browse Equipment",
        description: "Explore equipment and check availability.",
        icon: Search,
        path: "/equipment",
    },
    {
        title: "Book Equipment",
        description: "Request equipment for your laboratory work.",
        icon: CalendarPlus,
        path: "/bookings",
    },
    {
        title: "My Bookings",
        description: "Review your reservations and their status.",
        icon: ClipboardList,
        path: "/bookings",
    },
    {
        title: "Booking History",
        description: "View your previous booking records.",
        icon: History,
        path: "/bookings",
    },
];

export default function StudentDashboard() {
    const navigate = useNavigate();

    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        fetchMyBookings();
    }, []);

    const fetchMyBookings = async () => {
        try {
            setLoading(true);
            setError("");

            const response = await api.get("/bookings/my");

            setBookings(
                Array.isArray(response.data) ? response.data : []
            );
        } catch (err) {
            console.error("Failed to fetch bookings:", err);

            setError(
                err.response?.data?.message ||
                "Unable to load your booking information."
            );
        } finally {
            setLoading(false);
        }
    };

    const pendingBookings = bookings.filter(
        (booking) => booking.status === "PENDING"
    );

    const approvedBookings = bookings.filter(
        (booking) => booking.status === "APPROVED"
    );

    const statistics = [
        {
            title: "My Bookings",
            value: loading ? "..." : bookings.length,
            description: "Your total reservations",
            icon: CalendarCheck,
            color: "blue",
        },
        {
            title: "Awaiting Approval",
            value: loading ? "..." : pendingBookings.length,
            description: "Requests under review",
            icon: Clock,
            color: "orange",
        },
        {
            title: "Available Equipment",
            value: "—",
            description: "Resources ready to use",
            icon: Microscope,
            color: "green",
        },
        {
            title: "Approved Bookings",
            value: loading ? "..." : approvedBookings.length,
            description: "Reservations approved",
            icon: CheckCircle,
            color: "purple",
        },
    ];

    const recentBookings = [...bookings]
        .sort((a, b) => {
            return new Date(b.createdAt || b.startTime) -
                new Date(a.createdAt || a.startTime);
        })
        .slice(0, 5);

    const formatDate = (dateTime) => {
        if (!dateTime) return "Date unavailable";

        return new Date(dateTime).toLocaleString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    };

    const getStatusClass = (status) => {
        switch (status) {
            case "APPROVED":
                return "approved";
            case "PENDING":
                return "pending";
            case "REJECTED":
                return "rejected";
            case "CANCELLED":
                return "cancelled";
            case "COMPLETED":
                return "completed";
            default:
                return "";
        }
    };

    return (
        <div className="student-dashboard">
            <DashboardLayout title="Student Dashboard">
                <main className="student-dashboard-page">

                    <section className="student-welcome">
                        <div className="student-welcome-content">
                            <span className="student-eyebrow">
                                STUDENT LAB PORTAL
                            </span>

                            <h2>Welcome to Your Lab Portal!</h2>

                            <p>
                                Discover laboratory equipment, reserve
                                resources, and keep track of your bookings
                                in one place.
                            </p>
                        </div>

                        <div className="student-welcome-icon">
                            <GraduationCap
                                size={38}
                                strokeWidth={1.7}
                            />
                        </div>
                    </section>

                    <section className="student-statistics">
                        <div className="student-section-heading">
                            <div>
                                <h2>Your Overview</h2>
                                <p>
                                    A quick look at your bookings and
                                    laboratory resources
                                </p>
                            </div>
                        </div>

                        <div className="student-stats-grid">
                            {statistics.map((stat) => {
                                const Icon = stat.icon;

                                return (
                                    <article
                                        className="student-stat-card"
                                        key={stat.title}
                                    >
                                        <div
                                            className={`student-stat-icon ${stat.color}`}
                                        >
                                            <Icon
                                                size={23}
                                                strokeWidth={1.8}
                                            />
                                        </div>

                                        <div className="student-stat-info">
                                            <p>{stat.title}</p>
                                            <h3>{stat.value}</h3>
                                            <span>{stat.description}</span>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    </section>

                    <section className="student-quick-actions">
                        <div className="student-section-heading">
                            <div>
                                <h2>Quick Actions</h2>
                                <p>
                                    Get to the features you use most
                                </p>
                            </div>
                        </div>

                        <div className="student-actions-grid">
                            {quickActions.map((action) => {
                                const Icon = action.icon;

                                return (
                                    <button
                                        type="button"
                                        className="student-action-card"
                                        key={action.title}
                                        onClick={() =>
                                            navigate(action.path)
                                        }
                                    >
                                        <div className="student-action-icon">
                                            <Icon
                                                size={23}
                                                strokeWidth={1.8}
                                            />
                                        </div>

                                        <div className="student-action-content">
                                            <h3>{action.title}</h3>
                                            <p>{action.description}</p>
                                        </div>

                                        <ChevronRight
                                            size={20}
                                            className="student-action-arrow"
                                        />
                                    </button>
                                );
                            })}
                        </div>
                    </section>

                    <section className="student-activity">
                        <div className="student-activity-header">
                            <div className="student-activity-icon">
                                <Activity size={22} />
                            </div>

                            <div>
                                <h2>Recent Activity</h2>
                                <p>
                                    Updates about your equipment bookings
                                </p>
                            </div>
                        </div>

                        {loading ? (
                            <div className="student-activity-empty">
                                <p>Loading your booking activity...</p>
                            </div>
                        ) : error ? (
                            <div className="student-activity-empty">
                                <h3>Unable to load activity</h3>
                                <p>{error}</p>

                                <button
                                    type="button"
                                    onClick={fetchMyBookings}
                                >
                                    Try Again
                                </button>
                            </div>
                        ) : bookings.length === 0 ? (
                            <div className="student-activity-empty">
                                <div className="student-activity-empty-icon">
                                    <ClipboardList size={28} />
                                </div>

                                <h3>No bookings yet</h3>

                                <p>
                                    You haven't made any equipment
                                    booking requests yet. Browse the
                                    available laboratory equipment
                                    to get started.
                                </p>

                                <button
                                    type="button"
                                    onClick={() => navigate("/equipment")}
                                >
                                    Browse Equipment
                                    <ChevronRight size={17} />
                                </button>
                            </div>
                        ) : (
                            <div className="student-booking-activity">
                                {recentBookings.map((booking) => (
                                    <article
                                        className="student-booking-activity-item"
                                        key={booking.id}
                                    >
                                        <div>
                                            <h3>
                                                {booking.equipmentName ||
                                                    "Equipment"}
                                            </h3>

                                            <p>
                                                {booking.assetTag || ""}
                                            </p>

                                            <p>
                                                {booking.purpose ||
                                                    "No purpose specified"}
                                            </p>

                                            <span>
                                                {formatDate(
                                                    booking.createdAt ||
                                                    booking.startTime
                                                )}
                                            </span>
                                        </div>

                                        <span
                                            className={`student-booking-status ${getStatusClass(
                                                booking.status
                                            )}`}
                                        >
                                            {booking.status || "UNKNOWN"}
                                        </span>
                                    </article>
                                ))}
                            </div>
                        )}
                    </section>

                    <footer className="student-dashboard-footer">
                        Laboratory Resource Management Platform
                    </footer>

                </main>
            </DashboardLayout>
        </div>
    );
}
