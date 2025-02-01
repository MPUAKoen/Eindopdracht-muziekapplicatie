import React, { useState } from 'react';
import '../App.css';

const AboutPage = () => {
    const repertoireData = [
        { stuk: "'Ein Mädchen oder Weibchen'", componist: "Wolfgang Amadeus Mozart", type: "Opera" },
        { stuk: "'Pa-Pa-Papagena'", componist: "Wolfgang Amadeus Mozart", type: "Duet" },
        { stuk: "'Notte giorno faticar'", componist: "Wolfgang Amadeus Mozart", type: "Aria" },
        { stuk: "'Largo al factotum'", componist: "Gioachino Rossini", type: "Aria" },
        { stuk: "'O mio babbino caro'", componist: "Giacomo Puccini", type: "Aria" },
        { stuk: "'Voi che sapete'", componist: "Wolfgang Amadeus Mozart", type: "Aria" },
    ];

    const favoritePieces = [
        { stuk: "'Notte giorno faticar'", componist: "Wolfgang Amadeus Mozart" },
        { stuk: "'Nel cor più non mi sento'", componist: "Giuseppe Sarti" },
        { stuk: "'La ci darem la mano'", componist: "Wolfgang Amadeus Mozart" },
        { stuk: "'Caro mio ben'", componist: "Giuseppe Giordani" },
    ];

    const learningPieces = [
        { stuk: "'Concone 3'", componist: "Giuseppe Concone" },
        { stuk: "'Ein Mädchen oder Weibchen'", componist: "Wolfgang Amadeus Mozart" },
        { stuk: "'Ave Maria'", componist: "Franz Schubert" },
        { stuk: "'Casta Diva'", componist: "Vincenzo Bellini" },
    ];

    const wishlist = [
        { stuk: "'Requiem'", componist: "Wolfgang Amadeus Mozart" },
        { stuk: "'La Traviata'", componist: "Giuseppe Verdi" },
    ];

    const itemsPerPage = 2;
    const [currentRepertoirePage, setCurrentRepertoirePage] = useState(1);
    const [currentFavoritePage, setCurrentFavoritePage] = useState(1);
    const [currentLearningPage, setCurrentLearningPage] = useState(1);
    const [currentWishlistPage, setCurrentWishlistPage] = useState(1);

    const paginate = (data, currentPage, itemsPerPage) => {
        const indexOfLastItem = currentPage * itemsPerPage;
        const indexOfFirstItem = indexOfLastItem - itemsPerPage;
        return data.slice(indexOfFirstItem, indexOfLastItem);
    };

    const totalPages = (data) => Math.ceil(data.length / itemsPerPage);

    const nextPage = (setCurrentPage, currentPage, totalPages) => {
        if (currentPage < totalPages) {
            setCurrentPage(currentPage + 1);
        }
    };

    const prevPage = (setCurrentPage, currentPage) => {
        if (currentPage > 1) {
            setCurrentPage(currentPage - 1);
        }
    };

    return (
        <div className="mainpage">
            <div className="header">
                <h1>My Profile</h1>
            </div>
            <img src="src/assets/pfp.png" alt="Koen Green" className="profile-photo" />

            <div className="table-container">
                <table className="table">
                    <caption>Persoonsgegevens</caption>
                    <tbody>
                    <tr><td><strong>Voornaam:</strong> Koen</td></tr>
                    <tr><td><strong>Achternaam:</strong> Green</td></tr>
                    <tr><td><strong>Email:</strong> koen.green@email.com</td></tr>
                    <tr><td><strong>Telefoonnummer:</strong> 0612345678</td></tr>
                    <tr><td><strong>Instrument:</strong> Voice</td></tr>
                    </tbody>
                </table>

                {/* Flexbox to display tables side by side */}
                <div className="tables-flex">
                    <div className="table-wrapper">
                        <table className="table">
                            <caption>Favoriete Stukken</caption>
                            <thead>
                            <tr><th>Stuk</th><th>Componist</th></tr>
                            </thead>
                            <tbody>
                            {paginate(favoritePieces, currentFavoritePage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        <div className="pagination">
                            <button onClick={() => prevPage(setCurrentFavoritePage, currentFavoritePage)} disabled={currentFavoritePage === 1}>Previous</button>
                            <span>Page {currentFavoritePage} of {totalPages(favoritePieces)}</span>
                            <button onClick={() => nextPage(setCurrentFavoritePage, currentFavoritePage, totalPages(favoritePieces))} disabled={currentFavoritePage === totalPages(favoritePieces)}>Next</button>
                        </div>
                    </div>

                    <div className="table-wrapper">
                        <table className="table">
                            <caption>Wishlist</caption>
                            <thead>
                            <tr><th>Stuk</th><th>Componist</th></tr>
                            </thead>
                            <tbody>
                            {paginate(wishlist, currentWishlistPage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        <div className="pagination">
                            <button onClick={() => prevPage(setCurrentWishlistPage, currentWishlistPage)} disabled={currentWishlistPage === 1}>Previous</button>
                            <span>Page {currentWishlistPage} of {totalPages(wishlist)}</span>
                            <button onClick={() => nextPage(setCurrentWishlistPage, currentWishlistPage, totalPages(wishlist))} disabled={currentWishlistPage === totalPages(wishlist)}>Next</button>
                        </div>
                    </div>
                </div>

                <div className="tables-flex">
                    <div className="table-wrapper">
                        <table className="table">
                            <caption>Momenteel Aan Het Leren</caption>
                            <thead>
                            <tr><th>Stuk</th><th>Componist</th></tr>
                            </thead>
                            <tbody>
                            {paginate(learningPieces, currentLearningPage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        <div className="pagination">
                            <button onClick={() => prevPage(setCurrentLearningPage, currentLearningPage)} disabled={currentLearningPage === 1}>Previous</button>
                            <span>Page {currentLearningPage} of {totalPages(learningPieces)}</span>
                            <button onClick={() => nextPage(setCurrentLearningPage, currentLearningPage, totalPages(learningPieces))} disabled={currentLearningPage === totalPages(learningPieces)}>Next</button>
                        </div>
                    </div>

                    <div className="table-wrapper">
                        <table className="table">
                            <caption>Repertoire</caption>
                            <thead>
                            <tr><th>Stuk</th><th>Componist</th><th>Type</th></tr>
                            </thead>
                            <tbody>
                            {paginate(repertoireData, currentRepertoirePage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                    <td>{item.type}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        <div className="pagination">
                            <button onClick={() => prevPage(setCurrentRepertoirePage, currentRepertoirePage)} disabled={currentRepertoirePage === 1}>Previous</button>
                            <span>Page {currentRepertoirePage} of {totalPages(repertoireData)}</span>
                            <button onClick={() => nextPage(setCurrentRepertoirePage, currentRepertoirePage, totalPages(repertoireData))} disabled={currentRepertoirePage === totalPages(repertoireData)}>Next</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AboutPage;
