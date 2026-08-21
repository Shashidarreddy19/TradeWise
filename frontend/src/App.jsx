import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import ProblemStatement from './components/ProblemStatement';
import HowItWorks from './components/HowItWorks';
import CoreFeatures from './components/CoreFeatures';
import Footer from './components/Footer';
import { isAuthenticated, getUser, clearAuth } from './services/api';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import Exporter from './pages/Exporter';
import Logistics from './pages/Logistics';
import MlExportRanking from './components/MlExportRanking';
import MlModelTest from './components/MlModelTest';

function App() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  // Auth state — drives conditional rendering across the entire app.
  // Re-evaluated on every navigation and on mount.
  const [authState, setAuthState] = useState(() => ({
    authenticated: isAuthenticated(),
    user: getUser(),
  }));

  // Refresh auth state from storage (covers login/logout in other tabs)
  const refreshAuth = () => {
    setAuthState({
      authenticated: isAuthenticated(),
      user: getUser(),
    });
  };

  // Sync state with back/forward history actions
  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPath(window.location.pathname);
      refreshAuth();
    };
    window.addEventListener('popstate', handleLocationChange);
    // Listen for storage changes (other tabs logging in/out)
    window.addEventListener('storage', refreshAuth);
    return () => {
      window.removeEventListener('popstate', handleLocationChange);
      window.removeEventListener('storage', refreshAuth);
    };
  }, []);

  // Programmatic navigation utility
  const navigate = (path) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
    refreshAuth();
    window.scrollTo(0, 0);
  };

  // Helper: get the correct dashboard path for the current user's role
  const getDashboardPath = () => {
    const user = authState.user;
    if (!user) return '/login';
    return user.role === 'LOGISTICS' ? '/logistics' : '/exporter';
  };

  // Logout handler — clears everything and returns to landing
  const handleLogout = () => {
    clearAuth();
    // Also clear sessionStorage in case "Remember Me" was off
    sessionStorage.removeItem('trade_token');
    sessionStorage.removeItem('trade_user');
    refreshAuth();
    navigate('/');
  };

  // ══════════════════════════════════════════════════════════════════════════
  // ROUTE GUARDS
  // ══════════════════════════════════════════════════════════════════════════

  const { authenticated, user } = authState;

  // Guard: /login and /register should redirect to dashboard if already logged in
  if ((currentPath === '/login' || currentPath === '/register') && authenticated) {
    // Replace the history entry so back button doesn't return here
    const dash = getDashboardPath();
    window.history.replaceState({}, '', dash);
    setCurrentPath(dash);
    return null;
  }

  // Guard: protected routes require authentication
  if ((currentPath === '/exporter' || currentPath === '/logistics') && !authenticated) {
    window.history.replaceState({}, '', '/login');
    setCurrentPath('/login');
    return null;
  }

  // Guard: role mismatch — exporter can't access /logistics and vice versa
  if (authenticated && user) {
    if (currentPath === '/exporter' && user.role === 'LOGISTICS') {
      window.history.replaceState({}, '', '/logistics');
      setCurrentPath('/logistics');
      return null;
    }
    if (currentPath === '/logistics' && user.role === 'EXPORTER') {
      window.history.replaceState({}, '', '/exporter');
      setCurrentPath('/exporter');
      return null;
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  // ROUTE RENDERING
  // ══════════════════════════════════════════════════════════════════════════

  if (currentPath === '/login') {
    return <Login onNavigate={navigate} />;
  }

  if (currentPath === '/register') {
    return <Register onNavigate={navigate} />;
  }

  if (currentPath === '/exporter') {
    return <Exporter onNavigate={navigate} onLogout={handleLogout} />;
  }

  if (currentPath === '/logistics') {
    return <Logistics onNavigate={navigate} onLogout={handleLogout} />;
  }

  if (currentPath === '/ml-ranking') {
    if (!authenticated) { navigate('/login'); return null; }
    return (
      <div className="min-h-screen bg-slate-50 py-10 px-4">
        <div className="max-w-4xl mx-auto">
          <button onClick={() => navigate('/exporter')} className="text-xs text-indigo-600 hover:underline mb-4 inline-block">&larr; Back to Dashboard</button>
          <MlExportRanking />
        </div>
      </div>
    );
  }

  if (currentPath === '/ml-test') {
    if (!authenticated) { navigate('/login'); return null; }
    return (
      <div className="min-h-screen bg-slate-50 py-10 px-4">
        <div className="max-w-5xl mx-auto">
          <button onClick={() => navigate('/exporter')} className="text-xs text-indigo-600 hover:underline mb-4 inline-block">&larr; Back to Dashboard</button>
          <MlModelTest />
        </div>
      </div>
    );
  }

  // Default: Landing page "/"
  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-700 flex flex-col antialiased font-sans relative overflow-x-hidden">
      {/* Decorative Global Background Glows */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-7xl h-[600px] bg-gradient-to-b from-indigo-500/5 via-sky-500/2 to-transparent rounded-full glow-blur pointer-events-none z-0"></div>

      {/* Navigation Header — auth-aware */}
      <Navbar
        onNavigate={navigate}
        currentPath={currentPath}
        authenticated={authenticated}
        user={user}
        onLogout={handleLogout}
        getDashboardPath={getDashboardPath}
      />

      {/* Main Sections */}
      <main className="flex-grow z-10">
        <Hero onNavigate={navigate} authenticated={authenticated} getDashboardPath={getDashboardPath} />
        <ProblemStatement />
        <HowItWorks />
        <CoreFeatures />
      </main>

      {/* Site Footer */}
      <Footer onNavigate={navigate} />
    </div>
  );
}

export default App;
