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
        // Voeg meer gegevens toe als dat nodig is
    ];

    const itemsPerPage = 2; // Aantal items per pagina
    const [currentPage, setCurrentPage] = useState(1);

    const indexOfLastItem = currentPage * itemsPerPage;
    const indexOfFirstItem = indexOfLastItem - itemsPerPage;
    const currentItems = repertoireData.slice(indexOfFirstItem, indexOfLastItem);

    const totalPages = Math.ceil(repertoireData.length / itemsPerPage);

    const nextPage = () => {
        if (currentPage < totalPages) {
            setCurrentPage(currentPage + 1);
        }
    };

    const prevPage = () => {
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
                    <tr>
                        <td><strong>Voornaam:</strong> Koen</td>
                    </tr>
                    <tr>
                        <td><strong>Achternaam:</strong> Green</td>
                    </tr>
                    <tr>
                        <td><strong>Email:</strong> koen.green@email.com</td>
                    </tr>
                    <tr>
                        <td><strong>Telefoonnummer:</strong> 0612345678</td>
                    </tr>
                    <tr>
                        <td><strong>Instrument:</strong> Voice</td>
                    </tr>
                    </tbody>
                </table>

                <table className="table">
                    <caption>Favoriete Stukken</caption>
                    <thead>
                    <tr>
                        <th>Stuk</th>
                        <th>Componist</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>'Notte giorno faticar'</td>
                        <td>Giovanni Pergolesi</td>
                    </tr>
                    <tr>
                        <td>'Nel cor più non mi sento'</td>
                        <td>Giuseppe Sarti</td>
                    </tr>
                    </tbody>
                </table>

                <table className="table">
                    <caption>Repertoire</caption>
                    <thead>
                    <tr>
                        <th>Stuk</th>
                        <th>Componist</th>
                        <th>Type</th>
                    </tr>
                    </thead>
                    <tbody>
                    {currentItems.map((item, index) => (
                        <tr key={index}>
                            <td>{item.stuk}</td>
                            <td>{item.componist}</td>
                            <td>{item.type}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>

                <div className="pagination">
                    <button onClick={prevPage} disabled={currentPage === 1}>Previous</button>
                    <span>Page {currentPage} of {totalPages}</span>
                    <button onClick={nextPage} disabled={currentPage === totalPages}>Next</button>
                </div>
            </div>
        </div>
    );
};

export default AboutPage;
