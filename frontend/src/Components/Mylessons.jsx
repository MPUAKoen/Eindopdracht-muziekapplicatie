import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext'; // Import the useUser hook
import '../App.css';

const MyLessons = () => {
    const { user, loading } = useUser(); // Get current user and loading state
    const [teachers, setTeachers] = useState([]); // State to store available teachers
    const [selectedTeacher, setSelectedTeacher] = useState(null); // State to store selected teacher
    const [lessons, setLessons] = useState([/* Example lessons */]);

    // Fetch teachers when the component mounts and user has no teacher assigned (if user is not a teacher)
    useEffect(() => {
        if (!loading && user && user.role !== 'TEACHER') {
            // Fetch the list of teachers from backend on port 8080
            fetch('http://localhost:8080/api/user/teachers', {
                credentials: 'include' // Include credentials (cookies)
            })
                .then((res) => {
                    if (res.status === 204) {
                        return [];
                    }
                    if (!res.ok) {
                        throw new Error('Failed to fetch teachers');
                    }
                    return res.json(); // Parse the response as JSON
                })
                .then((data) => {
                    console.log('Teachers data:', data); // Log the teachers data
                    setTeachers(data);  // Populate the teachers list
                })
                .catch((err) => {
                    console.error('Error fetching teachers:', err);
                    setTeachers([]);  // Clear the teacher list or set an error state
                });
        }
    }, [user, loading]);

    // Handle teacher selection
    const handleTeacherSelect = (e) => {
        setSelectedTeacher(e.target.value);
    };

    // Handle assigning the selected teacher to the student
    const handleTeacherAssign = () => {
        if (selectedTeacher) {
            fetch(`http://localhost:8080/api/user/assign-teacher/${selectedTeacher}`, {
                method: 'PATCH',
                credentials: 'include' // Include credentials (cookies)
            })
                .then((res) => {
                    if (!res.ok) {
                        throw new Error('Failed to assign teacher');
                    }
                    return res.json();
                })
                .then((data) => {
                    // Optionally update user context or state here
                    console.log('Assigned teacher:', data);
                    setSelectedTeacher(null); // Clear the selection
                })
                .catch((err) => {
                    console.error('Error assigning teacher:', err);
                });
        }
    };

    // Show loading message while fetching user data
    if (loading) return <div>Loading...</div>;

    return (
        <div className="mainpage">
            <div className="header">
                <h1>My Lessons</h1>
            </div>

            <div className="dashboard">
                {/* Check if the user is a teacher */}
                {user?.role === 'TEACHER' ? (
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
                ) : (
                    <div className="no-teacher-message">
                        <p>You don't have a teacher yet. Please select a teacher so they can add lessons for you.</p>
                        <select
                            value={selectedTeacher || ''}
                            onChange={handleTeacherSelect}
                            className="teacher-dropdown"
                        >
                            <option value="">Select a teacher</option>
                            {teachers && teachers.length > 0 ? (
                                teachers.map((teacher) => (
                                    <option key={teacher.id} value={teacher.id}>
                                        {teacher.name} ({teacher.instrument})
                                    </option>
                                ))
                            ) : (
                                <option>No teachers available</option>
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
