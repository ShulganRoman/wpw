import { useState, useEffect } from 'react';
import { createBrowserRouter, RouterProvider, Navigate, Outlet } from 'react-router-dom';
import Navbar from './components/Navbar';
import { ToastProvider } from './components/Toast';
import CatalogPage from './pages/CatalogPage';
import ProductPage from './pages/ProductPage';
import ExportPage from './pages/ExportPage';
import AdminPage from './pages/AdminPage';
import LoginPage from './pages/LoginPage';
import LandingPage from './pages/LandingPage';

function Layout({ locale, onLocaleChange }) {
  useEffect(() => {
    const isRtl = locale === 'he';
    document.documentElement.setAttribute('dir', isRtl ? 'rtl' : 'ltr');
    document.documentElement.setAttribute('lang', locale);
  }, [locale]);

  return (
    <div className="app-layout">
      <Navbar locale={locale} onLocaleChange={onLocaleChange} />
      <main className="page-content">
        <Outlet />
      </main>
    </div>
  );
}

// Standalone layout without Navbar — used for the login and landing pages
function BareLayout() {
  return <Outlet />;
}

// Protected routes — require authentication
function PrivateRoute() {
  const token = localStorage.getItem('authToken');
  return token ? <Outlet /> : <Navigate to="/login" replace />;
}

function AppInner() {
  const [locale, setLocale] = useState(() => localStorage.getItem('pim_locale') || 'en');

  function handleLocaleChange(l) {
    setLocale(l);
    localStorage.setItem('pim_locale', l);
  }

  const router = createBrowserRouter([
    // Public routes (without Navbar)
    {
      path: '/',
      element: <BareLayout />,
      children: [
        { index: true, element: <LandingPage /> },
        { path: 'login', element: <LoginPage /> },
      ],
    },
    // Public routes (with Navbar, no auth required)
    {
      element: <Layout locale={locale} onLocaleChange={handleLocaleChange} />,
      children: [
        { path: 'catalog', element: <CatalogPage locale={locale} /> },
        { path: 'product/:toolNo', element: <ProductPage locale={locale} /> },
        { path: 'search', element: <Navigate to="/catalog" replace /> },
        { path: 'operations', element: <Navigate to="/catalog" replace /> },
        { path: 'dealer', element: <Navigate to="/catalog" replace /> },
        { path: '*', element: <Navigate to="/catalog" replace /> },
      ],
    },
    // Protected routes (with Navbar and auth check)
    {
      element: <PrivateRoute />,
      children: [
        {
          element: <Layout locale={locale} onLocaleChange={handleLocaleChange} />,
          children: [
            { path: 'export', element: <ExportPage locale={locale} /> },
            { path: 'admin', element: <AdminPage /> },
          ],
        },
      ],
    },
  ]);

  return <RouterProvider router={router} />;
}

export default function App() {
  return (
    <ToastProvider>
      <AppInner />
    </ToastProvider>
  );
}
