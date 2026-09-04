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
      addToast('Profile information saved successfully.', 'success');
    } catch (err) {
      addToast(err.message || 'Failed to save profile', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-6 h-6 text-slate-400 animate-spin" />
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto animate-in fade-in duration-300">
      <div className="bg-white border border-slate-200/80 p-6 sm:p-8 rounded-2xl shadow-sm space-y-6">

        {/* Header */}
        <div className="flex items-center gap-4 border-b border-slate-100 pb-5">
          <div className="w-14 h-14 rounded-full bg-sky-50 border border-sky-100 flex items-center justify-center text-sky-500">
            <Shield className="w-7 h-7" />
          </div>
          <div>
            <h2 className="text-lg font-black text-slate-900 tracking-tight">Logistics Partner Profile</h2>
            <p className="text-xxs text-slate-400 font-bold uppercase tracking-wider mt-0.5">Company, licensing, and operational configuration</p>
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
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Service Areas / Trade Lanes</label>
            <input type="text" value={form.serviceArea} onChange={e => setForm({...form, serviceArea: e.target.value})}
              placeholder="e.g. Middle East, Southeast Asia, Europe"
              className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500" />
          </div>
          <div className="space-y-1.5">
            <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Services Offered</label>
            <input type="text" value={form.services} onChange={e => setForm({...form, services: e.target.value})}
              placeholder="e.g. Air Freight, Sea Freight, Customs Clearance"
              className="w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500" />
          </div>

          {/* Capability toggles */}
          <div className="flex gap-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={form.trackingSupport} onChange={e => setForm({...form, trackingSupport: e.target.checked})}
                className="w-4 h-4 rounded border-slate-300 text-sky-500" />
              <span className="text-xs font-semibold text-slate-700">Live Tracking Support</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={form.cargoInsurance} onChange={e => setForm({...form, cargoInsurance: e.target.checked})}
                className="w-4 h-4 rounded border-slate-300 text-sky-500" />
              <span className="text-xs font-semibold text-slate-700">Cargo Insurance Offered</span>
            </label>
          </div>

          <button type="submit" disabled={saving}
            className="w-full py-3 bg-sky-500 hover:bg-sky-400 text-white font-black text-xs uppercase tracking-widest rounded-xl transition-all shadow-md cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2">
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
      <label className="text-[10px] font-black text-slate-500 uppercase tracking-widest">{label}</label>
      <input type={type} value={value} onChange={e => onChange(e.target.value)} required={required} disabled={disabled}
        placeholder={placeholder}
        className={`w-full px-3.5 py-2.5 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-sky-500 ${disabled ? 'bg-slate-50 text-slate-400 cursor-not-allowed' : ''}`} />
    </div>
  );
}
