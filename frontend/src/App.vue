<template>
  <div class="min-vh-100 bg-light pb-5">
    <!-- Navbar -->
    <nav class="navbar navbar-dark bg-primary shadow-sm mb-4">
      <div class="container-fluid px-4">
        <span class="navbar-brand mb-0 h1 d-flex align-items-center">
          <i class="bi bi-graph-up-arrow me-2 fs-4"></i>
          POS 自助結帳機營運分析與雙 ORM 效能評測系統
        </span>
        <span class="badge bg-light text-primary py-2 px-3 fw-semibold">
          Spring Boot 3 + JPA & MyBatis + Vue 3
        </span>
      </div>
    </nav>

    <div class="container-fluid px-4">
      <!-- 篩選列 -->
      <div class="card shadow-sm border-0 mb-4">
        <div class="card-body p-3">
          <div class="row g-3 align-items-center">
            <div class="col-auto">
              <label class="form-label fw-bold mb-0 text-secondary">
                <i class="bi bi-calendar3 me-1"></i>統計區間：
              </label>
            </div>
            <div class="col-md-3 col-sm-6">
              <input type="date" class="form-control" v-model="filter.startDate" :disabled="loading" />
            </div>
            <div class="col-auto text-muted">至</div>
            <div class="col-md-3 col-sm-6">
              <input type="date" class="form-control" v-model="filter.endDate" :disabled="loading" />
            </div>
            <div class="col-auto ms-auto d-flex gap-2">
              <button class="btn btn-primary px-4 fw-semibold shadow-sm" @click="loadData" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
                <i v-else class="bi bi-search me-1"></i>
                查詢分析
              </button>
              <button class="btn btn-outline-danger px-3 fw-semibold shadow-sm" @click="exportPdf" :disabled="loading || !summary">
                <span v-if="exporting" class="spinner-border spinner-border-sm me-1" role="status"></span>
                <i v-else class="bi bi-file-earmark-pdf me-1"></i>
                匯出 PDF 報表
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 錯誤訊息提示 -->
      <div v-if="errorMessage" class="alert alert-danger shadow-sm alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>{{ errorMessage }}
        <button type="button" class="btn-close" @click="errorMessage = ''"></button>
      </div>

      <!-- 雙 ORM 效能對比面板 (需求重點 2) -->
      <div v-if="comparison" class="card shadow-sm border-0 mb-4 bg-white">
        <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
          <h5 class="card-title mb-0 fw-bold text-dark d-flex align-items-center">
            <i class="bi bi-cpu-fill text-warning me-2 fs-5"></i>
            雙 ORM 效能對比 (Spring Data JPA vs MyBatis 接續執行)
          </h5>
          <span class="badge bg-success-subtle text-success border border-success px-3 py-2">
            <i class="bi bi-check-circle-fill me-1"></i>數據等價性校驗通過
          </span>
        </div>
        <div class="card-body p-4">
          <div class="row align-items-center g-4">
            <div class="col-md-5">
              <div class="p-3 rounded-3 border bg-light">
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <span class="fw-bold text-primary"><i class="bi bi-lightning-charge me-1"></i>MyBatis</span>
                  <span class="fs-4 fw-bold text-dark">{{ comparison.myBatisDurationMs }} <small class="fs-6 text-muted">ms</small></span>
                </div>
                <div class="progress" style="height: 10px;">
                  <div class="progress-bar bg-primary" :style="{ width: calcProgress(comparison.myBatisDurationMs, comparison.jpaDurationMs) + '%' }"></div>
                </div>
              </div>

              <div class="p-3 rounded-3 border bg-light mt-3">
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <span class="fw-bold text-success"><i class="bi bi-database me-1"></i>Spring Data JPA</span>
                  <span class="fs-4 fw-bold text-dark">{{ comparison.jpaDurationMs }} <small class="fs-6 text-muted">ms</small></span>
                </div>
                <div class="progress" style="height: 10px;">
                  <div class="progress-bar bg-success" :style="{ width: calcProgress(comparison.jpaDurationMs, comparison.myBatisDurationMs) + '%' }"></div>
                </div>
              </div>
            </div>

            <div class="col-md-7 border-start">
              <div class="ps-md-3">
                <div class="d-flex align-items-center gap-2 mb-2">
                  <span class="badge bg-primary px-3 py-2 fs-6">
                    <i class="bi bi-trophy-fill me-1 text-warning"></i>更快的 ORM：{{ comparison.fasterOrm }}
                  </span>
                  <span class="badge bg-secondary-subtle text-secondary px-3 py-2">接續執行順序：MyBatis ➔ JPA</span>
                </div>
                <p class="text-secondary mb-2 mt-3 fs-6">
                  <i class="bi bi-info-circle me-1"></i><strong>比對說明：</strong>
                  {{ comparison.speedComparisonSummary }}
                </p>
                <small class="text-muted d-block">
                  * 每次報表查詢時，後端於相同請求執行緒中接續觸發 MyBatis 與 JPA 聚合查詢，精準記錄資料庫擷取與物件映射時間。
                </small>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 核心指標 KPI 卡片 (需求重點 1 & 5) -->
      <div v-if="summary" class="row g-3 mb-4">
        <!-- 1. 自助結帳機使用率 -->
        <div class="col-md-3 col-sm-6">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted fw-semibold">自助結帳機使用率</span>
                <span class="badge bg-primary-subtle text-primary p-2"><i class="bi bi-phone fs-6"></i></span>
              </div>
              <h2 class="fw-bold text-primary mb-1">{{ summary.selfCheckoutUsageRate }} %</h2>
              <div class="text-muted small mt-2">
                SCO: <strong>{{ summary.selfCheckoutTransactionCount?.toLocaleString() }}</strong> 筆 /
                人工: <strong>{{ summary.cashierTransactionCount?.toLocaleString() }}</strong> 筆
              </div>
            </div>
          </div>
        </div>

        <!-- 5. 客單 (人工機) -->
        <div class="col-md-3 col-sm-6">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted fw-semibold">人工機客單價 (QPSCL)</span>
                <span class="badge bg-info-subtle text-info p-2"><i class="bi bi-person-badge fs-6"></i></span>
              </div>
              <h2 class="fw-bold text-dark mb-1">$ {{ summary.cashierAverageTicket }}</h2>
              <div class="text-muted small mt-2">
                總實收額：$ {{ summary.cashierTotalPayment?.toLocaleString() }}
              </div>
            </div>
          </div>
        </div>

        <!-- 5. 客單 (自助機) -->
        <div class="col-md-3 col-sm-6">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted fw-semibold">自助機客單價 (QPSCE)</span>
                <span class="badge bg-success-subtle text-success p-2"><i class="bi bi-display fs-6"></i></span>
              </div>
              <h2 class="fw-bold text-success mb-1">$ {{ summary.selfCheckoutAverageTicket }}</h2>
              <div class="text-muted small mt-2">
                總實收額：$ {{ summary.selfCheckoutTotalPayment?.toLocaleString() }}
              </div>
            </div>
          </div>
        </div>

        <!-- 5. 客單 (全體整合) -->
        <div class="col-md-3 col-sm-6">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted fw-semibold">全體整合客單價</span>
                <span class="badge bg-warning-subtle text-warning p-2"><i class="bi bi-cart-check fs-6"></i></span>
              </div>
              <h2 class="fw-bold text-dark mb-1">$ {{ summary.overallAverageTicket }}</h2>
              <div class="text-muted small mt-2">
                總分析筆數：<strong>{{ summary.totalAnalyzedCount?.toLocaleString() }}</strong> 筆
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 圖表與詳細分析區 (需求重點 3, 4, 2) -->
      <div v-if="summary" class="row g-4 mb-4">
        <!-- 3. 每 15 分鐘時段分佈分析 -->
        <div class="col-lg-8">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-white border-bottom py-3">
              <h5 class="card-title mb-0 fw-bold text-dark d-flex align-items-center">
                <i class="bi bi-clock-history text-primary me-2"></i>
                時段分析 (全天每 15 分鐘交易筆數分佈)
              </h5>
            </div>
            <div class="card-body p-3">
              <div style="height: 320px;">
                <canvas id="timeDistributionChart"></canvas>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. 付款別分析 (Tender Type) -->
        <div class="col-lg-4">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-white border-bottom py-3">
              <h5 class="card-title mb-0 fw-bold text-dark d-flex align-items-center">
                <i class="bi bi-credit-card-2-front text-info me-2"></i>
                付款別佔比分析
              </h5>
            </div>
            <div class="card-body p-3">
              <div style="height: 220px;" class="mb-3">
                <canvas id="tenderTypeChart"></canvas>
              </div>
              <div class="table-responsive" style="max-height: 160px; overflow-y: auto;">
                <table class="table table-sm table-hover align-middle mb-0">
                  <thead class="table-light">
                    <tr>
                      <th>支付別</th>
                      <th class="text-end">筆數</th>
                      <th class="text-end">佔比</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, idx) in summary.tenderTypeDistributions" :key="idx">
                      <td class="small text-truncate" style="max-width: 140px;">{{ item.tenderType }}</td>
                      <td class="text-end small">{{ item.transactionCount?.toLocaleString() }}</td>
                      <td class="text-end small fw-bold text-primary">{{ item.percentage }} %</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 結帳速度分析 (假日 13:30~17:30) -->
      <div v-if="summary" class="row g-4">
        <!-- 人工結帳機 staff_id 速度 -->
        <div class="col-lg-6">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
              <h5 class="card-title mb-0 fw-bold text-dark d-flex align-items-center">
                <i class="bi bi-person-lines-fill text-primary me-2"></i>
                人工結帳機 (QPSCL) 個別員工結帳速度
              </h5>
              <span class="badge bg-light text-muted border">
                假日 13:30~17:30 (共 {{ summary.holidayDaysCount }} 天)
              </span>
            </div>
            <div class="card-body p-0">
              <div class="table-responsive" style="max-height: 320px; overflow-y: auto;">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light sticky-top">
                    <tr>
                      <th class="ps-3">排名</th>
                      <th>員工編號 (Staff ID)</th>
                      <th class="text-end">總交易筆數</th>
                      <th class="text-end pe-3">每半小時平均筆數</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(s, idx) in summary.cashierStaffSpeeds" :key="idx">
                      <td class="ps-3 fw-bold text-secondary">#{{ idx + 1 }}</td>
                      <td class="fw-semibold">{{ s.identifier }}</td>
                      <td class="text-end">{{ s.totalTransactions?.toLocaleString() }}</td>
                      <td class="text-end pe-3 fw-bold text-primary">{{ s.avgTransactionsPerHalfHour }} <small class="text-muted">筆/0.5hr</small></td>
                    </tr>
                    <tr v-if="!summary.cashierStaffSpeeds || summary.cashierStaffSpeeds.length === 0">
                      <td colspan="4" class="text-center text-muted py-4">查無假日時段交易資料</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <!-- 自助結帳機 pos_terminal_no 速度 -->
        <div class="col-lg-6">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
              <h5 class="card-title mb-0 fw-bold text-dark d-flex align-items-center">
                <i class="bi bi-pc-display-horizontal text-success me-2"></i>
                自助結帳機 (QPSCE) 機台結帳速度
              </h5>
              <span class="badge bg-light text-muted border">
                假日 13:30~17:30 (共 {{ summary.holidayDaysCount }} 天)
              </span>
            </div>
            <div class="card-body p-0">
              <div class="table-responsive" style="max-height: 320px; overflow-y: auto;">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light sticky-top">
                    <tr>
                      <th class="ps-3">機號</th>
                      <th>機台代號 (Terminal No.)</th>
                      <th class="text-end">總交易筆數</th>
                      <th class="text-end pe-3">每半小時平均筆數</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(s, idx) in summary.selfCheckoutTerminalSpeeds" :key="idx">
                      <td class="ps-3 fw-bold text-secondary">#{{ idx + 1 }}</td>
                      <td class="fw-semibold">{{ s.identifier }}</td>
                      <td class="text-end">{{ s.totalTransactions?.toLocaleString() }}</td>
                      <td class="text-end pe-3 fw-bold text-success">{{ s.avgTransactionsPerHalfHour }} <small class="text-muted">筆/0.5hr</small></td>
                    </tr>
                    <tr v-if="!summary.selfCheckoutTerminalSpeeds || summary.selfCheckoutTerminalSpeeds.length === 0">
                      <td colspan="4" class="text-center text-muted py-4">查無假日時段交易資料</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading Overlay -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status">
          <span class="visually-hidden">載入中...</span>
        </div>
        <p class="text-muted mt-3 fw-semibold">正在執行雙 ORM 聚合運算與效能評測，請稍候...</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import Chart from 'chart.js/auto'
import { fetchOrmComparison, exportPdfReport } from './api/reportApi'

const filter = ref({
  startDate: '2024-01-01',
  endDate: '2024-01-31'
})

const loading = ref(false)
const exporting = ref(false)
const errorMessage = ref('')
const comparison = ref(null)
const summary = ref(null)

let timeChartInstance = null
let tenderChartInstance = null

function calcProgress(val, other) {
  const total = (val || 1) + (other || 1)
  return Math.min(100, Math.max(10, Math.round((val / total) * 100)))
}

async function loadData() {
  if (!filter.value.startDate || !filter.value.endDate) {
    errorMessage.value = '請輸入完整的起始與結束日期'
    return
  }
  if (filter.value.startDate > filter.value.endDate) {
    errorMessage.value = '起始日期不得晚於結束日期'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const res = await fetchOrmComparison(filter.value.startDate, filter.value.endDate)
    comparison.value = res
    summary.value = res.data

    await nextTick()
    renderCharts()
  } catch (err) {
    errorMessage.value = '查詢失敗：' + (err.response?.data?.message || err.message)
  } finally {
    loading.value = false
  }
}

async function exportPdf() {
  exporting.value = true
  try {
    await exportPdfReport(filter.value.startDate, filter.value.endDate)
  } catch (err) {
    errorMessage.value = 'PDF 匯出失敗：' + err.message
  } finally {
    exporting.value = false
  }
}

function renderCharts() {
  if (!summary.value) return

  // 1. 時段分佈圖
  const timeCanvas = document.getElementById('timeDistributionChart')
  if (timeCanvas) {
    if (timeChartInstance) timeChartInstance.destroy()

    const labels = summary.value.timeDistributions.map(item => item.timeSlot)
    const cashierData = summary.value.timeDistributions.map(item => item.cashierCount)
    const scoData = summary.value.timeDistributions.map(item => item.selfCheckoutCount)

    timeChartInstance = new Chart(timeCanvas, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: '人工結帳機 (QPSCL)',
            data: cashierData,
            borderColor: '#0d6efd',
            backgroundColor: 'rgba(13, 110, 253, 0.1)',
            fill: true,
            tension: 0.3
          },
          {
            label: '自助結帳機 (QPSCE)',
            data: scoData,
            borderColor: '#198754',
            backgroundColor: 'rgba(25, 135, 84, 0.1)',
            fill: true,
            tension: 0.3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'top' }
        },
        scales: {
          y: { beginAtZero: true, title: { display: true, text: '交易筆數' } },
          x: { title: { display: true, text: '15 分鐘時段' } }
        }
      }
    })
  }

  // 2. 付款別圓餅圖
  const tenderCanvas = document.getElementById('tenderTypeChart')
  if (tenderCanvas) {
    if (tenderChartInstance) tenderChartInstance.destroy()

    const topTenders = summary.value.tenderTypeDistributions.slice(0, 6)
    const labels = topTenders.map(item => item.tenderType)
    const data = topTenders.map(item => item.transactionCount)

    tenderChartInstance = new Chart(tenderCanvas, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: [
            '#0d6efd', '#20c997', '#ffc107', '#dc3545', '#6f42c1', '#fd7e14'
          ]
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 10 } } }
        }
      }
    })
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.progress {
  border-radius: 6px;
  background-color: #e9ecef;
}
</style>
