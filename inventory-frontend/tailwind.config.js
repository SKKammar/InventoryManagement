/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "#0a192f",
        foreground: "#00ffcc",
        primary: {
          DEFAULT: "#00ffcc",
          foreground: "#03040c",
        },
        secondary: {
          DEFAULT: "#112240",
          foreground: "#00ffcc",
        },
        destructive: {
          DEFAULT: "#ff00ff",
          foreground: "#03040c",
        },
        muted: {
          DEFAULT: "#112240",
          foreground: "#00ffcc",
        },
        accent: {
          DEFAULT: "#ffea00",
          foreground: "#03040c",
        },
        popover: {
          DEFAULT: "#112240",
          foreground: "#00ffcc",
        },
        card: {
          DEFAULT: "#112240",
          foreground: "#00ffcc",
        },
        'text-main': "#00ffcc",
        'accent-1': "#ff00ff",
        'accent-2': "#ffea00",
        'accent-3': "#39ff14",
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
}
