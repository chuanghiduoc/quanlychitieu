import { NextResponse } from "next/server";
import { firebaseAdmin } from "@/config/firebase";
import { Transaction } from "@/types/models";

const db = firebaseAdmin.firestore();

export async function GET() {
  try {
    const usersSnapshot = await db.collection("users").get();
    const allTransactions: Transaction[] = [];

    // Process each user's transactions
    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      const transactionsSnapshot = await db
        .collection("users")
        .doc(userId)
        .collection("transactions")
        .get();

      const userTransactions: Transaction[] = transactionsSnapshot.docs.map(
        (doc) =>
          ({
            firebaseId: doc.id,
            ...doc.data(),
            userId,
            date: doc.data().date.toDate(),
            endDate: doc.data().endDate?.toDate(),
          } as Transaction)
      );

      allTransactions.push(...userTransactions);
    }

    // Sort by date (newest first)
    allTransactions.sort((a, b) => b.date.getTime() - a.date.getTime());

    return NextResponse.json(allTransactions);
  } catch (error) {
    console.error("Lỗi khi lấy tất cả transactions:", error);
    return NextResponse.json(
      { error: "Internal Server Error" },
      { status: 500 }
    );
  }
}
