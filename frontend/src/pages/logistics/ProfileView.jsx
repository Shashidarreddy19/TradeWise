import React, { useState, useEffect } from 'react';
import { Shield, Loader2 } from 'lucide-react';
import { authApi } from '../../services';

export default function ProfileView({ addToast }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    companyName: '', contactPerson: '', email: '', phone: '',
    businessRegistrationNumber: '', experience: '', serviceArea: '', services: '',
    trackingSupport: false, cargoInsurance: false,
  });

  useEffect(() => {
    const load = async () => {
      try {
        const res = await authApi.getProfile();
        const p = res.data || res;
        setProfile(p);
        setForm({
          companyName: p.companyName || '',
          contactPerson: p.name || '',
          email: p.email || '',
          phone: p.phone || '',
          businessRegistrationNumber: p.businessRegistrationNumber || '',
          experience: p.experience || '',
          serviceArea: p.serviceArea || '',
          services: p.services || '',
          trackingSupport: p.trackingSupport || false,
          cargoInsurance: p.cargoInsurance || false,
        });
      } catch {
        // fallback to empty
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await authApi.updateProfile({
        companyName: form.companyName,
        contactPerson: form.contactPerson,
        phone: form.phone,
        businessRegistrationNumber: form.businessRegistrationNumber,
        experience: form.experience,
        serviceArea: form.serviceArea,
        services: form.services,
        trackingSupport: form.trackingSupport,
        cargoInsurance: form.cargoInsurance,
      });
      if (res?.data) {
        setProfile(res.data);
      }
      addToast('Profile information saved and synced successfully.', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to save profile', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-6 h-6 text-primary animate-spin" />
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto animate-in fade-in duration-300">
      <div className="card-claude p-6 sm:p-8 space-y-6">

        {/* Header */}
        <div className="flex items-center gap-4 border-b border-border pb-5">
          <div className="w-14 h-14 rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary">
            <Shield className="w-7 h-7" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-foreground tracking-tight">Logistics Partner Profile</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Company, licensing, and operational configuration</p>
          </div>
        </div>

        <form onSubmit={handleSave} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Company Name" value={form.companyName} onChange={v => setForm({...form, companyName: v})} required />
            <Field label="Contact Person" value={form.contactPerson} onChange={v => setForm({...form, contactPerson: v})} required />
            <Field label="Operating Email" type="email" value={form.email} onChange={v => setForm({...form, email: v})} required />
            <Field label="Contact Phone" value={form.phone} onChange={v => setForm({...form, phone: v})} required />
            <Field label="Business Reg. Number / GSTIN" value={form.businessRegistrationNumber} onChange={v => setForm({...form, businessRegistrationNumber: v})} />
            <Field label="Years of Experience" value={form.experience} onChange={v => setForm({...form, experience: v})} placeholder="e.g. 5-10 years" />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Service Areas / Trade Lanes</label>
            <input type="text" value={form.serviceArea} onChange={e => setForm({...form, serviceArea: e.target.value})}
              placeholder="e.g. Middle East, Southeast Asia, Europe"
              className="input-claude" />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Services Offered</label>
            <input type="text" value={form.services} onChange={e => setForm({...form, services: e.target.value})}
              placeholder="e.g. Air Freight, Sea Freight, Customs Clearance"
              className="input-claude" />
          </div>

          {/* Capability toggles */}
          <div className="flex flex-wrap gap-4 pt-2">
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={form.trackingSupport} onChange={e => setForm({...form, trackingSupport: e.target.checked})}
                className="w-4 h-4 rounded border-border accent-primary" />
              <span className="text-xs font-medium text-foreground">Live Tracking Support</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={form.cargoInsurance} onChange={e => setForm({...form, cargoInsurance: e.target.checked})}
                className="w-4 h-4 rounded border-border accent-primary" />
              <span className="text-xs font-medium text-foreground">Cargo Insurance Offered</span>
            </label>
          </div>

          <button type="submit" disabled={saving}
            className="btn-primary w-full py-3 text-xs flex items-center justify-center gap-2">
            {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : null} Save Profile
          </button>
        </form>
      </div>
    </div>
  );
}

function Field({ label, value, onChange, type = 'text', placeholder = '', required = false, disabled = false }) {
  return (
    <div className="space-y-1.5">
      <label className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} required={required} disabled={disabled}
        placeholder={placeholder}
        className={`input-claude ${disabled ? 'bg-muted text-muted-foreground cursor-not-allowed' : ''}`} />
    </div>
  );
}

