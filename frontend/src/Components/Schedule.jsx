// src/Schedule.jsx
import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const Schedule = () => {
  const [students, setStudents] = useState([]);
  const instruments = ['Piano','Guitar','Violin','Voice','Drums'];

  const [instrument, setInstrument] = useState('');
  const [studentId, setStudentId] = useState('');
  const [lessonDate, setLessonDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [homework, setHomework] = useState('');
  const [pdfFiles, setPdfFiles] = useState([]);
  const fileInputRef = useRef(null);

  useEffect(() => {
    // Fetch all users, then keep only those with role "USER" (i.e. students)
    axios.get(`${API_BASE}/api/user/all`, { withCredentials: true })
      .then(res => {
        const all = res.data;
        const onlyStudents = all.filter(u => u.role === 'USER');
        setStudents(onlyStudents);
      })
      .catch(err => {
        console.error('Error fetching users:', err);
        setStudents([]);
      });
  }, []);

  const handleSubmit = async e => {
    e.preventDefault();
    if (!instrument || !studentId || !lessonDate || !startTime || !endTime) {
      return alert('Please fill in all fields');
    }

    const formData = new FormData();
    formData.append('instrument', instrument);
    formData.append('studentName', students.find(s => s.id === +studentId)?.name || '');
    formData.append('lessonDate', lessonDate);
    formData.append('startTime', startTime);
    formData.append('endTime', endTime);
    formData.append('homework', homework);
    pdfFiles.forEach(file => formData.append('pdfFiles', file));

    try {
      await axios.post(
        `${API_BASE}/api/lesson/add`,
        formData,
        { withCredentials: true, headers: { 'Content-Type': 'multipart/form-data' } }
      );
      alert('Lesson scheduled successfully!');
      // reset
      setInstrument(''); setStudentId(''); setLessonDate('');
      setStartTime(''); setEndTime(''); setHomework(''); setPdfFiles([]);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      console.error('Scheduling error:', err);
      alert('Failed to schedule lesson.');
    }
  };

  return (
    <div className="mainpage">
      <div className="header"><h1>Schedule a Lesson</h1></div>
      <div className="dashboard">
        <form onSubmit={handleSubmit} className="lesson-form">
          <div className="form-group">
            <label htmlFor="instrument">Instrument</label>
            <select id="instrument" value={instrument} onChange={e => setInstrument(e.target.value)} required>
              <option value="">-- Select --</option>
              {instruments.map(ins => <option key={ins} value={ins}>{ins}</option>)}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="student">Student</label>
            <select id="student" value={studentId} onChange={e => setStudentId(e.target.value)} required>
              <option value="">-- Select --</option>
              {students.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.email})
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="date">Date</label>
            <input type="date" id="date" value={lessonDate} onChange={e => setLessonDate(e.target.value)} required/>
          </div>

          <div className="form-group">
            <label htmlFor="startTime">Start Time</label>
            <input type="time" id="startTime" value={startTime} onChange={e => setStartTime(e.target.value)} required/>
          </div>

          <div className="form-group">
            <label htmlFor="endTime">End Time</label>
            <input type="time" id="endTime" value={endTime} onChange={e => setEndTime(e.target.value)} required/>
          </div>

          <div className="form-group">
            <label htmlFor="homework">Homework</label>
            <textarea id="homework" value={homework} onChange={e => setHomework(e.target.value)} />
          </div>

          <div className="form-group">
            <label htmlFor="pdfFiles">Upload PDFs</label>
            <input
              type="file"
              id="pdfFiles"
              accept="application/pdf"
              multiple
              onChange={e => setPdfFiles(Array.from(e.target.files))}
              ref={fileInputRef}
            />
          </div>

          <button type="submit" className="submit-btn">Add Lesson</button>
        </form>
      </div>
    </div>
  );
};

export default Schedule;
