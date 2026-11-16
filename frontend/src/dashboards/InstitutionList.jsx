
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    Building2,
    MapPin,
    Users,
    ArrowUpRight,
    ArrowLeft,
    RefreshCw
} from "lucide-react";

import api from "../api";
import DashboardLayout from "./DashboardLayout";
import "./InstitutionList.css";

export default function InstitutionList() {
    const navigate = useNavigate();

    const [institutions, setInstitutions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const fetchInstitutions = async () => {
        try {
            setLoading(true);
            setError("");

            const response = await api.get("/system-admin/institutions");

            setInstitutions(
                Array.isArray(response.data) ? response.data : []
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load institutions."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchInstitutions();
    }, []);

    return (
        <DashboardLayout title="System Administrator">
            <div className="institution-list-page">

                <button
                    className="institution-back-button"
                    onClick={() => navigate("/system-admin-dashboard")}
                >
                    <ArrowLeft size={17} />
                    Back to Dashboard
                </button>

                <div className="institution-list-heading">
                    <div>
                        <span>PLATFORM MANAGEMENT</span>
                        <h2>Registered Institutions</h2>
                        <p>
                            Select an institution to view its complete details
                            and registered users.
                        </p>
                    </div>

                    <div className="institution-list-count">
                        <Building2 size={22} />
                        <strong>{institutions.length}</strong>
                        <span>Institutions</span>
                    </div>
                </div>

                {loading ? (
                    <div className="institution-list-message">
                        Loading institutions...
                    </div>
                ) : error ? (
                    <div className="institution-list-error">
                        <p>{error}</p>
                        <button onClick={fetchInstitutions}>
                            <RefreshCw size={15} />
                            Try Again
                        </button>
                    </div>
                ) : institutions.length === 0 ? (
                    <div className="institution-list-message">
                        No institutions registered yet.
                    </div>
                ) : (
                    <div className="institution-list-grid">
                        {institutions.map((institution) => (
                            <button
                                type="button"
                                className="institution-list-card"
                                key={institution.id}
                                onClick={() =>
                                    navigate(
                                        `/institution-details/${institution.id}`
                                    )
                                }
                            >
                                <div className="institution-list-card-top">
                                    <div className="institution-list-icon">
                                        <Building2 size={25} />
                                    </div>

                                    <ArrowUpRight
                                        size={20}
                                        className="institution-list-arrow"
                                    />
                                </div>

                                <h3>{institution.name}</h3>

                                <p className="institution-list-address">
                                    <MapPin size={15} />
                                    {institution.address ||
                                        "Address not available"}
                                </p>

                                <div className="institution-list-users">
                                    <span>
                                        <Users size={17} />
                                        Total Registered Users
                                    </span>

                                    <strong>
                                        {institution.totalUsers ?? 0}
                                    </strong>
                                </div>

                                <div className="institution-list-hint">
                                    View Institution Details
                                    <ArrowUpRight size={16} />
                                </div>
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
}
