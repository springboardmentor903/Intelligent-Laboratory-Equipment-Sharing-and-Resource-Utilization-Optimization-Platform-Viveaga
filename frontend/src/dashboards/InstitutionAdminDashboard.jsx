
import { useNavigate } from "react-router-dom";
import {
    Building2,
    Users,
    Microscope,
    FlaskConical,
    Network,
    UserCog,
    Package,
    ChartNoAxesCombined,
    ChevronRight,
} from "lucide-react";

import DashboardLayout from "./DashboardLayout";
import "./InstitutionAdminDashboard.css";

const statistics = [
    {
        title: "Total Departments",
        value: "—",
        icon: Network,
        color: "blue",
    },
    {
        title: "Institution Users",
        value: "—",
        icon: Users,
        color: "purple",
    },
    {
        title: "Total Equipment",
        value: "—",
        icon: Microscope,
        color: "green",
    },
    {
        title: "Total Laboratories",
        value: "—",
        icon: FlaskConical,
        color: "orange",
    },
];

const managementSections = [
    {
        title: "Manage Departments",
        description:
            "Create, update, and organize institution departments.",
        icon: Building2,
        path: "/departments",
    },
    {
        title: "Manage Institutional Users",
        description:
            "View users and manage authorized department assignments.",
        icon: UserCog,
        path: "/users",
    },
    {
        title: "Manage Laboratories",
        description:
            "Organize laboratories and manage laboratory assignments.",
        icon: FlaskConical,
        path: "/laboratories",
    },
    {
        title: "Equipment Inventory",
        description:
            "View equipment records and monitor resource availability.",
        icon: Package,
        path: "/equipment",
    },
    {
        title: "Resource Reports",
        description:
            "Review equipment utilization and institutional resources.",
        icon: ChartNoAxesCombined,
        path: "/reports",
    },
];

export default function InstitutionAdminDashboard() {
    const navigate = useNavigate();

    return (
        <div className="institution-admin-dashboard">
            <DashboardLayout title="Institution Administrator Dashboard">
                <main className="institution-admin-page">
                    <section className="institution-welcome">
                        <div className="institution-welcome-content">
                            <span className="institution-eyebrow">
                                INSTITUTIONAL OPERATIONS
                            </span>

                            <h2>Institution Overview</h2>

                            <p>
                                Manage departments, laboratories, institutional
                                users, and resource allocation from one place.
                            </p>
                        </div>

                        <div className="institution-welcome-icon">
                            <Building2 size={38} strokeWidth={1.7} />
                        </div>
                    </section>

                    <section className="institution-statistics">
                        <div className="institution-section-heading">
                            <div>
                                <h2>Institution at a Glance</h2>
                                <p>
                                    An overview of your institution's resources
                                </p>
                            </div>
                        </div>

                        <div className="institution-stats-grid">
                            {statistics.map((stat) => {
                                const Icon = stat.icon;

                                return (
                                    <article
                                        className="institution-stat-card"
                                        key={stat.title}
                                    >
                                        <div
                                            className={`institution-stat-icon ${stat.color}`}
                                        >
                                            <Icon
                                                size={23}
                                                strokeWidth={1.8}
                                            />
                                        </div>

                                        <div className="institution-stat-info">
                                            <p>{stat.title}</p>
                                            <h3>{stat.value}</h3>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    </section>

                    <section className="institution-management">
                        <div className="institution-section-heading">
                            <div>
                                <h2>Institution Management</h2>
                                <p>
                                    Manage institutional resources and
                                    administrative tasks
                                </p>
                            </div>
                        </div>

                        <div className="institution-management-grid">
                            {managementSections.map((section) => {
                                const Icon = section.icon;

                                return (
                                    <button
                                        type="button"
                                        className="institution-management-card"
                                        key={section.title}
                                        onClick={() => navigate(section.path)}
                                    >
                                        <div className="institution-management-icon">
                                            <Icon
                                                size={24}
                                                strokeWidth={1.8}
                                            />
                                        </div>

                                        <div className="institution-management-content">
                                            <h3>{section.title}</h3>
                                            <p>{section.description}</p>
                                        </div>

                                        <ChevronRight
                                            size={20}
                                            className="institution-management-arrow"
                                        />
                                    </button>
                                );
                            })}
                        </div>
                    </section>

                    <footer className="institution-footer">
                        <Building2 size={16} />
                        <span>
                            Laboratory Resource Management Platform
                        </span>
                    </footer>
                </main>
            </DashboardLayout>
        </div>
    );
}
