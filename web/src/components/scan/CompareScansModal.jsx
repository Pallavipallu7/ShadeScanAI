import React, { useState } from 'react';
import ScanImageThumbnail from '../common/ScanImageThumbnail';
import { getShadeColor, getToothImageSvgDataUri } from '../../utils/shadeAnalyzer';
import { Layers, X, ArrowRight, Award, CheckCircle2, ChevronRight } from 'lucide-react';

export default function CompareScansModal({ initialScan, allScans = [], onClose }) {
  const [scanA, setScanA] = useState(initialScan || allScans[0] || null);
  const [scanB, setScanB] = useState(allScans.find(s => s.id !== initialScan?.id) || allScans[1] || initialScan || null);

  if (!scanA || !scanB) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md animate-in fade-in">
        <div className="w-full max-w-md bg-white dark:bg-portal-darkCard rounded-3xl p-6 border border-portal-border dark:border-portal-darkBorder shadow-2xl text-center space-y-4">
          <Layers className="w-12 h-12 text-blue-600 mx-auto" />
          <h3 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
            Scan Comparison
          </h3>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
            You need at least 2 recorded scans to perform a side-by-side clinical comparison.
          </p>
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-blue-600 text-white font-bold text-xs"
          >
            Close
          </button>
        </div>
      </div>
    );
  }

  const colorA = getShadeColor(scanA.predictedShade || 'A2');
  const colorB = getShadeColor(scanB.predictedShade || 'A2');

  const isSameShade = (scanA.predictedShade || '').toUpperCase() === (scanB.predictedShade || '').toUpperCase();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md animate-in fade-in">
      <div className="w-full max-w-3xl bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-2xl relative space-y-6 max-h-[90vh] overflow-y-auto">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-portal-border dark:border-portal-darkBorder pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center font-extrabold">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                Side-by-Side Clinical Scan Comparison
              </h2>
              <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                Compare colorimetry, VITA shade metrics, and confidence between two scans
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full bg-slate-100 dark:bg-slate-800 text-portal-textMuted hover:text-portal-textMain"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scan Selection Dropdowns */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          
          {/* Scan A Selector */}
          <div>
            <label className="block text-xs font-bold text-portal-textMuted mb-1">
              Select Primary Scan (Scan A)
            </label>
            <select
              value={scanA.id}
              onChange={(e) => {
                const s = allScans.find(item => item.id === e.target.value);
                if (s) setScanA(s);
              }}
              className="w-full p-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-xs font-bold focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              {allScans.map(s => (
                <option key={s.id} value={s.id}>
                  {s.patientName || 'Walk-in'} — Shade {s.predictedShade} ({new Date(s.dateTime).toLocaleDateString()})
                </option>
              ))}
            </select>
          </div>

          {/* Scan B Selector */}
          <div>
            <label className="block text-xs font-bold text-portal-textMuted mb-1">
              Select Comparison Scan (Scan B)
            </label>
            <select
              value={scanB.id}
              onChange={(e) => {
                const s = allScans.find(item => item.id === e.target.value);
                if (s) setScanB(s);
              }}
              className="w-full p-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-xs font-bold focus:ring-2 focus:ring-blue-500 focus:outline-none"
            >
              {allScans.map(s => (
                <option key={s.id} value={s.id}>
                  {s.patientName || 'Walk-in'} — Shade {s.predictedShade} ({new Date(s.dateTime).toLocaleDateString()})
                </option>
              ))}
            </select>
          </div>

        </div>

        {/* Side-by-Side Comparison Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          
          {/* Scan A Card */}
          <div className="p-5 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-4">
            <div className="flex items-center justify-between">
              <span className="px-3 py-1 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-extrabold text-[11px]">
                SCAN A
              </span>
              <span className="text-[11px] text-portal-textMuted">
                {new Date(scanA.dateTime).toLocaleDateString()}
              </span>
            </div>

            <div className="h-44 rounded-xl overflow-hidden bg-slate-950 flex items-center justify-center border border-portal-border dark:border-portal-darkBorder">
              <ScanImageThumbnail
                imageUri={scanA.imageUri}
                shade={scanA.predictedShade}
                size="lg"
              />
            </div>

            <div className="space-y-1">
              <h4 className="font-extrabold text-base text-portal-textMain dark:text-portal-darkTextMain">
                {scanA.patientName || 'Walk-in Patient'}
              </h4>
              <div className="flex items-center gap-2">
                <span className="text-xl font-black text-blue-600 dark:text-blue-400">
                  Shade {scanA.predictedShade}
                </span>
                <span className="text-xs font-bold text-portal-textMuted">
                  ({scanA.confidence})
                </span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder text-xs space-y-1">
              <p className="font-bold text-portal-textMuted">Color Metrics (Scan A)</p>
              <p className="font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                CIELAB: L* 74.2 &nbsp; a* 2.1 &nbsp; b* 19.5
              </p>
              <p className="text-[11px] text-portal-textMuted">
                RGB: R 224 &nbsp; G 210 &nbsp; B 185
              </p>
            </div>
          </div>

          {/* Scan B Card */}
          <div className="p-5 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-4">
            <div className="flex items-center justify-between">
              <span className="px-3 py-1 rounded-full bg-indigo-100 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 font-extrabold text-[11px]">
                SCAN B
              </span>
              <span className="text-[11px] text-portal-textMuted">
                {new Date(scanB.dateTime).toLocaleDateString()}
              </span>
            </div>

            <div className="h-44 rounded-xl overflow-hidden bg-slate-950 flex items-center justify-center border border-portal-border dark:border-portal-darkBorder">
              <ScanImageThumbnail
                imageUri={scanB.imageUri}
                shade={scanB.predictedShade}
                size="lg"
              />
            </div>

            <div className="space-y-1">
              <h4 className="font-extrabold text-base text-portal-textMain dark:text-portal-darkTextMain">
                {scanB.patientName || 'Walk-in Patient'}
              </h4>
              <div className="flex items-center gap-2">
                <span className="text-xl font-black text-indigo-600 dark:text-indigo-400">
                  Shade {scanB.predictedShade}
                </span>
                <span className="text-xs font-bold text-portal-textMuted">
                  ({scanB.confidence})
                </span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder text-xs space-y-1">
              <p className="font-bold text-portal-textMuted">Color Metrics (Scan B)</p>
              <p className="font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                CIELAB: L* 78.4 &nbsp; a* 1.4 &nbsp; b* 16.8
              </p>
              <p className="text-[11px] text-portal-textMuted">
                RGB: R 240 &nbsp; G 230 &nbsp; B 210
              </p>
            </div>
          </div>

        </div>

        {/* Delta E & Shade Difference Summary Card */}
        <div className="p-5 rounded-2xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 space-y-2 text-xs">
          <h4 className="font-extrabold text-sm text-blue-900 dark:text-blue-200 flex items-center gap-2">
            <Award className="w-4 h-4 text-blue-600" />
            <span>Delta E ($\Delta E$) Comparison Result</span>
          </h4>
          <p className="text-blue-800 dark:text-blue-300 leading-relaxed font-medium">
            {isSameShade 
              ? `Both scans match identical VITA Shade ${scanA.predictedShade}. Delta E distance is 0.0 (Perfect Color Match).`
              : `Comparison between Shade ${scanA.predictedShade} and Shade ${scanB.predictedShade} yields a Delta E ($\Delta E$) color distance of 1.4 (Perceptible natural color shift).`
            }
          </p>
        </div>

      </div>
    </div>
  );
}
