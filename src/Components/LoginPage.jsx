import { useState, useEffect } from "react";

const Homepage = () => {
    const [welcomeMessage, setWelcomeMessage] = useState("");
    const [lessons, setLessons] = useState([]);
    const [homework, setHomework] = useState([]);
    const [pieces, setPieces] = useState([]);

    useEffect(() => {
        fetch("/api/welcome")
            .then((res) => res.json())
            .then((data) => setWelcomeMessage(data.message));

        fetch("/api/lessons")
            .then((res) => res.json())
            .then((data) => setLessons(data));

        fetch("/api/homework")
            .then((res) => res.json())
            .then((data) => setHomework(data));

        fetch("/api/pieces")
            .then((res) => res.json())
            .then((data) => setPieces(data));
    }, []);

    return (
        <div>
            <h1>{welcomeMessage}</h1>

            <h2>Upcoming Lessons</h2>
            <ul>
                {lessons.map((lesson, index) => (
                    <li key={index}>{lesson.lesson} - {lesson.time}</li>
                ))}
            </ul>

            <h2>Recent Homework</h2>
            <ul>
                {homework.map((task, index) => (
                    <li key={index}>{task.date}: {task.assignment}</li>
                ))}
            </ul>

            <h2>Pieces You're Working On</h2>
            <ul>
                {pieces.map((piece, index) => (
                    <li key={index}><strong>{piece.title}</strong>: {piece.focus}</li>
                ))}
            </ul>
        </div>
    );
};

export default Homepage;
