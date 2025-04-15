import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext'; // Import the useUser hook
import '../App.css';

const Homepage = () => {
    const [workingOnPieces, setWorkingOnPieces] = useState([]);
    const [newTitle, setNewTitle] = useState("");
    const [newFocus, setNewFocus] = useState("");
    const { user, loading } = useUser(); // ✅ include loading

    useEffect(() => {
        fetch("/api/pieces")
            .then((res) => res.json())
            .then((data) => setWorkingOnPieces(data));
    }, []);

    const handleAddPiece = () => {
        if (!newTitle || !newFocus) return;

        const newPiece = { title: newTitle, focus: newFocus };

        fetch("/api/pieces", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(newPiece),
        })
            .then((res) => res.text())
            .then(() => {
                setWorkingOnPieces([...workingOnPieces, newPiece]);
                setNewTitle("");
                setNewFocus("");
            });
    };

    if (loading) return <div>Loading user...</div>; // ✅ wait until user context is loaded

    return (
        <div className="mainpage">
            <div className="header">
                <h1>Home</h1>
            </div>

            <div className="homepage-content">
                <div className="widgets-container">
                    {/* Welcome Message */}
                    <table className="widget-table">
                        <caption>Welcome Back, {user ? user.name : "Guest"}!</caption> 
                        <tbody>
                            <tr>
                                <td>Here’s a quick overview of your account.</td>
                            </tr>
                        </tbody>
                    </table>

                    {/* Upcoming Lessons */}
                    <table className="widget-table">
                        <caption>Upcoming Lessons</caption>
                        <thead>
                            <tr>
                                <th>Lesson</th>
                                <th>Time</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>Lesson 1</td>
                                <td>10:00 AM - 11:00 AM</td>
                            </tr>
                            <tr>
                                <td>Lesson 2</td>
                                <td>2:00 PM - 3:00 PM</td>
                            </tr>
                        </tbody>
                    </table>

                    {/* Working on Pieces */}
                    <table className="widget-table">
                        <caption>Working on Pieces</caption>
                        <thead>
                            <tr>
                                <th>Piece</th>
                                <th>Focus</th>
                            </tr>
                        </thead>
                        <tbody>
                            {workingOnPieces.map((piece, index) => (
                                <tr key={index}>
                                    <td>{piece.title}</td>
                                    <td>{piece.focus}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    {/* Add New Piece Form */}
                    <div className="add-piece-form">
                        <input
                            type="text"
                            placeholder="Piece Title"
                            value={newTitle}
                            onChange={(e) => setNewTitle(e.target.value)}
                        />
                        <input
                            type="text"
                            placeholder="Focus"
                            value={newFocus}
                            onChange={(e) => setNewFocus(e.target.value)}
                        />
                        <button className="add-piece-btn" onClick={handleAddPiece}>Add Piece</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Homepage;
