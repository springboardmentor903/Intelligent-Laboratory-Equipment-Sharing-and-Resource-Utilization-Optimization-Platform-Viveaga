
import { useState } from "react";
import {
    BrowserRouter,
    Routes,
    Route,
    Navigate,
    useNavigate,
} from "react-router-dom";

import api from "./api";
import "./App.css";

import StudentDashboard from "./dashboards/StudentDashboard";
import TechnicianDashboard from "./dashboards/TechnicianDashboard";
import ManagerDashboard from "./dashboards/ManagerDashboard";
import DepartmentHeadDashboard from "./dashboards/DepartmentHeadDashboard";
import InstitutionAdminDashboard from "./dashboards/InstitutionAdminDashboard";
import SystemAdminDashboard from "./dashboards/SystemAdminDashboard";
import ResourcePage from "./dashboards/ResourcePage";
import RoleRequests from "./dashboards/RoleRequests";

const dashboardRoutes = {
    RESEARCHER_STUDENT: "/student-dashboard",
    LAB_TECHNICIAN: "/technician-dashboard",
    LAB_MANAGER: "/manager-dashboard",
    DEPARTMENT_HEAD: "/department-head-dashboard",
    INSTITUTION_ADMINISTRATOR: "/institution-admin-dashboard",
    SYSTEM_ADMINISTRATOR: "/system-admin-dashboard",
};

const roles = [
    { value: "RESEARCHER_STUDENT", label: "Researcher / Student" },
    { value: "LAB_TECHNICIAN", label: "Lab Technician" },
    { value: "LAB_MANAGER", label: "Lab Manager" },
    { value: "DEPARTMENT_HEAD", label: "Department Head" },
    {
        value: "INSTITUTION_ADMINISTRATOR",
        label: "Institution Administrator",
    },
];

function AuthPage() {
    const navigate = useNavigate();

    const [isLogin, setIsLogin] = useState(true);
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const [loginData, setLoginData] = useState({
        email: "",
        password: "",
    });

    const [registerData, setRegisterData] = useState({
        fullName: "",
        email: "",
        password: "",
        requestedRole: "RESEARCHER_STUDENT",
    });

    const handleLoginChange = (e) => {
        setLoginData((previous) => ({
            ...previous,
            [e.target.name]: e.target.value,
        }));
    };

    const handleRegisterChange = (e) => {
        setRegisterData((previous) => ({
            ...previous,
            [e.target.name]: e.target.value,
        }));
    };

    const getErrorMessage = (err) => {
        if (!err.response) {
            return "Cannot connect to the server. Check that Spring Boot is running on port 8080.";
        }

        const data = err.response.data;

        if (typeof data === "string" && data.trim()) {
            return data;
        }

        if (data?.message) {
            return data.message;
        }

        if (data?.error) {
            return data.error;
        }

        return `Request failed with status ${err.response.status}.`;
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setMessage("");
        setError("");
        setLoading(true);

        try {
            const response = await api.post("/auth/login", {
                email: loginData.email.trim(),
                password: loginData.password,
            });

            const data = response.data;

            if (!data?.token || !data?.role) {
                setError("Login response is missing the token or role.");
                return;
            }

            const dashboard = dashboardRoutes[data.role];

            if (!dashboard) {
                setError("Your account has an unrecognized role.");
                return;
            }

            sessionStorage.setItem("token", data.token);
            sessionStorage.setItem("userRole", data.role);
            sessionStorage.setItem("userName", data.fullName || "");
            sessionStorage.setItem("userId", data.userId ?? "");

            navigate(dashboard, { replace: true });
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setLoading(false);
        }
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setMessage("");
        setError("");
        setLoading(true);

        try {
            const response = await api.post("/auth/register", {
                ...registerData,
                fullName: registerData.fullName.trim(),
                email: registerData.email.trim(),
            });

            const data = response.data;

            setMessage(
                typeof data === "string"
                    ? data
                    : data?.message ||
                    "Registration successful! You can now sign in."
            );

            setLoginData({
                email: registerData.email.trim(),
                password: "",
            });

            setRegisterData({
                fullName: "",
                email: "",
                password: "",
                requestedRole: "RESEARCHER_STUDENT",
            });

            setIsLogin(true);
            setShowPassword(false);
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            setLoading(false);
        }
    };

    const switchMode = () => {
        setIsLogin((previous) => !previous);
        setMessage("");
        setError("");
        setShowPassword(false);
    };

    return (
        <main className="auth-page">
            <section className="auth-card">
                <header className="auth-brand">
                    <div className="brand-icon">L</div>

                    <div className="brand-text">
                        <h1>Lab Resource Platform</h1>
                        <p>Smart Laboratory Resource Management</p>
                    </div>
                </header>

                <div className="auth-heading">
                    <h2>
                        {isLogin ? "Welcome back!" : "Create your account"}
                    </h2>
                    <p>
                        {isLogin
                            ? "Sign in to access your laboratory workspace."
                            : "Register to get started with the platform."}
                    </p>
                </div>

                {message && (
                    <div className="success-message" role="status">
                        {message}
                    </div>
                )}

                {error && (
                    <div className="error-message" role="alert">
                        {error}
                    </div>
                )}

                {isLogin ? (
                    <form onSubmit={handleLogin}>
                        <div className="form-group">
                            <label htmlFor="login-email">
                                Email address
                            </label>
                            <input
                                id="login-email"
                                type="email"
                                name="email"
                                placeholder="Enter your email"
                                value={loginData.email}
                                onChange={handleLoginChange}
                                autoComplete="email"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="login-password">
                                Password
                            </label>
                            <div className="password-wrapper">
                                <input
                                    id="login-password"
                                    type={showPassword ? "text" : "password"}
                                    name="password"
                                    placeholder="Enter your password"
                                    value={loginData.password}
                                    onChange={handleLoginChange}
                                    autoComplete="current-password"
                                    required
                                />

                                <button
                                    type="button"
                                    className="password-toggle"
                                    onClick={() =>
                                        setShowPassword((previous) => !previous)
                                    }
                                    aria-label={
                                        showPassword
                                            ? "Hide password"
                                            : "Show password"
                                    }
                                >
                                    {showPassword ? "Hide" : "Show"}
                                </button>
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="auth-button"
                            disabled={loading}
                        >
                            {loading ? "Signing in..." : "Sign In"}
                        </button>
                    </form>
                ) : (
                    <form onSubmit={handleRegister}>
                        <div className="form-group">
                            <label htmlFor="register-name">Full name</label>
                            <input
                                id="register-name"
                                type="text"
                                name="fullName"
                                placeholder="Enter your full name"
                                value={registerData.fullName}
                                onChange={handleRegisterChange}
                                autoComplete="name"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="register-email">
                                Email address
                            </label>
                            <input
                                id="register-email"
                                type="email"
                                name="email"
                                placeholder="Enter your email"
                                value={registerData.email}
                                onChange={handleRegisterChange}
                                autoComplete="email"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="register-password">
                                Password
                            </label>
                            <div className="password-wrapper">
                                <input
                                    id="register-password"
                                    type={showPassword ? "text" : "password"}
                                    name="password"
                                    placeholder="Create a password"
                                    value={registerData.password}
                                    onChange={handleRegisterChange}
                                    autoComplete="new-password"
                                    required
                                />

                                <button
                                    type="button"
                                    className="password-toggle"
                                    onClick={() =>
                                        setShowPassword((previous) => !previous)
                                    }
                                    aria-label={
                                        showPassword
                                            ? "Hide password"
                                            : "Show password"
                                    }
                                >
                                    {showPassword ? "Hide" : "Show"}
                                </button>
                            </div>
                        </div>

                        <div className="form-group">
                            <label htmlFor="register-role">
                                Select your role
                            </label>
                            <select
                                id="register-role"
                                name="requestedRole"
                                value={registerData.requestedRole}
                                onChange={handleRegisterChange}
                                required
                            >
                                {roles.map((role) => (
                                    <option
                                        key={role.value}
                                        value={role.value}
                                    >
                                        {role.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <button
                            type="submit"
                            className="auth-button"
                            disabled={loading}
                        >
                            {loading ? "Creating account..." : "Create Account"}
                        </button>
                    </form>
                )}

                <div className="auth-switch">
                    <span>
                        {isLogin
                            ? "Don't have an account?"
                            : "Already have an account?"}
                    </span>

                    <button type="button" onClick={switchMode}>
                        {isLogin ? "Register" : "Sign In"}
                    </button>
                </div>

                <footer className="auth-footer">
                    <span className="footer-dot" />
                    Secure access
                    <span className="footer-separator">•</span>
                    Role-based laboratory management
                </footer>
            </section>
        </main>
    );
}

function ProtectedRoute({ allowedRole, children }) {
    const token = sessionStorage.getItem("token");
    const role = sessionStorage.getItem("userRole");

    if (!token || !role) {
        return <Navigate to="/" replace />;
    }

    if (role !== allowedRole) {
        return <Navigate to={dashboardRoutes[role] || "/"} replace />;
    }

    return children;
}

function ProtectedResourceRoute({ children }) {
    const token = sessionStorage.getItem("token");
    const role = sessionStorage.getItem("userRole");

    if (!token || !role || !dashboardRoutes[role]) {
        return <Navigate to="/" replace />;
    }

    return children;
}

function AppRoutes() {
    const token = sessionStorage.getItem("token");
    const role = sessionStorage.getItem("userRole");
    const defaultRoute = token ? dashboardRoutes[role] || "/" : "/";

    return (
        <Routes>
            <Route
                path="/"
                element={
                    token && defaultRoute !== "/" ? (
                        <Navigate to={defaultRoute} replace />
                    ) : (
                        <AuthPage />
                    )
                }
            />

            <Route
                path="/student-dashboard"
                element={
                    <ProtectedRoute allowedRole="RESEARCHER_STUDENT">
                        <StudentDashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/technician-dashboard"
                element={
                    <ProtectedRoute allowedRole="LAB_TECHNICIAN">
                        <TechnicianDashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/manager-dashboard"
                element={
                    <ProtectedRoute allowedRole="LAB_MANAGER">
                        <ManagerDashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/department-head-dashboard"
                element={
                    <ProtectedRoute allowedRole="DEPARTMENT_HEAD">
                        <DepartmentHeadDashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/institution-admin-dashboard"
                element={
                    <ProtectedRoute allowedRole="INSTITUTION_ADMINISTRATOR">
                        <InstitutionAdminDashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/system-admin-dashboard"
                element={
                    <ProtectedRoute allowedRole="SYSTEM_ADMINISTRATOR">
                        <SystemAdminDashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/role-requests"
                element={
                    <ProtectedRoute allowedRole="SYSTEM_ADMINISTRATOR">
                        <RoleRequests />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/equipment"
                element={
                    <ProtectedResourceRoute>
                        <ResourcePage page="equipment" />
                    </ProtectedResourceRoute>
                }
            />

            <Route
                path="/bookings"
                element={
                    <ProtectedResourceRoute>
                        <ResourcePage page="bookings" />
                    </ProtectedResourceRoute>
                }
            />

            <Route
                path="/reports"
                element={
                    <ProtectedResourceRoute>
                        <ResourcePage page="reports" />
                    </ProtectedResourceRoute>
                }
            />

            <Route
                path="*"
                element={<Navigate to="/" replace />}
            />
        </Routes>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AppRoutes />
        </BrowserRouter>
    );
}

export default App;
