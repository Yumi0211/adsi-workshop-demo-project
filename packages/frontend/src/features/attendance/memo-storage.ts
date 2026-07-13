const MEMO_STORAGE_PREFIX = "attendance_memo_draft_";

function getKey(employeeId: string, date: string) {
  return `${MEMO_STORAGE_PREFIX}${employeeId}_${date}`;
}

export function getDraftMemo(employeeId: string, date: string): string {
  if (typeof window === "undefined") return "";
  return localStorage.getItem(getKey(employeeId, date)) ?? "";
}

export function saveDraftMemo(employeeId: string, date: string, memo: string) {
  if (typeof window === "undefined") return;
  if (memo) {
    localStorage.setItem(getKey(employeeId, date), memo);
  } else {
    localStorage.removeItem(getKey(employeeId, date));
  }
}

export function clearDraftMemo(employeeId: string, date: string) {
  if (typeof window === "undefined") return;
  localStorage.removeItem(getKey(employeeId, date));
}

export function clearAllMemoDrafts() {
  if (typeof window === "undefined") return;
  const keysToRemove: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key?.startsWith(MEMO_STORAGE_PREFIX)) {
      keysToRemove.push(key);
    }
  }
  for (const key of keysToRemove) {
    localStorage.removeItem(key);
  }
}
