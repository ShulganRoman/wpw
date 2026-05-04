import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useLocale } from '../contexts/LocaleContext';

export default function LandingPage() {
  const navigate = useNavigate();
  const { t } = useLocale();

  // If already logged in - redirect
  useEffect(() => {
    if (localStorage.getItem('authToken')) {
      navigate('/catalog', { replace: true });
    }
  }, [navigate]);

  return (
    <div className="landing-page">
      <div className="landing-card">
        <div className="landing-logo">
          <img src="/wpw-logo.png" alt="WPW" className="landing-logo-img" />
        </div>

        <h1 className="landing-title">{t('landing_title')}</h1>
        <p className="landing-subtitle">{t('landing_subtitle')}</p>

        <div className="landing-actions">
          <button
            className="btn btn-primary btn-lg"
            onClick={() => navigate('/login')}
          >
            {t('landing_sign_in')}
          </button>
        </div>

        <p className="landing-footer-note">
          {t('landing_no_access')} <span>{t('landing_contact_admin')}</span>
        </p>
      </div>
    </div>
  );
}
