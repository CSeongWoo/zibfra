/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#041627', // Midnight Blue
        secondary: '#944a00', // Terracotta
        background: '#f8f9fa', // Soft Off-White
        tertiary: '#161616', // Rich Black
        'secondary-container': '#fc8f34',
        'surface-variant': '#e1e3e4',
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
      },
      lineHeight: {
        normal: '1.6',
        'body-dense': '1.3',
      },
      borderRadius: {
        sm: '4px',
        DEFAULT: '8px',
        md: '12px',
        lg: '16px',
        xl: '24px',
      },
      boxShadow: {
        'level-1': '0px 4px 12px rgba(26, 43, 60, 0.05)',
        'level-2': '0px 12px 32px rgba(26, 43, 60, 0.12)',
        'glow-secondary': '0 0 8px rgba(252, 143, 52, 0.4)',
      }
    },
  },
  plugins: [],
}
