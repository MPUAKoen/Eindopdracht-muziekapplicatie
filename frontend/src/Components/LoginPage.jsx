import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom'; // <-- import Link
import { useUser } from '../Context/UserContext';
import '../App.css';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const { login } = useUser();
    const navigate = useNavigate();

    const handleEmailChange = (e) => setEmail(e.target.value);
    const handlePasswordChange = (e) => setPassword(e.target.value);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!email || !password) {
            setError('Please fill in all fields');
            return;
        }

        setIsLoading(true);

        try {
            const loginResponse = await axios.post(
                'http://localhost:8080/api/user/login',
                { email, password },
                { withCredentials: true }
            );

            if (loginResponse.status === 200 && loginResponse.data?.email) {
                login(loginResponse.data);
                setEmail('');
                setPassword('');
                navigate('/');
            } else {
                setError('Login failed: invalid response');
            }
        } catch (error) {
            console.error('Login error:', error);
            setError(
                error.response?.data?.message || error.message || 'Login failed. Please try again.'
            );
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

                        {error && <div className="error-message">{error}</div>}

                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input
                                type="email"
                                id="email"
                                value={email}
                                onChange={handleEmailChange}
                                placeholder="Enter your email"
                                required
                                disabled={isLoading}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Password</label>
                            <input
                                type="password"
                                id="password"
                                value={password}
                                onChange={handlePasswordChange}
                                placeholder="Enter your password"
                                required
                                disabled={isLoading}
                            />
                        </div>

                        <button type="submit" className="submit-btn" disabled={isLoading}>
                            {isLoading ? 'Logging in...' : 'Login'}
                        </button>

                        {/* Add this section */}
                        <div className="redirect-link" style={{ marginTop: '20px', textAlign: 'center', color:'white' }}>
                            Don't have an account yet?{' '}
                            <Link to="/register">Register here</Link>
                        </div>

                    </div>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
