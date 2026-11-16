
import { useEffect, useState } from "react";
import {
    ArrowLeft,
    RefreshCw,
    Building2,
    Clock3,
    IndianRupee,
    Microscope,
    TrendingUp,
    Users,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import api from "../api";
import "./SharingAnalytics.css";

function formatNumber(value) {
    return Number(value || 0).toFixed(2);
}

function formatCurrency(value) {
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2,
    }).format(value || 0);
}

function SharingAnalytics() {
    const navigate = useNavigate();

    const [summary, setSummary] = useState(null);
    const [trend, setTrend] = useState([]);
    const [partners, setPartners] = useState([]);
    const [equipment, setEquipment] = useState([]);
    const [selectedPartner, setSelectedPartner] = useState(null);
    const [partnerReport, setPartnerReport] = useState(null);

    const [loading, setLoading] = useState(true);
    const [reportLoading, setReportLoading] = useState(false);
    const [error, setError] = useState("");

    const loadAnalytics = async () => {
        setLoading(true);
        setError("");

        try {
            const [summaryRes, trendRes, partnersRes, equipmentRes] =
                await Promise.all([
                    api.get("/sharing/analytics/summary"),
                    api.get("/sharing/analytics/trend"),
                    api.get("/sharing/analytics/partners"),
                    api.get("/sharing/analytics/equipment"),
                ]);

            setSummary(summaryRes.data);
            setTrend(Array.isArray(trendRes.data) ? trendRes.data : []);
            setPartners(Array.isArray(partnersRes.data) ? partnersRes.data : []);
            setEquipment(
                Array.isArray(equipmentRes.data) ? equipmentRes.data : []
            );
        } catch (err) {
            console.error("Sharing analytics error:", err);
            setError(
                err.response?.data?.message ||
                "Unable to load sharing analytics. Please try again."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAnalytics();
    }, []);

    const loadPartnerReport = async (partner) => {
        setSelectedPartner(partner);
        setPartnerReport(null);
        setReportLoading(true);

        try {
            const response = await api.get(
                `/sharing/analytics/partners/${partner.partnerId}/report`
            );
            setPartnerReport(response.data);
        } catch (err) {
            console.error("Partner report error:", err);
            setPartnerReport({ error: "Unable to load partner report." });
        } finally {
            setReportLoading(false);
        }
    };

    const maxTrendHours = Math.max(
        1,
        ...trend.map((item) =>
            Math.max(
                Number(item.providedHours || 0),
                Number(item.usedHours || 0)
            )
        )
    );

    if (loading) {
        return (
            <div className="sharing-analytics-page">
                <div className="sharing-analytics-loading">
                    <RefreshCw className="spin" size={28} />
                    <p>Loading sharing analytics...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="sharing-analytics-page">
            <div className="sharing-analytics-header">
                <button
                    className="analytics-back-button"
                    onClick={() => navigate(-1)}
                >
                    <ArrowLeft size={18} />
                    Back
                </button>

                <div className="analytics-heading">
                    <div>
                        <h1>Sharing Analytics</h1>
                        <p>
                            Monitor inter-institution equipment sharing,
                            partner activity and sharing fees.
                        </p>
                    </div>

                    <button
                        className="analytics-refresh-button"
                        onClick={loadAnalytics}
                    >
                        <RefreshCw size={17} />
                        Refresh
                    </button>
                </div>
            </div>

            {error && (
                <div className="analytics-error">
                    {error}
                </div>
            )}

            {/* SUMMARY CARDS */}
            <section className="analytics-summary-grid">
                <div className="analytics-summary-card">
                    <div className="analytics-card-icon blue">
                        <Clock3 size={22} />
                    </div>
                    <div>
                        <p>Provided Hours</p>
                        <h2>{formatNumber(summary?.providedHours)} hrs</h2>
                        <span>Equipment sharing hours provided</span>
                    </div>
                </div>

                <div className="analytics-summary-card">
                    <div className="analytics-card-icon green">
                        <TrendingUp size={22} />
                    </div>
                    <div>
                        <p>Used Hours</p>
                        <h2>{formatNumber(summary?.usedHours)} hrs</h2>
                        <span>Actual equipment usage</span>
                    </div>
                </div>

                <div className="analytics-summary-card">
                    <div className="analytics-card-icon purple">
                        <IndianRupee size={22} />
                    </div>
                    <div>
                        <p>Fees Earned</p>
                        <h2>{formatCurrency(summary?.feesEarned)}</h2>
                        <span>Sharing income</span>
                    </div>
                </div>

                <div className="analytics-summary-card">
                    <div className="analytics-card-icon orange">
                        <IndianRupee size={22} />
                    </div>
                    <div>
                        <p>Fees Owed</p>
                        <h2>{formatCurrency(summary?.feesOwed)}</h2>
                        <span>Sharing expenses</span>
                    </div>
                </div>
            </section>

            {/* MONTHLY TREND */}
            <section className="analytics-panel">
                <div className="analytics-section-heading">
                    <div>
                        <h2>Monthly Sharing Trend</h2>
                        <p>Provided hours compared with actual used hours</p>
                    </div>
                </div>

                {trend.length === 0 ? (
                    <p className="analytics-empty">
                        No monthly sharing data available yet.
                    </p>
                ) : (
                    <>
                        <div className="analytics-chart-legend">
                            <span>
                                <i className="legend-provided" />
                                Provided hours
                            </span>
                            <span>
                                <i className="legend-used" />
                                Used hours
                            </span>
                        </div>

                        <div className="analytics-bar-chart">
                            {trend.map((item) => (
                                <div
                                    className="analytics-chart-column"
                                    key={item.month}
                                >
                                    <div className="analytics-bars">
                                        <div
                                            className="analytics-bar provided"
                                            style={{
                                                height: `${Math.max(
                                                    3,
                                                    (Number(item.providedHours || 0) /
                                                        maxTrendHours) *
                                                    150
                                                )}px`,
                                            }}
                                            title={`Provided: ${formatNumber(
                                                item.providedHours
                                            )} hours`}
                                        />

                                        <div
                                            className="analytics-bar used"
                                            style={{
                                                height: `${Math.max(
                                                    3,
                                                    (Number(item.usedHours || 0) /
                                                        maxTrendHours) *
                                                    150
                                                )}px`,
                                            }}
                                            title={`Used: ${formatNumber(
                                                item.usedHours
                                            )} hours`}
                                        />
                                    </div>

                                    <span>{item.month}</span>
                                </div>
                            ))}
                        </div>
                    </>
                )}
            </section>

            {/* PARTNER INSTITUTIONS */}
            <section className="analytics-panel">
                <div className="analytics-section-heading">
                    <div>
                        <h2>Partner Institutions</h2>
                        <p>Institutions involved in equipment sharing</p>
                    </div>
                    <Users size={22} />
                </div>

                {partners.length === 0 ? (
                    <p className="analytics-empty">
                        No partner institutions found.
                    </p>
                ) : (
                    <div className="analytics-table-wrapper">
                        <table className="analytics-table">
                            <thead>
                            <tr>
                                <th>Partner Institution</th>
                                <th>Bookings</th>
                                <th>Hours</th>
                                <th>Fees</th>
                                <th>Action</th>
                            </tr>
                            </thead>

                            <tbody>
                            {partners.map((partner, index) => (
                                <tr key={partner.partnerId ?? index}>
                                    <td>
                                        <div className="analytics-partner-name">
                                            <Building2 size={18} />
                                            {partner.partnerName ||
                                                partner.ownerInstitutionName ||
                                                "Partner institution"}
                                        </div>
                                    </td>
                                    <td>{partner.bookingCount || 0}</td>
                                    <td>
                                        {formatNumber(partner.hours)} hrs
                                    </td>
                                    <td>
                                        {formatCurrency(partner.fees)}
                                    </td>
                                    <td>
                                        <button
                                            className="analytics-view-button"
                                            onClick={() =>
                                                loadPartnerReport(partner)
                                            }
                                        >
                                            View Report
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            {/* PARTNER REPORT */}
            {selectedPartner && (
                <section className="analytics-panel">
                    <div className="analytics-section-heading">
                        <div>
                            <h2>
                                Partner Report:{" "}
                                {selectedPartner.partnerName ||
                                    selectedPartner.ownerInstitutionName ||
                                    "Institution"}
                            </h2>
                            <p>Detailed sharing activity with this partner</p>
                        </div>

                        <button
                            className="analytics-close-button"
                            onClick={() => {
                                setSelectedPartner(null);
                                setPartnerReport(null);
                            }}
                        >
                            Close
                        </button>
                    </div>

                    {reportLoading ? (
                        <p className="analytics-empty">
                            Loading partner report...
                        </p>
                    ) : partnerReport?.error ? (
                        <p className="analytics-error">
                            {partnerReport.error}
                        </p>
                    ) : partnerReport ? (
                        <div className="partner-report-grid">
                            <div>
                                <span>Total Bookings</span>
                                <strong>
                                    {partnerReport.bookingCount || 0}
                                </strong>
                            </div>
                            <div>
                                <span>Booked Hours</span>
                                <strong>
                                    {formatNumber(partnerReport.bookedHours)} hrs
                                </strong>
                            </div>
                            <div>
                                <span>Actual Used Hours</span>
                                <strong>
                                    {formatNumber(partnerReport.usedHours)} hrs
                                </strong>
                            </div>
                            <div>
                                <span>Total Fees</span>
                                <strong>
                                    {formatCurrency(partnerReport.fees)}
                                </strong>
                            </div>
                        </div>
                    ) : null}
                </section>
            )}

            {/* SHARED EQUIPMENT */}
            <section className="analytics-panel">
                <div className="analytics-section-heading">
                    <div>
                        <h2>Shared Equipment Performance</h2>
                        <p>Booking and utilization details for shared equipment</p>
                    </div>
                    <Microscope size={22} />
                </div>

                {equipment.length === 0 ? (
                    <p className="analytics-empty">
                        No shared equipment activity found.
                    </p>
                ) : (
                    <div className="analytics-table-wrapper">
                        <table className="analytics-table">
                            <thead>
                            <tr>
                                <th>Equipment</th>
                                <th>Bookings</th>
                                <th>Booked Hours</th>
                                <th>Used Hours</th>
                                <th>Fees</th>
                            </tr>
                            </thead>

                            <tbody>
                            {equipment.map((item) => (
                                <tr key={item.equipmentId}>
                                    <td>{item.equipmentName}</td>
                                    <td>{item.bookingCount || 0}</td>
                                    <td>
                                        {formatNumber(item.bookedHours)} hrs
                                    </td>
                                    <td>
                                        {formatNumber(item.usedHours)} hrs
                                    </td>
                                    <td>{formatCurrency(item.fees)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    );
}

export default SharingAnalytics;
