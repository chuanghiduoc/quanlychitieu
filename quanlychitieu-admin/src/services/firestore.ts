import { firebaseAdmin } from '@/config/firebase';
import { Budget, Transaction, FinancialGoal } from '@/types/models';

const db = firebaseAdmin.firestore();

export async function getUserFinancialData(userId: string) {
  try {
    // Lấy transactions
    const transactionsSnapshot = await db
      .collection('users')
      .doc(userId)
      .collection('transactions')
      .get();
    
    const transactions: Transaction[] = transactionsSnapshot.docs.map(doc => ({
      firebaseId: doc.id,
      ...doc.data(),
      date: doc.data().date.toDate(),
      endDate: doc.data().endDate?.toDate(),
    } as Transaction));

    // Lấy budgets
    const budgetsSnapshot = await db
      .collection('users')
      .doc(userId)
      .collection('budgets')
      .get();
    
    const budgets: Budget[] = budgetsSnapshot.docs.map(doc => ({
      firebaseId: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate.toDate(),
      endDate: doc.data().endDate.toDate(),
    } as Budget));

    // Lấy goals
    const goalsSnapshot = await db
      .collection('users')
      .doc(userId)
      .collection('goals')
      .get();
    
    const goals: FinancialGoal[] = goalsSnapshot.docs.map(doc => ({
      firebaseId: doc.id,
      ...doc.data(),
      startDate: doc.data().startDate.toDate(),
      endDate: doc.data().endDate.toDate(),
    } as FinancialGoal));

    // Phân tích dữ liệu cơ bản
    const totalIncome = transactions
      .filter(t => t.isIncome)
      .reduce((sum, t) => sum + t.amount, 0);
    
    const totalExpense = transactions
      .filter(t => !t.isIncome)
      .reduce((sum, t) => sum + t.amount, 0);
    
    const categoryExpenses = transactions
      .filter(t => !t.isIncome)
      .reduce((acc, t) => {
        acc[t.category] = (acc[t.category] || 0) + t.amount;
        return acc;
      }, {} as { [key: string]: number });

    const budgetStatus = budgets.map(b => ({
      category: b.category,
      amount: b.amount,
      spent: b.spent,
      remaining: b.amount - b.spent,
      percentageUsed: (b.spent / b.amount) * 100
    }));

    return {
      transactions,
      budgets,
      goals,
      analysis: {
        totalIncome,
        totalExpense,
        netIncome: totalIncome - totalExpense,
        categoryExpenses,
        budgetStatus
      }
    };
  } catch (error) {
    console.error('Lỗi khi lấy dữ liệu tài chính:', error);
    throw error;
  }
}