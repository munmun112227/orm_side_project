# POS 自助結帳機與營運分析報表系統實施計畫

## 專案目標與背景
本專案基於 PostgreSQL `transactions` 資料表（全量 156 萬筆 / 採樣 5,000 筆之門市交易紀錄），建置具備互動式儀表板與報表匯出功能的前後端分離系統。
核心特色包含：
1. **雙 ORM 效能對比 (Spring Data JPA vs MyBatis)**：同一個報表計算請求中，接續執行兩大 ORM 並精準測量與比對查詢耗時（毫秒數），在前端頁面展示視覺化對比指標。
2. **五大營運分析報表邏輯**：自助結帳使用率、結帳速度（假日高峰時段）、15 分鐘時段分佈、付款別佔比、客單價分析（人工/自助/全體，以 `payment` 計算）。
3. **Apache PDFBox 報表匯出**：生成中文排版完整之 PDF 營運分析報表。
4. **AWS EC2 部署與埠號衝突排除**：
   - 避開 EC2 既有之 **8080** 與 **5432** 衝突。
   - Vue 前端映射至 `${FRONTEND_PORT:3000}:80`。
   - 後端不向宿主機暴露 8080/9080，透過 Docker 內部網路反向代理。
   - 共用 EC2 既有 PostgreSQL，以獨立 DB Name (`pos`) 進行邏輯隔離。
5. **DevSecOps**：GitHub Actions 整合 **CodeQL (SAST)** 與 **Trivy (SCA / 容器 CVE 掃描)**。
6. **版本控制過濾 (.gitignore)**：排除 DDL、DML、SQL 匯入腳本與交易 CSV 檔案，防範資料外洩。

---

## 指標計算邏輯詳細規範

- **機台分類定義**：
  - 人工結帳機台 (Cashier POS, QPSCL)：`pos_terminal_no BETWEEN 'QPSCL01' AND 'QPSCL20'`
  - 自助結帳機台 (Self-checkout, QPSCE)：`pos_terminal_no BETWEEN 'QPSCE01' AND 'QPSCE20'`
  - 排除教育訓練/測試交易：`training IS NOT TRUE`（`training = false` 或 `training IS NULL`）

### 1. 自助結帳機使用率 (SCO Usage Rate)
$$\text{SCO使用率 (\%)} = \frac{\text{Count}(\text{QPSCE01}\sim\text{QPSCE20})}{\text{Count}(\text{QPSCL01}\sim\text{QPSCL20}) + \text{Count}(\text{QPSCE01}\sim\text{QPSCE20})} \times 100\%$$

### 2. 結帳速度分析 (Checkout Speed)
- 區間內**週六、週日 13:30~17:30**
- 人工機依 `staff_id`、自助機依 `pos_terminal_no` 個別每半小時平均筆數。

### 3. 時段分佈分析 (Time Interval Distribution)
- 全天每 15 分鐘（共 96 個時段）分別統計 QPSCL、QPSCE 及全體交易筆數。

### 4. 付款別分析 (Tender Type Distribution)
- 各 `tender_type` 筆數與佔總交易筆數百分比。

### 5. 客單分析 (Average Ticket Size)
- 採用 `payment`（實付金額）計算人工、自助、全體三組均額。

---

## .gitignore 規則規劃
```gitignore
# DDL 與 DML 資料庫腳本 (防止敏感資料外洩)
*ddl*.sql
*DDL*.sql
*dml*.sql
*DML*.sql
*.sql
*.csv
*.xlsx

# Maven & Java
target/
*.class
*.jar

# Node & Frontend
node_modules/
dist/

# OS & IDE
.DS_Store
.idea/
.vscode/
*.log
```
