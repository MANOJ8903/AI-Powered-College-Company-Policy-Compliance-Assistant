import React, { useState, useEffect } from 'react';
import { api, type PolicyDocument } from '../services/api';
import { Upload, FileText, ShieldAlert, FileSpreadsheet, RefreshCw, Trash2, Users } from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [departmentScope, setDepartmentScope] = useState('ALL');
  const [roleScope, setRoleScope] = useState('ALL');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [documents, setDocuments] = useState<PolicyDocument[]>([]);
  const [escalations, setEscalations] = useState<any[]>([]);
  const [unansweredQueries, setUnansweredQueries] = useState<[string, number][]>([]);
  const [usageStats, setUsageStats] = useState<any[]>([]);
  const [usersList, setUsersList] = useState<any[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  const fetchDashboardData = async () => {
    setRefreshing(true);
    try {
      const docs = await api.getAdminDocuments();
      setDocuments(docs);
      
      const esc = await api.getEscalations();
      setEscalations(esc);

      const unanswered = await api.getUnansweredQueries();
      setUnansweredQueries(unanswered);

      const usage = await api.getUsageStats();
      setUsageStats(usage);

      const uList = await api.getAdminUsers();
      setUsersList(uList);
    } catch (err: any) {
      console.error('Failed to load dashboard data', err);
    } finally {
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      const extension = selected.name.split('.').pop()?.toLowerCase();
      if (extension !== 'pdf' && extension !== 'docx') {
        setError('Unsupported file format. Please upload PDF or DOCX only.');
        setFile(null);
      } else {
        setError('');
        setFile(selected);
      }
    }
  };

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a PDF or DOCX file to upload.');
      return;
    }

    setUploading(true);
    setError('');
    setSuccess('');

    try {
      await api.uploadDocument(file, departmentScope, roleScope);
      setSuccess('Policy document uploaded and indexed in ChromaDB successfully!');
      setFile(null);
      fetchDashboardData();
    } catch (err: any) {
      setError(err.message || 'Failed to upload document.');
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteDocument = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this document and remove all its vectors from ChromaDB?')) {
      return;
    }
    setError('');
    setSuccess('');
    try {
      await api.deleteAdminDocument(id);
      setSuccess('Document and associated vector embeddings deleted successfully.');
      fetchDashboardData();
    } catch (err: any) {
      setError(err.message || 'Failed to delete document.');
    }
  };

  const handleRoleChange = async (userId: number, role: string) => {
    setError('');
    setSuccess('');
    try {
      await api.updateUserRole(userId, role);
      setSuccess('User role updated successfully.');
      fetchDashboardData();
    } catch (err: any) {
      setError(err.message || 'Failed to update user role.');
    }
  };

  return (
    <div className="admin-grid">
      {/* Policy Ingestion Card */}
      <div className="admin-card">
        <h2>Ingest Policy Guidelines</h2>
        
        {error && (
          <div style={{
            background: 'var(--error-bg)',
            border: '1px solid var(--error-border)',
            color: 'var(--error)',
            padding: '12px',
            borderRadius: 'var(--radius-md)',
            marginBottom: '20px',
            fontSize: '13px'
          }}>
            {error}
          </div>
        )}

        {success && (
          <div style={{
            background: 'var(--success-bg)',
            border: '1px solid var(--success-border)',
            color: 'var(--success)',
            padding: '12px',
            borderRadius: 'var(--radius-md)',
            marginBottom: '20px',
            fontSize: '13px'
          }}>
            {success}
          </div>
        )}

        <form onSubmit={handleUploadSubmit}>
          <label className="file-dropzone">
            <input
              type="file"
              accept=".pdf,.docx"
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />
            <div className="file-icon">
              <Upload size={32} />
            </div>
            <p style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '4px' }}>
              Select policy document to index
            </p>
            <p>PDF and Word DOCX formats supported</p>
          </label>

          {file && (
            <div className="selected-file-info">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <FileText size={16} style={{ color: 'var(--accent)' }} />
                <span>{file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)</span>
              </div>
              <button
                type="button"
                onClick={() => setFile(null)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: 'var(--text-muted)',
                  cursor: 'pointer'
                }}
              >
                Clear
              </button>
            </div>
          )}

          <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginTop: '12px' }}>
            <div>
              <label className="form-label">Role Visibility Scope</label>
              <select
                className="form-input form-select"
                value={roleScope}
                onChange={(e) => setRoleScope(e.target.value)}
              >
                <option value="ALL">ALL (Universal)</option>
                <option value="STUDENT">STUDENT Only</option>
                <option value="EMPLOYEE">EMPLOYEE Only</option>
                <option value="HR">HR Only</option>
                <option value="WARDEN">WARDEN Only</option>
                <option value="ADMIN">ADMIN Only</option>
              </select>
            </div>
            <div>
              <label className="form-label">Department Scope</label>
              <input
                type="text"
                placeholder="e.g. CS (or ALL)"
                className="form-input"
                value={departmentScope}
                onChange={(e) => setDepartmentScope(e.target.value)}
                required
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={uploading || !file}
            style={{ marginTop: '12px', width: '100%' }}
          >
            {uploading ? 'Parsing & Indexing Vectors...' : 'Upload & Ingest Document'}
          </button>
        </form>
      </div>

      {/* Analytics Card */}
      <div className="admin-card" style={{ display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2>Guardian Analytics</h2>
          <button
            onClick={fetchDashboardData}
            disabled={refreshing}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-secondary)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '12px'
            }}
          >
            <RefreshCw size={12} className={refreshing ? 'spin' : ''} />
            Refresh
          </button>
        </div>

        <div className="analytics-stats" style={{ marginBottom: '16px' }}>
          <div className="stat-card">
            <div className="stat-number">{documents.length}</div>
            <div className="stat-label">Ingested Policies</div>
          </div>
          <div className="stat-card">
            <div className="stat-number" style={{ color: 'var(--error)' }}>
              {escalations.length}
            </div>
            <div className="stat-label">Escalated Tickets</div>
          </div>
        </div>

        {/* Lightweight Usage Visualization */}
        <h3 style={{ fontSize: '13px', textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '8px' }}>Usage Metrics (By Date)</h3>
        <div style={{ flex: 1, overflowY: 'auto', maxHeight: '150px' }}>
          {usageStats.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '12px' }}>No usage volume logs recorded yet.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {usageStats.map((item, index) => (
                <div key={index} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', background: 'rgba(255,255,255,0.02)', padding: '6px 10px', borderRadius: '4px' }}>
                  <span>{new Date(item[0]).toLocaleDateString()} ({item[1]} / {item[2]})</span>
                  <span style={{ fontWeight: 600, color: 'var(--accent)' }}>{item[3]} queries</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Unanswered Questions (Document Gaps) Table */}
      <div className="admin-card" style={{ gridColumn: '1 / -1' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--accent)' }}>
          <ShieldAlert size={20} />
          Unanswered Queries (Knowledge Gaps)
        </h2>
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
          Grouped list of user queries resulting in insufficient context. Upload relevant documents to fill these gaps.
        </p>
        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>User Query</th>
                <th style={{ width: '120px', textAlign: 'center' }}>Trigger Frequency</th>
              </tr>
            </thead>
            <tbody>
              {unansweredQueries.length === 0 ? (
                <tr>
                  <td colSpan={2} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                    No unanswered queries recorded yet.
                  </td>
                </tr>
              ) : (
                unansweredQueries.map(([query, count], idx) => (
                  <tr key={idx}>
                    <td style={{ color: 'var(--warning)', fontWeight: 500 }}>{query}</td>
                    <td style={{ textAlign: 'center', fontWeight: 700, color: 'var(--text-primary)' }}>{count} times</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* System Ingestion Register */}
      <div className="admin-card" style={{ gridColumn: '1 / -1' }}>
        <h2>System Ingestion Register</h2>
        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Document Name</th>
                <th>Format</th>
                <th>Role Scope</th>
                <th>Dept Scope</th>
                <th>Uploaded By</th>
                <th style={{ width: '100px', textAlign: 'center' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {documents.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                    No policy documents ingested yet.
                  </td>
                </tr>
              ) : (
                documents.map((doc) => (
                  <tr key={doc.id}>
                    <td style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <FileSpreadsheet size={14} style={{ color: 'var(--accent)' }} />
                        {doc.name}
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-type">{doc.docType}</span>
                    </td>
                    <td>
                      <span className="badge badge-role">{doc.roleScope}</span>
                    </td>
                    <td>
                      <span className="badge badge-dept">{doc.departmentScope}</span>
                    </td>
                    <td>{doc.uploadedBy}</td>
                    <td style={{ textAlign: 'center' }}>
                      <button
                        className="btn-logout"
                        onClick={() => handleDeleteDocument(doc.id)}
                        style={{ padding: '6px', background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--error)' }}
                        title="Delete Document"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* User Directory Management (ADMIN Only) */}
      <div className="admin-card" style={{ gridColumn: '1 / -1' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Users size={20} style={{ color: 'var(--accent)' }} />
          User Account Directory Management
        </h2>
        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email Address</th>
                <th>Department</th>
                <th>Current Role</th>
                <th style={{ width: '150px' }}>Change Role</th>
              </tr>
            </thead>
            <tbody>
              {usersList.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                    No user accounts found.
                  </td>
                </tr>
              ) : (
                usersList.map((user) => (
                  <tr key={user.id}>
                    <td style={{ fontWeight: 600 }}>{user.username}</td>
                    <td style={{ color: 'var(--text-secondary)' }}>{user.email}</td>
                    <td>{user.department}</td>
                    <td>
                      <span 
                        style={{
                          fontSize: '11px',
                          padding: '2px 8px',
                          borderRadius: '12px',
                          background: 'rgba(255,255,255,0.05)',
                          color: 'var(--accent)',
                          fontWeight: 600
                        }}
                      >
                        {user.role}
                      </span>
                    </td>
                    <td>
                      <select
                        style={{
                          background: 'var(--bg-dark-700)',
                          border: '1px solid rgba(255,255,255,0.1)',
                          color: 'var(--text-primary)',
                          borderRadius: '4px',
                          padding: '4px 8px',
                          fontSize: '12px',
                          cursor: 'pointer'
                        }}
                        value={user.role}
                        onChange={(e) => handleRoleChange(user.id, e.target.value)}
                      >
                        <option value="STUDENT">STUDENT</option>
                        <option value="EMPLOYEE">EMPLOYEE</option>
                        <option value="HR">HR</option>
                        <option value="WARDEN">WARDEN</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
