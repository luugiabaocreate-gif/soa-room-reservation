// Browser traffic has one public entry point after TH7. The gateway resolves
// service locations through Eureka; the browser never sees service instance ports.
const API_GATEWAY_BASE_URL = import.meta.env.VITE_API_GATEWAY_BASE_URL
  ?? 'http://localhost:8088';
const AUTH_API_BASE_URL = import.meta.env.VITE_AUTH_API_BASE_URL
  ?? `${API_GATEWAY_BASE_URL}/api/v1`;
const ROOM_API_BASE_URL = import.meta.env.VITE_ROOM_API_BASE_URL
  ?? `${API_GATEWAY_BASE_URL}/api/v1`;
const ROOM_QUERY_API_BASE_URL = import.meta.env.VITE_ROOM_QUERY_API_BASE_URL
  ?? `${API_GATEWAY_BASE_URL}/api/v2`;
const RESERVATION_API_BASE_URL = import.meta.env.VITE_RESERVATION_API_BASE_URL
  ?? `${API_GATEWAY_BASE_URL}/api/v1`;
const RESERVATION_QUERY_API_BASE_URL = import.meta.env.VITE_RESERVATION_QUERY_API_BASE_URL
  ?? `${API_GATEWAY_BASE_URL}/api/v2`;
const AVAILABILITY_API_BASE_URL = import.meta.env.VITE_AVAILABILITY_API_BASE_URL
  ?? `${API_GATEWAY_BASE_URL}/api/v3`;

// Diagnostics are deliberately not rendered as a business feature. They are
// available to the developer console for TH7 operational verification only.
export const serviceUrls = {
  gatewayHealth: `${API_GATEWAY_BASE_URL}/actuator/health`,
  registry: import.meta.env.VITE_SERVICE_REGISTRY_URL ?? 'http://localhost:8761',
};

export function buildUrl(baseUrl, path = '', params = {}) {
  const url = new URL(`${baseUrl}${path}`);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      url.searchParams.set(key, value);
    }
  });
  return url;
}

export class ApiError extends Error {
  constructor({ status = 0, code = 'REQUEST_FAILED', message, errors = [] }) {
    super(message);
    this.status = status;
    this.code = code;
    this.errors = errors;
  }
}

export async function requestJson(url, { accessToken, skipAuth = false, headers = {}, ...options } = {}) {
  let response;
  try {
    response = await fetch(url, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(!skipAuth && accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...headers,
      },
    });
  } catch {
    throw new ApiError({
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Could not reach API Gateway. Check the gateway, Service Registry, and services.',
    });
  }

  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = { message: text };
    }
  }

  if (!response.ok) {
    throw new ApiError({
      status: response.status,
      code: body?.code ?? 'REQUEST_FAILED',
      message: body?.message ?? `Request failed with HTTP ${response.status}.`,
      errors: body?.errors ?? [],
    });
  }

  return body;
}

export const authApi = {
  login(username, password) {
    return requestJson(buildUrl(AUTH_API_BASE_URL, '/auth/login'), {
      method: 'POST',
      skipAuth: true,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
  },
};

export const roomApi = {
  list(params, accessToken) {
    return requestJson(buildUrl(ROOM_QUERY_API_BASE_URL, '/rooms', params), { accessToken });
  },
  create(payload, accessToken) {
    return requestJson(buildUrl(ROOM_API_BASE_URL, '/rooms'), {
      method: 'POST',
      accessToken,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  },
  update(id, payload, accessToken) {
    return requestJson(buildUrl(ROOM_API_BASE_URL, `/rooms/${id}`), {
      method: 'PUT',
      accessToken,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  },
  remove(id, accessToken) {
    return requestJson(buildUrl(ROOM_API_BASE_URL, `/rooms/${id}`), {
      method: 'DELETE',
      accessToken,
    });
  },
};

export const reservationApi = {
  list(params, accessToken) {
    return requestJson(buildUrl(RESERVATION_QUERY_API_BASE_URL, '/reservations', params), { accessToken });
  },
  create(payload, accessToken) {
    return requestJson(buildUrl(RESERVATION_API_BASE_URL, '/reservations'), {
      method: 'POST',
      accessToken,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  },
  cancel(id, accessToken) {
    return requestJson(buildUrl(RESERVATION_API_BASE_URL, `/reservations/${id}/cancel`), {
      method: 'PATCH',
      accessToken,
    });
  },
  availability(params, accessToken) {
    return requestJson(buildUrl(AVAILABILITY_API_BASE_URL, '/availability', params), { accessToken });
  },
};

export function describeApiError(error) {
  if (!(error instanceof ApiError)) {
    return 'An unexpected error occurred.';
  }

  const details = error.errors?.length ? ` ${error.errors.join(' · ')}` : '';
  const explanations = {
    AUTHENTICATION_REQUIRED: 'Your session is invalid or expired. Please sign in again.',
    ACCESS_DENIED: 'Your account does not have permission for this action.',
    ROOM_NOT_FOUND: 'This room no longer exists in Room Service.',
    ROOM_NOT_AVAILABLE: 'This room no longer meets the required operating state or capacity.',
    ROOM_SCHEDULE_CONFLICT: 'Another Reservation just occupied this time slot. Search again for a room.',
    ROOM_SERVICE_UNAVAILABLE: 'Room Service is temporarily unavailable. No data was created.',
    RESERVATION_NOT_CANCELLABLE: 'This Reservation is no longer in a cancellable state.',
  };

  return `${explanations[error.code] ?? error.message}${details}`;
}
