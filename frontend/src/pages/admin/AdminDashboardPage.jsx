import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/adminService';
import { Users, Building2, Calendar, Ticket, IndianRupee, Shield, RefreshCw, MapPin, CheckCircle, Clock, XCircle, AlertCircle } from 'lucide-react';

export const AdminDashboardPage = () => {
  const [stats, setStats] = useState(null);
  const [branches, setBranches] = useState([]);
  const [recentBookings, setRecentBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAdminData();
  }, []);

  const fetchAdminData = async () => {
    setLoading(true);
    try {
      const [statsRes, branchesRes, bookingsRes] = await Promise.all([
        adminService.getDashboardStats(),
        adminService.getBranchStats(),
        adminService.getRecentBookings(10),
      ]);

      if (statsRes.success) setStats(statsRes.data);
      if (branchesRes.success) setBranches(branchesRes.data || []);
      if (bookingsRes.success) setRecentBookings(bookingsRes.data || []);
    } catch (err) {
      console.error('Failed to load admin dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  const getBadgeClass = (status) => {
    switch (status) {
      case 'CONFIRMED': return 'badge-confirmed';
      case 'HELD': return 'badge-held';
      case 'PENDING': return 'badge-held';
      case 'CANCELLED': return 'badge-cancelled';
      case 'EXPIRED': return 'badge-expired';
      default: return 'badge-available';
    }
  };

  // Group branches by city
  const cityBreakdown = branches.reduce((acc, b) => {
    const c = b.location || 'Other';
    acc[c] = (acc[c] || 0) + 1;
    return acc;
  }, {});

  if (loading) {
    return <div className="py-20 text-center text-slate-400">Loading system-wide admin telemetry data...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <span className="badge badge-confirmed mb-1 flex items-center gap-1 w-fit">
            <Shield className="w-3.5 h-3.5" /> System Administrator Panel
          </span>
          <h1 className="text-3xl font-extrabold text-white">National Cinema Network Telemetry</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time platform overview across 25 PVK multiplexes in 5 major metropolitan hubs</p>
        </div>
        <button
          onClick={fetchAdminData}
          className="btn-secondary text-xs px-4 py-2.5 flex items-center gap-2"
        >
          <RefreshCw className="w-3.5 h-3.5" /> Refresh Telemetry
        </button>
      </div>

      {/* User & Cinema Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {/* User Metrics */}
        <div className="glass-card p-5 border-indigo-500/30">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Users Telemetry</span>
            <div className="w-9 h-9 rounded-xl bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-extrabold text-white mb-2">{stats?.totalUsers || 0}</div>
          <div className="flex flex-col gap-1 text-xs text-slate-400 border-t border-slate-800 pt-3">
            <div className="flex justify-between">
              <span>Customers:</span>
              <strong className="text-indigo-300">{stats?.totalCustomers || 0}</strong>
            </div>
            <div className="flex justify-between">
              <span>Providers:</span>
              <strong className="text-purple-300">{stats?.totalProviders || 0}</strong>
            </div>
            <div className="flex justify-between">
              <span>Active Status:</span>
              <strong className="text-emerald-400">{stats?.activeUsers || 0}</strong>
            </div>
          </div>
        </div>

        {/* PVK Branches Metrics */}
        <div className="glass-card p-5 border-purple-500/30">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">PVK National Multiplexes</span>
            <div className="w-9 h-9 rounded-xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400">
              <Building2 className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-extrabold text-white mb-2">{stats?.totalBranches || 0}</div>
          <div className="flex flex-col gap-1 text-xs text-slate-400 border-t border-slate-800 pt-3">
            <div className="flex justify-between">
              <span>Total Auditoriums:</span>
              <strong className="text-purple-300">{stats?.totalScreens || 0} Screens</strong>
            </div>
            <div className="flex justify-between">
              <span>Total Physical Seats:</span>
              <strong className="text-white">{stats?.totalPhysicalSeats || 0} Seats</strong>
            </div>
          </div>
        </div>

        {/* Shows Telemetry */}
        <div className="glass-card p-5 border-amber-500/30">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Show Schedule</span>
            <div className="w-9 h-9 rounded-xl bg-amber-600/20 border border-amber-500/40 flex items-center justify-center text-amber-400">
              <Calendar className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-extrabold text-white mb-2">{stats?.totalScheduledShows || 0}</div>
          <div className="flex flex-col gap-1 text-xs text-slate-400 border-t border-slate-800 pt-3">
            <div className="flex justify-between">
              <span>Active Shows:</span>
              <strong className="text-emerald-400">{stats?.activeShows || 0}</strong>
            </div>
            <div className="flex justify-between">
              <span>Cancelled Shows:</span>
              <strong className="text-red-400">{stats?.cancelledShows || 0}</strong>
            </div>
          </div>
        </div>

        {/* Platform Revenue */}
        <div className="glass-card p-5 border-emerald-500/30">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Net Revenue</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-600/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
              <IndianRupee className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-extrabold text-emerald-400 mb-2">
            ₹{stats?.netRevenue?.toFixed(2) || '0.00'}
          </div>
          <div className="flex flex-col gap-1 text-xs text-slate-400 border-t border-slate-800 pt-3">
            <div className="flex justify-between">
              <span>Gross Confirmed:</span>
              <strong className="text-white">₹{stats?.confirmedRevenue?.toFixed(2) || '0.00'}</strong>
            </div>
            <div className="flex justify-between">
              <span>Simulated Refunds:</span>
              <strong className="text-red-400">₹{stats?.refundedAmount?.toFixed(2) || '0.00'}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* City Breakdown Bar */}
      <div className="glass-card p-6 mb-8 border border-indigo-500/20">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-2">
          <MapPin className="w-4 h-4 text-indigo-400" /> City Network Breakdown (5 Metropolitan Hubs)
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
          {['Chennai', 'Bengaluru', 'Mumbai', 'Delhi', 'Hyderabad'].map((c) => (
            <div key={c} className="bg-slate-900/80 p-4 rounded-xl border border-slate-800 text-center">
              <span className="text-xs font-bold text-slate-400 block">{c}</span>
              <span className="text-2xl font-black text-indigo-300 block mt-1">{cityBreakdown[c] || 5}</span>
              <span className="text-[10px] text-slate-500 block">PVK Multiplexes</span>
            </div>
          ))}
        </div>
      </div>

      {/* Bookings Breakdown Pills */}
      <div className="glass-card p-6 mb-8">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-2">
          <Ticket className="w-4 h-4 text-indigo-400" /> Platform Bookings Breakdown
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
          <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-800 text-center">
            <span className="block text-[10px] text-slate-500 uppercase font-bold">Total</span>
            <span className="text-lg font-extrabold text-white">{stats?.totalBookings || 0}</span>
          </div>
          <div className="bg-slate-900/60 p-3 rounded-xl border border-emerald-500/30 text-center">
            <span className="block text-[10px] text-emerald-400 uppercase font-bold">Confirmed</span>
            <span className="text-lg font-extrabold text-emerald-400">{stats?.confirmedBookings || 0}</span>
          </div>
          <div className="bg-slate-900/60 p-3 rounded-xl border border-amber-500/30 text-center">
            <span className="block text-[10px] text-amber-400 uppercase font-bold">Active Holds</span>
            <span className="text-lg font-extrabold text-amber-400">{stats?.heldBookings || 0}</span>
          </div>
          <div className="bg-slate-900/60 p-3 rounded-xl border border-indigo-500/30 text-center">
            <span className="block text-[10px] text-indigo-300 uppercase font-bold">Pending</span>
            <span className="text-lg font-extrabold text-indigo-300">{stats?.pendingBookings || 0}</span>
          </div>
          <div className="bg-slate-900/60 p-3 rounded-xl border border-red-500/30 text-center">
            <span className="block text-[10px] text-red-400 uppercase font-bold">Cancelled</span>
            <span className="text-lg font-extrabold text-red-400">{stats?.cancelledBookings || 0}</span>
          </div>
          <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-700 text-center">
            <span className="block text-[10px] text-slate-400 uppercase font-bold">Expired</span>
            <span className="text-lg font-extrabold text-slate-400">{stats?.expiredBookings || 0}</span>
          </div>
        </div>
      </div>

      {/* PVK Branches Operational Table */}
      <div className="glass-card p-6 mb-8">
        <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
          <Building2 className="w-5 h-5 text-purple-400" /> All 25 PVK Cinema Branches Overview
        </h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-900/80 text-slate-400 uppercase text-[10px] font-bold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">PVK Branch Name</th>
                <th className="py-3 px-4">City Hub</th>
                <th className="py-3 px-4 text-center">Screens</th>
                <th className="py-3 px-4 text-center">Seating Capacity</th>
                <th className="py-3 px-4 text-center">Shows Scheduled</th>
                <th className="py-3 px-4 text-center">Confirmed Bookings</th>
                <th className="py-3 px-4 text-right">Confirmed Revenue</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {branches.map((b) => (
                <tr key={b.theatreId} className="hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4 font-bold text-white">{b.name}</td>
                  <td className="py-3 px-4 text-slate-400">{b.location}</td>
                  <td className="py-3 px-4 text-center font-bold text-purple-300">{b.screenCount}</td>
                  <td className="py-3 px-4 text-center font-bold text-white">{b.totalSeatingCapacity} seats</td>
                  <td className="py-3 px-4 text-center font-bold text-amber-300">{b.scheduledShowsCount}</td>
                  <td className="py-3 px-4 text-center font-bold text-indigo-300">{b.confirmedBookingsCount}</td>
                  <td className="py-3 px-4 text-right font-extrabold text-emerald-400">₹{b.confirmedRevenue?.toFixed(2) || '0.00'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent System Activity Table */}
      <div className="glass-card p-6">
        <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
          <Ticket className="w-5 h-5 text-indigo-400" /> Recent System Bookings
        </h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-900/80 text-slate-400 uppercase text-[10px] font-bold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Booking Ref</th>
                <th className="py-3 px-4">Customer</th>
                <th className="py-3 px-4">PVK Branch</th>
                <th className="py-3 px-4">Movie</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Amount</th>
                <th className="py-3 px-4 text-right">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {recentBookings.map((bk) => (
                <tr key={bk.bookingId} className="hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4 font-mono font-bold text-indigo-300">{bk.bookingRef}</td>
                  <td className="py-3 px-4">
                    <span className="block font-bold text-white">{bk.customerName || 'Customer'}</span>
                    <span className="block text-[10px] text-slate-500">{bk.customerEmail}</span>
                  </td>
                  <td className="py-3 px-4 text-slate-300">{bk.theatreName} ({bk.screenName})</td>
                  <td className="py-3 px-4 font-semibold text-white">{bk.movieTitle}</td>
                  <td className="py-3 px-4">
                    <span className={`badge ${getBadgeClass(bk.status)}`}>{bk.status}</span>
                  </td>
                  <td className="py-3 px-4 text-right font-bold text-emerald-400">₹{bk.totalAmount?.toFixed(2)}</td>
                  <td className="py-3 px-4 text-right text-slate-500 text-[11px]">{bk.createdAt?.replace('T', ' ').substring(0, 16)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
