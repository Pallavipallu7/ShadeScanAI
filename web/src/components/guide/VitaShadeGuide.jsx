import React from 'react';
import { VITA_SHADES } from '../../utils/shadeAnalyzer';
import { BookOpen, Info, Sparkles } from 'lucide-react';

export default function VitaShadeGuide() {
  const groups = [
    { title: 'Group A - Reddish-Brownish Shades', codes: ['A1', 'A2', 'A3', 'A3.5', 'A4'], desc: 'Most prevalent natural dentin shade range in clinical dentistry.' },
    { title: 'Group B - Reddish-Yellowish Shades', codes: ['B1', 'B2', 'B3', 'B4'], desc: 'Higher value / yellow undertones, commonly selected for post-whitening.' },
    { title: 'Group C - Greyish Shades', codes: ['C1', 'C2', 'C3', 'C4'], desc: 'Cooler grey undertones with lower lightness values.' },
    { title: 'Group D - Reddish-Greyish Shades', codes: ['D2', 'D3', 'D4'], desc: 'Subtle reddish-grey hue blend.' },
  ];

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      
      {/* Header Banner */}
      <div className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 sm:p-8 shadow-sm space-y-3">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 text-xs font-bold border border-blue-200 dark:border-blue-800">
          <BookOpen className="w-3.5 h-3.5" />
          <span>Clinical Reference Standard</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
          VITA Classical A1–D4 Shade System
        </h1>
        <p className="text-sm text-portal-textMuted dark:text-portal-darkTextMuted leading-relaxed max-w-3xl">
          The worldwide standard for dental shade matching. ShadeScan AI evaluates pixel color distributions against these 16 calibrated shade targets.
        </p>
      </div>

      {/* Shade Groups Grid */}
      <div className="space-y-6">
        {groups.map((group) => {
          const groupShades = VITA_SHADES.filter(s => group.codes.includes(s.label));

          return (
            <div 
              key={group.title}
              className="bg-white dark:bg-portal-darkCard rounded-3xl border border-portal-border dark:border-portal-darkBorder p-6 shadow-sm space-y-4"
            >
              <div>
                <h2 className="text-base font-extrabold text-portal-textMain dark:text-portal-darkTextMain">
                  {group.title}
                </h2>
                <p className="text-xs text-portal-textMuted dark:text-portal-darkTextMuted">
                  {group.desc}
                </p>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
                {groupShades.map((shade) => (
                  <div
                    key={shade.label}
                    className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-portal-border dark:border-portal-darkBorder space-y-3 flex flex-col items-center text-center hover:border-blue-500 transition-colors"
                  >
                    <div 
                      className="w-16 h-16 rounded-2xl border-2 border-white dark:border-slate-800 shadow-md"
                      style={{ backgroundColor: shade.hex }}
                    />
                    <div>
                      <p className="font-extrabold text-sm text-portal-textMain dark:text-portal-darkTextMain">
                        Shade {shade.label}
                      </p>
                      <p className="text-[10px] text-portal-textMuted dark:text-portal-darkTextMuted mt-0.5 line-clamp-2">
                        {shade.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
}
