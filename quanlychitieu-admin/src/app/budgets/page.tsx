"use client";

import { useState, useEffect } from "react";
import { Budget } from "@/types/models";
import { getAllBudgets } from "@/services/firestore";
import {
  FiSearch,
  FiRefreshCw,
  FiChevronLeft,
  FiChevronRight,
  FiPieChart,
  FiFilter,
  FiAlertCircle,
  FiCalendar,
  FiUser,
} from "react-icons/fi";
import { format } from "date-fns";

interface FilterState {
  category: string;
  search: string;
  dateRange: {
    start: Date | null;
    end: Date | null;
  };
  userId: string; // Thêm userId vào đây
}

interface User {
  uid: string;
  displayName: string | null;
  email: string | null;
  disabled: boolean;
}

export default function BudgetsPage() {
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<FilterState>({
    category: "",
    search: "",
    dateRange: {
      start: null,
      end: null,
    },
    userId: "", // Mặc định là rỗng, nghĩa là hiển thị tất cả
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const itemsPerPage = 20;

  useEffect(() => {
    fetchBudgets();
    fetchUsers();
  }, []);

  useEffect(() => {
    // Reset to first page when filters change
    setCurrentPage(1);
  }, [filters]);

  const fetchBudgets = async () => {
    setError(null);
    setLoading(true);
    try {
      const budgetsData = await getAllBudgets();
      setBudgets(budgetsData);
    } catch (error) {
      console.error("Lỗi khi lấy danh sách ngân sách:", error);
      setError("Không thể tải danh sách ngân sách. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  };

  const fetchUsers = async () => {
    try {
      const response = await fetch("/api/users");
      if (!response.ok) {
        throw new Error("Lỗi khi lấy danh sách người dùng");
      }
      const { users: userData } = await response.json();
      setUsers(userData);
    } catch (error) {
      console.error("Lỗi khi lấy danh sách người dùng:", error);
    }
  };

  const refreshBudgets = async () => {
    setIsRefreshing(true);
    await fetchBudgets();
    setIsRefreshing(false);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  };

  const getUniqueCategories = () => {
    const categories = new Set<string>();
    budgets.forEach((budget) => categories.add(budget.category));
    return Array.from(categories).sort();
  };

  const filteredBudgets = budgets.filter((budget) => {
    const matchesCategory =
      filters.category === "" || budget.category === filters.category;

    const matchesSearch =
      budget.category.toLowerCase().includes(filters.search.toLowerCase()) ||
      (budget.note?.toLowerCase() || "").includes(filters.search.toLowerCase());

    let matchesDateRange = true;
    if (filters.dateRange.start) {
      matchesDateRange =
        matchesDateRange &&
        (budget.startDate >= filters.dateRange.start ||
          budget.endDate >= filters.dateRange.start);
    }
    if (filters.dateRange.end) {
      matchesDateRange =
        matchesDateRange &&
        (budget.startDate <= filters.dateRange.end ||
          budget.endDate <= filters.dateRange.end);
    }

    // Thêm điều kiện lọc theo userId
    const matchesUser =
      filters.userId === "" || budget.userId === filters.userId;

    return matchesCategory && matchesSearch && matchesDateRange && matchesUser;
  });

  const totalPages = Math.ceil(filteredBudgets.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const paginatedBudgets = filteredBudgets.slice(
    startIndex,
    startIndex + itemsPerPage
  );

  // Calculate summary data
  const totalBudgetAmount = filteredBudgets.reduce(
    (sum, b) => sum + b.amount,
    0
  );
  const totalBudgetSpent = filteredBudgets.reduce((sum, b) => sum + b.spent, 0);
  const totalBudgetRemaining = totalBudgetAmount - totalBudgetSpent;
  const averagePercentUsed = filteredBudgets.length
    ? filteredBudgets.reduce((sum, b) => sum + (b.spent / b.amount) * 100, 0) /
      filteredBudgets.length
    : 0;

  // Tìm thông tin người dùng được chọn để hiển thị
  const selectedUser = filters.userId
    ? users.find((user) => user.uid === filters.userId)
    : null;

  const LoadingSpinner = () => (
    <div className="flex flex-col justify-center items-center min-h-[60vh]">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      <p className="mt-4 text-gray-600 font-medium">Đang tải dữ liệu...</p>
    </div>
  );

  const EmptyState = () => (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center bg-white rounded-lg shadow">
      <FiPieChart className="h-16 w-16 text-gray-400 mb-4" />
      <h3 className="text-lg font-medium text-gray-900">
        Không tìm thấy ngân sách nào
      </h3>
      <p className="mt-1 text-sm text-gray-500">
        Không có ngân sách nào phù hợp với bộ lọc của bạn.
      </p>
      <button
        onClick={() =>
          setFilters({
            category: "",
            search: "",
            dateRange: { start: null, end: null },
            userId: "",
          })
        }
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
                <FiPieChart className="text-blue-600 h-5 w-5" />
              </div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-900 leading-tight">
                {filters.userId
                  ? `Ngân sách của ${
                      selectedUser?.displayName || filters.userId
                    }`
                  : "Tất cả ngân sách"}
              </h1>
            </div>

            <div className="inline-flex rounded-md shadow-sm">
              <button
                onClick={refreshBudgets}
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

          {budgets.length > 0 && (
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
                    Hiện có{" "}
                    <span className="font-medium">{budgets.length}</span> ngân
                    sách trong hệ thống.
                    {filteredBudgets.length !== budgets.length && (
                      <span className="ml-1">
                        Đang hiển thị{" "}
                        <span className="font-medium">
                          {filteredBudgets.length}
                        </span>{" "}
                        ngân sách theo bộ lọc.
                      </span>
                    )}
                    {filters.userId && (
                      <span className="ml-1">
                        Đang xem ngân sách của người dùng:{" "}
                        <span className="font-medium">
                          {selectedUser?.displayName || filters.userId}
                        </span>
                      </span>
                    )}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Summary cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                  <FiPieChart className="h-5 w-5 text-blue-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">
                    Tổng ngân sách
                  </h2>
                  <p className="text-lg font-semibold text-blue-700">
                    {formatCurrency(totalBudgetAmount)}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-red-100 flex items-center justify-center">
                  <FiPieChart className="h-5 w-5 text-red-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">
                    Đã chi tiêu
                  </h2>
                  <p className="text-lg font-semibold text-red-600">
                    {formatCurrency(totalBudgetSpent)}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-green-100 flex items-center justify-center">
                  <FiPieChart className="h-5 w-5 text-green-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">Còn lại</h2>
                  <p className="text-lg font-semibold text-green-600">
                    {formatCurrency(totalBudgetRemaining)}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-yellow-100 flex items-center justify-center">
                  <FiPieChart className="h-5 w-5 text-yellow-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">
                    Trung bình % đã dùng
                  </h2>
                  <p className="text-lg font-semibold text-yellow-700">
                    {averagePercentUsed.toFixed(1)}%
                  </p>
                </div>
              </div>
            </div>
          </div>
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
            <div className="flex flex-col space-y-4">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div className="relative flex-1">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <FiSearch className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    type="text"
                    value={filters.search}
                    onChange={(e) =>
                      setFilters((prev) => ({
                        ...prev,
                        search: e.target.value,
                      }))
                    }
                    placeholder="Tìm kiếm theo ghi chú, danh mục..."
                    className="block w-full pl-10 pr-3 py-2.5 border border-gray-300 rounded-lg leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm transition-colors duration-200"
                  />
                </div>

                {/* Dropdown cho người dùng */}
                <div className="inline-flex items-center">
                  <label
                    htmlFor="user-filter"
                    className="mr-2 text-sm font-medium text-gray-700 whitespace-nowrap"
                  >
                    <FiUser className="inline mr-1" /> Người dùng:
                  </label>
                  <select
                    id="user-filter"
                    value={filters.userId}
                    onChange={(e) =>
                      setFilters((prev) => ({
                        ...prev,
                        userId: e.target.value,
                      }))
                    }
                    className="block w-full pl-3 pr-10 py-2.5 text-base border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-lg"
                  >
                    <option value="">Tất cả người dùng</option>
                    {users.map((user) => (
                      <option key={user.uid} value={user.uid}>
                        {user.displayName || user.email || user.uid}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="inline-flex items-center">
                  <label
                    htmlFor="category-filter"
                    className="mr-2 text-sm font-medium text-gray-700"
                  >
                    Danh mục:
                  </label>
                  <select
                    id="category-filter"
                    value={filters.category}
                    onChange={(e) =>
                      setFilters((prev) => ({
                        ...prev,
                        category: e.target.value,
                      }))
                    }
                    className="block w-full pl-3 pr-10 py-2.5 text-base border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-lg"
                  >
                    <option value="">Tất cả</option>
                    {getUniqueCategories().map((category) => (
                      <option key={category} value={category}>
                        {category}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center gap-4">
                <div className="flex items-center">
                  <label
                    htmlFor="start-date"
                    className="block text-sm font-medium text-gray-700 mr-2"
                  >
                    Từ ngày:
                  </label>
                  <input
                    type="date"
                    id="start-date"
                    onChange={(e) => {
                      const date = e.target.value
                        ? new Date(e.target.value)
                        : null;
                      setFilters((prev) => ({
                        ...prev,
                        dateRange: {
                          ...prev.dateRange,
                          start: date,
                        },
                      }));
                    }}
                    className="block w-full pl-3 pr-3 py-2 border border-gray-300 rounded-lg leading-5 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  />
                </div>

                <div className="flex items-center">
                  <label
                    htmlFor="end-date"
                    className="block text-sm font-medium text-gray-700 mr-2"
                  >
                    Đến ngày:
                  </label>
                  <input
                    type="date"
                    id="end-date"
                    onChange={(e) => {
                      const date = e.target.value
                        ? new Date(e.target.value)
                        : null;
                      if (date) {
                        // Set to end of day
                        date.setHours(23, 59, 59, 999);
                      }
                      setFilters((prev) => ({
                        ...prev,
                        dateRange: {
                          ...prev.dateRange,
                          end: date,
                        },
                      }));
                    }}
                    className="block w-full pl-3 pr-3 py-2 border border-gray-300 rounded-lg leading-5 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                  />
                </div>

                <button
                  onClick={() =>
                    setFilters({
                      category: "",
                      search: "",
                      dateRange: { start: null, end: null },
                      userId: "",
                    })
                  }
                  className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                  <FiFilter className="mr-2 h-4 w-4" />
                  Xóa bộ lọc
                </button>
              </div>
            </div>
          </div>

          {/* Table or empty state */}
          {paginatedBudgets.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Danh mục
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Thời gian
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Ngân sách
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Đã chi
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Còn lại
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Tiến độ
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Người dùng
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {paginatedBudgets.map((budget, index) => {
                    const percentUsed = (budget.spent / budget.amount) * 100;
                    const isOverBudget = percentUsed > 100;
                    const percentRemaining = 100 - percentUsed;

                    return (
                      <tr
                        key={budget.firebaseId || budget.id}
                        className={`${
                          index % 2 === 0 ? "bg-white" : "bg-gray-50"
                        } hover:bg-blue-50 transition duration-150`}
                      >
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm font-medium text-gray-900">
                          {budget.category}
                        </td>
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-600">
                          <div className="flex flex-col">
                            <div className="flex items-center">
                              <FiCalendar className="mr-2 h-4 w-4 text-gray-500" />
                              {format(budget.startDate, "dd/MM/yyyy")}
                            </div>
                            <div className="text-xs text-gray-500 mt-1">
                              đến {format(budget.endDate, "dd/MM/yyyy")}
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm font-medium text-blue-700">
                          {formatCurrency(budget.amount)}
                        </td>
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm font-medium text-red-600">
                          {formatCurrency(budget.spent)}
                        </td>
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm font-medium text-green-600">
                          {formatCurrency(budget.amount - budget.spent)}
                        </td>
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm">
                          <div className="w-full bg-gray-200 rounded-full h-2.5 mb-1">
                            <div
                              className={`h-2.5 rounded-full ${
                                isOverBudget ? "bg-red-600" : "bg-green-600"
                              }`}
                              style={{
                                width: `${Math.min(percentUsed, 100)}%`,
                              }}
                            ></div>
                          </div>
                          <div className="flex justify-between text-xs">
                            <span
                              className={`${
                                isOverBudget
                                  ? "text-red-600 font-medium"
                                  : "text-gray-600"
                              }`}
                            >
                              {percentUsed.toFixed(1)}%
                            </span>
                            {!isOverBudget && (
                              <span className="text-gray-600">
                                {percentRemaining.toFixed(1)}% còn lại
                              </span>
                            )}
                            {isOverBudget && (
                              <span className="text-red-600 font-medium">
                                Vượt ngân sách!
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-600">
                          {users.find((u) => u.uid === budget.userId)
                            ?.displayName || budget.userId}
                        </td>
                      </tr>
                    );
                  })}
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
                        filteredBudgets.length
                      )}
                    </span>{" "}
                    trong tổng số{" "}
                    <span className="font-medium">
                      {filteredBudgets.length}
                    </span>{" "}
                    ngân sách
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
                      const pageNum = i + 1;
                      const showPage =
                        pageNum === 1 ||
                        pageNum === totalPages ||
                        Math.abs(pageNum - currentPage) <= 1;

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
