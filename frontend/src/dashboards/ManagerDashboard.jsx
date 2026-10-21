
import { useNavigate } from "react-router-dom";
import {
    Microscope,
    CircleCheck,
    CalendarCheck,
    Wrench,
    Package,
    CalendarDays,
    ChartNoAxesCombined,
    FileChartColumn,
    ChevronRight,
} from "lucide-react";

import DashboardLayout from "./DashboardLayout";
import "./ManagerDashboard.css";

const statistics = [
    {
        title: "Total Equipment",
        value: "—",
        icon: Microscope,
        color: "blue",
    },
    {
        title: "Available",
        value: "—",
        icon: CircleCheck,
        color: "green",
    },
    {
        title: "Currently Booked",
        value: "—",
        icon: CalendarCheck,
        color: "purple",
    },
    {
        title: "Under Maintenance",
        value: "—",
        icon: Wrench,
        color: "orange",
    },
];

const sections = [
    {
        title: "Equipment Inventory",
        description: "View and manage laboratory equipment records.",
        icon: Package,
        path: "/equipment",
    },
    {
        title: "Bookings",
        description: "View reservations and manage permitted booking tasks.",
        icon: CalendarDays,
        path: "/bookings",
    },
    {
        title: "Maintenance",
        description: "View equipment service schedules and reported issues.",
        icon: Wrench,
        path: "/equipment",
    },
    {
        title: "Utilization & Reports",
        description: "Review equipment usage and laboratory reports.",
        icon: ChartNoAxesCombined,
        path: "/reports",
    },
];

export default function ManagerDashboard() {
    const navigate = useNavigate();

    return (
        <div className="manager-dashboard">
            <DashboardLayout title="Lab Manager Dashboard">
                <main className="manager-page">
                    <section className="manager-welcome">
                        <div className="manager-welcome-content">
                            <span className="manager-eyebrow">
                                LABORATORY OPERATIONS
                            </span>

                            <h2>Laboratory Overview</h2>

                            <p>
                                Monitor equipment availability, track
                                laboratory resources, and coordinate
                                daily operations.
                            </p>
                        </div>

                        <div className="manager-welcome-icon">
                            <Microscope size={38} strokeWidth={1.7} />
                        </div>
                    </section>

                    <section className="manager-statistics">
                        <div className="manager-section-heading">
                            <div>
                                <h2>Equipment Overview</h2>
                                <p>
                                    Current status of laboratory resources
                                </p>
                            </div>
                        </div>

                        <div className="manager-stats-grid">
                            {statistics.map((stat) => {
                                const Icon = stat.icon;

                                return (
                                    <article
                                        className="manager-stat-card"
                                        key={stat.title}
                                    >
                                        <div
                                            className={`manager-stat-icon ${stat.color}`}
                                        >
                                            <Icon
                                                size={23}
                                                strokeWidth={1.8}
                                            />
                                        </div>

                                        <div className="manager-stat-info">
                                            <p>{stat.title}</p>
                                            <h3>{stat.value}</h3>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    </section>

                    <section className="manager-main-sections">
                        <div className="manager-section-heading">
                            <div>
                                <h2>Main Sections</h2>
                                <p>
                                    Access your laboratory management tools
                                </p>
                            </div>
                        </div>

                        <div className="manager-sections-grid">
                            {sections.map((section) => {
                                const Icon = section.icon;

                                return (
                                    <button
                                        type="button"
                                        className="manager-section-card"
                                        key={section.title}
                                        onClick={() =>
                                            navigate(section.path)
                                        }
                                    >
                                        <div className="manager-section-icon">
                                            <Icon
                                                size={24}
                                                strokeWidth={1.8}
                                            />
                                        </div>

                                        <div className="manager-section-content">
                                            <h3>{section.title}</h3>
                                            <p>{section.description}</p>
                                        </div>

                                        <ChevronRight
                                            className="manager-section-arrow"
                                            size={20}
                                        />
                                    </button>
                                );
                            })}
                        </div>
                    </section>

                    <footer className="manager-footer">
                        <FileChartColumn size={16} />
                        <span>
                            Laboratory Resource Management Platform
                        </span>
                    </footer>
                </main>
            </DashboardLayout>
        </div>
    );
}
