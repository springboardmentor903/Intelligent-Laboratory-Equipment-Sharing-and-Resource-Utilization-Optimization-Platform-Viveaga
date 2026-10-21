import { useNavigate } from "react-router-dom";
import {
    FlaskConical,
    ChartNoAxesCombined,
    Wrench,
    FileChartColumn,
    ArrowRight,
} from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import "./DepartmentHeadDashboard.css";

export default function DepartmentHeadDashboard() {
    const navigate = useNavigate();

    const stats = [
        {
            title: "Total Laboratories",
            value: "—",
            description: "Laboratories in your department",
            icon: FlaskConical,
            color: "teal",
        },
        {
            title: "Equipment Utilization",
            value: "—",
            description: "Current resource usage",
            icon: ChartNoAxesCombined,
            color: "blue",
        },
        {
            title: "Available Equipment",
            value: "—",
            description: "Resources ready to use",
            icon: Wrench,
            color: "green",
        },
        {
            title: "Maintenance Issues",
            value: "—",
            description: "Equipment requiring attention",
            icon: Wrench,
            color: "orange",
        },
    ];

    const actions = [
        {
            title: "Monitor Laboratories",
            description:
                "View laboratory details and equipment availability.",
            icon: FlaskConical,
            path: "/equipment",
        },
        {
            title: "Resource Utilization",
            description:
                "Review equipment usage and identify underutilized resources.",
            icon: ChartNoAxesCombined,
            path: "/reports",
        },
        {
            title: "Maintenance Overview",
            description:
                "Review reported equipment issues and maintenance status.",
            icon: Wrench,
            path: "/equipment",
        },
        {
            title: "Department Reports",
            description:
                "Review resource usage and department performance reports.",
            icon: FileChartColumn,
            path: "/reports",
        },
    ];

    return (
        <div className="department-head-dashboard">
            <DashboardLayout title="Department Head Dashboard">
                <div className="department-head-page">
                    <section className="dh-welcome">
                        <div>
                            <span className="dh-eyebrow">
                                DEPARTMENT OVERVIEW
                            </span>
                            <h2>Department Resource Overview</h2>
                            <p>
                                Monitor laboratory performance, equipment
                                availability, and resource utilization
                                across your department.
                            </p>
                        </div>

                        <div className="dh-welcome-icon">
                            <FlaskConical size={42} />
                        </div>
                    </section>

                    <section className="dh-overview">
                        <div className="dh-section-heading">
                            <div>
                                <h2>Department at a Glance</h2>
                                <p>
                                    An overview of your department's
                                    laboratory resources.
                                </p>
                            </div>
                        </div>

                        <div className="dh-stats-grid">
                            {stats.map((stat) => {
                                const Icon = stat.icon;

                                return (
                                    <article
                                        className="dh-stat-card"
                                        key={stat.title}
                                    >
                                        <div
                                            className={`dh-stat-icon ${stat.color}`}
                                        >
                                            <Icon size={22} />
                                        </div>

                                        <p className="dh-stat-title">
                                            {stat.title}
                                        </p>

                                        <h3>{stat.value}</h3>

                                        <p className="dh-stat-description">
                                            {stat.description}
                                        </p>
                                    </article>
                                );
                            })}
                        </div>
                    </section>

                    <section className="dh-actions-section">
                        <div className="dh-section-heading">
                            <div>
                                <h2>Department Management</h2>
                                <p>
                                    Access tools for monitoring and
                                    managing department resources.
                                </p>
                            </div>
                        </div>

                        <div className="dh-actions-grid">
                            {actions.map((action) => {
                                const Icon = action.icon;

                                return (
                                    <button
                                        type="button"
                                        className="dh-action-card"
                                        key={action.title}
                                        onClick={() =>
                                            navigate(action.path)
                                        }
                                    >
                                        <span className="dh-action-icon">
                                            <Icon size={23} />
                                        </span>

                                        <span className="dh-action-content">
                                            <strong>{action.title}</strong>
                                            <span>
                                                {action.description}
                                            </span>
                                        </span>

                                        <ArrowRight
                                            className="dh-action-arrow"
                                            size={19}
                                        />
                                    </button>
                                );
                            })}
                        </div>
                    </section>

                    <section className="dh-performance">
                        <div className="dh-performance-icon">
                            <ChartNoAxesCombined size={23} />
                        </div>

                        <div>
                            <h2>Department Performance</h2>
                            <p>
                                Laboratory utilization, resource allocation,
                                and department performance summaries can
                                be displayed here when the corresponding
                                data is connected.
                            </p>
                        </div>
                    </section>

                    <footer className="dh-footer">
                        Laboratory Resource Management Platform
                    </footer>
                </div>
            </DashboardLayout>
        </div>
    );
}