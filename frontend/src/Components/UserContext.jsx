import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const UserContext = createContext();
export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    console.log("Fetching current user...");
    axios.get('http://localhost:8080/api/user/current', { withCredentials: true })
      .then(response => {
        console.log("User from backend:", response.data);
        setUser(response.data);
      })
      .catch(error => {
        console.error("No active session found:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const login = (userData) => setUser(userData);
  const logout = () => setUser(null);

  return (
    <UserContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </UserContext.Provider>
  );
};
