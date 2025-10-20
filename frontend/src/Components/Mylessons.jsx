import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import { Link } from 'react-router-dom';
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

  // Fetch lessons
  useEffect(() => {
    if (loading || !user) return;
    const path = role === 'TEACHER' ? '/api/lesson/teacher' : '/api/lesson/student';

    fetch(`${API_BASE}${path}`, { credentials: 'include' })
      .then(res => (res.ok ? res.json() : []))
      .then(data => {
        // Server may return ISO dates (from projection) or Dutch if you later switch.
        // Normalize to Dutch for display here:
        const normalized = (Array.isArray(data) ? data : []).map(l => ({
          ...l,
          lessonDate: l.lessonDate?.includes('-') && l.lessonDate.indexOf('-') === 4
            ? toDutchFromIso(l.lessonDate) // ISO -> Dutch
            : l.lessonDate,                // already Dutch
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

  // Teachers list (for students without one)
  useEffect(() => {
    if (loading) return;
    if (user && role !== 'TEACHER' && !user.teacher) {
      fetch(`${API_BASE}/api/user/teachers`, { credentials: 'include' })
        .then(res => (res.status === 204 ? [] : res.json()))
        .then(setTeachers)
        .catch(() => setTeachers([]));
    }
  }, [user, loading, role]);

  // Assigned students for teacher dropdown
  useEffect(() => {
    if (role === 'TEACHER') {
      fetch(`${API_BASE}/api/user/my-students`, { credentials: 'include' })
        .then(res => (res.ok ? res.json() : []))
        .then(setAssignedStudents)
        .catch(() => setAssignedStudents([]));
    }
  }, [role]);

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

  // Filter by day/week (selectedDate is ISO; lessons are Dutch)
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

  // Inline edit
  const handleEditClick = (lesson) => {
    setEditingLessonId(lesson.id);
    setEditData({ ...lesson }); // keep date in Dutch in state
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
      // send Dutch date (controller accepts dd-MM-yyyy and ISO)
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

// Navigation helpers
const renderDateLabel = () => {
  const selected = new Date(selectedDate);

  // Map JS getDay() (0=Sun, 1=Mon...) → short 2-letter labels
  const weekdayLabels = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];

  const formatDutch = (d) => {
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}-${month}-${year}`;
  };

  if (viewMode === 'day') {
    const weekdayShort = weekdayLabels[selected.getDay()];
    return `${weekdayShort} ${formatDutch(selected)}`;
  } else {
    // Week view: Monday → Sunday
    const start = new Date(selected);
    const end = new Date(selected);

    // Adjust to Monday as start of week
    const day = selected.getDay(); // Sunday = 0, Monday = 1
    const diffToMonday = day === 0 ? -6 : 1 - day; // shift to Monday
    start.setDate(selected.getDate() + diffToMonday);

    // End of week (Sunday)
    end.setDate(start.getDate() + 6);

    const startLabel = `Mo ${formatDutch(start)}`;
    const endLabel = `Su ${formatDutch(end)}`;

    return `${startLabel} – ${endLabel}`;
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
                    {/* Instrument */}
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

                    {/* Student */}
                    <td>
                      {editingLessonId === lesson.id ? (
                        <select
                          value={editData.student?.id || lesson.studentId || ''}
                          onChange={(e) => handleEditChange('studentId', e.target.value)}
                        >
                          <option value="">Select student</option>
                          {assignedStudents.map((s) => (
                            <option key={s.id} value={s.id}>
                              {s.name}
                            </option>
                          ))}
                        </select>
                      ) : (
                        lesson.student?.name ?? lesson.studentName
                      )}
                    </td>

                    {/* Date */}
                    <td>
                      {editingLessonId === lesson.id ? (
                        // date input needs ISO; convert Dutch->ISO for the input value
                        <input
                          type="date"
                          value={toIsoFromDutch(editData.lessonDate || lesson.lessonDate || '')}
                          onChange={(e) =>
                            handleEditChange('lessonDate', toDutchFromIso(e.target.value))
                          }
                        />
                      ) : (
                        lesson.lessonDate // already Dutch for display
                      )}
                    </td>

                    {/* Start */}
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

                    {/* End */}
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

                    {/* Homework */}
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

                    {/* PDFs */}
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

                    {/* Actions */}
                    <td>
                      {editingLessonId === lesson.id ? (
                        <>
                          <button onClick={() => handleSaveEdit(lesson.id)}>
                            Save
                          </button>
                          <button
                            onClick={handleCancelEdit}
                            style={{ background: 'gray', color: 'white', marginLeft: '5px' }}
                          >
                            Cancel
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            onClick={() => handleEditClick(lesson)}
                            style={{ background: '#d0a16d', color: 'black', marginRight: '5px' }}
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteLesson(lesson.id)}
                            style={{ background: 'crimson', color: 'white' }}
                          >
                            Delete
                          </button>
                        </>
                      )}
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
        ) : (
          <p>Only teachers can edit or delete lessons.</p>
        )}
      </div>
    </div>
  );
}
