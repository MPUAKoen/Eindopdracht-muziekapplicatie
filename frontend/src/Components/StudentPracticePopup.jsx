import React, { useEffect, useState } from 'react';
import '../App.css';

const API_BASE = 'http://localhost:8080';

// base daily goals in minutes
const PRACTICE_GOAL = 60;
const GUIDED_GOAL = 60;
const LISTEN_GOAL = 30;

const RANGE_OPTIONS = [
  { key: 'today', label: 'Today' },
  { key: 'last7Days', label: 'Last 7 days' },
  { key: 'last30Days', label: 'Last 30 days' },
  { key: 'last365Days', label: 'Last 365 days' },
  { key: 'allTime', label: 'Total' }
];

// helper: convert minutes to "Xh YYm"
const formatMinutes = (mins) => {
  if (!mins || mins <= 0) {
    return '0h 00m';
  }
  const hours = Math.floor(mins / 60);
  const minutes = mins % 60;
  const mm = minutes.toString().padStart(2, '0');
  return `${hours}h ${mm}m`;
};

function StudentPracticePopup({ visible, onClose }) {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [range, setRange] = useState('today');
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const loadSummary = () => {
    setLoading(true);
    setError('');

    fetch(`${API_BASE}/api/practice/summary`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
      .then((data) => {
        setSummary(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Practice summary error', err);
        setError('Could not load practice data.');
        setLoading(false);
      });
  };

  useEffect(() => {
    if (!visible) return;
    loadSummary();
  }, [visible]);

  if (!visible) return null;

  const getRangeLabel = () =>
    RANGE_OPTIONS.find((opt) => opt.key === range)?.label || 'Today';

  const getValueForRange = (data) => {
    switch (range) {
      case 'today':
        return data.today;
      case 'last7Days':
        return data.last7Days;
      case 'last30Days':
        return data.last30Days;
      case 'last365Days':
        return data.last365Days;
      case 'allTime':
      default:
        return data.allTime;
    }
  };

  const getGoalForRange = (baseGoal) => {
    switch (range) {
      case 'today':
        return baseGoal;
      case 'last7Days':
        return baseGoal * 7;
      case 'last30Days':
        return baseGoal * 30;
      case 'last365Days':
        return baseGoal * 365;
      case 'allTime':
      default:
        return baseGoal * 365;
    }
  };

  const makePercent = (value, goal) => {
    if (!goal || goal <= 0) return 0;
    const pct = Math.round((value / goal) * 100);
    return pct > 100 ? 100 : pct;
  };

  const handleQuickAdd = async (type) => {
    const input = window.prompt('Minutes to add', '15');
    if (input == null) return;

    const minutes = parseInt(input, 10);
    if (Number.isNaN(minutes) || minutes <= 0) {
      alert('Please enter a positive number of minutes.');
      return;
    }

    setSaving(true);
    setError('');

    try {
      const res = await fetch(`${API_BASE}/api/practice/add`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          type,
          minutes
        })
      });

      if (!res.ok) {
        throw new Error(`Status ${res.status}`);
      }

      loadSummary();
    } catch (e) {
      console.error('Error adding practice entry', e);
      setError('Could not save practice entry.');
    } finally {
      setSaving(false);
    }
  };

  const renderRow = (label, data, baseGoal, type) => {
    if (!data) return null;

    const value = getValueForRange(data);          // minutes
    const goal = getGoalForRange(baseGoal);        // minutes
    const pct = makePercent(value, goal);
    const rangeLabel = getRangeLabel();

    return (
      <div className="practice-row">
        <div className="practice-row-header">
          <div className="practice-row-label">{label}</div>
          <button
            type="button"
            className="practice-plus-btn"
            disabled={saving}
            onClick={() => handleQuickAdd(type)}
          >
            +
          </button>
        </div>

        <div className="practice-progressbar">
          <div
            className="practice-progressbar-fill"
            style={{ width: `${pct}%` }}
          />
        </div>

        <div className="practice-row-meta">
          <span>
            {rangeLabel}: {formatMinutes(value)} of {formatMinutes(goal)}
          </span>
        </div>
      </div>
    );
  };

  return (
    <div className="practice-popup">
      <div className="practice-popup-header">
        <h3>Practice tracker</h3>
        <button
          type="button"
          className="practice-popup-close"
          onClick={onClose}
          aria-label="Close practice tracker"
        >
          ✕
        </button>
      </div>

      {/* View selector with dropdown */}
      <div className="practice-range-dropdown">
        <span className="practice-range-label">View</span>
        <button
          type="button"
          className="practice-range-current"
          onClick={() => setDropdownOpen((prev) => !prev)}
        >
          {getRangeLabel()}
          <span className="practice-range-arrow">
            {dropdownOpen ? '▲' : '▼'}
          </span>
        </button>

        {dropdownOpen && (
          <div className="practice-range-menu">
            {RANGE_OPTIONS.map((opt) => (
              <button
                key={opt.key}
                type="button"
                className={`practice-range-option ${
                  range === opt.key ? 'active' : ''
                }`}
                onClick={() => {
                  setRange(opt.key);
                  setDropdownOpen(false);
                }}
              >
                {opt.label}
              </button>
            ))}
          </div>
        )}
      </div>

      {loading && <div className="practice-popup-body">Loading…</div>}

      {error && !loading && (
        <div className="practice-popup-body">
          <p>{error}</p>
        </div>
      )}

      {summary && !loading && !error && (
        <div className="practice-popup-body">
          {renderRow('Practice hours', summary.practiceHours, PRACTICE_GOAL, 'PRACTICE')}
          {renderRow(
            'Guided lesson hours',
            summary.guidedLessonHours,
            GUIDED_GOAL,
            'GUIDED'
          )}
          {renderRow(
            'Listened hours',
            summary.listenedHours,
            LISTEN_GOAL,
            'LISTENED'
          )}
        </div>
      )}
    </div>
  );
}

export default StudentPracticePopup;
