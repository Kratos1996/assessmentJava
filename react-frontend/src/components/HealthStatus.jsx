import './HealthStatus.css'

function HealthStatus({ health }) {
  if (!health) {
    return (
      <div className="health-status unknown">
        <span className="status-indicator">⏳</span>
        <span>Checking backend status...</span>
      </div>
    )
  }

  const isNodeHealthy = health.status === 'ok'
  const javaBackend = health.javaBackend || {}
  const isJavaHealthy = javaBackend.status === 'ok'
  const isOverallHealthy = isNodeHealthy && isJavaHealthy
  const dataSource = javaBackend.dataSource || {}
  const storage = javaBackend.storage || {}
  const fallbackSourceDetail = storage.detail || ''
  const resolvedSourceDetail =
    dataSource.detail ||
    (fallbackSourceDetail.toLowerCase().includes('persistence file')
      ? 'File snapshot'
      : fallbackSourceDetail || 'Unknown')
  const resolvedSourceStatus =
    dataSource.status ||
    (fallbackSourceDetail.toLowerCase().includes('persistence file') ? 'degraded' : 'bad')

  return (
    <div className={`health-status ${isOverallHealthy ? 'healthy' : 'unhealthy'}`}>
      <div className="health-summary">
        <span className="status-indicator">
          {isOverallHealthy ? '✅' : '❌'}
        </span>
        <div>
          <div className="health-title">
            {health.message || 'Backend connectivity'}
          </div>
          <div className="health-subtitle">
            React talks to Node.js, and Node.js proxies to Java.
          </div>
        </div>
      </div>

      <div className="health-grid">
        <div className={`health-pill ${isNodeHealthy ? 'ok' : 'bad'}`}>
          <strong>Node.js backend</strong>
          <span>{isNodeHealthy ? 'Connected' : 'Down'}</span>
        </div>
        <div className={`health-pill ${isJavaHealthy ? 'ok' : 'bad'}`}>
          <strong>Java backend</strong>
          <span>{isJavaHealthy ? 'Connected' : 'Down'}</span>
        </div>
        <div className={`health-pill ${resolvedSourceStatus === 'ok' ? 'ok' : 'bad'}`}>
          <strong>Data source</strong>
          <span>{resolvedSourceDetail}</span>
        </div>
      </div>

      {storage.detail && (
        <div className="health-storage-note">
          <strong>Storage:</strong> {storage.detail}
        </div>
      )}
    </div>
  )
}

export default HealthStatus
