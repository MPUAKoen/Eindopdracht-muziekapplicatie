import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import { Link } from 'react-router-dom';
import '../App.css';

const API_BASE = 'http://localhost:8080';

export default function MyLessons() {
  const { user, loading, login } = useUser();
  const [teachers, setTeachers] = useState([]);
  const [selectedTeacher, setSelectedTeacher] = useState('');
  const [myLessons, setMyLessons] = useState([]);
  const [viewMode, setViewMode] = useState('day');
  const [selectedDate, setSelectedDate] = useState(() => new Date().toISOString().split('T')[0]);
  const role = user?.role?.toUpperCase();

  // Fetch lessons
  useEffect(() => {
    if (loading || !user) return;
    const path = role === 'TEACHER' ? '/api/lesson/teacher' : '/api/lesson/student';

    fetch(`${API_BASE}${path}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`Failed: ${res.status}`);
        return res.json();
      })
      .then(data => {
        const sorted = Array.isArray(data)
          ? data.sort(
              (a, b) =>
                new Date(`${a.lessonDate}T${a.startTime}`) -
                new Date(`${b.lessonDate}T${b.startTime}`)
            )
          : [];
        setMyLessons(sorted);
      })
      .catch(err => {
        console.error('Error loading lessons:', err);
        setMyLessons([]);
      });
  }, [user, loading, role]);

  // Fetch teachers (for students)
  useEffect(() => {
    if (loading) return;
    if (user && role !== 'TEACHER' && !user.teacher) {
      fetch(`${API_BASE}/api/user/teachers`, { credentials: 'include' })
        .then(res => (res.status === 204 ? [] : res.json()))
        .then(setTeachers)
        .catch(() => setTeachers([]));
    }
  }, [user, loading, role]);

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

  // Filter by date or week
  const filteredLessons = myLessons.filter(lesson => {
    const lessonDate = new Date(lesson.lessonDate);
    const selected = new Date(selectedDate);

    if (viewMode === 'day') {
      return (
        lessonDate.getFullYear() === selected.getFullYear() &&
        lessonDate.getMonth() === selected.getMonth() &&
        lessonDate.getDate() === selected.getDate()
      );
    } else {
      const startOfWeek = new Date(selected);
      startOfWeek.setDate(selected.getDate() - selected.getDay());
      const endOfWeek = new Date(startOfWeek);
      endOfWeek.setDate(startOfWeek.getDate() + 6);
      return lessonDate >= startOfWeek && lessonDate <= endOfWeek;
    }
  });

  const handleDeleteLesson = async (lessonId) => {
    if (!lessonId) return;
    if (!window.confirm('Delete this lesson?')) return;

    try {
      const res = await fetch(`${API_BASE}/api/lesson/${lessonId}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      if (!res.ok) throw new Error(`Failed with status ${res.status}`);
      setMyLessons(prev => prev.filter(l => (l.id ?? l.lessonId) !== lessonId));
    } catch (e) {
      console.error('Delete failed:', e);
      alert('Could not delete lesson.');
    }
  };

  if (loading) return <div>Loading…</div>;

  // Format date or week label
  const renderDateLabel = () => {
    if (viewMode === 'day') {
      return new Date(selectedDate).toLocaleDateString(undefined, {
        weekday: 'long',
        day: 'numeric',
        month: 'short',
      });
    } else {
      const start = new Date(selectedDate);
      const end = new Date(start);
      start.setDate(start.getDate() - start.getDay());
      end.setDate(start.getDate() + 6);
      const fmt = (d) =>
        d.toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
      return `${fmt(start)} – ${fmt(end)}`;
    }
  };

  const handlePrev = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() - (viewMode === 'week' ? 7 : 1));
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  const handleNext = () => {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + (viewMode === 'week' ? 7 : 1));
    setSelectedDate(d.toISOString().split('T')[0]);
  };

  return (
    <div className="mainpage">
      <div className="header">
        <h1>My Lessons</h1>
      </div>

      <div className="dashboard">
        {/* Centered title-style date navigation */}
        <div className="lesson-date-header">
          <button onClick={handlePrev} className="lesson-arrow">‹</button>
          <h2 className="lesson-title">{renderDateLabel()}</h2>
          <button onClick={handleNext} className="lesson-arrow">›</button>
          <button
            className="lesson-toggle-right"
            onClick={() => setViewMode(viewMode === 'day' ? 'week' : 'day')}
          >
            <img src="src/assets/calendar.png" alt="Toggle View" />
            {viewMode === 'day' ? 'Week View' : 'Day View'}
          </button>
        </div>

        {role === 'TEACHER' ? (
          <table className="table">
            <caption>Scheduled Lessons</caption>
            <thead>
              <tr>
                <th>Instrument</th>
                <th>Student</th>
                <th>Date</th>
                <th>Start</th>
                <th>End</th>
                <th>Homework</th>
                <th>PDFs</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredLessons.length > 0 ? (
                filteredLessons.map((lesson) => (
                  <tr key={lesson.id}>
                    <td>{lesson.instrument}</td>
                    <td>{lesson.student?.name ?? lesson.studentName}</td>
                    <td>{lesson.lessonDate}</td>
                    <td>{lesson.startTime}</td>
                    <td>{lesson.endTime}</td>
                    <td>
                      {lesson.id ? (
                        <Link to={`/homework/${lesson.id}`}>
                          <button className="submit-btn">View Homework</button>
                        </Link>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td>
                      {lesson.pdfFileNames?.length > 0
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
                    <td>
                      <button
                        onClick={() => handleDeleteLesson(lesson.id)}
                        style={{ background: 'crimson', color: 'white' }}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="8" style={{ textAlign: 'center' }}>
                    No lessons found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        ) : user.teacher ? (
          <>
            <p>
              Your assigned teacher: <strong>{user.teacher.name}</strong> ({user.teacher.instrument})
            </p>
            <table className="table">
              <caption>Scheduled Lessons</caption>
              <thead>
                <tr>
                  <th>Instrument</th>
                  <th>Date</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Homework</th>
                  <th>PDFs</th>
                </tr>
              </thead>
              <tbody>
                {filteredLessons.length > 0 ? (
                  filteredLessons.map((lesson) => (
                    <tr key={lesson.id}>
                      <td>{lesson.instrument}</td>
                      <td>{lesson.lessonDate}</td>
                      <td>{lesson.startTime}</td>
                      <td>{lesson.endTime}</td>
                      <td>
                        {lesson.id ? (
                          <Link to={`/homework/${lesson.id}`}>
                            <button className="submit-btn">View Homework</button>
                          </Link>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td>
                        {lesson.pdfFileNames?.length > 0
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
                ) : (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center' }}>
                      No lessons found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </>
        ) : (
          <div className="assign-teacher">
            <h2>Select a Teacher</h2>
            <select
              value={selectedTeacher}
              onChange={(e) => setSelectedTeacher(e.target.value)}
            >
              <option value="">-- Select --</option>
              {teachers.map((t) => (
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
}
