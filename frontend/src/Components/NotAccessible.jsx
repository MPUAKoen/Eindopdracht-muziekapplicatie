// src/Components/NotAccessible.jsx
import React from "react";
import { Link } from "react-router-dom";

const NotAccessible = () => {
  return (
    <div className="mainpage">
      <h1 className="formTitle">🚫 Access Denied</h1>
      <p className="profile-container">
        This page is not accessible for the logged-in user.
      </p>

      <Link to="/login">
        <button className="submit-btn">🔑 Login as other User</button>
      </Link>
    </div>
  );
};

export default NotAccessible;
