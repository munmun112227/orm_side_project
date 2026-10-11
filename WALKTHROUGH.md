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
   - 自動化測試專用 H2 記憶體資料庫，採用 Spring Boot 官方標準 `create-drop` 搭配 `defer-datasource-initialization`。
   - 僅使用 6 筆純手工合成脫敏資料 (`data-test.sql`)，完全不接觸亦不包含任何真實個資。
   - 在 `.github/workflows/ci-cd.yml` 中配置自動測試診斷輸出，任何錯誤皆自動印出 `surefire-reports` 堆疊日誌。

---

## 驗證結果

### 後端自動化單元測試與雙 ORM 整合測試 (H2 記憶體脫敏模式)
```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
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
