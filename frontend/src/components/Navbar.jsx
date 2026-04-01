import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import LocaleSwitcher from './LocaleSwitcher';

const ADMIN_PRIVILEGES = new Set([
  'BULK_IMPORT',
  'MANAGE_CATALOG',
  'CREATE_ROLES',
  'MODIFY_ROLES',
  'DELETE_ROLES',
]);

function getPrivileges() {
  try {
    return new Set(JSON.parse(localStorage.getItem('userPrivileges') || '[]'));
  } catch {
    return new Set();
  }
}

function isLoggedIn() {
  return !!localStorage.getItem('authToken');
}

export default function Navbar({ locale, onLocaleChange }) {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  async function handleLogout() {
    try {
      await fetch('/api/v1/auth/logout', { method: 'POST' });
    } catch {
      // ignore network errors on logout
    }
    localStorage.removeItem('authToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userPrivileges');
    setMenuOpen(false);
    navigate('/login');
  }

  const loggedIn = isLoggedIn();
  const privileges = loggedIn ? getPrivileges() : new Set();

  const canExport = privileges.has('BULK_EXPORT');
  const canAdmin = [...ADMIN_PRIVILEGES].some(p => privileges.has(p));

  const links = [
    { to: '/catalog', label: 'Catalog', show: true },
    { to: '/export', label: 'Export', show: canExport },
    { to: '/admin', label: 'Admin', show: canAdmin },
  ].filter(l => l.show);

  return (
    <>
      <nav className="navbar">
        <NavLink to="/catalog" className="navbar-brand" onClick={() => setMenuOpen(false)}>
          <img src="/wpw-logo.png" alt="WPW" />
        </NavLink>
        <span className="navbar-version">{__APP_VERSION__}</span>

        {/* Desktop links */}
        <div className="navbar-links">
          {links.map(link => (
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
          {/* Hamburger — mobile only */}
          <button
            className="navbar-hamburger"
            onClick={() => setMenuOpen(v => !v)}
            aria-label="Toggle menu"
            aria-expanded={menuOpen}
          >
            <span className={`hamburger-icon${menuOpen ? ' open' : ''}`} />
          </button>
        </div>
      </nav>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="mobile-menu">
          {links.map(link => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => `mobile-menu-link${isActive ? ' active' : ''}`}
              onClick={() => setMenuOpen(false)}
            >
              {link.label}
            </NavLink>
          ))}
          <div className="mobile-menu-footer">
            <LocaleSwitcher locale={locale} onChange={v => { onLocaleChange(v); setMenuOpen(false); }} />
            {loggedIn ? (
              <button className="navbar-auth-btn" onClick={handleLogout}>Logout</button>
            ) : (
              <NavLink to="/login" className="navbar-auth-btn" onClick={() => setMenuOpen(false)}>Login</NavLink>
            )}
          </div>
        </div>
      )}
    </>
  );
}
