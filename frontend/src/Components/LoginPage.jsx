import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../Context/UserContext';  // Import useUser from Context
import '../App.css';

const LoginPage = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const { login } = useUser();  // Access the login function from context
    const navigate = useNavigate();

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

        
            const token = response.data.token; 
            if (token) {
                const userData = response.data.user;  
                login(userData);  

                alert('Login successful');
                setEmail('');
                setPassword('');
                navigate('/');  // Redirect to homepage or user dashboard
            } else {
                alert('Login failed: No token received');
            }
        } catch (error) {
            alert('Login failed: ' + error.message);
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
