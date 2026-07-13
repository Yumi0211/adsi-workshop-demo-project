"use client";

import { LeaveSummaryTable } from "@/features/leave/LeaveSummaryTable";

export default function AdminLeavesPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">有給取得状況</h1>
      <LeaveSummaryTable />
    </div>
  );
}
