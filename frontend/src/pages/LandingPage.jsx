import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function LandingPage() {
  const navigate = useNavigate();

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

        <h1 className="landing-title">WPW Product Information Manager</h1>
        <p className="landing-subtitle">Manage your complete product catalog with ease</p>

        <div className="landing-actions">
          <button
            className="btn btn-primary btn-lg"
            onClick={() => navigate('/login')}
          >
            Sign In
          </button>
        </div>

        <p className="landing-footer-note">
          Don't have access? <span>Contact your administrator</span>
        </p>
      </div>
    </div>
  );
}
