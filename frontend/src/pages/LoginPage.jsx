import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/api';
import { useToast } from '../components/ToastContext';
import { useLocale } from '../contexts/LocaleContext';

export default function LoginPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { t } = useLocale();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // If already logged in - redirect
  if (localStorage.getItem('authToken')) {
    navigate('/catalog', { replace: true });
    return null;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError(t('login_error_empty'));
      return;
    }

    setLoading(true);
    setError('');

    try {
      const data = await login(username.trim(), password);

      const token = data.token || data.accessToken || data.access_token;
      const role = data.role || data.userRole || '';
      const privileges = data.privileges || data.userPrivileges || [];

      if (!token) {
        throw new Error(t('login_error_no_token'));
      }

      localStorage.setItem('authToken', token);
      localStorage.setItem('userRole', role);
      localStorage.setItem('userPrivileges', JSON.stringify(privileges));

      toast(t('login_welcome'), 'success');
      navigate('/catalog', { replace: true });
    } catch (err) {
      const msg = err.message || t('login_error_default');
      setError(msg);
      toast(msg, 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <img src="/wpw-logo.png" alt="WPW" className="login-logo-img" />
        </div>

        <h1 className="login-title">{t('login_title')}</h1>
        <p className="login-subtitle">{t('login_subtitle')}</p>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          {error && (
            <div className="login-error" role="alert">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.5" />
                <path d="M8 4.5v4M8 11h.01" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
              {error}
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="login-username">{t('login_username')}</label>
            <input
              id="login-username"
              className="form-control"
              type="text"
              autoComplete="username"
              autoFocus
              placeholder={t('login_username_placeholder')}
              value={username}
              onChange={e => { setUsername(e.target.value); setError(''); }}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="login-password">{t('login_password')}</label>
            <input
              id="login-password"
              className="form-control"
              type="password"
              autoComplete="current-password"
              placeholder={t('login_password_placeholder')}
              value={password}
              onChange={e => { setPassword(e.target.value); setError(''); }}
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary login-submit-btn"
            disabled={loading}
          >
            {loading ? (
              <>
                <span className="login-spinner" aria-hidden="true" />
                {t('login_submitting')}
              </>
            ) : (
              t('login_submit')
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
