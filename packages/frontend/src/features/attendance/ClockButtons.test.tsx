import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("./useAttendance", () => ({
  useTodayStatus: vi.fn(),
  useClockIn: () => ({ mutate: vi.fn(), isPending: false }),
  useClockOut: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateMemo: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/auth/useAuth", () => ({
  useAuth: () => ({
    user: { id: "u1", name: "Test", departmentName: "Dev" },
    isAuthenticated: true,
    isLoading: false,
  }),
}));

import type { TodayStatusResponse } from "./attendance-api";
import { ClockButtons } from "./ClockButtons";
import { useTodayStatus } from "./useAttendance";

const mockedUseTodayStatus = vi.mocked(useTodayStatus);

function mockStatus(status: TodayStatusResponse["status"], memo?: string | null) {
  const records =
    status === "NOT_CLOCKED_IN"
      ? []
      : [
          {
            id: "r1",
            workDate: "2026-07-13",
            clockIn: "2026-07-13T09:00:00",
            clockOut: status === "CLOCKED_OUT" ? "2026-07-13T18:00:00" : null,
            corrected: false,
            memo: memo ?? null,
          },
        ];

  mockedUseTodayStatus.mockReturnValue({
    data: { status, records },
    isLoading: false,
  } as ReturnType<typeof useTodayStatus>);
}

afterEach(() => {
  cleanup();
});

describe("ClockButtons", () => {
  it("出勤打刻後（CLOCKED_IN）は出勤ボタンが無効になる", () => {
    mockStatus("CLOCKED_IN");
    render(<ClockButtons />);
    expect(screen.getByRole("button", { name: /出勤/ })).toBeDisabled();
  });

  it("出勤打刻後（CLOCKED_IN）は退勤ボタンが有効になる", () => {
    mockStatus("CLOCKED_IN");
    render(<ClockButtons />);
    expect(screen.getByRole("button", { name: /退勤/ })).toBeEnabled();
  });

  it("退勤打刻後（CLOCKED_OUT）は出勤ボタンが無効になる", () => {
    mockStatus("CLOCKED_OUT");
    render(<ClockButtons />);
    expect(screen.getByRole("button", { name: /出勤/ })).toBeDisabled();
  });

  it("退勤打刻後（CLOCKED_OUT）は退勤ボタンが無効になる", () => {
    mockStatus("CLOCKED_OUT");
    render(<ClockButtons />);
    expect(screen.getByRole("button", { name: /退勤/ })).toBeDisabled();
  });

  it("未出勤（NOT_CLOCKED_IN）は出勤ボタンが有効になる", () => {
    mockStatus("NOT_CLOCKED_IN");
    render(<ClockButtons />);
    expect(screen.getByRole("button", { name: /出勤/ })).toBeEnabled();
  });

  it("未出勤（NOT_CLOCKED_IN）は退勤ボタンが無効になる", () => {
    mockStatus("NOT_CLOCKED_IN");
    render(<ClockButtons />);
    expect(screen.getByRole("button", { name: /退勤/ })).toBeDisabled();
  });

  it("出勤中（CLOCKED_IN）は備考入力欄が有効で保存ボタンが表示される", () => {
    mockStatus("CLOCKED_IN");
    render(<ClockButtons />);
    const input = screen.getByPlaceholderText("備考（任意）");
    expect(input).toBeEnabled();
    expect(screen.getByRole("button", { name: /保存/ })).toBeInTheDocument();
  });

  it("退勤済み（CLOCKED_OUT）は備考入力欄が無効で保存ボタンが非表示", () => {
    mockStatus("CLOCKED_OUT");
    render(<ClockButtons />);
    const input = screen.getByPlaceholderText("備考（任意）");
    expect(input).toBeDisabled();
    expect(screen.queryByRole("button", { name: /保存/ })).not.toBeInTheDocument();
  });

  it("未出勤（NOT_CLOCKED_IN）は備考入力欄が無効で保存ボタンが非表示", () => {
    mockStatus("NOT_CLOCKED_IN");
    render(<ClockButtons />);
    const input = screen.getByPlaceholderText("備考（任意）");
    expect(input).toBeDisabled();
    expect(screen.queryByRole("button", { name: /保存/ })).not.toBeInTheDocument();
  });

  it("出勤中にサーバーから取得した備考が入力欄に表示される", () => {
    mockStatus("CLOCKED_IN", "在宅勤務");
    render(<ClockButtons />);
    const input = screen.getByPlaceholderText("備考（任意）") as HTMLInputElement;
    expect(input.value).toBe("在宅勤務");
  });
});
