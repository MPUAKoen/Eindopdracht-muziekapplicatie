import React from 'react';
import '../App.css';

const workingOnPieces = [
    {
        title: "'Notte giorno faticar'",
        focus: "Focus on dynamics and phrasing."
    },
    {
        title: "'Ein Mädchen oder Weibchen'",
        focus: "Perfect the high notes."
    },
    {
        title: "'Pa-Pa-Papagena'",
        focus: "Work on duet timing"
    }
];

const Homepage = () => {
    return (
        <div className="mainpage">
            {/* Consistent Header */}
            <div className="header">
                <h1>Home</h1>
            </div>

            <div className="homepage-content">
                {/* Widgets Container */}
                <div className="widgets-container">
                    {/* Table 1: Welcome Message */}
                    <table className="widget-table">
                        <caption>Welcome Back, Koen!</caption>
                        <tbody>
                        <tr>
                            <td>Here’s a quick overview of your account.</td>
                        </tr>
                        </tbody>
                    </table>

                    {/* Table 2: Upcoming Lessons */}
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

                    {/* Table 3: Recent Homework */}
                    <table className="widget-table">
                        <caption>Recent Homework</caption>
                        <tbody>
                        <tr>
                            <th>Date</th>
                            <th>Assignment</th>
                        </tr>
                        <tr>
                            <td>19-01</td>
                            <td>Complete your last assignment on "Music Theory" to stay on track.</td>
                        </tr>
                        </tbody>
                    </table>

                    {/* Table 4: Working on Pieces */}
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
                </div>
            </div>
        </div>
    );
};

export default Homepage;