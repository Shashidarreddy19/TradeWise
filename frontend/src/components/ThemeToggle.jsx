import React, { useState, useRef, useEffect } from 'react';
import { Sun, Moon, Laptop, ChevronDown } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';

export default function ThemeToggle({ variant = 'simple', className = '' }) {
  const { theme, setTheme, isDark, toggleTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClick = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  if (variant === 'simple') {
    return (
      <button
        onClick={toggleTheme}
        type="button"
        title={isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
        className={`relative p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted/80 transition-colors cursor-pointer border border-border/60 ${className}`}
        aria-label="Toggle theme"
      >
        {isDark ? (
          <Sun className="w-4 h-4 text-amber-400 transition-transform duration-300 rotate-0 hover:rotate-45" />
        ) : (
          <Moon className="w-4 h-4 text-stone-600 transition-transform duration-300 rotate-0 hover:-rotate-12" />
        )}
      </button>
    );
  }

  // Dropdown variant (Light / Dark / System)
  return (
    <div className={`relative inline-block ${className}`} ref={dropdownRef}>
      <button
        onClick={() => setOpen(!open)}
        type="button"
        className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium text-muted-foreground hover:text-foreground bg-card hover:bg-muted border border-border transition-colors cursor-pointer"
        aria-label="Select theme"
      >
        {theme === 'dark' && <Moon className="w-3.5 h-3.5 text-primary" />}
        {theme === 'light' && <Sun className="w-3.5 h-3.5 text-amber-500" />}
        {theme === 'system' && <Laptop className="w-3.5 h-3.5 text-muted-foreground" />}
        <span className="capitalize">{theme}</span>
        <ChevronDown className="w-3 h-3 opacity-60 ml-0.5" />
      </button>

      {open && (
        <div className="absolute right-0 mt-1.5 w-32 py-1 bg-card border border-border rounded-lg shadow-lg z-50 text-xs animate-scale-in">
          <button
            onClick={() => { setTheme('light'); setOpen(false); }}
            className={`w-full flex items-center gap-2 px-3 py-1.5 text-left transition-colors hover:bg-muted ${theme === 'light' ? 'text-primary font-semibold' : 'text-foreground'}`}
          >
            <Sun className="w-3.5 h-3.5 text-amber-500" />
            <span>Light</span>
          </button>
          <button
            onClick={() => { setTheme('dark'); setOpen(false); }}
            className={`w-full flex items-center gap-2 px-3 py-1.5 text-left transition-colors hover:bg-muted ${theme === 'dark' ? 'text-primary font-semibold' : 'text-foreground'}`}
          >
            <Moon className="w-3.5 h-3.5 text-primary" />
            <span>Dark</span>
          </button>
          <button
            onClick={() => { setTheme('system'); setOpen(false); }}
            className={`w-full flex items-center gap-2 px-3 py-1.5 text-left transition-colors hover:bg-muted ${theme === 'system' ? 'text-primary font-semibold' : 'text-foreground'}`}
          >
            <Laptop className="w-3.5 h-3.5 text-muted-foreground" />
            <span>System</span>
          </button>
        </div>
      )}
    </div>
  );
}
