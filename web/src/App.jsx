import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';

import Navbar from './components/common/Navbar';
import Sidebar from './components/common/Sidebar';
import ScanImageThumbnail from './components/common/ScanImageThumbnail';

import LoginForm from './components/auth/LoginForm';
import RegisterForm from './components/auth/RegisterForm';
import ForgotPasswordModal from './components/auth/ForgotPasswordModal';

import DashboardOverview from './components/dashboard/DashboardOverview';
import ImageUploader from './components/scan/ImageUploader';
import ProcessingOverlay from './components/scan/ProcessingOverlay';
import ShadeResultCard from './components/scan/ShadeResultCard';

import PatientList from './components/patients/PatientList';
import PatientFormModal from './components/patients/PatientFormModal';
import PatientDetail from './components/patients/PatientDetail';

import VitaShadeGuide from './components/guide/VitaShadeGuide';
import ProfileSettings from './components/settings/ProfileSettings';
import CompareScansModal from './components/scan/CompareScansModal';

import { getPatients, savePatient, updatePatient, getScanHistory, saveScanReport, deleteScanReport } from './utils/storageService';
import { generateClinicalReportPDF } from './utils/pdfGenerator';
import { Trash2, Download, Scan, Eye, X, Award, Layers } from 'lucide-react';

function AppRoutes() {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Data State
  const [patients, setPatients] = useState([]);
  const [scans, setScans] = useState([]);
  
  // Scan Flow State
  const [scanImage, setScanImage] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [scanResult, setScanResult] = useState(null);

  // Patient Modals & Selections
  const [showPatientModal, setShowPatientModal] = useState(false);
  const [editingPatient, setEditingPatient] = useState(null);
  const [selectedPatient, setSelectedPatient] = useState(null);

  // Image Preview Modal State
  const [previewModalImage, setPreviewModalImage] = useState(null);
  const [compareModalScan, setCompareModalScan] = useState(null);

  // Fetch Patients & Scans on mount & user change
  useEffect(() => {
    if (currentUser?.uid) {
      loadData();
    }
  }, [currentUser?.uid]);

  const loadData = async () => {
    const userId = currentUser?.uid;
    const pList = await getPatients(userId);
    const sList = await getScanHistory(userId);
    setPatients(pList);
    setScans(sList);
  };

  const handleSavePatient = async (patientData) => {
    const userId = currentUser?.uid;
    if (patientData.id) {
      await updatePatient(patientData.id, patientData, userId);
    } else {
      await savePatient(patientData, userId);
    }
    await loadData();
    setShowPatientModal(false);
    setEditingPatient(null);
  };

  const handleStartScanWithImage = (imageSrc) => {
    if (isProcessing) return; // Prevent duplicate analysis requests
    setScanImage(imageSrc);
    setIsProcessing(true);
    setScanResult(null);
  };

  const handleProcessingComplete = (results) => {
    setScanResult(results);
    setIsProcessing(false);
  };

  const handleScanFail = () => {
    setScanImage(null);
    setScanResult(null);
    setIsProcessing(false);
    navigate('/', { replace: true });
  };

  const handleSaveScanReport = async (reportData) => {
    const userId = currentUser?.uid;
    await saveScanReport(reportData, userId);
    await loadData();
    setScanImage(null);
    setScanResult(null);
    navigate('/', { replace: true });
  };

  const handleDiscardAnalysis = () => {
    if (window.confirm('Discard this analysis?\nUnsaved shade analysis data will be lost.')) {
      setScanImage(null);
      setScanResult(null);
      navigate('/', { replace: true });
    }
  };

  const handleDeleteScan = async (scanId) => {
    if (window.confirm('Are you sure you want to delete this scan record?')) {
      const userId = currentUser?.uid;
      await deleteScanReport(scanId, userId);
      await loadData();
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-portal-bg dark:bg-portal-darkBg text-portal-textMain dark:text-portal-darkTextMain transition-colors">
      
      <Navbar 
        onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
        sidebarOpen={sidebarOpen}
      />

      <div className="flex-1 flex">
        <Sidebar 
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
        />

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto w-full overflow-x-hidden">
          <Routes>
            
            {/* Dashboard Route */}
            <Route 
              path="/" 
              element={
                <DashboardOverview 
                  patients={patients} 
                  scans={scans} 
                  onSelectPatient={(p) => setSelectedPatient(p)}
                />
              } 
            />

            {/* AI Scan Route */}
            <Route 
              path="/scan" 
              element={
                isProcessing ? (
                  <ProcessingOverlay
                    imageSrc={scanImage}
                    onComplete={handleProcessingComplete}
                    onFail={handleScanFail}
                  />
                ) : scanResult ? (
                  <ShadeResultCard
                    result={scanResult}
                    imageSrc={scanImage}
                    patients={patients}
                    onScanAgain={() => {
                      setScanImage(null);
                      setScanResult(null);
                    }}
                    onSaveReport={handleSaveScanReport}
                    onDeleteScan={() => {
                      handleDiscardAnalysis();
                    }}
                    onCompareScan={() => {
                      setCompareModalScan({
                        predictedShade: scanResult.topShade,
                        confidence: scanResult.confidence,
                        dateTime: Date.now(),
                        imageUri: scanImage,
                        patientName: 'Current Scan'
                      });
                    }}
                  />
                ) : (
                  <ImageUploader
                    onImageSelected={handleStartScanWithImage}
                  />
                )
              } 
            />

            {/* Patients Directory */}
            <Route 
              path="/patients" 
              element={
                selectedPatient ? (
                  <PatientDetail
                    patient={selectedPatient}
                    scans={scans}
                    onBack={() => setSelectedPatient(null)}
                    onEditPatient={(p) => {
                      setEditingPatient(p);
                      setShowPatientModal(true);
                    }}
                    onNewScanForPatient={(p) => {
                      setSelectedPatient(null);
                      navigate('/scan');
                    }}
                  />
                ) : (
                  <PatientList
                    patients={patients}
                    onAddPatient={() => {
                      setEditingPatient(null);
                      setShowPatientModal(true);
                    }}
                    onEditPatient={(p) => {
                      setEditingPatient(p);
                      setShowPatientModal(true);
                    }}
                    onSelectPatient={(p) => setSelectedPatient(p)}
                  />
                )
              } 
            />

            {/* History Table with Image & Swatch Thumbnails */}
            <Route 
              path="/history" 
              element={
                <div className="space-y-6">
                  <div>
                    <h1 className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                      Clinical Scan History
                    </h1>
                    <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                      Complete archive of all evaluated VITA tooth shade scans
                    </p>
                  </div>

                  <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm">
                    {scans.length > 0 ? (
                      <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                          <thead>
                            <tr className="border-b border-portal-border dark:border-portal-darkBorder text-[11px] font-bold uppercase tracking-wider text-portal-textMuted dark:text-portal-darkTextMuted">
                              <th className="pb-3 pl-2">Tooth Scan / Swatch</th>
                              <th className="pb-3">Patient Name</th>
                              <th className="pb-3">Predicted Shade</th>
                              <th className="pb-3">Confidence</th>
                              <th className="pb-3">Scan Date</th>
                              <th className="pb-3 pr-2 text-right">Actions</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-portal-border dark:divide-portal-darkBorder text-sm">
                            {scans.map((scan) => (
                              <tr key={scan.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                                
                                {/* Tooth Image or Swatch Thumbnail */}
                                <td className="py-3.5 pl-2">
                                  <ScanImageThumbnail
                                    imageUri={scan.imageUri}
                                    shade={scan.predictedShade}
                                    scanId={scan.id}
                                    size="md"
                                    onClick={() => setPreviewModalImage(scan)}
                                  />
                                </td>

                                <td className="py-3.5 font-bold text-portal-textMain dark:text-portal-darkTextMain">
                                  {scan.patientName || 'Walk-in Patient'}
                                </td>

                                <td className="py-3.5">
                                  <span className="px-3 py-1 rounded-xl bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-extrabold text-xs border border-blue-200 dark:border-blue-800">
                                    Shade {scan.predictedShade}
                                  </span>
                                </td>

                                <td className="py-3.5 text-xs text-portal-textMuted dark:text-portal-darkTextMuted font-semibold">
                                  {scan.confidence}
                                </td>

                                <td className="py-3.5 text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                                  {new Date(scan.dateTime).toLocaleString()}
                                </td>

                                <td className="py-3.5 pr-2 text-right space-x-2">
                                  <button
                                    onClick={() => setCompareModalScan(scan)}
                                    className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 hover:bg-indigo-100 text-indigo-600 dark:text-indigo-400 transition-colors"
                                    title="Compare with another scan"
                                  >
                                    <Layers className="w-4 h-4" />
                                  </button>

                                  <button
                                    onClick={() => generateClinicalReportPDF({
                                      scanResult: scan,
                                      patient: { name: scan.patientName },
                                      doctorInfo: { name: currentUser?.fullName }
                                    })}
                                    className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-blue-600 dark:text-blue-400 transition-colors"
                                    title="Download PDF Report"
                                  >
                                    <Download className="w-4 h-4" />
                                  </button>

                                  <button
                                    onClick={() => handleDeleteScan(scan.id)}
                                    className="p-2 rounded-xl bg-red-50 dark:bg-red-950/40 hover:bg-red-100 text-red-600 dark:text-red-400 transition-colors"
                                    title="Soft Delete Scan (Move to Recycle Bin)"
                                  >
                                    <Trash2 className="w-4 h-4" />
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <div className="text-center py-12 space-y-3">
                        <Scan className="w-10 h-10 text-portal-textMuted mx-auto opacity-40" />
                        <p className="text-sm font-semibold text-portal-textMuted">No scan history recorded yet.</p>
                      </div>
                    )}
                  </div>
                </div>
              } 
            />

            {/* VITA Guide */}
            <Route path="/vita-guide" element={<VitaShadeGuide />} />

            {/* Settings */}
            <Route path="/settings" element={<ProfileSettings onReloadData={loadData} />} />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/" replace />} />

          </Routes>
        </main>
      </div>

      {/* Add / Edit Patient Modal */}
      {showPatientModal && (
        <PatientFormModal
          initialPatient={editingPatient}
          onSave={handleSavePatient}
          onClose={() => {
            setShowPatientModal(false);
            setEditingPatient(null);
          }}
        />
      )}

      {/* Image Zoom Preview Modal */}
      {previewModalImage && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-lg bg-white dark:bg-portal-darkCard rounded-3xl p-6 border border-portal-border dark:border-portal-darkBorder shadow-2xl space-y-4 relative">
            
            <button
              onClick={() => setPreviewModalImage(null)}
              className="absolute top-4 right-4 p-2 rounded-full bg-slate-100 dark:bg-slate-800 text-portal-textMuted hover:text-portal-textMain"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3">
              <Award className="w-6 h-6 text-blue-600" />
              <div>
                <h3 className="font-extrabold text-base text-portal-textMain dark:text-portal-darkTextMain">
                  {previewModalImage.patientName} — Shade {previewModalImage.predictedShade}
                </h3>
                <p className="text-xs text-portal-textMuted">
                  Confidence: {previewModalImage.confidence} • {new Date(previewModalImage.dateTime).toLocaleDateString()}
                </p>
              </div>
            </div>

            <div className="rounded-2xl overflow-hidden bg-slate-950 border border-portal-border dark:border-portal-darkBorder min-h-[200px] flex items-center justify-center p-4">
              <ScanImageThumbnail
                imageUri={previewModalImage.imageUri}
                shade={previewModalImage.predictedShade}
                size="lg"
              />
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => {
                  generateClinicalReportPDF({
                    scanResult: previewModalImage,
                    patient: { name: previewModalImage.patientName },
                    doctorInfo: { name: currentUser?.fullName }
                  });
                }}
                className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center gap-2 shadow-md"
              >
                <Download className="w-4 h-4" />
                <span>Export PDF Report</span>
              </button>
            </div>

          </div>
        </div>
      )}

      {/* Compare Scans Modal */}
      {compareModalScan && (
        <CompareScansModal
          initialScan={compareModalScan}
          allScans={scans}
          onClose={() => setCompareModalScan(null)}
        />
      )}

    </div>
  );
}

function AppContent() {
  const { currentUser, loading } = useAuth();
  const [authMode, setAuthMode] = useState('login');
  const [authEmail, setAuthEmail] = useState('');
  const [authMessage, setAuthMessage] = useState('');
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');

  // 1. Session verification loading state
  if (loading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 dark:bg-portal-darkBg p-4">
        <div className="w-10 h-10 border-3 border-blue-600 border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-xs font-semibold text-portal-textMuted dark:text-portal-darkTextMuted tracking-wide">
          Verifying ShadeScan AI Session...
        </p>
      </div>
    );
  }

  // 2. Unauthenticated user -> Strictly render Sign In / Register routes ONLY
  if (!currentUser) {
    return (
      <BrowserRouter>
        <Routes>
          <Route 
            path="/register" 
            element={
              <RegisterForm 
                prefilledEmail={authEmail}
                onSwitchToLogin={(email, msg) => {
                  if (email) setAuthEmail(email);
                  if (msg) setAuthMessage(msg);
                  setAuthMode('login');
                }} 
              />
            } 
          />
          <Route 
            path="*" 
            element={
              <>
                {authMode === 'register' ? (
                  <RegisterForm
                    prefilledEmail={authEmail}
                    onSwitchToLogin={(email, msg) => {
                      if (email) setAuthEmail(email);
                      if (msg) setAuthMessage(msg);
                      setAuthMode('login');
                    }}
                  />
                ) : (
                  <LoginForm
                    initialEmail={authEmail}
                    authMessage={authMessage}
                    onSwitchToRegister={(email) => {
                      if (email) setAuthEmail(email);
                      setAuthMessage('');
                      setAuthMode('register');
                    }}
                    onForgotPassword={(email) => {
                      setForgotEmail(email);
                      setShowForgotModal(true);
                    }}
                  />
                )}

                {showForgotModal && (
                  <ForgotPasswordModal
                    initialEmail={forgotEmail}
                    onClose={() => setShowForgotModal(false)}
                  />
                )}
              </>
            } 
          />
        </Routes>
      </BrowserRouter>
    );
  }

  // 3. Authenticated user -> Access Protected App Routes (Dashboard)
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </ThemeProvider>
  );
}
