
import { useCallback, useEffect, useState } from "react";
import {
    ArrowLeft,
    ClipboardList,
    Check,
    X,
    RefreshCw,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import api from "../api";
import DashboardLayout from "./DashboardLayout";
import "./SystemAdminDashboard.css";

export default function RoleRequests() {
    const navigate = useNavigate();

    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [processingId, setProcessingId] = useState(null);

    const currentRole = sessionStorage.getItem("userRole");

    const isSystemAdmin =
        currentRole === "SYSTEM_ADMINISTRATOR";

    const isInstitutionAdmin =
        currentRole === "INSTITUTION_ADMINISTRATOR";

    const dashboardTitle = isSystemAdmin
        ? "System Administrator"
        : "Institution Administrator";

    const dashboardPath = isSystemAdmin
        ? "/system-admin-dashboard"
        : "/institution-admin-dashboard";

    // Check whether the current administrator
    // can approve or reject a particular request.
    const canReviewRequest = (request) => {
        if (isSystemAdmin) {
            return (
                request.requestedRole ===
                "INSTITUTION_ADMINISTRATOR"
            );
        }

        if (isInstitutionAdmin) {
            return (
                request.requestedRole !==
                "INSTITUTION_ADMINISTRATOR"
            );
        }

        return false;
    };

    // Fetch pending requests from backend.
    const fetchRequests = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const response = await api.get(
                "/role-requests/pending"
            );

            setRequests(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );

        } catch (err) {
            console.error(
                "Fetch role requests error:",
                err
            );

            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                "Unable to load role requests. Please try again."
            );

        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (isSystemAdmin || isInstitutionAdmin) {
            fetchRequests();
        } else {
            setError("You are not authorized to access role requests.");
            setLoading(false);
        }
    }, [fetchRequests, isSystemAdmin, isInstitutionAdmin]);

    // Approve or reject request.
    const reviewRequest = async (id, approved) => {
        try {
            setProcessingId(id);
            setError("");
            setSuccess("");

            const response = await api.put(
                `/role-requests/${id}/review`,
                {
                    approved: approved,
                }
            );

            console.log(
                "Role request review response:",
                response.data
            );

            setSuccess(
                approved
                    ? "Role request approved successfully."
                    : "Role request rejected successfully."
            );

            // Refresh pending requests.
            await fetchRequests();

        } catch (err) {
            console.error(
                "Review role request error:",
                err
            );

            console.error(
                "Backend response:",
                err.response?.data
            );

            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                `Unable to ${
                    approved ? "approve" : "reject"
                } the request. Please try again.`
            );

        } finally {
            setProcessingId(null);
        }
    };

    return (
        <div className="system-admin-dashboard">

            <DashboardLayout title={dashboardTitle}>

                <div className="sysadmin-page">

                    {/* Back to respective dashboard */}
                    <button
                        type="button"
                        className="sysadmin-refresh-button"
                        onClick={() => navigate(dashboardPath)}
                    >
                        <ArrowLeft size={16} />
                        Back to Dashboard
                    </button>

                    <section className="sysadmin-role-requests">

                        {/* Heading */}
                        <div className="sysadmin-section-heading">

                            <div>
                                <h2>Role Requests</h2>

                                <p>
                                    {isSystemAdmin
                                        ? "Review institution administrator registration requests."
                                        : "Review role upgrade requests belonging to your institution."}
                                </p>
                            </div>

                            <button
                                type="button"
                                className="sysadmin-refresh-button"
                                onClick={fetchRequests}
                                disabled={
                                    loading ||
                                    processingId !== null
                                }
                            >
                                <RefreshCw
                                    size={16}
                                    className={
                                        loading
                                            ? "sysadmin-spinning"
                                            : ""
                                    }
                                />

                                Refresh
                            </button>

                        </div>

                        {/* Error message */}
                        {error && (
                            <div
                                className="sysadmin-request-error"
                                role="alert"
                            >
                                <span>{error}</span>

                                <button
                                    type="button"
                                    onClick={fetchRequests}
                                    disabled={loading}
                                >
                                    Try again
                                </button>
                            </div>
                        )}

                        {/* Success message */}
                        {success && (
                            <div
                                className="sysadmin-request-success"
                                role="status"
                            >
                                {success}
                            </div>
                        )}

                        {/* Loading state */}
                        {loading ? (

                            <p className="sysadmin-request-message">
                                Loading role requests...
                            </p>

                        ) : !isSystemAdmin && !isInstitutionAdmin ? (

                            <div className="sysadmin-request-empty">
                                <X size={28} />

                                <h3>Access Denied</h3>

                                <p>
                                    You are not authorized to view role requests.
                                </p>
                            </div>

                        ) : requests.length === 0 ? (

                            /* Empty state */
                            <div className="sysadmin-request-empty">

                                <ClipboardList size={28} />

                                <h3>No pending role requests</h3>

                                <p>
                                    {isSystemAdmin
                                        ? "Institution administrator requests will appear here."
                                        : "New requests from your institution will appear here."}
                                </p>

                            </div>

                        ) : (

                            /* Request list */
                            <div className="sysadmin-request-list">

                                {requests.map((request) => (

                                    <article
                                        className="sysadmin-request-card"
                                        key={request.id}
                                    >

                                        {/* Applicant details */}
                                        <div className="sysadmin-request-details">

                                            <h3>
                                                {request.fullName ||
                                                    "Unnamed user"}
                                            </h3>

                                            <p>
                                                {request.email}
                                            </p>

                                            <span className="sysadmin-request-role">
                                                {String(
                                                    request.requestedRole || ""
                                                ).replaceAll("_", " ")}
                                            </span>

                                            <small>
                                                Requested:{" "}
                                                {request.createdAt
                                                    ? new Date(
                                                        request.createdAt
                                                    ).toLocaleString()
                                                    : "—"}
                                            </small>

                                        </div>

                                        {/* Review actions */}
                                        <div className="sysadmin-request-actions">

                                            {canReviewRequest(request) ? (

                                                <>

                                                    <button
                                                        type="button"
                                                        className="sysadmin-approve-button"
                                                        disabled={
                                                            processingId !== null
                                                        }
                                                        onClick={() =>
                                                            reviewRequest(
                                                                request.id,
                                                                true
                                                            )
                                                        }
                                                    >
                                                        <Check size={16} />

                                                        {processingId === request.id
                                                            ? "Processing..."
                                                            : "Approve"}
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="sysadmin-reject-button"
                                                        disabled={
                                                            processingId !== null
                                                        }
                                                        onClick={() =>
                                                            reviewRequest(
                                                                request.id,
                                                                false
                                                            )
                                                        }
                                                    >
                                                        <X size={16} />

                                                        {processingId === request.id
                                                            ? "Processing..."
                                                            : "Reject"}
                                                    </button>

                                                </>

                                            ) : (

                                                <span className="sysadmin-request-role">
                                                    Awaiting System Admin Review
                                                </span>

                                            )}

                                        </div>

                                    </article>

                                ))}

                            </div>

                        )}

                    </section>

                </div>

            </DashboardLayout>

        </div>
    );
}
