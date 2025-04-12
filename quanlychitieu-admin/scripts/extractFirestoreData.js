import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { readFileSync, writeFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Initialize Firebase Admin
const serviceAccount = JSON.parse(
  readFileSync(join(__dirname, '../quanlychitieu-586c9-firebase-adminsdk.json'))
);

initializeApp({
  credential: cert(serviceAccount)
});

const db = getFirestore();

async function extractSubcollectionData(userId, subcollectionName) {
  try {
    const snapshot = await db
      .collection('users')
      .doc(userId)
      .collection(subcollectionName)
      .get();
    
    const data = [];
    snapshot.forEach(doc => {
      data.push({
        id: doc.id,
        ...doc.data()
      });
    });
    return data;
  } catch (error) {
    console.error(`Error extracting ${subcollectionName} for user ${userId}:`, error);
    return [];
  }
}

async function extractUserData() {
  try {
    const usersSnapshot = await db.collection('users').get();
    const usersData = [];

    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      const userData = {
        id: userId,
        ...userDoc.data(),
        budgets: await extractSubcollectionData(userId, 'budgets'),
        categories: await extractSubcollectionData(userId, 'categories'),
        goals: await extractSubcollectionData(userId, 'goals'),
        reminders: await extractSubcollectionData(userId, 'reminders'),
        transactions: await extractSubcollectionData(userId, 'transactions')
      };

      // Extract user_categories from categories subcollection
      for (const category of userData.categories) {
        category.user_categories = await extractSubcollectionData(userId, `categories/${category.id}/user_categories`);
      }

      usersData.push(userData);
    }

    // Save to JSON file
    const fileName = `users_data_${Date.now()}.json`;
    writeFileSync(fileName, JSON.stringify(usersData, null, 2));
    console.log(`Data extracted successfully to ${fileName}`);
    
    return usersData;
  } catch (error) {
    console.error('Error extracting user data:', error);
    throw error;
  }
}

async function main() {
  try {
    await extractUserData();
    process.exit(0);
  } catch (error) {
    console.error('Error in main:', error);
    process.exit(1);
  }
}

main(); 