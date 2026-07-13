"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { LeaveType, PendingLeaveResponse } from "./leave-api";
import { useApproveLeave, usePendingLeaves, useRejectLeave } from "./useLeaves";

const TYPE_LABELS: Record<LeaveType, string> = {
  FULL: "全日",
  AM: "午前休",
  PM: "午後休",
};

export function PendingLeaveList() {
  const { data: pending, isLoading } = usePendingLeaves();
  const approveMutation = useApproveLeave();
  const rejectMutation = useRejectLeave();
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  if (isLoading) {
    return (
      <div className="animate-pulse space-y-2">
        {[...Array(2)].map((_, i) => (
          <div key={i} className="h-12 bg-muted rounded" />
        ))}
      </div>
    );
  }

  if (!pending || pending.length === 0) {
    return <p className="text-sm text-muted-foreground py-4">承認待ちの有給申請はありません</p>;
  }

  const handleReject = (leave: PendingLeaveResponse) => {
    if (!rejectReason.trim()) return;
    rejectMutation.mutate(
      { id: leave.id, rejectReason, version: leave.version },
      {
        onSuccess: () => {
          setRejectingId(null);
          setRejectReason("");
        },
      },
    );
  };

  return (
    <div className="space-y-3">
      {pending.map((leave) => (
        <div key={leave.id} className="rounded-md border p-4">
          <div className="flex items-center justify-between">
            <div>
              <span className="font-medium">{leave.requesterName}</span>
              <span className="mx-2 text-muted-foreground">|</span>
              <span>{leave.leaveDate}</span>
              <Badge variant="secondary" className="ml-2">
                {TYPE_LABELS[leave.leaveType]}
              </Badge>
            </div>
            <div className="flex gap-2">
              <Button
                size="sm"
                onClick={() => approveMutation.mutate({ id: leave.id, version: leave.version })}
                disabled={approveMutation.isPending}
              >
                承認
              </Button>
              <Button size="sm" variant="outline" onClick={() => setRejectingId(leave.id)}>
                却下
              </Button>
            </div>
          </div>
          {leave.reason && (
            <p className="text-sm text-muted-foreground mt-1">理由: {leave.reason}</p>
          )}
          {rejectingId === leave.id && (
            <div className="mt-3 flex gap-2">
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="却下理由を入力"
                rows={2}
                className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
              <div className="flex flex-col gap-1">
                <Button
                  size="sm"
                  variant="destructive"
                  onClick={() => handleReject(leave)}
                  disabled={!rejectReason.trim() || rejectMutation.isPending}
                >
                  確定
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setRejectingId(null);
                    setRejectReason("");
                  }}
                >
                  取消
                </Button>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
