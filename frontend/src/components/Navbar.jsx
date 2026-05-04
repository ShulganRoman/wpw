import { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import LocaleSwitcher from './LocaleSwitcher';
import { getCart } from '../api/api';
import { useLocale } from '../contexts/LocaleContext';

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

export default function Navbar() {
  const navigate = useNavigate();
  const { locale, setLocale, t } = useLocale();
  const [menuOpen, setMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);

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

  useEffect(() => {
    if (localStorage.getItem('userRole') !== 'dealer' || !loggedIn) return;
    getCart().then(c => setCartCount(c?.totalItems || c?.items?.length || 0)).catch(() => {});
  }, [loggedIn]);

  const canExport = privileges.has('BULK_EXPORT');
  const canAdmin = [...ADMIN_PRIVILEGES].some(p => privileges.has(p));
  const isDealer = localStorage.getItem('userRole') === 'dealer';

  const links = [
    { to: '/catalog', label: t('nav_catalog'), show: true },
    { to: '/export', label: t('nav_export'), show: canExport },
    { to: '/dealer', label: t('nav_my_orders'), show: isDealer },
    { to: '/import', label: t('nav_import'), show: isDealer },
    { to: '/admin', label: t('nav_admin'), show: canAdmin },
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
              style={{ position: 'relative' }}
            >
              {link.label}
              {link.to === '/dealer' && cartCount > 0 && (
                <span style={{
                  position: 'absolute', top: -6, right: -10,
                  background: 'var(--wpw-accent)', color: '#fff',
                  borderRadius: 10, fontSize: 10, padding: '1px 5px', fontWeight: 700,
                }}>{cartCount}</span>
              )}
            </NavLink>
          ))}
        </div>

        <div className="navbar-right">
          <LocaleSwitcher locale={locale} onChange={setLocale} />
          {loggedIn ? (
            <button className="navbar-auth-btn" onClick={handleLogout}>
              {t('nav_logout')}
            </button>
          ) : (
            <NavLink to="/login" className="navbar-auth-btn">
              {t('nav_login')}
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
            <LocaleSwitcher locale={locale} onChange={v => { setLocale(v); setMenuOpen(false); }} />
            {loggedIn ? (
              <button className="navbar-auth-btn" onClick={handleLogout}>{t('nav_logout')}</button>
            ) : (
              <NavLink to="/login" className="navbar-auth-btn" onClick={() => setMenuOpen(false)}>{t('nav_login')}</NavLink>
            )}
          </div>
        </div>
      )}
    </>
  );
}
