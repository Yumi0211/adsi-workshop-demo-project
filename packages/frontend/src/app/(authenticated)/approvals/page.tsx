"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/features/auth/useAuth";
import { PendingCorrectionList } from "@/features/correction/PendingCorrectionList";
import { PendingLeaveList } from "@/features/leave/PendingLeaveList";

export default function ApprovalsPage() {
  const { user } = useAuth();

  if (!user?.isManager) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-muted-foreground">このページを閲覧する権限がありません</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">承認</h1>
      <Tabs defaultValue="corrections">
        <TabsList>
          <TabsTrigger value="corrections">勤怠修正</TabsTrigger>
          <TabsTrigger value="leaves">有給休暇</TabsTrigger>
        </TabsList>
        <TabsContent value="corrections">
          <PendingCorrectionList />
        </TabsContent>
        <TabsContent value="leaves">
          <PendingLeaveList />
        </TabsContent>
      </Tabs>
    </div>
  );
}
