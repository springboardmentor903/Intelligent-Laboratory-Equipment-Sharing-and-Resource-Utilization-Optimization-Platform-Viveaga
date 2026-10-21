
import DashboardLayout from "./DashboardLayout";
import "./TechnicianDashboard.css";

export default function TechnicianDashboard() {
    return (
        <div className="technician-dashboard">
            <DashboardLayout title="Lab Technician Dashboard">

                <section className="dashboard-welcome">
                    <span className="technician-eyebrow">
                        LABORATORY OPERATIONS
                    </span>

                    <h2>Keep Your Laboratory Running Smoothly</h2>

                    <p>
                        Monitor equipment availability, manage maintenance,
                        and prepare equipment for approved laboratory bookings.
                    </p>
                </section>

                <section className="dashboard-stats">
                    <div className="stat-card">
                        <div className="technician-stat-icon equipment-icon">
                            <span>⚗</span>
                        </div>
                        <h3>Total Equipment</h3>
                        <p>—</p>
                        <span className="stat-description">
                            All equipment in your laboratory
                        </span>
                    </div>

                    <div className="stat-card">
                        <div className="technician-stat-icon booking-icon">
                            <span>▣</span>
                        </div>
                        <h3>Upcoming Bookings</h3>
                        <p>—</p>
                        <span className="stat-description">
                            Approved equipment bookings
                        </span>
                    </div>

                    <div className="stat-card">
                        <div className="technician-stat-icon maintenance-icon">
                            <span>⚙</span>
                        </div>
                        <h3>Maintenance Issues</h3>
                        <p>—</p>
                        <span className="stat-description">
                            Equipment requiring attention
                        </span>
                    </div>

                    <div className="stat-card">
                        <div className="technician-stat-icon available-icon">
                            <span>✓</span>
                        </div>
                        <h3>Operational Equipment</h3>
                        <p>—</p>
                        <span className="stat-description">
                            Equipment ready for use
                        </span>
                    </div>
                </section>

                <section className="dashboard-section technician-actions-section">
                    <div className="technician-section-heading">
                        <div>
                            <h2>Quick Actions</h2>
                            <p>Manage your laboratory's daily operations.</p>
                        </div>
                    </div>

                    <div className="dashboard-actions">
                        <button type="button">
                            <span>⚗</span>
                            View Equipment
                        </button>

                        <button type="button">
                            <span>✓</span>
                            Update Availability
                        </button>

                        <button type="button">
                            <span>⚙</span>
                            Manage Maintenance
                        </button>

                        <button type="button">
                            <span>▣</span>
                            View Bookings
                        </button>
                    </div>
                </section>

                <section className="dashboard-section technician-maintenance">
                    <div className="technician-section-heading">
                        <div>
                            <h2>Maintenance Overview</h2>
                            <p>
                                Keep track of equipment faults and service
                                progress in your laboratory.
                            </p>
                        </div>

                        <span className="technician-status-label">
                            Maintenance
                        </span>
                    </div>

                    <div className="technician-empty-state">
                        <div className="technician-empty-icon">⚙</div>
                        <h3>Maintenance information will appear here</h3>
                        <p>
                            Equipment issues, maintenance status, and service
                            updates will be displayed when connected to the
                            backend.
                        </p>
                    </div>
                </section>

                <section className="dashboard-section technician-bookings">
                    <div className="technician-section-heading">
                        <div>
                            <h2>Upcoming Equipment Bookings</h2>
                            <p>
                                Review approved bookings and prepare equipment
                                before the scheduled time.
                            </p>
                        </div>
                    </div>

                    <div className="technician-empty-state">
                        <div className="technician-empty-icon">▣</div>
                        <h3>No booking data available yet</h3>
                        <p>
                            Approved bookings will appear here once booking
                            data is connected.
                        </p>
                    </div>
                </section>

            </DashboardLayout>
        </div>
    );
}
