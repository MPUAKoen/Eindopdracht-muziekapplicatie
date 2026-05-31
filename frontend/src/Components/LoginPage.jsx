import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { useUser } from '../Context/UserContext';
import { API_BASE } from '../lib/auth';
import Button from './ui/Button';
import Input from './ui/Input';
import StatusMessage from './ui/StatusMessage';
import '../App.css';

const LoginPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useUser();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email || !password) {
      setError('Please fill in all fields');
      return;
    }

    setIsLoading(true);

    try {
      const res = await axios.post(
        `${API_BASE}/api/auth/login`,
        { email, password }
      );

      if (res.status === 200 && res.data?.user?.email) {
        login(res.data);
        setEmail('');
        setPassword('');
        navigate('/');
      } else {
        setError('Invalid email or password');
      }
    } catch (err) {
      if (err.response?.status === 400 || err.response?.status === 401 || err.response?.status === 403) {
        setError('Invalid email or password');
      } else if (err.response?.status === 500) {
        setError('Server error. Please try again later.');
      } else {
        setError('Unable to log in. Please check your connection.');
      }

      console.error('Login failed:', err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mainpage">
      <div className="header">
        <h1>Login</h1>
      </div>

      <div className="dashboard">
        <form onSubmit={handleSubmit} className="lesson-form">
          <div className="form-group">
            <div className="formTitle">Log in to your account</div>

            <StatusMessage message={error} type="error" />

            <Input
              id="email"
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              required
              disabled={isLoading}
            />

            <Input
              id="password"
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
              disabled={isLoading}
            />

            <Button type="submit" className="submit-btn" disabled={isLoading}>
              {isLoading ? 'Logging in...' : 'Login'}
            </Button>

            <div
              className="redirect-link"
              style={{ marginTop: '20px', textAlign: 'center', color: 'white' }}
            >
              Don’t have an account yet?{' '}
              <Link to="/register">Register here</Link>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
