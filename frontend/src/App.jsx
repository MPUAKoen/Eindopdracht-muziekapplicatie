// App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import Sidebar from "./Components/Sidebar";
import Homepage from "./Components/Homepage";
import AboutPage from "./Components/AboutPage.jsx";
import LessonsPage from "./Components/Schedule.jsx";
import Mylessons from "./Components/Mylessons.jsx";
import LoginPage from "./Components/LoginPage.jsx";
import RegisterPage from "./Components/RegisterPage.jsx";
import Admindashboard from "./Components/Admindashboard.jsx";
import MyStudents from './Components/MyStudents.jsx';
import { UserProvider } from './Context/UserContext';
import ProtectedRoute from "./Components/ProtectedRoute"; // ✅ import

const Navbar = () => <div className="navbar" />;

function AppRoutes() {
  const location = useLocation();

  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<Homepage key={`home-${location.key}`} />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Everyone logged in */}
      <Route
        path="/profile"
        element={
          <ProtectedRoute allowedRoles={["USER", "TEACHER", "ADMIN"]}>
            <AboutPage key={`about-${location.key}`} />
          </ProtectedRoute>
        }
      />

      {/* Students + Teachers */}
      <Route
        path="/mylessons"
        element={
          <ProtectedRoute allowedRoles={["USER", "TEACHER"]}>
            <Mylessons />
          </ProtectedRoute>
        }
      />

      {/* Teachers only */}
      <Route
        path="/schedule"
        element={
          <ProtectedRoute allowedRoles={["TEACHER"]}>
            <LessonsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/mystudents"
        element={
          <ProtectedRoute allowedRoles={["TEACHER"]}>
            <MyStudents />
          </ProtectedRoute>
        }
      />

      {/* Admin only */}
      <Route
        path="/admindashboard"
        element={
          <ProtectedRoute allowedRoles={["ADMIN"]}>
            <Admindashboard />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default function App() {
  return (
    <UserProvider>
      <Router>
        <div className="app-container">
          <Sidebar />
          <div className="main-content">
            <Navbar />
            <AppRoutes />
          </div>
        </div>
      </Router>
    </UserProvider>
  );
}
