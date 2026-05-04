import React, { useState, useEffect } from 'react';
import { useUser } from '../Context/UserContext';
import { API_BASE, authFetch, getToken } from '../lib/auth';
import '../App.css';

const UPDATE_PROFILE_URL = `${API_BASE}/api/users/me`;
const PIECES_URL = `${API_BASE}/api/pieces`;

const sortByDateAdded = (data) => {
  return [...data].sort((a, b) => {
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
  const { user, loading, login } = useUser();

  // piece lists
  const [favoritePieces, setFavoritePieces] = useState([]);
  const [wishlist, setWishlist] = useState([]);
  const [learningPieces, setLearningPieces] = useState([]);
  const [repertoire, setRepertoire] = useState([]);

  // pagination
  const [currentFavoritePage, setCurrentFavoritePage] = useState(1);
  const [currentWishlistPage, setCurrentWishlistPage] = useState(1);
  const [currentLearningPage, setCurrentLearningPage] = useState(1);
  const [currentRepertoirePage, setCurrentRepertoirePage] = useState(1);

  // inputs
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

  // personal info state for inline edit
  const [profile, setProfile] = useState({ name: '', email: '', instrument: '' });
  const [editingField, setEditingField] = useState(null); // 'name' | 'email' | 'instrument' | null
  const [tempValue, setTempValue] = useState('');

  const loadPieces = async (category, setList) => {
    try {
      const res = await authFetch(`${PIECES_URL}?category=${encodeURIComponent(category)}`);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      setList(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(`Error loading ${category} pieces:`, err);
      setList([]);
    }
  };

  useEffect(() => {
    if (user) {
      setProfile({
        name: user.name || '',
        email: user.email || '',
        instrument: user.instrument || '',
      });

      loadPieces('favorite', setFavoritePieces);
      loadPieces('wishlist', setWishlist);
      loadPieces('working-on-pieces', setLearningPieces);
      loadPieces('repertoire', setRepertoire);
      return;
    }

    setProfile({ name: '', email: '', instrument: '' });
    setFavoritePieces([]);
    setWishlist([]);
    setLearningPieces([]);
    setRepertoire([]);
  }, [user]);

  const addPiece = async (category, setList, title, composer, notes, resetFields) => {
    if (!title || !composer) return;
    const newPiece = { title, composer, notes };

    try {
      const res = await authFetch(PIECES_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...newPiece, category })
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || `HTTP ${res.status}`);
      }

      resetFields();
      await loadPieces(category, setList);
    } catch (err) {
      console.error('Error adding piece:', err);
    }
  };

  const handleDeletePiece = async (category, piece, setList) => {
    if (!piece?.id) {
      console.error('Error deleting piece: missing piece id', piece);
      await loadPieces(category, setList);
      return;
    }

    try {
      const res = await authFetch(`${PIECES_URL}/${piece.id}`, {
        method: 'DELETE',
      });

      if (!res.ok) {
        const errorText = await res.text();
        if (res.status === 401) {
          const meRes = await authFetch(UPDATE_PROFILE_URL);
          const meText = await meRes.text();
          const token = getToken();

          console.error('Delete diagnostics', {
            pieceId: piece.id,
            category,
            tokenPresent: Boolean(token),
            tokenPreview: token ? `${token.slice(0, 16)}...` : null,
            deleteStatus: res.status,
            deleteBody: errorText || null,
            currentUserStatus: meRes.status,
            currentUserBody: meText || null,
          });
        }
        throw new Error(errorText || `HTTP ${res.status}`);
      }

      await loadPieces(category, setList);
    } catch (err) {
      console.error('Error deleting piece:', err);
    }
  };

  const beginEdit = (field) => {
    setEditingField(field);
    setTempValue(profile[field] ?? '');
  };

  const cancelEdit = () => {
    setEditingField(null);
    setTempValue('');
  };

  const saveEdit = async () => {
    if (!editingField) return;
    const payload = { [editingField]: tempValue };

    try {
      const res = await authFetch(UPDATE_PROFILE_URL, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      let next = { ...profile, [editingField]: tempValue };
      if (res.ok) {
        try {
          const updated = await res.json();
          login(updated);
          const updatedUser = updated.user ?? updated;
          next = {
            name: updatedUser.name ?? next.name,
            email: updatedUser.email ?? next.email,
            instrument: updatedUser.instrument ?? next.instrument
          };
        } catch {
          // no JSON body, keep optimistic next
        }
        setProfile(next);
        setEditingField(null);
        setTempValue('');
      } else {
        const txt = await res.text();
        throw new Error(txt || `HTTP ${res.status}`);
      }
    } catch (e) {
      console.error('Update profile failed:', e);
      alert('Username already exists or invalid input.');
    }
  };

  const renderTable = (
    tableTitle, list, currentPage, setCurrentPage, setList, category,
    inputTitle, setInputTitle, inputComposer, setInputComposer, inputNotes, setInputNotes
  ) => (
    <div className="table-wrapper">
      <table className="table">
        <caption>{tableTitle}</caption>
        <thead>
          <tr>
            <th>Title</th>
            <th>Composer</th>
            <th>Notes</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {paginate(sortByDateAdded(list), currentPage, itemsPerPage).map((piece, idx) => (
            <tr key={piece.id ?? idx}>
              <td>{piece.title}</td>
              <td>{piece.composer}</td>
              <td>{piece.notes}</td>
              <td>
                <button onClick={() => handleDeletePiece(category, piece, setList)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}

          {/* input row */}
          <tr className="input-row">
            <td>
              <input
                type="text"
                placeholder="Title"
                value={inputTitle}
                onChange={(e) => setInputTitle(e.target.value)}
              />
            </td>
            <td>
              <input
                type="text"
                placeholder="Composer"
                value={inputComposer}
                onChange={(e) => setInputComposer(e.target.value)}
              />
            </td>
            <td>
              <input
                type="text"
                placeholder="Notes"
                value={inputNotes}
                onChange={(e) => setInputNotes(e.target.value)}
              />
            </td>
            <td>
              <button
                onClick={() =>
                  addPiece(
                    category,
                    setList,
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
                Add
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div className="pagination">
        <button onClick={() => setCurrentPage(Math.max(currentPage - 1, 1))} disabled={currentPage === 1}>
          Previous
        </button>
        <span>Page {currentPage} of {totalPages(list)}</span>
        <button
          onClick={() => setCurrentPage(Math.min(currentPage + 1, totalPages(list)))}
          disabled={currentPage === totalPages(list)}
        >
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

      {/* summary table with inline edit pens */}
      <div className="user-summary-pair">
        <table className="table mini-summary">
          <tbody>
            <tr>
              <td className="summary-cell">
                <strong>Name:</strong>{' '}
                {editingField === 'name' ? (
                  <>
                    <input
                      type="text"
                      value={tempValue}
                      onChange={(e) => setTempValue(e.target.value)}
                    />
                    <button className="icon-button" onClick={saveEdit} aria-label="Save">✓</button>
                    <button className="icon-button" onClick={cancelEdit} aria-label="Cancel">✕</button>
                  </>
                ) : (
                  <>
                    {profile.name || '-'}
                    <button
                      className="icon-button"
                      title="Edit name"
                      onClick={() => beginEdit('name')}
                      aria-label="Edit name"
                    >
                      🖉
                    </button>
                  </>
                )}
              </td>

              <td className="summary-cell">
                <strong>Email:</strong>{' '}
                {editingField === 'email' ? (
                  <>
                    <input
                      type="email"
                      value={tempValue}
                      onChange={(e) => setTempValue(e.target.value)}
                    />
                    <button className="icon-button" onClick={saveEdit} aria-label="Save">✓</button>
                    <button className="icon-button" onClick={cancelEdit} aria-label="Cancel">✕</button>
                  </>
                ) : (
                  <>
                    {profile.email || '-'}
                    <button
                      className="icon-button"
                      title="Edit email"
                      onClick={() => beginEdit('email')}
                      aria-label="Edit email"
                    >
                      🖉
                    </button>
                  </>
                )}
              </td>

              <td className="summary-cell">
                <strong>Instrument:</strong>{' '}
                {editingField === 'instrument' ? (
                  <>
                    <input
                      type="text"
                      value={tempValue}
                      onChange={(e) => setTempValue(e.target.value)}
                      placeholder="e.g. Piano"
                    />
                    <button className="icon-button" onClick={saveEdit} aria-label="Save">✓</button>
                    <button className="icon-button" onClick={cancelEdit} aria-label="Cancel">✕</button>
                  </>
                ) : (
                  <>
                    {profile.instrument || 'None'}
                    <button
                      className="icon-button"
                      title="Edit instrument"
                      onClick={() => beginEdit('instrument')}
                      aria-label="Edit instrument"
                    >
                      🖉
                    </button>
                  </>
                )}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <img src="src/assets/pfp.png" alt="Profile" className="profile-photo" />

      <div className="table-container">
        <div className="tables-flex">
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
        </div>
        <div className="tables-flex">
          {renderTable(
            'Learning Pieces',
            learningPieces,
            currentLearningPage,
            setCurrentLearningPage,
            setLearningPieces,
            'working-on-pieces',
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
    </div>
  );
};

export default AboutPage;
