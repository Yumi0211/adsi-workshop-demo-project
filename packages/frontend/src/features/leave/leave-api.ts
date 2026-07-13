import { apiClient } from "@/lib/api-client";

export type LeaveType = "FULL" | "AM" | "PM";
export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface LeaveResponse {
  id: string;
  leaveDate: string;
  leaveType: LeaveType;
  reason: string | null;
  status: LeaveStatus;
  approverName: string | null;
  rejectReason: string | null;
  version: number;
  createdAt: string;
}

export interface PendingLeaveResponse {
  id: string;
  requesterId: string;
  requesterName: string;
  leaveDate: string;
  leaveType: LeaveType;
  reason: string | null;
  status: LeaveStatus;
  version: number;
  createdAt: string;
}

export interface LeaveBalanceResponse {
  fiscalYear: number;
  grantedDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveSummaryResponse {
  employeeId: string;
  employeeName: string;
  departmentName: string;
  grantedDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveCreateRequest {
  leaveDate: string;
  leaveType: LeaveType;
  reason?: string;
}

export function createLeave(
  requesterId: string,
  request: LeaveCreateRequest,
): Promise<LeaveResponse> {
  return apiClient.post<LeaveResponse>(`/api/leaves?requesterId=${requesterId}`, request);
}

export function fetchLeaves(
  requesterId: string,
  status?: LeaveStatus,
  fiscalYear?: number,
): Promise<LeaveResponse[]> {
  const params = new URLSearchParams({ requesterId });
  if (status) params.set("status", status);
  if (fiscalYear) params.set("fiscalYear", fiscalYear.toString());
  return apiClient.get<LeaveResponse[]>(`/api/leaves?${params.toString()}`);
}

export function fetchLeaveBalance(
  employeeId: string,
  fiscalYear?: number,
): Promise<LeaveBalanceResponse> {
  const params = new URLSearchParams({ employeeId });
  if (fiscalYear) params.set("fiscalYear", fiscalYear.toString());
  return apiClient.get<LeaveBalanceResponse>(`/api/leaves/balance?${params.toString()}`);
}

export function fetchPendingLeaves(managerId: string): Promise<PendingLeaveResponse[]> {
  return apiClient.get<PendingLeaveResponse[]>(`/api/leaves/pending?managerId=${managerId}`);
}

export function approveLeave(id: string, approverId: string): Promise<LeaveResponse> {
  return apiClient.patch<LeaveResponse>(`/api/leaves/${id}/approve?approverId=${approverId}`);
}

export function rejectLeave(
  id: string,
  approverId: string,
  rejectReason: string,
  version: number,
): Promise<LeaveResponse> {
  return apiClient.patch<LeaveResponse>(`/api/leaves/${id}/reject?approverId=${approverId}`, {
    rejectReason,
    version,
  });
}

export function fetchLeaveSummary(
  fiscalYear?: number,
  departmentId?: string,
): Promise<LeaveSummaryResponse[]> {
  const params = new URLSearchParams();
  if (fiscalYear) params.set("fiscalYear", fiscalYear.toString());
  if (departmentId) params.set("departmentId", departmentId);
  return apiClient.get<LeaveSummaryResponse[]>(`/api/leaves/summary?${params.toString()}`);
}
