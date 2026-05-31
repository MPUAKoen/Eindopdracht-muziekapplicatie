import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUser } from '../Context/UserContext';
import axios from 'axios';
import { API_BASE } from '../lib/auth';
import Button from './ui/Button';
import Input from './ui/Input';
import Select from './ui/Select';
import '../App.css';

const INSTRUMENT_OPTIONS = [
    'Not sure',
    'Piano',
    'Guitar',
    'Violin',
    'Drums',
    'Flute',
    'Saxophone',
    'Classical Singing',
    'Bassoon',
    'Cello',
    'Trumpet',
    'Clarinet',
    'Harp',
    'Timpani',
    'French Horn',
    'Tuba'
];

const RegisterPage = () => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [instrument, setInstrument] = useState('');

    const navigate = useNavigate();
    const { login } = useUser();

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
            const response = await axios.post(`${API_BASE}/api/auth/register`, {
                name,
                email,
                password,
                instrument
            });

            if ((response.status === 200 || response.status === 201) && response.data?.user) {
                login(response.data);
                navigate('/');
            }

            setName('');
            setEmail('');
            setPassword('');
            setConfirmPassword('');
            setInstrument('');

        } catch (error) {
            console.error('Registration error:', error);
            alert('You already have an account!');
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

                        <Input
                            id="name"
                            label="Name"
                            value={name}
                            onChange={handleNameChange}
                            placeholder="Enter your name"
                            required
                        />

                        <Input
                            id="email"
                            label="Email"
                            type="email"
                            value={email}
                            onChange={handleEmailChange}
                            placeholder="Enter your email"
                            required
                        />

                        <Input
                            id="password"
                            label="Password"
                            type="password"
                            value={password}
                            onChange={handlePasswordChange}
                            placeholder="Enter your password"
                            required
                        />

                        <Input
                            id="confirmPassword"
                            label="Confirm Password"
                            type="password"
                            value={confirmPassword}
                            onChange={handleConfirmPasswordChange}
                            placeholder="Confirm your password"
                            required
                        />

                        <Select
                            id="instrument"
                            label="Instrument"
                            value={instrument}
                            onChange={handleInstrumentChange}
                            options={INSTRUMENT_OPTIONS}
                            placeholder="Select Instrument"
                        />

                        <Button type="submit" className="submit-btn">Register</Button>
                    </div>
                </form>

                <div style={{ marginTop: '20px', textAlign: 'center', color: '#ffd700', fontWeight: 'bold' }}>
                    Want to become a teacher? Please contact the administrator.
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;
