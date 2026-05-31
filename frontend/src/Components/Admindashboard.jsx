import React, { useEffect, useState } from 'react';
import { useUser } from '../Context/UserContext';
import { API_BASE, authFetch } from '../lib/auth';
import Button from './ui/Button';
import Input from './ui/Input';
import Pagination from './ui/Pagination';
import '../App.css';

const ITEMS_PER_PAGE = 5;

const Admindashboard = () => {
  const { user } = useUser();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [actionInProgress, setActionInProgress] = useState(false);
  const [search, setSearch] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const fetchUsers = async ({ showRefresh = false } = {}) => {
    if (showRefresh) {
      setRefreshing(true);
    }

    try {
      const res = await authFetch(`${API_BASE}/api/users`);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      const filtered = Array.isArray(data)
        ? data.filter(
            u =>
              String(u.id) !== String(user?.id) &&
              u.role?.toUpperCase() !== 'ADMIN'
          )
        : [];

      setUsers(filtered);
      setCurrentPage(page => {
        const nextTotalPages = Math.max(1, Math.ceil(filtered.length / ITEMS_PER_PAGE));
        return Math.min(page, nextTotalPages);
      });
    } catch (err) {
      console.error('Error fetching users:', err);
      setUsers([]);
      alert('Could not refresh users. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    if (user) fetchUsers();
  }, [user]);

  const toggleUserRole = async (userId) => {
    if (actionInProgress) return;

    const selectedUser = users.find((u) => String(u.id) === String(userId));
    const nextRole = selectedUser?.role === 'TEACHER' ? 'USER' : 'TEACHER';

    setActionInProgress(true);
    try {
      const res = await authFetch(`${API_BASE}/api/users/${userId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role: nextRole }),
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      await fetchUsers();
    } catch (err) {
      console.error('Toggle role error:', err);
      alert('Failed to update user role. Refreshing the list.');
      await fetchUsers({ showRefresh: true });
    } finally {
      setActionInProgress(false);
    }
  };

  const deleteUser = async (userId) => {
    if (actionInProgress) return;
    if (!window.confirm('Are you sure you want to delete this user?')) return;

    setActionInProgress(true);
    try {
      const res = await authFetch(`${API_BASE}/api/users/${userId}`, {
        method: 'DELETE',
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      setUsers(currentUsers => currentUsers.filter(u => String(u.id) !== String(userId)));
      await fetchUsers();
    } catch (err) {
      console.error('Delete error:', err);
      alert('Failed to delete user. Refreshing the list.');
      await fetchUsers({ showRefresh: true });
    } finally {
      setActionInProgress(false);
    }
  };

  if (loading) return <div>Loading...</div>;

  const filteredUsers = users.filter(u => {
    if (!search.trim()) return true;
    return (
      u.name?.toLowerCase().includes(search.toLowerCase()) ||
      u.email?.toLowerCase().includes(search.toLowerCase())
    );
  });

  const totalPages = Math.ceil(filteredUsers.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedUsers = filteredUsers.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  return (
    <div className="app-container">
      <div className="mainpage">
        <div className="header">
          <h1>Admin Dashboard</h1>
        </div>

        <div className="admin-toolbar">
          <div className="search-bar">
            <Input
              id="admin-user-search"
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setCurrentPage(1);
              }}
              placeholder="Search by name or email..."
            />
          </div>

          <Button
            className="refresh-btn"
            type="button"
            onClick={() => fetchUsers({ showRefresh: true })}
            disabled={refreshing || actionInProgress}
          >
            {refreshing ? 'Refreshing...' : 'Refresh'}
          </Button>
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
                paginatedUsers.map((u) => (
                  <tr key={u.id}>
                    <td>{u.name}</td>
                    <td className="email-col">{u.email}</td>
                    <td>{u.instrument || 'N/A'}</td>
                    <td>{u.role}</td>
                    <td>
                      <div className="action-buttons">
                        <Button
                          className="action-btn role-btn"
                          disabled={actionInProgress}
                          onClick={() => toggleUserRole(u.id)}
                        >
                          {u.role === 'TEACHER'
                            ? 'Demote to Student'
                            : 'Promote to Teacher'}
                        </Button>
                        <Button
                          className="action-btn delete-btn"
                          disabled={actionInProgress}
                          onClick={() => deleteUser(u.id)}
                        >
                          Delete
                        </Button>
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

          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        </div>
      </div>
    </div>
  );
};

export default Admindashboard;
