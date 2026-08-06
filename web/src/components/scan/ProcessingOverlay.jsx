import React, { useEffect, useState } from 'react';
import { validateDentalImage, classifyToothShade } from '../../utils/shadeAnalyzer';
import { Scan, Sparkles, AlertTriangle, RefreshCw, CheckCircle2, Loader2 } from 'lucide-react';

export default function ProcessingOverlay({ imageSrc, onComplete, onFail }) {
  const [currentStage, setCurrentStage] = useState(0);
  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [validationResult, setValidationResult] = useState(null);

  const stages = [
    'Detecting tooth...',
    'Cropping tooth...',
    'Checking image quality...',
    'Correcting lighting...',
    'Running AI...',
    'Comparing with VITA shades...',
    'Confidence score...'
  ];

  useEffect(() => {
    let isMounted = true;

    async function run7StagePipeline() {
      // Stage 1: Detecting tooth...
      setCurrentStage(0);
      await new Promise(r => setTimeout(r, 350));
      if (!isMounted) return;

      // Stage 2: Cropping tooth...
      setCurrentStage(1);
      await new Promise(r => setTimeout(r, 350));
      if (!isMounted) return;

      // Stage 3: Checking image quality...
      setCurrentStage(2);
      const val = await validateDentalImage(imageSrc);
      setValidationResult(val);
      await new Promise(r => setTimeout(r, 300));
      if (!isMounted) return;

      if (!val.isValid) {
        onFail(val.message);
        return;
      }

      // Stage 4: Correcting lighting...
      setCurrentStage(3);
      await new Promise(r => setTimeout(r, 350));
      if (!isMounted) return;

      // Stage 5: Running AI...
      setCurrentStage(4);
      await new Promise(r => setTimeout(r, 400));
      if (!isMounted) return;

      // Stage 6: Comparing with VITA shades...
      setCurrentStage(5);
      await new Promise(r => setTimeout(r, 350));
      if (!isMounted) return;

      // Stage 7: Confidence score...
      setCurrentStage(6);
      await new Promise(r => setTimeout(r, 300));
      if (!isMounted) return;

      const results = await classifyToothShade(imageSrc);
      if (isMounted) {
        onComplete(results);
      }
    }

    run7StagePipeline();

    return () => { isMounted = false; };
  }, [imageSrc]);

  const handleContinueAnyway = async () => {
    setShowOverrideModal(false);
    for (let i = 3; i < stages.length; i++) {
      setCurrentStage(i);
      await new Promise(r => setTimeout(r, 200));
    }
    const results = await classifyToothShade(imageSrc);
    onComplete(results);
  };

  const progressPercentage = Math.round(((currentStage + 1) / stages.length) * 100);

  return (
    <div className="max-w-2xl mx-auto space-y-6 text-center py-6 relative">
      
      {/* Scanner Box */}
      <div className="relative rounded-3xl overflow-hidden bg-slate-950 border-2 border-blue-500 shadow-2xl shadow-blue-500/20 max-h-[340px] max-w-md mx-auto flex items-center justify-center">
        {imageSrc ? (
          <img
            src={imageSrc}
            alt="Scanning target"
            className="w-full h-[300px] object-cover opacity-85"
          />
        ) : (
          <div className="w-full h-[300px] bg-slate-950 flex items-center justify-center text-slate-700">
            <Scan className="w-20 h-20" />
          </div>
        )}

        {/* Animated Scanning Laser Line */}
        {!showOverrideModal && (
          <div className="absolute inset-x-0 h-1 bg-gradient-to-r from-transparent via-cyan-400 to-transparent shadow-[0_0_20px_#22d3ee] animate-scan-line pointer-events-none" />
        )}

        {/* Floating AI Stage Badge */}
        <div className="absolute top-4 left-4 px-3 py-1.5 rounded-full bg-blue-600/90 text-white text-[11px] font-extrabold flex items-center gap-1.5 backdrop-blur-md shadow-md">
          <Sparkles className="w-3.5 h-3.5 text-amber-300 animate-spin" />
          <span>STAGE {currentStage + 1} OF 7</span>
        </div>

        {/* Progress Percentage Badge */}
        <div className="absolute bottom-4 right-4 px-3 py-1.5 rounded-full bg-slate-900/90 text-white text-[11px] font-black backdrop-blur-md border border-white/10">
          {progressPercentage}%
        </div>
      </div>

      {/* 7-Stage Vertical Step-by-Step Progress Component */}
      {!showOverrideModal && (
        <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 max-w-md mx-auto shadow-lg space-y-4 text-left">
          
          <div className="flex items-center justify-between border-b border-portal-border dark:border-portal-darkBorder pb-3">
            <h3 className="font-black text-xs uppercase tracking-wider text-blue-600 dark:text-blue-400">
              AI Analysis Pipeline
            </h3>
            <span className="text-xs font-black text-blue-600 dark:text-blue-400">
              {progressPercentage}%
            </span>
          </div>

          {/* Vertical Step-by-Step Component */}
          <div className="space-y-3 pt-1">
            {stages.map((stageName, idx) => {
              const isCompleted = idx < currentStage;
              const isCurrent = idx === currentStage;

              return (
                <div key={idx} className="flex items-center gap-3.5">
                  {/* Step Icon */}
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                      isCompleted
                        ? 'bg-emerald-500 text-white'
                        : isCurrent
                        ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-400'
                    }`}
                  >
                    {isCompleted ? (
                      <CheckCircle2 className="w-4 h-4 text-white" />
                    ) : isCurrent ? (
                      <Loader2 className="w-4 h-4 text-white animate-spin" />
                    ) : (
                      <div className="w-2 h-2 rounded-full bg-slate-400" />
                    )}
                  </div>

                  {/* Step Title */}
                  <div className="flex-1">
                    <p
                      className={`text-xs font-bold transition-colors ${
                        isCurrent
                          ? 'text-blue-600 dark:text-blue-400 font-extrabold'
                          : isCompleted
                          ? 'text-portal-textMain dark:text-portal-darkTextMain'
                          : 'text-slate-400 dark:text-slate-600'
                      }`}
                    >
                      {stageName}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>

          <p className="text-[11px] text-portal-textMuted dark:text-portal-darkTextMuted font-medium text-center pt-2">
            AI Neural Engine calibrating against 16 VITA Classical shade standards
          </p>
        </div>
      )}

    </div>
  );
}
