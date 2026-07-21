import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { 
  Sun, 
  Moon, 
  User, 
  LogOut, 
  Menu, 
  X, 
  Scan, 
  ShieldCheck, 
  Bell 
} from 'lucide-react';

export default function Navbar({ onToggleSidebar, sidebarOpen }) {
  const { currentUser, logout, hasFirebase } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const [showUserMenu, setShowUserMenu] = useState(false);

  return (
    <header className="sticky top-0 z-30 w-full bg-white/80 dark:bg-portal-darkCard/80 backdrop-blur-md border-b border-portal-border dark:border-portal-darkBorder transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Left: Mobile Toggle & Brand Logo */}
        <div className="flex items-center gap-3">
          <button
            onClick={onToggleSidebar}
            className="p-2 rounded-xl text-portal-textMuted hover:text-portal-textMain hover:bg-slate-100 dark:hover:bg-slate-800 lg:hidden transition-colors"
            aria-label="Toggle Navigation"
          >
            {sidebarOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>

          <div className="flex items-center gap-3 cursor-pointer">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-white shadow-lg shadow-blue-500/20">
              <Scan className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-blue-600 to-indigo-600 dark:from-blue-400 dark:to-indigo-400 bg-clip-text text-transparent">
                  ShadeScan AI
                </span>
                {!hasFirebase && (
                  <span className="px-2 py-0.5 text-[10px] font-semibold bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 rounded-full border border-emerald-200 dark:border-emerald-800">
                    Client Mode
                  </span>
                )}
              </div>
              <p className="text-[11px] text-portal-textMuted dark:text-portal-darkTextMuted font-medium -mt-1 hidden sm:block">
                Clinical Dental Portal
              </p>
            </div>
          </div>
        </div>

        {/* Right: Theme Toggle, Notifications, User Menu */}
        <div className="flex items-center gap-2 sm:gap-3">
          
          {/* Theme Switcher Button */}
          <button
            onClick={toggleTheme}
            className="p-2.5 rounded-xl text-portal-textMuted dark:text-portal-darkTextMuted hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            title={isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}
          >
            {isDark ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5 text-slate-600" />}
          </button>

          {/* User Profile Dropdown */}
          <div className="relative">
            <button
              onClick={() => setShowUserMenu(!showUserMenu)}
              className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors border border-transparent hover:border-portal-border dark:hover:border-portal-darkBorder"
            >
              <div className="w-9 h-9 rounded-xl bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 font-bold flex items-center justify-center border border-blue-200 dark:border-blue-800">
                {currentUser?.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'D'}
              </div>
              <div className="text-left hidden md:block pr-1">
                <p className="text-sm font-semibold leading-tight text-portal-textMain dark:text-portal-darkTextMain">
                  {currentUser?.fullName || 'Dr. Practitioner'}
                </p>
                <p className="text-[11px] text-portal-textMuted dark:text-portal-darkTextMuted">
                  {currentUser?.email || 'doctor@shadescan.ai'}
                </p>
              </div>
            </button>

            {/* Dropdown Menu */}
            {showUserMenu && (
              <div 
                className="absolute right-0 mt-2 w-56 rounded-2xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder shadow-2xl py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-150"
                onClick={() => setShowUserMenu(false)}
              >
                <div className="px-4 py-2.5 border-b border-portal-border dark:border-portal-darkBorder">
                  <p className="text-xs font-semibold text-portal-textMuted dark:text-portal-darkTextMuted uppercase tracking-wider">
                    Signed in as
                  </p>
                  <p className="text-sm font-bold text-portal-textMain dark:text-portal-darkTextMain truncate mt-0.5">
                    {currentUser?.fullName}
                  </p>
                </div>

                <div className="py-1">
                  <button
                    onClick={logout}
                    className="w-full px-4 py-2.5 text-left text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 flex items-center gap-2.5 transition-colors"
                  >
                    <LogOut className="w-4 h-4" />
                    Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>

        </div>

      </div>
    </header>
  );
}
