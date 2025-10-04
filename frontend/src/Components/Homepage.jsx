// src/Components/Homepage.jsx
import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const sortByDateAdded = (data) => {
  return data.sort((a, b) => {
    if (a.dateAdded && b.dateAdded) {
      return new Date(b.dateAdded) - new Date(a.dateAdded);
    }
    return 0;
  });
};

const itemsPerPage = 5;
const paginate = (data, currentPage, itemsPerPage) =>
  data.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);
const totalPages = (data) => Math.ceil(data.length / itemsPerPage);

const Homepage = () => {
  const { user, loading } = useUser();

  const [upcomingLessons, setUpcomingLessons] = useState([]);

  // Working on pieces state
  const [workingPieces, setWorkingPieces] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [title, setTitle] = useState('');
  const [composer, setComposer] = useState('');
  const [notes, setNotes] = useState('');

  // Load working on pieces from user
  useEffect(() => {
    if (user) {
      setWorkingPieces(user.workingOnPieces || []);
    }
  }, [user]);

  // Load upcoming lessons from backend
  useEffect(() => {
    if (loading || !user) return;

    const role = user.role?.toUpperCase();
    const path = role === 'TEACHER' ? '/api/lesson/teacher' : '/api/lesson/student';

    fetch(`${API_BASE}${path}`, { credentials: 'include' })
      .then(res => (res.ok ? res.json() : []))
      .then(data => {
        const toMs = (d, t) => Date.parse(`${d}T${t || '00:00:00'}`);
        const sorted = (Array.isArray(data) ? data : [])
          .slice()
          .sort((a, b) => toMs(a.lessonDate, a.startTime) - toMs(b.lessonDate, b.startTime));
        setUpcomingLessons(sorted.slice(0, 5));
      })
      .catch(() => setUpcomingLessons([]));
  }, [user, loading]);

  const addPiece = () => {
    if (!title || !composer) return;

    const newPiece = { title, composer, notes };

    fetch(`${API_BASE}/api/piece/add`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ ...newPiece, category: 'workingonpieces' })
    })
      .then(res => res.text())
      .then(() => {
        setWorkingPieces([...workingPieces, newPiece]);
        setTitle('');
        setComposer('');
        setNotes('');
      })
      .catch(err => console.error('Error adding piece:', err));
  };

  const deletePiece = (piece) => {
    fetch(`${API_BASE}/api/piece/delete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        title: piece.title,
        composer: piece.composer,
        notes: piece.notes,
        category: 'workingonpieces'
      })
    })
      .then(res => res.text())
      .then(() => {
        const updated = workingPieces.filter(
          p =>
            p.title !== piece.title ||
            p.composer !== piece.composer ||
            p.notes !== piece.notes
        );
        setWorkingPieces(updated);
      })
      .catch(err => console.error('Error deleting piece:', err));
  };

  if (loading) return <div>Loading user...</div>;

  const asLessonLabel = (lesson) => {
    const counterpart =
      user?.role?.toUpperCase() === 'TEACHER'
        ? lesson.student?.name ?? lesson.studentName
        : lesson.teacher?.name ?? lesson.teacherName;
    return `${lesson.instrument || 'Lesson'}${counterpart ? ` — ${counterpart}` : ''}`;
  };
  const asTimeLabel = (lesson) =>
    `${lesson.lessonDate} ${lesson.startTime ?? ''}${lesson.endTime ? `–${lesson.endTime}` : ''}`;

  return (
    <div className="mainpage">
      <div className="header">
        <h1>Home</h1>
      </div>

      <div className="homepage-content">
        <div className="widgets-container">
          {/* Welcome */}
          <table className="widget-table">
            <caption>Welcome Back, {user ? user.name : 'Guest'}!</caption>
            <tbody>
              <tr>
                <td>Here’s a quick overview of your account.</td>
              </tr>
            </tbody>
          </table>

          {/* Upcoming lessons */}
          <table className="widget-table">
            <caption>Upcoming Lessons</caption>
            <thead>
              <tr>
                <th>Lesson</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {upcomingLessons.length > 0 ? (
                upcomingLessons.map((lesson, i) => (
                  <tr key={lesson.id ?? `${i}`}>
                    <td>{asLessonLabel(lesson)}</td>
                    <td>{asTimeLabel(lesson)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="2" style={{ textAlign: 'center' }}>
                    No upcoming lessons
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Working on pieces */}
          <div className="table-wrapper">
            <table className="table">
              <caption>Pieces that I am learning</caption>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Composer</th>
                  <th>Notes</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {paginate(sortByDateAdded(workingPieces), currentPage, itemsPerPage).map((piece, index) => (
                  <tr key={index}>
                    <td>{piece.title}</td>
                    <td>{piece.composer}</td>
                    <td>{piece.notes}</td>
                    <td>
                      <button onClick={() => deletePiece(piece)}>Delete</button>
                    </td>
                  </tr>
                ))}

                {/* Input row inside the table */}
                <tr className="input-row">
                  <td>
                    <input
                      type="text"
                      placeholder="Title"
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      placeholder="Composer"
                      value={composer}
                      onChange={(e) => setComposer(e.target.value)}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      placeholder="Notes"
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                    />
                  </td>
                  <td>
                    <button className="add-piece-btn" onClick={addPiece}>
                      Add
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            {/* Pagination */}
            <div className="pagination">
              <button
                onClick={() => setCurrentPage(Math.max(currentPage - 1, 1))}
                disabled={currentPage === 1}
              >
                Previous
              </button>
              <span>
                Page {currentPage} of {totalPages(workingPieces)}
              </span>
              <button
                onClick={() =>
                  setCurrentPage(Math.min(currentPage + 1, totalPages(workingPieces)))
                }
                disabled={currentPage === totalPages(workingPieces)}
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Homepage;
