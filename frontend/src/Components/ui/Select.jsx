import React from 'react';

function Select({
  id,
  label,
  value,
  onChange,
  options,
  placeholder = '-- Select --',
  required = false,
  disabled = false
}) {
  return (
    <div className="form-group">
      {label && <label htmlFor={id}>{label}</label>}
      <select id={id} value={value} onChange={onChange} required={required} disabled={disabled}>
        <option value="">{placeholder}</option>
        {options.map((option) => {
          const optionValue = typeof option === 'string' ? option : option.value;
          const optionLabel = typeof option === 'string' ? option : option.label;

          return (
            <option key={optionValue} value={optionValue}>
              {optionLabel}
            </option>
          );
        })}
      </select>
    </div>
  );
}

export default Select;
