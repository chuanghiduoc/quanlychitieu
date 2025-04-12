import { NextResponse } from "next/server";
import { firebaseAdmin } from "@/config/firebase";
import { Budget } from "@/types/models";

const db = firebaseAdmin.firestore();

export async function GET() {
  try {
    const usersSnapshot = await db.collection("users").get();
    const allBudgets: Budget[] = [];

    // Process each user's budgets
    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      const budgetsSnapshot = await db
        .collection("users")
        .doc(userId)
        .collection("budgets")
        .get();

      const userBudgets: Budget[] = budgetsSnapshot.docs.map(
        (doc) =>
          ({
            firebaseId: doc.id,
            ...doc.data(),
            userId,
            startDate: doc.data().startDate.toDate(),
            endDate: doc.data().endDate.toDate(),
          } as Budget)
      );

      allBudgets.push(...userBudgets);
    }

    // Sort by start date (newest first)
    allBudgets.sort((a, b) => b.startDate.getTime() - a.startDate.getTime());

    return NextResponse.json(allBudgets);
  } catch (error) {
    console.error("Lỗi khi lấy tất cả budgets:", error);
    return NextResponse.json(
      { error: "Internal Server Error" },
      { status: 500 }
    );
  }
}
