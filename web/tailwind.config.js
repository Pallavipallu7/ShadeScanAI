/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        portal: {
          bg: '#F8FAFC',
          darkBg: '#0F172A',
          card: '#FFFFFF',
          darkCard: '#1E293B',
          accent: '#2563EB',
          accentLight: '#EFF6FF',
          darkAccent: '#3B82F6',
          textMain: '#0F172A',
          darkTextMain: '#F8FAFC',
          textMuted: '#64748B',
          darkTextMuted: '#94A3B8',
          border: '#E2E8F0',
          darkBorder: '#334155',
          teal: '#0D9488',
          tealLight: '#F0FDFA'
        }
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'scan-line': 'scan 2.5s ease-in-out infinite',
      },
      keyframes: {
        scan: {
          '0%, 100%': { transform: 'translateY(0%)' },
          '50%': { transform: 'translateY(100%)' },
        }
      }
    },
  },
  plugins: [],
}
