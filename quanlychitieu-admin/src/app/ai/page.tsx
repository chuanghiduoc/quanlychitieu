'use client';

import { useState } from 'react';
import ReactMarkdown from 'react-markdown';

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
}

interface FinancialAdviceForm {
  userId: string;
  message: string;
}

export default function AIPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [expenseData, setExpenseData] = useState('');
  const [loading, setLoading] = useState(false);
  const [financialAdvice, setFinancialAdvice] = useState<FinancialAdviceForm>({
    userId: '',
    message: ''
  });

  const handleChat = async () => {
    if (!input.trim()) return;

    setLoading(true);
    const newMessage: ChatMessage = { role: 'user', content: input };
    setMessages(prev => [...prev, newMessage]);
    setInput('');

    try {
      const response = await fetch('/api/ai', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          type: 'chat',
          content: input,
        }),
      });

      const data = await response.json();
      
      if (response.ok) {
        const aiMessage: ChatMessage = { role: 'ai', content: data.response };
        setMessages(prev => [...prev, aiMessage]);
      } else {
        throw new Error(data.error || 'Có lỗi xảy ra');
      }
    } catch (error) {
      console.error('Lỗi khi gửi tin nhắn:', error);
      alert('Có lỗi xảy ra khi gửi tin nhắn');
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyzeExpense = async () => {
    if (!expenseData.trim()) return;

    setLoading(true);
    try {
      const response = await fetch('/api/ai', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          type: 'analyze',
          content: expenseData,
        }),
      });

      const data = await response.json();
      
      if (response.ok) {
        const aiMessage: ChatMessage = { 
          role: 'ai', 
          content: data.response 
        };
        setMessages(prev => [
          ...prev,
          { role: 'user', content: `Phân tích chi tiêu:\n${expenseData}` },
          aiMessage
        ]);
        setExpenseData('');
      } else {
        throw new Error(data.error || 'Có lỗi xảy ra');
      }
    } catch (error) {
      console.error('Lỗi khi phân tích chi tiêu:', error);
      alert('Có lỗi xảy ra khi phân tích chi tiêu');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">AI Assistant</h1>
      
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Phân tích chi tiêu</h2>
        <textarea
          value={expenseData}
          onChange={(e) => setExpenseData(e.target.value)}
          className="w-full h-32 p-2 border rounded mb-4"
          placeholder="Nhập thông tin chi tiêu của bạn..."
        />
        <button
          onClick={handleAnalyzeExpense}
          disabled={loading}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:bg-gray-400"
        >
          {loading ? 'Đang phân tích...' : 'Phân tích'}
        </button>
      </div>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">Tư vấn tài chính</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              User ID
            </label>
            <input
              type="text"
              value={financialAdvice.userId}
              onChange={(e) => setFinancialAdvice(prev => ({...prev, userId: e.target.value}))}
              className="w-full p-2 border rounded"
              placeholder="Nhập User ID..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Câu hỏi tư vấn
            </label>
            <textarea
              value={financialAdvice.message}
              onChange={(e) => setFinancialAdvice(prev => ({...prev, message: e.target.value}))}
              className="w-full h-32 p-2 border rounded"
              placeholder="Nhập câu hỏi của bạn về tài chính..."
            />
          </div>
          <button
            onClick={async () => {
              if (!financialAdvice.userId || !financialAdvice.message) {
                alert('Vui lòng nhập đầy đủ thông tin');
                return;
              }
              
              setLoading(true);
              try {
                const response = await fetch('/api/ai', {
                  method: 'POST',
                  headers: {
                    'Content-Type': 'application/json',
                  },
                  body: JSON.stringify({
                    type: 'financial-advice',
                    content: financialAdvice
                  }),
                });
                
                const data = await response.json();
                
                if (response.ok) {
                  setMessages(prev => [
                    ...prev,
                    { role: 'user', content: `Tư vấn tài chính:\n${financialAdvice.message}` },
                    { role: 'ai', content: data.response }
                  ]);
                  setFinancialAdvice({ userId: '', message: '' });
                } else {
                  throw new Error(data.error || 'Có lỗi xảy ra');
                }
              } catch (error) {
                console.error('Lỗi khi gửi yêu cầu tư vấn:', error);
                alert('Có lỗi xảy ra khi gửi yêu cầu tư vấn');
              } finally {
                setLoading(false);
              }
            }}
            disabled={loading}
            className="w-full bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:bg-gray-400"
          >
            {loading ? 'Đang xử lý...' : 'Gửi yêu cầu tư vấn'}
          </button>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Chat với AI</h2>
        <div className="space-y-4 mb-4 max-h-96 overflow-y-auto">
          {messages.map((msg, index) => (
            <div
              key={index}
              className={`p-3 rounded ${
                msg.role === 'user'
                  ? 'bg-blue-100 ml-12'
                  : 'bg-gray-100 mr-12'
              }`}
            >
              {msg.role === 'ai' ? (
                <div className="prose max-w-none">
                  <ReactMarkdown>{msg.content}</ReactMarkdown>
                </div>
              ) : (
                <p className="whitespace-pre-line">{msg.content}</p>
              )}
            </div>
          ))}
        </div>
        
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleChat()}
            className="flex-1 p-2 border rounded"
            placeholder="Nhập tin nhắn..."
          />
          <button
            onClick={handleChat}
            disabled={loading}
            className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:bg-gray-400"
          >
            {loading ? 'Đang gửi...' : 'Gửi'}
          </button>
        </div>
      </div>
    </div>
  );
}