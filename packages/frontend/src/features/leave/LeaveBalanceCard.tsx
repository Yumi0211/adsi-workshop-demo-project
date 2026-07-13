"use client";

import { useLeaveBalance } from "./useLeaves";

export function LeaveBalanceCard() {
  const { data: balance, isLoading } = useLeaveBalance();

  if (isLoading) {
    return <div className="rounded-lg border p-4 animate-pulse h-24" />;
  }

  if (!balance) return null;

  const percentage =
    balance.grantedDays > 0 ? Math.round((balance.usedDays / balance.grantedDays) * 100) : 0;

  return (
    <div className="rounded-lg border p-4">
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-sm font-medium text-muted-foreground">
          {balance.fiscalYear}年度 有給残日数
        </h3>
        <span className="text-2xl font-bold">
          {balance.remainingDays}{" "}
          <span className="text-sm font-normal text-muted-foreground">
            / {balance.grantedDays}日
          </span>
        </span>
      </div>
      <div className="w-full bg-secondary rounded-full h-2">
        <div
          className="bg-primary rounded-full h-2 transition-all"
          style={{ width: `${percentage}%` }}
        />
      </div>
      <p className="text-xs text-muted-foreground mt-1">
        使用: {balance.usedDays}日 ({percentage}%)
      </p>
    </div>
  );
}
