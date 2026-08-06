import React, { createContext, useContext, useState, useEffect } from 'react';
import { 
  auth, 
  rtdb,
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  sendEmailVerification,
  firebaseSignOut,
  ref,
  get,
  set,
  child
} from '../firebase/config';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (user) {
        console.log("[AUTH] Web Auth state changed: Logged in UID =", user.uid);
        let profile = {
          uid: user.uid,
          email: user.email,
          emailVerified: user.emailVerified,
          fullName: user.displayName || user.email?.split('@')[0] || 'Doctor',
          accountStatus: 'active'
        };

        try {
          const dbRef = ref(rtdb);
          const snapshot = await get(child(dbRef, `Users/${user.uid}`));
          if (snapshot.exists()) {
            const data = snapshot.val();
            profile.fullName = data.fullName || profile.fullName;
            profile.accountStatus = data.accountStatus || 'active';
          }
        } catch (e) {
          console.warn("[AUTH] Could not fetch user profile from Realtime DB:", e);
        }

        setCurrentUser(profile);
      } else {
        console.log("[AUTH] Web Auth state changed: No authenticated user session");
        setCurrentUser(null);
      }
      setLoading(false);
    });

    return unsubscribe;
  }, []);

  const login = async (email, password) => {
    const result = await signInWithEmailAndPassword(auth, email, password);
    return result.user;
  };

  const register = async (fullName, email, password) => {
    const res = await createUserWithEmailAndPassword(auth, email, password);
    const user = res.user;

    // Send email verification link
    try {
      await sendEmailVerification(user);
      console.log("[AUTH] Verification email sent to:", email);
    } catch (e) {
      console.warn("[AUTH] Could not send verification email:", e);
    }

    try {
      await set(ref(rtdb, `Users/${user.uid}`), {
        uid: user.uid,
        email: email,
        fullName: fullName || email.split('@')[0],
        provider: 'email',
        accountStatus: 'active',
        createdAt: Date.now()
      });
      await set(ref(rtdb, `doctors/${user.uid}`), {
        uid: user.uid,
        email: email,
        fullName: fullName || email.split('@')[0],
        provider: 'email',
        accountStatus: 'active',
        createdAt: Date.now()
      });
    } catch (e) {
      console.error("Error saving user profile to Realtime DB:", e);
    }

    return user;
  };

  const logout = async () => {
    try {
      await firebaseSignOut(auth);
      setCurrentUser(null);
      console.log("[AUTH] Web Auth logged out successfully");
    } catch (e) {
      console.error("[AUTH] Logout error:", e);
    }
  };

  return (
    <AuthContext.Provider value={{
      currentUser,
      loading,
      login,
      register,
      logout
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
