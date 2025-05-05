import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const UserContext = createContext();
export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    console.log("Fetching current user...");
    
    // Fetch the current user with credentials (cookies)
    axios.get('http://localhost:8080/api/user/current', { withCredentials: true })
      .then(response => {
        console.log("User from backend:", response.data);  // Log the response data
        setUser(response.data);  // Set user data from the response
      })
      .catch(error => {
        console.warn("Session not found:", error);  // Log any errors
      })
      .finally(() => {
        setLoading(false);  // Set loading state to false after request completes
      });
  }, []);

  // Login function to set the user context
  const login = (userData) => setUser(userData);
  
  // Logout function to clear user context
  const logout = () => setUser(null);

  return (
    <UserContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </UserContext.Provider>
  );
};
