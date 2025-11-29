import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../Context/UserContext';
import StudentPracticePopup from './StudentPracticePopup';
import '../App.css';

const Sidebar = () => {
  const { user, logout } = useUser();
  const isAuthenticated = Boolean(user);
  const role = user?.role?.toUpperCase();
  const [isOpen, setIsOpen] = useState(false);
  const [showPractice, setShowPractice] = useState(false);

  const isStudent = role === 'USER';

  const getLink = (path) => (isAuthenticated ? path : '/login');

  const handleLogout = () => {
    logout();
    setIsOpen(false);
    setShowPractice(false);
  };

  const handleLinkClick = () => {
    setIsOpen(false);
  };

  return (
    <>
      {/* Hamburger (mobile only) */}
      <button
        className="hamburger-btn"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Toggle navigation"
      >
        <span />
        <span />
        <span />
      </button>

      <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
        <ul>
          {/* Always visible */}
          <li>
            <Link to={getLink('/')} onClick={handleLinkClick}>
              <img className="homeIcon" src="src/assets/home.png" alt="Home" />
            </Link>
          </li>

          {/* Only TEACHERS */}
          {role === 'TEACHER' && (
            <>
              <li>
                <Link to={getLink('/schedule')} onClick={handleLinkClick}>
                  <img
                    className="homeIcon"
                    src="src/assets/schedule.jpg"
                    alt="Schedule"
                  />
                </Link>
              </li>
              <li>
                <Link to={getLink('/mystudents')} onClick={handleLinkClick}>
                  <img
                    className="homeIcon"
                    src="src/assets/my students.png"
                    alt="My Students"
                  />
                </Link>
              </li>
            </>
          )}

          {/* Only STUDENTS: practice tracker popup */}
          {isAuthenticated && isStudent && (
            <li>
              <button
                type="button"
                className="sidebar-icon-button"
                onClick={() => setShowPractice((prev) => !prev)}
              >
                <img
                  className="homeIcon"
                  src="src\assets\performance-tracking-icon-can-be-600nw-2350304125.png"
                  alt="Practice tracker"
                />
              </button>
            </li>
          )}

          {/* Only ADMINS */}
          {role === 'ADMIN' && (
            <li>
              <Link to={getLink('/Admindashboard')} onClick={handleLinkClick}>
                <img
                  className="homeIcon"
                  src="src/assets/studentoverview.png"
                  alt="Admin Dashboard"
                />
              </Link>
            </li>
          )}

          {/* Everyone logged in */}
          {isAuthenticated && (
            <>
              <li>
                <Link to={getLink('/profile')} onClick={handleLinkClick}>
                  <img
                    className="homeIcon"
                    src="src/assets/music_user_account_profile-512.png"
                    alt="Profile"
                  />
                </Link>
              </li>

              {/* Only USERS and TEACHERS */}
              {(role === 'USER' || role === 'TEACHER') && (
                <li>
                  <Link to={getLink('/Mylessons')} onClick={handleLinkClick}>
                    <img
                      className="homeIcon"
                      src="src/assets/calendar.png"
                      alt="My Lessons"
                    />
                  </Link>
                </li>
              )}
            </>
          )}

          {/* Auth toggle */}
          <li>
            {isAuthenticated ? (
              <Link to="/login" onClick={handleLogout}>
                <img
                  className="homeIcon"
                  src="src/assets/logout-512 (1).png"
                  alt="Logout"
                />
              </Link>
            ) : (
              <Link to="/login" onClick={handleLinkClick}>
                <img
                  className="homeIcon"
                  src="src/assets/login-icon.png"
                  alt="Login"
                />
              </Link>
            )}
          </li>
        </ul>
      </aside>

      <StudentPracticePopup
        visible={showPractice && isAuthenticated && isStudent}
        onClose={() => setShowPractice(false)}
      />
    </>
  );
};

export default Sidebar;
