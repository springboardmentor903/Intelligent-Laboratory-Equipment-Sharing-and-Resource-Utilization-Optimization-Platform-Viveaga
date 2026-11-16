
import React, {
    useCallback,
    useEffect,
    useState,
} from "react";

import { useNavigate } from "react-router-dom";

import {
    AlertCircle,
    CalendarDays,
    CheckCircle2,
    Clock3,
    Inbox,
    RefreshCw,
    XCircle,
} from "lucide-react";

import api from "../api";
import DashboardLayout from "./DashboardLayout";
import "./WaitlistPage.css";

// ==========================================
// STATUS HELPERS
// ==========================================

const getStatusLabel = (status) => {
    const labels = {
        WAITING: "Waiting",
        NOTIFIED: "Offer Received",
        BOOKED: "Booking Created",
        CANCELLED: "Cancelled",
        DECLINED: "Declined",
        EXPIRED: "Expired",
    };

    return labels[status] || status || "Unknown";
};

// ==========================================
// DATE FORMATTER
// ==========================================

const formatDateTime = (value) => {
    if (!value) {
        return "Not available";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
};

// ==========================================
// OFFER COUNTDOWN
// ==========================================

const getRemainingTime = (expiryTime, currentTime) => {
    if (!expiryTime) {
        return "Expiry time unavailable";
    }

    const difference =
        new Date(expiryTime).getTime() -
        currentTime.getTime();

    if (Number.isNaN(difference)) {
        return "Expiry time unavailable";
    }

    if (difference <= 0) {
        return "Offer expired";
    }

    const totalSeconds = Math.floor(difference / 1000);

    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    return `${minutes}m ${String(seconds).padStart(2, "0")}s remaining`;
};

// ==========================================
// WAITLIST PAGE
// ==========================================

export default function WaitlistPage() {

    const navigate = useNavigate();

    const [waitlists, setWaitlists] = useState([]);

    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    const [busyId, setBusyId] = useState(null);

    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    const [currentTime, setCurrentTime] = useState(
        new Date()
    );

    // ==========================================
    // ERROR MESSAGE
    // ==========================================

    const getErrorMessage = (err) => {
        const data = err.response?.data;

        if (typeof data === "string" && data.trim()) {
            return data;
        }

        if (data?.message) {
            return data.message;
        }

        if (data?.error) {
            return data.error;
        }

        if (!err.response) {
            return "Unable to connect to the backend. Please check whether Spring Boot is running.";
        }

        return `Request failed with status ${err.response.status}.`;
    };

    // ==========================================
    // LOAD MY WAITLIST
    // ==========================================

    const loadWaitlist = useCallback(async (showLoader = true) => {

        try {

            if (showLoader) {
                setLoading(true);
            } else {
                setRefreshing(true);
            }

            setError("");

            const response = await api.get("/waitlist/my");

            setWaitlists(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );

        } catch (err) {

            console.error("Waitlist loading error:", err);

            setError(getErrorMessage(err));

        } finally {

            setLoading(false);
            setRefreshing(false);
        }

    }, []);

    // ==========================================
    // INITIAL LOAD AND AUTO REFRESH
    // ==========================================

    useEffect(() => {

        loadWaitlist();

        // Update countdown every second.
        const clockInterval = setInterval(() => {
            setCurrentTime(new Date());
        }, 1000);

        // Refresh server data every 30 seconds.
        const refreshInterval = setInterval(() => {
            loadWaitlist(false);
        }, 30000);

        return () => {
            clearInterval(clockInterval);
            clearInterval(refreshInterval);
        };

    }, [loadWaitlist]);

    // ==========================================
    // HANDLE WAITLIST ACTIONS
    // ==========================================

    const handleAction = async (waitlistId, action) => {

        setBusyId(waitlistId);
        setError("");
        setMessage("");

        try {

            let response;

            if (action === "accept") {

                response = await api.post(
                    `/waitlist/${waitlistId}/accept`
                );

                if (response.data?.status === "BOOKED") {

                    setMessage(
                        "Offer accepted successfully. Your booking request has been created and is awaiting manager approval."
                    );

                } else if (response.data?.status === "EXPIRED") {

                    setMessage(
                        "This offer has expired. Please refresh your waitlist."
                    );

                } else {

                    setMessage(
                        "Your offer response was processed."
                    );
                }

            } else if (action === "decline") {

                response = await api.post(
                    `/waitlist/${waitlistId}/decline`
                );

                setMessage(
                    "Offer declined. The system can now offer the slot to the next eligible user."
                );

            } else if (action === "cancel") {

                response = await api.put(
                    `/waitlist/${waitlistId}/cancel`
                );

                setMessage(
                    "Your waitlist request has been cancelled."
                );
            }

            await loadWaitlist(false);

        } catch (err) {

            console.error("Waitlist action error:", err);

            setError(getErrorMessage(err));

        } finally {

            setBusyId(null);
        }
    };

    // ==========================================
    // STATUS COUNTS
    // ==========================================

    const totalRequests = waitlists.length;

    const waitingCount = waitlists.filter(
        (item) => item.status === "WAITING"
    ).length;

    const notifiedCount = waitlists.filter(
        (item) => item.status === "NOTIFIED"
    ).length;

    const bookedCount = waitlists.filter(
        (item) => item.status === "BOOKED"
    ).length;

    // ==========================================
    // PAGE UI
    // ==========================================

    return (
        <DashboardLayout title="My Waitlist">

            <div className="waitlist-page">

                {/* PAGE HEADER */}

                <div className="waitlist-header">

                    <div>
                        <div className="waitlist-eyebrow">
                            LAB RESOURCE MANAGEMENT
                        </div>

                        <h2>My Waitlist</h2>

                        <p>
                            Track your equipment waitlist requests
                            and respond when a slot becomes available.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="waitlist-refresh-button"
                        onClick={() => loadWaitlist(false)}
                        disabled={refreshing || loading}
                    >
                        <RefreshCw
                            size={17}
                            className={
                                refreshing
                                    ? "waitlist-spinning"
                                    : ""
                            }
                        />

                        Refresh
                    </button>

                </div>

                {/* SUMMARY CARDS */}

                <div className="waitlist-summary">

                    <div className="waitlist-summary-card">
                        <div className="waitlist-summary-icon total">
                            <Inbox size={21} />
                        </div>

                        <div>
                            <span>Total Requests</span>
                            <strong>{totalRequests}</strong>
                        </div>
                    </div>

                    <div className="waitlist-summary-card">
                        <div className="waitlist-summary-icon waiting">
                            <Clock3 size={21} />
                        </div>

                        <div>
                            <span>Waiting</span>
                            <strong>{waitingCount}</strong>
                        </div>
                    </div>

                    <div className="waitlist-summary-card">
                        <div className="waitlist-summary-icon notified">
                            <AlertCircle size={21} />
                        </div>

                        <div>
                            <span>Offers Received</span>
                            <strong>{notifiedCount}</strong>
                        </div>
                    </div>

                    <div className="waitlist-summary-card">
                        <div className="waitlist-summary-icon booked">
                            <CheckCircle2 size={21} />
                        </div>

                        <div>
                            <span>Bookings Created</span>
                            <strong>{bookedCount}</strong>
                        </div>
                    </div>

                </div>

                {/* SUCCESS MESSAGE */}

                {message && (
                    <div
                        className="waitlist-alert success"
                        role="status"
                    >
                        <CheckCircle2 size={19} />
                        <span>{message}</span>
                    </div>
                )}

                {/* ERROR MESSAGE */}

                {error && (
                    <div
                        className="waitlist-alert error"
                        role="alert"
                    >
                        <AlertCircle size={19} />
                        <span>{error}</span>
                    </div>
                )}

                {/* SECTION HEADER */}

                <div className="waitlist-section-heading">

                    <div>
                        <h3>Your Requests</h3>

                        <p>
                            Manage your pending and previous
                            waitlist requests.
                        </p>
                    </div>

                    <span className="waitlist-total-label">
                        {totalRequests} requests
                    </span>

                </div>

                {/* LOADING */}

                {loading ? (

                    <div className="waitlist-loading">

                        <RefreshCw
                            size={25}
                            className="waitlist-spinning"
                        />

                        <p>Loading your waitlist...</p>

                    </div>

                ) : waitlists.length === 0 ? (

                    /* EMPTY STATE */

                    <div className="waitlist-empty">

                        <div className="waitlist-empty-icon">
                            <Inbox size={34} />
                        </div>

                        <h3>No waitlist requests yet</h3>

                        <p>
                            When an equipment booking slot is
                            unavailable, you can join its waitlist
                            from the booking page.
                        </p>

                        <button
                            type="button"
                            onClick={() => navigate("/equipment")}
                        >
                            Browse Equipment
                        </button>

                    </div>

                ) : (

                    /* WAITLIST CARDS */

                    <div className="waitlist-list">

                        {waitlists.map((item) => {

                            const status =
                                item.status || "WAITING";

                            const isNotified =
                                status === "NOTIFIED";

                            const isWaiting =
                                status === "WAITING";

                            const isActive =
                                isNotified || isWaiting;

                            const isBusy =
                                busyId === item.id;

                            const remainingTime =
                                isNotified
                                    ? getRemainingTime(
                                        item.offerExpiresAt,
                                        currentTime
                                    )
                                    : "";

                            const offerExpired =
                                isNotified &&
                                remainingTime === "Offer expired";

                            return (

                                <article
                                    className="waitlist-card"
                                    key={item.id}
                                >

                                    {/* CARD HEADER */}

                                    <div className="waitlist-card-header">

                                        <div className="waitlist-equipment-icon">
                                            <CalendarDays size={23} />
                                        </div>

                                        <div className="waitlist-equipment-info">

                                            <h3>
                                                {item.equipmentName ||
                                                    `Equipment #${item.equipmentId}`}
                                            </h3>

                                            <span>
                                                Equipment ID: {item.equipmentId}
                                            </span>

                                        </div>

                                        <span
                                            className={`waitlist-status status-${status.toLowerCase()}`}
                                        >
                                            {getStatusLabel(status)}
                                        </span>

                                    </div>

                                    {/* BOOKING DETAILS */}

                                    <div className="waitlist-details">

                                        <div className="waitlist-detail-item">
                                            <span>Requested Start</span>

                                            <strong>
                                                {formatDateTime(
                                                    item.requestedStartTime
                                                )}
                                            </strong>
                                        </div>

                                        <div className="waitlist-detail-item">
                                            <span>Requested End</span>

                                            <strong>
                                                {formatDateTime(
                                                    item.requestedEndTime
                                                )}
                                            </strong>
                                        </div>

                                        <div className="waitlist-detail-item">
                                            <span>Joined On</span>

                                            <strong>
                                                {formatDateTime(
                                                    item.createdAt
                                                )}
                                            </strong>
                                        </div>

                                        <div className="waitlist-detail-item purpose">
                                            <span>Purpose</span>

                                            <strong>
                                                {item.purpose || "Not specified"}
                                            </strong>
                                        </div>

                                    </div>

                                    {/* OFFER DETAILS */}

                                    {isNotified && (

                                        <div className="waitlist-offer-panel">

                                            <div className="waitlist-offer-heading">
                                                <CheckCircle2 size={20} />

                                                <strong>
                                                    Equipment slot offered to you!
                                                </strong>
                                            </div>

                                            <div className="waitlist-offer-times">

                                                <div>
                                                    <span>Offered Start</span>

                                                    <strong>
                                                        {formatDateTime(
                                                            item.offeredStartTime
                                                        )}
                                                    </strong>
                                                </div>

                                                <div>
                                                    <span>Offered End</span>

                                                    <strong>
                                                        {formatDateTime(
                                                            item.offeredEndTime
                                                        )}
                                                    </strong>
                                                </div>

                                                <div>
                                                    <span>Offer Expires</span>

                                                    <strong>
                                                        {formatDateTime(
                                                            item.offerExpiresAt
                                                        )}
                                                    </strong>
                                                </div>

                                            </div>

                                            <div className="waitlist-countdown">

                                                <Clock3 size={19} />

                                                <strong>
                                                    {remainingTime}
                                                </strong>

                                            </div>

                                            {offerExpired && (
                                                <p className="waitlist-expiry-note">
                                                    The offer deadline has
                                                    passed. The system will
                                                    process the expiry.
                                                </p>
                                            )}

                                        </div>

                                    )}

                                    {/* WAITING MESSAGE */}

                                    {isWaiting && (

                                        <div className="waitlist-waiting-note">

                                            <Clock3 size={18} />

                                            <span>
                                                You are in the waiting queue.
                                                We will show an offer here
                                                when your requested slot
                                                becomes available.
                                            </span>

                                        </div>

                                    )}

                                    {/* BOOKED MESSAGE */}

                                    {status === "BOOKED" && (

                                        <div className="waitlist-booked-note">

                                            <CheckCircle2 size={18} />

                                            <span>
                                                Your offer was accepted and
                                                a booking request was created.
                                                Check My Bookings for its
                                                approval status.
                                            </span>

                                        </div>

                                    )}

                                    {/* DECLINED MESSAGE */}

                                    {status === "DECLINED" && (

                                        <div className="waitlist-inactive-note">

                                            <XCircle size={18} />

                                            <span>
                                                You declined this equipment
                                                offer.
                                            </span>

                                        </div>

                                    )}

                                    {/* EXPIRED MESSAGE */}

                                    {status === "EXPIRED" && (

                                        <div className="waitlist-inactive-note">

                                            <AlertCircle size={18} />

                                            <span>
                                                This offer has expired.
                                            </span>

                                        </div>

                                    )}

                                    {/* CANCELLED MESSAGE */}

                                    {status === "CANCELLED" && (

                                        <div className="waitlist-inactive-note">

                                            <XCircle size={18} />

                                            <span>
                                                You cancelled this waitlist
                                                request.
                                            </span>

                                        </div>

                                    )}

                                    {/* ACTION BUTTONS */}

                                    {isActive && (

                                        <div className="waitlist-card-actions">

                                            {isNotified && (

                                                <>

                                                    <button
                                                        type="button"
                                                        className="waitlist-action-button accept"
                                                        disabled={isBusy || offerExpired}
                                                        onClick={() =>
                                                            handleAction(
                                                                item.id,
                                                                "accept"
                                                            )
                                                        }
                                                    >
                                                        <CheckCircle2 size={17} />

                                                        {isBusy
                                                            ? "Processing..."
                                                            : "Accept Offer"}
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="waitlist-action-button decline"
                                                        disabled={isBusy || offerExpired}
                                                        onClick={() =>
                                                            handleAction(
                                                                item.id,
                                                                "decline"
                                                            )
                                                        }
                                                    >
                                                        <XCircle size={17} />

                                                        Decline
                                                    </button>

                                                </>

                                            )}

                                            <button
                                                type="button"
                                                className="waitlist-action-button cancel"
                                                disabled={isBusy}
                                                onClick={() =>
                                                    handleAction(
                                                        item.id,
                                                        "cancel"
                                                    )
                                                }
                                            >
                                                Cancel Request
                                            </button>

                                        </div>

                                    )}

                                </article>

                            );

                        })}

                    </div>

                )}

            </div>

        </DashboardLayout>
    );
}
