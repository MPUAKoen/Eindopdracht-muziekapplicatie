import React, { useEffect, useState } from 'react';
import '../App.css';

const StudentOverview = () => {
    const [students, setStudents] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchStudents = () => {
        fetch('http://localhost:8080/api/user/all', { credentials: 'include' })
            .then(res => res.json())
            .then(data => {
                setStudents(data);
                setLoading(false);
            })
            .catch(err => {
                console.error("Error fetching students:", err);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchStudents();
    }, []);

    const toggleUserRole = (userId) => {
        fetch(`http://localhost:8080/api/user/toggle-role/${userId}`, {
            method: 'PATCH',
            credentials: 'include'
        })
            .then(res => {
                if (res.ok) {
                    fetchStudents(); // Refresh list
                } else {
                    alert("Failed to update user role.");
                }
            })
            .catch(err => console.error("Toggle role error:", err));
    };

    const deleteUser = (userId) => {
        if (!window.confirm("Are you sure you want to delete this user?")) return;

        fetch(`http://localhost:8080/api/user/delete/${userId}`, {
            method: 'DELETE',
            credentials: 'include'
        })
            .then(res => {
                if (res.ok) {
                    fetchStudents(); // Refresh list
                } else {
                    alert("Failed to delete user.");
                }
            })
            .catch(err => console.error("Delete error:", err));
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="app-container">
            <div className="mainpage">
                <div className="header">
                    <h1>Admin Dashboard</h1>
                </div>

                <div className="table-container">
                    <table className="table">
                        <caption>Users</caption>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Instrument</th>
                                <th>Role</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {students.map((student, index) => (
                                <tr key={index}>
                                    <td>{student.name}</td>
                                    <td>{student.email}</td>
                                    <td>{student.instrument || 'N/A'}</td>  
                                    <td>{student.role}</td>
                                    <td>
                                        <button onClick={() => toggleUserRole(student.id)}>
                                            {student.role === 'TEACHER' ? 'Demote to Student' : 'Promote to Teacher'}
                                        </button>
                                        <button
                                            onClick={() => deleteUser(student.id)}
                                            style={{
                                                marginLeft: '8px',
                                                backgroundColor: 'red',
                                                color: 'white'
                                            }}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <div className="pagination">
                        <button disabled>Previous</button>
                        <span>Page 1 of 1</span>
                        <button disabled>Next</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StudentOverview;
