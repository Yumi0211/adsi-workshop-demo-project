"use client";

import { useLeaveSummary } from "./useLeaves";

export function LeaveSummaryTable() {
  const { data: summary, isLoading } = useLeaveSummary();

  if (isLoading) {
    return (
      <div className="animate-pulse space-y-2">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-12 bg-muted rounded" />
        ))}
      </div>
    );
  }

  if (!summary || summary.length === 0) {
    return <p className="text-sm text-muted-foreground py-4">データがありません</p>;
  }

  return (
    <div className="rounded-md border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            <th className="px-4 py-2 text-left font-medium">社員名</th>
            <th className="px-4 py-2 text-left font-medium">部署</th>
            <th className="px-4 py-2 text-right font-medium">付与</th>
            <th className="px-4 py-2 text-right font-medium">使用</th>
            <th className="px-4 py-2 text-right font-medium">残日数</th>
          </tr>
        </thead>
        <tbody>
          {summary.map((row) => (
            <tr key={row.employeeId} className="border-b last:border-0">
              <td className="px-4 py-2">{row.employeeName}</td>
              <td className="px-4 py-2">{row.departmentName}</td>
              <td className="px-4 py-2 text-right">{row.grantedDays}</td>
              <td className="px-4 py-2 text-right">{row.usedDays}</td>
              <td className="px-4 py-2 text-right font-medium">{row.remainingDays}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
