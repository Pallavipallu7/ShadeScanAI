import React, { useState } from 'react';
import { 
  Users, 
  Search, 
  UserPlus, 
  Phone, 
  Calendar, 
  ChevronRight, 
  Edit3, 
  FileText,
  Scan
} from 'lucide-react';

export default function PatientList({ 
  patients = [], 
  onAddPatient, 
  onEditPatient, 
  onSelectPatient 
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('all');

  const filteredPatients = patients
    .filter(p => 
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (p.phone && p.phone.includes(searchTerm))
    )
    .sort((a, b) => {
      if (sortBy === 'name-asc') return a.name.localeCompare(b.name);
      if (sortBy === 'name-desc') return b.name.localeCompare(a.name);
      if (sortBy === 'most-scans') return (b.scanCount || 0) - (a.scanCount || 0);
      return 0;
    });

  return (
    <div className="space-y-6">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
            Patient Directory
          </h1>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
            Manage clinical profiles, contact info, and linked shade reports
          </p>
        </div>

        <button
          onClick={onAddPatient}
          className="px-5 py-3 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-xs shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 transition-transform active:scale-95 self-start sm:self-auto"
        >
          <UserPlus className="w-4 h-4" />
          <span>Add New Patient</span>
        </button>
      </div>

      {/* Search & Sort Controls */}
      <div className="bg-white dark:bg-portal-darkCard p-4 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-sm space-y-3">
        <div className="relative">
          <Search className="w-5 h-5 absolute left-3.5 top-3 text-portal-textMuted" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search patients by name or phone number..."
            className="w-full pl-11 pr-4 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>

        {/* Sorting Chips */}
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <span className="text-xs font-bold text-portal-textMuted mr-1">Sort:</span>
          {[
            { id: 'all', label: 'All' },
            { id: 'name-asc', label: 'Name A-Z' },
            { id: 'name-desc', label: 'Name Z-A' },
            { id: 'most-scans', label: 'Most Scans' }
          ].map(chip => (
            <button
              key={chip.id}
              onClick={() => setSortBy(chip.id)}
              className={`px-3 py-1.5 rounded-full text-xs font-bold transition-colors ${
                sortBy === chip.id
                  ? 'bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800'
                  : 'bg-slate-50 dark:bg-slate-900 text-portal-textMuted border border-portal-border dark:border-portal-darkBorder hover:bg-slate-100'
              }`}
            >
              {chip.label}
            </button>
          ))}
        </div>
      </div>

      {/* Patient Grid */}
      {filteredPatients.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredPatients.map((patient) => (
            <div
              key={patient.id}
              onClick={() => onSelectPatient(patient)}
              className="group cursor-pointer p-5 rounded-3xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder hover:border-blue-500 dark:hover:border-blue-500 shadow-sm transition-all hover:shadow-md space-y-4"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-2xl bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 font-extrabold flex items-center justify-center text-lg border border-blue-200 dark:border-blue-800">
                    {patient.name.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <h3 className="font-extrabold text-base text-portal-textMain dark:text-portal-darkTextMain group-hover:text-blue-600 transition-colors">
                      {patient.name}
                    </h3>
                    <span className="inline-block px-2 py-0.5 mt-1 rounded-md bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 font-bold text-[11px]">
                      {patient.age ? `${patient.age} yrs` : 'Age N/A'} • {patient.gender || 'Male'}
                    </span>
                  </div>
                </div>

                <button
                  onClick={(e) => { e.stopPropagation(); onEditPatient(patient); }}
                  className="p-2 rounded-xl text-portal-textMuted hover:text-portal-textMain hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                  title="Edit Patient"
                >
                  <Edit3 className="w-4 h-4" />
                </button>
              </div>

              <div className="flex items-center justify-between text-xs font-bold text-blue-600 dark:text-blue-400 pt-2 border-t border-portal-border dark:border-portal-darkBorder">
                <span>View Profile & History</span>
                <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-16 bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder space-y-3">
          <Users className="w-12 h-12 text-portal-textMuted mx-auto opacity-40" />
          <h3 className="font-bold text-base text-portal-textMain dark:text-portal-darkTextMain">
            No patients match your search
          </h3>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
            Try searching a different name or add a new patient to the record.
          </p>
          <button
            onClick={onAddPatient}
            className="px-5 py-2.5 rounded-xl bg-blue-600 text-white font-bold text-xs shadow-md"
          >
            Add Patient
          </button>
        </div>
      )}

    </div>
  );
}
