import React, { useEffect, useState } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

const ITEMS_PER_PAGE = 5;

const Admindashboard = () => {
  const { user } = useUser();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  // 🔹 Fetch all users except the logged-in admin
  const fetchUsers = () => {
    fetch('http://localhost:8080/api/user/all', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        // ✅ Filter out the current admin AND all admin accounts
        const filtered = Array.isArray(data)
          ? data.filter(
              u =>
                String(u.id) !== String(user?.id) &&
                u.role.toUpperCase() !== 'ADMIN'
            )
          : [];
        setUsers(filtered);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching users:', err);
        setUsers([]);
        setLoading(false);
      });
  };

  useEffect(() => {
    if (user) fetchUsers();
  }, [user]);

  // 🔹 Toggle user role (teacher <-> student)
  const toggleUserRole = (userId) => {
    fetch(`http://localhost:8080/api/user/toggle-role/${userId}`, {
      method: 'PATCH',
      credentials: 'include'
    })
      .then(res => (res.ok ? fetchUsers() : alert('Failed to update user role.')))
      .catch(err => console.error('Toggle role error:', err));
  };

  // 🔹 Delete user
  const deleteUser = (userId) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    fetch(`http://localhost:8080/api/user/delete/${userId}`, {
      method: 'DELETE',
      credentials: 'include'
    })
      .then(res => (res.ok ? fetchUsers() : alert('Failed to delete user.')))
      .catch(err => console.error('Delete error:', err));
  };

  if (loading) return <div>Loading...</div>;

  // 🔍 Filter users by name or email
  const filteredUsers = users.filter(u => {
    if (!search.trim()) return true;
    return (
      u.name?.toLowerCase().includes(search.toLowerCase()) ||
      u.email?.toLowerCase().includes(search.toLowerCase())
    );
  });

  // 🔹 Pagination
  const totalPages = Math.ceil(filteredUsers.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedUsers = filteredUsers.slice(startIndex, startIndex + ITEMS_PER_PAGE);

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
            onChange={(e) => {
              setSearch(e.target.value);
              setCurrentPage(1); // reset to first page when searching
            }}
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
              {paginatedUsers.length > 0 ? (
                paginatedUsers.map((u, index) => (
                  <tr key={index}>
                    <td>{u.name}</td>
                    <td className="email-col">{u.email}</td>
                    <td>{u.instrument || 'N/A'}</td>
                    <td>{u.role}</td>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="action-btn role-btn"
                          onClick={() => toggleUserRole(u.id)}
                        >
                          {u.role === 'TEACHER'
                            ? 'Demote to Student'
                            : 'Promote to Teacher'}
                        </button>
                        <button
                          className="action-btn delete-btn"
                          onClick={() => deleteUser(u.id)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center' }}>
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* 🔹 Pagination controls */}
          <div className="pagination">
            <button
              onClick={() => setCurrentPage(p => Math.max(p - 1, 1))}
              disabled={currentPage === 1}
            >
              Previous
            </button>
            <span>
              Page {currentPage} of {totalPages || 1}
            </span>
            <button
              onClick={() => setCurrentPage(p => Math.min(p + 1, totalPages))}
              disabled={currentPage === totalPages || totalPages === 0}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Admindashboard;
