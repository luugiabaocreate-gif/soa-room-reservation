const SESSION_KEY = 'soa.room-reservation.access-token';

export function decodeJwtPayload(accessToken) {
  const [, payload] = String(accessToken ?? '').split('.');
  if (!payload) {
    throw new Error('Phiên đăng nhập không đúng định dạng JWT.');
  }

  const normalized = payload.replaceAll('-', '+').replaceAll('_', '/');
  const padded = normalized.padEnd(
    normalized.length + ((4 - normalized.length % 4) % 4),
    '=',
  );
  const json = decodeURIComponent(Array.from(atob(padded))
    .map((character) => `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`)
    .join(''));
  const payloadData = JSON.parse(json);

  if (!payloadData.sub || !Array.isArray(payloadData.roles) || !payloadData.roles.length) {
    throw new Error('JWT không có đủ thông tin danh tính và vai trò.');
  }
  if (payloadData.exp && payloadData.exp * 1000 <= Date.now()) {
    throw new Error('Phiên đăng nhập đã hết hạn.');
  }

  return {
    username: payloadData.sub,
    displayName: payloadData.displayName ?? payloadData.sub,
    requesterId: payloadData.requesterId ?? '',
    roles: payloadData.roles,
    expiresAt: payloadData.exp ? payloadData.exp * 1000 : null,
  };
}

export function saveSession(accessToken) {
  sessionStorage.setItem(SESSION_KEY, accessToken);
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

export function currentSession() {
  const accessToken = sessionStorage.getItem(SESSION_KEY);
  if (!accessToken) {
    return null;
  }
  try {
    return { accessToken, profile: decodeJwtPayload(accessToken) };
  } catch {
    clearSession();
    return null;
  }
}

export function hasRole(profile, role) {
  return profile?.roles?.includes(role) ?? false;
}

export function redirectToLogin() {
  window.location.replace('./index.html');
}

export function redirectToApp() {
  window.location.replace('./app.html');
}
