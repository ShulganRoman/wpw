const LOCALES = [
  { code: 'en', label: 'EN' },
  { code: 'he', label: 'HE' },
  { code: 'ru', label: 'RU' },
];

export default function LocaleSwitcher({ locale, onChange }) {
  return (
    <div className="locale-switcher">
      {LOCALES.map(l => (
        <button
          key={l.code}
          className={`locale-btn ${locale === l.code ? 'active' : ''}`}
          onClick={() => onChange(l.code)}
        >
          {l.label}
        </button>
      ))}
    </div>
  );
}
