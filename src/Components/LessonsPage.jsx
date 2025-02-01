import React, { useState } from 'react';
import '../App.css';

const LessonsPage = () => {
    const students = [
        'John Doe',
        'Jane Smith',
        'Alice Brown',
        'Tom White'
    ];

    const instruments = [
        'Piano',
        'Guitar',
        'Violin',
        'Voice',
        'Drums'
    ];

    const [instrument, setInstrument] = useState('');
    const [student, setStudent] = useState('');
    const [lessonDate, setLessonDate] = useState('');
    const [startTime, setStartTime] = useState('');
    const [endTime, setEndTime] = useState('');

    const handleInstrumentChange = (e) => setInstrument(e.target.value);
    const handleStudentChange = (e) => setStudent(e.target.value);
    const handleDateChange = (e) => setLessonDate(e.target.value);
    const handleStartTimeChange = (e) => setStartTime(e.target.value);
    const handleEndTimeChange = (e) => setEndTime(e.target.value);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!instrument || !student || !lessonDate || !startTime || !endTime) {
            alert('Please fill in all fields');
        } else {
            alert(`Lesson scheduled for ${student} on ${lessonDate} from ${startTime} to ${endTime} for ${instrument}`);
            setInstrument('');
            setStudent('');
            setLessonDate('');
            setStartTime('');
            setEndTime('');
        }
    };

    return (
        <div className="mainpage">
            <div className="header">
                <h1>Schedule a Lesson</h1>
            </div>

            <div className="dashboard">
                <form onSubmit={handleSubmit} className="lesson-form">
                    <div className="form-group">
                        <div className="formTitle">Schedule lesson</div>
                        <label htmlFor="instrument">Select Instrument</label>
                        <select
                            id="instrument"
                            value={instrument}
                            onChange={handleInstrumentChange}
                            required
                        >
                            <option value="">-- Select an Instrument --</option>
                            {instruments.map((instrumentOption, index) => (
                                <option key={index} value={instrumentOption}>
                                    {instrumentOption}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="student">Select Student</label>
                        <select
                            id="student"
                            value={student}
                            onChange={handleStudentChange}
                            required
                        >
                            <option value="">-- Select a Student --</option>
                            {students.map((studentOption, index) => (
                                <option key={index} value={studentOption}>
                                    {studentOption}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label htmlFor="date">Lesson Date</label>
                        <input
                            type="date"
                            id="date"
                            value={lessonDate}
                            onChange={handleDateChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="startTime">Start Time</label>
                        <input
                            type="time"
                            id="startTime"
                            value={startTime}
                            onChange={handleStartTimeChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="endTime">End Time</label>
                        <input
                            type="time"
                            id="endTime"
                            value={endTime}
                            onChange={handleEndTimeChange}
                            required
                        />
                    </div>

                    <button type="submit" className="submit-btn">
                        Add
                    </button>
                </form>
            </div>
        </div>
    );
};

export default LessonsPage;
