import axios from 'axios'

const API_BASE = '/api'

export async function fetchOrmComparison(startDate, endDate) {
  const response = await axios.get(`${API_BASE}/analytics/compare`, {
    params: { startDate, endDate }
  })
  return response.data
}

export async function exportPdfReport(startDate, endDate) {
  const response = await axios.get(`${API_BASE}/reports/export/pdf`, {
    params: { startDate, endDate },
    responseType: 'blob'
  })

  // 觸發瀏覽器下載檔案
  const blob = new Blob([response.data], { type: 'application/pdf' })
  const downloadUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.setAttribute('download', `pos-report-${startDate}-to-${endDate}.pdf`)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(downloadUrl)
}
