
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
    LayoutDashboard,
    FlaskConical,
    CalendarCheck,
    ChartNoAxesCombined,
    LogOut,
    Menu,
    X,
    ShieldCheck,
    Activity,
    ListChecks,
} from "lucide-react";
import "./DashboardLayout.css";

// ==========================================
// ROLE DASHBOARD ROUTES
// ==========================================

const dashboardRoutes = {
    RESEARCHER_STUDENT: "/student-dashboard",
    LAB_TECHNICIAN: "/technician-dashboard",
    LAB_MANAGER: "/manager-dashboard",
    DEPARTMENT_HEAD: "/department-head-dashboard",
    INSTITUTION_ADMINISTRATOR: "/institution-admin-dashboard",
    SYSTEM_ADMINISTRATOR: "/system-admin-dashboard",
};

// ==========================================
// NAVIGATION ITEMS
// ==========================================

const navItems = [
    {
        label: "Dashboard",
        pathType: "dashboard",
        icon: LayoutDashboard,
    },
    {
        label: "Equipment",
        path: "/equipment",
        icon: FlaskConical,
    },
    {
        label: "Live Monitoring",
        path: "/live-equipment",
        icon: Activity,
    },
    {
        label: "Bookings",
        path: "/bookings",
        icon: CalendarCheck,
    },
    {
        label: "My Waitlist",
        path: "/waitlist",
        icon: ListChecks,
        allowedRoles: [
            "RESEARCHER_STUDENT",
            "DEPARTMENT_HEAD",
        ],
    },
    {
        label: "Reports",
        path: "/reports",
        icon: ChartNoAxesCombined,
    },
    {
        label: "Utilization Analytics",
        path: "/utilization-analytics",
        icon: ChartNoAxesCombined,
        allowedRoles: [
            "SYSTEM_ADMINISTRATOR",
            "INSTITUTION_ADMINISTRATOR",
            "LAB_MANAGER",
            "DEPARTMENT_HEAD",
        ],
    },
];

// ==========================================
// DASHBOARD LAYOUT
// ==========================================

export default function DashboardLayout({ title, children }) {
    const navigate = useNavigate();
    const location = useLocation();

    const [menuOpen, setMenuOpen] = useState(false);

    const role = sessionStorage.getItem("userRole");

    const userName =
        sessionStorage.getItem("userName") || "User";

    const dashboardPath = dashboardRoutes[role] || "/";

    // ==========================================
    // FORMATTED ROLE
    // ==========================================

    const formattedRole = (role || "USER")
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, (letter) =>
            letter.toUpperCase()
        );

    // ==========================================
    // ROLE-BASED NAVIGATION
    // ==========================================

    const visibleNavItems = navItems.filter(
        (item) =>
            !item.allowedRoles ||
            item.allowedRoles.includes(role)
    );

    // ==========================================
    // NAVIGATION HANDLER
    // ==========================================

    const handleNavigation = (item) => {
        const destination =
            item.pathType === "dashboard"
                ? dashboardPath
                : item.path;

        navigate(destination);
        setMenuOpen(false);
    };

    // ==========================================
    // LOGOUT
    // ==========================================

    const handleLogout = () => {
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("userRole");
        sessionStorage.removeItem("userName");
        sessionStorage.removeItem("userId");
        sessionStorage.removeItem("email");

        setMenuOpen(false);

        navigate("/", { replace: true });
    };

    // ==========================================
    // UI
    // ==========================================

    return (
        <div className="dashboard-layout">

            {/* SIDEBAR OVERLAY */}

            {menuOpen && (
                <button
                    type="button"
                    className="sidebar-overlay overlay-visible"
                    aria-label="Close navigation menu"
                    onClick={() => setMenuOpen(false)}
                />
            )}

            {/* ==================================
                SIDEBAR
            ================================== */}

            <aside
                className={`dashboard-sidebar ${
                    menuOpen ? "sidebar-open" : ""
                }`}
            >

                {/* Sidebar Brand */}

                <div className="sidebar-brand">

                    <div className="brand-icon">
                        <ShieldCheck size={25} />
                    </div>

                    <div className="brand-text">
                        <h2>LabResource</h2>
                        <p>Resource Management</p>
                    </div>

                    <button
                        type="button"
                        className="mobile-close-button"
                        onClick={() => setMenuOpen(false)}
                        aria-label="Close menu"
                    >
                        <X size={21} />
                    </button>

                </div>

                {/* Navigation */}

                <div className="sidebar-section-label">
                    MAIN MENU
                </div>

                <nav className="dashboard-navigation">

                    {visibleNavItems.map((item) => {
                        const Icon = item.icon;

                        const destination =
                            item.pathType === "dashboard"
                                ? dashboardPath
                                : item.path;

                        const isActive =
                            location.pathname === destination;

                        return (
                            <button
                                key={item.label}
                                type="button"
                                className={`nav-item ${
                                    isActive ? "active" : ""
                                }`}
                                onClick={() =>
                                    handleNavigation(item)
                                }
                            >
                                <Icon size={19} />

                                <span>
                                    {item.label}
                                </span>
                            </button>
                        );
                    })}

                </nav>

                {/* ==================================
                    BOTTOM USER DETAILS AND LOGOUT
                ================================== */}

                <div className="sidebar-bottom">

                    <div className="sidebar-user">

                        <div className="user-avatar">
                            {userName.charAt(0).toUpperCase()}
                        </div>

                        <div className="user-info">
                            <h4>{userName}</h4>
                            <p>{formattedRole}</p>
                        </div>

                    </div>

                    <button
                        type="button"
                        className="logout-button"
                        onClick={handleLogout}
                    >
                        <LogOut size={18} />
                        <span>Logout</span>
                    </button>

                </div>

            </aside>

            {/* ==================================
                MAIN DASHBOARD
            ================================== */}

            <main className="dashboard-main">

                {/* TOPBAR */}

                <header className="dashboard-topbar">

                    <button
                        type="button"
                        className="mobile-menu-button"
                        onClick={() => setMenuOpen(true)}
                        aria-label="Open menu"
                    >
                        <Menu size={23} />
                    </button>

                    <div className="topbar-heading">

                        <h1>{title}</h1>

                        <p>
                            Laboratory Resource Management Platform
                        </p>

                    </div>

                    <div className="topbar-profile">

                        <div className="topbar-avatar">
                            {userName.charAt(0).toUpperCase()}
                        </div>

                    </div>

                </header>

                {/* PAGE CONTENT */}

                <div className="dashboard-content">
                    {children}
                </div>

            </main>

        </div>
    );
}
