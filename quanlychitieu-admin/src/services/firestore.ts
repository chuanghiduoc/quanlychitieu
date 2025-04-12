import { Budget, Transaction, FinancialGoal } from "@/types/models";

interface FinancialData {
  transactions: Transaction[];
  budgets: Budget[];
  goals: FinancialGoal[];
  analysis: {
    totalIncome: number;
    totalExpense: number;
    netIncome: number;
    categoryExpenses: { [key: string]: number };
    budgetStatus: {
      category: string;
      amount: number;
      spent: number;
      remaining: number;
      percentageUsed: number;
    }[];
  };
}

export async function getUserFinancialData(
  userId: string
): Promise<FinancialData> {
  try {
    const response = await fetch(`/api/financial-data?userId=${userId}`);
    if (!response.ok) {
      throw new Error("Lỗi khi lấy dữ liệu tài chính");
    }
    return await response.json();
  } catch (error) {
    console.error("Lỗi khi lấy dữ liệu tài chính:", error);
    throw error;
  }
}

export async function getAllTransactions(): Promise<Transaction[]> {
  try {
    const response = await fetch("/api/transactions");
    if (!response.ok) {
      throw new Error("Lỗi khi lấy tất cả transactions");
    }
    return await response.json();
  } catch (error) {
    console.error("Lỗi khi lấy tất cả transactions:", error);
    throw error;
  }
}

export async function getAllBudgets(): Promise<Budget[]> {
  try {
    const response = await fetch("/api/budgets");
    if (!response.ok) {
      throw new Error("Lỗi khi lấy tất cả budgets");
    }
    return await response.json();
  } catch (error) {
    console.error("Lỗi khi lấy tất cả budgets:", error);
    throw error;
  }
}
