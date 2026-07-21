import React, { useEffect, useState } from 'react';
import { validateDentalImage, classifyToothShade } from '../../utils/shadeAnalyzer';
import { Scan, Sparkles, AlertTriangle, RefreshCw, ArrowRight } from 'lucide-react';

export default function ProcessingOverlay({ imageSrc, onComplete, onFail }) {
  const [step, setStep] = useState(0);
  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [validationResult, setValidationResult] = useState(null);

  const steps = [
    'Initializing ShadeScan AI Engine...',
    'Validating dental enamel & tooth structure...',
    'Sampling CIE L*a*b* color coordinates...',
    'Evaluating VITA Classical shade Delta E distance...',
    'Finalizing clinical confidence breakdown...'
  ];

  useEffect(() => {
    let isMounted = true;

    async function runValidationAndScan() {
      // Step 1: Progress step animation
      setStep(0);
      await new Promise(r => setTimeout(r, 600));
      if (!isMounted) return;

      // Step 2: Run strict dental image validation
      setStep(1);
      const val = await validateDentalImage(imageSrc);
      setValidationResult(val);
      await new Promise(r => setTimeout(r, 600));
      if (!isMounted) return;

      if (!val.isValid) {
        // Show detection inconclusive alert dialog matching Android app
        setShowOverrideModal(true);
        return;
      }

      // Step 3 & 4: Proceed with AI shade classification
      setStep(2);
      await new Promise(r => setTimeout(r, 500));
      if (!isMounted) return;

      setStep(3);
      await new Promise(r => setTimeout(r, 500));
      if (!isMounted) return;

      setStep(4);
      const results = await classifyToothShade(imageSrc);
      await new Promise(r => setTimeout(r, 400));
      if (!isMounted) return;

      onComplete(results);
    }

    runValidationAndScan();

    return () => { isMounted = false; };
  }, [imageSrc]);

  const handleContinueAnyway = async () => {
    setShowOverrideModal(false);
    setStep(3);
    const results = await classifyToothShade(imageSrc);
    onComplete(results);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 text-center py-6 relative">
      
      {/* Scanner Box */}
      <div className="relative rounded-3xl overflow-hidden bg-slate-900 border-2 border-blue-500 shadow-2xl shadow-blue-500/20 max-h-[360px] max-w-md mx-auto flex items-center justify-center">
        {imageSrc ? (
          <img
            src={imageSrc}
            alt="Scanning target"
            className="w-full h-[320px] object-cover opacity-80"
          />
        ) : (
          <div className="w-full h-[320px] bg-slate-950 flex items-center justify-center text-slate-700">
            <Scan className="w-20 h-20" />
          </div>
        )}

        {/* Animated Scanning Laser Line */}
        {!showOverrideModal && (
          <div className="absolute inset-x-0 h-1 bg-gradient-to-r from-transparent via-cyan-400 to-transparent shadow-[0_0_15px_#22d3ee] animate-scan-line pointer-events-none" />
        )}

        {/* Floating Badge */}
        <div className="absolute top-4 left-4 px-3 py-1 rounded-full bg-blue-600/90 text-white text-[11px] font-extrabold flex items-center gap-1.5 backdrop-blur-md">
          <Sparkles className="w-3.5 h-3.5 text-amber-300 animate-spin" />
          <span>AI ANALYZING</span>
        </div>
      </div>

      {/* Progress Steps */}
      {!showOverrideModal && (
        <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 max-w-md mx-auto shadow-lg space-y-4">
          <div className="flex items-center justify-center gap-2">
            <div className="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
            <h3 className="font-extrabold text-base text-portal-textMain dark:text-portal-darkTextMain">
              {steps[step]}
            </h3>
          </div>

          <div className="w-full h-2 rounded-full bg-slate-100 dark:bg-slate-800 overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-blue-600 to-indigo-600 transition-all duration-500 rounded-full"
              style={{ width: `${((step + 1) / steps.length) * 100}%` }}
            />
          </div>

          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted font-medium">
            Calibrating against 16 VITA Classical shade standards
          </p>
        </div>
      )}

      {/* Detection Inconclusive Warning Modal (Matching Android App DashboardActivity.kt) */}
      {showOverrideModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-2xl space-y-4 text-left">
            
            <div className="w-12 h-12 rounded-2xl bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400 flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div>
              <h3 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                Detection Inconclusive
              </h3>
              <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1 leading-relaxed">
                {validationResult?.message || "AI could not verify clear dental or tooth structures with high confidence. Do you want to retake the photo or continue anyway?"}
              </p>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3">
              <button
                onClick={onFail}
                className="flex-1 py-3 px-4 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs flex items-center justify-center gap-2 transition-colors"
              >
                <RefreshCw className="w-4 h-4" />
                <span>Retake Photo</span>
              </button>

              <button
                onClick={handleContinueAnyway}
                className="flex-1 py-3 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-md transition-colors"
              >
                <span>Continue Anyway</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
