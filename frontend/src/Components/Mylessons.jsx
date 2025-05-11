// src/Components/MyLessons.jsx
import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const MyLessons = () => {
  const { user, loading, login } = useUser();
  const [teachers, setTeachers] = useState([]);
  const [selectedTeacher, setSelectedTeacher] = useState('');
  const [lessons, setLessons] = useState([]);
  const [teacherLessons, setTeacherLessons] = useState([]);

  // Fetch available teachers (student-only)
  useEffect(() => {
    if (loading) return;
    if (user && user.role !== 'TEACHER' && !user.teacher) {
      fetch(`${API_BASE}/api/user/teachers`, {
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      })
        .then(res => (res.status === 204 ? [] : res.json()))
        .then(setTeachers)
        .catch(() => setTeachers([]));
    }
  }, [user, loading]);

  // Fetch student's lessons (student-only)
  useEffect(() => {
    if (loading) return;
    if (user && user.role !== 'TEACHER' && user.teacher) {
      fetch(`${API_BASE}/api/lesson/student`, { credentials: 'include' })
        .then(res => (res.ok ? res.json() : Promise.reject()))
        .then(setLessons)
        .catch(() => setLessons([]));
    }
  }, [user, loading]);

  // Fetch teacher's lessons (teacher-only)
  useEffect(() => {
    if (loading) return;
    if (user && user.role === 'TEACHER') {
      fetch(`${API_BASE}/api/lesson/teacher`, { credentials: 'include' })
        .then(res => (res.ok ? res.json() : Promise.reject()))
        .then(setTeacherLessons)
        .catch(() => setTeacherLessons([]));
    }
  }, [user, loading]);

  const handleTeacherSelect = e => setSelectedTeacher(e.target.value);
  const handleTeacherAssign = () => {
    if (!selectedTeacher) return;
    fetch(`${API_BASE}/api/user/assign-teacher/${selectedTeacher}`, {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    })
      .then(res => res.json())
      .then(updatedUser => {
        login(updatedUser);
        setSelectedTeacher('');
      })
      .catch(console.error);
  };

  if (loading) return <div>Loading…</div>;

  return (
    <div className="mainpage">
      <div className="header">
        <h1>My Lessons</h1>
      </div>
      <div className="dashboard">
        {user.role === 'TEACHER' ? (
          <div className="teacher-lessons">
            <h2>Your Scheduled Lessons</h2>
            <table className="table">
              <caption>Lessons You’re Teaching</caption>
              <thead>
                <tr>
                  <th>Instrument</th>
                  <th>Student</th>
                  <th>Date</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Homework</th>
                  <th>PDFs</th>
                </tr>
              </thead>
              <tbody>
                {teacherLessons.length > 0
                  ? teacherLessons.map((lesson, idx) => (
                      <tr key={lesson.id ?? idx}>
                        <td>{lesson.instrument}</td>
                        <td>{lesson.student.name}</td>
                        <td>{lesson.lessonDate}</td>
                        <td>{lesson.startTime}</td>
                        <td>{lesson.endTime}</td>
                        <td>{lesson.homework || '—'}</td>
                        <td>
                          {lesson.pdfFileNames && lesson.pdfFileNames.length > 0
                            ? lesson.pdfFileNames.map((file, i) => (
                                <div key={i}>
                                  <a
                                    href={`${API_BASE}/api/lesson/file/${file}`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                  >
                                    {file}
                                  </a>
                                </div>
                              ))
                            : 'No files'}
                        </td>
                      </tr>
                    ))
                  : (
                      <tr>
                        <td colSpan="7" style={{ textAlign: 'center' }}>
                          No lessons scheduled yet.
                        </td>
                      </tr>
                    )}
              </tbody>
            </table>
          </div>
        ) : user.teacher ? (
          <div className="assigned-teacher">
            <p>
              Your assigned teacher:{' '}
              <strong>{user.teacher.name}</strong> ({user.teacher.instrument})
            </p>
            <table className="table">
              <caption>My Lessons</caption>
              <thead>
                <tr>
                  <th>Instrument</th>
                  <th>Date</th>
                  <th>Start Time</th>
                  <th>End Time</th>
                  <th>Homework</th>
                  <th>PDFs</th>
                </tr>
              </thead>
              <tbody>
                {lessons.length > 0
                  ? lessons.map((lesson, idx) => (
                      <tr key={lesson.id ?? idx}>
                        <td>{lesson.instrument}</td>
                        <td>{lesson.lessonDate}</td>
                        <td>{lesson.startTime}</td>
                        <td>{lesson.endTime}</td>
                        <td>{lesson.homework || '—'}</td>
                        <td>
                          {lesson.pdfFileNames && lesson.pdfFileNames.length > 0
                            ? lesson.pdfFileNames.map((file, i) => (
                                <div key={i}>
                                  <a
                                    href={`${API_BASE}/api/lesson/file/${file}`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                  >
                                    {file}
                                  </a>
                                </div>
                              ))
                            : 'No files'}
                        </td>
                      </tr>
                    ))
                  : (
                      <tr>
                        <td colSpan="6" style={{ textAlign: 'center' }}>
                          No lessons scheduled yet.
                        </td>
                      </tr>
                    )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="assign-teacher">
            <h2>Select a Teacher</h2>
            {/* existing teacher-assignment UI */}
            <select
              value={selectedTeacher}
              onChange={handleTeacherSelect}
            >
              <option value="">-- Select --</option>
              {teachers.map(t => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.instrument})
                </option>
              ))}
            </select>
            <button onClick={handleTeacherAssign}>Assign Teacher</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default MyLessons;
