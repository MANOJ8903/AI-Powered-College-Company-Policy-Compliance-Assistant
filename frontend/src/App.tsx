import React, { useState, useEffect } from 'react';
import { Login } from './pages/Login';
import { Chat } from './pages/Chat';
import { AdminDashboard } from './pages/AdminDashboard';
import { MessageSquare, LayoutDashboard, LogOut, Shield } from 'lucide-react';
import { type User, setInMemoryToken } from './services/api';

const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [activeTab, setActiveTab] = useState<'chat' | 'admin'>('chat');

  useEffect(() => {
    // Pure in-memory authorization on mount (requiring login on tab fresh load)
    setUser(null);
    setInMemoryToken(null);
  }, []);

  const handleLoginSuccess = (loggedInUser: User) => {
    setUser(loggedInUser);
    if (loggedInUser.role === 'ADMIN') {
      setActiveTab('admin');
    } else {
      setActiveTab('chat');
    }
  };

  const handleLogout = () => {
    setInMemoryToken(null);
    setUser(null);
  };

  if (!user) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  const isAdmin = user.role === 'ADMIN';

  return (
    <div className="dashboard-container">
      {/* Sidebar navigation panel */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <div style={{
            background: 'var(--primary)',
            padding: '6px',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Shield size={18} style={{ color: 'var(--text-primary)' }} />
          </div>
          <span className="sidebar-logo-text">Policy Guardian</span>
        </div>

        <nav className="sidebar-menu">
          {isAdmin && (
            <button
              onClick={() => setActiveTab('admin')}
              className={`menu-item btn-link ${activeTab === 'admin' ? 'active' : ''}`}
              style={{ background: 'transparent', border: 'none', textAlign: 'left', cursor: 'pointer', width: '100%' }}
            >
              <LayoutDashboard size={16} />
              Admin Dashboard
            </button>
          )}
          
          <button
            onClick={() => setActiveTab('chat')}
            className={`menu-item btn-link ${activeTab === 'chat' ? 'active' : ''}`}
            style={{ background: 'transparent', border: 'none', textAlign: 'left', cursor: 'pointer', width: '100%' }}
          >
            <MessageSquare size={16} />
            Compliance Chat
          </button>
        </nav>

        <div className="sidebar-footer">
          <div className="user-profile-info">
            <span className="profile-name" title={user.username}>{user.username}</span>
            <span className="profile-role">{user.role} | {user.department}</span>
          </div>
          <button className="btn-logout" onClick={handleLogout} title="Sign Out">
            <LogOut size={16} />
          </button>
        </div>
      </aside>

      {/* Main dashboard content panel */}
      <main className="main-content">
        <header className="top-bar">
          <div className="page-title">
            {activeTab === 'admin' ? 'Organization Administration Console' : 'AI Grounded Compliance Query Portal'}
          </div>
          <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Authenticated under: <span style={{ fontWeight: 600, color: 'var(--accent)' }}>{user.department}</span>
          </div>
        </header>

        {activeTab === 'admin' && isAdmin ? <AdminDashboard /> : <Chat />}
      </main>
    </div>
  );
};

export default App;
