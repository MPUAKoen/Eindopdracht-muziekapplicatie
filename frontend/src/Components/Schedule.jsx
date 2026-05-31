import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { useUser } from '../Context/UserContext';
import DatePicker from 'react-datepicker';
import { format } from 'date-fns';
import { API_BASE, getAuthAxiosConfig } from '../lib/auth';
import Button from './ui/Button';
import FormField from './ui/FormField';
import Select from './ui/Select';
import 'react-datepicker/dist/react-datepicker.css';
import '../App.css';

const createTime = (hour, minute) => {
  const date = new Date();
  date.setHours(hour, minute, 0, 0);
  return date;
};

const normalizedTime = (date) => {
  const normalized = new Date(date);
  normalized.setSeconds(0, 0);
  return normalized;
};

const minutesFromTime = (time) => {
  if (!time) return null;

  if (time instanceof Date) {
    return time.getHours() * 60 + time.getMinutes();
  }

  const [hours, minutes] = String(time).split(':').map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return null;

  return hours * 60 + minutes;
};

const datesMatch = (lessonDate, selectedDate) => {
  if (!lessonDate || !selectedDate) return false;

  return (
    lessonDate === format(selectedDate, 'yyyy-MM-dd') ||
    lessonDate === format(selectedDate, 'dd-MM-yyyy')
  );
};

const hasLessonConflict = (lessons, selectedStudentId, selectedDate, selectedStart, selectedEnd) => {
  const selectedStartMinutes = minutesFromTime(selectedStart);
  const selectedEndMinutes = minutesFromTime(selectedEnd);

  if (selectedStartMinutes == null || selectedEndMinutes == null) return false;

  return lessons.some((lesson) => {
    const sameStudent = String(lesson.studentId) === String(selectedStudentId);
    const sameDate = datesMatch(lesson.lessonDate, selectedDate);
    const lessonStart = minutesFromTime(lesson.startTime);
    const lessonEnd = minutesFromTime(lesson.endTime);

    if (!sameDate || lessonStart == null || lessonEnd == null) {
      return false;
    }

    const overlaps = lessonStart < selectedEndMinutes && lessonEnd > selectedStartMinutes;
    const overlapStart = Math.max(lessonStart, selectedStartMinutes);
    const overlapEnd = Math.min(lessonEnd, selectedEndMinutes);

    return overlaps && overlapEnd - overlapStart > 10 && (sameStudent || lesson.teacherId);
  });
};

const getSchedulingErrorMessage = (err) => {
  const responseText = JSON.stringify(err.response?.data || '').toLowerCase();
  const errorText = `${err.message || ''} ${responseText}`.toLowerCase();

  if (err.response?.status === 409 || errorText.includes('already has a lesson')) {
    return 'This teacher or student already has a lesson at that time.';
  }

  if (err.response?.status === 400) {
    return 'Please check the lesson details and try again.';
  }

  if (err.response?.status === 401 || err.response?.status === 403) {
    return 'You are not allowed to schedule this lesson.';
  }

  return 'Failed to schedule lesson.';
};

const Schedule = () => {
  const { user, loading } = useUser();
  const [students, setStudents] = useState([]);

  const instruments = ['Piano', 'Guitar', 'Violin', 'Voice', 'Drums'];
  const [instrument, setInstrument] = useState('');
  const [studentId, setStudentId] = useState('');
  const [lessonDate, setLessonDate] = useState(null);
  const [startTime, setStartTime] = useState(() => createTime(9, 0));
  const [endTime, setEndTime] = useState(() => createTime(9, 30));
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
      .get(`${API_BASE}/api/teachers/me/students`, getAuthAxiosConfig())
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

    const cleanStartTime = normalizedTime(startTime);
    const cleanEndTime = normalizedTime(endTime);

    if (cleanStartTime >= cleanEndTime) {
      alert('End time must be after start time.');
      return;
    }

    const formattedDate = format(lessonDate, 'dd-MM-yyyy');
    const formattedStart = format(cleanStartTime, 'HH:mm:ss');
    const formattedEnd = format(cleanEndTime, 'HH:mm:ss');

    try {
      const lessonResponse = await axios.get(
        `${API_BASE}/api/lessons?scope=teaching`,
        getAuthAxiosConfig()
      );
      const existingLessons = Array.isArray(lessonResponse.data) ? lessonResponse.data : [];

      if (hasLessonConflict(existingLessons, studentId, lessonDate, cleanStartTime, cleanEndTime)) {
        alert('This teacher or student already has a lesson at that time.');
        return;
      }
    } catch (err) {
      console.error('Lesson conflict check failed:', err.response?.data || err);
    }

    const formData = new FormData();
    formData.append('instrument', instrument);
    formData.append('studentId', studentId);
    formData.append('lessonDate', formattedDate);
    formData.append('startTime', formattedStart);
    formData.append('endTime', formattedEnd);
    formData.append('homework', homework);
    pdfFiles.forEach((file) => formData.append('pdfFiles', file));

    try {
      await axios.post(`${API_BASE}/api/lessons`, formData, getAuthAxiosConfig());
      alert('Lesson scheduled successfully!');
      setInstrument('');
      setStudentId('');
      setLessonDate(null);
      setStartTime(createTime(9, 0));
      setEndTime(createTime(9, 30));
      setHomework('');
      setPdfFiles([]);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      console.error('Scheduling error:', err.response?.data || err);
      alert(getSchedulingErrorMessage(err));
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
            <Select
              id="instrument"
              label="Instrument"
              value={instrument}
              onChange={(e) => setInstrument(e.target.value)}
              options={instruments}
              required
            />

            {/* Student */}
            <Select
              id="student"
              label="Student"
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              options={students.map((s) => ({
                value: s.id,
                label: `${s.name} (${s.email})`
              }))}
              required
            />

            {/* Calendar popup (Dutch format) */}
            <FormField label="Date" htmlFor="date">
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
            </FormField>

            {/* Start Time (default 09:00) */}
            <FormField label="Start Time" htmlFor="startTime">
              <DatePicker
                selected={startTime}
                onChange={(time) => setStartTime(normalizedTime(time))}
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
            </FormField>

            {/* End Time */}
            <FormField label="End Time" htmlFor="endTime">
              <DatePicker
                selected={endTime}
                onChange={(time) => setEndTime(normalizedTime(time))}
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
            </FormField>

            {/* Homework */}
            <FormField label="Homework" htmlFor="homework">
              <textarea
                id="homework"
                value={homework}
                onChange={(e) => setHomework(e.target.value)}
              />
            </FormField>

            {/* File upload */}
            <FormField label="Upload PDFs" htmlFor="pdfFiles">
              <input
                type="file"
                id="pdfFiles"
                accept="application/pdf"
                multiple
                onChange={(e) => setPdfFiles(Array.from(e.target.files))}
                ref={fileInputRef}
              />
            </FormField>

            <Button type="submit" className="submit-btn">
              Add Lesson
            </Button>
          </form>
        )}
      </div>
    </div>
  );
};

export default Schedule;
