
import { useCallback, useEffect, useState } from "react";
import {
    Share2,
    RefreshCw,
    CheckCircle,
    XCircle,
    Building2,
    CalendarDays,
    IndianRupee,
    Package,
    Send,
    Clock,
} from "lucide-react";

import api from "../api";
import "./InstitutionSharing.css";

function formatDate(date) {
    if (!date) return "Not specified";

    return new Date(`${date}T00:00:00`).toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

function normalizeList(response) {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.content)) return response.content;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.data?.content)) return response.data.content;
    if (Array.isArray(response?.items)) return response.items;

    return [];
}

function getRequestEquipment(request) {
    return (
        request?.equipmentSharing?.equipment?.name ||
        request?.equipmentSharing?.equipmentName ||
        "Equipment"
    );
}

function getRequestInstitution(request) {
    return (
        request?.requestingInstitution?.name ||
        request?.requestingInstitution?.institutionName ||
        "Institution"
    );
}

function getRequestOwner(request) {
    return (
        request?.equipmentSharing?.institution?.name ||
        request?.equipmentSharing?.institutionName ||
        "Equipment owner"
    );
}

function getRequestDates(request) {
    const sharing = request?.equipmentSharing;

    return `${formatDate(sharing?.startDate)} – ${formatDate(sharing?.endDate)}`;
}

function getRequestFee(request) {
    const fee = request?.equipmentSharing?.hourlyCost;

    return fee == null ? "Fee not specified" : `₹${fee} / hour`;
}

function getErrorMessage(err, fallback) {
    const responseData = err?.response?.data;

    if (typeof responseData === "string") {
        return responseData;
    }

    return (
        responseData?.message ||
        responseData?.error ||
        fallback
    );
}

function StatusBadge({ status }) {
    const normalized = status || "UNKNOWN";

    return (
        <span className={`sharing-status status-${normalized.toLowerCase()}`}>
            {normalized.replaceAll("_", " ")}
        </span>
    );
}

// CARD FOR AVAILABLE EQUIPMENT
function EquipmentCard({ item, onRequest, requesting }) {
    return (
        <div className="sharing-card">
            <div className="sharing-card-heading">
                <div className="sharing-icon">
                    <Package size={22} />
                </div>

                <div className="sharing-card-title">
                    <h3>{item.equipmentName || "Equipment"}</h3>
                    <p>{item.assetTag || "No asset tag"}</p>
                </div>

                <StatusBadge status={item.status} />
            </div>

            <div className="sharing-details">
                <div>
                    <Building2 size={16} />
                    <span>{item.institutionName || "Unknown institution"}</span>
                </div>

                <div>
                    <CalendarDays size={16} />
                    <span>
                        {formatDate(item.startDate)} – {formatDate(item.endDate)}
                    </span>
                </div>

                <div>
                    <IndianRupee size={16} />
                    <span>
                        {item.hourlyCost == null
                            ? "Fee not specified"
                            : `₹${item.hourlyCost} per hour`}
                    </span>
                </div>
            </div>

            {item.sharingTerms && (
                <p className="sharing-terms">{item.sharingTerms}</p>
            )}

            <button
                className="sharing-primary-button"
                onClick={() => onRequest(item)}
                disabled={requesting || item.status !== "ACTIVE"}
            >
                <Send size={16} />
                {requesting ? "Submitting..." : "Request Equipment"}
            </button>
        </div>
    );
}

// CARD FOR APPROVAL REQUESTS
function RequestCard({
                         request,
                         actionType,
                         onApprove,
                         onReject,
                         processingId,
                     }) {
    const isProcessing = processingId === request.id;

    return (
        <div className="sharing-request-card">
            <div className="sharing-request-top">
                <div>
                    <h3>{getRequestEquipment(request)}</h3>
                    <p>
                        {actionType === "partner"
                            ? `Requested by ${getRequestInstitution(request)}`
                            : `Equipment owner: ${getRequestOwner(request)}`}
                    </p>
                </div>

                <StatusBadge status={request.status} />
            </div>

            <div className="sharing-request-info">
                <p>
                    <CalendarDays size={15} />
                    {getRequestDates(request)}
                </p>

                <p>
                    <IndianRupee size={15} />
                    {getRequestFee(request)}
                </p>
            </div>

            {request.purpose && (
                <p className="sharing-purpose">
                    <strong>Purpose:</strong> {request.purpose}
                </p>
            )}

            <div className="sharing-actions">
                <button
                    className="sharing-approve-button"
                    onClick={() => onApprove(request.id, actionType)}
                    disabled={isProcessing}
                >
                    <CheckCircle size={16} />
                    {isProcessing ? "Processing..." : "Approve"}
                </button>

                <button
                    className="sharing-reject-button"
                    onClick={() => onReject(request.id)}
                    disabled={isProcessing}
                >
                    <XCircle size={16} />
                    Reject
                </button>
            </div>
        </div>
    );
}

// CARD FOR ALL SUBMITTED REQUESTS
function SubmittedRequestCard({ request }) {
    return (
        <div className="sharing-request-card">
            <div className="sharing-request-top">
                <div>
                    <h3>{getRequestEquipment(request)}</h3>
                    <p>
                        Equipment owner: {getRequestOwner(request)}
                    </p>
                </div>

                <StatusBadge status={request.status} />
            </div>

            <div className="sharing-request-info">
                <p>
                    <CalendarDays size={15} />
                    {getRequestDates(request)}
                </p>

                <p>
                    <IndianRupee size={15} />
                    {getRequestFee(request)}
                </p>
            </div>

            {request.purpose && (
                <p className="sharing-purpose">
                    <strong>Purpose:</strong> {request.purpose}
                </p>
            )}

            {request.requestedAt && (
                <p className="sharing-purpose">
                    <Clock size={15} />
                    <strong> Requested:</strong>{" "}
                    {new Date(request.requestedAt).toLocaleString("en-IN")}
                </p>
            )}

            <div className="sharing-submitted-status">
                {request.status === "PENDING_PARTNER_APPROVAL" && (
                    <p>
                        <Clock size={16} />
                        Waiting for approval from your institution.
                    </p>
                )}

                {request.status === "PENDING_OWNER_APPROVAL" && (
                    <p>
                        <Clock size={16} />
                        Your institution approved this request. Waiting for
                        equipment owner approval.
                    </p>
                )}

                {request.status === "APPROVED" && (
                    <p>
                        <CheckCircle size={16} />
                        Request approved. Equipment access agreement created.
                    </p>
                )}

                {request.status === "REJECTED" && (
                    <p>
                        <XCircle size={16} />
                        This sharing request was rejected.
                    </p>
                )}
            </div>
        </div>
    );
}

export default function InstitutionSharing() {
    const [availableEquipment, setAvailableEquipment] = useState([]);
    const [mySharings, setMySharings] = useState([]);
    const [partnerRequests, setPartnerRequests] = useState([]);
    const [ownerRequests, setOwnerRequests] = useState([]);
    const [mySubmittedRequests, setMySubmittedRequests] = useState([]);

    const [loading, setLoading] = useState(true);
    const [requestingId, setRequestingId] = useState(null);
    const [processingId, setProcessingId] = useState(null);

    const [selectedEquipment, setSelectedEquipment] = useState(null);
    const [purpose, setPurpose] = useState("");

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    // LOAD ALL INSTITUTION SHARING DATA
    const loadData = useCallback(async (showLoader = true) => {
        if (showLoader) {
            setLoading(true);
        }

        try {
            const results = await Promise.allSettled([
                api.get("/sharing/available"),
                api.get("/sharing/my-institution"),
                api.get("/sharing/institution-requests/partner/pending"),
                api.get("/sharing/institution-requests/owner/pending"),
                api.get("/sharing/institution-requests/my-requests"),
            ]);

            const [
                available,
                mine,
                partner,
                owner,
                submitted,
            ] = results;

            if (available.status === "fulfilled") {
                setAvailableEquipment(
                    normalizeList(available.value.data)
                );
            } else {
                console.error(
                    "Available equipment error:",
                    available.reason
                );
            }

            if (mine.status === "fulfilled") {
                setMySharings(
                    normalizeList(mine.value.data)
                );
            } else {
                console.error(
                    "My institution sharings error:",
                    mine.reason
                );
            }

            if (partner.status === "fulfilled") {
                const requests = normalizeList(partner.value.data);

                setPartnerRequests(
                    requests.filter(
                        (request) =>
                            request.status === "PENDING_PARTNER_APPROVAL"
                    )
                );

                console.log("Partner pending requests:", requests);
            } else {
                console.error(
                    "Partner requests error:",
                    partner.reason
                );
            }

            if (owner.status === "fulfilled") {
                const requests = normalizeList(owner.value.data);

                setOwnerRequests(
                    requests.filter(
                        (request) =>
                            request.status === "PENDING_OWNER_APPROVAL"
                    )
                );

                console.log("Owner requests:", requests);
            } else {
                console.error(
                    "Owner requests error:",
                    owner.reason
                );
            }

            if (submitted.status === "fulfilled") {
                const requests = normalizeList(submitted.value.data);

                setMySubmittedRequests(requests);

                console.log("My submitted requests:", requests);
            } else {
                console.error(
                    "My submitted requests error:",
                    submitted.reason
                );

                setError(
                    "Unable to load your submitted requests."
                );
            }
        } catch (err) {
            console.error("Institution sharing load error:", err);

            setError(
                "Unable to load institutional sharing information."
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadData();
    }, [loadData]);

    // SUBMIT A NEW REQUEST
    const submitRequest = async (event) => {
        event.preventDefault();

        if (!selectedEquipment || !purpose.trim()) {
            setError(
                "Please enter the purpose for requesting this equipment."
            );
            return;
        }

        setRequestingId(selectedEquipment.id);
        setError("");
        setMessage("");

        try {
            const params = new URLSearchParams({
                equipmentSharingId: String(selectedEquipment.id),
                purpose: purpose.trim(),
            });

            await api.post(
                `/sharing/institution-requests?${params.toString()}`
            );

            setMessage(
                "Sharing request submitted successfully. Waiting for partner institution approval."
            );

            setSelectedEquipment(null);
            setPurpose("");

            await loadData(false);
        } catch (err) {
            console.error(
                "Institution sharing request failed:",
                err
            );

            const errorMessage = getErrorMessage(
                err,
                "Unable to submit the sharing request."
            );

            if (
                errorMessage.toLowerCase().includes(
                    "already has a pending"
                )
            ) {
                setMessage(
                    "Your institution already has a pending request for this equipment. Please wait for approval."
                );

                setSelectedEquipment(null);
                setPurpose("");

                await loadData(false);
            } else {
                setError(errorMessage);
            }
        } finally {
            setRequestingId(null);
        }
    };

    // APPROVE REQUEST
    const approveRequest = async (id, actionType) => {
        setProcessingId(id);
        setError("");
        setMessage("");

        try {
            const endpoint =
                actionType === "partner"
                    ? `/sharing/institution-requests/${id}/partner-approve`
                    : `/sharing/institution-requests/${id}/owner-approve`;

            await api.put(endpoint);

            setMessage(
                actionType === "partner"
                    ? "Partner approval completed. Waiting for equipment owner approval."
                    : "Final approval completed. The sharing agreement and equipment access grant have been created."
            );

            await loadData(false);
        } catch (err) {
            console.error("Approval failed:", err);

            setError(
                getErrorMessage(
                    err,
                    "Unable to approve this request."
                )
            );
        } finally {
            setProcessingId(null);
        }
    };

    // REJECT REQUEST
    const rejectRequest = async (id) => {
        const confirmed = window.confirm(
            "Are you sure you want to reject this sharing request?"
        );

        if (!confirmed) return;

        setProcessingId(id);
        setError("");
        setMessage("");

        try {
            await api.put(
                `/sharing/institution-requests/${id}/reject`
            );

            setMessage(
                "Sharing request rejected successfully."
            );

            await loadData(false);
        } catch (err) {
            console.error("Rejection failed:", err);

            setError(
                getErrorMessage(
                    err,
                    "Unable to reject this request."
                )
            );
        } finally {
            setProcessingId(null);
        }
    };

    return (
        <div className="institution-sharing-page">

            {/* PAGE HEADER */}
            <div className="institution-sharing-header">
                <div>
                    <div className="sharing-heading-icon">
                        <Share2 size={25} />
                    </div>

                    <h1>Institutional Equipment Sharing</h1>

                    <p>
                        Request equipment from partner institutions and manage
                        institutional sharing approvals.
                    </p>
                </div>

                <button
                    className="sharing-refresh-button"
                    onClick={() => loadData()}
                    disabled={loading}
                >
                    <RefreshCw size={16} />
                    Refresh
                </button>
            </div>

            {/* SUCCESS MESSAGE */}
            {message && (
                <div className="sharing-alert sharing-success">
                    <CheckCircle size={18} />
                    {message}
                </div>
            )}

            {/* ERROR MESSAGE */}
            {error && (
                <div className="sharing-alert sharing-error">
                    <XCircle size={18} />
                    {error}
                </div>
            )}

            {loading ? (
                <div className="sharing-loading">
                    <RefreshCw
                        className="sharing-spin"
                        size={25}
                    />
                    <p>
                        Loading equipment sharing information...
                    </p>
                </div>
            ) : (
                <>

                    {/* AVAILABLE EQUIPMENT */}
                    <section className="sharing-section">
                        <div className="sharing-section-heading">
                            <div>
                                <h2>Available Shared Equipment</h2>
                                <p>
                                    Browse equipment published by other institutions.
                                </p>
                            </div>

                            <span className="sharing-count">
                                {availableEquipment.length} available
                            </span>
                        </div>

                        {availableEquipment.length === 0 ? (
                            <div className="sharing-empty">
                                No shared equipment is currently available.
                            </div>
                        ) : (
                            <div className="sharing-grid">
                                {availableEquipment.map((item) => (
                                    <EquipmentCard
                                        key={item.id}
                                        item={item}
                                        onRequest={setSelectedEquipment}
                                        requesting={
                                            requestingId === item.id
                                        }
                                    />
                                ))}
                            </div>
                        )}
                    </section>

                    {/* MY SUBMITTED REQUESTS - NEW SECTION */}
                    <section className="sharing-section">
                        <div className="sharing-section-heading">
                            <div>
                                <h2>My Submitted Requests</h2>
                                <p>
                                    Track all equipment requests submitted by your institution.
                                </p>
                            </div>

                            <span className="sharing-count">
                                {mySubmittedRequests.length} requests
                            </span>
                        </div>

                        {mySubmittedRequests.length === 0 ? (
                            <div className="sharing-empty">
                                You have not submitted any equipment sharing requests yet.
                            </div>
                        ) : (
                            <div className="sharing-request-list">
                                {mySubmittedRequests.map((request) => (
                                    <SubmittedRequestCard
                                        key={request.id}
                                        request={request}
                                    />
                                ))}
                            </div>
                        )}
                    </section>

                    {/* PARTNER APPROVAL REQUESTS */}
                    <section className="sharing-section">
                        <div className="sharing-section-heading">
                            <div>
                                <h2>Partner Approval Requests</h2>
                                <p>
                                    Requests submitted by your institution that need its approval.
                                </p>
                            </div>

                            <span className="sharing-count">
                                {partnerRequests.length} pending
                            </span>
                        </div>

                        {partnerRequests.length === 0 ? (
                            <div className="sharing-empty">
                                No partner approval requests pending.
                            </div>
                        ) : (
                            <div className="sharing-request-list">
                                {partnerRequests.map((request) => (
                                    <RequestCard
                                        key={request.id}
                                        request={request}
                                        actionType="partner"
                                        onApprove={approveRequest}
                                        onReject={rejectRequest}
                                        processingId={processingId}
                                    />
                                ))}
                            </div>
                        )}
                    </section>

                    {/* OWNER APPROVAL REQUESTS */}
                    <section className="sharing-section">
                        <div className="sharing-section-heading">
                            <div>
                                <h2>Owner Approval Requests</h2>
                                <p>
                                    Requests for equipment owned by your institution.
                                </p>
                            </div>

                            <span className="sharing-count">
                                {ownerRequests.length} pending
                            </span>
                        </div>

                        {ownerRequests.length === 0 ? (
                            <div className="sharing-empty">
                                No owner approval requests pending.
                            </div>
                        ) : (
                            <div className="sharing-request-list">
                                {ownerRequests.map((request) => (
                                    <RequestCard
                                        key={request.id}
                                        request={request}
                                        actionType="owner"
                                        onApprove={approveRequest}
                                        onReject={rejectRequest}
                                        processingId={processingId}
                                    />
                                ))}
                            </div>
                        )}
                    </section>

                    {/* MY INSTITUTION'S SHARED EQUIPMENT */}
                    <section className="sharing-section">
                        <div className="sharing-section-heading">
                            <div>
                                <h2>My Institution's Shared Equipment</h2>
                                <p>
                                    Equipment listings published by your institution.
                                </p>
                            </div>

                            <span className="sharing-count">
                                {mySharings.length} listings
                            </span>
                        </div>

                        {mySharings.length === 0 ? (
                            <div className="sharing-empty">
                                Your institution has not published any equipment for sharing.
                            </div>
                        ) : (
                            <div className="sharing-grid">
                                {mySharings.map((item) => (
                                    <div
                                        className="sharing-card"
                                        key={item.id}
                                    >
                                        <div className="sharing-card-heading">
                                            <div className="sharing-icon">
                                                <Package size={22} />
                                            </div>

                                            <div className="sharing-card-title">
                                                <h3>
                                                    {item.equipmentName || "Equipment"}
                                                </h3>

                                                <p>
                                                    {item.assetTag || "No asset tag"}
                                                </p>
                                            </div>

                                            <StatusBadge status={item.status} />
                                        </div>

                                        <div className="sharing-details">
                                            <div>
                                                <CalendarDays size={16} />
                                                <span>
                                                    {formatDate(item.startDate)} – {formatDate(item.endDate)}
                                                </span>
                                            </div>

                                            <div>
                                                <IndianRupee size={16} />
                                                <span>
                                                    {item.hourlyCost == null
                                                        ? "Fee not specified"
                                                        : `₹${item.hourlyCost} per hour`}
                                                </span>
                                            </div>
                                        </div>

                                        {item.sharingTerms && (
                                            <p className="sharing-terms">
                                                {item.sharingTerms}
                                            </p>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </section>
                </>
            )}

            {/* REQUEST EQUIPMENT MODAL */}
            {selectedEquipment && (
                <div className="sharing-modal-backdrop">
                    <div className="sharing-modal">
                        <h2>Request Equipment</h2>

                        <p className="sharing-modal-equipment">
                            {selectedEquipment.equipmentName}
                        </p>

                        <p className="sharing-modal-owner">
                            Owner: {selectedEquipment.institutionName}
                        </p>

                        <p className="sharing-modal-fee">
                            Hourly fee:{" "}
                            {selectedEquipment.hourlyCost == null
                                ? "Not specified"
                                : `₹${selectedEquipment.hourlyCost}`}
                        </p>

                        <form onSubmit={submitRequest}>
                            <label htmlFor="sharing-purpose">
                                Purpose of request
                            </label>

                            <textarea
                                id="sharing-purpose"
                                value={purpose}
                                onChange={(event) =>
                                    setPurpose(event.target.value)
                                }
                                placeholder="Explain why your institution needs this equipment..."
                                rows={4}
                                required
                            />

                            <div className="sharing-modal-actions">
                                <button
                                    type="button"
                                    className="sharing-cancel-button"
                                    onClick={() => {
                                        setSelectedEquipment(null);
                                        setPurpose("");
                                        setError("");
                                    }}
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    className="sharing-primary-button"
                                    disabled={
                                        requestingId === selectedEquipment.id
                                    }
                                >
                                    <Send size={16} />

                                    {requestingId === selectedEquipment.id
                                        ? "Submitting..."
                                        : "Submit Request"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
