// src/Schedule.jsx
import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useUser } from '../Context/UserContext';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const Schedule = () => {
  const { user, loading } = useUser();
  const [students, setStudents] = useState([]);

  const instruments = ['Piano', 'Guitar', 'Violin', 'Voice', 'Drums'];
  const [instrument, setInstrument] = useState('');
  const [studentId, setStudentId] = useState('');
  const [lessonDate, setLessonDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [homework, setHomework] = useState('');
  const [pdfFiles, setPdfFiles] = useState([]);
  const fileInputRef = useRef(null);

  // 1) After session loads, fetch only your assigned students
  useEffect(() => {
    if (loading) return;
    if (!user || user.role !== 'TEACHER') {
      setStudents([]);
      return;
    }

    axios
      .get(`${API_BASE}/api/user/my-students`, { withCredentials: true })
      .then((res) => setStudents(res.data))
      .catch((err) => {
        console.error('Error fetching my students:', err);
        setStudents([]);
      });
  }, [user, loading]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!instrument || !studentId || !lessonDate || !startTime || !endTime) {
      alert('Please fill in all required fields');
      return;
    }

    const formData = new FormData();
    formData.append('instrument', instrument);
    formData.append('studentId', studentId);        // ← send ID, not name
    formData.append('lessonDate', lessonDate);
    formData.append('startTime', startTime);
    formData.append('endTime', endTime);
    formData.append('homework', homework);
    pdfFiles.forEach((file) => formData.append('pdfFiles', file));

    try {
      await axios.post(`${API_BASE}/api/lesson/add`, formData, {
        withCredentials: true,
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      alert('Lesson scheduled successfully!');
      setInstrument('');
      setStudentId('');
      setLessonDate('');
      setStartTime('');
      setEndTime('');
      setHomework('');
      setPdfFiles([]);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      console.error('Scheduling error:', err);
      alert('Failed to schedule lesson.');
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="mainpage">
      <div className="header">
        <h1>Schedule a Lesson</h1>
      </div>
      <div className="dashboard">
        {user.role !== 'TEACHER' ? (
          <p style={{ color: 'red' }}>Only teachers can schedule lessons.</p>
        ) : students.length === 0 ? (
          <p style={{ color: 'red' }}>You currently have no students assigned.</p>
        ) : (
          <form onSubmit={handleSubmit} className="lesson-form">
            <div className="form-group">
              <label htmlFor="instrument">Instrument</label>
              <select
                id="instrument"
                value={instrument}
                onChange={(e) => setInstrument(e.target.value)}
                required
              >
                <option value="">-- Select --</option>
                {instruments.map((ins) => (
                  <option key={ins} value={ins}>
                    {ins}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="student">Student</label>
              <select
                id="student"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                required
              >
                <option value="">-- Select --</option>
                {students.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.email})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="date">Date</label>
              <input
                type="date"
                id="date"
                value={lessonDate}
                onChange={(e) => setLessonDate(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="startTime">Start Time</label>
              <input
                type="time"
                id="startTime"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="endTime">End Time</label>
              <input
                type="time"
                id="endTime"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="homework">Homework</label>
              <textarea
                id="homework"
                value={homework}
                onChange={(e) => setHomework(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="pdfFiles">Upload PDFs</label>
              <input
                type="file"
                id="pdfFiles"
                accept="application/pdf"
                multiple
                onChange={(e) => setPdfFiles(Array.from(e.target.files))}
                ref={fileInputRef}
              />
            </div>

            <button type="submit" className="submit-btn">
              Add Lesson
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default Schedule;
