import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { API_BASE, authFetch } from "../lib/auth";

export default function HomeworkPage() {
  const { lessonId } = useParams();
  const [lesson, setLesson] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!lessonId) {
      return;
    }

    authFetch(`${API_BASE}/api/lesson/${lessonId}`)
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

  const handleDownload = async (filename) => {
    try {
      const res = await authFetch(`${API_BASE}/api/lesson/${lessonId}/file/${filename}`);
      if (!res.ok) {
        throw new Error("Failed to download file");
      }

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Homework download error:", err);
      window.alert("Could not download the file.");
    }
  };

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
                      <button type="button" onClick={() => handleDownload(file)}>
                        {file}
                      </button>
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
            <button className="submit-btn">Back to My Lessons</button>
          </Link>
        </div>
      </div>
    </div>
  );
}
