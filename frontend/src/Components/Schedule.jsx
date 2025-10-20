import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useUser } from '../Context/UserContext';
import DatePicker from 'react-datepicker';
import { format, setHours, setMinutes } from 'date-fns';
import 'react-datepicker/dist/react-datepicker.css';
import '../App.css';

const API_BASE = 'http://localhost:8080';

const Schedule = () => {
  const { user, loading } = useUser();
  const [students, setStudents] = useState([]);

  const instruments = ['Piano', 'Guitar', 'Violin', 'Voice', 'Drums'];
  const [instrument, setInstrument] = useState('');
  const [studentId, setStudentId] = useState('');
  const [lessonDate, setLessonDate] = useState(null);
  const [startTime, setStartTime] = useState(() => setHours(setMinutes(new Date(), 0), 9)); // default 09:00
  const [endTime, setEndTime] = useState(() => setHours(setMinutes(new Date(), 30), 9));   // optional: default 09:30
  const [homework, setHomework] = useState('');
  const [pdfFiles, setPdfFiles] = useState([]);
  const fileInputRef = useRef(null);

  // Fetch teacher’s students
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

    const formattedDate = format(lessonDate, 'dd-MM-yyyy');
    const formattedStart = format(startTime, 'HH:mm:ss');
    const formattedEnd = format(endTime, 'HH:mm:ss');

    const formData = new FormData();
    formData.append('instrument', instrument);
    formData.append('studentId', studentId);
    formData.append('lessonDate', formattedDate);
    formData.append('startTime', formattedStart);
    formData.append('endTime', formattedEnd);
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
      setLessonDate(null);
      setStartTime(setHours(setMinutes(new Date(), 0), 9)); // reset to 09:00
      setEndTime(setHours(setMinutes(new Date(), 30), 9));   // reset to 09:30
      setHomework('');
      setPdfFiles([]);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      console.error('Scheduling error:', err);
      alert('Failed to schedule lesson.');
    }
  };

  if (loading) return <div>Loading...</div>;

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
            {/* Instrument */}
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

            {/* Student */}
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

            {/* Calendar popup (Dutch format) */}
            <div className="form-group">
              <label htmlFor="date">Date</label>
              <DatePicker
                id="date"
                selected={lessonDate}
                onChange={(date) => setLessonDate(date)}
                dateFormat="dd-MM-yyyy"
                placeholderText="Select a date"
                className="date-picker-input"
                calendarStartDay={1}
                required
              />
            </div>

            {/* Start Time (default 09:00) */}
            <div className="form-group">
              <label htmlFor="startTime">Start Time</label>
              <DatePicker
                selected={startTime}
                onChange={(time) => setStartTime(time)}
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

            {/* End Time */}
            <div className="form-group">
              <label htmlFor="endTime">End Time</label>
              <DatePicker
                selected={endTime}
                onChange={(time) => setEndTime(time)}
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

            {/* Homework */}
            <div className="form-group">
              <label htmlFor="homework">Homework</label>
              <textarea
                id="homework"
                value={homework}
                onChange={(e) => setHomework(e.target.value)}
              />
            </div>

            {/* File upload */}
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
