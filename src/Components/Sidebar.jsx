import { Link } from 'react-router-dom';  // Import the Link component
import '../App.css';

const Sidebar = () => {
    return (
        <aside className="sidebar">
            <ul>
                <li>
                    <Link to="/">
                        <img className="homeIcon" src="src/assets/home.png" alt="Home Icon"/>
                    </Link>
                </li>
                <li>
                    <Link to="/Lessons">
                        <img className="homeIcon" src="src/assets/music-note2.png" alt="Lessons Icon"/>
                    </Link>
                </li>
                <li>
                    <Link to="/profile">
                        <img className="homeIcon" src="src/assets/music_user_account_profile-512.png"
                             alt="Profile Icon"/>
                    </Link>
                    <Link to="/register">
                        <img className="homeIcon" src="src/assets/music_user_account_profile-512.png"
                             alt="Profile Icon"/>
                    </Link>
                    <li>
                        <Link to="/homework">
                            <img className="homeIcon" src="src/assets/Homework.png"
                                 alt="Profile Icon"/>
                        </Link>

                    </li>
                </li>
            </ul>
        </aside>
    );
};

export default Sidebar;
