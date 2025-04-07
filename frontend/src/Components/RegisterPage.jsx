import React, { useState } from 'react';
import axios from 'axios';
import '../App.css';

const RegisterPage = () => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [instrument, setInstrument] = useState(null); // State for instrument

    const handleNameChange = (e) => setName(e.target.value);
    const handleEmailChange = (e) => setEmail(e.target.value);
    const handlePasswordChange = (e) => setPassword(e.target.value);
    const handleConfirmPasswordChange = (e) => setConfirmPassword(e.target.value);
    const handleInstrumentChange = (e) => setInstrument(e.target.value || null); // Set instrument to null if "empty" is selected

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Client-side validation
        if (!name || !email || !password || !confirmPassword) {
            alert('Please fill in all fields');
            return;
        }
        if (password !== confirmPassword) {
            alert('Passwords do not match');
            return;
        }

        try {
            // Send registration data to backend
            const response = await axios.post('http://localhost:8080/api/user/register', {
                name,
                email,
                password,
                instrument // Send instrument selection
            }, {
                withCredentials: true
            });

            // Handle success
            alert(response.data);
            setName('');
            setEmail('');
            setPassword('');
            setConfirmPassword('');
            setInstrument(null); // Reset instrument after submission

        } catch (error) {
            // Handle errors
            const errorMessage = error.response?.data || 'Registration failed';
            alert(errorMessage);
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
                    </div>

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

                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirm Password</label>
                        <input
                            type="password"
                            id="confirmPassword"
                            value={confirmPassword}
                            onChange={handleConfirmPasswordChange}
                            placeholder="Confirm your password"
                            required
                        />
                    </div>

                    {/* Dropdown for instrument selection */}
                    <div className="form-group">
                        <label htmlFor="instrument">Instrument</label>
                        <select
                            id="instrument"
                            value={instrument}
                            onChange={handleInstrumentChange}
                        >
                            <option value={null}>Empty</option>
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
                            {/* Add more instruments as needed */}
                        </select>
                    </div>

                    <button type="submit" className="submit-btn">Register</button>
                </form>
            </div>
        </div>
    );
};

export default RegisterPage;
