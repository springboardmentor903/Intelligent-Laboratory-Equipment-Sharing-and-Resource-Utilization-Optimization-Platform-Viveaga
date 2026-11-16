
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    Building2,
    MapPin,
    Users,
    Mail,
    UserCog,
    ShieldCheck,
    GraduationCap,
    ArrowLeft,
    RefreshCw
} from "lucide-react";

import api from "../api";
import DashboardLayout from "./DashboardLayout";
import "./InstitutionDetails.css";

function StaffSection({ title, staff, icon: Icon }) {
    return (
        <section className="institution-detail-section">
            <div className="institution-detail-section-heading">
                <Icon size={19} />
                <h3>{title}</h3>
                <span>{staff?.length || 0}</span>
            </div>

            {staff?.length > 0 ? (
                <div className="institution-detail-staff-grid">
                    {staff.map((person) => (
                        <div className="institution-detail-person" key={person.id}>
                            <div className="institution-detail-avatar">
                                {person.fullName?.charAt(0)?.toUpperCase() || "U"}
                            </div>

                            <div>
                                <strong>{person.fullName}</strong>
                                <p>
                                    <Mail size={13} />
                                    {person.email}
                                </p>
                                <small>
                                    {person.role?.replaceAll("_", " ")}
                                </small>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <p className="institution-detail-empty">
                    No {title.toLowerCase()} assigned yet.
                </p>
            )}
        </section>
    );
}

export default function InstitutionDetails() {
    const navigate = useNavigate();
    const { institutionId } = useParams();

    const [institution, setInstitution] = useState(null);
    const [institutionUsers, setInstitutionUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const fetchDetails = async () => {
        try {
            setLoading(true);
            setError("");

            const [institutionsResponse, usersResponse] =
                await Promise.all([
                    api.get("/system-admin/institutions"),
                    api.get("/system-admin/users")
                ]);

            const institutions = Array.isArray(institutionsResponse.data)
                ? institutionsResponse.data
                : [];

            const users = Array.isArray(usersResponse.data)
                ? usersResponse.data
                : [];

            const selectedInstitution = institutions.find(
                (item) => Number(item.id) === Number(institutionId)
            );

            if (!selectedInstitution) {
                setInstitution(null);
                setInstitutionUsers([]);
                setError("Institution not found.");
                return;
            }

            setInstitution(selectedInstitution);

            setInstitutionUsers(
                users.filter(
                    (user) =>
                        Number(user.institutionId) === Number(institutionId)
                )
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load institution details."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDetails();
    }, [institutionId]);

    return (
        <DashboardLayout title="System Administrator">
            <div className="institution-details-page">

                <button
                    className="institution-details-back"
                    onClick={() => navigate("/institutions")}
                >
                    <ArrowLeft size={17} />
                    Back to Institutions
                </button>

                {loading ? (
                    <div className="institution-details-message">
                        Loading institution details...
                    </div>
                ) : error ? (
                    <div className="institution-details-error">
                        <p>{error}</p>
                        <button onClick={fetchDetails}>
                            <RefreshCw size={15} />
                            Try Again
                        </button>
                    </div>
                ) : institution && (
                    <>
                        <section className="institution-details-hero">
                            <div className="institution-details-main-icon">
                                <Building2 size={32} />
                            </div>

                            <div>
                                <span>INSTITUTION INFORMATION</span>
                                <h2>{institution.name}</h2>
                                <p>
                                    <MapPin size={16} />
                                    {institution.address || "Address not available"}
                                </p>
                            </div>

                            <div className="institution-details-total">
                                <Users size={21} />
                                <strong>{institution.totalUsers ?? institutionUsers.length}</strong>
                                <span>Total Users</span>
                            </div>
                        </section>

                        <div className="institution-details-heading">
                            <h2>Institution Members</h2>
                            <p>
                                Staff and users registered under this institution.
                            </p>
                        </div>

                        <div className="institution-details-sections">
                            <StaffSection
                                title="Lab Managers"
                                staff={institution.labManagers}
                                icon={UserCog}
                            />

                            <StaffSection
                                title="Institution Administrators"
                                staff={institution.institutionAdmins}
                                icon={ShieldCheck}
                            />

                            <StaffSection
                                title="Department Heads"
                                staff={institution.departmentHeads}
                                icon={GraduationCap}
                            />
                        </div>

                        <section className="institution-all-users">
                            <div className="institution-detail-section-heading">
                                <Users size={19} />
                                <h3>All Institution Users</h3>
                                <span>{institutionUsers.length}</span>
                            </div>

                            {institutionUsers.length > 0 ? (
                                <div className="institution-users-table-wrapper">
                                    <table className="institution-users-table">
                                        <thead>
                                        <tr>
                                            <th>User</th>
                                            <th>Email</th>
                                            <th>Role</th>
                                        </tr>
                                        </thead>

                                        <tbody>
                                        {institutionUsers.map((user) => (
                                            <tr key={user.id}>
                                                <td>
                                                    <div className="institution-table-user">
                                                            <span>
                                                                {user.fullName
                                                                    ?.charAt(0)
                                                                    ?.toUpperCase() || "U"}
                                                            </span>
                                                        <strong>{user.fullName}</strong>
                                                    </div>
                                                </td>
                                                <td>{user.email}</td>
                                                <td>
                                                        <span className="institution-user-role">
                                                            {user.role?.replaceAll("_", " ")}
                                                        </span>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <p className="institution-detail-empty">
                                    No users found in this institution.
                                </p>
                            )}
                        </section>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}
