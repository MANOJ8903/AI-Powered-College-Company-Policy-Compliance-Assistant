import axios from 'axios';

export interface User {
  token: string;
  tokenType: string;
  username: string;
  email: string;
  role: string;
  department: string;
}

export interface Citation {
  doc_id?: string;
  doc_name: string;
  section: string;
  excerpt: string;
}

export interface ChatResponse {
  answer: string;
  citations: Citation[];
  confidence: string;
  insufficientContext: boolean;
  escalate: boolean;
  escalationContact?: string | null;
  followUpSuggestions: string[];
  sessionId: string;
}

export interface PolicyDocument {
  id: number;
  name: string;
  docType: string;
  departmentScope: string;
  roleScope: string;
  uploadedBy: string;
  uploadTime: string;
}

const API_BASE = (import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api')).replace(/\/$/, '');

// In-Memory JWT token storage for security (avoids localStorage XSS capture vulnerabilities)
let inMemoryToken: string | null = null;

export const setInMemoryToken = (token: string | null) => {
  inMemoryToken = token;
};

export const getInMemoryToken = () => {
  return inMemoryToken;
};

// Create Axios Client Instance
const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Configure Axios Request Interceptor to dynamically attach JWT headers
apiClient.interceptors.request.use(
  (config) => {
    if (inMemoryToken) {
      config.headers.Authorization = `Bearer ${inMemoryToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const api = {
  async register(username: string, email: string, role: string, department: string, password: string) {
    try {
      const res = await apiClient.post('/auth/register', {
        username,
        email,
        role,
        department,
        password,
      });
      return res.data;
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 
                       (err.response?.data?.errors && err.response.data.errors[0]?.defaultMessage) ||
                       'Registration failed';
      throw new Error(errorMsg);
    }
  },

  async login(username: string, password: string): Promise<User> {
    try {
      const res = await apiClient.post('/auth/login', {
        username,
        password,
      });
      const user: User = res.data;
      // Save token in memory
      setInMemoryToken(user.token);
      return user;
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 
                       (err.response?.data?.errors && err.response.data.errors[0]?.defaultMessage) ||
                       'Login failed';
      throw new Error(errorMsg);
    }
  },

  async askQuestion(question: string, sessionId?: string): Promise<ChatResponse> {
    try {
      const res = await apiClient.post('/chat', {
        question,
        sessionId,
      });
      return res.data;
    } catch (err: any) {
      if (err.response?.status === 403) {
        throw new Error('Access denied: You do not have permission to query this resource.');
      }
      throw new Error(err.response?.data?.message || 'Failed to retrieve response from assistant');
    }
  },

  async uploadDocument(file: File, departmentScope: string, roleScope: string): Promise<PolicyDocument> {
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('departmentScope', departmentScope);
      formData.append('roleScope', roleScope);

      const res = await apiClient.post('/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Document upload failed');
    }
  },

  async getDocuments(): Promise<PolicyDocument[]> {
    try {
      const res = await apiClient.get('/documents/all');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve uploaded documents');
    }
  },

  async getQueriesByDept(): Promise<[string, number][]> {
    try {
      const res = await apiClient.get('/analytics/queries-by-dept');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve department metrics');
    }
  },

  async getEscalations(): Promise<any[]> {
    try {
      const res = await apiClient.get('/analytics/escalated');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve escalated queries');
    }
  },

  async deleteDocument(id: number): Promise<void> {
    try {
      await apiClient.delete(`/documents/${id}`);
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to delete document');
    }
  },

  async getUnansweredQueries(): Promise<[string, number][]> {
    try {
      const res = await apiClient.get('/admin/analytics/unanswered');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve unanswered queries');
    }
  },

  async getUsageStats(): Promise<any[]> {
    try {
      const res = await apiClient.get('/admin/analytics/usage');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve usage stats');
    }
  },

  async getAdminDocuments(): Promise<PolicyDocument[]> {
    try {
      const res = await apiClient.get('/admin/documents');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve admin documents');
    }
  },

  async deleteAdminDocument(id: number): Promise<void> {
    try {
      await apiClient.delete(`/admin/documents/${id}`);
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to delete document');
    }
  },

  async getAdminUsers(): Promise<any[]> {
    try {
      const res = await apiClient.get('/admin/users');
      return res.data;
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to retrieve user directory');
    }
  },

  async updateUserRole(id: number, role: string): Promise<void> {
    try {
      await apiClient.put(`/admin/users/${id}/role`, { role });
    } catch (err: any) {
      throw new Error(err.response?.data?.message || 'Failed to update user role');
    }
  }
};
