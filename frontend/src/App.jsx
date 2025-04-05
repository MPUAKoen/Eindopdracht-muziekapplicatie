import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Sidebar from "./Components/Sidebar";
import Homepage from "./Components/Homepage";
import AboutPage from "./Components/AboutPage.jsx";  // Make sure this component exists and is properly imported
import './App.css';
import LessonsPage from "./Components/Schedule.jsx";
import Homework from "./Components/Mylessons.jsx";
import LoginPage from "./Components/LoginPage.jsx";
import RegisterPage from "./Components/RegisterPage.jsx";
import StudentOverview from "./Components/StudentOverview.jsx";
import Mylessons from "./Components/Mylessons.jsx";

const Navbar = () => (
    <div className="navbar">
        {/* Navbar content */}
    </div>
);

const App = () => {
    return (
        <Router>
            <div className="app-container">
                <Sidebar />
                <div className="main-content">
                    <Navbar />
                    <Routes>
                        <Route path="/" element={<Homepage />} />
                        <Route path="/profile" element={<AboutPage />} />  {/* Add other routes here */}
                        <Route path="/schedule" element={<LessonsPage />} />  {/* Add other routes here */}
                        <Route path="/Mylessons" element={<Mylessons />} />  {/* Add other routes here */}
                        <Route path="/login" element={<LoginPage />} />  {/* Add other routes here */}
                        <Route path="/register" element={<RegisterPage />} />  {/* Add other routes here */}
                        <Route path="/Studentoverview" element={<StudentOverview />} />  {/* Add other routes here */}

                    </Routes>
                </div>
            </div>
        </Router>
    );
};

export default App;
