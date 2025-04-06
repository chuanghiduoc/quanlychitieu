export interface Budget {
  firebaseId?: string;
  id: number;
  userId: string;
  category: string;
  amount: number;
  spent: number;
  startDate: Date;
  endDate: Date;
  note?: string;
  notificationsEnabled: boolean;
  notificationThreshold: number;
  notificationSent: boolean;
}

export interface Transaction {
  firebaseId?: string;
  id: number;
  description: string;
  amount: number;
  category: string;
  date: Date;
  isIncome: boolean;
  note?: string;
  repeat: boolean;
  userId: string;
  repeatType?: 'daily' | 'weekly' | 'monthly' | 'yearly';
  endDate?: Date;
}

export interface FinancialGoal {
  firebaseId?: string;
  id: number;
  userId: string;
  name: string;
  description: string;
  targetAmount: number;
  currentAmount: number;
  startDate: Date;
  endDate: Date;
  category: string;
  completed: boolean;
}