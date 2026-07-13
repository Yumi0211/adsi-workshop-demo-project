import { apiClient } from "@/lib/api-client";

export type CorrectionStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface CorrectionResponse {
  id: string;
  attendanceRecordId: string | null;
  requesterId: string;
  requesterName: string;
  approverId: string | null;
  approverName: string | null;
  targetDate: string;
  correctedClockIn: string;
  correctedClockOut: string;
  correctedMemo: string | null;
  reason: string;
  status: CorrectionStatus;
  rejectReason: string | null;
  version: number;
  createdAt: string;
}

export interface PendingCorrectionResponse {
  id: string;
  attendanceRecordId: string | null;
  requesterId: string;
  requesterName: string;
  targetDate: string;
  correctedClockIn: string;
  correctedClockOut: string;
  correctedMemo: string | null;
  reason: string;
  version: number;
  createdAt: string;
}

export interface CorrectionCreateRequest {
  attendanceRecordId?: string | null;
  targetDate: string;
  correctedClockIn: string;
  correctedClockOut: string;
  reason: string;
}

export function createCorrection(
  requesterId: string,
  request: CorrectionCreateRequest,
): Promise<CorrectionResponse> {
  return apiClient.post<CorrectionResponse>(
    `/api/corrections?requesterId=${requesterId}`,
    request,
  );
}

export function fetchCorrections(
  requesterId: string,
  status?: CorrectionStatus,
): Promise<CorrectionResponse[]> {
  const params = new URLSearchParams({ requesterId });
  if (status) {
    params.set("status", status);
  }
  return apiClient.get<CorrectionResponse[]>(`/api/corrections?${params.toString()}`);
}

export function fetchPendingCorrections(
  managerId: string,
): Promise<PendingCorrectionResponse[]> {
  return apiClient.get<PendingCorrectionResponse[]>(
    `/api/corrections/pending?managerId=${managerId}`,
  );
}

export function approveCorrection(
  id: string,
  approverId: string,
  version: number,
): Promise<CorrectionResponse> {
  return apiClient.patch<CorrectionResponse>(
    `/api/corrections/${id}/approve?approverId=${approverId}&version=${version}`,
  );
}

export function rejectCorrection(
  id: string,
  approverId: string,
  reason: string,
  version: number,
): Promise<CorrectionResponse> {
  return apiClient.patch<CorrectionResponse>(
    `/api/corrections/${id}/reject?approverId=${approverId}`,
    { reason, version },
  );
}
