import React, { useState, useEffect } from 'react';
import { customerService } from '../../services/customerService';
import { useAuth } from '../../context/AuthContext';
import { User, Mail, Shield, Key, CheckCircle, AlertCircle, Ticket, Clock, Calendar } from 'lucide-react';

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

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await customerService.getProfile();
      if (res.success && res.data) {
        setProfile(res.data);
        setName(res.data.name || '');
      }
    } catch (err) {
      console.error('Failed to load profile:', err);
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
        <h1 className="text-3xl font-extrabold text-white">Customer Profile</h1>
        <p className="text-xs text-slate-400 mt-1">Manage your account information and credentials</p>
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
