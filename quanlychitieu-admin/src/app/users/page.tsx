"use client";

import { useState, useEffect } from "react";
import { UserResponse, UsersApiResponse } from "@/types/api";
import {
  FiSearch,
  FiRefreshCw,
  FiChevronLeft,
  FiChevronRight,
  FiUser,
  FiFilter,
  FiAlertCircle,
} from "react-icons/fi";

interface FilterState {
  status: "all" | "active" | "disabled";
  search: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<FilterState>({
    status: "all",
    search: "",
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const itemsPerPage = 10;

  useEffect(() => {
    fetchUsers();
  }, []);

  useEffect(() => {
    // Reset to first page when filters change
    setCurrentPage(1);
  }, [filters]);

  const fetchUsers = async () => {
    setError(null);
    setLoading(true);
    try {
      const response = await fetch("/api/users");
      if (!response.ok) {
        throw new Error("Lỗi khi tải dữ liệu người dùng");
      }
      const data = await response.json();
      const { users } = data as UsersApiResponse;
      setUsers(
        users.map((user) => ({
          ...user,
          email: user.email || "",
          displayName: user.displayName || "Không có tên",
        }))
      );
    } catch (error) {
      console.error("Lỗi khi lấy danh sách users:", error);
      setError("Không thể tải danh sách người dùng. Vui lòng thử lại sau.");
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
      const response = await fetch("/api/users", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          uid,
          disabled: !currentStatus,
        }),
      });

      if (!response.ok) {
        throw new Error("Lỗi khi cập nhật trạng thái user");
      }
      await fetchUsers();
    } catch (error) {
      console.error("Lỗi khi cập nhật trạng thái user:", error);
      setError(
        "Không thể cập nhật trạng thái người dùng. Vui lòng thử lại sau."
      );
    }
  };

  const filteredUsers = users.filter((user) => {
    const matchesStatus =
      filters.status === "all"
        ? true
        : filters.status === "active"
        ? !user.disabled
        : user.disabled;

    const matchesSearch =
      (user.email?.toLowerCase() || "").includes(
        filters.search.toLowerCase()
      ) ||
      (user.displayName?.toLowerCase() || "").includes(
        filters.search.toLowerCase()
      ) ||
      user.uid.toLowerCase().includes(filters.search.toLowerCase());

    return matchesStatus && matchesSearch;
  });

  const totalPages = Math.ceil(filteredUsers.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const paginatedUsers = filteredUsers.slice(
    startIndex,
    startIndex + itemsPerPage
  );

  const LoadingSpinner = () => (
    <div className="flex flex-col justify-center items-center min-h-[60vh]">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      <p className="mt-4 text-gray-600 font-medium">Đang tải dữ liệu...</p>
    </div>
  );

  const EmptyState = () => (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center bg-white rounded-lg shadow">
      <FiUser className="h-16 w-16 text-gray-400 mb-4" />
      <h3 className="text-lg font-medium text-gray-900">
        Không tìm thấy người dùng nào
      </h3>
      <p className="mt-1 text-sm text-gray-500">
        Không có người dùng nào phù hợp với bộ lọc của bạn.
      </p>
      <button
        onClick={() => setFilters({ status: "all", search: "" })}
        className="mt-6 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
      >
        <FiFilter className="mr-2" /> Xóa bộ lọc
      </button>
    </div>
  );

  if (loading) return <LoadingSpinner />;

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-5">
            <div className="flex items-center mb-4 sm:mb-0">
              <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center mr-3 shadow-sm">
                <FiUser className="text-blue-600 h-5 w-5" />
              </div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-900 leading-tight">
                Quản lý người dùng
              </h1>
            </div>

            <div className="inline-flex rounded-md shadow-sm">
              <button
                onClick={refreshUsers}
                disabled={isRefreshing}
                className="inline-flex items-center px-4 py-2.5 bg-white border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-all duration-200"
              >
                <FiRefreshCw
                  className={`mr-2 h-4 w-4 ${
                    isRefreshing ? "animate-spin" : ""
                  }`}
                />
                Làm mới dữ liệu
              </button>
            </div>
          </div>

          {users.length > 0 && (
            <div className="bg-blue-50 border-l-4 border-blue-400 p-4 rounded-md mb-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <FiAlertCircle
                    className="h-5 w-5 text-blue-400"
                    aria-hidden="true"
                  />
                </div>
                <div className="ml-3">
                  <p className="text-sm text-blue-700">
                    Hiện có <span className="font-medium">{users.length}</span>{" "}
                    người dùng trong hệ thống.
                    <span className="font-medium ml-1">
                      {users.filter((u) => !u.disabled).length} đang hoạt động
                    </span>{" "}
                    và
                    <span className="font-medium ml-1">
                      {users.filter((u) => u.disabled).length} đã bị vô hiệu hóa
                    </span>
                    .
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Error message */}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 rounded-md text-red-800 flex items-start">
            <FiAlertCircle className="h-5 w-5 mr-3 mt-0.5 text-red-500" />
            <div>
              <p className="font-medium">Đã xảy ra lỗi</p>
              <p className="text-sm mt-1">{error}</p>
            </div>
          </div>
        )}

        {/* Main content card */}
        <div className="bg-white rounded-xl shadow-md overflow-hidden border border-gray-200">
          {/* Filter bar */}
          <div className="p-5 border-b border-gray-200 bg-gray-50">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div className="relative flex-1">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <FiSearch className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="text"
                  value={filters.search}
                  onChange={(e) =>
                    setFilters((prev) => ({ ...prev, search: e.target.value }))
                  }
                  placeholder="Tìm kiếm theo email, tên người dùng..."
                  className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm transition-colors duration-200"
                />
              </div>

              <div className="inline-flex items-center">
                <label
                  htmlFor="status-filter"
                  className="mr-2 text-sm font-medium text-gray-700"
                >
                  Trạng thái:
                </label>
                <div className="relative">
                  <select
                    id="status-filter"
                    value={filters.status}
                    onChange={(e) =>
                      setFilters((prev) => ({
                        ...prev,
                        status: e.target.value as FilterState["status"],
                      }))
                    }
                    className="block w-full pl-3 pr-10 py-2.5 text-base border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-lg"
                  >
                    <option value="all">Tất cả</option>
                    <option value="active">Đang hoạt động</option>
                    <option value="disabled">Đã vô hiệu hóa</option>
                  </select>
                  <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-700">
                    <FiChevronDown className="h-4 w-4" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Table or empty state */}
          {paginatedUsers.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-1/6"
                    >
                      ID
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-1/4"
                    >
                      Email
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-1/4"
                    >
                      Tên hiển thị
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-1/6"
                    >
                      Trạng thái
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-1/6"
                    >
                      Thao tác
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {paginatedUsers.map((user, index) => (
                    <tr
                      key={user.uid}
                      className={`${
                        index % 2 === 0 ? "bg-white" : "bg-gray-50"
                      } hover:bg-blue-50 transition duration-150`}
                    >
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm font-medium text-gray-900 truncate max-w-[150px]">
                        <span className="font-mono">{user.uid}</span>
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-600 truncate max-w-[200px]">
                        {user.email}
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-600">
                        {user.displayName}
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${
                            user.disabled
                              ? "bg-red-100 text-red-800 border border-red-200"
                              : "bg-green-100 text-green-800 border border-green-200"
                          }`}
                        >
                          <span
                            className={`h-2 w-2 rounded-full mr-1.5 ${
                              user.disabled ? "bg-red-500" : "bg-green-500"
                            }`}
                          ></span>
                          {user.disabled ? "Vô hiệu hóa" : "Hoạt động"}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm">
                        <button
                          onClick={() =>
                            toggleUserStatus(user.uid, user.disabled)
                          }
                          className={`inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-medium rounded-md shadow-sm text-white ${
                            user.disabled
                              ? "bg-green-600 hover:bg-green-700"
                              : "bg-red-600 hover:bg-red-700"
                          } focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                            user.disabled
                              ? "focus:ring-green-500"
                              : "focus:ring-red-500"
                          } transition duration-150`}
                        >
                          {user.disabled ? "Kích hoạt" : "Vô hiệu hóa"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState />
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="bg-white px-4 py-4 flex items-center justify-between border-t border-gray-200 sm:px-6">
              <div className="flex-1 flex justify-between sm:hidden">
                <button
                  onClick={() =>
                    setCurrentPage((prev) => Math.max(prev - 1, 1))
                  }
                  disabled={currentPage === 1}
                  className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <FiChevronLeft className="mr-1 h-4 w-4" />
                  Trước
                </button>
                <div className="mx-2 flex items-center">
                  <span className="text-sm text-gray-700">
                    {currentPage} / {totalPages}
                  </span>
                </div>
                <button
                  onClick={() =>
                    setCurrentPage((prev) => Math.min(prev + 1, totalPages))
                  }
                  disabled={currentPage === totalPages}
                  className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Sau
                  <FiChevronRight className="ml-1 h-4 w-4" />
                </button>
              </div>
              <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700">
                    Hiển thị{" "}
                    <span className="font-medium">{startIndex + 1}</span> đến{" "}
                    <span className="font-medium">
                      {Math.min(
                        startIndex + itemsPerPage,
                        filteredUsers.length
                      )}
                    </span>{" "}
                    trong tổng số{" "}
                    <span className="font-medium">{filteredUsers.length}</span>{" "}
                    người dùng
                  </p>
                </div>
                <div>
                  <nav
                    className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px"
                    aria-label="Pagination"
                  >
                    <button
                      onClick={() =>
                        setCurrentPage((prev) => Math.max(prev - 1, 1))
                      }
                      disabled={currentPage === 1}
                      className="relative inline-flex items-center px-3 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <span className="sr-only">Trang trước</span>
                      <FiChevronLeft className="h-5 w-5" />
                    </button>

                    {/* Show limited page numbers for better UX */}
                    {[...Array(totalPages)].map((_, i) => {
                      // Show first page, last page, and pages around current page
                      const pageNum = i + 1;
                      const showPage =
                        pageNum === 1 ||
                        pageNum === totalPages ||
                        Math.abs(pageNum - currentPage) <= 1;

                      // Show ellipsis for page gaps
                      const showEllipsisBefore = i === 1 && currentPage - 1 > 2;
                      const showEllipsisAfter =
                        i === totalPages - 2 &&
                        currentPage + 1 < totalPages - 1;

                      if (showEllipsisBefore) {
                        return (
                          <span
                            key="ellipsis-before"
                            className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700"
                          >
                            ...
                          </span>
                        );
                      } else if (showEllipsisAfter) {
                        return (
                          <span
                            key="ellipsis-after"
                            className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700"
                          >
                            ...
                          </span>
                        );
                      } else if (showPage) {
                        return (
                          <button
                            key={pageNum}
                            onClick={() => setCurrentPage(pageNum)}
                            className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                              currentPage === pageNum
                                ? "z-10 bg-blue-50 border-blue-500 text-blue-600"
                                : "bg-white border-gray-300 text-gray-500 hover:bg-gray-50"
                            }`}
                          >
                            {pageNum}
                          </button>
                        );
                      }

                      return null;
                    })}

                    <button
                      onClick={() =>
                        setCurrentPage((prev) => Math.min(prev + 1, totalPages))
                      }
                      disabled={currentPage === totalPages}
                      className="relative inline-flex items-center px-3 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
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

// Added FiChevronDown component for select dropdown
function FiChevronDown(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...props}
    >
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}
