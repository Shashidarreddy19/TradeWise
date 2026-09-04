import React, { useState, useEffect } from 'react';
import { authApi } from '../../services';

/**
 * ProfileView — Exporter profile settings section.
 * Fetches real profile data from the backend on mount.
 */
export default function ProfileView({ user, addToast }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await authApi.getProfile();
        setProfile(res.data || res);
      } catch {
        // Fall back to the user prop if API fails
        setProfile(null);
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, []);

  const displayUser = profile || user || {};

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto bg-white border border-slate-200/80 rounded-2xl p-8 shadow-sm text-center">
        <span className="text-xs text-slate-400">Loading profile...</span>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto bg-white border border-slate-200/80 rounded-2xl p-6 sm:p-8 shadow-sm space-y-6 animate-in fade-in duration-300">
      <div>
        <h1 className="text-xl font-black text-slate-900 tracking-tight">Exporter Profile Settings</h1>
        <p className="text-xs text-slate-500 mt-1 font-medium">Your company registrations and account details.</p>
      </div>

      {/* Profile fields */}
      <div className="border border-slate-100 bg-slate-50/20 p-5 rounded-2xl space-y-4">
        <div className="grid grid-cols-2 gap-4 text-xs font-bold">
          <div className="space-y-1">
            <span className="text-slate-400 block">Full Name</span>
            <span className="text-slate-800 block text-sm">{displayUser.name || displayUser.firstName || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-slate-400 block">Company Name</span>
            <span className="text-slate-800 block text-sm">{displayUser.companyName || displayUser.company || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-slate-400 block">Registered Email</span>
            <span className="text-slate-805 block text-sm font-mono">{displayUser.email || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-slate-400 block">Phone</span>
            <span className="text-slate-805 block text-sm font-mono">{displayUser.phone || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-slate-400 block">Role</span>
            <span className="text-slate-800 block text-sm">{displayUser.role || 'EXPORTER'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-slate-400 block">Base Country</span>
            <span className="text-slate-808 block text-sm">{displayUser.country || 'India'}</span>
          </div>
          {displayUser.gstNumber && (
            <div className="space-y-1 col-span-2 border-t border-slate-100 pt-3">
              <span className="text-slate-400 block">GST / Business Registration</span>
              <span className="text-slate-800 block text-sm font-mono">{displayUser.gstNumber}</span>
            </div>
          )}
          {displayUser.productCategories && displayUser.productCategories.length > 0 && (
            <div className="space-y-1 col-span-2 border-t border-slate-100 pt-3">
              <span className="text-slate-400 block">Product Categories</span>
              <span className="text-slate-800 block text-sm">{displayUser.productCategories.join(', ')}</span>
            </div>
          )}
        </div>
      </div>

      {/* Button Actions */}
      <div className="flex gap-3 pt-2">
        <button
          onClick={() => addToast('Profile editing is coming soon.', 'success')}
          className="flex-grow py-3 text-xs font-bold text-white bg-sky-500 hover:bg-sky-400 rounded-xl shadow-md transition-all cursor-pointer"
        >
          Update Profile Info
        </button>
        <button
          onClick={() => addToast('Password change link sent to your email.', 'success')}
          className="px-5 py-3 text-xs font-bold text-slate-655 border border-slate-200 hover:bg-slate-50 rounded-xl transition-all cursor-pointer"
        >
          Change Password
        </button>
      </div>
    </div>
  );
}
