"use client";

import { useEffect, useState } from "react";

export function CurrentTime() {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const timeStr = now.toLocaleTimeString("ja-JP", {
    timeZone: "Asia/Tokyo",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });

  const dateStr = now.toLocaleDateString("ja-JP", {
    timeZone: "Asia/Tokyo",
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  });

  return (
    <div className="text-center">
      <p className="text-4xl font-bold tabular-nums tracking-tight">{timeStr}</p>
      <p className="text-sm text-muted-foreground mt-1">{dateStr}</p>
    </div>
  );
}
