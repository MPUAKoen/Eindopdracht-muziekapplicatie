import React, { useState } from 'react';
import '../App.css';

const AllLessonsPage = () => {
    const [lessons, setLessons] = useState([
        { student: 'John Doe', instrument: 'Piano', date: '2024-02-05', startTime: '10:00', endTime: '11:00', homework: 'Practice scales' },
        { student: 'Jane Smith', instrument: 'Guitar', date: '2024-02-06', startTime: '12:00', endTime: '13:00', homework: 'Strumming exercises' },
        { student: 'Alice Brown', instrument: 'Violin', date: '2024-02-07', startTime: '14:00', endTime: '15:00', homework: 'Bow control' },
    ]);

    return (
        <div className="mainpage">
            <div className="header">
                <h1>My lessons</h1>
            </div>

            <div className="dashboard">
                <table className="lesson-table">
                    <thead>
                    <tr>
                        <th>Student</th>
                        <th>Instrument</th>
                        <th>Date</th>
                        <th>Start Time</th>
                        <th>End Time</th>
                        <th>Homework</th>
                    </tr>
                    </thead>
                    <tbody>
                    {lessons.map((lesson, index) => (
                        <tr key={index}>
                            <td>{lesson.student}</td>
                            <td>{lesson.instrument}</td>
                            <td>{lesson.date}</td>
                            <td>{lesson.startTime}</td>
                            <td>{lesson.endTime}</td>
                            <td>{lesson.homework}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default AllLessonsPage;
