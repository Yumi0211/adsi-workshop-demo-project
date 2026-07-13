# Unit 07: 有給休暇

有給休暇の申請・承認・却下・残高管理。労基法準拠の付与計算を含む。

## 依存関係

- 依存先: Unit 00（共通基盤）, Unit 02（社員 — 申請者・承認者・入社日）, Unit 03（認証）
- 依存元: Unit 04（勤怠履歴に有給表示）, Unit 06（月次集計で有給日数参照）
- **Unit 05（勤怠修正）とは相互依存なし → 並列実装可能**

## ユーザーストーリー

- **LEAVE-01**: 社員として、有給休暇を申請したい（日付・種別・理由を指定）
- **LEAVE-02**: 上長として、部下の有給休暇申請を承認/却下したい
- **LEAVE-03**: 社員として、自分の有給休暇の残日数を確認したい
- **LEAVE-04**: 社員として、自分の申請履歴を確認したい
- **LEAVE-05**: 管理者として、全社員の有給取得状況を確認したい
- **LEAVE-06**: 社員として、半日休暇（午前休・午後休）を申請したい

## テーブル

### leave_requests

```sql
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY,
    requester_id UUID NOT NULL REFERENCES employees(id),
    approver_id UUID REFERENCES employees(id),
    leave_date DATE NOT NULL,
    leave_type VARCHAR(10) NOT NULL CHECK (leave_type IN ('FULL', 'AM', 'PM')),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reject_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_leave_requests_requester ON leave_requests(requester_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_date ON leave_requests(requester_id, leave_date);
```

Flyway: `V7__create_leave_requests.sql`

### leave_balances

```sql
CREATE TABLE leave_balances (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    fiscal_year INT NOT NULL,
    granted_days DECIMAL(4,1) NOT NULL,
    used_days DECIMAL(4,1) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_leave_balances_employee_year UNIQUE (employee_id, fiscal_year)
);

CREATE INDEX idx_leave_balances_employee ON leave_balances(employee_id);
```

Flyway: `V8__create_leave_balances.sql`

## API

| メソッド | パス | 説明 | 権限 |
|---------|------|------|------|
| POST | `/api/leaves` | 有給申請 | 全ロール |
| GET | `/api/leaves` | 自分の申請一覧 | 全ロール |
| GET | `/api/leaves/balance` | 自分の残日数 | 全ロール |
| GET | `/api/leaves/pending` | 承認待ち一覧 | 上長 |
| PATCH | `/api/leaves/{id}/approve` | 承認 | 上長 |
| PATCH | `/api/leaves/{id}/reject` | 却下 | 上長 |
| GET | `/api/leaves/summary` | 全社員の取得状況 | 管理者 |

## 実装対象

### Backend

| レイヤー | ファイル |
|---------|---------|
| Entity | `LeaveRequest.java`, `LeaveBalance.java` |
| Enum | `LeaveType.java`, `LeaveStatus.java` |
| Repository | `LeaveRequestRepository.java`, `LeaveBalanceRepository.java` |
| Service | `LeaveService.java` (interface), `LeaveServiceImpl.java`, `LeaveGrantCalculator.java` |
| Controller | `LeaveController.java` |
| DTO | `LeaveRequestDto.java`, `LeaveCreateRequest.java`, `LeaveRejectRequest.java`, `LeaveBalanceDto.java`, `LeaveSummaryDto.java` |

### Frontend

| ファイル | 内容 |
|---------|------|
| `features/leave/leave-api.ts` | API クライアント |
| `features/leave/useLeaves.ts` | TanStack Query hooks |
| `features/leave/LeaveRequestList.tsx` | 申請一覧テーブル |
| `features/leave/LeaveRequestForm.tsx` | 申請フォーム |
| `features/leave/LeaveBalanceCard.tsx` | 残日数カード |
| `features/leave/PendingLeaveList.tsx` | 承認待ち一覧 |
| `features/leave/LeaveSummaryTable.tsx` | 管理者: 取得状況テーブル |
| `app/(authenticated)/leaves/page.tsx` | 申請一覧ページ |
| `app/(authenticated)/leaves/new/page.tsx` | 申請作成ページ |
| `app/(authenticated)/admin/leaves/page.tsx` | 管理者ページ |

## ビジネスルール

- 残日数 0 → 申請不可（エラー）
- 同日同種別の PENDING/APPROVED 重複不可（午前休+午後休は許容）
- 承認時に `leave_balances.used_days` を加算（FULL: +1.0, AM/PM: +0.5）
- 却下理由は必須
- 承認者は同部署の `isManager=true`
- 付与計算: 労基法テーブル + 4/1一斉付与 + 初年度按分

## テストケース（主要）

### LeaveGrantCalculator

- 勤続0.5年 → 10日
- 勤続6.5年以上 → 20日
- 初年度按分（4月入社 → 残6ヶ月 → 5日）
- 入社6ヶ月未満 → 0日

### LeaveService

- 申請成功（残日数あり）
- 申請失敗（残日数0）
- 申請失敗（同日重複）
- 承認 → usedDays 加算
- 却下 → usedDays 変動なし
- 半日申請 → 0.5 減算

### LeaveController

- 各エンドポイントの正常系・異常系
- 権限チェック（一般社員が pending にアクセス → 403）
