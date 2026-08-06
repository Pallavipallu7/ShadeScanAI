import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { 
  Mail, 
  Lock, 
  User,
  ArrowLeft, 
  AlertCircle,
  ArrowRight
} from 'lucide-react';

export default function RegisterForm({ prefilledEmail = '', onSwitchToLogin }) {
  const { register } = useAuth();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState(prefilledEmail);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [verificationSent, setVerificationSent] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const cleanEmail = email.trim();
    if (!cleanEmail || !password || !confirmPassword) {
      setError('Please fill in all required fields.');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    try {
      setLoading(true);
      await register(fullName, cleanEmail, password);
      setVerificationSent(true);
    } catch (err) {
      console.error(err);
      const code = err.code || '';
      const msg = err.message || '';
      if (code === 'auth/email-already-in-use' || msg.includes('already in use') || msg.includes('email-already-in-use')) {
        onSwitchToLogin(cleanEmail, "An account already exists with this email. Please sign in.");
      } else {
        setError(msg || 'Failed to create account.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-slate-50 dark:bg-portal-darkBg p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-md space-y-6">
        
        {/* Header Navigation */}
        <div className="flex items-center justify-between">
          <button
            onClick={() => onSwitchToLogin()}
            className="flex items-center gap-2 text-sm font-semibold text-portal-textMuted dark:text-portal-darkTextMuted hover:text-portal-textMain dark:hover:text-portal-darkTextMain transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Login</span>
          </button>
          <div className="flex items-center gap-2">
            <svg className="w-6 h-6 text-blue-600 dark:text-blue-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2C8.5 2 6 4.5 6 8c0 3.5 1.5 8 3 11.5 1 2.3 2 2.5 3 2.5s2-.2 3-2.5C16.5 16 18 11.5 18 8c0-3.5-2.5-6-6-6z" />
              <path d="M9 8c.5-1.5 1.5-2.5 3-2.5s2.5 1 3 2.5" />
            </svg>
            <span className="font-bold text-sm text-portal-textMain dark:text-portal-darkTextMain">ShadeScan AI</span>
          </div>
        </div>

        {/* Card */}
        <div className="bg-white dark:bg-portal-darkCard p-6 sm:p-8 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl shadow-slate-200/50 dark:shadow-none">
          
          {verificationSent ? (
            <div className="text-center space-y-4 py-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 mx-auto flex items-center justify-center font-bold">
                ✓
              </div>
              <h2 className="text-xl font-bold text-portal-textMain dark:text-portal-darkTextMain">
                Account Created!
              </h2>
              <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted leading-relaxed">
                A verification link has been sent to <span className="font-bold text-portal-textMain dark:text-portal-darkTextMain">{email}</span>. Please verify your email before logging in.
              </p>
              <button
                onClick={() => onSwitchToLogin(email)}
                className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-md transition-all"
              >
                Proceed to Sign In
              </button>
            </div>
          ) : (
            <>
              <div className="mb-6">
                <h2 className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                  Create Account
                </h2>
                <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
                  Sign up with your email and password
                </p>
              </div>

          {error && (
            <div className="mb-4 p-3.5 rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 flex items-start gap-2.5 text-xs text-red-600 dark:text-red-400 font-medium">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Full Name */}
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1.5">
                Full Name
              </label>
              <div className="relative">
                <User className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Enter your full name"
                  className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                />
              </div>
            </div>

            {/* Email Address */}
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1.5">
                Email Address *
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                  required
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1.5">
                Password (6+ characters) *
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                  required
                />
              </div>
            </div>

            {/* Confirm Password */}
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1.5">
                Confirm Password *
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Confirm your password"
                  className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                  required
                />
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 transition-all transform active:scale-95 disabled:opacity-60"
              >
                {loading ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <span>Create Account</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>

          </form>
            </>
          )}

        </div>

        {/* Footer */}
        <p className="text-center text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
          Already have an account?{' '}
          <button
            onClick={onSwitchToLogin}
            className="font-bold text-blue-600 dark:text-blue-400 hover:underline"
          >
            Sign In Here
          </button>
        </p>

      </div>
    </div>
  );
}
