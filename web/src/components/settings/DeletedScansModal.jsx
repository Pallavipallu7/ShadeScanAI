import React, { useState, useEffect } from 'react';
import { getDeletedScanHistory, restoreScanReport, permanentlyDeleteScanReport, deleteAllDeletedScanReports } from '../../utils/storageService';
import { useAuth } from '../../context/AuthContext';
import { Trash2, RotateCcw, AlertTriangle, X, Scan, CheckCircle2 } from 'lucide-react';

export default function DeletedScansModal({ onClose, onScanRestored }) {
  const { currentUser } = useAuth();
  const [deletedScans, setDeletedScans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showConfirmDeleteAll, setShowConfirmDeleteAll] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  useEffect(() => {
    loadDeletedScans();
  }, [currentUser?.uid]);

  const loadDeletedScans = async () => {
    setLoading(true);
    const list = await getDeletedScanHistory(currentUser?.uid);
    setDeletedScans(list);
    setLoading(false);
  };

  const handleRestore = async (scanId) => {
    await restoreScanReport(scanId, currentUser?.uid);
    setToastMessage("Scan restored to active history!");
    setTimeout(() => setToastMessage(null), 2500);
    await loadDeletedScans();
    if (onScanRestored) onScanRestored();
  };

  const handlePermanentDelete = async (scanId) => {
    if (window.confirm("Are you sure you want to permanently delete this scan? This cannot be undone.")) {
      await permanentlyDeleteScanReport(scanId, currentUser?.uid);
      setToastMessage("Scan permanently deleted.");
      setTimeout(() => setToastMessage(null), 2500);
      await loadDeletedScans();
    }
  };

  const handleDeleteAll = async () => {
    await deleteAllDeletedScanReports(currentUser?.uid);
    setShowConfirmDeleteAll(false);
    setToastMessage("All deleted scans removed from Recycle Bin.");
    setTimeout(() => setToastMessage(null), 2500);
    await loadDeletedScans();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md animate-in fade-in">
      <div className="w-full max-w-2xl bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-2xl relative space-y-5 max-h-[90vh] flex flex-col">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-portal-border dark:border-portal-darkBorder pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 flex items-center justify-center">
              <Trash2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                Deleted Scans (Recycle Bin)
              </h2>
              <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                Recoverable for 30 days before permanent deletion
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {deletedScans.length > 0 && (
              <button
                onClick={() => setShowConfirmDeleteAll(true)}
                className="px-3 py-1.5 rounded-xl bg-red-50 dark:bg-red-950/40 text-red-600 dark:text-red-400 hover:bg-red-100 font-bold text-xs transition-colors"
              >
                Empty Recycle Bin
              </button>
            )}

            <button
              onClick={onClose}
              className="p-2 rounded-full bg-slate-100 dark:bg-slate-800 text-portal-textMuted hover:text-portal-textMain"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Toast Alert */}
        {toastMessage && (
          <div className="p-3 rounded-2xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300 font-bold text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Deleted List Content */}
        <div className="flex-1 overflow-y-auto pr-1 space-y-3">
          {loading ? (
            <div className="text-center py-12 space-y-3">
              <div className="w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto" />
              <p className="text-xs font-semibold text-portal-textMuted">Loading Recycle Bin...</p>
            </div>
          ) : deletedScans.length === 0 ? (
            <div className="text-center py-16 space-y-3 bg-slate-50 dark:bg-slate-900/50 rounded-2xl border border-dashed border-portal-border dark:border-portal-darkBorder">
              <Scan className="w-10 h-10 text-portal-textMuted mx-auto opacity-40" />
              <p className="text-sm font-bold text-portal-textMain dark:text-portal-darkTextMain">
                Recycle Bin is Empty
              </p>
              <p className="text-xs text-portal-textMuted">
                Soft-deleted scan records will appear here for 30 days.
              </p>
            </div>
          ) : (
            deletedScans.map((record) => (
              <div
                key={record.id}
                className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder flex items-center justify-between gap-4"
              >
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 font-extrabold text-sm flex items-center justify-center border border-blue-200 dark:border-blue-800">
                    {record.predictedShade}
                  </div>
                  <div>
                    <h4 className="font-extrabold text-sm text-portal-textMain dark:text-portal-darkTextMain">
                      {record.patientName}
                    </h4>
                    <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                      Shade {record.predictedShade} • {record.confidence} confidence
                    </p>
                    <p className="text-[10px] text-portal-textMuted dark:text-portal-darkTextMuted">
                      Deleted: {new Date(record.deletedAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleRestore(record.id)}
                    className="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center gap-1.5 shadow-sm transition-colors"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Restore</span>
                  </button>

                  <button
                    onClick={() => handlePermanentDelete(record.id)}
                    className="px-3 py-1.5 rounded-xl bg-red-100 dark:bg-red-950/60 hover:bg-red-200 text-red-600 dark:text-red-400 font-bold text-xs transition-colors"
                  >
                    <span>Delete</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Empty Recycle Bin Confirmation Dialog */}
        {showConfirmDeleteAll && (
          <div className="absolute inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-sm rounded-3xl animate-in fade-in">
            <div className="w-full max-w-md bg-white dark:bg-portal-darkCard rounded-2xl p-6 border border-portal-border dark:border-portal-darkBorder shadow-2xl space-y-4 text-left">
              <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-950 text-red-600 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                  Empty Recycle Bin?
                </h3>
                <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
                  Are you sure you want to permanently delete all items in Deleted Scans? This action cannot be undone.
                </p>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  onClick={() => setShowConfirmDeleteAll(false)}
                  className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs"
                >
                  Cancel
                </button>
                <button
                  onClick={handleDeleteAll}
                  className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs shadow-md"
                >
                  Delete All Permanently
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
