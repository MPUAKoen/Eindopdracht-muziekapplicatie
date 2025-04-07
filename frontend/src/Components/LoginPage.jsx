import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';  // Import useNavigate
import '../App.css';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();  // Initialize the navigate function

    const handleEmailChange = (e) => setEmail(e.target.value);
    const handlePasswordChange = (e) => setPassword(e.target.value);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!email || !password) {
            alert('Please fill in all fields');
            return;
        }

        try {
            const response = await axios.post('http://localhost:8080/api/user/login', {
                email,
                password
            }, {
                withCredentials: true 
            });

            alert(response.data); // e.g. "Login successful"
            setEmail('');
            setPassword('');

            // Redirect to the homepage after successful login
            navigate('/'); 
        } catch (error) {
            const errorMessage = error.response?.data || 'Login failed';
            alert(errorMessage);
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

                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input
                                type="email"
                                id="email"
                                value={email}
                                onChange={handleEmailChange}
                                placeholder="Enter your email"
                                required
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
                            />
                        </div>

                        <button type="submit" className="submit-btn">Login</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
