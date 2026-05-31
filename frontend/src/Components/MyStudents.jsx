// src/Pages/MyStudents.jsx
import React, { useEffect, useMemo, useState } from 'react';
import { useUser } from '../Context/UserContext';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { API_BASE, getAuthAxiosConfig } from '../lib/auth';
import Button from './ui/Button';
import Input from './ui/Input';
import StatusMessage from './ui/StatusMessage';
import '../App.css';

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
      .get(`${API_BASE}/api/teachers/me/students`, getAuthAxiosConfig())
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
      await axios.delete(
        `${API_BASE}/api/teachers/me/students/${studentId}`,
        getAuthAxiosConfig()
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
              <Input
                id="student-search"
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by name, email, or instrument…"
                aria-label="Filter students"
              />
            </div>

            <StatusMessage message={error} type="error" />

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
                        <td >{s.email}</td>
                        <td>{s.instrument || '—'}</td>
                        <td>
                          <Button
                            onClick={(e) => {
                              e.stopPropagation(); // prevent navigation
                              handleUnassign(s.id);
                            }}
                          >
                            Unassign
                          </Button>
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
