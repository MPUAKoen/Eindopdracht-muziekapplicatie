import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Sidebar from "./Components/Sidebar";
import Homepage from "./Components/Homepage";
import AboutPage from "./Components/AboutPage.jsx";  // Make sure this component exists and is properly imported
import './App.css';
import LessonsPage from "./Components/LessonsPage.jsx";
import Homework from "./Components/homework.jsx";

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
                        <Route path="/lessons" element={<LessonsPage />} />  {/* Add other routes here */}
                        <Route path="/homework" element={<Homework />} />  {/* Add other routes here */}
                    </Routes>
                </div>
            </div>
        </Router>
    );
};

export default App;
