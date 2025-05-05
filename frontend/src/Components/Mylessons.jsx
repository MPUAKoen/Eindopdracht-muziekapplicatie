import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

// Use an absolute API base URL to avoid dev-server 404 issues
const API_BASE = 'http://localhost:8080';

const MyLessons = () => {
    const { user, loading, login } = useUser();
    const [teachers, setTeachers] = useState([]);
    const [selectedTeacher, setSelectedTeacher] = useState('');
    const [lessons, setLessons] = useState([]);

    // Fetch available teachers if student has no teacher
    useEffect(() => {
        if (!loading && user && user.role !== 'TEACHER' && !user.teacher) {
            fetch(`${API_BASE}/api/user/teachers`, {
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' }
            })
                .then((res) => {
                    if (res.status === 204) return [];
                    if (!res.ok) throw new Error('Failed to fetch teachers');
                    return res.json();
                })
                .then((data) => setTeachers(data))
                .catch((err) => {
                    console.error('Error fetching teachers:', err);
                    setTeachers([]);
                });
        }
    }, [user, loading]);

    const handleTeacherSelect = (e) => setSelectedTeacher(e.target.value);

    const handleTeacherAssign = () => {
        if (!selectedTeacher) return;

        fetch(`${API_BASE}/api/user/assign-teacher/${selectedTeacher}`, {
            method: 'PATCH',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' }
        })
            .then((res) => {
                if (!res.ok) throw new Error('Failed to assign teacher');
                return res.json();
            })
            .then((updatedUser) => {
                console.log('Assigned teacher:', updatedUser);
                // Refresh context so UI updates
                login(updatedUser);
                setSelectedTeacher('');
            })
            .catch((err) => console.error('Error assigning teacher:', err));
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="mainpage">
            <div className="header">
                <h1>My Lessons</h1>
            </div>
            <div className="dashboard">
                {user.role === 'TEACHER' ? (
                    <table className="table">
                        <caption>Lesson Schedule</caption>
                        <thead>
                            <tr>
                                <th>Student</th>
                                <th>Instrument</th>
                                <th>Date</th>
                                <th>Start Time</th>
                                <th>End Time</th>
                                <th>Homework</th>
                                <th>PDF Document</th>
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
                                    <td>
                                        <a href={`/path/to/pdfs/${lesson.pdfDocument}`} target="_blank" rel="noopener noreferrer">
                                            Sheet Music
                                        </a>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : user.teacher ? (
                    <div className="assigned-teacher">
                        <p>
                            Your assigned teacher: <strong>{user.teacher.name}</strong> ({user.teacher.instrument || 'No instrument specified'})
                        </p>
                    </div>
                ) : (
                    <div className="no-teacher-message">
                        <p>You don't have a teacher yet. Please select a teacher so they can add lessons for you.</p>
                        <select
                            value={selectedTeacher}
                            onChange={handleTeacherSelect}
                            className="teacher-dropdown"
                        >
                            <option value="">Select a teacher</option>
                            {teachers.length > 0 ? (
                                teachers.map((teacher) => (
                                    <option key={teacher.id} value={teacher.id}>
                                        {teacher.name} ({teacher.instrument})
                                    </option>
                                ))
                            ) : (
                                <option disabled>No teachers available</option>
                            )}
                        </select>
                        <button onClick={handleTeacherAssign} disabled={!selectedTeacher}>
                            Assign Teacher
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MyLessons;
