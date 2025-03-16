import { Link } from 'react-router-dom';  // Import the Link component
import '../App.css';

// Default value for isAuthenticated
const defaultIsAuthenticated = true;

const Sidebar = ({ isAuthenticated = defaultIsAuthenticated }) => {
    return (
        <aside className="sidebar">
            <ul>
                <li>
                    {isAuthenticated ? (
                        <Link to="/">
                            <img className="homeIcon" src="src/assets/home.png" alt="Home Icon"/>
                        </Link>
                    ) : (
                        <Link to="/login">
                            <img className="homeIcon" src="src/assets/home.png" alt="Home Icon"/>
                        </Link>
                    )}
                </li>
                <li>
                    {isAuthenticated ? (
                        <Link to="/schedule">
                            <img className="homeIcon" src="src/assets/schedule.jpg" alt="Lessons Icon"/>
                        </Link>
                    ) : (
                        <Link to="/login">
                            <img className="homeIcon" src="src/assets/schedule.jpg" alt="Lessons Icon"/>
                        </Link>
                    )}
                </li>
                <li>
                    {isAuthenticated ? (
                        <Link to="/profile">
                            <img className="homeIcon" src="src/assets/music_user_account_profile-512.png" alt="Profile Icon"/>
                        </Link>
                    ) : (
                        <Link to="/login">
                            <img className="homeIcon" src="src/assets/music_user_account_profile-512.png" alt="Profile Icon"/>
                        </Link>
                    )}
                    {isAuthenticated ? (
                        <Link to="/Studentoverview">
                            <img className="homeIcon" src="src/assets/studentoverview.png" alt="Profile Icon"/>
                        </Link>
                    ) : (
                        <Link to="/login">
                            <img className="homeIcon" src="src/assets/studentoverview.png" alt="Profile Icon"/>
                        </Link>
                    )}
                    {isAuthenticated ? (
                        <Link to="/Mylessons">
                            <img className="homeIcon" src="src/assets/calendar.png" alt="Profile Icon"/>
                        </Link>
                    ) : (
                        <Link to="/login">
                            <img className="homeIcon" src="src/assets/calendar.png" alt="Profile Icon"/>
                        </Link>
                    )}
                </li>
            </ul>
        </aside>
    );
};

export default Sidebar;