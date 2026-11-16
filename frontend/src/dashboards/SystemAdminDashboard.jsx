
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
    Building2,
    X,
    Search
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


// ALL USERS MODAL
function UsersModal({ users, loading, error, onClose }) {

    const [searchTerm, setSearchTerm] = useState("");
    const [roleFilter, setRoleFilter] = useState("ALL");

    const filteredUsers = users.filter((user) => {

        const search = searchTerm.toLowerCase();

        const matchesSearch =
            user.fullName?.toLowerCase().includes(search) ||
            user.email?.toLowerCase().includes(search) ||
            user.institutionName?.toLowerCase().includes(search);

        const matchesRole =
            roleFilter === "ALL" || user.role === roleFilter;

        return matchesSearch && matchesRole;
    });

    return (
        <div
            className="sysadmin-modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) {
                    onClose();
                }
            }}
        >
            <div
                className="sysadmin-users-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="sysadmin-users-title"
            >

                {/* HEADER */}
                <div className="sysadmin-modal-header">

                    <div>
                        <h2 id="sysadmin-users-title">
                            All Registered Users
                        </h2>

                        <p>
                            View users across all registered institutions.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="sysadmin-modal-close"
                        onClick={onClose}
                        aria-label="Close user list"
                    >
                        <X size={21} />
                    </button>

                </div>


                {/* USER COUNT */}
                <div className="sysadmin-users-count">
                    <Users size={19} />
                    <strong>
                        {users.length} Registered Users
                    </strong>
                </div>


                {/* SEARCH AND FILTER */}
                <div className="sysadmin-users-filters">

                    <div className="sysadmin-search-box">

                        <Search size={17} />

                        <input
                            type="text"
                            placeholder="Search name, email or institution..."
                            value={searchTerm}
                            onChange={(event) =>
                                setSearchTerm(event.target.value)
                            }
                        />

                    </div>

                    <select
                        value={roleFilter}
                        onChange={(event) =>
                            setRoleFilter(event.target.value)
                        }
                    >
                        <option value="ALL">All Roles</option>

                        <option value="SYSTEM_ADMINISTRATOR">
                            System Administrator
                        </option>

                        <option value="INSTITUTION_ADMINISTRATOR">
                            Institution Administrator
                        </option>

                        <option value="DEPARTMENT_HEAD">
                            Department Head
                        </option>

                        <option value="LAB_MANAGER">
                            Lab Manager
                        </option>

                        <option value="LAB_TECHNICIAN">
                            Lab Technician
                        </option>

                        <option value="RESEARCHER_STUDENT">
                            Researcher / Student
                        </option>

                    </select>

                </div>


                {/* USERS TABLE */}
                {loading ? (

                    <div className="sysadmin-modal-message">
                        Loading users...
                    </div>

                ) : error ? (

                    <div className="sysadmin-modal-error">
                        {error}
                    </div>

                ) : (

                    <div className="sysadmin-users-table-wrapper">

                        <table className="sysadmin-users-table">

                            <thead>
                            <tr>
                                <th>User</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Institution</th>
                            </tr>
                            </thead>

                            <tbody>

                            {filteredUsers.map((user) => (

                                <tr key={user.id}>

                                    <td>
                                        <div className="sysadmin-table-user">

                                                <span>
                                                    {user.fullName
                                                        ?.charAt(0)
                                                        ?.toUpperCase() || "U"}
                                                </span>

                                            <strong>
                                                {user.fullName}
                                            </strong>

                                        </div>
                                    </td>

                                    <td>{user.email}</td>

                                    <td>
                                            <span className="sysadmin-role-badge">
                                                {user.role?.replaceAll("_", " ")}
                                            </span>
                                    </td>

                                    <td>
                                        {user.institutionName || "Not Assigned"}
                                    </td>

                                </tr>

                            ))}

                            {filteredUsers.length === 0 && (

                                <tr>
                                    <td
                                        colSpan="4"
                                        className="sysadmin-no-users"
                                    >
                                        No matching users found.
                                    </td>
                                </tr>

                            )}

                            </tbody>

                        </table>

                    </div>

                )}


                {/* FOOTER */}
                <div className="sysadmin-modal-footer">

                    <span>
                        Showing {filteredUsers.length} of {users.length} users
                    </span>

                    <button
                        type="button"
                        onClick={onClose}
                    >
                        Close
                    </button>

                </div>

            </div>
        </div>
    );
}



// MAIN SYSTEM ADMIN DASHBOARD
export default function SystemAdminDashboard() {

    const navigate = useNavigate();


    // PENDING REQUESTS
    const [requests, setRequests] = useState([]);
    const [loadingRequests, setLoadingRequests] = useState(true);
    const [requestError, setRequestError] = useState("");


    // INSTITUTIONS
    const [institutions, setInstitutions] = useState([]);
    const [loadingInstitutions, setLoadingInstitutions] = useState(true);
    const [institutionError, setInstitutionError] = useState("");


    // ALL USERS
    const [allUsers, setAllUsers] = useState([]);
    const [loadingUsers, setLoadingUsers] = useState(false);
    const [usersError, setUsersError] = useState("");


    // USERS MODAL
    const [showUsersModal, setShowUsersModal] = useState(false);



    // FETCH PENDING REQUESTS
    const fetchPendingRequests = useCallback(async () => {

        try {

            setLoadingRequests(true);
            setRequestError("");

            const response = await api.get("/role-requests/pending");

            setRequests(
                Array.isArray(response.data)
                    ? response.data
                    : []
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



    // FETCH INSTITUTIONS
    const fetchInstitutions = useCallback(async () => {

        try {

            setLoadingInstitutions(true);
            setInstitutionError("");

            const response = await api.get("/system-admin/institutions");

            setInstitutions(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );

        } catch (error) {

            setInstitutionError(
                error.response?.data?.message ||
                "Unable to load institutions."
            );

        } finally {

            setLoadingInstitutions(false);

        }

    }, []);



    // FETCH ALL USERS
    const fetchAllUsers = useCallback(async () => {

        try {

            setLoadingUsers(true);
            setUsersError("");

            const response = await api.get("/system-admin/users");

            setAllUsers(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );

        } catch (error) {

            setUsersError(
                error.response?.data?.message ||
                "Unable to load registered users."
            );

        } finally {

            setLoadingUsers(false);

        }

    }, []);



    // INITIAL DATA LOADING
    useEffect(() => {

        fetchPendingRequests();
        fetchInstitutions();
        fetchAllUsers();

    }, [
        fetchPendingRequests,
        fetchInstitutions,
        fetchAllUsers
    ]);



    // OPEN USERS MODAL
    const openUsersModal = () => {

        setShowUsersModal(true);

        if (usersError) {
            fetchAllUsers();
        }

    };



    // OPEN INSTITUTIONS PAGE
    const openInstitutionsPage = () => {

        navigate("/institutions");

    };



    // OPEN ROLE REQUESTS PAGE
    const openRoleRequests = () => {

        navigate("/role-requests");

    };



    // TOTAL USERS
    const totalUsers = allUsers.length;



    // DASHBOARD STATISTICS
    const statistics = [

        {
            title: "Total Users",
            value: loadingUsers ? "…" : String(totalUsers),
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
            title: "Institutions",
            value: loadingInstitutions
                ? "…"
                : String(institutions.length),
            description: "Registered institutions",
            icon: Building2,
            color: "green",
        },

        {
            title: "Pending Requests",
            value: loadingRequests
                ? "…"
                : String(requests.length),
            description: "Requests awaiting review",
            icon: Clock3,
            color: "orange",
        },

    ];



    return (

        <div className="system-admin-dashboard">

            <DashboardLayout title="System Administrator">

                <div className="sysadmin-page">


                    {/* HERO SECTION */}
                    <section className="sysadmin-hero">

                        <div className="sysadmin-hero-content">

                            <span className="sysadmin-eyebrow">
                                <ShieldCheck size={15} />
                                ADMINISTRATION OVERVIEW
                            </span>

                            <h2>
                                System Control Center
                            </h2>

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



                    {/* PLATFORM OVERVIEW */}
                    <section className="sysadmin-overview">

                        <div className="sysadmin-section-heading">

                            <div>
                                <h2>Platform Overview</h2>

                                <p>
                                    Key system statistics at a glance
                                </p>
                            </div>


                            <span className="sysadmin-live-label">
                                <span />
                                Overview
                            </span>

                        </div>



                        {/* STATISTICS CARDS */}
                        <div className="sysadmin-stats-grid">

                            {statistics.map((stat) => {

                                const Icon = stat.icon;

                                const isPendingCard =
                                    stat.title === "Pending Requests";

                                const isUsersCard =
                                    stat.title === "Total Users";

                                const isInstitutionsCard =
                                    stat.title === "Institutions";


                                const isClickable =
                                    isPendingCard ||
                                    isUsersCard ||
                                    isInstitutionsCard;


                                return (

                                    <article

                                        className={`sysadmin-stat-card ${
                                            isClickable
                                                ? "sysadmin-clickable-card"
                                                : ""
                                        }`}

                                        key={stat.title}


                                        onClick={
                                            isPendingCard
                                                ? openRoleRequests
                                                : isUsersCard
                                                    ? openUsersModal
                                                    : isInstitutionsCard
                                                        ? openInstitutionsPage
                                                        : undefined
                                        }


                                        onKeyDown={(event) => {

                                            if (
                                                isClickable &&
                                                (
                                                    event.key === "Enter" ||
                                                    event.key === " "
                                                )
                                            ) {

                                                event.preventDefault();

                                                if (isPendingCard) {

                                                    openRoleRequests();

                                                } else if (isUsersCard) {

                                                    openUsersModal();

                                                } else if (isInstitutionsCard) {

                                                    openInstitutionsPage();

                                                }

                                            }

                                        }}


                                        role={
                                            isClickable
                                                ? "button"
                                                : undefined
                                        }


                                        tabIndex={
                                            isClickable
                                                ? 0
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


                                        <h3>
                                            {stat.value}
                                        </h3>


                                        <p className="sysadmin-stat-description">
                                            {stat.description}
                                        </p>


                                        {isClickable && (

                                            <span className="sysadmin-card-hint">
                                                Click to view details
                                            </span>

                                        )}

                                    </article>

                                );

                            })}

                        </div>



                        {/* PENDING REQUEST ERROR */}
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



                        {/* INSTITUTION ERROR */}
                        {institutionError && (

                            <div
                                className="sysadmin-request-error"
                                role="alert"
                            >

                                <span>{institutionError}</span>

                                <button
                                    type="button"
                                    onClick={fetchInstitutions}
                                    disabled={loadingInstitutions}
                                >
                                    Try again
                                </button>

                            </div>

                        )}

                    </section>



                    {/* SYSTEM ADMINISTRATION */}
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

                                            <h3>
                                                {action.title}
                                            </h3>

                                            <p>
                                                {action.description}
                                            </p>

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



                    {/* SYSTEM ACTIVITY */}
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


                            <h3>
                                No activity to display yet
                            </h3>


                            <p>
                                User activity, access events, and audit records
                                will appear here when connected to the backend.
                            </p>

                        </div>

                    </section>



                    {/* FOOTER */}
                    <footer className="sysadmin-footer">

                        <Settings size={15} />

                        <span>
                            Laboratory Resource Management Platform
                        </span>

                        <span className="sysadmin-footer-separator">
                            •
                        </span>

                        <span>
                            System Administration
                        </span>

                    </footer>


                </div>

            </DashboardLayout>



            {/* ALL USERS POPUP */}
            {showUsersModal && (

                <UsersModal

                    users={allUsers}

                    loading={loadingUsers}

                    error={usersError}

                    onClose={() => setShowUsersModal(false)}

                />

            )}

        </div>

    );

}
