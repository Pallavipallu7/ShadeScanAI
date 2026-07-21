import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Scan, 
  Users, 
  History, 
  BookOpen, 
  Settings, 
  PlusCircle
} from 'lucide-react';

export default function Sidebar({ isOpen, onClose }) {
  const navItems = [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/scan', label: 'New AI Scan', icon: Scan, badge: 'AI' },
    { path: '/patients', label: 'Patients', icon: Users },
    { path: '/history', label: 'Scan History', icon: History },
    { path: '/vita-guide', label: 'VITA Shade Guide', icon: BookOpen },
    { path: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <>
      {/* Backdrop for Mobile */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-30 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed lg:static inset-y-0 left-0 z-40 w-64 bg-white dark:bg-portal-darkCard border-r border-portal-border dark:border-portal-darkBorder flex flex-col transition-transform duration-300 ease-in-out ${
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        {/* Navigation List */}
        <div className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
          
          <div className="mb-6 px-3">
            <NavLink
              to="/scan"
              onClick={onClose}
              className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25 transition-all transform hover:-translate-y-0.5 active:translate-y-0"
            >
              <PlusCircle className="w-5 h-5" />
              <span>Start New Scan</span>
            </NavLink>
          </div>

          <p className="px-3 text-[11px] font-bold tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted uppercase">
            Main Navigation
          </p>

          {navItems.map((item) => {
            const IconComponent = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3.5 py-3 rounded-xl font-semibold text-sm transition-all ${
                    isActive
                      ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800/60'
                      : 'text-portal-textMuted dark:text-portal-darkTextMuted hover:bg-slate-100 dark:hover:bg-slate-800/60 hover:text-portal-textMain dark:hover:text-portal-darkTextMain'
                  }`
                }
              >
                <div className="flex items-center gap-3">
                  <IconComponent className="w-5 h-5" />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span className="px-2 py-0.5 text-[10px] font-bold bg-blue-600 text-white rounded-full">
                    {item.badge}
                  </span>
                )}
              </NavLink>
            );
          })}
        </div>

        {/* Quick Help Footer */}
        <div className="p-4 m-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder">
          <p className="text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain">
            VITA Classical Shade System
          </p>
          <p className="text-[11px] text-portal-textMuted dark:text-portal-darkTextMuted mt-0.5">
            Calibrated for 16 standard tooth shade matches (A1-D4).
          </p>
        </div>
      </aside>
    </>
  );
}
