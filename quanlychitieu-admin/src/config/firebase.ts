import admin from 'firebase-admin';
import serviceAccount from '../../quanlychitieu-586c9-firebase-adminsdk.json';

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount as admin.ServiceAccount)
  });
}

export const firebaseAdmin = admin;