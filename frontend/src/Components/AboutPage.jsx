// src/Components/AboutPage.jsx

import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import '../App.css';

const sortByDateAdded = (data) => {
  // If pieces include a dateAdded attribute, sort them.
  // Otherwise return the data as is.
  return data.sort((a, b) => {
    if (a.dateAdded && b.dateAdded) {
      return new Date(b.dateAdded) - new Date(a.dateAdded);
    }
    return 0;
  });
};

const itemsPerPage = 5;
const paginate = (data, currentPage, itemsPerPage) =>
  data.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);
const totalPages = (data) => Math.ceil(data.length / itemsPerPage);

const AboutPage = () => {
  const { user, loading } = useUser();

  // State for each category of pieces
  const [favoritePieces, setFavoritePieces] = useState([]);
  const [wishlist, setWishlist] = useState([]);
  const [learningPieces, setLearningPieces] = useState([]);
  const [repertoire, setRepertoire] = useState([]);

  // Pagination state for each table
  const [currentFavoritePage, setCurrentFavoritePage] = useState(1);
  const [currentWishlistPage, setCurrentWishlistPage] = useState(1);
  const [currentLearningPage, setCurrentLearningPage] = useState(1);
  const [currentRepertoirePage, setCurrentRepertoirePage] = useState(1);

  // Input fields for adding a new piece to each category
  const [favoriteTitle, setFavoriteTitle] = useState('');
  const [favoriteComposer, setFavoriteComposer] = useState('');
  const [favoriteNotes, setFavoriteNotes] = useState('');

  const [wishlistTitle, setWishlistTitle] = useState('');
  const [wishlistComposer, setWishlistComposer] = useState('');
  const [wishlistNotes, setWishlistNotes] = useState('');

  const [learningTitle, setLearningTitle] = useState('');
  const [learningComposer, setLearningComposer] = useState('');
  const [learningNotes, setLearningNotes] = useState('');

  const [repertoireTitle, setRepertoireTitle] = useState('');
  const [repertoireComposer, setRepertoireComposer] = useState('');
  const [repertoireNotes, setRepertoireNotes] = useState('');

  // When the user object is loaded, initialize all piece lists from user properties
  useEffect(() => {
    if (user) {
      setFavoritePieces(user.favoritePieces || []);
      setWishlist(user.wishlist || []);
      setLearningPieces(user.workingOnPieces || []);
      setRepertoire(user.repertoire || []);
    }
  }, [user]);

  // Function to add a new piece via the API
  const addPiece = (category, setList, list, title, composer, notes, resetFields) => {
    if (!title || !composer) return;

    const newPiece = { title, composer, notes };

    fetch('http://localhost:8080/api/piece/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ ...newPiece, category })
    })
      .then((res) => res.text())
      .then(() => {
        setList([...list, newPiece]);
        resetFields();
      })
      .catch(error => console.error("Error adding piece:", error));
  };

  // Function to delete a piece from a category using the delete API
  const handleDeletePiece = (category, piece, list, setList) => {
    // The backend delete endpoint expects a JSON payload with keys: title, composer, notes, category.
    fetch('http://localhost:8080/api/piece/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        title: piece.title,
        composer: piece.composer,
        notes: piece.notes,
        category: category
      })
    })
      .then((res) => res.text())
      .then(() => {
        // Filter out the piece that matches the criteria from the list.
        const updatedList = list.filter(
          (p) => p.title !== piece.title || p.composer !== piece.composer || p.notes !== piece.notes
        );
        setList(updatedList);
      })
      .catch(error => console.error("Error deleting piece:", error));
  };

  // Render function for each category table that includes an "Actions" column with a delete button
  const renderTable = (
    tableTitle, list, currentPage, setCurrentPage, setList, category,
    inputTitle, setInputTitle, inputComposer, setInputComposer, inputNotes, setInputNotes
  ) => (
    <div className="table-wrapper">
      <table className="table">
        <caption>{tableTitle} </caption>
        <thead>
          <tr>
            <th>Title</th>
            <th>Composer</th>
            <th>Notes</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {paginate(sortByDateAdded(list), currentPage, itemsPerPage).map((piece, index) => (
            <tr key={index}>
              <td>{piece.title}</td>
              <td>{piece.composer}</td>
              <td>{piece.notes}</td>
              <td>
                <button onClick={() => handleDeletePiece(category, piece, list, setList)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Form for adding a new piece */}
      <div className="add-piece-form">
        <input
          type="text"
          placeholder="Title"
          value={inputTitle}
          onChange={(e) => setInputTitle(e.target.value)}
        />
        <input
          type="text"
          placeholder="Composer"
          value={inputComposer}
          onChange={(e) => setInputComposer(e.target.value)}
        />
        <input
          type="text"
          placeholder="Notes"
          value={inputNotes}
          onChange={(e) => setInputNotes(e.target.value)}
        />
        <button
          className="add-piece-btn"
          onClick={() =>
            addPiece(
              category,
              setList,
              list,
              inputTitle,
              inputComposer,
              inputNotes,
              () => {
                setInputTitle('');
                setInputComposer('');
                setInputNotes('');
              }
            )
          }
        >
          Add Piece
        </button>
      </div>

      <div className="pagination">
        <button onClick={() => setCurrentPage(Math.max(currentPage - 1, 1))} disabled={currentPage === 1}>
          Previous
        </button>
        <span>Page {currentPage} of {totalPages(list)}</span>
        <button onClick={() => setCurrentPage(Math.min(currentPage + 1, totalPages(list)))}
                disabled={currentPage === totalPages(list)}>
          Next
        </button>
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
        {renderTable(
          'Favorite Pieces',
          favoritePieces,
          currentFavoritePage,
          setCurrentFavoritePage,
          setFavoritePieces,
          'favorite',
          favoriteTitle,
          setFavoriteTitle,
          favoriteComposer,
          setFavoriteComposer,
          favoriteNotes,
          setFavoriteNotes
        )}
        {renderTable(
          'Wishlist',
          wishlist,
          currentWishlistPage,
          setCurrentWishlistPage,
          setWishlist,
          'wishlist',
          wishlistTitle,
          setWishlistTitle,
          wishlistComposer,
          setWishlistComposer,
          wishlistNotes,
          setWishlistNotes
        )}
        {renderTable(
          'Learning Pieces',
          learningPieces,
          currentLearningPage,
          setCurrentLearningPage,
          setLearningPieces,
          'workingonpieces',
          learningTitle,
          setLearningTitle,
          learningComposer,
          setLearningComposer,
          learningNotes,
          setLearningNotes
        )}
        {renderTable(
          'Repertoire',
          repertoire,
          currentRepertoirePage,
          setCurrentRepertoirePage,
          setRepertoire,
          'repertoire',
          repertoireTitle,
          setRepertoireTitle,
          repertoireComposer,
          setRepertoireComposer,
          repertoireNotes,
          setRepertoireNotes
        )}
      </div>
    </div>
  );
};

export default AboutPage;
