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

  const filteredPatients = patients.filter(p => 
    p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (p.phone && p.phone.includes(searchTerm))
  );

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

      {/* Search Input Bar */}
      <div className="bg-white dark:bg-portal-darkCard p-4 rounded-3xl border border-portal-border dark:border-portal-darkBorder shadow-sm">
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
      </div>

      {/* Patient Grid */}
      {filteredPatients.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredPatients.map((patient) => (
            <div
              key={patient.id}
              onClick={() => onSelectPatient(patient)}
              className="group cursor-pointer p-6 rounded-3xl bg-white dark:bg-portal-darkCard border border-portal-border dark:border-portal-darkBorder hover:border-blue-500 dark:hover:border-blue-500 shadow-sm transition-all hover:shadow-md space-y-4"
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
                    <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted font-medium">
                      {patient.gender || 'N/A'}, {patient.age ? `${patient.age} yrs` : 'Age N/A'}
                    </p>
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

              {/* Phone & Notes snippet */}
              <div className="space-y-1.5 text-xs text-portal-textMuted dark:text-portal-darkTextMuted border-t border-portal-border dark:border-portal-darkBorder pt-3">
                {patient.phone && (
                  <p className="flex items-center gap-2 font-medium">
                    <Phone className="w-3.5 h-3.5 text-slate-400" />
                    <span>{patient.phone}</span>
                  </p>
                )}
                {patient.notes && (
                  <p className="flex items-start gap-2 text-[11px] line-clamp-2">
                    <FileText className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                    <span>{patient.notes}</span>
                  </p>
                )}
              </div>

              <div className="flex items-center justify-between text-xs font-bold text-blue-600 dark:text-blue-400 pt-1">
                <span>View Patient Scans</span>
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
