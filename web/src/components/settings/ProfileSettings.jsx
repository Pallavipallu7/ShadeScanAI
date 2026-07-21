import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { 
  User, 
  Moon, 
  Sun, 
  Phone, 
  ShieldCheck, 
  Bell, 
  CheckCircle2, 
  Save 
} from 'lucide-react';

export default function ProfileSettings() {
  const { currentUser, updateProfile, hasFirebase } = useAuth();
  const { isDark, toggleTheme } = useTheme();

  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [age, setAge] = useState(currentUser?.age || '');
  const [gender, setGender] = useState(currentUser?.gender || 'Male');
  const [mobile, setMobile] = useState(currentUser?.mobile || '');

  const [notifications, setNotifications] = useState(true);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSave = (e) => {
    e.preventDefault();
    updateProfile({
      fullName,
      age,
      gender,
      mobile
    });
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 3000);
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      
      <div>
        <h1 className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
          Account & Portal Settings
        </h1>
        <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
          Manage your practitioner profile, application theme, and preferences
        </p>
      </div>

      {savedSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300 font-bold text-xs flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5" />
          <span>Profile changes saved successfully!</span>
        </div>
      )}

      {/* Profile Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 sm:p-8 shadow-sm space-y-6">
        
        <h2 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain flex items-center gap-2">
          <User className="w-5 h-5 text-blue-600" />
          <span>Practitioner Profile</span>
        </h2>

        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Full Name
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Mobile Number
              </label>
              <input
                type="tel"
                value={mobile}
                onChange={(e) => setMobile(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Age
              </label>
              <input
                type="number"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Gender
              </label>
              <select
                value={gender}
                onChange={(e) => setGender(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>

          <div className="pt-2">
            <button
              type="submit"
              className="px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md flex items-center gap-2 transition-colors"
            >
              <Save className="w-4 h-4" />
              <span>Save Profile Changes</span>
            </button>
          </div>
        </form>

      </div>

      {/* Application Preferences Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 sm:p-8 shadow-sm space-y-6">
        
        <h2 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
          Application Preferences
        </h2>

        <div className="space-y-4 text-sm">
          
          {/* Theme Preference */}
          <div className="flex items-center justify-between p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder">
            <div className="flex items-center gap-3">
              {isDark ? <Moon className="w-5 h-5 text-amber-400" /> : <Sun className="w-5 h-5 text-slate-600" />}
              <div>
                <p className="font-bold text-portal-textMain dark:text-portal-darkTextMain">
                  Dark Mode Theme
                </p>
                <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                  Toggle dark mode visual interface for low-light clinics
                </p>
              </div>
            </div>

            <button
              onClick={toggleTheme}
              className={`w-12 h-6 rounded-full p-1 transition-colors ${isDark ? 'bg-blue-600' : 'bg-slate-300'}`}
            >
              <div className={`w-4 h-4 rounded-full bg-white transition-transform ${isDark ? 'translate-x-6' : 'translate-x-0'}`} />
            </button>
          </div>

          {/* Notifications */}
          <div className="flex items-center justify-between p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder">
            <div className="flex items-center gap-3">
              <Bell className="w-5 h-5 text-indigo-600" />
              <div>
                <p className="font-bold text-portal-textMain dark:text-portal-darkTextMain">
                  Scan & Export Alerts
                </p>
                <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                  Receive audio tone and notification alerts on AI scan completion
                </p>
              </div>
            </div>

            <button
              onClick={() => setNotifications(!notifications)}
              className={`w-12 h-6 rounded-full p-1 transition-colors ${notifications ? 'bg-blue-600' : 'bg-slate-300'}`}
            >
              <div className={`w-4 h-4 rounded-full bg-white transition-transform ${notifications ? 'translate-x-6' : 'translate-x-0'}`} />
            </button>
          </div>

          {/* System Status */}
          <div className="p-4 rounded-2xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 space-y-1 text-xs text-blue-900 dark:text-blue-200">
            <p className="font-extrabold flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-blue-600" />
              <span>System Status: ShadeScan AI Web v1.0.0</span>
            </p>
            <p className="text-blue-700 dark:text-blue-300">
              {hasFirebase ? 'Connected to Firebase Auth & Firestore' : 'Operating in Standalone Client Mode (LocalStorage / IndexedDB)'}
            </p>
          </div>

        </div>

      </div>

    </div>
  );
}
