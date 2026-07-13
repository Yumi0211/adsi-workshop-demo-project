"use client";

import { SidebarProvider } from "@/components/ui/sidebar";
import { Header } from "./Header";
import { AppSidebar } from "./Sidebar";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <SidebarProvider>
      <AppSidebar />
      <div className="flex flex-1 flex-col">
        <Header />
        <main className="flex-1 p-6 relative">
          <div className="pointer-events-none absolute inset-0 overflow-hidden select-none opacity-25 text-6xl leading-16 break-all" aria-hidden="true">
            {"🐻".repeat(500)}
          </div>
          <div className="relative">{children}</div>
        </main>
      </div>
    </SidebarProvider>
  );
}
