import React, { useState } from 'react';
import { User, Phone, FileText, X } from 'lucide-react';

export default function PatientFormModal({ initialPatient, onSave, onClose }) {
  const [name, setName] = useState(initialPatient?.name || '');
  const [age, setAge] = useState(initialPatient?.age || '');
  const [gender, setGender] = useState(initialPatient?.gender || 'Female');
  const [phone, setPhone] = useState(initialPatient?.phone || '');
  const [notes, setNotes] = useState(initialPatient?.notes || '');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      ...(initialPatient ? { id: initialPatient.id } : {}),
      name,
      age,
      gender,
      phone,
      notes
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-white dark:bg-portal-darkCard rounded-3xl p-6 sm:p-8 border border-portal-border dark:border-portal-darkBorder shadow-2xl relative space-y-6">
        
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-portal-textMuted hover:text-portal-textMain p-1 rounded-lg"
        >
          <X className="w-5 h-5" />
        </button>

        <div>
          <h2 className="text-xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
            {initialPatient ? 'Edit Patient Profile' : 'Add New Patient'}
          </h2>
          <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted mt-1">
            Enter clinical demographics and treatment notes
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
              Patient Full Name *
            </label>
            <div className="relative">
              <User className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted" />
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Jane Doe"
                className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Age
              </label>
              <input
                type="number"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                placeholder="32"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
                Gender
              </label>
              <select
                value={gender}
                onChange={(e) => setGender(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option value="Female">Female</option>
                <option value="Male">Male</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
              Phone / Mobile
            </label>
            <div className="relative">
              <Phone className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted" />
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 (555) 019-2831"
                className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-portal-textMain dark:text-portal-darkTextMain mb-1">
              Clinical Notes & Treatment Target
            </label>
            <div className="relative">
              <FileText className="w-4 h-4 absolute left-3.5 top-3.5 text-portal-textMuted" />
              <textarea
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Upper anterior restoration target shade B1..."
                className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder text-sm font-medium focus:ring-2 focus:ring-blue-500 focus:outline-none resize-none"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-portal-textMain dark:text-portal-darkTextMain font-bold text-xs"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition-colors"
            >
              {initialPatient ? 'Update Profile' : 'Save Patient'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
