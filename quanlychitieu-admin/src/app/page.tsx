'use client';

import { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { FiSend, FiBarChart2, FiMessageCircle, FiDollarSign } from 'react-icons/fi';

interface ChatMessage {
  role: 'user' | 'ai';
  content: string;
  timestamp?: Date;
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
  const [activeTab, setActiveTab] = useState<'chat' | 'analyze' | 'advice'>('chat');
  const [financialAdvice, setFinancialAdvice] = useState<FinancialAdviceForm>({
    userId: '',
    message: ''
  });

  const handleChat = async () => {
    if (!input.trim() || loading) return;

    setLoading(true);
    const newMessage: ChatMessage = { 
      role: 'user', 
      content: input,
      timestamp: new Date()
    };
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
        const aiMessage: ChatMessage = { 
          role: 'ai', 
          content: data.response,
          timestamp: new Date()
        };
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
    if (!expenseData.trim() || loading) return;

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
          content: data.response,
          timestamp: new Date()
        };
        setMessages(prev => [
          ...prev,
          { 
            role: 'user', 
            content: `Phân tích chi tiêu:\n${expenseData}`,
            timestamp: new Date()
          },
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

  const handleFinancialAdvice = async () => {
    if (!financialAdvice.userId || !financialAdvice.message || loading) {
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
          { 
            role: 'user', 
            content: `Tư vấn tài chính:\n${financialAdvice.message}`,
            timestamp: new Date()
          },
          { 
            role: 'ai', 
            content: data.response,
            timestamp: new Date()
          }
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
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-5xl mx-auto px-4">
        <h1 className="text-3xl font-bold text-gray-800 mb-8 flex items-center">
          <FiMessageCircle className="mr-2" />
          AI Tư Vấn Tài Chính
        </h1>

        <div className="bg-white rounded-xl shadow-lg overflow-hidden">
          {/* Tab Navigation */}
          <div className="flex border-b">
            <button
              onClick={() => setActiveTab('chat')}
              className={`flex items-center px-6 py-4 ${
                activeTab === 'chat'
                  ? 'border-b-2 border-blue-500 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              <FiMessageCircle className="mr-2" />
              Chat với AI
            </button>
            <button
              onClick={() => setActiveTab('analyze')}
              className={`flex items-center px-6 py-4 ${
                activeTab === 'analyze'
                  ? 'border-b-2 border-blue-500 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              <FiBarChart2 className="mr-2" />
              Phân tích chi tiêu
            </button>
            <button
              onClick={() => setActiveTab('advice')}
              className={`flex items-center px-6 py-4 ${
                activeTab === 'advice'
                  ? 'border-b-2 border-blue-500 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              <FiDollarSign className="mr-2" />
              Tư vấn tài chính
            </button>
          </div>

          {/* Tab Content */}
          <div className="p-6">
            {/* Messages Display */}
            <div className="mb-6 space-y-4 max-h-[500px] overflow-y-auto">
              {messages.map((msg, index) => (
                <div
                  key={index}
                  className={`flex ${
                    msg.role === 'user' ? 'justify-end' : 'justify-start'
                  }`}
                >
                  <div
                    className={`max-w-[80%] rounded-lg p-4 ${
                      msg.role === 'user'
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    <div className="prose prose-sm max-w-none">
                      <ReactMarkdown>
                        {msg.content}
                      </ReactMarkdown>
                    </div>
                    {msg.timestamp && (
                      <div className={`text-xs mt-2 ${
                        msg.role === 'user' ? 'text-blue-100' : 'text-gray-500'
                      }`}>
                        {msg.timestamp.toLocaleTimeString()}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Input Areas */}
            {activeTab === 'chat' && (
              <div className="flex items-center space-x-2">
                <input
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleChat()}
                  placeholder="Nhập tin nhắn của bạn..."
                  className="flex-1 p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  onClick={handleChat}
                  disabled={loading}
                  className="bg-blue-500 text-white p-3 rounded-lg hover:bg-blue-600 disabled:bg-gray-400 transition-colors"
                >
                  <FiSend className="w-5 h-5" />
                </button>
              </div>
            )}

            {activeTab === 'analyze' && (
              <div className="space-y-4">
                <textarea
                  value={expenseData}
                  onChange={(e) => setExpenseData(e.target.value)}
                  className="w-full h-40 p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Nhập thông tin chi tiêu của bạn..."
                />
                <button
                  onClick={handleAnalyzeExpense}
                  disabled={loading}
                  className="w-full bg-blue-500 text-white px-6 py-3 rounded-lg hover:bg-blue-600 disabled:bg-gray-400 transition-colors flex items-center justify-center"
                >
                  <FiBarChart2 className="mr-2" />
                  {loading ? 'Đang phân tích...' : 'Phân tích'}
                </button>
              </div>
            )}

            {activeTab === 'advice' && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    User ID
                  </label>
                  <input
                    type="text"
                    value={financialAdvice.userId}
                    onChange={(e) => setFinancialAdvice(prev => ({...prev, userId: e.target.value}))}
                    className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
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
                    className="w-full h-40 p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Nhập câu hỏi của bạn về tài chính..."
                  />
                </div>
                <button
                  onClick={handleFinancialAdvice}
                  disabled={loading}
                  className="w-full bg-blue-500 text-white px-6 py-3 rounded-lg hover:bg-blue-600 disabled:bg-gray-400 transition-colors flex items-center justify-center"
                >
                  <FiDollarSign className="mr-2" />
                  {loading ? 'Đang xử lý...' : 'Gửi yêu cầu tư vấn'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}