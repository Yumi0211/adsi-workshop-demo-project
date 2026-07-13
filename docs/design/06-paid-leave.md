# 有給休暇申請機能 — 設計

要求仕様: `docs/requirements/02-paid-leave.md`

---

## 1. ドメイン分析

### 新規 Entity

#### LeaveRequest（有給休暇申請）

| フィールド | 型 | 制約 | 備考 |
|-----------|-----|------|------|
| id | UUID (v7) | PK | |
| requester | Employee | FK, NOT NULL | 申請者 |
| approver | Employee | FK, nullable | 承認/却下した上長。処理前は null |
| leaveDate | LocalDate | NOT NULL | 取得日 |
| leaveType | LeaveType (enum) | NOT NULL | FULL / AM / PM |
| reason | String | nullable | 理由（任意） |
| status | LeaveStatus (enum) | NOT NULL | PENDING / APPROVED / REJECTED |
| rejectReason | String | nullable | 却下理由（却下時に必須） |
| version | Long | @Version | 楽観ロック |
| createdAt | Instant | NOT NULL | |
| updatedAt | Instant | NOT NULL | |

ビジネスルール:
- 同一社員・同一日・同一種別で重複申請不可（PENDING/APPROVED のもの）
- 午前休と午後休は同日に共存可能（合計1日分）
- 残日数 0 の場合は申請不可
- 承認者は申請者の所属部署の `isManager=true` の社員
- 上長が自分の有給を申請した場合は自己承認（修正申請と同パターン）

#### LeaveBalance（有給残高）

| フィールド | 型 | 制約 | 備考 |
|-----------|-----|------|------|
| id | UUID (v7) | PK | |
| employee | Employee | FK, NOT NULL | 対象社員 |
| fiscalYear | int | NOT NULL | 会計年度（例: 2026 → 2026年4月〜2027年3月） |
| grantedDays | BigDecimal | NOT NULL | 付与日数 |
| usedDays | BigDecimal | NOT NULL, DEFAULT 0 | 使用日数（承認済み分の合計） |
| version | Long | @Version | 楽観ロック |
| createdAt | Instant | NOT NULL | |
| updatedAt | Instant | NOT NULL | |

ビジネスルール:
- `remainingDays = grantedDays - usedDays`
- 半日休は 0.5 日として加算
- 承認時に `usedDays` を加算、（将来的に）取消時に減算
- 付与日数は労基法テーブル + 初年度按分で計算
- `(employee_id, fiscal_year)` で一意

### 新規 Enum

#### LeaveType（休暇種別）

```
FULL  — 全日休
AM    — 午前休（半日）
PM    — 午後休（半日）
```

#### LeaveStatus（申請ステータス）

```
PENDING   — 申請中（上長の承認待ち）
APPROVED  — 承認済み
REJECTED  — 却下
```

### Value Object

#### LeaveGrant（付与計算結果）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| grantedDays | BigDecimal | 付与日数 |
| yearsOfService | double | 勤続年数 |
| isProrated | boolean | 初年度按分かどうか |

---

## 2. ドメイン関連図（追加分）

```
                       ┌──────────────────┐
                       │    Employee       │
                       └──────┬──────┬─────┘
                              │1     │1
                              │      │
                           N  │      │  N
               ┌──────────────┘      └───────────────┐
               │                                      │
    ┌──────────▼─────────┐          ┌─────────────────▼──────┐
    │  LeaveRequest      │          │  LeaveBalance          │
    ├────────────────────┤          ├────────────────────────┤
    │  leaveDate         │          │  fiscalYear            │
    │  leaveType (enum)  │          │  grantedDays           │
    │  reason            │          │  usedDays              │
    │  status (enum)     │          │                        │
    │  rejectReason      │          └────────────────────────┘
    │  approver →Employee│
    └────────────────────┘
```

関連:
- Employee `1 : N` LeaveRequest — 1社員が複数の有給申請を出す
- Employee `1 : N` LeaveBalance — 1社員が年度ごとに残高を持つ
- Employee → LeaveRequest (approver) — 承認者としての参照

---

## 3. DB 設計

### leave_requests（有給休暇申請）

```sql
-- V7__create_leave_requests.sql

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

### leave_balances（有給残高）

```sql
-- V8__create_leave_balances.sql

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

### ER 図（追加分）

```
┌─────────────────────────┐
│       employees         │
└──────┬───────┬──────────┘
       │1      │1
       │       │
    N  │       │  N
┌──────▼──────────────┐   ┌──────────▼──────────────┐
│   leave_requests    │   │    leave_balances       │
├─────────────────────┤   ├─────────────────────────┤
│ id (PK)             │   │ id (PK)                 │
│ requester_id (FK)   │   │ employee_id (FK)        │
│ approver_id (FK)    │   │ fiscal_year             │
│ leave_date          │   │ granted_days            │
│ leave_type          │   │ used_days               │
│ reason              │   │ (UQ: employee+year)     │
│ status              │   └─────────────────────────┘
│ reject_reason       │
│ version             │
└─────────────────────┘
```

### インデックス設計

| テーブル | インデックス | 用途 |
|---------|------------|------|
| leave_requests | `idx_leave_requests_requester` | 自分の申請一覧 |
| leave_requests | `idx_leave_requests_status` | ステータス別フィルタ（承認待ち一覧） |
| leave_requests | `idx_leave_requests_date` | 社員×日付での重複チェック |
| leave_balances | `idx_leave_balances_employee` | 社員の残高取得 |
| leave_balances | `uq_leave_balances_employee_year` | 年度一意制約 |

---

## 4. API 設計

### 有給休暇 (leave)

| メソッド | パス | 説明 | 権限 |
|---------|------|------|------|
| POST | `/api/leaves` | 有給申請 | 全ロール |
| GET | `/api/leaves` | 自分の申請一覧 | 全ロール |
| GET | `/api/leaves/balance` | 自分の残日数 | 全ロール |
| GET | `/api/leaves/pending` | 承認待ち一覧 | 上長 |
| PATCH | `/api/leaves/{id}/approve` | 承認 | 上長 |
| PATCH | `/api/leaves/{id}/reject` | 却下 | 上長 |
| GET | `/api/leaves/summary` | 全社員の有給取得状況 | 管理者 |

### POST /api/leaves

有給休暇を申請する。

Request:
```json
{
  "leaveDate": "2026-07-20",
  "leaveType": "FULL",
  "reason": "私用のため"
}
```

Response (201):
```json
{
  "id": "019059d1-...",
  "leaveDate": "2026-07-20",
  "leaveType": "FULL",
  "reason": "私用のため",
  "status": "PENDING",
  "createdAt": "2026-07-13T02:00:00Z"
}
```

Error:
- (400): 残日数不足 / 同日に既に申請済み
- (400): バリデーションエラー

### GET /api/leaves

自分の有給申請一覧。

Query Parameters:
- `status` (任意): `PENDING` / `APPROVED` / `REJECTED` で絞り込み
- `fiscalYear` (任意): 年度で絞り込み（デフォルト: 現在年度）

Response (200):
```json
[
  {
    "id": "019059d1-...",
    "leaveDate": "2026-07-20",
    "leaveType": "FULL",
    "reason": "私用のため",
    "status": "PENDING",
    "approverName": null,
    "rejectReason": null,
    "createdAt": "2026-07-13T02:00:00Z"
  }
]
```

### GET /api/leaves/balance

自分の有給残日数を返す。

Query Parameters:
- `fiscalYear` (任意): デフォルト現在年度

Response (200):
```json
{
  "fiscalYear": 2026,
  "grantedDays": 10.0,
  "usedDays": 2.5,
  "remainingDays": 7.5
}
```

### GET /api/leaves/pending

自部署メンバーの承認待ち有給申請一覧。上長向け。

Response (200):
```json
[
  {
    "id": "019059d1-...",
    "requesterId": "019059a1-...",
    "requesterName": "田中太郎",
    "leaveDate": "2026-07-20",
    "leaveType": "FULL",
    "reason": "私用のため",
    "status": "PENDING",
    "createdAt": "2026-07-13T02:00:00Z"
  }
]
```

### PATCH /api/leaves/{id}/approve

有給申請を承認する。承認時に LeaveBalance の `usedDays` を加算する。

Request: ボディなし

Response (200): 更新後の申請情報（`status: "APPROVED"`, `approverName` が設定される）

Error (409): 楽観ロックエラー / (404): 申請が見つからない

### PATCH /api/leaves/{id}/reject

有給申請を却下する。

Request:
```json
{ "rejectReason": "業務都合上、別日に変更をお願いします" }
```

Response (200): 更新後の申請情報（`status: "REJECTED"`）

Error (409): 楽観ロックエラー

### GET /api/leaves/summary

全社員の有給取得状況。管理者向け。

Query Parameters:
- `fiscalYear` (任意): デフォルト現在年度
- `departmentId` (任意): 部署で絞り込み

Response (200):
```json
{
  "fiscalYear": 2026,
  "records": [
    {
      "employeeId": "019059a1-...",
      "employeeName": "田中太郎",
      "departmentName": "開発部",
      "grantedDays": 10.0,
      "usedDays": 2.5,
      "remainingDays": 7.5
    }
  ]
}
```

### エンドポイント × 権限マトリクス（追加分）

| エンドポイント | 未認証 | 一般社員 | 上長 | 管理者 |
|--------------|--------|---------|------|--------|
| POST /api/leaves | - | o | o | o |
| GET /api/leaves | - | o | o | o |
| GET /api/leaves/balance | - | o | o | o |
| GET /api/leaves/pending | - | - | o | - |
| PATCH /api/leaves/{id}/approve | - | - | o | - |
| PATCH /api/leaves/{id}/reject | - | - | o | - |
| GET /api/leaves/summary | - | - | - | o |

---

## 5. 付与日数計算ロジック

### 労基法準拠テーブル

```java
// LeaveGrantCalculator（Service 内のドメインロジック）
static final Map<Double, BigDecimal> GRANT_TABLE = Map.of(
    0.5,  BigDecimal.valueOf(10),
    1.5,  BigDecimal.valueOf(11),
    2.5,  BigDecimal.valueOf(12),
    3.5,  BigDecimal.valueOf(14),
    4.5,  BigDecimal.valueOf(16),
    5.5,  BigDecimal.valueOf(18),
    6.5,  BigDecimal.valueOf(20)
);
```

### 年度起点（4月1日）

- 会計年度: `fiscalYear` = 2026 → 2026/4/1〜2027/3/31
- 年度切替時（4/1）に全社員の LeaveBalance を新規作成

### 初年度按分ロジック

入社日から最初の4/1までの期間が6ヶ月以上あれば初回付与:

```
例: 2026/4/1 入社
  → 2026/10/1 に6ヶ月到達
  → 2026年度（2026/4/1〜2027/3/31）の残月数: 6ヶ月
  → 按分: 10日 × 6/12 = 5日（端数切り上げ）

例: 2026/10/1 入社
  → 2027/4/1 に6ヶ月到達
  → 2027年度から付与対象（初年度按分なし、満額10日）
```

按分計算: `付与日数 × (年度末までの残月数 / 12)` 端数切り上げ

---

## 6. 画面設計

### 新規ページ

| パス | ページ名 | 概要 | 権限 |
|------|---------|------|------|
| `/leaves` | 有給申請一覧 | 自分の申請履歴 + 残日数表示 | 全ロール |
| `/leaves/new` | 有給申請作成 | 日付・種別・理由を入力して申請 | 全ロール |
| `/approvals` | 承認待ち一覧 | 有給申請の承認/却下（既存の修正承認と統合） | 上長 |
| `/admin/leaves` | 有給取得状況 | 全社員の有給取得状況一覧 | 管理者 |

### /leaves（有給申請一覧）

```
┌─────────────────────────────────────────────────┐
│  有給休暇                                        │
├─────────────────────────────────────────────────┤
│  ┌───────────────────────────────────┐          │
│  │ 残日数: 7.5日 / 10.0日            │  [申請]  │
│  │ ████████████░░░░ (75%)            │          │
│  └───────────────────────────────────┘          │
│                                                  │
│  年度: [2026 ▼]  ステータス: [すべて ▼]          │
│                                                  │
│  ┌────────┬──────┬────────┬──────┬──────────┐   │
│  │ 取得日  │ 種別 │ 理由   │状態  │ 承認者   │   │
│  ├────────┼──────┼────────┼──────┼──────────┤   │
│  │ 7/20   │ 全日 │ 私用   │承認済│ 鈴木一郎 │   │
│  │ 7/15   │午前休│ 通院   │申請中│ —        │   │
│  │ 6/10   │ 全日 │ —     │却下  │ 鈴木一郎 │   │
│  └────────┴──────┴────────┴──────┴──────────┘   │
└─────────────────────────────────────────────────┘
```

### /leaves/new（有給申請作成）

```
┌─────────────────────────────────────────────────┐
│  有給休暇申請                                    │
├─────────────────────────────────────────────────┤
│                                                  │
│  残日数: 7.5日                                   │
│                                                  │
│  取得日 *   [ 2026-07-20    ] 📅               │
│                                                  │
│  種別 *     ○ 全日  ○ 午前休  ○ 午後休         │
│                                                  │
│  理由       [ 私用のため              ]          │
│                                                  │
│          [キャンセル]  [申請する]                 │
│                                                  │
└─────────────────────────────────────────────────┘
```

### 勤怠履歴への表示（既存画面の拡張）

勤怠履歴テーブルに有給取得日を表示する:

```
┌────────┬──────┬──────┬──────────┬──────┐
│ 日付    │ 出勤 │ 退勤 │ 勤務時間  │ 備考 │
├────────┼──────┼──────┼──────────┼──────┤
│ 7/18   │ 9:00 │18:00 │ 8:00     │      │
│ 7/19   │  —   │  —   │   —      │有給  │  ← 有給取得日
│ 7/20   │  —   │  —   │   —      │午前休│  ← 半日有給
│ 7/21   │ 9:00 │18:00 │ 8:00     │      │
└────────┴──────┴──────┴──────────┴──────┘
```

---

## 7. パッケージ構成（追加分）

```
com.example.attendance
└── leave/              — 有給休暇（申請・承認・残高管理）
    ├── controller/
    │   └── LeaveController.java
    ├── service/
    │   ├── LeaveService.java (interface)
    │   ├── LeaveServiceImpl.java
    │   └── LeaveGrantCalculator.java
    ├── repository/
    │   ├── LeaveRequestRepository.java
    │   └── LeaveBalanceRepository.java
    ├── entity/
    │   ├── LeaveRequest.java
    │   ├── LeaveBalance.java
    │   ├── LeaveType.java (enum)
    │   └── LeaveStatus.java (enum)
    └── dto/
        ├── LeaveRequestDto.java (record)
        ├── LeaveCreateRequest.java (record)
        ├── LeaveRejectRequest.java (record)
        ├── LeaveBalanceDto.java (record)
        └── LeaveSummaryDto.java (record)
```

Frontend:
```
packages/frontend/src/
├── app/(authenticated)/
│   ├── leaves/
│   │   ├── page.tsx          — 申請一覧
│   │   └── new/
│   │       └── page.tsx      — 申請作成
│   └── admin/
│       └── leaves/
│           └── page.tsx      — 管理者: 取得状況
├── features/
│   └── leave/
│       ├── LeaveRequestList.tsx
│       ├── LeaveRequestForm.tsx
│       ├── LeaveBalanceCard.tsx
│       ├── LeaveSummaryTable.tsx
│       ├── PendingLeaveList.tsx
│       ├── leave-api.ts
│       └── useLeaves.ts
```

---

## 8. 既存機能への影響

### 勤怠履歴 API（`GET /api/attendance/history`）

レスポンスの各日に `leaveType` フィールドを追加:

```json
{
  "date": "2026-07-19",
  "records": [],
  "leaveType": "FULL",
  "totalWorkMinutes": 0,
  ...
}
```

### 月次レポート（`GET /api/reports/monthly`）

レスポンスに `paidLeaveDays` を追加し、`absentDays` から有給取得日を除外:

```json
{
  "employeeName": "田中太郎",
  "workDays": 20,
  "paidLeaveDays": 2.5,
  "absentDays": 0,
  ...
}
```

### 承認画面の統合

既存の `/approvals` ページに有給承認タブを追加（または別タブ）:
- タブ1: 勤怠修正
- タブ2: 有給休暇

---

## 9. ドメイン間依存

```
leave → employee（申請者・承認者の参照）
attendance → leave（履歴表示で有給情報を参照）
report → leave（月次集計で有給取得日数を参照）
```

---

## 10. 設計判断まとめ

| 項目 | 決定 | 根拠 |
|------|------|------|
| テーブル構成 | leave_requests + leave_balances | 申請と残高を分離。残高は年度単位で管理 |
| 残高管理 | 承認時に加算 | 申請時ではなく承認確定時に減算（却下時に戻す必要がない） |
| 付与タイミング | 4/1 一斉 | 要求仕様で確定。管理がシンプル |
| 半日の型 | BigDecimal (DECIMAL 4,1) | 0.5 刻みを正確に扱う |
| API パス | `/api/leaves` | 修正申請 `/api/corrections` と同レベルの新ドメイン |
| 重複チェック | アプリ層 + DB INDEX | 同日同種別の PENDING/APPROVED を許さない |
| 承認画面 | 既存 `/approvals` にタブ追加 | 上長が1画面で全承認タスクを処理できる |
