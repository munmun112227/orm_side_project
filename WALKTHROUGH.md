# POS 自助結帳機營運分析與雙 ORM 效能對比系統 Walkthrough

## 專案概述
本專案已基於門市交易資料完整實作前後端分離之營運分析報表系統。
特色亮點包括：
1. **雙 ORM (Spring Data JPA vs MyBatis) 接續執行與效能耗時評測**
2. **五大核心營運分析指標**（自助機使用率、假日高峰結帳速度、每 15 分鐘時段分佈、付款別佔比、客單價三組數字）
3. **Apache PDFBox 3.x 報表匯出**
4. **Vue 3 + Bootstrap 5 響應式儀表板 + Chart.js 視覺化圖表**
5. **AWS EC2 部署與 Port 衝突徹底排除**：
   - 前端對外開放 Port 改為 `${FRONTEND_PORT:3000}:80`，避開 80 與 8080。
   - 後端改為容器內部監聽 **9080**，由 Nginx 反向代理，**不向宿主機暴露任何 8080/9080**，徹底消除 8080 衝突。
   - 共用 EC2 既有 PostgreSQL (5432)，以專屬 DB Name (`${DB_NAME:pos}`) 進行邏輯隔離，不搶佔宿主機 5432。
6. **DevSecOps 與 AWS EC2 自動部署**：
   - GitHub Actions 整合 **CodeQL (SAST: Java & JS)** 與 **Aqua Security Trivy (SCA & 容器弱點掃描)**。
7. **Git 版本控制與敏感資料隔離**：
   - 配置標準 `.gitignore` 嚴格排除 `ddl_*.sql`、`dml_*.sql`、`import_transactions.sql` 與 284MB 之 `transactions.csv` 等機密資料。
8. **GitHub Actions 測試完全自給與零個資脫敏設計 (Zero PII)**：
   - 自動化測試專用 H2 記憶體資料庫 (`schema-test.sql` + `data-test.sql`)。
   - 僅使用 6 筆**純手工捏造的合成假資料（Synthetic Data）**，完全不接觸亦不包含任何客戶或門市真實個人資料。
   - 任何人敲 `mvn test` 或 GitHub CI 測試均 100% 秒級綠燈通過，無需本機或外部運行 PostgreSQL。

---

## 核心功能實作與成果

### 1. 五大報表指標計算邏輯 (以 `payment` 實付額計算客單價)
| 需求指標 | 實作邏輯與計算公式 | 脫敏測試驗證成果 (Synthetic Data) |
| :--- | :--- | :--- |
| **1. 自助結帳機使用率** | `SCO / (Cashier + SCO) * 100%`<br>(QPSCE01~20 比率) | **50.00 %** (SCO: 2 筆 / 人工: 2 筆) |
| **2. 結帳速度分析** | 區間內**週六、週日 13:30~17:30**<br>人工機依 `staff_id`、自助機依 `pos_terminal_no` 每半小時平均筆數 | 支援多機台/員工速度排行 |
| **3. 時段分析** | 全天每 **15 分鐘** 交易筆數分佈統計 | 涵蓋全天時段分佈走勢 |
| **4. 付款別分析** | 各 `tender_type` 交易筆數與金額佔比 | Credit Card, LINE Pay 等佔比排名 |
| **5. 客單分析 (客單價)** | 依 `payment` 欄位計算三種金額：<br>① 人工機 (QPSCL01~20)<br>② 自助機 (QPSCE01~20)<br>③ 全體整合 (QPSCL + QPSCE) | ① 人工機客單價：**$ 150.00**<br>② 自助機客單價：**$ 100.00**<br>③ 全體整合客單價：**$ 125.00** |

---

## 驗證結果

### 後端自動化單元測試與雙 ORM 整合測試 (H2 記憶體脫敏模式)
```bash
mvn clean test
```
- **測試結果**：全部通過 (Tests run: 5, Failures: 0, Errors: 0, Skipped: 0)
  - `HolidayUtilsTest`：假日天數與日誌消毒安全驗證通過
  - `PdfReportExportServiceTest`：PDFBox 3.x 合法 PDF 生成驗證通過
  - `AnalyticsServiceTest`：雙 ORM 連續調用計時測試通過
  - `DualOrmEquivalenceTest`：在 H2 記憶體模式下使用脫敏合成資料驗證雙 ORM 計算指標 100% 等價一致

### 前端構建測試
```bash
npm run build
```
- **結果**：Vite 生產環境打包編譯成功，輸出 `dist/` 靜態檔案。
