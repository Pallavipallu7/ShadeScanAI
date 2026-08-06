import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { 
  ShieldCheck, 
  User, 
  Phone, 
  Globe, 
  MapPin, 
  Building, 
  Stethoscope, 
  LogOut,
  AlertCircle,
  CheckCircle2,
  Loader2
} from 'lucide-react';

export default function MandatoryVerification() {
  const { currentUser, completeRegistration, logout } = useAuth();

  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [mobile, setMobile] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState('Male');
  const [country, setCountry] = useState('');
  const [state, setState] = useState('');
  const [city, setCity] = useState('');

  // Optional fields
  const [clinicName, setClinicName] = useState('');
  const [dentistName, setDentistName] = useState('');
  const [address, setAddress] = useState('');
  const [emergencyContact, setEmergencyContact] = useState('');

  // Consents
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const isFormValid = 
    fullName.trim() !== '' &&
    mobile.trim() !== '' &&
    age.trim() !== '' &&
    country.trim() !== '' &&
    state.trim() !== '' &&
    city.trim() !== '' &&
    acceptedTerms &&
    acceptedPrivacy;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    if (!isFormValid) {
      setError('Please complete all required fields and accept terms.');
      return;
    }

    try {
      setLoading(true);
      await completeRegistration({
        fullName,
        mobile,
        age,
        gender,
        country,
        state,
        city,
        clinicName,
        dentistName,
        address,
        emergencyContact,
        acceptedTerms,
        acceptedPrivacy,
        email: currentUser?.email || ''
      });
      setSuccessMessage('Account verified successfully!');
    } catch (err) {
      console.error(err);
      setError(err.message || 'Verification failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-slate-50 dark:bg-portal-darkBg p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-2xl space-y-6">
        
        {/* Header Action / Logout */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-7 h-7 text-blue-600 dark:text-blue-400" />
            <span className="font-extrabold text-lg text-portal-textMain dark:text-portal-darkTextMain">ShadeScan AI</span>
          </div>

          <button
            onClick={logout}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span>Logout</span>
          </button>
        </div>

        {/* Card */}
        <div className="bg-white dark:bg-portal-darkCard p-6 sm:p-8 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl space-y-6">
          
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              Complete Your ShadeScan AI Account
            </h1>
            <p className="text-xs sm:text-sm font-semibold text-blue-600 dark:text-blue-400 mt-1">
              Mandatory Practitioner Verification
            </p>
          </div>

          {/* Recognized Google Account Banner */}
          <div className="p-4 rounded-2xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 space-y-1">
            <p className="text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Google Account: <span className="text-blue-600 dark:text-blue-400">{currentUser?.email || 'Authenticated User'}</span>
            </p>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted leading-relaxed">
              Your Google account has been recognized. Please complete the required verification/registration steps before accessing ShadeScan AI.
            </p>
          </div>

          {error && (
            <div className="p-3.5 rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 flex items-start gap-2.5 text-xs text-red-600 dark:text-red-400 font-medium">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {successMessage && (
            <div className="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 flex items-start gap-2.5 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
              <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{successMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            
            <p className="text-xs font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
              Required Information
            </p>

            {/* Full Name */}
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Full Name *
              </label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Dr. Sarah Miller"
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                  required
                />
              </div>
            </div>

            {/* Mobile & Age & Gender */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Mobile Number *
                </label>
                <div className="relative">
                  <Phone className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                  <input
                    type="tel"
                    value={mobile}
                    onChange={(e) => setMobile(e.target.value)}
                    placeholder="+1 555-0199"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Age *
                </label>
                <input
                  type="number"
                  value={age}
                  onChange={(e) => setAge(e.target.value)}
                  placeholder="35"
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Gender
                </label>
                <select
                  value={gender}
                  onChange={(e) => setGender(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                >
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>

            {/* Location: Country, State, City */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Country *
                </label>
                <div className="relative">
                  <Globe className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                  <input
                    type="text"
                    value={country}
                    onChange={(e) => setCountry(e.target.value)}
                    placeholder="United States"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  State *
                </label>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  placeholder="California"
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  City *
                </label>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  placeholder="Los Angeles"
                  className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder focus:ring-2 focus:ring-blue-500 text-sm text-portal-textMain dark:text-portal-darkTextMain"
                  required
                />
              </div>
            </div>

            {/* Optional Information */}
            <p className="text-xs font-bold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted pt-2">
              Optional Practice Details
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Clinic Name (Optional)
                </label>
                <div className="relative">
                  <Building className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                  <input
                    type="text"
                    value={clinicName}
                    onChange={(e) => setClinicName(e.target.value)}
                    placeholder="Apex Dental Clinic"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm text-portal-textMain dark:text-portal-darkTextMain"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                  Dentist Title / License (Optional)
                </label>
                <div className="relative">
                  <Stethoscope className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted dark:text-portal-darkTextMuted" />
                  <input
                    type="text"
                    value={dentistName}
                    onChange={(e) => setDentistName(e.target.value)}
                    placeholder="DDS / Practitioner"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm text-portal-textMain dark:text-portal-darkTextMain"
                  />
                </div>
              </div>
            </div>

            {/* Legal Consents */}
            <div className="space-y-2 pt-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={acceptedTerms}
                  onChange={(e) => setAcceptedTerms(e.target.checked)}
                  className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500"
                />
                <span className="text-xs text-portal-textMain dark:text-portal-darkTextMain font-medium">
                  I accept the Terms & Conditions *
                </span>
              </label>

              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={acceptedPrivacy}
                  onChange={(e) => setAcceptedPrivacy(e.target.checked)}
                  className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500"
                />
                <span className="text-xs text-portal-textMain dark:text-portal-darkTextMain font-medium">
                  I accept the Privacy Policy *
                </span>
              </label>
            </div>

            {/* Submit Button */}
            <div className="pt-4">
              <button
                type="submit"
                disabled={loading || !isFormValid}
                className="w-full py-3.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 transition-all transform active:scale-95 disabled:opacity-50"
              >
                {loading ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <span>Complete Registration</span>
                )}
              </button>
            </div>

          </form>

        </div>
      </div>
    </div>
  );
}
