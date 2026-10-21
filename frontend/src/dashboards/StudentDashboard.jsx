
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
import "./StudentDashboard.css";

const statistics = [
    {
        title: "My Bookings",
        value: "—",
        description: "Your total reservations",
        icon: CalendarCheck,
        color: "blue",
    },
    {
        title: "Awaiting Approval",
        value: "—",
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
        value: "—",
        description: "Reservations approved",
        icon: CheckCircle,
        color: "purple",
    },
];

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

                        <div className="student-activity-empty">
                            <div className="student-activity-empty-icon">
                                <ClipboardList size={28} />
                            </div>

                            <h3>Your activity will appear here</h3>

                            <p>
                                Your recent booking requests and their
                                approval status will appear here once
                                activity data is connected.
                            </p>

                            <button
                                type="button"
                                onClick={() => navigate("/bookings")}
                            >
                                View My Bookings
                                <ChevronRight size={17} />
                            </button>
                        </div>
                    </section>

                    <footer className="student-dashboard-footer">
                        Laboratory Resource Management Platform
                    </footer>
                </main>
            </DashboardLayout>
        </div>
    );
}
