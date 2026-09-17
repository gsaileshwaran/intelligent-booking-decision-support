import { useState, useEffect } from 'react';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { Header } from './components/layout/Header';
import { Footer } from './components/layout/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';

// Customer Views (UI-01 to UI-10)
import { HomeView } from './views/customer/HomeView';
import { MovieListingView } from './views/customer/MovieListingView';
import { MovieDetailsView } from './views/customer/MovieDetailsView';
import { SearchResultsView } from './views/customer/SearchResultsView';
import { CityTheatresView } from './views/customer/CityTheatresView';
import { TheatreDetailsView } from './views/customer/TheatreDetailsView';
import { ShowDetailsView } from './views/customer/ShowDetailsView';
import { SeatAvailabilityView } from './views/customer/SeatAvailabilityView';
import { BookingConfirmationView } from './views/customer/BookingConfirmationView';
import { AuthView } from './views/customer/AuthView';
import { ProfileView } from './views/customer/ProfileView';

// Theatre Manager Views (UI-11 to UI-14)
import { ManagerDashboardView } from './views/manager/ManagerDashboardView';
import { ScreenManagementView } from './views/manager/ScreenManagementView';
import { SeatManagementView } from './views/manager/SeatManagementView';
import { ShowManagementView } from './views/manager/ShowManagementView';
import { ManagerShowInspectionView } from './views/manager/ManagerShowInspectionView';

// Super Admin Views (UI-15 to UI-21)
import { AdminDashboardView } from './views/admin/AdminDashboardView';
import { UserManagementView } from './views/admin/UserManagementView';
import { RolePermissionsView } from './views/admin/RolePermissionsView';
import { CityTheatreManagementView } from './views/admin/CityTheatreManagementView';
import { MovieManagementView } from './views/admin/MovieManagementView';
import { AuditLogsView } from './views/admin/AuditLogsView';
import { SearchAdminView } from './views/admin/SearchAdminView';

function hashToRoute(hash: string): { view: string; param: any } {
  const clean = hash.replace(/^#\/?/, '').trim();
  if (!clean || clean === 'home') {
    return { view: 'home', param: null };
  }
  if (clean === 'movies') {
    return { view: 'movies', param: null };
  }
  if (clean.startsWith('movie/')) {
    const id = clean.split('/')[1];
    return { view: 'movie-details', param: Number(id) || 1 };
  }
  if (clean.startsWith('search')) {
    const qIndex = clean.indexOf('?q=');
    const q = qIndex !== -1 ? decodeURIComponent(clean.substring(qIndex + 3)) : '';
    return { view: 'search', param: q };
  }
  if (clean === 'theatres') {
    return { view: 'theatres', param: null };
  }
  if (clean.startsWith('theatre/')) {
    const id = clean.split('/')[1];
    return { view: 'theatre-details', param: Number(id) || 1 };
  }
  if (clean.startsWith('show/')) {
    const id = clean.split('/')[1];
    return { view: 'show-details', param: Number(id) || 1 };
  }
  if (clean.startsWith('seats/') || clean.startsWith('show-seats/')) {
    const id = clean.split('/')[1];
    return { view: 'show-seats', param: Number(id) || 1 };
  }
  if (clean === 'login') {
    return { view: 'login', param: null };
  }
  if (clean === 'profile') {
    return { view: 'profile', param: null };
  }
  if (clean.startsWith('booking-confirmation/') || clean.startsWith('ticket/')) {
    const ref = clean.split('/')[1];
    return { view: 'booking-confirmation', param: ref };
  }

  // Manager routes
  if (clean === 'manager/dashboard' || clean === 'manager-dashboard') {
    return { view: 'manager-dashboard', param: null };
  }
  if (clean.startsWith('manager/screens/') || clean.startsWith('manager-screens/')) {
    const id = clean.split('/')[2] || clean.split('/')[1];
    return { view: 'manager-screens', param: Number(id) || 1 };
  }
  if (clean.startsWith('manager/seats/') || clean.startsWith('manager-seats/')) {
    const id = clean.split('/')[2] || clean.split('/')[1];
    return { view: 'manager-seats', param: Number(id) || 1 };
  }
  if (clean.startsWith('manager/shows/') || clean.startsWith('manager-shows/')) {
    const id = clean.split('/')[2] || clean.split('/')[1];
    return { view: 'manager-shows', param: Number(id) || 1 };
  }
  if (clean.startsWith('manager/show-inspect/') || clean.startsWith('manager-show-seats/')) {
    const id = clean.split('/')[2] || clean.split('/')[1];
    return { view: 'manager-show-seats', param: Number(id) || 1 };
  }

  // Admin routes
  if (clean === 'admin/dashboard' || clean === 'admin-dashboard') {
    return { view: 'admin-dashboard', param: null };
  }
  if (clean === 'admin/users' || clean === 'admin-users') {
    return { view: 'admin-users', param: null };
  }
  if (clean === 'admin/roles' || clean === 'admin-roles') {
    return { view: 'admin-roles', param: null };
  }
  if (clean === 'admin/cities-theatres' || clean === 'admin-cities-theatres') {
    return { view: 'admin-cities-theatres', param: null };
  }
  if (clean === 'admin/movies' || clean === 'admin-movies') {
    return { view: 'admin-movies', param: null };
  }
  if (clean === 'admin/audit' || clean === 'admin-audit') {
    return { view: 'admin-audit', param: null };
  }
  if (clean === 'admin/search' || clean === 'admin-search') {
    return { view: 'admin-search', param: null };
  }

  return { view: 'home', param: null };
}

function routeToHash(view: string, param?: any): string {
  switch (view) {
    case 'home': return '#/home';
    case 'movies': return '#/movies';
    case 'movie-details': return `#/movie/${param || 1}`;
    case 'search': return param ? `#/search?q=${encodeURIComponent(String(param))}` : '#/search';
    case 'theatres': return '#/theatres';
    case 'theatre-details': return `#/theatre/${param || 1}`;
    case 'show-details': return `#/show/${param || 1}`;
    case 'show-seats': return `#/seats/${param || 1}`;
    case 'login': return '#/login';
    case 'profile': return '#/profile';
    case 'booking-confirmation': return `#/booking-confirmation/${param || ''}`;

    case 'manager-dashboard': return '#/manager/dashboard';
    case 'manager-screens': return `#/manager/screens/${param || 1}`;
    case 'manager-seats': return `#/manager/seats/${param || 1}`;
    case 'manager-shows': return `#/manager/shows/${param || 1}`;
    case 'manager-show-seats': return `#/manager/show-inspect/${param || 1}`;

    case 'admin-dashboard': return '#/admin/dashboard';
    case 'admin-users': return '#/admin/users';
    case 'admin-roles': return '#/admin/roles';
    case 'admin-cities-theatres': return '#/admin/cities-theatres';
    case 'admin-movies': return '#/admin/movies';
    case 'admin-audit': return '#/admin/audit';
    case 'admin-search': return '#/admin/search';

    default: return '#/home';
  }
}

export function AppContent() {
  const initial = hashToRoute(window.location.hash);
  const [currentView, setCurrentView] = useState<string>(initial.view);
  const [viewParam, setViewParam] = useState<any>(initial.param);

  const navigate = (view: string, param?: any) => {
    setCurrentView(view);
    setViewParam(param ?? null);
    const targetHash = routeToHash(view, param);
    if (window.location.hash !== targetHash) {
      window.location.hash = targetHash;
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  useEffect(() => {
    // Sync view when browser URL hash changes (back/forward or user entry)
    const handleHashChange = () => {
      const { view, param } = hashToRoute(window.location.hash);
      setCurrentView(view);
      setViewParam(param);
    };

    if (!window.location.hash) {
      window.history.replaceState(null, '', '#/home');
    }

    window.addEventListener('hashchange', handleHashChange);

    // Architectural Test Synchronization Hook
    (window as any).__pvk_navigate = (view: string, param?: any) => {
      navigate(view, param);
    };

    return () => {
      window.removeEventListener('hashchange', handleHashChange);
    };
  }, []);

  const renderView = () => {
    switch (currentView) {
      // Customer Portal (UI-01 to UI-10)
      case 'home':
        return <HomeView onNavigate={navigate} />;
      case 'movies':
        return <MovieListingView onNavigate={navigate} />;
      case 'movie-details':
        return <MovieDetailsView movieId={Number(viewParam) || 1} onNavigate={navigate} />;
      case 'search':
        return <SearchResultsView initialQuery={String(viewParam || '')} onNavigate={navigate} />;
      case 'theatres':
        return <CityTheatresView onNavigate={navigate} />;
      case 'theatre-details':
        return <TheatreDetailsView theatreId={Number(viewParam) || 1} onNavigate={navigate} />;
      case 'show-details':
        return <ShowDetailsView showId={Number(viewParam) || 1} onNavigate={navigate} />;
      case 'show-seats': {
        const showId = typeof viewParam === 'object' && viewParam !== null ? viewParam.showId : (Number(viewParam) || 1);
        const initialPartySize = typeof viewParam === 'object' && viewParam !== null ? viewParam.partySize : undefined;
        const initialRecommendedSeatIds = typeof viewParam === 'object' && viewParam !== null ? viewParam.recommendedSeatIds : undefined;
        const initialRecommendedSeatLabels = typeof viewParam === 'object' && viewParam !== null ? viewParam.recommendedSeatLabels : undefined;
        return (
          <SeatAvailabilityView
            showId={showId}
            initialPartySize={initialPartySize}
            initialRecommendedSeatIds={initialRecommendedSeatIds}
            initialRecommendedSeatLabels={initialRecommendedSeatLabels}
            onNavigate={navigate}
          />
        );
      }
      case 'login':
        return <AuthView onNavigate={navigate} />;
      case 'profile':
        return (
          <ProtectedRoute onNavigate={navigate}>
            <ProfileView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'booking-confirmation':
        return (
          <ProtectedRoute onNavigate={navigate}>
            <BookingConfirmationView bookingReference={String(viewParam || '')} onNavigate={navigate} />
          </ProtectedRoute>
        );

      // Theatre Manager Portal (UI-11 to UI-14)
      case 'manager-dashboard':
        return (
          <ProtectedRoute requiredRole="MANAGER" onNavigate={navigate}>
            <ManagerDashboardView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'manager-screens':
        return (
          <ProtectedRoute requiredRole="MANAGER" onNavigate={navigate}>
            <ScreenManagementView theatreId={Number(viewParam) || 1} onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'manager-seats':
        return (
          <ProtectedRoute requiredRole="MANAGER" onNavigate={navigate}>
            <SeatManagementView theatreId={Number(viewParam) || 1} onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'manager-shows':
        return (
          <ProtectedRoute requiredRole="MANAGER" onNavigate={navigate}>
            <ShowManagementView theatreId={Number(viewParam) || 1} onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'manager-show-seats':
        return (
          <ProtectedRoute requiredRole="MANAGER" onNavigate={navigate}>
            <ManagerShowInspectionView showId={Number(viewParam) || 1} onNavigate={navigate} />
          </ProtectedRoute>
        );

      // Super Admin Portal (UI-15 to UI-21)
      case 'admin-dashboard':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <AdminDashboardView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'admin-users':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <UserManagementView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'admin-roles':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <RolePermissionsView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'admin-cities-theatres':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <CityTheatreManagementView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'admin-movies':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <MovieManagementView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'admin-audit':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <AuditLogsView onNavigate={navigate} />
          </ProtectedRoute>
        );
      case 'admin-search':
        return (
          <ProtectedRoute requiredRole="ADMIN" onNavigate={navigate}>
            <SearchAdminView onNavigate={navigate} />
          </ProtectedRoute>
        );

      default:
        return <HomeView onNavigate={navigate} />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <Header onNavigate={navigate} currentView={currentView} />
      <main id="main-content" className="main-content" tabIndex={-1}>
        <div className="app-container">
          {renderView()}
        </div>
      </main>
      <Footer />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <AppContent />
      </ToastProvider>
    </AuthProvider>
  );
}
