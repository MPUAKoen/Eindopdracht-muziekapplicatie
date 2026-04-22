import React, { createContext, useContext, useEffect, useState } from 'react';
import { API_BASE, authFetch, clearToken, getToken, normalizeAuthPayload, setToken } from '../lib/auth';

const UserContext = createContext();

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setLoading(false);
      return;
    }

    authFetch(`${API_BASE}/api/user/current`)
      .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
      .then((currentUser) => setUser(currentUser))
      .catch(() => {
        clearToken();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = (payload) => {
    const { token, user: nextUser } = normalizeAuthPayload(payload);
    if (token) {
      setToken(token);
    }
    setUser(nextUser);
  };

  const logout = async () => {
    try {
      await authFetch(`${API_BASE}/api/user/logout`, { method: 'POST' });
    } catch (err) {
      console.error('Logout error:', err);
    }

    clearToken();
    setUser(null);
  };

  return (
    <UserContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </UserContext.Provider>
  );
};
