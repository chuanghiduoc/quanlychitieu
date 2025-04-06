'use client';

import { useState, useEffect } from 'react';
import { UserResponse, UsersApiResponse } from '@/types/api';

export default function UsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await fetch('/api/users');
      const data = await response.json();
      const { users } = data as UsersApiResponse;
      setUsers(users.map(user => ({
        ...user,
        email: user.email || '',
        displayName: user.displayName || 'Không có tên'
      })));
    } catch (error) {
      console.error('Lỗi khi lấy danh sách users:', error);
    } finally {
      setLoading(false);
    }
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
      fetchUsers();
    } catch (error) {
      console.error('Lỗi khi cập nhật trạng thái user:', error);
    }
  };

  if (loading) return <div>Đang tải...</div>;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">Quản lý người dùng</h1>
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white border border-gray-300">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 border-b">ID</th>
              <th className="px-6 py-3 border-b">Email</th>
              <th className="px-6 py-3 border-b">Tên hiển thị</th>
              <th className="px-6 py-3 border-b">Trạng thái</th>
              <th className="px-6 py-3 border-b">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.uid} className="hover:bg-gray-50">
                <td className="px-6 py-4 border-b">{user.uid}</td>
                <td className="px-6 py-4 border-b">{user.email}</td>
                <td className="px-6 py-4 border-b">{user.displayName}</td>
                <td className="px-6 py-4 border-b">
                  <span className={`px-2 py-1 rounded ${user.disabled ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`}>
                    {user.disabled ? 'Vô hiệu hóa' : 'Hoạt động'}
                  </span>
                </td>
                <td className="px-6 py-4 border-b">
                  <button
                    onClick={() => toggleUserStatus(user.uid, user.disabled)}
                    className={`px-4 py-2 rounded ${user.disabled ? 'bg-green-500 hover:bg-green-600' : 'bg-red-500 hover:bg-red-600'} text-white`}
                  >
                    {user.disabled ? 'Kích hoạt' : 'Vô hiệu hóa'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}