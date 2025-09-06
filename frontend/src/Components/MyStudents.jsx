// src/Pages/MyStudents.jsx
import React, { useEffect, useState } from 'react';
import { useUser } from '../Context/UserContext';
import axios from 'axios';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const MyStudents = () => {
  const { user, loading } = useUser();
  const [students, setStudents] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    if (loading) return;
    if (!user || user.role !== 'TEACHER') return;

    axios
      .get(`${API_BASE}/api/user/my-students`, { withCredentials: true })
      .then((res) => setStudents(res.data))
      .catch((err) => {
        console.error('Error fetching students:', err);
        setError('Failed to load students');
      });
  }, [user, loading]);

  if (loading) return <div>Loading…</div>;

  if (!user || user.role !== 'TEACHER') {
    return (
      <div className="mainpage">
        <div className="header">
          <h1>My Students</h1>
        </div>
        <p style={{ color: 'red' }}>Only teachers can view their students.</p>
      </div>
    );
  }

  return (
    <div className="mainpage">
      <div className="header">
        <h1>My Students</h1>
      </div>
      <div className="dashboard">
        {error && <p style={{ color: 'red' }}>{error}</p>}
        {students.length === 0 ? (
          <p>You currently have no students assigned.</p>
        ) : (
          <table className="table">
            <caption>Assigned Students</caption>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Instrument</th>
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>{s.email}</td>
                  <td>{s.instrument || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default MyStudents;
