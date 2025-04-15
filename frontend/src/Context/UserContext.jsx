// src/Context/UserContext.jsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const UserContext = createContext();
export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios.get('http://localhost:8080/api/user/current', { withCredentials: true })
      .then(response => {
        setUser(response.data);
      })
      .catch(error => {
        console.warn("Session not found:", error);
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
