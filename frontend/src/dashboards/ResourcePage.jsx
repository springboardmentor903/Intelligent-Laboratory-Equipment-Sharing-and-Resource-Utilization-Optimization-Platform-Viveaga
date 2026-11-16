
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    FlaskConical,
    CalendarCheck,
    ChartNoAxesCombined,
    ArrowLeft,
    Search,
    RefreshCw,
    CalendarPlus,
    X,
    Check,
    Ban,
    Clock3,
    UserRound,
    Tag,
    Plus,
    Share2,
} from "lucide-react";

import api from "../api";
import DashboardLayout from "./DashboardLayout";

// ==========================================
// PAGE DETAILS
// ==========================================

const pageDetails = {
    equipment: {
        title: "Equipment",
        heading: "Laboratory Equipment",
        description: "Explore equipment and check availability.",
        icon: FlaskConical,
    },
    bookings: {
        title: "Bookings",
        heading: "Booking Management",
        description: "Review booking requests and track their status.",
        icon: CalendarCheck,
    },
    reports: {
        title: "Reports",
        heading: "Resource Reports",
        description: "Laboratory resource usage and booking reports.",
        icon: ChartNoAxesCombined,
    },
};

// ==========================================
// ERROR HANDLING
// ==========================================

function getErrorMessage(error) {
    if (!error.response) {
        return "Cannot connect to the backend. Check that Spring Boot is running.";
    }

    const data = error.response.data;

    if (typeof data === "string") {
        return data;
    }

    return (
        data?.message ||
        data?.error ||
        `Request failed (${error.response.status}).`
    );
}

function isBookingConflictError(error) {
    const status = error.response?.status;

    if (![400, 409].includes(status)) {
        return false;
    }

    const data = error.response?.data;

    const message =
        typeof data === "string"
            ? data
            : data?.message || data?.error || "";

    return /already has a booking|already.*booked|not available for booking|overlap|time slot|conflict|occupied/i.test(
        message
    );
}

// ==========================================
// DATE HELPERS
// ==========================================

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
    });
}

function getTodayLocalDate() {
    const now = new Date();
    const localDate = new Date(
        now.getTime() - now.getTimezoneOffset() * 60000
    );

    return localDate.toISOString().slice(0, 10);
}

// ==========================================
// ROLE FORMATTER
// ==========================================

function formatRole(role) {
    const roles = {
        RESEARCHER_STUDENT: "Student / Researcher",
        LAB_TECHNICIAN: "Lab Technician",
        LAB_MANAGER: "Lab Manager",
        DEPARTMENT_HEAD: "Department Head",
        INSTITUTION_ADMINISTRATOR: "Institution Administrator",
        SYSTEM_ADMINISTRATOR: "System Administrator",
    };

    return roles[role] || role || "User";
}

// ==========================================
// CHECK-IN VALIDATION
// ==========================================

function canCheckIn(booking) {
    if (booking.status !== "APPROVED") {
        return false;
    }

    const startTime = new Date(booking.startTime).getTime();
    const endTime = new Date(booking.endTime).getTime();
    const now = Date.now();

    return now >= startTime - 15 * 60 * 1000 && now <= endTime;
}

// ==========================================
// STATUS BADGE
// ==========================================

function Status({ value }) {
    const status = String(value || "UNKNOWN").toUpperCase();

    const colors = {
        AVAILABLE: {
            text: "#15803d",
            background: "#dcfce7",
        },
        ACTIVE: {
            text: "#15803d",
            background: "#dcfce7",
        },
        INACTIVE: {
            text: "#475569",
            background: "#e2e8f0",
        },
        EXPIRED: {
            text: "#b91c1c",
            background: "#fee2e2",
        },
        PENDING: {
            text: "#b45309",
            background: "#fef3c7",
        },
        APPROVED: {
            text: "#15803d",
            background: "#dcfce7",
        },
        REJECTED: {
            text: "#b91c1c",
            background: "#fee2e2",
        },
        CANCELLED: {
            text: "#475569",
            background: "#e2e8f0",
        },
        IN_USE: {
            text: "#1d4ed8",
            background: "#dbeafe",
        },
        BOOKED: {
            text: "#1d4ed8",
            background: "#dbeafe",
        },
        UNDER_MAINTENANCE: {
            text: "#b91c1c",
            background: "#fee2e2",
        },
    };

    const selected = colors[status] || {
        text: "#475569",
        background: "#f1f5f9",
    };

    return (
        <span
            style={{
                display: "inline-flex",
                alignItems: "center",
                padding: "6px 11px",
                borderRadius: 30,
                background: selected.background,
                color: selected.text,
                fontSize: 11,
                fontWeight: 800,
                letterSpacing: "0.5px",
                whiteSpace: "nowrap",
            }}
        >
            {status.replaceAll("_", " ")}
        </span>
    );
}

// ==========================================
// ADD EQUIPMENT FORM
// ==========================================

function AddEquipmentForm({ onClose, onAdded }) {
    const [categories, setCategories] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [loadingOptions, setLoadingOptions] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    const [form, setForm] = useState({
        name: "Compound Microscope",
        assetTag: "MCET-EQ-002",
        model: "",
        manufacturer: "",
        serialNumber: "",
        location: "",
        hourlyCost: "",
        categoryId: "",
        departmentId: "",
    });

    useEffect(() => {
        let active = true;

        async function loadOptions() {
            setLoadingOptions(true);
            setError("");

            try {
                const [categoryResponse, departmentResponse] =
                    await Promise.all([
                        api.get("/equipment-categories"),
                        api.get("/departments"),
                    ]);

                if (!active) return;

                const categoryData = categoryResponse.data;
                const departmentData = departmentResponse.data;

                setCategories(
                    Array.isArray(categoryData)
                        ? categoryData
                        : categoryData?.content || []
                );

                setDepartments(
                    Array.isArray(departmentData)
                        ? departmentData
                        : departmentData?.content || []
                );
            } catch (err) {
                if (active) {
                    setError(getErrorMessage(err));
                }
            } finally {
                if (active) {
                    setLoadingOptions(false);
                }
            }
        }

        loadOptions();

        return () => {
            active = false;
        };
    }, []);

    const updateField = (event) => {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const submit = async (event) => {
        event.preventDefault();
        setError("");

        if (!form.name.trim() || !form.assetTag.trim()) {
            setError("Equipment name and asset tag are required.");
            return;
        }

        if (!form.categoryId || !form.departmentId) {
            setError("Please select a category and department.");
            return;
        }

        if (
            form.hourlyCost !== "" &&
            (!Number.isFinite(Number(form.hourlyCost)) ||
                Number(form.hourlyCost) < 0)
        ) {
            setError("Hourly cost must be a valid non-negative number.");
            return;
        }

        setSaving(true);

        try {
            const payload = {
                name: form.name.trim(),
                assetTag: form.assetTag.trim(),
                model: form.model.trim() || null,
                manufacturer: form.manufacturer.trim() || null,
                serialNumber: form.serialNumber.trim() || null,
                location: form.location.trim() || null,
                hourlyCost:
                    form.hourlyCost === ""
                        ? null
                        : Number(form.hourlyCost),
                category: {
                    id: Number(form.categoryId),
                },
                department: {
                    id: Number(form.departmentId),
                },
            };

            await api.post("/equipment", payload);

            await onAdded();
            onClose();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setSaving(false);
        }
    };

    return (
        <section
            style={{
                ...modernCardStyle,
                marginBottom: 24,
            }}
        >
            <div style={spaceBetweenStyle}>
                <div>
                    <h3 style={{ margin: "0 0 8px", color: "#172554" }}>
                        Add Laboratory Equipment
                    </h3>

                    <p style={pageDescriptionStyle}>
                        Register a new piece of equipment in the inventory.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={onClose}
                    style={secondaryButtonStyle}
                >
                    <X size={17} />
                    Close
                </button>
            </div>

            {loadingOptions ? (
                <p style={infoTextStyle}>
                    Loading categories and departments...
                </p>
            ) : (
                <form onSubmit={submit} style={formStyle}>
                    <div style={equipmentDetailsStyle}>
                        <label style={labelStyle}>
                            Equipment name *
                            <input
                                name="name"
                                required
                                value={form.name}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>

                        <label style={labelStyle}>
                            Asset tag *
                            <input
                                name="assetTag"
                                required
                                value={form.assetTag}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>

                        <label style={labelStyle}>
                            Category *
                            <select
                                name="categoryId"
                                required
                                value={form.categoryId}
                                onChange={updateField}
                                style={inputStyle}
                            >
                                <option value="">Select category</option>

                                {categories.map((category) => (
                                    <option
                                        key={category.id}
                                        value={category.id}
                                    >
                                        {category.name}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label style={labelStyle}>
                            Department *
                            <select
                                name="departmentId"
                                required
                                value={form.departmentId}
                                onChange={updateField}
                                style={inputStyle}
                            >
                                <option value="">Select department</option>

                                {departments.map((department) => (
                                    <option
                                        key={department.id}
                                        value={department.id}
                                    >
                                        {department.name}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label style={labelStyle}>
                            Model
                            <input
                                name="model"
                                value={form.model}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>

                        <label style={labelStyle}>
                            Manufacturer
                            <input
                                name="manufacturer"
                                value={form.manufacturer}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>

                        <label style={labelStyle}>
                            Serial number
                            <input
                                name="serialNumber"
                                value={form.serialNumber}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>

                        <label style={labelStyle}>
                            Location
                            <input
                                name="location"
                                value={form.location}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>

                        <label style={labelStyle}>
                            Hourly cost (₹)
                            <input
                                name="hourlyCost"
                                type="number"
                                min="0"
                                step="0.01"
                                value={form.hourlyCost}
                                onChange={updateField}
                                style={inputStyle}
                            />
                        </label>
                    </div>

                    {categories.length === 0 && (
                        <p style={errorMessageStyle}>
                            No equipment categories were found. Create a
                            category before adding equipment.
                        </p>
                    )}

                    {departments.length === 0 && (
                        <p style={errorMessageStyle}>
                            No departments were found. Create a department
                            before adding equipment.
                        </p>
                    )}

                    {error && (
                        <p role="alert" style={errorMessageStyle}>
                            {error}
                        </p>
                    )}

                    <p style={mutedStyle}>
                        The backend sets the new equipment status to AVAILABLE.
                    </p>

                    <div style={actionRowStyle}>
                        <button
                            type="submit"
                            disabled={
                                saving ||
                                loadingOptions ||
                                categories.length === 0 ||
                                departments.length === 0
                            }
                            style={primaryButtonStyle}
                        >
                            <Check size={17} />
                            {saving ? "Adding..." : "Add Equipment"}
                        </button>

                        <button
                            type="button"
                            onClick={onClose}
                            style={secondaryButtonStyle}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}
        </section>
    );
}

// ==========================================
// EQUIPMENT CARD
// ==========================================

function EquipmentCard({
                           equipment,
                           onBook,
                           onJoinWaitlist,
                           allowWaitlist,
                           canPublishSharing,
                           onPublishSharing,
                       }) {
    const [showForm, setShowForm] = useState(false);
    const [startTime, setStartTime] = useState("");
    const [endTime, setEndTime] = useState("");
    const [purpose, setPurpose] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const [isRecurring, setIsRecurring] = useState(false);
    const [recurrenceType, setRecurrenceType] = useState("WEEKLY");
    const [recurrenceCount, setRecurrenceCount] = useState(2);

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [canJoinWaitlist, setCanJoinWaitlist] = useState(false);

    // Sharing form state
    const [showSharingForm, setShowSharingForm] = useState(false);
    const [sharingStartDate, setSharingStartDate] = useState("");
    const [sharingEndDate, setSharingEndDate] = useState("");
    const [sharingTerms, setSharingTerms] = useState("");
    const [sharingHourlyCost, setSharingHourlyCost] = useState("");
    const [sharingSubmitting, setSharingSubmitting] = useState(false);

    const unavailable = equipment.status !== "AVAILABLE";

    // ==========================================
    // NORMAL / RECURRING BOOKING
    // ==========================================

    const submitBooking = async (event) => {
        event.preventDefault();

        setMessage("");
        setError("");
        setCanJoinWaitlist(false);

        if (!purpose.trim()) {
            setError("Please enter the purpose of your booking.");
            return;
        }

        if (!startTime || !endTime) {
            setError("Please select both start and end times.");
            return;
        }

        if (new Date(startTime) <= new Date()) {
            setError("Start time must be in the future.");
            return;
        }

        if (new Date(endTime) <= new Date(startTime)) {
            setError("End time must be after the start time.");
            return;
        }

        if (
            isRecurring &&
            (
                !Number.isInteger(Number(recurrenceCount)) ||
                Number(recurrenceCount) < 2 ||
                Number(recurrenceCount) > 30
            )
        ) {
            setError("Choose between 2 and 30 occurrences.");
            return;
        }

        setSubmitting(true);

        try {
            await onBook({
                equipmentId: equipment.id,
                startTime,
                endTime,
                purpose: purpose.trim(),
                recurring: isRecurring,
                recurrenceType: isRecurring ? recurrenceType : null,
                recurrenceCount: isRecurring
                    ? Number(recurrenceCount)
                    : null,
            });

            setMessage(
                isRecurring
                    ? "Recurring booking series submitted successfully."
                    : "Booking request submitted successfully."
            );

            setStartTime("");
            setEndTime("");
            setPurpose("");
            setIsRecurring(false);
            setRecurrenceType("WEEKLY");
            setRecurrenceCount(2);
            setShowForm(false);
        } catch (err) {
            setError(getErrorMessage(err));

            if (allowWaitlist && isBookingConflictError(err)) {
                setCanJoinWaitlist(true);
            }
        } finally {
            setSubmitting(false);
        }
    };

    // ==========================================
    // WAITLIST
    // ==========================================

    const submitWaitlist = async () => {
        setError("");
        setMessage("");

        if (!startTime || !endTime || !purpose.trim()) {
            setError("Please complete all booking details first.");
            return;
        }

        if (new Date(startTime) <= new Date()) {
            setError("Start time must be in the future.");
            return;
        }

        if (new Date(endTime) <= new Date(startTime)) {
            setError("End time must be after the start time.");
            return;
        }

        setSubmitting(true);

        try {
            await onJoinWaitlist({
                equipmentId: equipment.id,
                startTime,
                endTime,
                purpose: purpose.trim(),
            });

            setMessage(
                "Waitlist request submitted successfully. Check My Waitlist for updates."
            );

            setCanJoinWaitlist(false);
            setShowForm(false);
            setStartTime("");
            setEndTime("");
            setPurpose("");
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setSubmitting(false);
        }
    };

    // ==========================================
    // PUBLISH EQUIPMENT SHARING
    // ==========================================

    const submitSharing = async (event) => {
        event.preventDefault();

        setError("");
        setMessage("");

        if (!sharingStartDate || !sharingEndDate) {
            setError("Please select both sharing dates.");
            return;
        }

        if (sharingStartDate < getTodayLocalDate()) {
            setError("Sharing start date cannot be in the past.");
            return;
        }

        if (sharingEndDate <= sharingStartDate) {
            setError("Sharing end date must be after start date.");
            return;
        }

        if (
            sharingHourlyCost !== "" &&
            (!Number.isFinite(Number(sharingHourlyCost)) ||
                Number(sharingHourlyCost) < 0)
        ) {
            setError("Hourly cost cannot be negative.");
            return;
        }

        setSharingSubmitting(true);

        try {
            await onPublishSharing({
                equipmentId: equipment.id,
                startDate: sharingStartDate,
                endDate: sharingEndDate,
                sharingTerms: sharingTerms.trim(),
                hourlyCost:
                    sharingHourlyCost === ""
                        ? null
                        : Number(sharingHourlyCost),
            });

            setMessage(
                "Equipment published for institutional sharing successfully."
            );

            setSharingStartDate("");
            setSharingEndDate("");
            setSharingTerms("");
            setSharingHourlyCost("");
            setShowSharingForm(false);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setSharingSubmitting(false);
        }
    };

    // ==========================================
    // CARD UI
    // ==========================================

    return (
        <article style={modernCardStyle}>
            <div style={spaceBetweenStyle}>
                <div style={{ minWidth: 0 }}>
                    <div style={iconTileStyle}>
                        <FlaskConical size={22} />
                    </div>

                    <h3 style={equipmentTitleStyle}>
                        {equipment.name}
                    </h3>

                    <p style={mutedStyle}>
                        {equipment.assetTag || "No asset tag"}
                    </p>
                </div>

                <Status value={equipment.status} />
            </div>

            <div style={equipmentDetailsStyle}>
                <div>
                    <span style={detailLabelStyle}>MODEL</span>
                    <strong>{equipment.model || "—"}</strong>
                </div>

                <div>
                    <span style={detailLabelStyle}>MANUFACTURER</span>
                    <strong>{equipment.manufacturer || "—"}</strong>
                </div>

                <div>
                    <span style={detailLabelStyle}>CATEGORY</span>
                    <strong>{equipment.categoryName || "—"}</strong>
                </div>

                <div>
                    <span style={detailLabelStyle}>DEPARTMENT</span>
                    <strong>{equipment.departmentName || "—"}</strong>
                </div>

                <div>
                    <span style={detailLabelStyle}>HOURLY COST</span>
                    <strong>
                        {equipment.hourlyCost == null
                            ? "Not specified"
                            : `₹${equipment.hourlyCost}`}
                    </strong>
                </div>
            </div>

            {message && (
                <p role="status" style={successMessageStyle}>
                    {message}
                </p>
            )}

            {error && (
                <p role="alert" style={errorMessageStyle}>
                    {error}
                </p>
            )}

            {/* BOOKING BUTTON AND FORM */}

            {!showForm ? (
                <button
                    type="button"
                    onClick={() => {
                        setError("");
                        setMessage("");
                        setCanJoinWaitlist(false);
                        setShowForm(true);
                    }}
                    style={{
                        ...primaryButtonStyle,
                        width: "100%",
                        marginTop: 20,
                    }}
                >
                    <CalendarPlus size={17} />

                    {unavailable
                        ? "Check Slot / Join Waitlist"
                        : "Book Equipment"}
                </button>
            ) : (
                <form onSubmit={submitBooking} style={formStyle}>
                    <h4 style={{ margin: 0, color: "#172554" }}>
                        Request Equipment
                    </h4>

                    <label style={labelStyle}>
                        Start time

                        <input
                            type="datetime-local"
                            required
                            value={startTime}
                            onChange={(event) => {
                                setStartTime(event.target.value);
                                setCanJoinWaitlist(false);
                                setError("");
                            }}
                            style={inputStyle}
                        />
                    </label>

                    <label style={labelStyle}>
                        End time

                        <input
                            type="datetime-local"
                            required
                            value={endTime}
                            onChange={(event) => {
                                setEndTime(event.target.value);
                                setCanJoinWaitlist(false);
                                setError("");
                            }}
                            style={inputStyle}
                        />
                    </label>

                    <label style={labelStyle}>
                        Purpose

                        <textarea
                            required
                            minLength={3}
                            maxLength={500}
                            value={purpose}
                            onChange={(event) => {
                                setPurpose(event.target.value);
                                setError("");
                            }}
                            placeholder="Explain why you need this equipment"
                            style={{
                                ...inputStyle,
                                minHeight: 90,
                                resize: "vertical",
                            }}
                        />
                    </label>

                    {/* RECURRING BOOKING OPTIONS */}

                    <div
                        style={{
                            padding: 15,
                            background: "#f8fafc",
                            border: "1px solid #e2e8f0",
                            borderRadius: 12,
                            display: "grid",
                            gap: 14,
                        }}
                    >
                        <label
                            style={{
                                display: "flex",
                                alignItems: "center",
                                gap: 10,
                                cursor: "pointer",
                                color: "#334155",
                                fontSize: 14,
                                fontWeight: 650,
                            }}
                        >
                            <input
                                type="checkbox"
                                checked={isRecurring}
                                onChange={(event) => {
                                    setIsRecurring(event.target.checked);
                                    setError("");
                                }}
                            />

                            Make this a recurring booking
                        </label>

                        {isRecurring && (
                            <>
                                <label style={labelStyle}>
                                    Repeat

                                    <select
                                        value={recurrenceType}
                                        onChange={(event) =>
                                            setRecurrenceType(event.target.value)
                                        }
                                        style={inputStyle}
                                    >
                                        <option value="DAILY">Daily</option>
                                        <option value="WEEKLY">Weekly</option>
                                        <option value="MONTHLY">Monthly</option>
                                    </select>
                                </label>

                                <label style={labelStyle}>
                                    Number of occurrences

                                    <input
                                        type="number"
                                        min="2"
                                        max="30"
                                        step="1"
                                        required
                                        value={recurrenceCount}
                                        onChange={(event) =>
                                            setRecurrenceCount(event.target.value)
                                        }
                                        style={inputStyle}
                                    />
                                </label>

                                <p style={mutedStyle}>
                                    The total includes your first booking.
                                    All occurrences are submitted as one series.
                                </p>
                            </>
                        )}
                    </div>

                    {/* BOOKING FORM ACTIONS */}

                    <div style={actionRowStyle}>
                        <button
                            type="submit"
                            disabled={submitting}
                            style={primaryButtonStyle}
                        >
                            <Check size={17} />

                            {submitting
                                ? "Submitting..."
                                : isRecurring
                                    ? "Submit Recurring Series"
                                    : "Submit Request"}
                        </button>

                        {canJoinWaitlist && allowWaitlist && (
                            <button
                                type="button"
                                disabled={submitting}
                                onClick={submitWaitlist}
                                style={{
                                    ...secondaryButtonStyle,
                                    borderColor: "#fcd34d",
                                    background: "#fffbeb",
                                    color: "#92400e",
                                }}
                            >
                                <Clock3 size={17} />
                                {submitting ? "Joining..." : "Join Waitlist"}
                            </button>
                        )}

                        <button
                            type="button"
                            onClick={() => {
                                setShowForm(false);
                                setError("");
                                setCanJoinWaitlist(false);
                            }}
                            style={secondaryButtonStyle}
                        >
                            <X size={17} />
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            {/* SHARING BUTTON AND FORM */}

            {canPublishSharing && (
                <>
                    <button
                        type="button"
                        onClick={() => {
                            setError("");
                            setMessage("");
                            setShowSharingForm((previous) => !previous);
                        }}
                        style={{
                            ...secondaryButtonStyle,
                            width: "100%",
                            marginTop: 12,
                            borderColor: "#86bfa5",
                            color: "#166534",
                        }}
                    >
                        <Share2 size={17} />
                        {showSharingForm
                            ? "Close Sharing Form"
                            : "Share Equipment"}
                    </button>

                    {showSharingForm && (
                        <form onSubmit={submitSharing} style={formStyle}>
                            <h4 style={{ margin: 0, color: "#166534" }}>
                                Publish Equipment for Sharing
                            </h4>

                            <p style={mutedStyle}>
                                {equipment.name} ·{" "}
                                {equipment.assetTag || "No asset tag"}
                            </p>

                            <label style={labelStyle}>
                                Sharing start date

                                <input
                                    type="date"
                                    required
                                    min={getTodayLocalDate()}
                                    value={sharingStartDate}
                                    onChange={(event) =>
                                        setSharingStartDate(event.target.value)
                                    }
                                    style={inputStyle}
                                />
                            </label>

                            <label style={labelStyle}>
                                Sharing end date

                                <input
                                    type="date"
                                    required
                                    min={
                                        sharingStartDate || getTodayLocalDate()
                                    }
                                    value={sharingEndDate}
                                    onChange={(event) =>
                                        setSharingEndDate(event.target.value)
                                    }
                                    style={inputStyle}
                                />
                            </label>

                            <label style={labelStyle}>
                                Hourly cost (₹)

                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={sharingHourlyCost}
                                    onChange={(event) =>
                                        setSharingHourlyCost(event.target.value)
                                    }
                                    placeholder="Example: 100"
                                    style={inputStyle}
                                />
                            </label>

                            <label style={labelStyle}>
                                Sharing terms

                                <textarea
                                    value={sharingTerms}
                                    onChange={(event) =>
                                        setSharingTerms(event.target.value)
                                    }
                                    maxLength={1000}
                                    placeholder="Enter availability conditions and sharing rules"
                                    style={{
                                        ...inputStyle,
                                        minHeight: 90,
                                        resize: "vertical",
                                    }}
                                />
                            </label>

                            <div style={actionRowStyle}>
                                <button
                                    type="submit"
                                    disabled={sharingSubmitting}
                                    style={primaryButtonStyle}
                                >
                                    <Share2 size={17} />

                                    {sharingSubmitting
                                        ? "Publishing..."
                                        : "Publish Sharing"}
                                </button>

                                <button
                                    type="button"
                                    disabled={sharingSubmitting}
                                    onClick={() => setShowSharingForm(false)}
                                    style={secondaryButtonStyle}
                                >
                                    <X size={17} />
                                    Cancel
                                </button>
                            </div>
                        </form>
                    )}
                </>
            )}
        </article>
    );
}

// ==========================================
// REQUEST ACCESS CARD
// ==========================================

function RequestAccessCard({ listing, onRequestAccess }) {
    const [showForm, setShowForm] = useState(false);
    const [startTime, setStartTime] = useState("");
    const [endTime, setEndTime] = useState("");
    const [purpose, setPurpose] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const startDate = listing.startDate || "";
    const endDate = listing.endDate || "";

    const submitRequest = async (event) => {
        event.preventDefault();

        setError("");
        setSuccess("");

        if (!startTime || !endTime || !purpose.trim()) {
            setError("Please complete all booking details.");
            return;
        }

        if (new Date(startTime) <= new Date()) {
            setError("Booking start time must be in the future.");
            return;
        }

        if (new Date(endTime) <= new Date(startTime)) {
            setError("End time must be after start time.");
            return;
        }

        const requestedStartDate = startTime.slice(0, 10);
        const requestedEndDate = endTime.slice(0, 10);

        if (
            requestedStartDate < startDate ||
            requestedStartDate > endDate ||
            requestedEndDate < startDate ||
            requestedEndDate > endDate
        ) {
            setError(
                "Your booking dates must fall within the sharing agreement period."
            );
            return;
        }

        if (purpose.trim().length > 500) {
            setError("Purpose cannot exceed 500 characters.");
            return;
        }

        setSubmitting(true);

        try {
            await onRequestAccess({
                equipmentId:
                    listing.equipmentId ||
                    listing.equipment?.id,
                sharingId: listing.id,
                startTime,
                endTime,
                purpose: purpose.trim(),
            });

            setSuccess(
                "Access request submitted successfully. Waiting for approval from the owning institution."
            );

            setStartTime("");
            setEndTime("");
            setPurpose("");
            setShowForm(false);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div style={{ marginTop: 18 }}>
            {success && (
                <p role="status" style={successMessageStyle}>
                    {success}
                </p>
            )}

            {!showForm ? (
                <button
                    type="button"
                    onClick={() => {
                        setError("");
                        setSuccess("");
                        setShowForm(true);
                    }}
                    style={{
                        ...primaryButtonStyle,
                        width: "100%",
                    }}
                >
                    <CalendarPlus size={17} />
                    Request Access
                </button>
            ) : (
                <form onSubmit={submitRequest} style={formStyle}>
                    <h4 style={{ margin: 0, color: "#172554" }}>
                        Request External Equipment Access
                    </h4>

                    <p style={mutedStyle}>
                        Select your required booking time within the sharing
                        agreement period: {startDate} to {endDate}.
                    </p>

                    <label style={labelStyle}>
                        Booking start time *

                        <input
                            type="datetime-local"
                            required
                            min={`${startDate}T00:00`}
                            max={`${endDate}T23:59`}
                            value={startTime}
                            onChange={(event) => {
                                setStartTime(event.target.value);
                                setError("");
                            }}
                            style={inputStyle}
                        />
                    </label>

                    <label style={labelStyle}>
                        Booking end time *

                        <input
                            type="datetime-local"
                            required
                            min={`${startDate}T00:00`}
                            max={`${endDate}T23:59`}
                            value={endTime}
                            onChange={(event) => {
                                setEndTime(event.target.value);
                                setError("");
                            }}
                            style={inputStyle}
                        />
                    </label>

                    <label style={labelStyle}>
                        Purpose of access *

                        <textarea
                            required
                            minLength={3}
                            maxLength={500}
                            value={purpose}
                            onChange={(event) => {
                                setPurpose(event.target.value);
                                setError("");
                            }}
                            placeholder="Explain why you need this equipment"
                            style={{
                                ...inputStyle,
                                minHeight: 90,
                                resize: "vertical",
                            }}
                        />
                    </label>

                    {error && (
                        <p role="alert" style={errorMessageStyle}>
                            {error}
                        </p>
                    )}

                    <p style={mutedStyle}>
                        Your request will be sent to the owning institution's
                        Lab Manager for approval.
                    </p>

                    <div style={actionRowStyle}>
                        <button
                            type="submit"
                            disabled={submitting}
                            style={primaryButtonStyle}
                        >
                            <Check size={17} />
                            {submitting
                                ? "Submitting..."
                                : "Submit Access Request"}
                        </button>

                        <button
                            type="button"
                            disabled={submitting}
                            onClick={() => {
                                setShowForm(false);
                                setError("");
                            }}
                            style={secondaryButtonStyle}
                        >
                            <X size={17} />
                            Cancel
                        </button>
                    </div>
                </form>
            )}
        </div>
    );
}

// ==========================================
// BOOKING CARD
// ==========================================

function BookingCard({
                         booking,
                         isManager,
                         onCancel,
                         onDecision,
                         onSeriesDecision,
                         onCheckIn,
                         onCheckOut,
                         busy,
                         showSeriesActions,
                     }) {
    const isRecurring = Boolean(booking.recurringSeriesId);

    return (
        <article style={modernCardStyle}>
            <div style={spaceBetweenStyle}>
                <div style={{ minWidth: 0 }}>
                    <h3 style={bookingTitleStyle}>
                        {booking.equipmentName || "Equipment"}
                    </h3>

                    <p style={mutedStyle}>
                        Booking #{booking.id}
                    </p>

                    {isRecurring && (
                        <p style={{ ...mutedStyle, color: "#1d4ed8" }}>
                            <CalendarCheck
                                size={14}
                                style={{ verticalAlign: "middle" }}
                            />{" "}
                            Recurring booking series
                        </p>
                    )}

                    <p style={mutedStyle}>
                        <Tag
                            size={14}
                            style={{ verticalAlign: "middle" }}
                        />{" "}
                        {booking.assetTag || "No asset tag"}
                    </p>
                </div>

                <Status value={booking.status} />
            </div>

            <div style={bookingDetailsGridStyle}>
                <div style={detailBoxStyle}>
                    <span style={detailLabelStyle}>
                        <UserRound size={13} /> REQUESTED BY
                    </span>

                    <strong>
                        {booking.userName ||
                            `User #${booking.userId ?? "—"}`}
                    </strong>

                    {booking.userRole && (
                        <span style={rolePillStyle}>
                            {formatRole(booking.userRole)}
                        </span>
                    )}
                </div>

                <div style={detailBoxStyle}>
                    <span style={detailLabelStyle}>
                        <Clock3 size={13} /> START TIME
                    </span>

                    <strong>{formatDate(booking.startTime)}</strong>
                </div>

                <div style={detailBoxStyle}>
                    <span style={detailLabelStyle}>
                        <Clock3 size={13} /> END TIME
                    </span>

                    <strong>{formatDate(booking.endTime)}</strong>
                </div>

                <div style={detailBoxStyle}>
                    <span style={detailLabelStyle}>PURPOSE</span>
                    <strong>{booking.purpose || "—"}</strong>
                </div>
            </div>

            {/* MANAGER APPROVAL ACTIONS */}

            {isManager &&
                booking.status === "PENDING" &&
                !isRecurring && (
                    <div style={actionRowStyle}>
                        <button
                            type="button"
                            disabled={busy}
                            onClick={() =>
                                onDecision(booking.id, "approve")
                            }
                            style={primaryButtonStyle}
                        >
                            <Check size={17} />
                            {busy ? "Please wait..." : "Approve"}
                        </button>

                        <button
                            type="button"
                            disabled={busy}
                            onClick={() =>
                                onDecision(booking.id, "reject")
                            }
                            style={rejectButtonStyle}
                        >
                            <Ban size={17} />
                            Reject
                        </button>
                    </div>
                )}

            {/* RECURRING SERIES APPROVAL */}

            {isManager &&
                isRecurring &&
                booking.status === "PENDING" &&
                showSeriesActions && (
                    <div style={actionRowStyle}>
                        <button
                            type="button"
                            disabled={busy}
                            onClick={() =>
                                onSeriesDecision(
                                    booking.recurringSeriesId,
                                    "approve"
                                )
                            }
                            style={primaryButtonStyle}
                        >
                            <Check size={17} />
                            {busy ? "Please wait..." : "Approve Series"}
                        </button>

                        <button
                            type="button"
                            disabled={busy}
                            onClick={() =>
                                onSeriesDecision(
                                    booking.recurringSeriesId,
                                    "reject"
                                )
                            }
                            style={rejectButtonStyle}
                        >
                            <Ban size={17} />
                            Reject Series
                        </button>
                    </div>
                )}

            {/* CANCEL PENDING BOOKING */}

            {!isManager &&
                booking.status === "PENDING" &&
                !isRecurring && (
                    <div style={actionRowStyle}>
                        <button
                            type="button"
                            disabled={busy}
                            onClick={() => onCancel(booking.id)}
                            style={secondaryButtonStyle}
                        >
                            <X size={17} />
                            Cancel Booking
                        </button>
                    </div>
                )}

            {/* CHECK IN */}

            {!isManager && canCheckIn(booking) && (
                <div style={actionRowStyle}>
                    <button
                        type="button"
                        disabled={busy}
                        onClick={() => onCheckIn(booking.id)}
                        style={primaryButtonStyle}
                    >
                        <Check size={17} />
                        {busy ? "Please wait..." : "Check In"}
                    </button>
                </div>
            )}

            {/* CHECK OUT */}

            {!isManager && booking.status === "IN_USE" && (
                <div style={actionRowStyle}>
                    <button
                        type="button"
                        disabled={busy}
                        onClick={() => onCheckOut(booking.id)}
                        style={primaryButtonStyle}
                    >
                        <Check size={17} />
                        {busy ? "Please wait..." : "Check Out"}
                    </button>
                </div>
            )}
        </article>
    );
}

// ==========================================
// EMPTY STATE
// ==========================================

function EmptyState({ icon: Icon, title, description }) {
    return (
        <div style={emptyStateStyle}>
            <div style={emptyIconStyle}>
                <Icon size={28} />
            </div>

            <h3 style={{ margin: "14px 0 8px", color: "#172554" }}>
                {title}
            </h3>

            <p
                style={{
                    margin: 0,
                    color: "#64748b",
                    lineHeight: 1.6,
                }}
            >
                {description}
            </p>
        </div>
    );
}

// ==========================================
// MAIN RESOURCE PAGE
// ==========================================

export default function ResourcePage({ page }) {
    const navigate = useNavigate();
    const details = pageDetails[page];

    const role = sessionStorage.getItem("userRole");

    const isManager = role === "LAB_MANAGER";

    const canPublishSharing = [
        "LAB_MANAGER",
        "DEPARTMENT_HEAD",
        "INSTITUTION_ADMINISTRATOR",
    ].includes(role);

    const allowWaitlist = [
        "RESEARCHER_STUDENT",
        "DEPARTMENT_HEAD",
    ].includes(role);

    const [equipment, setEquipment] = useState([]);
    const [bookings, setBookings] = useState([]);
    const [sharingListings, setSharingListings] = useState([]);
    const [availableSharings, setAvailableSharings] = useState([]);

    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");

    const [loading, setLoading] = useState(false);
    const [busyBookingId, setBusyBookingId] = useState(null);

    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");

    const [showAddEquipment, setShowAddEquipment] = useState(false);

    // ==========================================
    // LOAD EQUIPMENT
    // ==========================================

    const loadEquipment = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const params = {
                page: 0,
                size: 50,
            };

            if (keyword.trim()) {
                params.keyword = keyword.trim();
            }

            if (status) {
                params.status = status;
            }

            const response = await api.get("/equipment", {
                params,
            });

            const data = response.data;

            setEquipment(
                Array.isArray(data)
                    ? data
                    : data?.content || []
            );
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setLoading(false);
        }
    }, [keyword, status]);

    // ==========================================
    // LOAD MY INSTITUTION SHARINGS
    // ==========================================

    const loadMySharings = useCallback(async () => {
        try {
            const response = await api.get("/sharing/my-institution");

            setSharingListings(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );
        } catch (err) {
            setError(getErrorMessage(err));
        }
    }, []);

    // ==========================================
    // LOAD OTHER INSTITUTIONS' SHARINGS
    // ==========================================

    const loadAvailableSharings = useCallback(async () => {
        try {
            const response = await api.get("/sharing/available");

            setAvailableSharings(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );
        } catch (err) {
            setError(getErrorMessage(err));
        }
    }, []);

    // ==========================================
    // PUBLISH SHARING
    // ==========================================

    const publishEquipmentSharing = async (request) => {
        setError("");
        setNotice("");

        try {
            const response = await api.post(
                "/sharing/publish",
                request
            );

            setNotice(
                "Equipment published for sharing successfully."
            );

            await Promise.all([
                loadMySharings(),
                loadAvailableSharings(),
            ]);

            return response.data;
        } catch (err) {
            setError(getErrorMessage(err));
            throw err;
        }
    };

    // ==========================================
    // REQUEST ACCESS TO EXTERNAL EQUIPMENT
    // ==========================================

    const requestSharedEquipmentAccess = async (request) => {
        setError("");
        setNotice("");

        try {
            const response = await api.post("/bookings", {
                equipmentId: request.equipmentId,
                sharingId: request.sharingId,
                startTime: request.startTime,
                endTime: request.endTime,
                purpose: request.purpose,
                usageMode: "EXCLUSIVE",
                recurring: false,
            });

            setNotice(
                "Access request submitted successfully. It is waiting for approval from the equipment-owning institution."
            );

            return response.data;
        } catch (err) {
            setError(getErrorMessage(err));
            throw err;
        }
    };

    // ==========================================
    // LOAD BOOKINGS
    // ==========================================

    const loadBookings = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const endpoint = isManager
                ? "/bookings/all"
                : "/bookings/my";

            const response = await api.get(endpoint);

            setBookings(
                Array.isArray(response.data)
                    ? response.data
                    : []
            );
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setLoading(false);
        }
    }, [isManager]);

    // ==========================================
    // INITIAL DATA LOAD
    // ==========================================

    useEffect(() => {
        if (page === "equipment") {
            loadEquipment();
            loadAvailableSharings();

            if (canPublishSharing) {
                loadMySharings();
            }
        }

        if (page === "bookings") {
            loadBookings();
        }
    }, [
        page,
        loadEquipment,
        loadBookings,
        canPublishSharing,
        loadMySharings,
        loadAvailableSharings,
    ]);

    // ==========================================
    // NORMAL BOOKING
    // ==========================================

    const createBooking = async (request) => {
        setError("");
        setNotice("");

        const payload = {
            equipmentId: request.equipmentId,
            startTime: request.startTime,
            endTime: request.endTime,
            purpose: request.purpose,
            recurring: request.recurring,
            recurrenceType: request.recurrenceType,
            recurrenceCount: request.recurrenceCount,
        };

        const response = await api.post("/bookings", payload);

        setNotice(
            request.recurring
                ? "Recurring booking series submitted successfully."
                : "Booking request submitted successfully."
        );

        return response.data;
    };

    // ==========================================
    // JOIN WAITLIST
    // ==========================================

    const joinWaitlist = async (request) => {
        setError("");
        setNotice("");

        const payload = {
            equipmentId: request.equipmentId,
            requestedStartTime: request.startTime,
            requestedEndTime: request.endTime,
            purpose: request.purpose,
        };

        const response = await api.post("/waitlist", payload);

        setNotice(
            "Waitlist request submitted successfully. Check My Waitlist for updates."
        );

        return response.data;
    };

    // ==========================================
    // CANCEL BOOKING
    // ==========================================

    const cancelBooking = async (bookingId) => {
        const confirmed = window.confirm(
            "Are you sure you want to cancel this booking?"
        );

        if (!confirmed) return;

        setNotice("");
        setError("");

        try {
            await api.patch(`/bookings/${bookingId}/cancel`);

            setNotice("Booking cancelled successfully.");

            await loadBookings();
        } catch (err) {
            setError(getErrorMessage(err));
        }
    };

    // ==========================================
    // APPROVE / REJECT BOOKING
    // ==========================================

    const decideBooking = async (bookingId, decision) => {
        const action =
            decision === "approve" ? "approve" : "reject";

        const confirmed = window.confirm(
            `Are you sure you want to ${action} booking #${bookingId}?`
        );

        if (!confirmed) return;

        setBusyBookingId(bookingId);
        setError("");
        setNotice("");

        try {
            await api.patch(
                `/bookings/${bookingId}/${action}`
            );

            setNotice(
                `Booking #${bookingId} ${
                    decision === "approve"
                        ? "approved"
                        : "rejected"
                } successfully.`
            );

            await loadBookings();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setBusyBookingId(null);
        }
    };

    // ==========================================
    // RECURRING SERIES APPROVAL
    // ==========================================

    const decideRecurringSeries = async (seriesId, decision) => {
        const action =
            decision === "approve" ? "approve" : "reject";

        const confirmed = window.confirm(
            `Are you sure you want to ${action} the entire recurring booking series?`
        );

        if (!confirmed) return;

        const busyKey = `series:${seriesId}`;

        setBusyBookingId(busyKey);
        setError("");
        setNotice("");

        try {
            await api.patch(
                `/bookings/series/${seriesId}/${action}`
            );

            setNotice(
                `Recurring booking series ${
                    decision === "approve" ? "approved" : "rejected"
                } successfully.`
            );

            await loadBookings();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setBusyBookingId(null);
        }
    };

    // ==========================================
    // CHECK IN / CHECK OUT
    // ==========================================

    const updateUsage = async (bookingId, action) => {
        setBusyBookingId(bookingId);
        setError("");
        setNotice("");

        try {
            await api.post(
                `/bookings/${bookingId}/${action}`
            );

            setNotice(
                action === "check-in"
                    ? `Booking #${bookingId} checked in successfully.`
                    : `Booking #${bookingId} checked out successfully.`
            );

            await loadBookings();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setBusyBookingId(null);
        }
    };

    // ==========================================
    // PAGE VALIDATION
    // ==========================================

    if (!details) {
        return <p>Page not found.</p>;
    }

    const PageIcon = details.icon;

    // ==========================================
    // MAIN UI
    // ==========================================

    return (
        <DashboardLayout title={details.title}>
            <main style={pageContainerStyle}>
                {/* PAGE HEADER */}

                <section style={pageHeaderStyle}>
                    <div style={pageHeaderIconStyle}>
                        <PageIcon size={26} />
                    </div>

                    <div style={{ minWidth: 0 }}>
                        <h2 style={pageHeadingStyle}>
                            {page === "bookings" && !isManager
                                ? "My Bookings"
                                : details.heading}
                        </h2>

                        <p style={pageDescriptionStyle}>
                            {page === "bookings"
                                ? isManager
                                    ? "Review student booking requests and approve or reject them."
                                    : "View your bookings and track request status."
                                : details.description}
                        </p>
                    </div>
                </section>

                {/* SUCCESS NOTICE */}

                {notice && (
                    <div role="status" style={successBannerStyle}>
                        <Check size={18} />
                        {notice}
                    </div>
                )}

                {/* ERROR NOTICE */}

                {error && (
                    <div role="alert" style={errorBannerStyle}>
                        <Ban size={18} />
                        {error}
                    </div>
                )}

                {/* ==================================
                    EQUIPMENT PAGE
                ================================== */}

                {page === "equipment" && (
                    <>
                        {/* ADD EQUIPMENT */}

                        {isManager && !showAddEquipment && (
                            <div style={{ marginBottom: 18 }}>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setError("");
                                        setNotice("");
                                        setShowAddEquipment(true);
                                    }}
                                    style={primaryButtonStyle}
                                >
                                    <Plus size={17} />
                                    Add Equipment
                                </button>
                            </div>
                        )}

                        {isManager && showAddEquipment && (
                            <AddEquipmentForm
                                onClose={() =>
                                    setShowAddEquipment(false)
                                }
                                onAdded={async () => {
                                    setNotice(
                                        "Equipment added successfully."
                                    );

                                    setError("");

                                    await loadEquipment();
                                }}
                            />
                        )}

                        {/* MY INSTITUTION'S SHARED EQUIPMENT */}

                        {canPublishSharing && (
                            <section
                                style={{
                                    ...modernCardStyle,
                                    marginBottom: 24,
                                }}
                            >
                                <div style={spaceBetweenStyle}>
                                    <div>
                                        <h3
                                            style={{
                                                margin: "0 0 8px",
                                                color: "#172554",
                                            }}
                                        >
                                            My Institution's Shared Equipment
                                        </h3>

                                        <p style={pageDescriptionStyle}>
                                            Equipment listings published by
                                            your institution.
                                        </p>
                                    </div>

                                    <button
                                        type="button"
                                        onClick={loadMySharings}
                                        style={secondaryButtonStyle}
                                    >
                                        <RefreshCw size={16} />
                                        Refresh
                                    </button>
                                </div>

                                {sharingListings.length === 0 ? (
                                    <p style={infoTextStyle}>
                                        No equipment has been published for
                                        sharing yet.
                                    </p>
                                ) : (
                                    <div
                                        style={{
                                            display: "grid",
                                            gap: 12,
                                            marginTop: 18,
                                        }}
                                    >
                                        {sharingListings.map((listing) => (
                                            <div
                                                key={listing.id}
                                                style={{
                                                    padding: 16,
                                                    border: "1px solid #e2e8f0",
                                                    borderRadius: 12,
                                                    background: "#f8fafc",
                                                }}
                                            >
                                                <div style={spaceBetweenStyle}>
                                                    <div>
                                                        <strong
                                                            style={{
                                                                color: "#172554",
                                                            }}
                                                        >
                                                            {listing.equipmentName}
                                                        </strong>

                                                        <p style={mutedStyle}>
                                                            {listing.assetTag ||
                                                                "No asset tag"}
                                                        </p>
                                                    </div>

                                                    <Status
                                                        value={listing.status}
                                                    />
                                                </div>

                                                <div style={equipmentDetailsStyle}>
                                                    <div>
                                                        <span style={detailLabelStyle}>
                                                            START DATE
                                                        </span>
                                                        <strong>
                                                            {listing.startDate}
                                                        </strong>
                                                    </div>

                                                    <div>
                                                        <span style={detailLabelStyle}>
                                                            END DATE
                                                        </span>
                                                        <strong>
                                                            {listing.endDate}
                                                        </strong>
                                                    </div>

                                                    <div>
                                                        <span style={detailLabelStyle}>
                                                            HOURLY COST
                                                        </span>
                                                        <strong>
                                                            {listing.hourlyCost == null
                                                                ? "Not specified"
                                                                : `₹${listing.hourlyCost}`}
                                                        </strong>
                                                    </div>
                                                </div>

                                                {listing.sharingTerms && (
                                                    <p style={mutedStyle}>
                                                        Sharing terms:{" "}
                                                        {listing.sharingTerms}
                                                    </p>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </section>
                        )}

                        {/* OTHER INSTITUTIONS' SHARED EQUIPMENT */}

                        <section
                            style={{
                                ...modernCardStyle,
                                marginBottom: 24,
                            }}
                        >
                            <div style={spaceBetweenStyle}>
                                <div>
                                    <h3
                                        style={{
                                            margin: "0 0 8px",
                                            color: "#172554",
                                        }}
                                    >
                                        Other Institutions' Shared Equipment
                                    </h3>

                                    <p style={pageDescriptionStyle}>
                                        Explore equipment made available for
                                        sharing by other institutions.
                                    </p>
                                </div>

                                <button
                                    type="button"
                                    onClick={loadAvailableSharings}
                                    style={secondaryButtonStyle}
                                >
                                    <RefreshCw size={16} />
                                    Refresh
                                </button>
                            </div>

                            {availableSharings.length === 0 ? (
                                <p style={infoTextStyle}>
                                    No equipment from other institutions is
                                    currently available for sharing.
                                </p>
                            ) : (
                                <div
                                    style={{
                                        display: "grid",
                                        gap: 12,
                                        marginTop: 18,
                                    }}
                                >
                                    {availableSharings.map((listing) => {
                                        const equipmentName =
                                            listing.equipmentName ||
                                            listing.equipment?.name ||
                                            listing.name ||
                                            "Equipment";

                                        const assetTag =
                                            listing.assetTag ||
                                            listing.equipment?.assetTag;

                                        const institutionName =
                                            listing.institutionName ||
                                            listing.institution?.name ||
                                            listing.institution?.institutionName ||
                                            "Other institution";

                                        return (
                                            <div
                                                key={listing.id}
                                                style={{
                                                    padding: 16,
                                                    border: "1px solid #bfdbfe",
                                                    borderRadius: 12,
                                                    background: "#f8fbff",
                                                }}
                                            >
                                                <div style={spaceBetweenStyle}>
                                                    <div>
                                                        <strong
                                                            style={{
                                                                color: "#172554",
                                                                fontSize: 16,
                                                            }}
                                                        >
                                                            {equipmentName}
                                                        </strong>

                                                        <p style={mutedStyle}>
                                                            {assetTag || "No asset tag"}
                                                        </p>

                                                        <p
                                                            style={{
                                                                ...mutedStyle,
                                                                color: "#1d4ed8",
                                                                fontWeight: 700,
                                                            }}
                                                        >
                                                            <FlaskConical
                                                                size={14}
                                                                style={{
                                                                    verticalAlign: "middle",
                                                                }}
                                                            />{" "}
                                                            {institutionName}
                                                        </p>
                                                    </div>

                                                    <Status value={listing.status} />
                                                </div>

                                                <div style={equipmentDetailsStyle}>
                                                    <div>
                                                        <span style={detailLabelStyle}>
                                                            START DATE
                                                        </span>
                                                        <strong>
                                                            {listing.startDate
                                                                ? formatDate(listing.startDate)
                                                                : "—"}
                                                        </strong>
                                                    </div>

                                                    <div>
                                                        <span style={detailLabelStyle}>
                                                            END DATE
                                                        </span>
                                                        <strong>
                                                            {listing.endDate
                                                                ? formatDate(listing.endDate)
                                                                : "—"}
                                                        </strong>
                                                    </div>

                                                    <div>
                                                        <span style={detailLabelStyle}>
                                                            HOURLY COST
                                                        </span>
                                                        <strong>
                                                            {listing.hourlyCost == null
                                                                ? "Not specified"
                                                                : `₹${listing.hourlyCost}`}
                                                        </strong>
                                                    </div>
                                                </div>

                                                {listing.sharingTerms && (
                                                    <p style={mutedStyle}>
                                                        Sharing terms: {listing.sharingTerms}
                                                    </p>
                                                )}

                                                <RequestAccessCard
                                                    listing={listing}
                                                    onRequestAccess={requestSharedEquipmentAccess}
                                                />
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </section>

                        {/* FILTER BAR */}

                        <section style={filterBarStyle}>
                            <div style={{ flex: "1 1 230px" }}>
                                <input
                                    value={keyword}
                                    onChange={(event) =>
                                        setKeyword(event.target.value)
                                    }
                                    onKeyDown={(event) => {
                                        if (event.key === "Enter") {
                                            loadEquipment();
                                        }
                                    }}
                                    placeholder="Search equipment..."
                                    style={inputStyle}
                                />
                            </div>

                            <div style={{ flex: "0 1 220px" }}>
                                <select
                                    value={status}
                                    onChange={(event) =>
                                        setStatus(event.target.value)
                                    }
                                    style={inputStyle}
                                >
                                    <option value="">All statuses</option>
                                    <option value="AVAILABLE">Available</option>
                                    <option value="IN_USE">In use</option>
                                    <option value="UNDER_MAINTENANCE">
                                        Under maintenance
                                    </option>
                                </select>
                            </div>

                            <button
                                type="button"
                                onClick={loadEquipment}
                                style={primaryButtonStyle}
                            >
                                <Search size={17} />
                                Search
                            </button>

                            <button
                                type="button"
                                onClick={loadEquipment}
                                style={secondaryButtonStyle}
                            >
                                <RefreshCw size={17} />
                                Refresh
                            </button>
                        </section>

                        {/* EQUIPMENT CONTENT */}

                        {loading ? (
                            <p style={infoTextStyle}>
                                Loading equipment...
                            </p>
                        ) : equipment.length === 0 ? (
                            <EmptyState
                                icon={FlaskConical}
                                title="No equipment found"
                                description="Try changing your search or status filter."
                            />
                        ) : (
                            <section style={equipmentGridStyle}>
                                {equipment.map((item) => (
                                    <EquipmentCard
                                        key={item.id}
                                        equipment={item}
                                        onBook={createBooking}
                                        onJoinWaitlist={joinWaitlist}
                                        allowWaitlist={allowWaitlist}
                                        canPublishSharing={canPublishSharing}
                                        onPublishSharing={publishEquipmentSharing}
                                    />
                                ))}
                            </section>
                        )}
                    </>
                )}

                {/* ==================================
                    BOOKINGS PAGE
                ================================== */}

                {page === "bookings" && (
                    <>
                        <section style={toolbarStyle}>
                            {!isManager && (
                                <button
                                    type="button"
                                    onClick={() =>
                                        navigate("/equipment")
                                    }
                                    style={primaryButtonStyle}
                                >
                                    <CalendarPlus size={17} />
                                    Browse Equipment
                                </button>
                            )}

                            <button
                                type="button"
                                onClick={loadBookings}
                                style={secondaryButtonStyle}
                            >
                                <RefreshCw size={17} />
                                Refresh Bookings
                            </button>
                        </section>

                        {loading ? (
                            <p style={infoTextStyle}>
                                Loading bookings...
                            </p>
                        ) : bookings.length === 0 ? (
                            <EmptyState
                                icon={CalendarCheck}
                                title={
                                    isManager
                                        ? "No booking requests"
                                        : "No bookings yet"
                                }
                                description={
                                    isManager
                                        ? "New booking requests will appear here."
                                        : "Browse laboratory equipment to create your first booking."
                                }
                            />
                        ) : (
                            <section style={bookingListStyle}>
                                {(() => {
                                    const firstBookingIdBySeries = new Map();

                                    bookings.forEach((item) => {
                                        if (!item.recurringSeriesId) return;

                                        const existingId =
                                            firstBookingIdBySeries.get(
                                                item.recurringSeriesId
                                            );

                                        if (existingId === undefined) {
                                            firstBookingIdBySeries.set(
                                                item.recurringSeriesId,
                                                item.id
                                            );
                                        }
                                    });

                                    return bookings.map((booking) => {
                                        const showSeriesActions =
                                            Boolean(booking.recurringSeriesId) &&
                                            firstBookingIdBySeries.get(
                                                booking.recurringSeriesId
                                            ) === booking.id;

                                        return (
                                            <BookingCard
                                                key={booking.id}
                                                booking={booking}
                                                isManager={isManager}
                                                busy={
                                                    busyBookingId === booking.id ||
                                                    busyBookingId ===
                                                    `series:${booking.recurringSeriesId}`
                                                }
                                                showSeriesActions={
                                                    showSeriesActions
                                                }
                                                onCancel={cancelBooking}
                                                onDecision={decideBooking}
                                                onSeriesDecision={
                                                    decideRecurringSeries
                                                }
                                                onCheckIn={(id) =>
                                                    updateUsage(id, "check-in")
                                                }
                                                onCheckOut={(id) =>
                                                    updateUsage(id, "check-out")
                                                }
                                            />
                                        );
                                    });
                                })()}
                            </section>
                        )}
                    </>
                )}

                {/* ==================================
                    REPORTS PAGE
                ================================== */}

                {page === "reports" && (
                    <section style={modernCardStyle}>
                        <div style={pageHeaderIconStyle}>
                            <ChartNoAxesCombined size={25} />
                        </div>

                        <h3 style={{ color: "#172554" }}>
                            Reports are not connected yet
                        </h3>

                        <p style={pageDescriptionStyle}>
                            Equipment and booking reports need a reporting
                            API before actual report data can appear.
                        </p>
                    </section>
                )}

                {/* BACK BUTTON */}

                <button
                    type="button"
                    onClick={() => navigate(-1)}
                    style={{
                        ...secondaryButtonStyle,
                        marginTop: 24,
                    }}
                >
                    <ArrowLeft size={17} />
                    Back
                </button>
            </main>
        </DashboardLayout>
    );
}

// ==========================================
// STYLES
// ==========================================

const pageContainerStyle = {
    width: "100%",
    maxWidth: 1180,
    boxSizing: "border-box",
    margin: "0 auto",
    padding: "28px 24px 40px",
};

const pageHeaderStyle = {
    display: "flex",
    alignItems: "center",
    gap: 16,
    marginBottom: 26,
};

const pageHeaderIconStyle = {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    flexShrink: 0,
    width: 54,
    height: 54,
    borderRadius: 16,
    color: "#1d4ed8",
    background: "#dbeafe",
};

const pageHeadingStyle = {
    margin: "0 0 7px",
    color: "#172554",
    fontSize: 26,
    fontWeight: 800,
    letterSpacing: "-0.6px",
};

const pageDescriptionStyle = {
    margin: 0,
    color: "#64748b",
    fontSize: 14,
    lineHeight: 1.7,
};

const filterBarStyle = {
    display: "flex",
    flexWrap: "wrap",
    alignItems: "center",
    gap: 12,
    marginBottom: 24,
    padding: 16,
    background: "#fff",
    border: "1px solid #e5eaf3",
    borderRadius: 16,
};

const toolbarStyle = {
    display: "flex",
    flexWrap: "wrap",
    gap: 10,
    marginBottom: 22,
};

const equipmentGridStyle = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 300px), 1fr))",
    gap: 18,
};

const bookingListStyle = {
    display: "grid",
    gap: 18,
};

const modernCardStyle = {
    boxSizing: "border-box",
    minWidth: 0,
    padding: 22,
    background: "#fff",
    border: "1px solid #e5eaf3",
    borderRadius: 18,
    boxShadow: "0 4px 18px rgba(15, 23, 42, 0.04)",
};

const equipmentTitleStyle = {
    margin: "14px 0 7px",
    color: "#172554",
    fontSize: 19,
    fontWeight: 750,
    overflowWrap: "anywhere",
};

const bookingTitleStyle = {
    margin: "0 0 8px",
    color: "#172554",
    fontSize: 20,
    fontWeight: 750,
    overflowWrap: "anywhere",
};

const iconTileStyle = {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    width: 44,
    height: 44,
    borderRadius: 13,
    color: "#1d4ed8",
    background: "#eff6ff",
};

const equipmentDetailsStyle = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))",
    gap: 16,
    marginTop: 22,
};

const bookingDetailsGridStyle = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 210px), 1fr))",
    gap: 14,
    marginTop: 22,
};

const detailBoxStyle = {
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
    gap: 9,
    padding: 15,
    minWidth: 0,
    overflowWrap: "anywhere",
    background: "#f8fafc",
    border: "1px solid #eef2f7",
    borderRadius: 12,
    color: "#1e293b",
    fontSize: 14,
    lineHeight: 1.6,
};

const detailLabelStyle = {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    color: "#64748b",
    fontSize: 11,
    fontWeight: 800,
    letterSpacing: "0.5px",
};

const rolePillStyle = {
    padding: "5px 9px",
    borderRadius: 20,
    background: "#e0f2fe",
    color: "#075985",
    fontSize: 12,
    fontWeight: 650,
};

const inputStyle = {
    boxSizing: "border-box",
    width: "100%",
    minWidth: 0,
    padding: "12px 13px",
    border: "1px solid #dbe2ee",
    borderRadius: 10,
    background: "#fff",
    color: "#0f172a",
    font: "inherit",
    outlineColor: "#93c5fd",
};

const labelStyle = {
    display: "grid",
    gap: 8,
    color: "#334155",
    fontSize: 14,
    fontWeight: 600,
};

const primaryButtonStyle = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    padding: "12px 16px",
    border: "1px solid #1d4ed8",
    borderRadius: 10,
    background: "#1d4ed8",
    color: "#fff",
    cursor: "pointer",
    fontWeight: 700,
    fontSize: 14,
};

const secondaryButtonStyle = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    padding: "11px 15px",
    border: "1px solid #dbe2ee",
    borderRadius: 10,
    background: "#fff",
    color: "#334155",
    cursor: "pointer",
    fontWeight: 650,
    fontSize: 14,
};

const rejectButtonStyle = {
    ...secondaryButtonStyle,
    color: "#b91c1c",
    borderColor: "#fecaca",
    background: "#fff",
};

const actionRowStyle = {
    display: "flex",
    flexWrap: "wrap",
    gap: 10,
    marginTop: 20,
    paddingTop: 18,
    borderTop: "1px solid #eef2f7",
};

const formStyle = {
    display: "grid",
    gap: 14,
    marginTop: 22,
    paddingTop: 18,
    borderTop: "1px solid #eef2f7",
};

const mutedStyle = {
    display: "block",
    margin: "5px 0",
    color: "#64748b",
    fontSize: 13,
};

const successMessageStyle = {
    padding: 12,
    borderRadius: 10,
    background: "#f0fdf4",
    color: "#15803d",
    fontSize: 14,
    lineHeight: 1.6,
};

const errorMessageStyle = {
    padding: 12,
    borderRadius: 10,
    background: "#fef2f2",
    color: "#b91c1c",
    fontSize: 14,
    lineHeight: 1.6,
};

const successBannerStyle = {
    display: "flex",
    alignItems: "center",
    gap: 10,
    marginBottom: 18,
    padding: 14,
    border: "1px solid #bbf7d0",
    borderRadius: 12,
    background: "#f0fdf4",
    color: "#166534",
    fontSize: 14,
    lineHeight: 1.5,
};

const errorBannerStyle = {
    display: "flex",
    alignItems: "center",
    gap: 10,
    marginBottom: 18,
    padding: 14,
    border: "1px solid #fecaca",
    borderRadius: 12,
    background: "#fef2f2",
    color: "#b91c1c",
    fontSize: 14,
    lineHeight: 1.5,
};

const infoTextStyle = {
    padding: "24px 0",
    color: "#64748b",
    textAlign: "center",
};

const emptyStateStyle = {
    padding: "48px 24px",
    textAlign: "center",
    background: "#fff",
    border: "1px dashed #cbd5e1",
    borderRadius: 18,
};

const emptyIconStyle = {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 58,
    height: 58,
    borderRadius: 18,
    color: "#1d4ed8",
    background: "#eff6ff",
};

const spaceBetweenStyle = {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: 16,
};
