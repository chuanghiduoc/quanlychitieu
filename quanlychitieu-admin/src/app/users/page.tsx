'use client';

import { useState, useEffect } from 'react';
import { UserResponse, UsersApiResponse } from '@/types/api';
import { FiSearch, FiRefreshCw, FiChevronLeft, FiChevronRight, FiUser } from 'react-icons/fi';

interface FilterState {
  status: 'all' | 'active' | 'disabled';
  search: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<FilterState>({
    status: 'all',
    search: ''
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const itemsPerPage = 10;

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setError(null);
    setLoading(true);
    try {
      const response = await fetch('/api/users');
      if (!response.ok) {
        throw new Error('Lỗi khi tải dữ liệu người dùng');
      }
      const data = await response.json();
      const { users } = data as UsersApiResponse;
      setUsers(users.map(user => ({
        ...user,
        email: user.email || '',
        displayName: user.displayName || 'Không có tên'
      })));
    } catch (error) {
      console.error('Lỗi khi lấy danh sách users:', error);
      setError('Không thể tải danh sách người dùng. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  const refreshUsers = async () => {
    setIsRefreshing(true);
    await fetchUsers();
    setIsRefreshing(false);
  };

  const toggleUserStatus = async (uid: string, currentStatus: boolean) => {
    try {
      const response = await fetch('/api/users', {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          uid,
          disabled: !currentStatus
        })
      });

      if (!response.ok) {
        throw new Error('Lỗi khi cập nhật trạng thái user');
      }
      await fetchUsers();
    } catch (error) {
      console.error('Lỗi khi cập nhật trạng thái user:', error);
      setError('Không thể cập nhật trạng thái người dùng. Vui lòng thử lại sau.');
    }
  };

  const filteredUsers = users.filter(user => {
    const matchesStatus = 
      filters.status === 'all' ? true :
      filters.status === 'active' ? !user.disabled :
      user.disabled;

    const matchesSearch = 
      (user.email?.toLowerCase() || '').includes(filters.search.toLowerCase()) ||
      (user.displayName?.toLowerCase() || '').includes(filters.search.toLowerCase()) ||
      user.uid.toLowerCase().includes(filters.search.toLowerCase());

    return matchesStatus && matchesSearch;
  });

  const totalPages = Math.ceil(filteredUsers.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const paginatedUsers = filteredUsers.slice(startIndex, startIndex + itemsPerPage);

  const LoadingSpinner = () => (
    <div className="flex justify-center items-center p-8">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
    </div>
  );

  if (loading) return <LoadingSpinner />;

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900 flex items-center">
            <FiUser className="mr-2" />
            Quản lý người dùng
          </h1>
          <button
            onClick={refreshUsers}
            disabled={isRefreshing}
            className="flex items-center px-4 py-2 bg-white border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <FiRefreshCw className={`mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
          </div>
        )}

        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="p-4 border-b border-gray-200 space-y-4 sm:space-y-0 sm:flex sm:items-center sm:justify-between">
            <div className="flex items-center space-x-4 flex-1 max-w-lg">
              <div className="flex-1 relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <FiSearch className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="text"
                  value={filters.search}
                  onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                  placeholder="Tìm kiếm theo email, tên..."
                  className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
              </div>
              <div className="relative inline-block text-left">
                <select
                  value={filters.status}
                  onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value as FilterState['status'] }))}
                  className="block w-full pl-3 pr-10 py-2 text-base border border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
                >
                  <option value="all">Tất cả</option>
                  <option value="active">Đang hoạt động</option>
                  <option value="disabled">Đã vô hiệu hóa</option>
                </select>
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    ID
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Tên hiển thị
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Trạng thái
                  </th>
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {paginatedUsers.map((user) => (
                  <tr key={user.uid} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {user.uid}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {user.email}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {user.displayName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        user.disabled 
                          ? 'bg-red-100 text-red-800' 
                          : 'bg-green-100 text-green-800'
                      }`}>
                        {user.disabled ? 'Vô hiệu hóa' : 'Hoạt động'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <button
                        onClick={() => toggleUserStatus(user.uid, user.disabled)}
                        className={`inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-medium rounded-full shadow-sm text-white ${
                          user.disabled
                            ? 'bg-green-600 hover:bg-green-700'
                            : 'bg-red-600 hover:bg-red-700'
                        } focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                          user.disabled
                            ? 'focus:ring-green-500'
                            : 'focus:ring-red-500'
                        }`}
                      >
                        {user.disabled ? 'Kích hoạt' : 'Vô hiệu hóa'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
              <div className="flex-1 flex justify-between sm:hidden">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                  disabled={currentPage === 1}
                  className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  Trước
                </button>
                <button
                  onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  Sau
                </button>
              </div>
              <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700">
                    Hiển thị <span className="font-medium">{startIndex + 1}</span> đến{' '}
                    <span className="font-medium">
                      {Math.min(startIndex + itemsPerPage, filteredUsers.length)}
                    </span>{' '}
                    trong tổng số{' '}
                    <span className="font-medium">{filteredUsers.length}</span> người dùng
                  </p>
                </div>
                <div>
                  <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                    <button
                      onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                      disabled={currentPage === 1}
                      className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <span className="sr-only">Trang trước</span>
                      <FiChevronLeft className="h-5 w-5" />
                    </button>
                    {[...Array(totalPages)].map((_, i) => (
                      <button
                        key={i + 1}
                        onClick={() => setCurrentPage(i + 1)}
                        className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                          currentPage === i + 1
                            ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                            : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                        }`}
                      >
                        {i + 1}
                      </button>
                    ))}
                    <button
                      onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                      disabled={currentPage === totalPages}
                      className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      <span className="sr-only">Trang sau</span>
                      <FiChevronRight className="h-5 w-5" />
                    </button>
                  </nav>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}