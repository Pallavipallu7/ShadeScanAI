import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { auth, fetchSignInMethodsForEmail } from '../../firebase/config';
import { 
  Scan, 
  Mail, 
  Lock, 
  Eye, 
  EyeOff, 
  ArrowRight, 
  AlertCircle 
} from 'lucide-react';

export default function LoginForm({ initialEmail = '', authMessage = '', onSwitchToRegister, onForgotPassword }) {
  const { login } = useAuth();
  
  const [email, setEmail] = useState(initialEmail);
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(authMessage);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const cleanEmail = email.trim();
    if (!cleanEmail || !password) {
      setError('Please enter your email and password.');
      return;
    }

    try {
      setLoading(true);
      
      // Check in real time if email exists in Firebase Auth
      let signInMethods = [];
      try {
        signInMethods = await fetchSignInMethodsForEmail(auth, cleanEmail);
      } catch (err) {
        console.warn("fetchSignInMethodsForEmail error:", err);
      }

      if (signInMethods && signInMethods.length === 0) {
        // Email does NOT exist in Firebase Auth -> Automatically redirect to Create Account page pre-filling email!
        setLoading(false);
        onSwitchToRegister(cleanEmail);
        return;
      }

      // Email exists, attempt password sign-in
      await login(cleanEmail, password);
    } catch (err) {
      console.error(err);
      const code = err.code || '';
      const msg = err.message || '';
      if (code === 'auth/user-not-found' || msg.includes('user-not-found') || msg.includes('no user record')) {
        onSwitchToRegister(cleanEmail);
      } else if (code === 'auth/wrong-password' || code === 'auth/invalid-credential' || msg.includes('wrong-password') || msg.includes('invalid-credential')) {
        setError('Incorrect password. Please try again.');
      } else if (code === 'auth/invalid-email' || msg.includes('invalid-email')) {
        setError('Enter a valid email address.');
      } else if (code === 'auth/network-request-failed' || msg.includes('network')) {
        setError('Check your internet connection and try again.');
      } else {
        setError('Incorrect password. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-slate-50 dark:bg-portal-darkBg p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-md space-y-6">
        
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex p-3.5 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-xl shadow-blue-500/25 mb-1">
            <svg className="w-10 h-10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2C8.5 2 6 4.5 6 8c0 3.5 1.5 8 3 11.5 1 2.3 2 2.5 3 2.5s2-.2 3-2.5C16.5 16 18 11.5 18 8c0-3.5-2.5-6-6-6z" />
              <path d="M9 8c.5-1.5 1.5-2.5 3-2.5s2.5 1 3 2.5" />
            </svg>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-portal-textMain dark:text-portal-darkTextMain">
            ShadeScan AI
          </h1>
          <p className="text-sm text-portal-textMuted dark:text-portal-darkTextMuted font-medium">
            Clinical Tooth Shade Intelligence Portal
          </p>
        </div>

        {/* Card Form */}
        <div className="bg-white dark:bg-portal-darkCard p-6 sm:p-8 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl shadow-slate-200/50 dark:shadow-none">
          
          <div className="mb-6">
            <h2 className="text-xl font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Welcome Back
            </h2>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
              Sign in with your email and password
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
