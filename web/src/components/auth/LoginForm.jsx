import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { 
  Scan, 
  Mail, 
  Lock, 
  Eye, 
  EyeOff, 
  ArrowRight, 
  AlertCircle 
} from 'lucide-react';

export default function LoginForm({ onSwitchToRegister, onForgotPassword }) {
  const { login, loginWithGoogle } = useAuth();
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!email || !password) {
      setError('Please enter your email and password.');
      return;
    }
    try {
      setLoading(true);
      await login(email, password);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    try {
      setError('');
      setLoading(true);
      await loginWithGoogle();
    } catch (err) {
      setError('Google Sign-In failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-slate-50 dark:bg-portal-darkBg p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-md space-y-6">
        
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-xl shadow-blue-500/25 mb-1">
            <Scan className="w-10 h-10" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-portal-textMain dark:text-portal-darkTextMain">
            ShadeScan AI
          </h1>
          <p className="text-sm text-portal-textMuted dark:text-portal-darkTextMuted font-medium">
            Clinical Portal Login & Dental Shade Matcher
          </p>
        </div>

        {/* Card Form */}
        <div className="bg-white dark:bg-portal-darkCard p-6 sm:p-8 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl shadow-slate-200/50 dark:shadow-none">
          
          <div className="mb-6">
            <h2 className="text-xl font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Welcome Back
            </h2>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
              Sign in with your registered ShadeScan AI account
            </p>
          </div>

          {error && (
            <div className="mb-4 p-3.5 rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 flex items-start gap-2.5 text-xs text-red-600 dark:text-red-400 font-medium">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Email Field */}
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1.5">
                Email Address
              </label>
              <div className="relative">
                <Mail className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium transition-colors"
                  required
                />
              </div>
            </div>

            {/* Password Field */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain">
                  Password
                </label>
                <button
                  type="button"
                  onClick={() => onForgotPassword(email)}
                  className="text-xs font-bold text-blue-600 dark:text-blue-400 hover:underline"
                >
                  Forgot Password?
                </button>
              </div>
              <div className="relative">
                <Lock className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  className="w-full pl-11 pr-11 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium transition-colors"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted hover:text-portal-textMain"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            {/* Remember Me */}
            <div className="flex items-center justify-between py-1">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500 border-gray-300"
                />
                <span className="text-xs font-medium text-portal-textMuted dark:text-portal-darkTextMuted">
                  Remember Me
                </span>
              </label>
            </div>

            {/* Submit Button */}
            <button
              id="login-button"
              type="submit"
              disabled={loading}
              className="w-full py-3.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 transition-all transform active:scale-95 disabled:opacity-70"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Divider */}
          <div className="relative my-6 text-center">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-portal-border dark:border-portal-darkBorder" />
            </div>
            <span className="relative px-3 bg-white dark:bg-portal-darkCard text-[11px] font-bold text-portal-textMuted dark:text-portal-darkTextMuted uppercase tracking-wider">
              OR
            </span>
          </div>

          {/* Google Sign-in */}
          <button
            type="button"
            onClick={handleGoogleSignIn}
            disabled={loading}
            className="w-full py-3 rounded-xl border border-portal-border dark:border-portal-darkBorder hover:bg-slate-50 dark:hover:bg-slate-800 text-portal-textMain dark:text-portal-darkTextMain font-semibold text-sm flex items-center justify-center gap-3 transition-colors"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
            </svg>
            <span>Sign in with Google</span>
          </button>

        </div>

        {/* Footer Link */}
        <p className="text-center text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
          New to ShadeScan AI?{' '}
          <button
            onClick={onSwitchToRegister}
            className="font-bold text-blue-600 dark:text-blue-400 hover:underline"
          >
            Create Clinical Account
          </button>
        </p>

      </div>
    </div>
  );
}
