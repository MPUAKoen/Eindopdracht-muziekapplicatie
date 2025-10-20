import React, { useEffect, useState } from 'react';
import { useUser } from '../Context/UserContext'; // <-- import logged in user
import '../App.css';

const Admindashboard = () => {
  const { user } = useUser(); // <-- get logged in user
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  const fetchStudents = () => {
    fetch('http://localhost:8080/api/user/all', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        console.log("Fetched data:", data);
        console.log("Logged-in user:", user);

        // ✅ Only filter out the logged-in account
        const filtered = Array.isArray(data)
          ? data.filter(s => String(s.id) !== String(user?.id))
          : [];

        console.log("Filtered students:", filtered);
        setStudents(filtered);
        setLoading(false);
      })
      .catch(err => {
        console.error("Error fetching students:", err);
        setStudents([]);
        setLoading(false);
      });
  };

  useEffect(() => {
    if (user) {
      fetchStudents();
    }
  }, [user]);

  const toggleUserRole = (userId) => {
    fetch(`http://localhost:8080/api/user/toggle-role/${userId}`, {
      method: 'PATCH',
      credentials: 'include'
    })
      .then(res => {
        if (res.ok) {
          fetchStudents();
        } else {
          alert("Failed to update user role.");
        }
      })
      .catch(err => console.error("Toggle role error:", err));
  };

  const deleteUser = (userId) => {
    if (!window.confirm("Are you sure you want to delete this user?")) return;

    fetch(`http://localhost:8080/api/user/delete/${userId}`, {
      method: 'DELETE',
      credentials: 'include'
    })
      .then(res => {
        if (res.ok) {
          fetchStudents();
        } else {
          alert("Failed to delete user.");
        }
      })
      .catch(err => console.error("Delete error:", err));
  };

  if (loading) return <div>Loading...</div>;

  // Filter students by search (supports name + email)
  const filteredStudents = students.filter(s => {
    if (!search.trim()) return true; // show all if search is empty
    return (
      s.name?.toLowerCase().includes(search.toLowerCase()) ||
      s.email?.toLowerCase().includes(search.toLowerCase())
    );
  });

  return (
    <div className="app-container">
      <div className="mainpage">
        <div className="header">
          <h1>Admin Dashboard</h1>
        </div>

        {/* 🔍 Search bar */}
        <div className="search-bar">
          <input
            type="text"
            placeholder="Search by name or email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="table-container">
          <table className="table">
            <caption>Users</caption>
            <thead>
              <tr>
                <th>Name</th>
                <th className="email-col">Email</th>
                <th>Instrument</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredStudents.map((student, index) => (
                <tr key={index}>
                  <td>{student.name}</td>
                  <td className="email-col">{student.email}</td>
                  <td>{student.instrument || 'N/A'}</td>
                  <td>{student.role}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="action-btn role-btn"
                        onClick={() => toggleUserRole(student.id)}
                      >
                        {student.role === 'TEACHER'
                          ? 'Demote to Student'
                          : 'Promote to Teacher'}
                      </button>
                      <button
                        className="action-btn delete-btn"
                        onClick={() => deleteUser(student.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button disabled>Previous</button>
            <span>Page 1 of 1</span>
            <button disabled>Next</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Admindashboard;
