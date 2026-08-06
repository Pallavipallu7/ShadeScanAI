import { initializeApp, getApps } from 'firebase/app';
import { 
  getAuth, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  sendEmailVerification,
  signOut as firebaseSignOut,
  sendPasswordResetEmail,
  updatePassword,
  fetchSignInMethodsForEmail
} from 'firebase/auth';
import { 
  getDatabase, 
  ref, 
  get, 
  set, 
  push, 
  update, 
  remove, 
  child, 
  onValue 
} from 'firebase/database';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyCUuhDXnb9T2fUQHhdTQytXTMmmbQIF3w4",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "shadescan-ai.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "shadescan-ai",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "shadescan-ai.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "543866248276",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:543866248276:web:a6748e179a7068fe53e1c1",
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL || "https://shadescan-ai-default-rtdb.firebaseio.com/"
};

const app = !getApps().length ? initializeApp(firebaseConfig) : getApps()[0];
const auth = getAuth(app);
const rtdb = getDatabase(app, firebaseConfig.databaseURL);

export { 
  app, 
  auth, 
  rtdb, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  sendEmailVerification,
  firebaseSignOut, 
  sendPasswordResetEmail,
  updatePassword,
  fetchSignInMethodsForEmail,
  ref, 
  get, 
  set, 
  push, 
  update, 
  remove, 
  child, 
  onValue 
};
