import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Sidebar from "./Components/Sidebar";
import Homepage from "./Components/Homepage";
import AboutPage from "./Components/AboutPage.jsx";
import LessonsPage from "./Components/Schedule.jsx";
import Mylessons from "./Components/Mylessons.jsx";
import LoginPage from "./Components/LoginPage.jsx";
import RegisterPage from "./Components/RegisterPage.jsx";
import Admindashboard from "./Components/Admindashboard.jsx";import { UserProvider } from './Context/UserContext';
import MyStudents from './Components/MyStudents.jsx';

const Navbar = () => (
    <div className="navbar">
        {/* Navbar content */}
    </div>
);

const App = () => {
    return (
        <UserProvider>  {/* Wrap your app in UserProvider */}
            <Router>
                <div className="app-container">
                    <Sidebar />
                    <div className="main-content">
                        <Navbar />
                        <Routes>
                            <Route path="/" element={<Homepage />} />
                            <Route path="/profile" element={<AboutPage />} />
                            <Route path="/schedule" element={<LessonsPage />} />
                            <Route path="/Mylessons" element={<Mylessons />} />
                            <Route path="/login" element={<LoginPage />} />
                            <Route path="/register" element={<RegisterPage />} />
                            <Route path="/Admindashboard" element={<Admindashboard />} />  
                            <Route path="/mystudents" element={<MyStudents />} />                      
                        </Routes>
                    </div>
                </div>
            </Router>
        </UserProvider>
    );
};

export default App;
