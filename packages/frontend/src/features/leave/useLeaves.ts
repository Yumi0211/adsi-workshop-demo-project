"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/components/Toast";
import { useAuth } from "@/features/auth/useAuth";
import {
  approveLeave,
  createLeave,
  fetchLeaveBalance,
  fetchLeaveSummary,
  fetchLeaves,
  fetchPendingLeaves,
  type LeaveCreateRequest,
  type LeaveStatus,
  rejectLeave,
} from "./leave-api";

const LEAVES_KEY = ["leaves"] as const;
const LEAVES_PENDING_KEY = ["leaves", "pending"] as const;
const LEAVES_BALANCE_KEY = ["leaves", "balance"] as const;
const LEAVES_SUMMARY_KEY = ["leaves", "summary"] as const;

export function useLeaves(status?: LeaveStatus, fiscalYear?: number) {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...LEAVES_KEY, user?.id, status, fiscalYear],
    queryFn: () => fetchLeaves(user!.id, status, fiscalYear),
    enabled: !!user,
  });
}

export function useLeaveBalance(fiscalYear?: number) {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...LEAVES_BALANCE_KEY, user?.id, fiscalYear],
    queryFn: () => fetchLeaveBalance(user!.id, fiscalYear),
    enabled: !!user,
  });
}

export function useCreateLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: LeaveCreateRequest) => createLeave(user!.id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVES_BALANCE_KEY });
      toast.success("有給休暇を申請しました");
    },
    onError: () => {
      toast.error("有給休暇の申請に失敗しました");
    },
  });
}

export function usePendingLeaves() {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...LEAVES_PENDING_KEY, user?.id],
    queryFn: () => fetchPendingLeaves(user!.id),
    enabled: !!user?.isManager,
  });
}

export function useApproveLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      approveLeave(id, user!.id, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVES_PENDING_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVES_BALANCE_KEY });
      toast.success("有給申請を承認しました");
    },
    onError: () => {
      toast.error("承認に失敗しました");
    },
  });
}

export function useRejectLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      rejectReason,
      version,
    }: {
      id: string;
      rejectReason: string;
      version: number;
    }) => rejectLeave(id, user!.id, rejectReason, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVES_PENDING_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      toast.success("有給申請を却下しました");
    },
    onError: () => {
      toast.error("却下に失敗しました");
    },
  });
}

export function useLeaveSummary(fiscalYear?: number, departmentId?: string) {
  return useQuery({
    queryKey: [...LEAVES_SUMMARY_KEY, fiscalYear, departmentId],
    queryFn: () => fetchLeaveSummary(fiscalYear, departmentId),
  });
}
