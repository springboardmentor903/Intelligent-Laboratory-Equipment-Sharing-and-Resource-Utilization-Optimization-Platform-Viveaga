
import { useNavigate } from "react-router-dom";
import {
    FlaskConical,
    CalendarCheck,
    ChartNoAxesCombined,
    ArrowLeft,
} from "lucide-react";
import DashboardLayout from "./DashboardLayout";

const pageDetails = {
    equipment: {
        title: "Equipment",
        heading: "Laboratory Equipment",
        description:
            "Explore laboratory equipment and check resource availability.",
        icon: FlaskConical,
    },
    bookings: {
        title: "Bookings",
        heading: "My Bookings",
        description:
            "View your equipment bookings and track request status.",
        icon: CalendarCheck,
    },
    reports: {
        title: "Reports",
        heading: "Resource Reports",
        description:
            "View laboratory resource usage and booking reports.",
        icon: ChartNoAxesCombined,
    },
};

export default function ResourcePage({ page }) {
    const navigate = useNavigate();
    const details = pageDetails[page];

    if (!details) {
        return <p>Page not found.</p>;
    }

    const PageIcon = details.icon;

    return (
        <DashboardLayout title={details.title}>
            <section className="dashboard-welcome">
                <div>
                    <PageIcon size={30} />
                    <h2>{details.heading}</h2>
                    <p>{details.description}</p>
                </div>
            </section>

            <section className="dashboard-section">
                <h2>Coming Soon</h2>

                <p>
                    This page is ready for the next development step.
                    Equipment details and booking data will appear here
                    after connecting the backend APIs.
                </p>

                <button
                    type="button"
                    onClick={() => navigate(-1)}
                >
                    <ArrowLeft size={17} />
                    Back
                </button>
            </section>
        </DashboardLayout>
    );
}
