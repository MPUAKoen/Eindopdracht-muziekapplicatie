import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

const API_BASE = 'http://localhost:8080';

// Helpers for dd-MM-yyyy <-> yyyy-MM-dd
const toIsoFromDutch = (ddmmyyyy) => {
  if (!ddmmyyyy?.includes('-')) return ddmmyyyy;
  const [d, m, y] = ddmmyyyy.split('-');
  return `${y}-${m}-${d}`;
};
const toDutchFromIso = (yyyymmdd) => {
  if (!yyyymmdd?.includes('-')) return yyyymmdd;
  const [y, m, d] = yyyymmdd.split('-');
  return `${d}-${m}-${y}`;
};
const parseDutchDateTime = (ddmmyyyy, time = '00:00:00') => {
  const iso = toIsoFromDutch(ddmmyyyy);
  const t = time?.length === 5 ? `${time}:00` : time;
  return new Date(`${iso}T${t || '00:00:00'}`);
};
const padSeconds = (t) => (t && t.length === 5 ? `${t}:00` : t || '00:00:00');

export default function MyLessons() {
  const { user, loading, login } = useUser();
  const [teachers, setTeachers] = useState([]);
  const [selectedTeacher, setSelectedTeacher] = useState('');
  const [myLessons, setMyLessons] = useState([]);
  const [assignedStudents, setAssignedStudents] = useState([]);
  const [editingLessonId, setEditingLessonId] = useState(null);
  const [editData, setEditData] = useState({});
  const [viewMode, setViewMode] = useState('day');
  const [selectedDate, setSelectedDate] = useState(() =>
    new Date().toISOString().split('T')[0]
  );

  const role = user?.role?.toUpperCase();

  // === Fetch lessons ===
  useEffect(() => {
    if (loading || !user) return;
    const path = role === 'TEACHER' ? '/api/lesson/teacher' : '/api/lesson/student';

    fetch(`${API_BASE}${path}`, { credentials: 'include' })
      .then(res => (res.ok ? res.json() : []))
      .then(data => {
        const normalized = (Array.isArray(data) ? data : []).map(l => ({
          ...l,
          lessonDate: l.lessonDate?.includes('-') && l.lessonDate.indexOf('-') === 4
            ? toDutchFromIso(l.lessonDate)
            : l.lessonDate,
        }));
        const sorted = normalized.sort(
          (a, b) =>
            parseDutchDateTime(a.lessonDate, a.startTime) -
            parseDutchDateTime(b.lessonDate, b.startTime)
        );
        setMyLessons(sorted);
      })
      .catch(() => setMyLessons([]));
  }, [user, loading, role]);

  // === Fetch available teachers (for unassigned students) ===
  useEffect(() => {
    if (loading) return;
    if (user && role !== 'TEACHER' && !user.teacher) {
      fetch(`${API_BASE}/api/user/teachers`, { credentials: 'include' })
        .then(res => (res.status === 204 ? [] : res.json()))
        .then(setTeachers)
        .catch(() => setTeachers([]));
    }
  }, [user, loading, role]);

  // === Assigned students (for teacher dropdown in lesson editing) ===
  useEffect(() => {
    if (role === 'TEACHER') {
      fetch(`${API_BASE}/api/user/my-students`, { credentials: 'include' })
        .then(res => (res.ok ? res.json() : []))
        .then(setAssignedStudents)
        .catch(() => setAssignedStudents([]));
    }
  }, [role]);

  // === Assign teacher to student ===
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

  // === Filter by day/week ===
  const filteredLessons = myLessons.filter(lesson => {
    const lessonDate = parseDutchDateTime(lesson.lessonDate);
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

  // === Delete lesson (teacher only) ===
  const handleDeleteLesson = async (lessonId) => {
    if (!lessonId || !window.confirm('Delete this lesson?')) return;
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

  // === Edit helpers (teacher only) ===
  const handleEditClick = (lesson) => {
    setEditingLessonId(lesson.id);
    setEditData({ ...lesson });
  };
  const handleCancelEdit = () => {
    setEditingLessonId(null);
    setEditData({});
  };
  const handleEditChange = (field, value) => {
    setEditData(prev => ({ ...prev, [field]: value }));
  };
  const handleSaveEdit = async (lessonId) => {
    const payload = {
      ...editData,
      lessonDate: editData.lessonDate,
      startTime: padSeconds(editData.startTime),
      endTime: padSeconds(editData.endTime),
    };

    const res = await fetch(`${API_BASE}/api/lesson/${lessonId}`, {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      setMyLessons(prev =>
        prev.map(l => (l.id === lessonId ? { ...l, ...payload } : l))
      );
      setEditingLessonId(null);
    } else {
      const txt = await res.text();
      alert(`Failed to update lesson. ${txt || ''}`);
    }
  };

  // === Date navigation ===
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

  if (loading) return <div>Loading…</div>;

  // === Render ===
  return (
    <div className="mainpage">
      <div className="header">
        <h1>My Lessons</h1>
      </div>

      <div className="dashboard">
        {/* Navigation header */}
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

        {/* === TEACHER MODE === */}
        {role === 'TEACHER' && (
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
                    <td>
                      {editingLessonId === lesson.id ? (
                        <select
                          value={editData.instrument || ''}
                          onChange={(e) => handleEditChange('instrument', e.target.value)}
                        >
                          <option value="">Select</option>
                          <option value="Piano">Piano</option>
                          <option value="Violin">Violin</option>
                          <option value="Guitar">Guitar</option>
                          <option value="Drums">Drums</option>
                          <option value="Voice">Voice</option>
                        </select>
                      ) : (
                        lesson.instrument
                      )}
                    </td>
                    <td>
                      {editingLessonId === lesson.id ? (
                        <select
                          value={editData.student?.id || lesson.studentId || ''}
                          onChange={(e) => handleEditChange('studentId', e.target.value)}
                        >
                          <option value="">Select student</option>
                          {assignedStudents.map((s) => (
                            <option key={s.id} value={s.id}>{s.name}</option>
                          ))}
                        </select>
                      ) : (
                        lesson.student?.name ?? lesson.studentName
                      )}
                    </td>
                    <td>
                      {editingLessonId === lesson.id ? (
                        <input
                          type="date"
                          value={toIsoFromDutch(editData.lessonDate || lesson.lessonDate || '')}
                          onChange={(e) =>
                            handleEditChange('lessonDate', toDutchFromIso(e.target.value))
                          }
                        />
                      ) : (
                        lesson.lessonDate
                      )}
                    </td>
                    <td>
                      {editingLessonId === lesson.id ? (
                        <input
                          type="time"
                          value={(editData.startTime || '').slice(0, 5)}
                          onChange={(e) => handleEditChange('startTime', e.target.value)}
                        />
                      ) : (
                        (lesson.startTime || '').slice(0, 8)
                      )}
                    </td>
                    <td>
                      {editingLessonId === lesson.id ? (
                        <input
                          type="time"
                          value={(editData.endTime || '').slice(0, 5)}
                          onChange={(e) => handleEditChange('endTime', e.target.value)}
                        />
                      ) : (
                        (lesson.endTime || '').slice(0, 8)
                      )}
                    </td>
                    <td>
                      {editingLessonId === lesson.id ? (
                        <textarea
                          value={editData.homework || ''}
                          onChange={(e) => handleEditChange('homework', e.target.value)}
                        />
                      ) : (
                        lesson.homework || '—'
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
                      {editingLessonId === lesson.id ? (
                        <>
                          <button onClick={() => handleSaveEdit(lesson.id)}>Save</button>
                          <button onClick={handleCancelEdit}>Cancel</button>
                        </>
                      ) : (
                        <>
                          <button onClick={() => handleEditClick(lesson)}>Edit</button>
                          <button onClick={() => handleDeleteLesson(lesson.id)}>Delete</button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan="8" style={{ textAlign: 'center' }}>No lessons found.</td></tr>
              )}
            </tbody>
          </table>
        )}

        {/* === STUDENT MODE === */}
        {role !== 'TEACHER' && (
          <>
            {!user.teacher ? (
              <div>
                <h3>Select your teacher</h3>
                <div className="search-bar">
                  <select
                    value={selectedTeacher}
                    onChange={(e) => setSelectedTeacher(e.target.value)}
                  >
                    <option value="">Select a teacher</option>
                    {teachers.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} – {t.instrument || 'No instrument'}
                      </option>
                    ))}
                  </select>
                  <button onClick={handleTeacherAssign}>Assign Teacher</button>
                </div>
              </div>
            ) : (
              <table className="table">
                <caption>My Lessons</caption>
                <thead>
                  <tr>
                    <th>Instrument</th>
                    <th>Teacher</th>
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
                        <td>{lesson.teacherName || user.teacher?.name}</td>
                        <td>{lesson.lessonDate}</td>
                        <td>{lesson.startTime}</td>
                        <td>{lesson.endTime}</td>
                        <td>{lesson.homework || '—'}</td>
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
                    <tr><td colSpan="7">No lessons scheduled.</td></tr>
                  )}
                </tbody>
              </table>
            )}
          </>
        )}
      </div>
    </div>
  );
}
