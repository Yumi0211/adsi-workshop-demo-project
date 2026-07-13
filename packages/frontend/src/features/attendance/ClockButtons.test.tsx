import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("./useAttendance", () => ({
  useTodayStatus: vi.fn(),
  useClockIn: () => ({ mutate: vi.fn(), isPending: false }),
  useClockOut: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/auth/useAuth", () => ({
  useAuth: () => ({ user: { id: "u1", name: "Test", departmentName: "Dev" }, isAuthenticated: true, isLoading: false }),
}));

import { useTodayStatus } from "./useAttendance";
import { ClockButtons } from "./ClockButtons";

const mockedUseTodayStatus = vi.mocked(useTodayStatus);

afterEach(() => {
  cleanup();
});

describe("ClockButtons", () => {
  it("出勤打刻後（CLOCKED_IN）は出勤ボタンが無効になる", () => {
    mockedUseTodayStatus.mockReturnValue({
      data: {
        status: "CLOCKED_IN",
        records: [{ id: "r1", workDate: "2026-07-13", clockIn: "2026-07-13T09:00:00", clockOut: null, corrected: false }],
      },
      isLoading: false,
    } as ReturnType<typeof useTodayStatus>);

    render(<ClockButtons />);

    const clockInButton = screen.getByRole("button", { name: /出勤/ });
    expect(clockInButton).toBeDisabled();
  });

  it("出勤打刻後（CLOCKED_IN）は退勤ボタンが有効になる", () => {
    mockedUseTodayStatus.mockReturnValue({
      data: {
        status: "CLOCKED_IN",
        records: [{ id: "r1", workDate: "2026-07-13", clockIn: "2026-07-13T09:00:00", clockOut: null, corrected: false }],
      },
      isLoading: false,
    } as ReturnType<typeof useTodayStatus>);

    render(<ClockButtons />);

    const clockOutButton = screen.getByRole("button", { name: /退勤/ });
    expect(clockOutButton).toBeEnabled();
  });

  it("退勤打刻後（CLOCKED_OUT）は出勤ボタンが無効になる", () => {
    mockedUseTodayStatus.mockReturnValue({
      data: {
        status: "CLOCKED_OUT",
        records: [{ id: "r1", workDate: "2026-07-13", clockIn: "2026-07-13T09:00:00", clockOut: "2026-07-13T18:00:00", corrected: false }],
      },
      isLoading: false,
    } as ReturnType<typeof useTodayStatus>);

    render(<ClockButtons />);

    const clockInButton = screen.getByRole("button", { name: /出勤/ });
    expect(clockInButton).toBeDisabled();
  });

  it("未出勤（NOT_CLOCKED_IN）は出勤ボタンが有効になる", () => {
    mockedUseTodayStatus.mockReturnValue({
      data: {
        status: "NOT_CLOCKED_IN",
        records: [],
      },
      isLoading: false,
    } as ReturnType<typeof useTodayStatus>);

    render(<ClockButtons />);

    const clockInButton = screen.getByRole("button", { name: /出勤/ });
    expect(clockInButton).toBeEnabled();
  });
});
