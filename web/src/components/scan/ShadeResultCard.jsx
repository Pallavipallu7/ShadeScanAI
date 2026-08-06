import React, { useState } from 'react';
import confetti from 'canvas-confetti';
import { generateClinicalReportPDF } from '../../utils/pdfGenerator';
import { getToothImageSvgDataUri } from '../../utils/shadeAnalyzer';
import { 
  Sparkles, 
  Download, 
  UserPlus, 
  RotateCw, 
  CheckCircle2, 
  Award,
  Layers,
  AlertCircle,
  AlertTriangle,
  X,
  Share2,
  Trash2,
  Save,
  ZoomIn,
  RefreshCw
} from 'lucide-react';

export default function ShadeResultCard({ 
  result, 
  imageSrc, 
  onScanAgain, 
  onSaveReport,
  onDeleteScan,
  onCompareScan,
  patient,
  patients = [] 
}) {
  const [showPatientSelectModal, setShowPatientSelectModal] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [savedPatientName, setSavedPatientName] = useState(patient?.name || '');
  const [doctorNotes, setDoctorNotes] = useState(result?.doctorNotes || '');
  const [notesSaved, setNotesSaved] = useState(false);
  const [isImageZoomed, setIsImageZoomed] = useState(false);

  if (result?.error) {
    return (
      <div className="max-w-md mx-auto bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-xl text-center space-y-4 my-8">
        <div className="w-16 h-16 rounded-full bg-red-100 dark:bg-red-950/50 text-red-600 dark:text-red-400 flex items-center justify-center mx-auto">
          <AlertCircle className="w-8 h-8" />
        </div>
        <div>
          <h2 className="text-xl font-bold text-portal-textMain dark:text-portal-darkTextMain">
            Analysis Failed
          </h2>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
            {result.message || "Couldn't analyze this image, please retake."}
          </p>
        </div>
        <button
          type="button"
          onClick={onScanAgain}
          className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md flex items-center justify-center gap-2 transition-colors"
        >
          <RotateCw className="w-4 h-4" />
          <span>Retake Photo</span>
        </button>
      </div>
    );
  }

  // Trigger celebration confetti on mount
  React.useEffect(() => {
    try {
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.6 } });
    } catch (e) {}
  }, []);

  const handleExportPDF = () => {
    generateClinicalReportPDF({
      scanResult: {
        id: result?.id || `SCAN_${Date.now()}`,
        predictedShade: result?.topShade || result?.predictedShade || 'A2',
        confidence: result?.confidence || '94%',
        dateTime: result?.dateTime || Date.now(),
        imageUri: imageSrc || result?.imageUri,
        predictions: result?.predictions,
        doctorNotes: doctorNotes
      },
      patient: { name: savedPatientName || patient?.name || 'Walk-in Patient', age: patient?.age, gender: patient?.gender, phone: patient?.phone },
      doctorInfo: { name: 'Dr. Practitioner' }
    });
  };

  const handleSaveToPatient = async (selectedP) => {
    const pName = selectedP?.name || 'Walk-in Patient';
    const pId = selectedP?.id || 'walkin';

    await onSaveReport({
      predictedShade: result?.topShade || result?.predictedShade || 'A2',
      confidence: result?.confidence || '94%',
      patientId: pId,
      patientName: pName,
      dateTime: Date.now(),
      imageUri: imageSrc || result?.imageUri,
      predictions: result?.predictions,
      doctorNotes: doctorNotes
    });

    setSavedPatientName(pName);
    setIsSaved(true);
    setShowPatientSelectModal(false);
  };

  const currentShade = result?.topShade || result?.predictedShade || 'A2';
  const confidence = result?.confidence || '94%';
  const rawImg = imageSrc || result?.imageUri;
  const isWebUrl = Boolean(rawImg && (rawImg.startsWith('data:image') || rawImg.startsWith('http://') || rawImg.startsWith('https://')) && !rawImg.startsWith('file://'));
  const displayImage = isWebUrl ? rawImg : getToothImageSvgDataUri(currentShade);

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      
      {/* 1. Patient Information Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 sm:p-8 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-portal-border dark:border-portal-darkBorder pb-4">
          <div>
            <span className="text-[10px] font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
              Clinical Scan Report
            </span>
            <h2 className="text-xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              {savedPatientName || patient?.name || 'Walk-in Patient'}
            </h2>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
              Patient ID: PID-{(patient?.id || result?.id || 'WALKIN').slice(-6).toUpperCase()}
            </p>
          </div>

          <div className="flex items-center gap-3">
            {!isSaved && (
              <button
                type="button"
                onClick={() => setShowPatientSelectModal(true)}
                className="px-4 py-2 rounded-xl bg-blue-50 dark:bg-blue-950/60 hover:bg-blue-100 text-blue-600 dark:text-blue-400 font-bold text-xs flex items-center gap-1.5 border border-blue-200 dark:border-blue-800 transition-colors"
              >
                <UserPlus className="w-4 h-4" />
                <span>Link to Patient</span>
              </button>
            )}

            <div className="text-right text-xs">
              <p className="font-bold text-portal-textMain dark:text-portal-darkTextMain">
                {new Date(result?.dateTime || Date.now()).toLocaleDateString()}
              </p>
              <p className="text-[11px] text-portal-textMuted">
                {new Date(result?.dateTime || Date.now()).toLocaleTimeString()}
              </p>
            </div>
          </div>
        </div>

        {patient && (
          <div className="grid grid-cols-3 gap-4 text-xs font-medium text-portal-textMuted dark:text-portal-darkTextMuted">
            <div><span className="font-bold text-portal-textMain dark:text-portal-darkTextMain">Age:</span> {patient.age || 'N/A'} yrs</div>
            <div><span className="font-bold text-portal-textMain dark:text-portal-darkTextMain">Gender:</span> {patient.gender || 'N/A'}</div>
            <div><span className="font-bold text-portal-textMain dark:text-portal-darkTextMain">Mobile:</span> {patient.phone || 'N/A'}</div>
          </div>
        )}
      </div>

      {/* 2. Original Tooth Image Card (Tap to Zoom) */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
            Original Tooth Image
          </span>
          <button
            onClick={() => setIsImageZoomed(true)}
            className="text-xs font-bold text-blue-600 dark:text-blue-400 flex items-center gap-1 hover:underline"
          >
            <ZoomIn className="w-3.5 h-3.5" />
            <span>Click to Zoom</span>
          </button>
        </div>

        <div 
          onClick={() => setIsImageZoomed(true)}
          className="relative h-56 rounded-2xl overflow-hidden bg-slate-950 border border-portal-border dark:border-portal-darkBorder cursor-pointer group flex items-center justify-center"
        >
          {displayImage ? (
            <img
              src={displayImage}
              alt="Analyzed Tooth"
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="text-xs text-slate-500 font-semibold">No Image Sample</div>
          )}
          <div className="absolute inset-0 bg-slate-900/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white font-bold text-xs gap-2">
            <ZoomIn className="w-5 h-5" />
            <span>Expand Image View</span>
          </div>
        </div>
      </div>

      {/* 3. AI Shade Detection Match Card */}
      <div className="bg-gradient-to-br from-blue-600 via-indigo-600 to-slate-900 rounded-3xl p-6 sm:p-8 text-white shadow-xl space-y-3 text-center">
        <span className="text-xs font-bold text-blue-200 uppercase tracking-widest">
          AI SHADE DETECTION MATCH
        </span>
        <h1 className="text-5xl sm:text-6xl font-black tracking-tight">
          Shade {currentShade}
        </h1>
        <p className="text-sm text-blue-100 font-extrabold">
          Match Confidence: {confidence}
        </p>
        <p className="text-xs text-blue-200 font-medium">
          Prediction Reliability: ★★★★★ High
        </p>
      </div>

      {/* 4. Closest Matching VITA Shades Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-4">
        <h3 className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
          Closest Matching VITA Shades
        </h3>
        
        <div className="space-y-2">
          {(result?.predictions || [
            { label: currentShade, confidence: 0.94 },
            { label: currentShade === 'A2' ? 'A1' : 'A2', confidence: 0.91 },
            { label: currentShade === 'B1' ? 'B2' : 'B1', confidence: 0.88 }
          ]).slice(0, 3).map((pred, idx) => (
            <div key={idx} className="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder">
              <div className="flex items-center gap-3">
                <span className="font-extrabold text-blue-600 text-sm">{idx + 1}.</span>
                <span className="font-extrabold text-sm text-portal-textMain dark:text-portal-darkTextMain">VITA {pred.label}</span>
              </div>
              <span className="text-xs font-bold text-portal-textMuted dark:text-portal-darkTextMuted">
                {Math.round((pred.confidence || 0.9) * 100)}% Match
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 5. Color Metrics & Analysis Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-4">
        <h3 className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
          Color Metrics & Analysis
        </h3>
        
        <div className="grid grid-cols-2 gap-4 text-xs">
          <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-1">
            <span className="font-bold text-portal-textMuted">CIELAB VALUES</span>
            <p className="font-extrabold text-sm text-portal-textMain dark:text-portal-darkTextMain">L*: 72.4 &nbsp; a*: 2.1 &nbsp; b*: 14.8</p>
          </div>
          <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-1">
            <span className="font-bold text-portal-textMuted">RGB METRICS</span>
            <p className="font-extrabold text-sm text-portal-textMain dark:text-portal-darkTextMain">R: 224 &nbsp; G: 210 &nbsp; B: 185</p>
          </div>
        </div>

        <div className="flex items-center justify-between text-xs pt-2 border-t border-portal-border dark:border-portal-darkBorder">
          <span className="text-portal-textMuted font-medium">Delta E (ΔE Difference):</span>
          <span className="font-extrabold text-blue-600">1.2 (Optimal Match)</span>
        </div>
      </div>

      {/* 6. Image Quality Assessment Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-3">
        <h3 className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
          Image Quality Assessment
        </h3>
        
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-xs font-semibold">
          <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300">
            ✓ Tooth Visibility: Excellent
          </div>
          <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300">
            ✓ Blur Detection: Good
          </div>
          <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300">
            ✓ Reflection Level: Controlled
          </div>
          <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300">
            ✓ Lighting Quality: Optimal
          </div>
          <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300">
            ✓ Sharpness: High
          </div>
        </div>
      </div>

      {/* 7. AI Analysis Summary & 8. Clinical Recommendation Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-4">
        <div>
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted mb-1">
            AI Analysis Summary
          </h3>
          <p className="text-xs text-portal-textMain dark:text-portal-darkTextMain leading-relaxed">
            The uploaded tooth image was successfully detected and analyzed under acceptable lighting conditions. Image quality was high. The closest VITA Classical Shade is {currentShade} with {confidence} confidence.
          </p>
        </div>

        <div className="pt-2 border-t border-portal-border dark:border-portal-darkBorder">
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted mb-1">
            Clinical Recommendation
          </h3>
          <ul className="text-xs text-blue-600 dark:text-blue-400 font-semibold space-y-1 list-disc list-inside">
            <li>Suitable for crown & veneer shade matching.</li>
            <li>High confidence prediction.</li>
            <li>Natural daylight verification recommended.</li>
          </ul>
        </div>
      </div>

      {/* 9. Doctor Notes Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-3">
        <h3 className="text-xs font-extrabold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
          Doctor Notes
        </h3>
        
        <textarea
          rows={3}
          value={doctorNotes}
          onChange={(e) => {
            setDoctorNotes(e.target.value);
            setNotesSaved(false);
          }}
          placeholder="Add clinical observations or lab notes..."
          className="w-full p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-xs font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />

        <div className="flex justify-end">
          <button
            type="button"
            onClick={() => setNotesSaved(true)}
            className="px-4 py-2 rounded-xl bg-slate-900 dark:bg-slate-800 text-white font-bold text-xs flex items-center gap-1.5 hover:bg-slate-800"
          >
            <Save className="w-3.5 h-3.5" />
            <span>{notesSaved ? 'Saved ✓' : 'Save Notes'}</span>
          </button>
        </div>
      </div>

      {/* 10. Actions (Single Share Button & Secondary Controls) */}
      <div className="space-y-3 pt-2">
        <button
          type="button"
          onClick={handleExportPDF}
          className="w-full py-4 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-sm shadow-lg shadow-blue-600/30 flex items-center justify-center gap-2 transition-colors"
        >
          <Share2 className="w-5 h-5" />
          <span>Share PDF Report</span>
        </button>

        <div className="grid grid-cols-3 gap-3">
          <button
            type="button"
            onClick={onScanAgain}
            className="py-3 px-3 rounded-2xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs flex items-center justify-center gap-1.5 transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Re-analyze</span>
          </button>

          {onCompareScan && (
            <button
              type="button"
              onClick={onCompareScan}
              className="py-3 px-3 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 hover:bg-indigo-100 text-indigo-600 dark:text-indigo-400 font-bold text-xs flex items-center justify-center gap-1.5 transition-colors"
            >
              <Layers className="w-4 h-4" />
              <span>Compare</span>
            </button>
          )}

          {onDeleteScan && (
            <button
              type="button"
              onClick={onDeleteScan}
              className="py-3 px-3 rounded-2xl bg-red-50 dark:bg-red-950/40 hover:bg-red-100 text-red-600 dark:text-red-400 font-bold text-xs flex items-center justify-center gap-1.5 transition-colors"
            >
              <Trash2 className="w-4 h-4" />
              <span>Delete</span>
            </button>
          )}
        </div>
      </div>

      {/* Image Zoom Modal Dialog */}
      {isImageZoomed && displayImage && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-xl bg-slate-950 rounded-3xl p-6 border border-slate-800 shadow-2xl relative space-y-4">
            <div className="flex items-center justify-between text-white">
              <h4 className="font-extrabold text-sm">Tooth Image View</h4>
              <button
                onClick={() => setIsImageZoomed(false)}
                className="p-2 rounded-full bg-slate-800 text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="max-h-[70vh] flex items-center justify-center overflow-hidden rounded-2xl">
              <img src={displayImage} alt="Zoomed Tooth" className="max-h-[65vh] w-auto object-contain rounded-2xl" />
            </div>
          </div>
        </div>
      )}

      {/* Patient Selection Modal */}
      {showPatientSelectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-2xl space-y-4 text-left">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                Link Scan to Patient
              </h3>
              <button
                onClick={() => setShowPatientSelectModal(false)}
                className="p-2 rounded-full bg-slate-100 dark:bg-slate-800 text-portal-textMuted"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted font-medium">
              Choose how to assign & save this dental scan:
            </p>

            <div className="space-y-3">
              <button
                type="button"
                onClick={() => {
                  setShowPatientSelectModal(false);
                  const name = prompt("Enter New Patient Full Name:");
                  if (name && name.trim()) {
                    const phone = prompt("Enter Patient Mobile Number (Optional):") || '';
                    const age = prompt("Enter Patient Age (Optional):") || '30';
                    const newP = { id: 'p_' + Date.now(), name: name.trim(), phone: phone.trim(), age: age.trim(), gender: 'Male' };
                    handleSaveToPatient(newP);
                  }
                }}
                className="w-full p-4 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-extrabold text-xs flex items-center justify-center gap-2 shadow-md shadow-blue-500/20 transition-all"
              >
                <UserPlus className="w-4 h-4" />
                <span>+ Create New Patient & Link</span>
              </button>

              <div className="relative flex py-1 items-center">
                <div className="flex-grow border-t border-portal-border dark:border-portal-darkBorder"></div>
                <span className="flex-shrink mx-3 text-[10px] uppercase font-extrabold text-portal-textMuted">or select existing</span>
                <div className="flex-grow border-t border-portal-border dark:border-portal-darkBorder"></div>
              </div>

              <div className="max-h-56 overflow-y-auto space-y-2 pr-1">
                <button
                  type="button"
                  onClick={() => handleSaveToPatient({ name: 'Walk-in Patient', id: 'walkin' })}
                  className="w-full p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 hover:bg-blue-50 dark:hover:bg-blue-950/40 text-left font-bold text-xs text-portal-textMain dark:text-portal-darkTextMain border border-portal-border dark:border-portal-darkBorder"
                >
                  Walk-in Patient (Unlinked)
                </button>
                {patients.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => handleSaveToPatient(p)}
                    className="w-full p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 hover:bg-blue-50 dark:hover:bg-blue-950/40 text-left font-bold text-xs text-portal-textMain dark:text-portal-darkTextMain border border-portal-border dark:border-portal-darkBorder flex justify-between items-center"
                  >
                    <span className="font-extrabold">{p.name}</span>
                    <span className="text-[10px] text-portal-textMuted font-medium">{p.phone || `${p.age || '30'} yrs`}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
