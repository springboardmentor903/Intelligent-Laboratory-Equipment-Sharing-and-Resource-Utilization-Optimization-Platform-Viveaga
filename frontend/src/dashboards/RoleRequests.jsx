
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
    const [processingId, setProcessingId] = useState(null);

    const fetchRequests = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const response = await api.get("/role-requests/pending");

            setRequests(
                Array.isArray(response.data) ? response.data : []
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load role requests. Please try again."
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchRequests();
    }, [fetchRequests]);

    const reviewRequest = async (id, approved) => {
        try {
            setProcessingId(id);
            setError("");

            await api.put(`/role-requests/${id}/review`, {
                approved,
            });

            setRequests((current) =>
                current.filter((request) => request.id !== id)
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
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
            <DashboardLayout title="System Administrator">
                <div className="sysadmin-page">
                    <button
                        type="button"
                        className="sysadmin-refresh-button"
                        onClick={() =>
                            navigate("/system-admin-dashboard")
                        }
                    >
                        <ArrowLeft size={16} />
                        Back to Dashboard
                    </button>

                    <section className="sysadmin-role-requests">
                        <div className="sysadmin-section-heading">
                            <div>
                                <h2>Role Requests</h2>
                                <p>
                                    Review and manage user role upgrade
                                    requests
                                </p>
                            </div>

                            <button
                                type="button"
                                className="sysadmin-refresh-button"
                                onClick={fetchRequests}
                                disabled={
                                    loading || processingId !== null
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

                        {loading ? (
                            <p className="sysadmin-request-message">
                                Loading role requests...
                            </p>
                        ) : requests.length === 0 ? (
                            <div className="sysadmin-request-empty">
                                <ClipboardList size={28} />

                                <h3>No pending role requests</h3>

                                <p>
                                    New requests will appear here for review.
                                </p>
                            </div>
                        ) : (
                            <div className="sysadmin-request-list">
                                {requests.map((request) => (
                                    <article
                                        className="sysadmin-request-card"
                                        key={request.id}
                                    >
                                        <div className="sysadmin-request-details">
                                            <h3>
                                                {request.fullName ||
                                                    "Unnamed user"}
                                            </h3>

                                            <p>{request.email}</p>

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

                                        <div className="sysadmin-request-actions">
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
                                                Reject
                                            </button>
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
