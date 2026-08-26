import type { ApiResponse } from '../types/shipment'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export async function httpClient<T>(
  path: string,
  apiKey: string,
  options: RequestInit = {},
): Promise<ApiResponse<T>> {
  const headers = new Headers(options.headers)

  headers.set('x-api-key', apiKey)

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const body = (await response.json()) as ApiResponse<T>

  if (!response.ok || body.resultCode === 'E') {
    throw new Error(body.message || `HTTP error: ${response.status}`)
  }

  return body
}
