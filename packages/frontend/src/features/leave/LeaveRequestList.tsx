"use client";

import { Badge } from "@/components/ui/badge";
import type { LeaveStatus, LeaveType } from "./leave-api";
import { useLeaves } from "./useLeaves";

const TYPE_LABELS: Record<LeaveType, string> = {
  FULL: "全日",
  AM: "午前休",
  PM: "午後休",
};

const STATUS_LABELS: Record<LeaveStatus, string> = {
  PENDING: "申請中",
  APPROVED: "承認済",
  REJECTED: "却下",
};

const STATUS_VARIANTS: Record<LeaveStatus, "default" | "secondary" | "destructive"> = {
  PENDING: "secondary",
  APPROVED: "default",
  REJECTED: "destructive",
};

export function LeaveRequestList() {
  const { data: leaves, isLoading } = useLeaves();

  if (isLoading) {
    return (
      <div className="animate-pulse space-y-2">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-12 bg-muted rounded" />
        ))}
      </div>
    );
  }

  if (!leaves || leaves.length === 0) {
    return <p className="text-sm text-muted-foreground py-4">申請履歴がありません</p>;
  }

  return (
    <div className="rounded-md border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            <th className="px-4 py-2 text-left font-medium">取得日</th>
            <th className="px-4 py-2 text-left font-medium">種別</th>
            <th className="px-4 py-2 text-left font-medium">理由</th>
            <th className="px-4 py-2 text-left font-medium">状態</th>
            <th className="px-4 py-2 text-left font-medium">承認者</th>
          </tr>
        </thead>
        <tbody>
          {leaves.map((leave) => (
            <tr key={leave.id} className="border-b last:border-0">
              <td className="px-4 py-2">{leave.leaveDate}</td>
              <td className="px-4 py-2">{TYPE_LABELS[leave.leaveType]}</td>
              <td className="px-4 py-2 text-muted-foreground">{leave.reason || "—"}</td>
              <td className="px-4 py-2">
                <Badge variant={STATUS_VARIANTS[leave.status]}>{STATUS_LABELS[leave.status]}</Badge>
              </td>
              <td className="px-4 py-2">{leave.approverName || "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
