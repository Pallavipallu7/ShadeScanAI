import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import ScanImageThumbnail from '../common/ScanImageThumbnail';
import { 
  Users, 
  Scan, 
  BookOpen, 
  ArrowRight, 
  Sparkles, 
  ChevronRight,
  TrendingUp
} from 'lucide-react';

export default function DashboardOverview({ patients = [], scans = [], onSelectPatient }) {
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  const totalPatients = patients.length;
  const totalScans = scans.length;

  const handlePatientClick = (scan) => {
    const matchingPatient = patients.find(p => p.id === scan.patientId || (scan.patientName && p.name.toLowerCase() === scan.patientName.toLowerCase()));
    if (matchingPatient && onSelectPatient) {
      onSelectPatient(matchingPatient);
      navigate('/patients');
    } else {
      navigate('/history');
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Welcome Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-blue-600 via-indigo-600 to-slate-900 p-6 sm:p-8 text-white shadow-xl shadow-blue-500/15">
        <div className="relative z-10 max-w-2xl space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>AI Tooth Shade Classification Portal</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Welcome back, {currentUser?.fullName || 'Doctor'}
          </h1>
          <p className="text-sm text-blue-100 font-medium leading-relaxed">
            Ready to analyze tooth shades with precision. Manage your patient records, conduct automated colorimetry scans, and export clinical PDF reports.
          </p>
          <div className="pt-2 flex flex-wrap items-center gap-3">
            <button
              onClick={() => navigate('/scan')}
              className="px-5 py-3 rounded-2xl bg-white text-blue-600 hover:bg-blue-50 font-bold text-sm shadow-lg flex items-center gap-2 transition-transform active:scale-95"
            >
              <Scan className="w-4 h-4" />
              <span>New AI Scan</span>
            </button>
            <button
              onClick={() => navigate('/patients')}
              className="px-5 py-3 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-semibold text-sm backdrop-blur-md flex items-center gap-2 transition-colors"
            >
              <Users className="w-4 h-4" />
              <span>View Patients ({totalPatients})</span>
            </button>
          </div>
        </div>

        {/* Decorative Grid SVG background */}
        <div className="absolute right-0 top-0 bottom-0 w-1/3 opacity-10 pointer-events-none hidden md:block">
          <svg className="w-full h-full" viewBox="0 0 100 100" fill="none" stroke="currentColor">
            <path d="M0 0h100v100H0z" strokeWidth="0.5" />
            <circle cx="50" cy="50" r="30" strokeWidth="1" />
            <circle cx="50" cy="50" r="10" strokeWidth="1" />
          </svg>
        </div>
      </div>

      {/* Metrics Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        
        {/* Metric 1 */}
        <div className="p-5 rounded-2xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-portal-textMuted dark:text-portal-darkTextMuted">
              Total Patients
            </p>
            <p className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              {totalPatients}
            </p>
          </div>
        </div>

        {/* Metric 2 */}
        <div className="p-5 rounded-2xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
            <Scan className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-portal-textMuted dark:text-portal-darkTextMuted">
              Completed Scans
            </p>
            <p className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              {totalScans}
            </p>
          </div>
        </div>

        {/* Metric 3 */}
        <div className="p-5 rounded-2xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-portal-textMuted dark:text-portal-darkTextMuted">
              Avg AI Confidence
            </p>
            <p className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              95.4%
            </p>
          </div>
        </div>

        {/* Metric 4 */}
        <div className="p-5 rounded-2xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-50 dark:bg-amber-950 text-amber-600 dark:text-amber-400 flex items-center justify-center font-bold">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-portal-textMuted dark:text-portal-darkTextMuted">
              VITA Standards
            </p>
            <p className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              16 Shades
            </p>
          </div>
        </div>

      </div>

      {/* Quick Action Shortcuts Banner */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          onClick={() => navigate('/scan')}
          className="group cursor-pointer p-6 rounded-3xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder hover:border-blue-500 dark:hover:border-blue-500 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 rounded-2xl bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center group-hover:scale-110 transition-transform">
              <Scan className="w-6 h-6" />
            </div>
            <ChevronRight className="w-5 h-5 text-portal-textMuted group-hover:translate-x-1 transition-transform" />
          </div>
          <h3 className="font-bold text-base text-portal-textMain dark:text-portal-darkTextMain">
            Upload & Classify Image
          </h3>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
            Capture or drag tooth photos to calculate VITA shade matches.
          </p>
        </div>

        <div
          onClick={() => navigate('/patients')}
          className="group cursor-pointer p-6 rounded-3xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder hover:border-indigo-500 dark:hover:border-indigo-500 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 rounded-2xl bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 flex items-center justify-center group-hover:scale-110 transition-transform">
              <Users className="w-6 h-6" />
            </div>
            <ChevronRight className="w-5 h-5 text-portal-textMuted group-hover:translate-x-1 transition-transform" />
          </div>
          <h3 className="font-bold text-base text-portal-textMain dark:text-portal-darkTextMain">
            Manage Patient Database
          </h3>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
            Create patient profiles, track scan histories, and store notes.
          </p>
        </div>

        <div
          onClick={() => navigate('/vita-guide')}
          className="group cursor-pointer p-6 rounded-3xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder hover:border-amber-500 dark:hover:border-amber-500 shadow-sm transition-all hover:shadow-md"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 rounded-2xl bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400 flex items-center justify-center group-hover:scale-110 transition-transform">
              <BookOpen className="w-6 h-6" />
            </div>
            <ChevronRight className="w-5 h-5 text-portal-textMuted group-hover:translate-x-1 transition-transform" />
          </div>
          <h3 className="font-bold text-base text-portal-textMain dark:text-portal-darkTextMain">
            VITA Shade Reference
          </h3>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
            Explore hue groups A1-A4, B1-B4, C1-C4, and D2-D4 color standards.
          </p>
        </div>
      </div>

      {/* Recent Scans Table */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-sm p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              Recent Clinical Scans
            </h2>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
              Latest shade evaluations generated by ShadeScan AI
            </p>
          </div>
          <button
            onClick={() => navigate('/history')}
            className="text-xs font-bold text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
          >
            <span>View All History</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {scans.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-portal-border dark:border-portal-darkBorder text-[11px] font-bold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
                  <th className="pb-3 pl-2">Photo / Swatch</th>
                  <th className="pb-3">Patient</th>
                  <th className="pb-3">Predicted Shade</th>
                  <th className="pb-3">Confidence</th>
                  <th className="pb-3">Scan Date</th>
                  <th className="pb-3 pr-2 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-portal-border dark:divide-portal-darkBorder text-sm">
                {scans.slice(0, 5).map((scan) => (
                  <tr key={scan.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                    <td className="py-3 pl-2">
                      <ScanImageThumbnail
                        imageUri={scan.imageUri}
                        shade={scan.predictedShade}
                        size="sm"
                        onClick={() => navigate('/history')}
                      />
                    </td>
                    <td className="py-3 font-bold text-portal-textMain dark:text-portal-darkTextMain">
                      <button
                        onClick={() => handlePatientClick(scan)}
                        className="hover:text-blue-600 dark:hover:text-blue-400 hover:underline text-left"
                      >
                        {scan.patientName || 'Walk-in Patient'}
                      </button>
                    </td>
                    <td className="py-3">
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-extrabold text-xs border border-blue-200 dark:border-blue-800">
                        Shade {scan.predictedShade}
                      </span>
                    </td>
                    <td className="py-3 text-portal-textMuted dark:text-portal-darkTextMuted font-semibold text-xs">
                      {scan.confidence}
                    </td>
                    <td className="py-3 text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                      {new Date(scan.dateTime).toLocaleDateString()}
                    </td>
                    <td className="py-3 pr-2 text-right">
                      <button
                        onClick={() => handlePatientClick(scan)}
                        className="px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-xs font-semibold text-portal-textMain dark:text-portal-darkTextMain transition-colors"
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-12 space-y-3">
            <Scan className="w-10 h-10 text-portal-textMuted mx-auto opacity-50" />
            <p className="text-sm font-semibold text-portal-textMuted dark:text-portal-darkTextMuted">
              No scans performed yet.
            </p>
            <button
              onClick={() => navigate('/scan')}
              className="px-4 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs shadow-md"
            >
              Start First Scan
            </button>
          </div>
        )}
      </div>

    </div>
  );
}
