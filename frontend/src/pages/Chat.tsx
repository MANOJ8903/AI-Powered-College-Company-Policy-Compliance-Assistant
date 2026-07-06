import React, { useState, useRef, useEffect } from 'react';
import { api, type Citation } from '../services/api';
import { Send, BookOpen, AlertCircle, CheckCircle2, ShieldAlert } from 'lucide-react';

interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  citations?: Citation[];
  confidence?: 'high' | 'medium' | 'low';
  escalate?: boolean;
  escalationContact?: string | null;
  insufficientContext?: boolean;
  followUpSuggestions?: string[];
}

export const Chat: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'assistant',
      text: 'Hello! I am your AI Policy Guardian compliance assistant. Ask me any questions about organization policies, leave rules, hostel regulations, or HR SOPs.',
    },
  ]);
  const [input, setInput] = useState('');
  const [sessionId, setSessionId] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [activeCitation, setActiveCitation] = useState<Citation | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userQuestion = input.trim();
    setInput('');
    
    // Add user message
    const userMessageId = Math.random().toString();
    setMessages((prev) => [
      ...prev,
      { id: userMessageId, sender: 'user', text: userQuestion },
    ]);
    
    setLoading(true);

    try {
      // Call REST RAG endpoint
      const response = await api.askQuestion(userQuestion, sessionId || undefined);
      
      // Update session ID if it was returned new
      if (response.sessionId) {
        setSessionId(response.sessionId);
      }

      setMessages((prev) => [
        ...prev,
        {
          id: Math.random().toString(),
          sender: 'assistant',
          text: response.answer,
          citations: response.citations,
          confidence: response.confidence as any,
          escalate: response.escalate,
          escalationContact: response.escalationContact,
          insufficientContext: response.insufficientContext,
          followUpSuggestions: response.followUpSuggestions,
        },
      ]);
    } catch (err: any) {
      setMessages((prev) => [
        ...prev,
        {
          id: Math.random().toString(),
          sender: 'assistant',
          text: err.message || 'Sorry, I encountered an error retrieving policy details. Please try again.',
          escalate: true,
          confidence: 'low',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="chat-window">
      {/* Scrollable messages thread */}
      <div className="messages-list">
        {messages.map((msg) => (
          <div key={msg.id} className={`message-wrapper ${msg.sender}`}>
            <div className="message-avatar">
              {msg.sender === 'user' ? 'U' : 'AI'}
            </div>
            
            <div 
              className={`message-bubble ${msg.insufficientContext ? 'insufficient' : ''}`}
              style={msg.insufficientContext ? {
                border: '1px dashed var(--accent)',
                background: 'rgba(255, 179, 0, 0.05)'
              } : {}}
            >
              <div>{msg.text}</div>

              {/* Confidence scores and escalation alerts */}
              {msg.sender === 'assistant' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '10px' }}>
                  {msg.escalate && (
                    <span className="confidence-indicator escalated" style={{ width: 'fit-content' }}>
                      <ShieldAlert size={12} />
                      Escalated to: {msg.escalationContact || 'HR Department'}
                    </span>
                  )}
                  
                  {msg.confidence && (
                    <span 
                      style={{
                        width: 'fit-content',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '11px',
                        padding: '2px 8px',
                        borderRadius: '12px',
                        textTransform: 'capitalize',
                        fontWeight: 600,
                        border: '1px solid',
                        borderColor: msg.confidence === 'high' ? 'var(--success)' : (msg.confidence === 'medium' ? 'var(--accent)' : 'var(--error)'),
                        color: msg.confidence === 'high' ? 'var(--success)' : (msg.confidence === 'medium' ? 'var(--accent)' : 'var(--error)')
                      }}
                    >
                      {msg.confidence === 'high' && <CheckCircle2 size={10} />}
                      {msg.confidence === 'medium' && <CheckCircle2 size={10} />}
                      {msg.confidence === 'low' && <AlertCircle size={10} />}
                      {msg.confidence} confidence
                    </span>
                  )}
                </div>
              )}

              {/* Citations block */}
              {msg.sender === 'assistant' && msg.citations && msg.citations.length > 0 && (
                <div className="citation-container">
                  <div className="citation-title">Grounded Source Citations:</div>
                  <div className="citation-pills">
                    {msg.citations.map((cite, index) => (
                      <button
                        key={index}
                        className="citation-pill"
                        onClick={() => setActiveCitation(cite)}
                        title="Click to view parsed source text snippet"
                      >
                        <BookOpen size={10} style={{ marginRight: '4px', verticalAlign: 'middle' }} />
                        {cite.doc_name.length > 20 ? cite.doc_name.substring(0, 17) + '...' : cite.doc_name} : {cite.section}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Follow-up suggestions */}
              {msg.sender === 'assistant' && msg.followUpSuggestions && msg.followUpSuggestions.length > 0 && (
                <div style={{ marginTop: '12px' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginBottom: '6px' }}>Suggested follow-up:</div>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    {msg.followUpSuggestions.map((suggestion, idx) => (
                      <button
                        key={idx}
                        onClick={() => setInput(suggestion)}
                        style={{
                          background: 'rgba(255, 255, 255, 0.05)',
                          border: '1px solid rgba(255, 255, 255, 0.1)',
                          color: 'var(--text-primary)',
                          borderRadius: '16px',
                          padding: '4px 12px',
                          fontSize: '11px',
                          cursor: 'pointer',
                          transition: 'all 0.2s',
                          textAlign: 'left'
                        }}
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        ))}
        
        {loading && (
          <div className="message-wrapper assistant">
            <div className="message-avatar">AI</div>
            <div className="message-bubble" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{
                width: '8px',
                height: '8px',
                background: 'var(--accent)',
                borderRadius: '50%',
                animation: 'fadeIn 0.6s infinite alternate'
              }}></div>
              <div style={{
                width: '8px',
                height: '8px',
                background: 'var(--accent)',
                borderRadius: '50%',
                animation: 'fadeIn 0.6s infinite alternate',
                animationDelay: '0.2s'
              }}></div>
              <div style={{
                width: '8px',
                height: '8px',
                background: 'var(--accent)',
                borderRadius: '50%',
                animation: 'fadeIn 0.6s infinite alternate',
                animationDelay: '0.4s'
              }}></div>
              <span style={{ color: 'var(--text-secondary)', fontSize: '13px', marginLeft: '6px' }}>Searching vector stores & analyzing guidelines...</span>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input bar */}
      <form onSubmit={handleSend} className="chat-input-container">
        <div className="chat-input-wrapper">
          <input
            type="text"
            className="chat-input"
            placeholder="Ask compliance questions (e.g. Can students leave hostel after 9 PM?)"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={loading}
          />
          <button
            type="submit"
            className="chat-submit-btn"
            disabled={loading || !input.trim()}
          >
            <Send size={16} />
          </button>
        </div>
      </form>

      {/* Citation overlay dialog */}
      {activeCitation && (
        <div className="modal-overlay" onClick={() => setActiveCitation(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <h3>{activeCitation.doc_name}</h3>
                <div className="modal-subtitle">Section: {activeCitation.section}</div>
              </div>
              <button className="modal-close-btn" onClick={() => setActiveCitation(null)}>
                &times;
              </button>
            </div>
            <div className="modal-body">
              <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)', marginBottom: '8px' }}>
                Grounded snippet from index:
              </p>
              <div style={{ lineHeight: '1.5', whiteSpace: 'pre-wrap' }}>{activeCitation.excerpt}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
