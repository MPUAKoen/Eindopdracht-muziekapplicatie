import React from "react";
import { Navigate } from "react-router-dom";
import { useUser } from "../Context/UserContext";
import NotAccessible from "./NotAccessible";

const ProtectedRoute = ({ allowedRoles, children }) => {
  const { user, loading } = useUser(); // 👈 make sure UserContext provides loading

  // While checking session → show nothing or spinner
  if (loading) {
    return <div>Loading...</div>;
  }

  // If not logged in → go to login
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // If logged in but role not allowed → show NotAccessible
  if (allowedRoles && !allowedRoles.includes(user.role.toUpperCase())) {
    return <NotAccessible />;
  }

  return children;
};

export default ProtectedRoute;
