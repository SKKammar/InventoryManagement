import { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  axios.defaults.baseURL = 'http://localhost:8080';
  axios.defaults.withCredentials = true;

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    try {
      const { data } = await axios.get('/api/auth/me');
      setUser(data);
    } catch (error) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const login = async (username, password) => {
    const { data } = await axios.post('/api/auth/login', { username, password });
    setUser(data);
  };

  const logout = async () => {
    await axios.post('/api/auth/logout');
    setUser(null);
  };

  const register = async (username, email, password) => {
    const { data } = await axios.post('/api/auth/register', { username, email, password });
    setUser(data);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, register, loading }}>
      {children}
    </AuthContext.Provider>
  );
};
