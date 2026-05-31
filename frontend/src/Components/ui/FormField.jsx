import React from 'react';

function FormField({ label, htmlFor, error, children }) {
  return (
    <div className="form-group">
      {label && <label htmlFor={htmlFor}>{label}</label>}
      {children}
      {error && <div className="error-message">{error}</div>}
    </div>
  );
}

export default FormField;
