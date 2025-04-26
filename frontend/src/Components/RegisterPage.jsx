import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom'; // <-- IMPORT useNavigate
import { useUser } from '../Context/UserContext'; // <-- IMPORT useUser to save user after registration
import axios from 'axios';
import '../App.css';

const RegisterPage = () => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [instrument, setInstrument] = useState('');

    const navigate = useNavigate(); // <-- INITIALIZE navigate
    const { login } = useUser();     // <-- Get login() from context

    const handleNameChange = (e) => setName(e.target.value);
    const handleEmailChange = (e) => setEmail(e.target.value);
    const handlePasswordChange = (e) => setPassword(e.target.value);
    const handleConfirmPasswordChange = (e) => setConfirmPassword(e.target.value);
    const handleInstrumentChange = (e) => setInstrument(e.target.value);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!name || !email || !password || !confirmPassword) {
            alert('Please fill in all fields');
            return;
        }
        if (password !== confirmPassword) {
            alert('Passwords do not match');
            return;
        }

        try {
            const response = await axios.post('http://localhost:8080/api/user/register', {
                name,
                email,
                password,
                instrument
            }, {
                withCredentials: true
            });

            if (response.status === 200) {
                login(response.data);    // Save the user into context
                navigate('/');        // Redirect to home page
            }

            setName('');
            setEmail('');
            setPassword('');
            setConfirmPassword('');
            setInstrument('');

        } catch (error) {
            console.error('Registration error:', error);
            alert('Registration failed. Please try again.');
        }
    };

    return (
        <div className="mainpage">
            <div className="header">
                <h1>Register</h1>
            </div>

            <div className="dashboard">
                <form onSubmit={handleSubmit} className="lesson-form">
                    <div className="form-group">
                        <div className="formTitle">Create an account</div>

                        <label htmlFor="name">Name</label>
                        <input
                            type="text"
                            id="name"
                            value={name}
                            onChange={handleNameChange}
                            placeholder="Enter your name"
                            required
                        />

                        <label htmlFor="email">Email</label>
                        <input
                            type="email"
                            id="email"
                            value={email}
                            onChange={handleEmailChange}
                            placeholder="Enter your email"
                            required
                        />

                        <label htmlFor="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            value={password}
                            onChange={handlePasswordChange}
                            placeholder="Enter your password"
                            required
                        />

                        <label htmlFor="confirmPassword">Confirm Password</label>
                        <input
                            type="password"
                            id="confirmPassword"
                            value={confirmPassword}
                            onChange={handleConfirmPasswordChange}
                            placeholder="Confirm your password"
                            required
                        />

                        <label htmlFor="instrument">Instrument</label>
                        <select
                            id="instrument"
                            value={instrument}
                            onChange={handleInstrumentChange}
                        >
                            <option value="">Select Instrument</option>
                            <option value="Not sure">Not sure</option>
                            <option value="Piano">Piano</option>
                            <option value="Guitar">Guitar</option>
                            <option value="Violin">Violin</option>
                            <option value="Drums">Drums</option>
                            <option value="Flute">Flute</option>
                            <option value="Saxophone">Saxophone</option>
                            <option value="Classical Singing">Classical Singing</option>
                            <option value="Bassoon">Bassoon</option>
                            <option value="Cello">Cello</option>
                            <option value="Trumpet">Trumpet</option>
                            <option value="Clarinet">Clarinet</option>
                            <option value="Harp">Harp</option>
                            <option value="Timpani">Timpani</option>
                            <option value="French Horn">French Horn</option>
                            <option value="Tuba">Tuba</option>
                        </select>

                        <button type="submit" className="submit-btn">Register</button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default RegisterPage;
