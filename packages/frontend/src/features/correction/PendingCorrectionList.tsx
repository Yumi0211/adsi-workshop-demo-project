"use client";

import { type Column, DataTable } from "@/components/DataTable";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDate, formatTime } from "@/features/attendance/format";
import type { PendingCorrectionResponse } from "./correction-api";
import { ApprovalActions } from "./ApprovalActions";
import { usePendingCorrections } from "./useCorrections";

const columns: Column<PendingCorrectionResponse>[] = [
  {
    key: "requesterName",
    header: "申請者",
  },
  {
    key: "targetDate",
    header: "対象日",
    render: (item) => formatDate(item.targetDate),
  },
  {
    key: "correctedClockIn",
    header: "修正出勤",
    render: (item) => formatTime(item.correctedClockIn),
  },
  {
    key: "correctedClockOut",
    header: "修正退勤",
    render: (item) => formatTime(item.correctedClockOut),
  },
  {
    key: "correctedMemo",
    header: "備考",
    render: (item) => item.correctedMemo ?? "-",
  },
  {
    key: "reason",
    header: "理由",
    render: (item) => (
      <span className="max-w-[200px] truncate block">{item.reason}</span>
    ),
  },
  {
    key: "actions",
    header: "",
    render: (item) => (
      <ApprovalActions correctionId={item.id} version={item.version} />
    ),
  },
];

export function PendingCorrectionList() {
  const { data, isLoading } = usePendingCorrections();

  return (
    <div className="space-y-4">
      {isLoading ? (
        <div className="space-y-2">
          {["r1", "r2", "r3", "r4", "r5"].map((id) => (
            <Skeleton key={id} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <DataTable<PendingCorrectionResponse & Record<string, unknown>>
          columns={
            columns as Column<PendingCorrectionResponse & Record<string, unknown>>[]
          }
          data={
            (data ?? []) as (PendingCorrectionResponse & Record<string, unknown>)[]
          }
          rowKey={(item) => item.id}
          emptyMessage="承認待ちの修正申請はありません"
        />
      )}
    </div>
  );
}
