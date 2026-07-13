"use client";

import { LogIn, LogOut, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/useAuth";
import { CurrentTime } from "./CurrentTime";
import { formatTime } from "./format";
import { useClockIn, useClockOut, useTodayStatus, useUpdateMemo } from "./useAttendance";

const STATUS_LABELS = {
  NOT_CLOCKED_IN: "未出勤",
  CLOCKED_IN: "勤務中",
  CLOCKED_OUT: "退勤済み",
} as const;

export function ClockButtons() {
  const { user } = useAuth();
  const { data: todayStatus, isLoading } = useTodayStatus();
  const clockInMutation = useClockIn();
  const clockOutMutation = useClockOut();
  const updateMemoMutation = useUpdateMemo();
  const [memo, setMemo] = useState("");

  const status = todayStatus?.status ?? "NOT_CLOCKED_IN";
  const lastRecord = todayStatus?.records[todayStatus.records.length - 1];
  const canClockIn = status === "NOT_CLOCKED_IN";
  const canClockOut = status === "CLOCKED_IN";
  const isMemoEditable = status === "CLOCKED_IN";
  const isPending = clockInMutation.isPending || clockOutMutation.isPending;

  useEffect(() => {
    if (!todayStatus) return;
    if (status === "NOT_CLOCKED_IN") {
      setMemo("");
    } else {
      setMemo(lastRecord?.memo ?? "");
    }
  }, [todayStatus, status, lastRecord?.memo]);

  if (isLoading) {
    return (
      <div className="rounded-lg border p-6 space-y-4">
        <Skeleton className="h-12 w-48 mx-auto" />
        <Skeleton className="h-5 w-24 mx-auto" />
        <div className="flex justify-center gap-4">
          <Skeleton className="h-10 w-28" />
          <Skeleton className="h-10 w-28" />
        </div>
      </div>
    );
  }

  const handleSaveMemo = () => {
    if (!lastRecord) return;
    updateMemoMutation.mutate({ recordId: lastRecord.id, memo });
  };

  return (
    <div className="rounded-lg border p-6 space-y-4">
      <CurrentTime />
      <div className="flex items-center justify-center gap-2">
        <span className="text-sm text-muted-foreground">{STATUS_LABELS[status]}</span>
        {lastRecord && status === "CLOCKED_IN" && (
          <span className="text-sm text-muted-foreground">
            ({formatTime(lastRecord.clockIn)} ~)
          </span>
        )}
      </div>
      <div className="grid grid-cols-2 gap-4 max-w-md mx-auto">
        <button
          type="button"
          disabled={!canClockIn || isPending}
          onClick={() => clockInMutation.mutate()}
          className="flex flex-col items-center justify-center gap-2 rounded-xl bg-purple-300 py-8 text-white transition-colors hover:bg-purple-400 active:bg-purple-500 disabled:bg-gray-200 disabled:text-gray-400"
        >
          <LogIn className="h-8 w-8" />
          <span className="text-lg font-bold">出勤</span>
        </button>
        <button
          type="button"
          disabled={!canClockOut || isPending}
          onClick={() => clockOutMutation.mutate()}
          className="flex flex-col items-center justify-center gap-2 rounded-xl bg-pink-300 py-8 text-white transition-colors hover:bg-pink-400 active:bg-pink-500 disabled:bg-gray-200 disabled:text-gray-400"
        >
          <LogOut className="h-8 w-8" />
          <span className="text-lg font-bold">退勤</span>
        </button>
      </div>
      <div className="flex max-w-md mx-auto gap-2">
        <Input
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          placeholder="備考（任意）"
          maxLength={50}
          disabled={!isMemoEditable}
          className="flex-1 bg-white"
        />
        {isMemoEditable && (
          <Button
            size="sm"
            variant="outline"
            onClick={handleSaveMemo}
            disabled={updateMemoMutation.isPending}
          >
            <Save className="h-4 w-4 mr-1" />
            保存
          </Button>
        )}
      </div>
    </div>
  );
}
