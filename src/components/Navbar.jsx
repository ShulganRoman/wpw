import { NavLink, useNavigate } from 'react-router-dom';
import LocaleSwitcher from './LocaleSwitcher';

const LINKS = [
  { to: '/catalog', label: 'Catalog' },
  { to: '/search', label: 'Search' },
  { to: '/operations', label: 'Operations' },
  { to: '/export', label: 'Export' },
  { to: '/admin', label: 'Admin' },
  { to: '/dealer', label: 'Dealer' },
];

function isLoggedIn() {
  return !!localStorage.getItem('authToken');
}

export default function Navbar({ locale, onLocaleChange }) {
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await fetch('/api/v1/auth/logout', { method: 'POST' });
    } catch {
      // ignore network errors on logout
    }
    localStorage.removeItem('authToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userPrivileges');
    navigate('/login');
  }

  const loggedIn = isLoggedIn();

  return (
    <nav className="navbar">
      <NavLink to="/catalog" className="navbar-brand">
        <img src="/wpw-logo.png" alt="WPW" />
      </NavLink>
      <div className="navbar-links">
        {LINKS.map(link => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
          >
            {link.label}
          </NavLink>
        ))}
      </div>
      <div className="navbar-right">
        <LocaleSwitcher locale={locale} onChange={onLocaleChange} />
        {loggedIn ? (
          <button className="navbar-auth-btn" onClick={handleLogout}>
            Logout
          </button>
        ) : (
          <NavLink to="/login" className="navbar-auth-btn">
            Login
          </NavLink>
        )}
      </div>
    </nav>
  );
}
