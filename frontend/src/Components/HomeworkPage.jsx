import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";

const API_BASE = "http://localhost:8080";

export default function HomeworkPage() {
  const { lessonId } = useParams(); // <-- must match :lessonId in route
  const [lesson, setLesson] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!lessonId) return; // prevent undefined fetch

    fetch(`${API_BASE}/api/lesson/${lessonId}`, { credentials: "include" })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to load lesson");
        return res.json();
      })
      .then((data) => {
        setLesson(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Homework fetch error:", err);
        setLoading(false);
      });
  }, [lessonId]);

  if (loading) return <div>Loading...</div>;
  if (!lesson) return <div>No lesson found</div>;

  return (
    <div className="mainpage">
      <div className="header">
        <h1>Homework</h1>
      </div>

      <div className="dashboard">
        <table className="table">
          <caption>Lesson Homework</caption>
          <tbody>
            <tr>
              <th>PDFs</th>
              <td>
                {lesson.pdfFileNames?.length > 0 ? (
                  lesson.pdfFileNames.map((file, i) => (
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
                ) : (
                  "No files"
                )}
              </td>
            </tr>
            <tr>
              <th>Homework</th>
              <td>{lesson.homework || "No homework given"}</td>
            </tr>
          </tbody>
        </table>

        <div style={{ marginTop: "20px" }}>
          <Link to="/mylessons">
            <button className="submit-btn">⬅ Back to My Lessons</button>
          </Link>
        </div>
      </div>
    </div>
  );
}
