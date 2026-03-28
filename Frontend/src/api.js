async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  })

  const contentType = response.headers.get('content-type') ?? ''
  const isJson = contentType.includes('application/json')
  const payload = isJson ? await response.json() : await response.text()

  if (!response.ok) {
    const message = typeof payload === 'string'
      ? payload
      : payload.message || '요청 처리 중 오류가 발생했습니다.'
    throw new Error(message)
  }

  return payload
}

function toQueryString(params = {}) {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === '' || value === null || value === undefined) {
      return
    }
    searchParams.set(key, String(value))
  })

  const query = searchParams.toString()
  return query ? `?${query}` : ''
}

export function createOrder(body) {
  return request('/api/orders', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function fetchOrder(orderId) {
  return request(`/api/orders/${orderId}`)
}

export function fetchOrders(params = {}) {
  return request(`/api/orders${toQueryString(params)}`)
}

export function fetchOrderDetail(orderId) {
  return request(`/api/orders/${orderId}/detail`)
}

export function runExperiment(body) {
  return request('/api/experiments/run', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function fetchExperiment(runId) {
  return request(`/api/experiments/${runId}`)
}

export function runReconciliation() {
  return request('/api/reconciliations/run', {
    method: 'POST',
  })
}

export function fetchReconciliation(jobId) {
  return request(`/api/reconciliations/${jobId}`)
}
