import React, { createContext, useContext, useState, useEffect } from 'react';
import { 
  auth, 
  rtdb,
  googleProvider,
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  firebaseSignOut,
  signInWithPopup,
  ref,
  get,
  set,
  update,
  child
} from '../firebase/config';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (user) {
        let profile = {
          uid: user.uid,
          email: user.email,
          fullName: user.displayName || user.email?.split('@')[0] || 'Doctor',
          age: '',
          gender: 'Male',
          username: user.email?.split('@')[0] || '',
          mobile: ''
        };

        // Fetch user profile from Firebase Realtime DB node "Users/{uid}"
        try {
          const dbRef = ref(rtdb);
          const snapshot = await get(child(dbRef, `Users/${user.uid}`));
          if (snapshot.exists()) {
            const data = snapshot.val();
            profile.fullName = data.fullName || profile.fullName;
            profile.age = data.age || '';
            profile.gender = data.gender || 'Male';
            profile.username = data.username || profile.username;
            profile.mobile = data.mobile || '';
          }
        } catch (e) {
          console.warn("Could not fetch user profile from Realtime DB:", e);
        }

        setCurrentUser(profile);
      } else {
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

  const register = async ({ email, password, fullName, age, gender, username, mobile }) => {
    const res = await createUserWithEmailAndPassword(auth, email, password);
    const uid = res.user.uid;

    // Save doctor profile to Firebase Realtime DB node "Users/{uid}"
    try {
      await set(ref(rtdb, `Users/${uid}`), {
        fullName,
        age: age || '',
        gender: gender || 'Male',
        username: username || email.split('@')[0],
        mobile: mobile || '',
        email: email
      });
    } catch (e) {
      console.error("Error saving user profile to Realtime DB:", e);
    }

    return res.user;
  };

  const loginWithGoogle = async () => {
    const result = await signInWithPopup(auth, googleProvider);
    return result.user;
  };

  const logout = async () => {
    await firebaseSignOut(auth);
    setCurrentUser(null);
  };

  const updateProfile = async (updatedFields) => {
    if (!currentUser?.uid) return;
    try {
      await update(ref(rtdb, `Users/${currentUser.uid}`), {
        fullName: updatedFields.fullName,
        age: updatedFields.age || '',
        gender: updatedFields.gender || 'Male',
        mobile: updatedFields.mobile || ''
      });
      setCurrentUser(prev => ({ ...prev, ...updatedFields }));
    } catch (e) {
      console.error("Error updating user profile in Realtime DB:", e);
    }
  };

  return (
    <AuthContext.Provider value={{
      currentUser,
      loading,
      login,
      register,
      loginWithGoogle,
      logout,
      updateProfile
    }}>
      {!loading && children}
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
