export const API_BASE = 'http://localhost:8080';

const TOKEN_STORAGE_KEY = 'music-app-token';

export const getToken = () => window.localStorage.getItem(TOKEN_STORAGE_KEY);

export const setToken = (token) => {
  if (token) {
    window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
    return;
  }

  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
};

export const clearToken = () => {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
};

export const getAuthHeaders = (headers = {}) => {
  const token = getToken();
  if (!token) {
    return { ...headers };
  }

  return {
    ...headers,
    Authorization: `Bearer ${token}`,
  };
};

export const authFetch = (input, init = {}) => {
  const { headers, ...rest } = init;
  return fetch(input, {
    ...rest,
    headers: getAuthHeaders(headers),
  });
};

export const getAuthAxiosConfig = (config = {}) => ({
  ...config,
  headers: getAuthHeaders(config.headers || {}),
});

export const normalizeAuthPayload = (payload) => {
  if (!payload) {
    return { token: null, user: null };
  }

  if (payload.token && payload.user) {
    return {
      token: payload.token,
      user: payload.user,
    };
  }

  return {
    token: null,
    user: payload,
  };
};
