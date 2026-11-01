import { useEffect, useState } from "react";
import { RefreshCw, Activity, Microscope } from "lucide-react";
import DashboardLayout from "./DashboardLayout";
import api from "../api";
import {
    connectWebSocket,
    disconnectWebSocket,
} from "../websocket";
import "./LiveEquipment.css";

export default function LiveEquipment() {
    const [equipment, setEquipment] = useState([]);
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const pageSize = 10;

    const fetchEquipment = async () => {
        try {
            setLoading(true);
            setError("");

            const response = await api.get("/live-equipment", {
                params: {
                    status: status || undefined,
                    page,
                    size: pageSize,
                },
            });

            setEquipment(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load live equipment status."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchEquipment();
    }, [status, page]);

    useEffect(() => {
        connectWebSocket((updatedEquipment) => {
            const filteredEquipment = status
                ? updatedEquipment.filter(
                    (item) => item.currentStatus === status
                )
                : updatedEquipment;

            const startIndex = page * pageSize;

            setEquipment(
                filteredEquipment.slice(startIndex, startIndex + pageSize)
            );
        });

        return () => {
            disconnectWebSocket();
        };
    }, [page, status]);

    const handleRefresh = () => {
        fetchEquipment();
    };

    return (
        <div className="live-equipment-dashboard">
            <DashboardLayout title="Live Equipment Monitoring">
                <main className="live-equipment-page">

                    <section className="live-equipment-header">
                        <div>
                            <span className="live-eyebrow">
                                REAL-TIME MONITORING
                            </span>

                            <h2>Equipment Status</h2>

                            <p>
                                Monitor current laboratory equipment
                                availability and active usage.
                            </p>
                        </div>

                        <button
                            className="live-refresh-button"
                            onClick={handleRefresh}
                            disabled={loading}
                        >
                            <RefreshCw size={17} />
                            Refresh
                        </button>
                    </section>

                    <section className="live-filter-section">
                        <label htmlFor="equipment-status">
                            Filter by status
                        </label>

                        <select
                            id="equipment-status"
                            value={status}
                            onChange={(e) => {
                                setStatus(e.target.value);
                                setPage(0);
                            }}
                        >
                            <option value="">All Equipment</option>
                            <option value="AVAILABLE">Available</option>
                            <option value="BOOKED">Booked</option>
                            <option value="UNDER_MAINTENANCE">
                                Under Maintenance
                            </option>
                            <option value="OUT_OF_SERVICE">
                                Out of Service
                            </option>
                        </select>
                    </section>

                    {error && (
                        <div className="live-error">
                            {error}
                            <button onClick={handleRefresh}>
                                Retry
                            </button>
                        </div>
                    )}

                    {loading ? (
                        <div className="live-message">
                            Loading equipment status...
                        </div>
                    ) : equipment.length === 0 ? (
                        <div className="live-empty">
                            <Microscope size={35} />
                            <h3>No equipment found</h3>
                            <p>
                                No equipment matches the selected status.
                            </p>
                        </div>
                    ) : (
                        <section className="live-equipment-grid">
                            {equipment.map((item) => (
                                <article
                                    className="live-equipment-card"
                                    key={item.equipmentId}
                                >
                                    <div className="live-card-top">
                                        <div className="live-card-icon">
                                            <Microscope size={24} />
                                        </div>

                                        <span
                                            className={`live-status ${item.currentStatus.toLowerCase()}`}
                                        >
                                            {item.currentStatus.replaceAll(
                                                "_",
                                                " "
                                            )}
                                        </span>
                                    </div>

                                    <h3>{item.equipmentName}</h3>

                                    <p className="live-asset-tag">
                                        Asset Tag: {item.assetTag}
                                    </p>

                                    <div className="live-card-details">
                                        <div>
                                            <span>Currently in use</span>
                                            <strong>
                                                {item.inUse ? "Yes" : "No"}
                                            </strong>
                                        </div>

                                        <div>
                                            <span>Actual start</span>
                                            <strong>
                                                {item.actualStartTime
                                                    ? new Date(
                                                        item.actualStartTime
                                                    ).toLocaleString()
                                                    : "—"}
                                            </strong>
                                        </div>

                                        <div>
                                            <span>Scheduled end</span>
                                            <strong>
                                                {item.scheduledEndTime
                                                    ? new Date(
                                                        item.scheduledEndTime
                                                    ).toLocaleString()
                                                    : "—"}
                                            </strong>
                                        </div>

                                        <div>
                                            <span>Elapsed time</span>
                                            <strong>
                                                {item.elapsedMinutes != null
                                                    ? `${item.elapsedMinutes} minutes`
                                                    : "—"}
                                            </strong>
                                        </div>

                                        <div>
                                            <span>Next booking</span>
                                            <strong>
                                                {item.nextBookingTime
                                                    ? new Date(
                                                        item.nextBookingTime
                                                    ).toLocaleString()
                                                    : "No upcoming booking"}
                                            </strong>
                                        </div>
                                    </div>

                                    {item.inUse && (
                                        <div className="live-active-indicator">
                                            <Activity size={16} />
                                            Equipment currently in use
                                        </div>
                                    )}
                                </article>
                            ))}
                        </section>
                    )}

                    <div className="live-pagination">
                        <button
                            onClick={() =>
                                setPage((previous) => Math.max(0, previous - 1))
                            }
                            disabled={page === 0 || loading}
                        >
                            Previous
                        </button>

                        <span>Page {page + 1}</span>

                        <button
                            onClick={() => setPage((previous) => previous + 1)}
                            disabled={equipment.length < pageSize || loading}
                        >
                            Next
                        </button>
                    </div>

                </main>
            </DashboardLayout>
        </div>
    );
}