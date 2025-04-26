import { Link } from 'react-router-dom';
import { useUser } from '../Context/UserContext'; // <-- Import user context here
import '../App.css';

const Sidebar = () => {
    const { user } = useUser();
    const isAuthenticated = !!user;

    const getLink = (path) => (isAuthenticated ? path : '/login');

    return (
        <aside className="sidebar">
            <ul>
                <li>
                    <Link to={getLink('/')}>
                        <img className="homeIcon" src="src/assets/home.png" alt="Home Icon" />
                    </Link>
                </li>
                <li>
                    <Link to={getLink('/schedule')}>
                        <img className="homeIcon" src="src/assets/schedule.jpg" alt="Lessons Icon" />
                    </Link>
                </li>
                <li>
                    <Link to={getLink('/profile')}>
                        <img className="homeIcon" src="src/assets/music_user_account_profile-512.png" alt="Profile Icon" />
                    </Link>
                    <Link to={getLink('/Studentoverview')}>
                        <img className="homeIcon" src="src/assets/studentoverview.png" alt="Student Overview Icon" />
                    </Link>
                    <Link to={getLink('/Mylessons')}>
                        <img className="homeIcon" src="src/assets/calendar.png" alt="Calendar Icon" />
                    </Link>
                </li>
            </ul>
        </aside>
    );
};

export default Sidebar;
