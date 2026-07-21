import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { 
  Scan, 
  User, 
  Mail, 
  Lock, 
  Phone, 
  ShieldCheck, 
  ArrowLeft, 
  CheckCircle2, 
  AlertCircle,
  Loader2
} from 'lucide-react';

export default function RegisterForm({ onSwitchToLogin }) {
  const { register } = useAuth();

  const [fullName, setFullName] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState('Male');
  const [username, setUsername] = useState('');
  const [mobile, setMobile] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // Email verification simulation
  const [isVerifying, setIsVerifying] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [emailError, setEmailError] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleVerifyEmail = () => {
    setEmailError('');
    if (!email) {
      setEmailError('Please enter a valid email address.');
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setEmailError('Invalid email format.');
      return;
    }

    setIsVerifying(true);
    setTimeout(() => {
      setIsVerifying(false);
      setIsEmailVerified(true);
    }, 1200);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!fullName || !email || !password) {
      setError('Please fill in all required fields.');
      return;
    }

    if (!isEmailVerified) {
      setError('Please verify your email address before creating an account.');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    try {
      setLoading(true);
      await register({
        fullName,
        age: age || '30',
        gender,
        username: username || email.split('@')[0],
        mobile: mobile || '+1 555-0199',
        email,
        password
      });
    } catch (err) {
      setError(err.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-slate-50 dark:bg-portal-darkBg p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-xl space-y-6">
        
        {/* Header Navigation */}
        <div className="flex items-center justify-between">
          <button
            onClick={onSwitchToLogin}
            className="flex items-center gap-2 text-sm font-semibold text-portal-textMuted dark:text-portal-darkTextMuted hover:text-portal-textMain dark:hover:text-portal-darkTextMain transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Login</span>
          </button>
          <div className="flex items-center gap-2">
            <Scan className="w-6 h-6 text-blue-600 dark:text-blue-400" />
            <span className="font-bold text-sm text-portal-textMain dark:text-portal-darkTextMain">ShadeScan AI</span>
          </div>
        </div>

        {/* Card */}
        <div className="bg-white dark:bg-portal-darkCard p-6 sm:p-8 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl shadow-slate-200/50 dark:shadow-none">
          
          <div className="mb-6">
            <h2 className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              Create Clinical Account
            </h2>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
              Complete your profile to register your dental practice
            </p>
          </div>

          {error && (
            <div className="mb-4 p-3.5 rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 flex items-start gap-2.5 text-xs text-red-600 dark:text-red-400 font-medium">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Personal Info Group */}
            <div className="space-y-3">
              <p className="text-xs font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
                1. Personal Information
              </p>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Full Name *
                </label>
                <div className="relative">
                  <User className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Dr. Sarah Jenkins"
                    className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                    Age
                  </label>
                  <input
                    type="number"
                    value={age}
                    onChange={(e) => setAge(e.target.value)}
                    placeholder="34"
                    className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                    Gender
                  </label>
                  <select
                    value={gender}
                    onChange={(e) => setGender(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                  >
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                    Username
                  </label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="drsarahj"
                    className="w-full px-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                    Mobile Number
                  </label>
                  <div className="relative">
                    <Phone className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                    <input
                      type="tel"
                      value={mobile}
                      onChange={(e) => setMobile(e.target.value)}
                      placeholder="+1 555-0199"
                      className="w-full pl-10 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Account Credentials & Email Verification Group */}
            <div className="space-y-3 pt-2">
              <p className="text-xs font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
                2. Account Access & Verification
              </p>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Email Address *
                </label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Mail className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        setIsEmailVerified(false);
                        setEmailError('');
                      }}
                      placeholder="doctor@clinic.com"
                      className={`w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium ${
                        emailError ? 'border-red-500' : 'border-portal-border dark:border-portal-darkBorder'
                      }`}
                      required
                    />
                  </div>

                  <button
                    type="button"
                    onClick={handleVerifyEmail}
                    disabled={isVerifying || isEmailVerified || !email}
                    className={`px-4 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shrink-0 ${
                      isEmailVerified
                        ? 'bg-emerald-600 text-white'
                        : 'bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50'
                    }`}
                  >
                    {isVerifying ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : isEmailVerified ? (
                      <>
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Verified</span>
                      </>
                    ) : (
                      <span>Verify</span>
                    )}
                  </button>
                </div>

                {emailError && (
                  <p className="text-[11px] text-red-500 font-medium mt-1 pl-1">{emailError}</p>
                )}
                {isEmailVerified && (
                  <p className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium mt-1 pl-1 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Verification link sent. Email successfully validated.
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Password (6+ characters) *
                </label>
                <div className="relative">
                  <Lock className="w-5 h-5 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 focus:outline-none text-sm text-portal-textMain dark:text-portal-darkTextMain font-medium"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-4">
              <button
                type="submit"
                disabled={loading || !isEmailVerified}
                className="w-full py-3.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 transition-all transform active:scale-95 disabled:opacity-60"
              >
                {loading ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <span>Complete Account Registration</span>
                )}
              </button>
            </div>

          </form>

        </div>

        {/* Footer */}
        <p className="text-center text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
          Already have a clinical profile?{' '}
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
