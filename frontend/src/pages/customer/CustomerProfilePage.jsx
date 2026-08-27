import React, { useState, useEffect } from 'react';
import { customerService } from '../../services/customerService';
import { convenienceService } from '../../services/convenienceService';
import { useAuth } from '../../context/AuthContext';
import { User, Mail, Shield, Key, CheckCircle, AlertCircle, Ticket, Clock, Calendar, Sliders, DollarSign, Users } from 'lucide-react';

export const CustomerProfilePage = () => {
  const { updateUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  // Edit Profile Form State
  const [name, setName] = useState('');
  const [updatingProfile, setUpdatingProfile] = useState(false);
  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');

  // Change Password Form State
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [updatingPassword, setUpdatingPassword] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');

  // Booking Preferences State
  const [budgetLimit, setBudgetLimit] = useState(500);
  const [preferredTime, setPreferredTime] = useState('EVENING');
  const [preferredSeatType, setPreferredSeatType] = useState('PREMIUM');
  const [groupSize, setGroupSize] = useState(2);
  const [updatingPref, setUpdatingPref] = useState(false);
  const [prefSuccess, setPrefSuccess] = useState('');

  useEffect(() => {
    fetchProfileAndPreferences();
  }, []);

  const fetchProfileAndPreferences = async () => {
    setLoading(true);
    try {
      const [profRes, prefRes] = await Promise.all([
        customerService.getProfile(),
        convenienceService.getPreferences(),
      ]);

      if (profRes.success && profRes.data) {
        setProfile(profRes.data);
        setName(profRes.data.name || '');
      }

      if (prefRes.success && prefRes.data) {
        setBudgetLimit(prefRes.data.budgetLimit || 500);
        setPreferredTime(prefRes.data.preferredTime || 'EVENING');
        setPreferredSeatType(prefRes.data.preferredSeatType || 'PREMIUM');
        setGroupSize(prefRes.data.groupSize || 2);
      }
    } catch (err) {
      console.error('Failed to load profile or preferences:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setProfileSuccess('');
    setProfileError('');

    if (!name.trim()) {
      setProfileError('Name cannot be empty.');
      return;
    }

    setUpdatingProfile(true);
    try {
      const res = await customerService.updateProfile(name.trim());
      if (res.success && res.data) {
        setProfile(res.data);
        updateUser({ name: res.data.name });
        setProfileSuccess('Profile name updated successfully!');
      } else {
        setProfileError(res.message || 'Failed to update profile.');
      }
    } catch (err) {
      setProfileError(err.response?.data?.message || 'Error updating profile.');
    } finally {
      setUpdatingProfile(false);
    }
  };

  const handlePreferencesSubmit = async (e) => {
    e.preventDefault();
    setPrefSuccess('');
    setUpdatingPref(true);
    try {
      const res = await convenienceService.updatePreferences({
        budgetLimit,
        preferredTime,
        preferredSeatType,
        groupSize,
      });
      if (res.success) {
        setPrefSuccess('Booking preferences saved! Future AI decision recommendations will utilize these rules.');
      }
    } catch (err) {
      console.error('Failed to save preferences:', err);
    } finally {
      setUpdatingPref(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setPasswordSuccess('');
    setPasswordError('');

    if (!currentPassword) {
      setPasswordError('Current password is required.');
      return;
    }

    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirmation do not match.');
      return;
    }

    if (newPassword === currentPassword) {
      setPasswordError('New password cannot be the same as your current password.');
      return;
    }

    setUpdatingPassword(true);
    try {
      const res = await customerService.changePassword(currentPassword, newPassword, confirmPassword);
      if (res.success) {
        setPasswordSuccess('Password changed successfully!');
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
      } else {
        setPasswordError(res.message || 'Failed to change password.');
      }
    } catch (err) {
      setPasswordError(err.response?.data?.message || 'Error changing password.');
    } finally {
      setUpdatingPassword(false);
    }
  };

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading profile data...</div>;
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Page Header */}
      <div className="mb-8">
        <span className="badge badge-confirmed mb-1">Account & Settings</span>
        <h1 className="text-3xl font-extrabold text-white">Customer Profile & Settings</h1>
        <p className="text-xs text-slate-400 mt-1">Manage your account information, security credentials, and decision preferences</p>
      </div>

      {/* Overview Card */}
      {profile && (
        <div className="glass-card p-6 mb-8 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 border-indigo-500/20">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400 font-extrabold text-2xl">
              {profile.name ? profile.name.charAt(0).toUpperCase() : 'C'}
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">{profile.name}</h2>
              <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                <Mail className="w-3.5 h-3.5 text-slate-500" />
                <span>{profile.email}</span>
              </div>
              <div className="flex items-center gap-2 text-[11px] text-slate-500 mt-1">
                <Calendar className="w-3 h-3 text-slate-600" />
                <span>Member since {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '2026'}</span>
              </div>
            </div>
          </div>

          <div className="flex gap-4 border-t sm:border-t-0 sm:border-l border-slate-800 pt-4 sm:pt-0 sm:pl-6 w-full sm:w-auto">
            <div className="text-center sm:text-left bg-slate-900/60 px-4 py-2.5 rounded-xl border border-slate-800 flex-1 sm:flex-none">
              <span className="block text-[10px] text-slate-500 uppercase font-bold flex items-center gap-1">
                <Ticket className="w-3 h-3 text-indigo-400" /> Total Bookings
              </span>
              <span className="text-xl font-extrabold text-white">{profile.totalBookings || 0}</span>
            </div>
            <div className="text-center sm:text-left bg-slate-900/60 px-4 py-2.5 rounded-xl border border-slate-800 flex-1 sm:flex-none">
              <span className="block text-[10px] text-slate-500 uppercase font-bold flex items-center gap-1">
                <Clock className="w-3 h-3 text-amber-400" /> Active Holds
              </span>
              <span className="text-xl font-extrabold text-amber-400">{profile.activeHolds || 0}</span>
            </div>
          </div>
        </div>
      )}

      {/* Booking Preferences Card */}
      <div className="glass-card p-6 mb-8 border border-slate-800">
        <div className="flex items-center gap-2 border-b border-slate-800 pb-4 mb-6">
          <Sliders className="w-5 h-5 text-cyan-400" />
          <div>
            <h3 className="text-lg font-bold text-white">Customer Booking Preferences</h3>
            <p className="text-xs text-slate-400">Baseline constraints for future AI Decision Support recommendation engine</p>
          </div>
        </div>

        {prefSuccess && (
          <div className="mb-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs flex items-center gap-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{prefSuccess}</span>
          </div>
        )}

        <form onSubmit={handlePreferencesSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="form-label text-xs">Max Budget Per Ticket (₹)</label>
            <input
              type="number"
              className="form-input text-xs"
              value={budgetLimit}
              onChange={(e) => setBudgetLimit(Number(e.target.value))}
              min="100"
              max="2000"
              required
            />
          </div>

          <div>
            <label className="form-label text-xs">Preferred Showtime Slot</label>
            <select
              className="form-input text-xs bg-slate-900"
              value={preferredTime}
              onChange={(e) => setPreferredTime(e.target.value)}
            >
              <option value="MORNING">Morning (9 AM - 12 PM)</option>
              <option value="AFTERNOON">Afternoon (12 PM - 4 PM)</option>
              <option value="EVENING">Evening (4 PM - 8 PM)</option>
              <option value="NIGHT">Night (8 PM - 11 PM)</option>
            </select>
          </div>

          <div>
            <label className="form-label text-xs">Preferred Seating Tier</label>
            <select
              className="form-input text-xs bg-slate-900"
              value={preferredSeatType}
              onChange={(e) => setPreferredSeatType(e.target.value)}
            >
              <option value="REGULAR">Regular Tier (1.0x Base)</option>
              <option value="PREMIUM">Premium Tier (1.25x Base)</option>
              <option value="BALCONY">Balcony Tier (1.50x Base)</option>
            </select>
          </div>

          <div>
            <label className="form-label text-xs">Typical Group Size</label>
            <input
              type="number"
              className="form-input text-xs"
              value={groupSize}
              onChange={(e) => setGroupSize(Number(e.target.value))}
              min="1"
              max="10"
              required
            />
          </div>

          <div className="sm:col-span-2 pt-2">
            <button
              type="submit"
              disabled={updatingPref}
              className="btn-primary bg-gradient-to-r from-cyan-600 to-indigo-600 text-xs py-2.5 px-6 font-bold"
            >
              {updatingPref ? 'Saving Preferences...' : 'Save Decision Preferences'}
            </button>
          </div>
        </form>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Edit Profile Form */}
        <div className="glass-card p-6 border-slate-800">
          <div className="flex items-center gap-2 border-b border-slate-800 pb-4 mb-6">
            <User className="w-5 h-5 text-indigo-400" />
            <h3 className="text-lg font-bold text-white">Personal Information</h3>
          </div>

          {profileSuccess && (
            <div className="mb-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs flex items-center gap-2">
              <CheckCircle className="w-4 h-4 shrink-0" />
              <span>{profileSuccess}</span>
            </div>
          )}

          {profileError && (
            <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{profileError}</span>
            </div>
          )}

          <form onSubmit={handleProfileSubmit} className="space-y-4">
            <div>
              <label className="form-label text-xs">Full Name</label>
              <input
                type="text"
                className="form-input text-xs"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Enter your full name"
                required
              />
            </div>

            <div>
              <label className="form-label text-xs">Email Address (Read-only)</label>
              <input
                type="email"
                className="form-input text-xs opacity-60 cursor-not-allowed bg-slate-900/80"
                value={profile?.email || ''}
                disabled
              />
            </div>

            <div>
              <label className="form-label text-xs">Account Role</label>
              <div className="flex items-center gap-2 bg-slate-900/60 p-2.5 rounded-lg border border-slate-800 text-xs text-slate-300">
                <Shield className="w-4 h-4 text-indigo-400" />
                <span className="font-bold text-indigo-300">{profile?.role || 'ROLE_CUSTOMER'}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={updatingProfile}
              className="btn-primary w-full py-2.5 text-xs justify-center font-bold mt-2"
            >
              {updatingProfile ? 'Saving...' : 'Save Profile Changes'}
            </button>
          </form>
        </div>

        {/* Change Password Form */}
        <div className="glass-card p-6 border-slate-800">
          <div className="flex items-center gap-2 border-b border-slate-800 pb-4 mb-6">
            <Key className="w-5 h-5 text-purple-400" />
            <h3 className="text-lg font-bold text-white">Change Security Password</h3>
          </div>

          {passwordSuccess && (
            <div className="mb-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs flex items-center gap-2">
              <CheckCircle className="w-4 h-4 shrink-0" />
              <span>{passwordSuccess}</span>
            </div>
          )}

          {passwordError && (
            <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{passwordError}</span>
            </div>
          )}

          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div>
              <label className="form-label text-xs">Current Password</label>
              <input
                type="password"
                className="form-input text-xs"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <div>
              <label className="form-label text-xs">New Password (min 6 chars)</label>
              <input
                type="password"
                className="form-input text-xs"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <div>
              <label className="form-label text-xs">Confirm New Password</label>
              <input
                type="password"
                className="form-input text-xs"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                required
              />
            </div>

            <button
              type="submit"
              disabled={updatingPassword}
              className="btn-primary bg-purple-600 hover:bg-purple-500 border-purple-500 w-full py-2.5 text-xs justify-center font-bold mt-2"
            >
              {updatingPassword ? 'Updating Password...' : 'Update Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
