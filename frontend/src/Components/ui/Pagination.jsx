import React from 'react';

function Pagination({ currentPage, totalPages, onPageChange }) {
  const pageCount = totalPages || 1;

  return (
    <div className="pagination">
      <button
        type="button"
        onClick={() => onPageChange(Math.max(currentPage - 1, 1))}
        disabled={currentPage === 1}
      >
        Previous
      </button>
      <span>
        Page {currentPage} of {pageCount}
      </span>
      <button
        type="button"
        onClick={() => onPageChange(Math.min(currentPage + 1, pageCount))}
        disabled={currentPage === pageCount}
      >
        Next
      </button>
    </div>
  );
}

export default Pagination;
