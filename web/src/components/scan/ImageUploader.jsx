import React, { useState, useRef } from 'react';
import { getToothImageSvgDataUri, validateDentalImage } from '../../utils/shadeAnalyzer';
import { 
  UploadCloud, 
  Camera, 
  Image as ImageIcon, 
  Sparkles, 
  X, 
  RotateCw, 
  Sliders, 
  CheckCircle2, 
  AlertCircle 
} from 'lucide-react';

export default function ImageUploader({ onImageSelected }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [showWebcam, setShowWebcam] = useState(false);
  const [validationError, setValidationError] = useState('');
  const [isValidating, setIsValidating] = useState(false);

  // Canvas brightness adjustment
  const [brightness, setBrightness] = useState(100);

  const fileInputRef = useRef(null);
  const videoRef = useRef(null);
  const streamRef = useRef(null);

  const SAMPLE_IMAGES = [
    { label: 'Sample Tooth A2 (Natural)', url: getToothImageSvgDataUri('A2') },
    { label: 'Sample Tooth B1 (Bright Bleach)', url: getToothImageSvgDataUri('B1') },
    { label: 'Sample Tooth C2 (Greyish Tone)', url: getToothImageSvgDataUri('C2') },
    { label: 'Sample Tooth D3 (Warm Neutral)', url: getToothImageSvgDataUri('D3') }
  ];

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      processFile(file);
    }
  };

  const processFile = (file) => {
    setValidationError('');
    if (!file.type.startsWith('image/')) {
      setValidationError("No tooth detected. Please upload or capture a clear image of the patient's teeth.");
      return;
    }
    const reader = new FileReader();
    reader.onload = (event) => {
      setPreviewUrl(event.target.result);
      setSelectedFile(file);
    };
    reader.readAsDataURL(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processFile(e.dataTransfer.files[0]);
    }
  };

  const startWebcam = async () => {
    setShowWebcam(true);
    setValidationError('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      setValidationError('Could not access camera. Please check camera permissions.');
      setShowWebcam(false);
    }
  };

  const stopWebcam = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
    }
    setShowWebcam(false);
  };

  const capturePhoto = () => {
    if (videoRef.current) {
      const canvas = document.createElement('canvas');
      canvas.width = videoRef.current.videoWidth || 640;
      canvas.height = videoRef.current.videoHeight || 480;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(videoRef.current, 0, 0);
      const dataUrl = canvas.toDataURL('image/jpeg');
      setPreviewUrl(dataUrl);
      setValidationError('');
      stopWebcam();
    }
  };

  const handleSelectSample = (sampleUrl) => {
    setPreviewUrl(sampleUrl);
    setValidationError('');
  };

  const handleContinueToAnalysis = async () => {
    if (!previewUrl) {
      setValidationError("Please capture or upload a clear tooth image before analysis.");
      return;
    }

    setValidationError('');
    setIsValidating(true);

    try {
      const valResult = await validateDentalImage(previewUrl);
      if (!valResult.isValid) {
        setValidationError(valResult.message);
        setIsValidating(false);
        return;
      }
      // Validation passed -> proceed to analysis
      onImageSelected(previewUrl);
    } catch (err) {
      setValidationError("No tooth detected. Please upload or capture a clear image of the patient's teeth.");
    } finally {
      setIsValidating(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      
      {/* Page Title */}
      <div className="text-center space-y-2">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
          Upload Dental Image
        </h1>
        <p className="text-sm text-portal-textMuted dark:text-portal-darkTextMuted">
          Select or capture a clear photo of the patient's teeth for AI shade analysis
        </p>
      </div>

      {/* Error Notification Banner */}
      {validationError && (
        <div className="p-4 rounded-2xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 font-bold text-xs flex items-center gap-3 animate-in fade-in">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{validationError}</span>
        </div>
      )}

      {/* Main Upload / Camera / Preview Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl p-6 sm:p-8 space-y-6">
        
        {!previewUrl && !showWebcam && (
          <div
            onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
            onDragLeave={() => setIsDragOver(false)}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-3xl p-8 text-center transition-all cursor-pointer flex flex-col items-center justify-center min-h-[280px] ${
              isDragOver
                ? 'border-blue-500 bg-blue-50/50 dark:bg-blue-950/40'
                : 'border-portal-border dark:border-portal-darkBorder hover:border-blue-400 bg-slate-50/50 dark:bg-slate-900/50'
            }`}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileChange}
              accept="image/*"
              className="hidden"
            />

            <div className="w-16 h-16 rounded-3xl bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center shadow-lg shadow-blue-500/10 mb-4">
              <UploadCloud className="w-8 h-8" />
            </div>

            <p className="text-base font-bold text-portal-textMain dark:text-portal-darkTextMain">
              Drag & Drop Dental Photo Here
            </p>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1 mb-4">
              Supports JPEG, PNG, WEBP files up to 10MB
            </p>

            <div className="flex items-center gap-3">
              <button
                type="button"
                className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition-colors"
              >
                Browse Files
              </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); startWebcam(); }}
                className="px-5 py-2.5 rounded-xl bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs flex items-center gap-1.5 transition-colors"
              >
                <Camera className="w-4 h-4" />
                <span>Use Camera</span>
              </button>
            </div>
          </div>
        )}

        {/* Live Webcam Overlay */}
        {showWebcam && (
          <div className="space-y-4 text-center">
            <div className="relative rounded-2xl overflow-hidden bg-black aspect-video max-w-lg mx-auto">
              <video
                ref={videoRef}
                autoPlay
                playsInline
                className="w-full h-full object-cover"
              />
              {/* Reference Card Zone Overlay */}
              <div className="absolute top-4 left-4 border-2 border-dashed border-cyan-400 rounded-lg w-24 h-16 pointer-events-none flex items-center justify-center bg-cyan-950/40 backdrop-blur-xs">
                <span className="text-cyan-300 font-extrabold text-[8px] uppercase tracking-wider text-center px-1">Reference Card</span>
              </div>

              {/* Tooth ROI Center Box */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="border-2 border-dashed border-amber-400 rounded-2xl w-36 h-36 flex items-center justify-center bg-amber-400/10">
                  <span className="text-amber-300 font-extrabold text-[9px] uppercase tracking-wider">Tooth Target</span>
                </div>
              </div>

              <button
                onClick={stopWebcam}
                className="absolute top-3 right-3 p-2 rounded-full bg-black/60 text-white hover:bg-black z-10"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex justify-center gap-3">
              <button
                onClick={capturePhoto}
                className="px-6 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-lg flex items-center gap-2"
              >
                <Camera className="w-5 h-5" />
                <span>Capture Dental Photo</span>
              </button>
            </div>
          </div>
        )}

        {/* Image Preview & ROI Adjustment */}
        {previewUrl && !showWebcam && (
          <div className="space-y-6">
            <div className="relative rounded-2xl overflow-hidden bg-slate-900 border border-portal-border dark:border-portal-darkBorder max-h-[380px] flex items-center justify-center">
              <img
                src={previewUrl}
                alt="Tooth Preview"
                style={{ filter: `brightness(${brightness}%)` }}
                className="max-h-[360px] w-auto object-contain"
              />

              {/* Target Scan ROI Box */}
              <div className="absolute border-2 border-dashed border-amber-400 rounded-xl w-44 h-44 pointer-events-none flex items-center justify-center shadow-2xl">
                <div className="bg-amber-400/20 text-amber-300 font-extrabold text-[10px] px-2 py-0.5 rounded-full uppercase tracking-wider">
                  Target Tooth ROI
                </div>
              </div>

              {/* Reference Card Calibration Guide Box */}
              <div className="absolute top-4 left-4 border-2 border-dashed border-cyan-400 rounded-lg w-28 h-20 pointer-events-none flex items-center justify-center bg-cyan-950/40 backdrop-blur-xs">
                <div className="text-cyan-300 font-extrabold text-[9px] px-1.5 py-0.5 text-center uppercase tracking-wider">
                  Reference Card Zone
                </div>
              </div>

              <button
                onClick={() => { setPreviewUrl(null); setValidationError(''); }}
                className="absolute top-3 right-3 p-2 rounded-full bg-slate-900/80 hover:bg-slate-900 text-white transition-colors z-10"
                title="Remove photo"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Calibration Guide Banner */}
            <div className="p-3.5 rounded-2xl bg-cyan-50 dark:bg-cyan-950/40 border border-cyan-200 dark:border-cyan-800 flex items-start gap-3">
              <div className="w-2.5 h-2.5 rounded-full bg-cyan-500 mt-1 shrink-0 animate-pulse" />
              <p className="text-xs text-cyan-900 dark:text-cyan-200 font-semibold leading-relaxed">
                <span className="font-extrabold">Color Calibration Guidance:</span> Position a neutral grey reference card or physical VITA shade tab within the top-left reference zone to enable automatic white-balance correction.
              </p>
            </div>

            {/* Brightness Adjustment Slider */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-2">
              <div className="flex items-center justify-between text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain">
                <div className="flex items-center gap-2">
                  <Sliders className="w-4 h-4 text-blue-600" />
                  <span>Exposure / Brightness Adjustment</span>
                </div>
                <span>{brightness}%</span>
              </div>
              <input
                type="range"
                min="70"
                max="130"
                value={brightness}
                onChange={(e) => setBrightness(Number(e.target.value))}
                className="w-full h-2 bg-slate-200 dark:bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-600"
              />
            </div>

            {/* Action Buttons */}
            <div className="flex items-center justify-between pt-2">
              <button
                onClick={() => { setPreviewUrl(null); setValidationError(''); }}
                className="px-5 py-3 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-portal-textMain dark:text-portal-darkTextMain font-semibold text-sm transition-colors"
              >
                Retake / Change Image
              </button>

              <button
                onClick={handleContinueToAnalysis}
                disabled={!previewUrl || isValidating}
                className="px-8 py-3.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 flex items-center gap-2 transition-transform active:scale-95 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed"
              >
                {isValidating ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <Sparkles className="w-5 h-5 text-amber-300" />
                    <span>Run AI Shade Analysis</span>
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* Quick Sample Dental Photos */}
        {!previewUrl && !showWebcam && (
          <div className="pt-4 border-t border-portal-border dark:border-portal-darkBorder">
            <p className="text-xs font-bold text-portal-textMuted dark:text-portal-darkTextMuted uppercase tracking-wider mb-3">
              Or Test With Sample Clinical Dental Photos:
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {SAMPLE_IMAGES.map((sample, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSelectSample(sample.url)}
                  className="p-2.5 rounded-2xl border border-portal-border dark:border-portal-darkBorder hover:border-blue-500 bg-slate-50 dark:bg-slate-900 flex items-center gap-3 transition-colors text-left"
                >
                  <img
                    src={sample.url}
                    alt={sample.label}
                    className="w-12 h-12 rounded-xl object-cover"
                  />
                  <div>
                    <p className="text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain">
                      {sample.label}
                    </p>
                    <p className="text-[10px] text-portal-textMuted">Click to load</p>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
