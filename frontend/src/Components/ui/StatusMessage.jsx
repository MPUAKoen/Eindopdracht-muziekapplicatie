import React from 'react';

function StatusMessage({ message, type = 'info' }) {
  if (!message) return null;

  return <div className={`status-message status-message-${type}`}>{message}</div>;
}

export default StatusMessage;
