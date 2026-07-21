import React, { useState } from 'react';
import { Mail, CheckCircle2, X, AlertCircle } from 'lucide-react';

export default function ForgotPasswordModal({ initialEmail, onClose }) {
  const [email, setEmail] = useState(initialEmail || '');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSendReset = (e) => {
    e.preventDefault();
    if (!email) return;
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSent(true);
    }, 1000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-2xl relative">
        
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-portal-textMuted hover:text-portal-textMain p-1 rounded-lg"
        >
          <X className="w-5 h-5" />
        </button>

        {!sent ? (
          <div>
            <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center mb-4">
              <Mail className="w-6 h-6" />
            </div>

            <h3 className="text-xl font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Reset Password
            </h3>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1 mb-6">
              Enter your registered clinical email address and we'll send you a password reset link.
            </p>

            <form onSubmit={handleSendReset} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1.5">
                  Email Address
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="doctor@clinic.com"
                  className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm font-medium"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={loading || !email}
                className="w-full py-3.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 transition-all"
              >
                {loading ? 'Sending Request...' : 'Send Reset Link'}
              </button>
            </form>
          </div>
        ) : (
          <div className="text-center py-4 space-y-3">
            <div className="w-14 h-14 rounded-full bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Reset Email Sent!
            </h3>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
              Instructions have been dispatched to <span className="font-semibold text-portal-textMain dark:text-portal-darkTextMain">{email}</span>. Please check your inbox.
            </p>
            <button
              onClick={onClose}
              className="mt-4 w-full py-3 rounded-xl bg-slate-100 dark:bg-slate-800 text-portal-textMain dark:text-portal-darkTextMain font-bold text-sm"
            >
              Done
            </button>
          </div>
        )}

      </div>
    </div>
  );
}
