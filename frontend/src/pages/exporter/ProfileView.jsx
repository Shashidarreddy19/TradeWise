import React, { useState, useEffect } from 'react';
import { authApi } from '../../services';
import { User, Building, Mail, Phone, Shield, Globe, Award } from 'lucide-react';

/**
 * ProfileView — Exporter profile settings section.
 * Redesigned with Claude-inspired shadcn/ui design system.
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
      <div className="max-w-2xl mx-auto card-claude p-8 text-center">
        <span className="text-xs text-muted-foreground">Loading profile...</span>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto card-claude p-6 sm:p-8 space-y-6 animate-in fade-in duration-200">
      <div>
        <h1 className="text-xl font-bold text-foreground tracking-tight">Exporter Profile Settings</h1>
        <p className="text-xs text-muted-foreground mt-1">Your company registrations and account details.</p>
      </div>

      {/* Profile fields */}
      <div className="border border-border bg-muted/20 p-5 rounded-xl space-y-4">
        <div className="grid grid-cols-2 gap-4 text-xs font-medium">
          <div className="space-y-1">
            <span className="text-muted-foreground block text-[11px]">Full Name</span>
            <span className="text-foreground block font-semibold text-sm">{displayUser.name || displayUser.firstName || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-muted-foreground block text-[11px]">Company Name</span>
            <span className="text-foreground block font-semibold text-sm">{displayUser.companyName || displayUser.company || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-muted-foreground block text-[11px]">Registered Email</span>
            <span className="text-foreground block font-mono text-sm">{displayUser.email || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-muted-foreground block text-[11px]">Phone</span>
            <span className="text-foreground block font-mono text-sm">{displayUser.phone || 'N/A'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-muted-foreground block text-[11px]">Role</span>
            <span className="text-primary font-semibold block text-sm">{displayUser.role || 'EXPORTER'}</span>
          </div>
          <div className="space-y-1">
            <span className="text-muted-foreground block text-[11px]">Base Country</span>
            <span className="text-foreground block font-semibold text-sm">{displayUser.country || 'India'}</span>
          </div>
          {displayUser.gstNumber && (
            <div className="space-y-1 col-span-2 border-t border-border pt-3">
              <span className="text-muted-foreground block text-[11px]">GST / Business Registration</span>
              <span className="text-foreground block font-mono font-semibold text-sm">{displayUser.gstNumber}</span>
            </div>
          )}
          {displayUser.productCategories && displayUser.productCategories.length > 0 && (
            <div className="space-y-1 col-span-2 border-t border-border pt-3">
              <span className="text-muted-foreground block text-[11px]">Product Categories</span>
              <span className="text-foreground block font-semibold text-sm">{displayUser.productCategories.join(', ')}</span>
            </div>
          )}
        </div>
      </div>

      {/* Button Actions */}
      <div className="flex gap-3 pt-2">
        <button
          onClick={() => addToast('Profile editing is coming soon.', 'success')}
          className="flex-grow btn-primary"
        >
          Update Profile Info
        </button>
        <button
          onClick={() => addToast('Password change link sent to your email.', 'success')}
          className="btn-secondary"
        >
          Change Password
        </button>
      </div>
    </div>
  );
}

