import React, { useState } from 'react';
import '../App.css';

const AboutPage = () => {
    // Add dateAdded field to each item
    const repertoireData = [
        { stuk: "'Ein Mädchen oder Weibchen'", componist: "Wolfgang Amadeus Mozart", type: "Opera", dateAdded: "2023-10-01" },
        { stuk: "'Pa-Pa-Papagena'", componist: "Wolfgang Amadeus Mozart", type: "Duet", dateAdded: "2023-09-15" },
        { stuk: "'Notte giorno faticar'", componist: "Wolfgang Amadeus Mozart", type: "Aria", dateAdded: "2023-10-05" },
        { stuk: "'Largo al factotum'", componist: "Gioachino Rossini", type: "Aria", dateAdded: "2023-08-20" },
        { stuk: "'O mio babbino caro'", componist: "Giacomo Puccini", type: "Aria", dateAdded: "2023-09-01" },
        { stuk: "'Voi che sapete'", componist: "Wolfgang Amadeus Mozart", type: "Aria", dateAdded: "2023-10-10" },
    ];

    const favoritePieces = [
        { stuk: "'Notte giorno faticar'", componist: "Wolfgang Amadeus Mozart", dateAdded: "2023-09-10" },
        { stuk: "'Nel cor più non mi sento'", componist: "Giuseppe Sarti", dateAdded: "2023-08-25" },
        { stuk: "'La ci darem la mano'", componist: "Wolfgang Amadeus Mozart", dateAdded: "2023-10-02" },
        { stuk: "'Caro mio ben'", componist: "Giuseppe Giordani", dateAdded: "2023-09-20" },
    ];

    const learningPieces = [
        { stuk: "'Concone 3'", componist: "Giuseppe Concone", dateAdded: "2023-08-15" },
        { stuk: "'Ein Mädchen oder Weibchen'", componist: "Wolfgang Amadeus Mozart", dateAdded: "2023-09-05" },
        { stuk: "'Ave Maria'", componist: "Franz Schubert", dateAdded: "2023-10-01" },
        { stuk: "'Casta Diva'", componist: "Vincenzo Bellini", dateAdded: "2023-09-30" },
    ];

    const wishlist = [
        { stuk: "'Requiem'", componist: "Wolfgang Amadeus Mozart", dateAdded: "2023-08-10" },
        { stuk: "'La Traviata'", componist: "Giuseppe Verdi", dateAdded: "2023-09-25" },
    ];

    // Helper function to format date as dag-maand-jaar
    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const day = String(date.getDate()).padStart(2, '0'); // Ensure 2 digits
        const month = String(date.getMonth() + 1).padStart(2, '0'); // Months are 0-indexed
        const year = date.getFullYear();
        return `${day}-${month}-${year}`;
    };

    // Sort data by dateAdded (newest first)
    const sortByDateAdded = (data) => {
        return data.sort((a, b) => new Date(b.dateAdded) - new Date(a.dateAdded));
    };

    const itemsPerPage = 5;
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
                            <tr><th>Stuk</th><th>Componist</th><th>Date Added</th></tr>
                            </thead>
                            <tbody>
                            {paginate(sortByDateAdded(favoritePieces), currentFavoritePage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                    <td>{formatDate(item.dateAdded)}</td>
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
                            <tr><th>Stuk</th><th>Componist</th><th>Date Added</th></tr>
                            </thead>
                            <tbody>
                            {paginate(sortByDateAdded(wishlist), currentWishlistPage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                    <td>{formatDate(item.dateAdded)}</td>
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
                            <tr><th>Stuk</th><th>Componist</th><th>Date Added</th></tr>
                            </thead>
                            <tbody>
                            {paginate(sortByDateAdded(learningPieces), currentLearningPage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                    <td>{formatDate(item.dateAdded)}</td>
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
                            <tr><th>Stuk</th><th>Componist</th><th>Type</th><th>Date Added</th></tr>
                            </thead>
                            <tbody>
                            {paginate(sortByDateAdded(repertoireData), currentRepertoirePage, itemsPerPage).map((item, index) => (
                                <tr key={index}>
                                    <td>{item.stuk}</td>
                                    <td>{item.componist}</td>
                                    <td>{item.type}</td>
                                    <td>{formatDate(item.dateAdded)}</td>
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