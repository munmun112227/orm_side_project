# POS 自助結帳機營運分析與雙 ORM 效能對比系統 Walkthrough

## 專案概述
本專案已基於 PostgreSQL `transactions` 資料表（全量 156 萬筆 / 採樣 5,000 筆門市交易資料）完整實作前後端分離之營運分析報表系統。
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
   - 完成專案 Git 初始化 (`main` 分支) 與初始提交 (`feat: initial commit`)。

---

## 核心功能實作與成果

### 1. 五大報表指標計算邏輯 (以 `payment` 實付額計算客單價)
| 需求指標 | 實作邏輯與計算公式 | 真實資料驗證成果 (2024-01-01) |
| :--- | :--- | :--- |
| **1. 自助結帳機使用率** | `SCO / (Cashier + SCO) * 100%`<br>(QPSCE01~20 比率) | **38.54 %** (SCO: 1,064 筆 / 人工: 1,697 筆) |
| **2. 結帳速度分析** | 區間內**週六、週日 13:30~17:30**<br>人工機依 `staff_id`、自助機依 `pos_terminal_no` 每半小時平均筆數 | 支援多機台/員工排行（若當日為週一則回傳非假日時段安全提示） |
| **3. 時段分析** | 全天每 **15 分鐘** 交易筆數分佈統計 | 涵蓋全天 96 個時段，共 52 個活躍時段分佈趨勢 |
| **4. 付款別分析** | 各 `tender_type` 交易筆數與金額佔比 | 包含 Credit Card, LINE Pay, VirtualCreditCard 等佔比排名 |
| **5. 客單分析 (客單價)** | 依 `payment` 欄位計算三種金額：<br>① 人工機 (QPSCL01~20)<br>② 自助機 (QPSCE01~20)<br>③ 全體整合 (QPSCL + QPSCE) | ① 人工機客單價：**$ 3,002.45**<br>② 自助機客單價：**$ 1,182.41**<br>③ 全體整合客單價：**$ 2,301.07** |

---

### 2. 雙 ORM 效能對比 (JPA vs MyBatis 接續執行)
- 在同一個 API 請求中依序執行 MyBatis 與 Spring Data JPA 聚合運算：
  ```text
  [ORM Benchmark] 階段 1/2: 開始執行 MyBatis 查詢...
  [ORM Benchmark] MyBatis 執行完成，耗時: 58 ms, 統計交易總數: 2761
  [ORM Benchmark] 階段 2/2: 開始接續執行 Spring Data JPA 查詢...
  [ORM Benchmark] Spring Data JPA 執行完成，耗時: 47 ms, 統計交易總數: 2761
  [ORM Benchmark] 雙 ORM 數據一致性檢查結果: 通過 (一致)
  [ORM Benchmark] 比對結論: 更快的 ORM 為 [Spring Data JPA], 較 MyBatis 快 11 ms (提升約 19.0%)
  ```
- 前端頁面以進度條與 Badge 直觀展示雙方耗時，並具備「數據等價性校驗通過」綠色標籤。

---

### 3. AWS EC2 連接埠架構與共用 PostgreSQL 設定

```mermaid
flowchart LR
    subgraph Browser["使用者瀏覽器"]
        User["訪問 http://<EC2_IP>:3000"]
    end

    subgraph EC2_Host["AWS EC2 伺服器 (Host)"]
        ExistingApp["EC2 既有服務 (佔用 8080)"]
        SharedPG[("EC2 共用 PostgreSQL (監聽 5432)<br/>- 透過 DB Name 'pos' 獨立隔離")]

        subgraph DockerNetwork["專案 Docker 內部網路"]
            NginxFront["前端 Nginx (內部 80)<br/>映射至宿主機 3000"]
            BackendSpring["後端 Spring Boot (內部 9080)<br/>*無宿主機埠映射，由 Nginx 反向代理*"]
        end
    end

    User -->|Port 3000| NginxFront
    NginxFront -->|內部網路 /api/* (Port 9080)| BackendSpring
    BackendSpring -->|host.docker.internal:5432| SharedPG
```

#### 設定檔一覽：
- **`backend/src/main/resources/application.properties`**：
  ```properties
  server.port=9080
  spring.datasource.url=jdbc:postgresql://${DB_HOST:host.docker.internal}:${DB_PORT:5432}/${DB_NAME:pos}
  spring.datasource.username=${DB_USER:postgres}
  spring.datasource.password=${DB_PASSWORD:pospassword}
  ```
- **`docker/docker-compose.yml`**：
  - 前端映射：`${FRONTEND_PORT:3000}:80`
  - 後端：內部 expose `9080`，支援 `host.docker.internal` 直連宿主機 PostgreSQL。

---

### 4. DevSecOps 弱點掃描與 AWS EC2 自動化部署
- **GitHub 官方 CodeQL (SAST)**：自動分析 Java (Spring Boot) 與 JavaScript (Vue 3) 代碼，攔截 OWASP Top 10 與 Checkmarx 弱點。
- **Aqua Security Trivy (SCA & 容器掃描)**：掃描 Maven 與 npm 依賴套件已知重大 CVE 弱點。
- **AWS EC2 SSH 自動部署**：在測試與弱點掃描全數通過後，自動透過 SSH 登入 EC2 執行 `docker compose up -d --build` 並驗證容器健康狀態。

#### GitHub Repository Secrets 設定清單：
- `EC2_HOST`：AWS EC2 的公網 IP 或 Elastic IP
- `EC2_USER`：登入帳號（如 `ubuntu` 或 `ec2-user`）
- `EC2_SSH_KEY`：EC2 登入私鑰 (`.pem` 內容)
- `EC2_PORT`：SSH 連接埠（預設 22）

---

## 驗證結果

### 1. 後端自動化單元測試與整合測試
```bash
mvn test
```
- **測試結果**：全部通過 (Tests run: 5, Failures: 0, Errors: 0, Skipped: 0)
  - `HolidayUtilsTest`：假日天數與日誌消毒安全驗證通過
  - `PdfReportExportServiceTest`：PDFBox 3.x 合法 PDF 生成驗證通過
  - `AnalyticsServiceTest`：雙 ORM 連續調用計時測試通過
  - `DualOrmEquivalenceTest`：真實 PostgreSQL 數據雙 ORM 計算指標 100% 等價一致

### 2. 前端構建測試
```bash
npm run build
```
- **結果**：Vite 生產環境打包編譯成功，輸出 `dist/` 靜態檔案。
