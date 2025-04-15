import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

const formatDate = (dateString) => {
  const date = new Date(dateString);
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  return `${day}-${month}-${year}`;
};

const sortByDateAdded = (data) => data.sort((a, b) => new Date(b.dateAdded) - new Date(a.dateAdded));
const itemsPerPage = 5;
const paginate = (data, currentPage, itemsPerPage) => data.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);
const totalPages = (data) => Math.ceil(data.length / itemsPerPage);

const AboutPage = () => {
  const { user, loading } = useUser();

  const [favoritePieces, setFavoritePieces] = useState([]);
  const [wishlist, setWishlist] = useState([]);
  const [learningPieces, setLearningPieces] = useState([]);
  const [repertoire, setRepertoire] = useState([]);

  const [currentFavoritePage, setCurrentFavoritePage] = useState(1);
  const [currentWishlistPage, setCurrentWishlistPage] = useState(1);
  const [currentLearningPage, setCurrentLearningPage] = useState(1);
  const [currentRepertoirePage, setCurrentRepertoirePage] = useState(1);

  const [newPieceTitle, setNewPieceTitle] = useState('');
  const [newPieceComposer, setNewPieceComposer] = useState('');
  const [newPieceNotes, setNewPieceNotes] = useState('');

  useEffect(() => {
    if (user) {
      setFavoritePieces(user.favoritePieces || []);
      setWishlist(user.wishlist || []);
      setLearningPieces(user.workingOnPieces || []);
      setRepertoire(user.repertoire || []);
    }
  }, [user]);

  const addPiece = (category, setList, list) => {
    if (!newPieceTitle || !newPieceComposer) return;
    const newPiece = {
      title: newPieceTitle,
      composer: newPieceComposer,
      notes: newPieceNotes,
      dateAdded: new Date().toISOString().split('T')[0]
    };

    fetch('http://localhost:8080/api/piece/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...newPiece, category }),
      credentials: 'include'
    })
      .then((res) => res.text())
      .then(() => {
        setList([...list, newPiece]);
        setNewPieceTitle('');
        setNewPieceComposer('');
        setNewPieceNotes('');
      });
  };

  const renderTable = (title, list, currentPage, setCurrentPage, setList, category) => (
    <div className="table-wrapper">
      <table className="table">
        <caption>{title} {user ? user.name : 'Guest'}</caption>
        <thead>
          <tr>
            <th>Title</th>
            <th>Composer</th>
            <th>Notes</th>
            <th>Date Added</th>
          </tr>
        </thead>
        <tbody>
          {paginate(sortByDateAdded(list), currentPage, itemsPerPage).map((item, index) => (
            <tr key={index}>
              <td>{item.title}</td>
              <td>{item.composer}</td>
              <td>{item.notes}</td>
              <td>{formatDate(item.dateAdded)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="add-piece-form">
        <input
          type="text"
          placeholder="Title"
          value={newPieceTitle}
          onChange={(e) => setNewPieceTitle(e.target.value)}
        />
        <input
          type="text"
          placeholder="Composer"
          value={newPieceComposer}
          onChange={(e) => setNewPieceComposer(e.target.value)}
        />
        <input
          type="text"
          placeholder="Notes"
          value={newPieceNotes}
          onChange={(e) => setNewPieceNotes(e.target.value)}
        />
        <button className="add-piece-btn" onClick={() => addPiece(category, setList, list)}>Add Piece</button>
      </div>
      <div className="pagination">
        <button onClick={() => setCurrentPage(Math.max(currentPage - 1, 1))} disabled={currentPage === 1}>Previous</button>
        <span>Page {currentPage} of {totalPages(list)}</span>
        <button onClick={() => setCurrentPage(Math.min(currentPage + 1, totalPages(list)))} disabled={currentPage === totalPages(list)}>Next</button>
      </div>
    </div>
  );

  if (loading) return <div>Loading...</div>;

  return (
    <div className="mainpage">
      <div className="header">
        <h1>My Profile</h1>
      </div>
      <img src="src/assets/pfp.png" alt="Profile" className="profile-photo" />

      <div className="table-container">
        {renderTable('Favorite Pieces', favoritePieces, currentFavoritePage, setCurrentFavoritePage, setFavoritePieces, 'favorite')}
        {renderTable('Wishlist', wishlist, currentWishlistPage, setCurrentWishlistPage, setWishlist, 'wishlist')}
        {renderTable('Learning Pieces', learningPieces, currentLearningPage, setCurrentLearningPage, setLearningPieces, 'workingonpieces')}
        {renderTable('Repertoire', repertoire, currentRepertoirePage, setCurrentRepertoirePage, setRepertoire, 'repertoire')}
      </div>
    </div>
  );
};

export default AboutPage;