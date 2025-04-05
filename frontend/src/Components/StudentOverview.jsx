import React, { useState } from 'react';
import '../App.css';

const StudentOverview = () => {
    return (
        <div className="app-container">

            {/* Main Content */}
            <div className="mainpage">
                <div className="header">
                    <h1>Student Overview</h1>
                </div>

                {/* Student Table */}
                <div className="table-container ">
                    <table className="table">
                        <caption>Students</caption>
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Instrument</th>
                            <th>Progress</th>
                            <th>Last Lesson</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td>Koen Green</td>
                            <td>koen.green@email.com</td>
                            <td>Voice</td>
                            <td>85%</td>
                            <td>2023-10-15</td>
                        </tr>
                        <tr>
                            <td>Jane Doe</td>
                            <td>jane.doe@email.com</td>
                            <td>Piano</td>
                            <td>70%</td>
                            <td>2023-10-10</td>
                        </tr>
                        <tr>
                            <td>John Smith</td>
                            <td>john.smith@email.com</td>
                            <td>Guitar</td>
                            <td>90%</td>
                            <td>2023-10-12</td>
                        </tr>
                        <tr>
                            <td>Emily White</td>
                            <td>emily.white@email.com</td>
                            <td>Violin</td>
                            <td>65%</td>
                            <td>2023-10-08</td>
                        </tr>
                        </tbody>
                    </table>

                    {/* Pagination */}
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

export default StudentOverview;