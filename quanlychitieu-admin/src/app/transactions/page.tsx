"use client";

import { useState, useEffect } from "react";
import { Transaction } from "@/types/models";
import { getAllTransactions } from "@/services/firestore";
import {
  FiSearch,
  FiRefreshCw,
  FiChevronLeft,
  FiChevronRight,
  FiDollarSign,
  FiFilter,
  FiAlertCircle,
  FiCalendar,
  FiUser,
} from "react-icons/fi";
import { format } from "date-fns";

interface FilterState {
  type: "all" | "income" | "expense";
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

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<FilterState>({
    type: "all",
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
    fetchTransactions();
    fetchUsers();
  }, []);

  useEffect(() => {
    // Reset to first page when filters change
    setCurrentPage(1);
  }, [filters]);

  const fetchTransactions = async () => {
    setError(null);
    setLoading(true);
    try {
      const transactionsData = await getAllTransactions();
      setTransactions(transactionsData);
    } catch (error) {
      console.error("Lỗi khi lấy danh sách giao dịch:", error);
      setError("Không thể tải danh sách giao dịch. Vui lòng thử lại sau.");
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

  const refreshTransactions = async () => {
    setIsRefreshing(true);
    await fetchTransactions();
    setIsRefreshing(false);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  };

  const filteredTransactions = transactions.filter((transaction) => {
    const matchesType =
      filters.type === "all"
        ? true
        : filters.type === "income"
        ? transaction.isIncome
        : !transaction.isIncome;

    const matchesSearch =
      transaction.description
        .toLowerCase()
        .includes(filters.search.toLowerCase()) ||
      transaction.category
        .toLowerCase()
        .includes(filters.search.toLowerCase()) ||
      (transaction.note?.toLowerCase() || "").includes(
        filters.search.toLowerCase()
      );

    let matchesDateRange = true;
    if (filters.dateRange.start) {
      matchesDateRange =
        matchesDateRange && transaction.date >= filters.dateRange.start;
    }
    if (filters.dateRange.end) {
      matchesDateRange =
        matchesDateRange && transaction.date <= filters.dateRange.end;
    }

    // Thêm điều kiện lọc theo userId
    const matchesUser =
      filters.userId === "" || transaction.userId === filters.userId;

    return matchesType && matchesSearch && matchesDateRange && matchesUser;
  });

  const totalPages = Math.ceil(filteredTransactions.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const paginatedTransactions = filteredTransactions.slice(
    startIndex,
    startIndex + itemsPerPage
  );

  // Calculate summary data
  const totalIncome = filteredTransactions
    .filter((t) => t.isIncome)
    .reduce((sum, t) => sum + t.amount, 0);

  const totalExpense = filteredTransactions
    .filter((t) => !t.isIncome)
    .reduce((sum, t) => sum + t.amount, 0);

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
      <FiDollarSign className="h-16 w-16 text-gray-400 mb-4" />
      <h3 className="text-lg font-medium text-gray-900">
        Không tìm thấy giao dịch nào
      </h3>
      <p className="mt-1 text-sm text-gray-500">
        Không có giao dịch nào phù hợp với bộ lọc của bạn.
      </p>
      <button
        onClick={() =>
          setFilters({
            type: "all",
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
                <FiDollarSign className="text-blue-600 h-5 w-5" />
              </div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-900 leading-tight">
                {filters.userId
                  ? `Giao dịch của ${
                      selectedUser?.displayName || filters.userId
                    }`
                  : "Tất cả giao dịch"}
              </h1>
            </div>

            <div className="inline-flex rounded-md shadow-sm">
              <button
                onClick={refreshTransactions}
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

          {transactions.length > 0 && (
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
                    <span className="font-medium">{transactions.length}</span>{" "}
                    giao dịch trong hệ thống.
                    {filteredTransactions.length !== transactions.length && (
                      <span className="ml-1">
                        Đang hiển thị{" "}
                        <span className="font-medium">
                          {filteredTransactions.length}
                        </span>{" "}
                        giao dịch theo bộ lọc.
                      </span>
                    )}
                    {filters.userId && (
                      <span className="ml-1">
                        Đang xem giao dịch của người dùng:{" "}
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
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                  <FiDollarSign className="h-5 w-5 text-blue-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">
                    Tổng giao dịch
                  </h2>
                  <p className="text-lg font-semibold text-gray-900">
                    {filteredTransactions.length}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-green-100 flex items-center justify-center">
                  <FiDollarSign className="h-5 w-5 text-green-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">
                    Tổng thu
                  </h2>
                  <p className="text-lg font-semibold text-green-600">
                    {formatCurrency(totalIncome)}
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
              <div className="flex items-center">
                <div className="flex-shrink-0 h-10 w-10 rounded-full bg-red-100 flex items-center justify-center">
                  <FiDollarSign className="h-5 w-5 text-red-600" />
                </div>
                <div className="ml-4">
                  <h2 className="text-sm font-medium text-gray-500">
                    Tổng chi
                  </h2>
                  <p className="text-lg font-semibold text-red-600">
                    {formatCurrency(totalExpense)}
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
                    placeholder="Tìm kiếm theo mô tả, danh mục..."
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
                    htmlFor="type-filter"
                    className="mr-2 text-sm font-medium text-gray-700"
                  >
                    Loại giao dịch:
                  </label>
                  <select
                    id="type-filter"
                    value={filters.type}
                    onChange={(e) =>
                      setFilters((prev) => ({
                        ...prev,
                        type: e.target.value as FilterState["type"],
                      }))
                    }
                    className="block w-full pl-3 pr-10 py-2.5 text-base border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-lg"
                  >
                    <option value="all">Tất cả</option>
                    <option value="income">Khoản thu</option>
                    <option value="expense">Khoản chi</option>
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
                      type: "all",
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
          {paginatedTransactions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Ngày
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Mô tả
                    </th>
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
                      Số tiền
                    </th>
                    <th
                      scope="col"
                      className="px-4 py-3.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      Loại
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
                  {paginatedTransactions.map((transaction, index) => (
                    <tr
                      key={transaction.firebaseId || transaction.id}
                      className={`${
                        index % 2 === 0 ? "bg-white" : "bg-gray-50"
                      } hover:bg-blue-50 transition duration-150`}
                    >
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-900">
                        <div className="flex items-center">
                          <FiCalendar className="mr-2 h-4 w-4 text-gray-500" />
                          {format(transaction.date, "dd/MM/yyyy")}
                        </div>
                      </td>
                      <td className="px-4 py-3.5 text-sm text-gray-900 max-w-[200px] truncate">
                        {transaction.description}
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-600">
                        {transaction.category}
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm font-medium">
                        <span
                          className={
                            transaction.isIncome
                              ? "text-green-600"
                              : "text-red-600"
                          }
                        >
                          {formatCurrency(transaction.amount)}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${
                            transaction.isIncome
                              ? "bg-green-100 text-green-800 border border-green-200"
                              : "bg-red-100 text-red-800 border border-red-200"
                          }`}
                        >
                          {transaction.isIncome ? "Khoản thu" : "Khoản chi"}
                        </span>
                      </td>
                      <td className="px-4 py-3.5 whitespace-nowrap text-sm text-gray-600">
                        {users.find((u) => u.uid === transaction.userId)
                          ?.displayName || transaction.userId}
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
                        filteredTransactions.length
                      )}
                    </span>{" "}
                    trong tổng số{" "}
                    <span className="font-medium">
                      {filteredTransactions.length}
                    </span>{" "}
                    giao dịch
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
