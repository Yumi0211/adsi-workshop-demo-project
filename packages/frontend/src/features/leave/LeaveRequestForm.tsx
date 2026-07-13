"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { LeaveType } from "./leave-api";
import { useCreateLeave, useLeaveBalance } from "./useLeaves";

export function LeaveRequestForm() {
  const router = useRouter();
  const mutation = useCreateLeave();
  const { data: balance } = useLeaveBalance();

  const [leaveDate, setLeaveDate] = useState("");
  const [leaveType, setLeaveType] = useState<LeaveType>("FULL");
  const [reason, setReason] = useState("");

  const isValid = leaveDate.length > 0;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(
      { leaveDate, leaveType, reason: reason || undefined },
      { onSuccess: () => router.push("/leaves") },
    );
  };

  return (
    <form onSubmit={handleSubmit} className="max-w-md space-y-6">
      {balance && (
        <p className="text-sm text-muted-foreground">
          残日数: <span className="font-bold text-foreground">{balance.remainingDays}日</span>
        </p>
      )}

      <div className="space-y-2">
        <Label htmlFor="leaveDate">取得日 *</Label>
        <Input
          id="leaveDate"
          type="date"
          value={leaveDate}
          onChange={(e) => setLeaveDate(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <Label>種別 *</Label>
        <div className="flex gap-4">
          {(
            [
              ["FULL", "全日"],
              ["AM", "午前休"],
              ["PM", "午後休"],
            ] as const
          ).map(([value, label]) => (
            <label key={value} className="flex items-center gap-1.5 cursor-pointer">
              <input
                type="radio"
                name="leaveType"
                value={value}
                checked={leaveType === value}
                onChange={() => setLeaveType(value)}
                className="accent-primary"
              />
              <span className="text-sm">{label}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="reason">理由</Label>
        <textarea
          id="reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="私用のため"
          rows={3}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
      </div>

      <div className="flex gap-3">
        <Button type="button" variant="outline" onClick={() => router.push("/leaves")}>
          キャンセル
        </Button>
        <Button type="submit" disabled={!isValid || mutation.isPending}>
          {mutation.isPending ? "送信中..." : "申請する"}
        </Button>
      </div>
    </form>
  );
}
