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

const Navbar = () => <div className="navbar" />;

function AppRoutes() {
  const location = useLocation();

  return (
    <Routes>
      {/* Remount on route change ONLY for these two */}
      <Route path="/" element={<Homepage key={`home-${location.key}`} />} />
      <Route path="/profile" element={<AboutPage key={`about-${location.key}`} />} />

      {/* Others unchanged (no forced remount) */}
      <Route path="/schedule" element={<LessonsPage />} />
      <Route path="/mylessons" element={<Mylessons />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/admindashboard" element={<Admindashboard />} />
      <Route path="/mystudents" element={<MyStudents />} />
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
