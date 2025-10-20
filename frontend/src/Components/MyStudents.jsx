// src/Pages/MyStudents.jsx
import React, { useEffect, useMemo, useState } from 'react';
import { useUser } from '../Context/UserContext';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const MyStudents = () => {
  const { user, loading } = useUser();
  const [students, setStudents] = useState([]);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const isTeacher = !!user && String(user.role).toUpperCase() === 'TEACHER';

  const loadStudents = () => {
    if (!isTeacher) return;
    setError('');
    axios
      .get(`${API_BASE}/api/user/my-students`, { withCredentials: true })
      .then((res) => setStudents(Array.isArray(res.data) ? res.data : []))
      .catch((err) => {
        console.error('Error fetching students:', err);
        setError('Failed to load students.');
        setStudents([]);
      });
  };

  useEffect(() => {
    if (!loading && isTeacher) loadStudents();
  }, [loading, isTeacher]);

  const handleUnassign = async (studentId) => {
    if (!studentId) return;
    if (!window.confirm('Unassign this student from you?')) return;
    try {
      await axios.patch(
        `${API_BASE}/api/user/unassign-student/${studentId}`,
        {},
        { withCredentials: true }
      );
      setStudents((prev) => prev.filter((s) => String(s.id) !== String(studentId)));
    } catch (err) {
      console.error('Unassign error:', err);
      alert('Could not unassign the student.');
    }
  };

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return students;
    return students.filter((s) =>
      (s.name || '').toLowerCase().includes(q) ||
      (s.email || '').toLowerCase().includes(q) ||
      (s.instrument || '').toLowerCase().includes(q)
    );
  }, [students, search]);

  if (loading) {
    return (
      <div className="mainpage">
        <div className="header"><h1>My Students</h1></div>
        <div className="dashboard">Loading…</div>
      </div>
    );
  }

  return (
    <div className="mainpage">
      <div className="header"><h1>My Students</h1></div>
      <div className="dashboard">
        {!isTeacher ? (
          <p>Only teachers can view their students.</p>
        ) : (
          <>
            <div className="search-bar">
              <input
                type="text"
                placeholder="Search by name, email, or instrument…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Filter students"
              />
            </div>

            {error && <p>{error}</p>}

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
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.length > 0 ? (
                    filtered.map((s) => (
                      <tr
                        key={s.id}
                        onClick={() => navigate(`/student/${s.id}`)}
                        style={{ cursor: 'pointer' }}
                      >
                        <td>{s.name}</td>
                        <td className="email-col">{s.email}</td>
                        <td>{s.instrument || '—'}</td>
                        <td>
                          <button
                            onClick={(e) => {
                              e.stopPropagation(); // prevent navigation
                              handleUnassign(s.id);
                            }}
                          >
                            Unassign
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="4" style={{ textAlign: 'center' }}>
                        No matches for “{search}”.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default MyStudents;
