import React from 'react';
import { generateClinicalReportPDF } from '../../utils/pdfGenerator';
import ScanImageThumbnail from '../common/ScanImageThumbnail';
import { 
  ArrowLeft, 
  Phone, 
  FileText, 
  Scan, 
  Download, 
  Edit3
} from 'lucide-react';

export default function PatientDetail({ 
  patient, 
  scans = [], 
  onBack, 
  onEditPatient, 
  onNewScanForPatient 
}) {
  const patientScans = scans.filter(s => s.patientId === patient.id);

  const handleExportPDF = (scan) => {
    generateClinicalReportPDF({
      scanResult: scan,
      patient: patient,
      doctorInfo: { name: 'Dr. Practitioner' }
    });
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      
      {/* Back Button */}
      <button
        onClick={onBack}
        className="flex items-center gap-2 text-sm font-semibold text-portal-textMuted dark:text-portal-darkTextMuted hover:text-portal-textMain transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Patient Directory</span>
      </button>

      {/* Patient Profile Summary Card */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-xl p-6 sm:p-8 space-y-6">
        
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-portal-border dark:border-portal-darkBorder pb-6">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-3xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-extrabold flex items-center justify-center text-2xl shadow-lg shadow-blue-500/20">
              {patient.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <h1 className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                {patient.name}
              </h1>
              <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted font-medium mt-0.5">
                {patient.gender || 'N/A'}, {patient.age ? `${patient.age} years old` : 'Age not specified'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => onEditPatient(patient)}
              className="px-4 py-2.5 rounded-xl border border-portal-border dark:border-portal-darkBorder hover:bg-slate-50 dark:hover:bg-slate-800 text-portal-textMain font-bold text-xs flex items-center gap-1.5 transition-colors"
            >
              <Edit3 className="w-4 h-4" />
              <span>Edit Details</span>
            </button>
            <button
              onClick={() => onNewScanForPatient(patient)}
              className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md flex items-center gap-1.5 transition-colors"
            >
              <Scan className="w-4 h-4" />
              <span>New Scan</span>
            </button>
          </div>
        </div>

        {/* Info Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-1">
            <p className="font-bold text-portal-textMuted dark:text-portal-darkTextMuted flex items-center gap-1.5">
              <Phone className="w-3.5 h-3.5 text-blue-600" />
              <span>Contact Mobile</span>
            </p>
            <p className="font-bold text-sm text-portal-textMain dark:text-portal-darkTextMain">
              {patient.phone || 'No phone number on file'}
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-1">
            <p className="font-bold text-portal-textMuted dark:text-portal-darkTextMuted flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-indigo-600" />
              <span>Clinical Notes & Restorative Target</span>
            </p>
            <p className="font-medium text-sm text-portal-textMain dark:text-portal-darkTextMain">
              {patient.notes || 'No clinical notes recorded'}
            </p>
          </div>
        </div>

      </div>

      {/* Patient Scan History Section */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-sm p-6 sm:p-8 space-y-6">
        
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
              Patient Shade Scan History ({patientScans.length})
            </h2>
            <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
              Chronological history of VITA tooth shade evaluations
            </p>
          </div>
        </div>

        {patientScans.length > 0 ? (
          <div className="space-y-4">
            {patientScans.map((scan) => (
              <div
                key={scan.id}
                className="p-5 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div className="flex items-center gap-4">
                  <ScanImageThumbnail
                    imageUri={scan.imageUri}
                    shade={scan.predictedShade}
                    size="lg"
                  />

                  <div>
                    <div className="flex items-center gap-2">
                      <span className="px-3 py-1 rounded-xl bg-blue-600 text-white font-extrabold text-xs">
                        Shade {scan.predictedShade}
                      </span>
                      <span className="text-xs font-bold text-portal-textMuted">
                        Confidence {scan.confidence}
                      </span>
                    </div>
                    <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
                      Evaluated on {new Date(scan.dateTime).toLocaleString()}
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => handleExportPDF(scan)}
                  className="px-4 py-2.5 rounded-xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder hover:bg-blue-50 text-blue-600 dark:text-blue-400 font-bold text-xs flex items-center gap-2 transition-colors self-start sm:self-auto"
                >
                  <Download className="w-4 h-4" />
                  <span>Download PDF Report</span>
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 space-y-3">
            <Scan className="w-10 h-10 text-portal-textMuted mx-auto opacity-40" />
            <p className="text-sm font-semibold text-portal-textMuted">
              No scans recorded for {patient.name} yet.
            </p>
            <button
              onClick={() => onNewScanForPatient(patient)}
              className="px-4 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs shadow-md"
            >
              Perform First Scan
            </button>
          </div>
        )}

      </div>

    </div>
  );
}
