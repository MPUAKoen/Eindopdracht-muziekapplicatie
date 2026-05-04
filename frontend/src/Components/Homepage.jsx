import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import { API_BASE, authFetch } from '../lib/auth';
import '../App.css';

const PIECES_URL = `${API_BASE}/api/pieces`;

const sortByDateAdded = (data) => {
  return [...data].sort((a, b) => {
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
  const [workingPieces, setWorkingPieces] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [title, setTitle] = useState('');
  const [composer, setComposer] = useState('');
  const [notes, setNotes] = useState('');

  const loadWorkingPieces = async () => {
    try {
      const res = await authFetch(`${PIECES_URL}?category=working-on-pieces`);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      setWorkingPieces(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error loading working pieces:', err);
      setWorkingPieces([]);
    }
  };

  // Load working on pieces from user
  useEffect(() => {
    if (user) {
      loadWorkingPieces();
      return;
    }

    setWorkingPieces([]);
    setUpcomingLessons([]);
  }, [user]);

  // Load only upcoming (future) lessons
  useEffect(() => {
    if (loading || !user) return;

    const role = user.role?.toUpperCase();
    const path = role === 'TEACHER' ? '/api/lessons?scope=teaching' : '/api/lessons?scope=learning';

    authFetch(`${API_BASE}${path}`)
      .then(res => (res.ok ? res.json() : []))
      .then(data => {
        const now = new Date();
        const toMs = (d, t) => Date.parse(`${d}T${t || '00:00:00'}`);

        const upcoming = (Array.isArray(data) ? data : [])
          .filter(l => {
            const lessonTime = toMs(l.lessonDate, l.startTime);
            return !isNaN(lessonTime) && lessonTime >= now.getTime();
          })
          .sort((a, b) => toMs(a.lessonDate, a.startTime) - toMs(b.lessonDate, b.startTime));

        setUpcomingLessons(upcoming.slice(0, 5));
      })
      .catch(() => setUpcomingLessons([]));
  }, [user, loading]);

  const addPiece = async () => {
    if (!title || !composer) return;

    const newPiece = { title, composer, notes };

    try {
      const res = await authFetch(PIECES_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...newPiece, category: 'working-on-pieces' })
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || `HTTP ${res.status}`);
      }

      setTitle('');
      setComposer('');
      setNotes('');
      await loadWorkingPieces();
    } catch (err) {
      console.error('Error adding piece:', err);
    }
  };

  const deletePiece = async (piece) => {
    if (!piece?.id) {
      console.error('Error deleting piece: missing piece id', piece);
      await loadWorkingPieces();
      return;
    }

    try {
      const res = await authFetch(`${PIECES_URL}/${piece.id}`, {
        method: 'DELETE',
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || `HTTP ${res.status}`);
      }

      await loadWorkingPieces();
    } catch (err) {
      console.error('Error deleting piece:', err);
    }
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

          {/* Upcoming Lessons */}
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

          {/* Working on Pieces */}
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
                  <tr key={piece.id ?? index}>
                    <td>{piece.title}</td>
                    <td>{piece.composer}</td>
                    <td>{piece.notes}</td>
                    <td>
                      <button onClick={() => deletePiece(piece)}>Delete</button>
                    </td>
                  </tr>
                ))}

                {/* Input row */}
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
