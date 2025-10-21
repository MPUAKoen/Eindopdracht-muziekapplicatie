import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import '../App.css';

const API_BASE = 'http://localhost:8080';

// Helper functions
const sortByDateAdded = (data) => {
  return (data ? [...data] : []).sort((a, b) => {
    if (a.dateAdded && b.dateAdded) {
      return new Date(b.dateAdded) - new Date(a.dateAdded);
    }
    return 0;
  });
};

const itemsPerPage = 5;
const paginate = (data, currentPage, itemsPerPage) =>
  (data || []).slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);
const totalPages = (data) => Math.ceil(((data || []).length) / itemsPerPage);

const StudentAboutPage = () => {
  const { id } = useParams();
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);

  // Pagination states
  const [currentFavoritePage, setCurrentFavoritePage] = useState(1);
  const [currentWishlistPage, setCurrentWishlistPage] = useState(1);
  const [currentLearningPage, setCurrentLearningPage] = useState(1);
  const [currentRepertoirePage, setCurrentRepertoirePage] = useState(1);

  useEffect(() => {
    fetch(`${API_BASE}/api/user/${id}`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
      .then(setStudent)
      .catch((err) => console.error('Error fetching student:', err))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading)
    return (
      <div className="mainpage">
        <div className="header">
          <h1>Loading...</h1>
        </div>
      </div>
    );

  if (!student)
    return (
      <div className="mainpage">
        <div className="header">
          <h1>Student not found</h1>
        </div>
      </div>
    );

  const renderTable = (title, list, currentPage, setCurrentPage) => (
    <div className="table-wrapper">
      <table className="table">
        <caption>{title}</caption>
        <thead>
          <tr>
            <th>Title</th>
            <th>Composer</th>
            <th>Notes</th>
          </tr>
        </thead>
        <tbody>
          {paginate(sortByDateAdded(list || []), currentPage, itemsPerPage).length > 0 ? (
            paginate(sortByDateAdded(list || []), currentPage, itemsPerPage).map((piece, index) => (
              <tr key={index}>
                <td>{piece.title}</td>
                <td>{piece.composer}</td>
                <td>{piece.notes}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="3">No pieces yet.</td>
            </tr>
          )}
        </tbody>
      </table>

      {/* Pagination controls */}
      <div className="pagination">
        <button
          onClick={() => setCurrentPage(Math.max(currentPage - 1, 1))}
          disabled={currentPage === 1}
        >
          Previous
        </button>
        <span>
          Page {currentPage} of {totalPages(list)}
        </span>
        <button
          onClick={() => setCurrentPage(Math.min(currentPage + 1, totalPages(list)))}
          disabled={currentPage === totalPages(list) || totalPages(list) === 0}
        >
          Next
        </button>
      </div>
    </div>
  );

  const name = student.name ?? '-';
  const email = student.email ?? '-';
  const instrument = student.instrument ?? 'None';

  return (
    <div className="mainpage">
      <div className="header">
        <h1>{student.name}: Profile</h1>
      </div>

      <img src="/src/assets/pfp.png" alt="Profile" className="profile-photo" />

      {/* Compact summary table + spacing before pieces */}
      <div className="user-summary-pair">
        <table className="table mini-summary">
          <tbody>
            <tr>
              <td><strong>Name:</strong> {name}</td>
              <td><strong>Email:</strong> {email}</td>
              <td><strong>Instrument:</strong> {instrument}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className="table-container">
        <div className="tables-flex">
          {renderTable('Favorite Pieces', student.favoritePieces, currentFavoritePage, setCurrentFavoritePage)}
          {renderTable('Wishlist', student.wishlist, currentWishlistPage, setCurrentWishlistPage)}
        </div>

        <div className="tables-flex">
          {renderTable('Learning Pieces', student.workingOnPieces, currentLearningPage, setCurrentLearningPage)}
          {renderTable('Repertoire', student.repertoire, currentRepertoirePage, setCurrentRepertoirePage)}
        </div>
      </div>
    </div>
  );
};

export default StudentAboutPage;
