"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { LeaveBalanceCard } from "@/features/leave/LeaveBalanceCard";
import { LeaveRequestList } from "@/features/leave/LeaveRequestList";

export default function LeavesPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">有給休暇</h1>
        <Link href="/leaves/new">
          <Button>申請する</Button>
        </Link>
      </div>
      <LeaveBalanceCard />
      <LeaveRequestList />
    </div>
  );
}
