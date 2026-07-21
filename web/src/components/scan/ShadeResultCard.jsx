import React, { useState } from 'react';
import confetti from 'canvas-confetti';
import { generateClinicalReportPDF } from '../../utils/pdfGenerator';
import { 
  Sparkles, 
  Download, 
  UserPlus, 
  RotateCw, 
  CheckCircle2, 
  Award,
  Layers,
  ArrowRight,
  X
} from 'lucide-react';

export default function ShadeResultCard({ 
  result, 
  imageSrc, 
  onScanAgain, 
  onSaveReport,
  patients = [] 
}) {
  const [showPatientSelectModal, setShowPatientSelectModal] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [savedPatientName, setSavedPatientName] = useState('');

  // Trigger celebration confetti on mount
  React.useEffect(() => {
    try {
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.6 } });
    } catch (e) {}
  }, []);

  const handleExportPDF = () => {
    generateClinicalReportPDF({
      scanResult: {
        id: `SCAN_${Date.now()}`,
        predictedShade: result.topShade,
        confidence: result.confidence,
        dateTime: Date.now(),
        imageUri: imageSrc,
        predictions: result.predictions
      },
      patient: { name: savedPatientName || 'Walk-in Patient' },
      doctorInfo: { name: 'Dr. Practitioner' }
    });
  };

  const handleSaveToPatient = async (patient) => {
    const pName = patient?.name || 'Walk-in Patient';
    const pId = patient?.id || 'walkin';

    await onSaveReport({
      predictedShade: result.topShade,
      confidence: result.confidence,
      patientId: pId,
      patientName: pName,
      dateTime: Date.now(),
      imageUri: imageSrc,
      predictions: result.predictions
    });

    setSavedPatientName(pName);
    setIsSaved(true);
    setShowPatientSelectModal(false);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      
      {/* Top Banner Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl overflow-hidden">
        <div className="p-6 sm:p-8 bg-gradient-to-br from-blue-600 via-indigo-600 to-slate-900 text-white flex flex-col md:flex-row items-center justify-between gap-6">
          
          <div className="space-y-2 text-center md:text-left">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-xs font-semibold backdrop-blur-md">
              <Award className="w-4 h-4 text-amber-300" />
              <span>VITA Classical Shade Match</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              Shade {result.topShade}
            </h1>
            <p className="text-sm text-blue-100 font-medium max-w-md">
              {result.description}
            </p>
          </div>

          {/* Confidence Badge & Color Swatch */}
          <div className="flex items-center gap-4 bg-white/10 p-4 rounded-2xl border border-white/20 backdrop-blur-md">
            <div 
              className="w-16 h-16 rounded-2xl shadow-inner border-2 border-white"
              style={{ backgroundColor: result.predictions[0]?.hex || '#F0E6D2' }}
              title={`Color swatch for shade ${result.topShade}`}
            />
            <div>
              <p className="text-[11px] uppercase font-bold text-blue-200">AI Confidence</p>
              <p className="text-3xl font-extrabold text-white">{result.confidence}</p>
              <p className="text-[10px] text-blue-100">Match Probability</p>
            </div>
          </div>

        </div>

        {/* Content Body Grid */}
        <div className="p-6 sm:p-8 grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Column 1: Captured Image Preview & Color Coordinates */}
          <div className="space-y-4">
            <h3 className="font-extrabold text-sm uppercase tracking-wider text-portal-textMain dark:text-portal-darkTextMain flex items-center gap-2">
              <Layers className="w-4 h-4 text-blue-600" />
              <span>Evaluated Tooth Region</span>
            </h3>

            <div className="rounded-2xl overflow-hidden border border-portal-border dark:border-portal-darkBorder bg-slate-900 max-h-64 flex items-center justify-center">
              <img
                src={imageSrc}
                alt="Analyzed Tooth"
                className="max-h-64 w-auto object-contain"
              />
            </div>

            {/* CIE L*a*b* & RGB Color Coordinates */}
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder">
                <p className="font-bold text-portal-textMuted dark:text-portal-darkTextMuted">
                  CIE L*a*b* Space
                </p>
                <p className="font-mono font-bold text-portal-textMain dark:text-portal-darkTextMain mt-0.5">
                  L:{result.lab?.L} a:{result.lab?.a} b:{result.lab?.b}
                </p>
              </div>

              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder">
                <p className="font-bold text-portal-textMuted dark:text-portal-darkTextMuted">
                  Mean RGB Color
                </p>
                <p className="font-mono font-bold text-portal-textMain dark:text-portal-darkTextMain mt-0.5">
                  R:{result.rgb?.r} G:{result.rgb?.g} B:{result.rgb?.b}
                </p>
              </div>
            </div>
          </div>

          {/* Column 2: Candidate Probability Breakdown Chart */}
          <div className="space-y-4">
            <h3 className="font-extrabold text-sm uppercase tracking-wider text-portal-textMain dark:text-portal-darkTextMain flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-600" />
              <span>Candidate Shade Probabilities</span>
            </h3>

            <div className="space-y-3">
              {result.predictions?.map((pred, idx) => (
                <div key={pred.label} className="space-y-1">
                  <div className="flex items-center justify-between text-xs font-bold">
                    <div className="flex items-center gap-2">
                      <span 
                        className="w-3.5 h-3.5 rounded-full border border-slate-300"
                        style={{ backgroundColor: pred.hex || '#E2E8F0' }}
                      />
                      <span className="text-portal-textMain dark:text-portal-darkTextMain">
                        Shade {pred.label}
                      </span>
                    </div>
                    <span className="text-portal-textMuted dark:text-portal-darkTextMuted">
                      {Math.round(pred.confidence * 100)}%
                    </span>
                  </div>

                  <div className="w-full h-2.5 rounded-full bg-slate-100 dark:bg-slate-800 overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        idx === 0 
                          ? 'bg-gradient-to-r from-blue-600 to-indigo-600' 
                          : 'bg-slate-400 dark:bg-slate-600'
                      }`}
                      style={{ width: `${Math.round(pred.confidence * 100)}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>

            {/* Quick Action buttons */}
            <div className="pt-4 space-y-3">
              <button
                type="button"
                onClick={() => setShowPatientSelectModal(true)}
                disabled={isSaved}
                className={`w-full py-3.5 rounded-xl font-bold text-sm shadow-md flex items-center justify-center gap-2 transition-all ${
                  isSaved
                    ? 'bg-emerald-600 text-white cursor-default'
                    : 'bg-blue-600 hover:bg-blue-700 text-white'
                }`}
              >
                {isSaved ? (
                  <>
                    <CheckCircle2 className="w-5 h-5" />
                    <span>Report Saved to {savedPatientName || 'Patient Record'}!</span>
                  </>
                ) : (
                  <>
                    <UserPlus className="w-5 h-5" />
                    <span>Save & Link to Patient Record</span>
                  </>
                )}
              </button>

              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={handleExportPDF}
                  className="py-3 px-4 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs flex items-center justify-center gap-2 transition-colors"
                >
                  <Download className="w-4 h-4 text-blue-600" />
                  <span>Export PDF Report</span>
                </button>

                <button
                  type="button"
                  onClick={onScanAgain}
                  className="py-3 px-4 rounded-xl border border-portal-border dark:border-portal-darkBorder hover:bg-slate-50 dark:hover:bg-slate-800 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs flex items-center justify-center gap-2 transition-colors"
                >
                  <RotateCw className="w-4 h-4 text-indigo-600" />
                  <span>Scan Another Image</span>
                </button>
              </div>
            </div>

          </div>

        </div>
      </div>

      {/* Patient Selection Modal */}
      {showPatientSelectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-portal-darkCard rounded-3xl p-6 border border-portal-border dark:border-portal-darkBorder shadow-2xl space-y-4 relative">
            <button
              onClick={() => setShowPatientSelectModal(false)}
              className="absolute top-4 right-4 p-1 text-portal-textMuted hover:text-portal-textMain"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Link Scan to Patient Record
            </h3>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
              Select a patient from your clinical database or save as a walk-in patient.
            </p>

            <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
              <button
                type="button"
                onClick={() => handleSaveToPatient({ name: 'Walk-in Patient', id: 'walkin' })}
                className="w-full p-3 rounded-xl border border-portal-border dark:border-portal-darkBorder hover:bg-blue-50 dark:hover:bg-blue-950 text-left font-semibold text-xs transition-colors"
              >
                + Save as Anonymous Walk-in Patient
              </button>

              {patients.map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => handleSaveToPatient(p)}
                  className="w-full p-3 rounded-xl border border-portal-border dark:border-portal-darkBorder hover:bg-blue-50 dark:hover:bg-blue-950 text-left transition-colors flex items-center justify-between"
                >
                  <div>
                    <p className="font-bold text-sm text-portal-textMain dark:text-portal-darkTextMain">{p.name}</p>
                    <p className="text-[11px] text-portal-textMuted">{p.gender}, {p.age} yrs • {p.phone || 'No phone'}</p>
                  </div>
                  <ArrowRight className="w-4 h-4 text-blue-600" />
                </button>
              ))}
            </div>

            <button
              type="button"
              onClick={() => setShowPatientSelectModal(false)}
              className="w-full py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

    </div>
  );
}
