import React, { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { useUser } from '../Context/UserContext';
import '../App.css';

const API_BASE = 'http://localhost:8080';

// === Helper functions ===
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

// Date helpers for the pickers
const dutchDateStrToDate = (ddmmyyyy) => {
  if (!ddmmyyyy) return null;
  const [d, m, y] = ddmmyyyy.split('-').map(Number);
  if (!d || !m || !y) return null;
  return new Date(y, m - 1, d);
};
const dateToDutchDateStr = (date) => {
  if (!(date instanceof Date) || isNaN(date)) return '';
  const d = String(date.getDate()).padStart(2, '0');
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const y = date.getFullYear();
  return `${d}-${m}-${y}`;
};
const timeStrToDate = (hhmmOrHhmmss) => {
  if (!hhmmOrHhmmss) return null;
  const parts = hhmmOrHhmmss.split(':').map(Number);
  if (parts.length < 2) return null;
  const [h, m] = parts;
  const dt = new Date();
  dt.setHours(h ?? 0, m ?? 0, 0, 0);
  return dt;
};
const dateToTimeHHmm = (date) => {
  if (!(date instanceof Date) || isNaN(date)) return '';
  const h = String(date.getHours()).padStart(2, '0');
  const m = String(date.getMinutes()).padStart(2, '0');
  return `${h}:${m}`;
};

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
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => {
        const normalized = (Array.isArray(data) ? data : []).map((l) => ({
          ...l,
          lessonDate:
            l.lessonDate?.includes('-') && l.lessonDate.indexOf('-') === 4
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

  // === Fetch teachers (for students) ===
  useEffect(() => {
    if (loading) return;
    if (user && role !== 'TEACHER' && !user.teacher) {
      fetch(`${API_BASE}/api/user/teachers`, { credentials: 'include' })
        .then((res) => (res.status === 204 ? [] : res.json()))
        .then(setTeachers)
        .catch(() => setTeachers([]));
    }
  }, [user, loading, role]);

  // === Fetch students (for teachers) ===
  useEffect(() => {
    if (role === 'TEACHER') {
      fetch(`${API_BASE}/api/user/my-students`, { credentials: 'include' })
        .then((res) => (res.ok ? res.json() : []))
        .then(setAssignedStudents)
        .catch(() => setAssignedStudents([]));
    }
  }, [role]);

  // === Assign teacher ===
  const handleTeacherAssign = () => {
    if (!selectedTeacher) return;
    fetch(`${API_BASE}/api/user/assign-teacher/${selectedTeacher}`, {
      method: 'PATCH',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    })
      .then((res) => res.json())
      .then((updatedUser) => {
        login(updatedUser);
        setSelectedTeacher('');
      })
      .catch(console.error);
  };

  // === Filter lessons by day or week ===
  const filteredLessons = myLessons.filter((lesson) => {
    const lessonDate = parseDutchDateTime(lesson.lessonDate);
    const selected = new Date(selectedDate);

    if (viewMode === 'day') {
      return (
        lessonDate.getFullYear() === selected.getFullYear() &&
        lessonDate.getMonth() === selected.getMonth() &&
        lessonDate.getDate() === selected.getDate()
      );
    } else {
      const monday = new Date(selected);
      const day = monday.getDay();
      const diff = (day === 0 ? -6 : 1) - day;
      monday.setDate(selected.getDate() + diff);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);
      return lessonDate >= monday && lessonDate <= sunday;
    }
  });

  // === Delete lesson ===
  const handleDeleteLesson = async (lessonId) => {
    if (!lessonId || !window.confirm('Delete this lesson?')) return;
    try {
      const res = await fetch(`${API_BASE}/api/lesson/${lessonId}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      if (!res.ok) throw new Error(`Failed with status ${res.status}`);
      setMyLessons((prev) => prev.filter((l) => (l.id ?? l.lessonId) !== lessonId));
      if (editingLessonId === lessonId) {
        setEditingLessonId(null);
        setEditData({});
      }
    } catch (e) {
      console.error('Delete failed:', e);
      alert('Could not delete lesson.');
    }
  };

  // === Edit logic ===
  const handleEditClick = (lesson) => {
    setEditingLessonId(lesson.id);
    setEditData({
      id: lesson.id,
      instrument: lesson.instrument || '',
      lessonDate: lesson.lessonDate || '',
      startTime: lesson.startTime || '',
      endTime: lesson.endTime || '',
      homework: lesson.homework || '',
      studentId: lesson.student?.id ?? lesson.studentId ?? null,
    });
  };
  const handleCancelEdit = () => {
    setEditingLessonId(null);
    setEditData({});
  };
  const handleEditChange = (field, value) => {
    setEditData((prev) => ({ ...prev, [field]: value }));
  };
  const handleSaveEdit = async (lessonId) => {
    const payload = {
      instrument: editData.instrument,
      lessonDate: editData.lessonDate, // dd-MM-yyyy for backend
      startTime: padSeconds(editData.startTime),
      endTime: padSeconds(editData.endTime),
      homework: editData.homework,
      ...(editData.studentId ? { studentId: editData.studentId } : {}),
    };

    try {
      const res = await fetch(`${API_BASE}/api/lesson/${lessonId}`, {
        method: 'PATCH',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const txt = await res.text();
        throw new Error(txt || `HTTP ${res.status}`);
      }

      const updated = await res.json();

      setMyLessons((prev) =>
        prev.map((l) =>
          l.id === lessonId
            ? {
                ...l,
                instrument: updated.instrument ?? payload.instrument,
                lessonDate: updated.lessonDate ?? payload.lessonDate,
                startTime: updated.startTime ?? payload.startTime,
                endTime: updated.endTime ?? payload.endTime,
                homework: updated.homework ?? payload.homework,
                studentId: updated.studentId ?? l.studentId,
                student: l.student, // keep nested if present
              }
            : l
        )
      );

      setEditingLessonId(null);
      setEditData({});
    } catch (err) {
      console.error(err);
      alert(`Failed to update lesson. ${err.message}`);
    }
  };

  // === Date formatting and navigation ===
  const renderDateLabel = () => {
    const selected = new Date(selectedDate);
    const weekdays = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

    const formatDay = (date) => {
      const day = weekdays[(date.getDay() + 6) % 7];
      const d = String(date.getDate()).padStart(2, '0');
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const y = date.getFullYear();
      return `${day} ${d}-${m}-${y}`;
    };

    if (viewMode === 'day') {
      return formatDay(selected);
    } else {
      const monday = new Date(selected);
      const day = monday.getDay();
      const diff = (day === 0 ? -6 : 1) - day;
      monday.setDate(selected.getDate() + diff);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);

      return `${formatDay(monday)} – ${formatDay(sunday)}`;
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
        {/* Week or Day toggle */}
        <button
          className="lesson-toggle-right mobile-top-toggle"
          onClick={() => setViewMode(viewMode === 'day' ? 'week' : 'day')}
        >
          <img src="src/assets/calendar.png" alt="Toggle View" />
          {viewMode === 'day' ? 'Week View' : 'Day View'}
        </button>

        {/* Inline date and arrows */}
        <div className="lesson-date-header">
          <h2 className="lesson-title">
            <button onClick={handlePrev} className="lesson-arrow inline">‹</button>
            {renderDateLabel()}
            <button onClick={handleNext} className="lesson-arrow inline">›</button>
          </h2>
        </div>

        {/* === TEACHER MODE === */}
        {role === 'TEACHER' && (
          <div className="table-scroll">
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
                  filteredLessons.map((lesson) => {
                    const isEditing = editingLessonId === lesson.id;

                    // Precompute picker values from strings
                    const datePickerValue = isEditing
                      ? dutchDateStrToDate(editData.lessonDate)
                      : null;
                    const startPickerValue = isEditing
                      ? timeStrToDate(editData.startTime || '09:00')
                      : null;
                    const endPickerValue = isEditing
                      ? timeStrToDate(editData.endTime || '10:00')
                      : null;

                    return (
                      <tr key={lesson.id}>
                        {/* Instrument */}
                        <td>
                          {isEditing ? (
                            <input
                              value={editData.instrument || ''}
                              onChange={(e) => handleEditChange('instrument', e.target.value)}
                            />
                          ) : (
                            lesson.instrument
                          )}
                        </td>

                        {/* Student dropdown */}
                        <td>
                          {isEditing ? (
                            <select
                              value={editData.studentId ?? ''}
                              onChange={(e) =>
                                handleEditChange('studentId', e.target.value ? Number(e.target.value) : null)
                              }
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

                        {/* Date picker */}
                        <td>
                          {isEditing ? (
                            <div className="form-group compact">
                              <DatePicker
                                selected={datePickerValue}
                                onChange={(date) =>
                                  handleEditChange('lessonDate', dateToDutchDateStr(date))
                                }
                                dateFormat="dd-MM-yyyy"
                                placeholderText="Select a date"
                                className="date-picker-input"
                                calendarStartDay={1}
                                required
                              />
                            </div>
                          ) : (
                            lesson.lessonDate
                          )}
                        </td>

                        {/* Start time picker */}
                        <td>
                          {isEditing ? (
                            <div className="form-group compact">
                              <DatePicker
                                selected={startPickerValue}
                                onChange={(time) =>
                                  handleEditChange('startTime', dateToTimeHHmm(time))
                                }
                                showTimeSelect
                                showTimeSelectOnly
                                timeIntervals={5}
                                timeCaption="Start"
                                dateFormat="HH:mm"
                                timeFormat="HH:mm"
                                placeholderText="Select time"
                                className="date-picker-input"
                                required
                              />
                            </div>
                          ) : (
                            lesson.startTime
                          )}
                        </td>

                        {/* End time picker */}
                        <td>
                          {isEditing ? (
                            <div className="form-group compact">
                              <DatePicker
                                selected={endPickerValue}
                                onChange={(time) =>
                                  handleEditChange('endTime', dateToTimeHHmm(time))
                                }
                                showTimeSelect
                                showTimeSelectOnly
                                timeIntervals={5}
                                timeCaption="End"
                                dateFormat="HH:mm"
                                timeFormat="HH:mm"
                                placeholderText="Select time"
                                className="date-picker-input"
                                required
                              />
                            </div>
                          ) : (
                            lesson.endTime
                          )}
                        </td>

                        {/* Homework */}
                        <td>
                          {isEditing ? (
                            <input
                              value={editData.homework || ''}
                              onChange={(e) => handleEditChange('homework', e.target.value)}
                            />
                          ) : (
                            lesson.homework || 'None'
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
                          {!isEditing ? (
                            <>
                              <button onClick={() => handleEditClick(lesson)}>Edit</button>
                              <button onClick={() => handleDeleteLesson(lesson.id)}>Delete</button>
                            </>
                          ) : (
                            <>
                              <button onClick={() => handleSaveEdit(lesson.id)}>Save</button>
                              <button onClick={handleCancelEdit}>Cancel</button>
                            </>
                          )}
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center' }}>
                      No lessons found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
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
                        {t.name} · {t.instrument || 'No instrument'}
                      </option>
                    ))}
                  </select>
                  <button onClick={handleTeacherAssign}>Assign Teacher</button>
                </div>
              </div>
            ) : (
              <div className="table-scroll">
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
                          <td>{lesson.homework || 'None'}</td>
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
                        <td colSpan="7">No lessons scheduled.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
