import './style.css';

const ROOM_API_BASE_URL = import.meta.env.VITE_ROOM_API_BASE_URL
  ?? 'http://localhost:8080/api/v1';
const RESERVATION_API_BASE_URL = import.meta.env.VITE_RESERVATION_API_BASE_URL
  ?? 'http://localhost:8081/api/v1';
const ROOM_QUERY_API_BASE_URL = import.meta.env.VITE_ROOM_QUERY_API_BASE_URL
  ?? 'http://localhost:8080/api/v2';
const RESERVATION_QUERY_API_BASE_URL = import.meta.env.VITE_RESERVATION_QUERY_API_BASE_URL
  ?? 'http://localhost:8081/api/v2';

const state = {
  rooms: {
    page: 0,
    size: 5,
    totalPages: 0,
  },
  reservations: {
    page: 0,
    size: 5,
    totalPages: 0,
  },
};

const elements = {
  notice: document.querySelector('#notice'),
  roomFilterForm: document.querySelector('#room-filter-form'),
  roomKeyword: document.querySelector('#room-keyword'),
  roomStatus: document.querySelector('#room-status'),
  roomType: document.querySelector('#room-type'),
  roomMinCapacity: document.querySelector('#room-min-capacity'),
  roomSort: document.querySelector('#room-sort'),
  roomTableBody: document.querySelector('#rooms-table-body'),
  roomEmpty: document.querySelector('#room-empty'),
  roomSummary: document.querySelector('#room-summary'),
  roomPageInfo: document.querySelector('#room-page-info'),
  roomPrevious: document.querySelector('#room-previous'),
  roomNext: document.querySelector('#room-next'),
  resetRoomFilter: document.querySelector('#reset-room-filter'),
  reservationForm: document.querySelector('#reservation-form'),
  reservationRoomId: document.querySelector('#reservation-room-id'),
  reservationSubmit: document.querySelector('#reservation-submit'),
  reservationResult: document.querySelector('#reservation-result'),
  reservationFilterForm: document.querySelector('#reservation-filter-form'),
  reservationFilterRoomId: document.querySelector('#reservation-filter-room-id'),
  reservationFilterRequesterId: document.querySelector('#reservation-filter-requester-id'),
  reservationFilterStatus: document.querySelector('#reservation-filter-status'),
  reservationSort: document.querySelector('#reservation-sort'),
  reservationTableBody: document.querySelector('#reservations-table-body'),
  reservationEmpty: document.querySelector('#reservation-empty'),
  reservationSummary: document.querySelector('#reservation-summary'),
  reservationPageInfo: document.querySelector('#reservation-page-info'),
  reservationPrevious: document.querySelector('#reservation-previous'),
  reservationNext: document.querySelector('#reservation-next'),
  resetReservationFilter: document.querySelector('#reset-reservation-filter'),
};

function buildUrl(baseUrl, path, params = {}) {
  const url = new URL(`${baseUrl}${path}`);

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      url.searchParams.set(key, value);
    }
  });

  return url;
}

async function requestJson(url, options = {}) {
  let response;
  const { headers = {}, ...requestOptions } = options;

  try {
    response = await fetch(url, {
      ...requestOptions,
      headers: {
        Accept: 'application/json',
        ...headers,
      },
    });
  } catch {
    throw {
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Không thể kết nối đến service. Hãy kiểm tra service có đang chạy hay không.',
    };
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
    throw {
      status: response.status,
      code: body?.code ?? 'REQUEST_FAILED',
      message: body?.message ?? `Request thất bại với HTTP ${response.status}.`,
      errors: body?.errors ?? [],
    };
  }

  return body;
}

function normalizePage(payload, requestedPage, requestedSize, requestedSort) {
  if (Array.isArray(payload)) {
    return {
      content: payload,
      page: requestedPage,
      size: requestedSize,
      totalElements: payload.length,
      totalPages: payload.length === 0 ? 0 : 1,
      sort: requestedSort,
    };
  }

  return payload;
}

function showNotice(message) {
  elements.notice.textContent = message;
  elements.notice.classList.remove('hidden');
}

function clearNotice() {
  elements.notice.textContent = '';
  elements.notice.classList.add('hidden');
}

function showReservationResult(type, title, lines) {
  const lineHtml = lines
    .filter(Boolean)
    .map((line) => `<p>${escapeHtml(line)}</p>`)
    .join('');

  elements.reservationResult.className = `result-card ${type}`;
  elements.reservationResult.innerHTML = `<h3>${escapeHtml(title)}</h3>${lineHtml}`;
}

function hideReservationResult() {
  elements.reservationResult.className = 'result-card hidden';
  elements.reservationResult.innerHTML = '';
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function statusClass(status) {
  return String(status ?? '').toLowerCase().replaceAll('_', '-');
}

function formatDateTime(value) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ');
  }

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date);
}

function roomQueryParams() {
  return {
    keyword: elements.roomKeyword.value,
    status: elements.roomStatus.value,
    type: elements.roomType.value,
    minCapacity: elements.roomMinCapacity.value,
    page: state.rooms.page,
    size: state.rooms.size,
    sort: elements.roomSort.value,
  };
}

function reservationQueryParams() {
  return {
    roomId: elements.reservationFilterRoomId.value,
    requesterId: elements.reservationFilterRequesterId.value,
    status: elements.reservationFilterStatus.value,
    page: state.reservations.page,
    size: state.reservations.size,
    sort: elements.reservationSort.value,
  };
}

async function loadRooms() {
  const params = roomQueryParams();
  elements.roomSummary.textContent = 'Đang tải...';
  elements.roomPrevious.disabled = true;
  elements.roomNext.disabled = true;

  try {
    const payload = await requestJson(buildUrl(ROOM_QUERY_API_BASE_URL, '/rooms', params));
    const page = normalizePage(payload, params.page, params.size, params.sort);
    state.rooms.page = page.page;
    state.rooms.totalPages = page.totalPages;

    renderRooms(page);
    clearNotice();
  } catch (error) {
    renderRooms({ content: [], page: state.rooms.page, totalPages: 0, totalElements: 0 });
    showNotice(formatError('Không tải được danh sách phòng', error));
  }
}

function renderRooms(page) {
  const rooms = page.content ?? [];
  elements.roomTableBody.innerHTML = rooms.map((room) => `
    <tr>
      <td><strong>${escapeHtml(room.code)}</strong><br /><small>ID: ${escapeHtml(room.id)}</small></td>
      <td>${escapeHtml(room.building)} / tầng ${escapeHtml(room.floor)}</td>
      <td>${escapeHtml(room.type)}</td>
      <td>${escapeHtml(room.capacity)} người</td>
      <td><span class="status ${statusClass(room.status)}">${escapeHtml(room.status)}</span></td>
      <td>
        <button class="button secondary small select-room" type="button" data-room-id="${escapeHtml(room.id)}">
          Chọn phòng
        </button>
      </td>
    </tr>
  `).join('');

  elements.roomEmpty.classList.toggle('hidden', rooms.length !== 0);
  elements.roomSummary.textContent = `${page.totalElements ?? 0} phòng`;
  elements.roomPageInfo.textContent = page.totalPages > 0
    ? `Trang ${(page.page ?? 0) + 1}/${page.totalPages}`
    : 'Không có kết quả';
  elements.roomPrevious.disabled = (page.page ?? 0) <= 0;
  elements.roomNext.disabled = page.totalPages === 0 || (page.page ?? 0) + 1 >= page.totalPages;

  document.querySelectorAll('.select-room').forEach((button) => {
    button.addEventListener('click', () => {
      elements.reservationRoomId.value = button.dataset.roomId;
      elements.reservationRoomId.focus();
      document.querySelector('#create-title').scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
}

async function loadReservations() {
  const params = reservationQueryParams();
  elements.reservationSummary.textContent = 'Đang tải...';
  elements.reservationPrevious.disabled = true;
  elements.reservationNext.disabled = true;

  try {
    const payload = await requestJson(
      buildUrl(RESERVATION_QUERY_API_BASE_URL, '/reservations', params),
    );
    const page = normalizePage(payload, params.page, params.size, params.sort);
    state.reservations.page = page.page;
    state.reservations.totalPages = page.totalPages;

    renderReservations(page);
    clearNotice();
  } catch (error) {
    renderReservations({ content: [], page: state.reservations.page, totalPages: 0, totalElements: 0 });
    showNotice(formatError('Không tải được danh sách đăng ký', error));
  }
}

function renderReservations(page) {
  const reservations = page.content ?? [];
  elements.reservationTableBody.innerHTML = reservations.map((reservation) => `
    <tr>
      <td>${escapeHtml(reservation.id)}</td>
      <td><strong>${escapeHtml(reservation.roomCode)}</strong><br /><small>ID: ${escapeHtml(reservation.roomId)}</small></td>
      <td>${escapeHtml(reservation.requesterName)}<br /><small>${escapeHtml(reservation.requesterId)}</small></td>
      <td>${escapeHtml(formatDateTime(reservation.startTime))}<br /><small>đến ${escapeHtml(formatDateTime(reservation.endTime))}</small></td>
      <td>${escapeHtml(reservation.attendeeCount)}</td>
      <td><span class="status ${statusClass(reservation.status)}">${escapeHtml(reservation.status)}</span></td>
    </tr>
  `).join('');

  elements.reservationEmpty.classList.toggle('hidden', reservations.length !== 0);
  elements.reservationSummary.textContent = `${page.totalElements ?? 0} đăng ký`;
  elements.reservationPageInfo.textContent = page.totalPages > 0
    ? `Trang ${(page.page ?? 0) + 1}/${page.totalPages}`
    : 'Chưa có đăng ký';
  elements.reservationPrevious.disabled = (page.page ?? 0) <= 0;
  elements.reservationNext.disabled = page.totalPages === 0
    || (page.page ?? 0) + 1 >= page.totalPages;
}

function formatError(prefix, error) {
  const details = error.errors?.length ? ` ${error.errors.join(' | ')}` : '';
  const status = error.status ? ` (HTTP ${error.status})` : '';
  return `${prefix}${status}: ${error.code ?? 'ERROR'} — ${error.message ?? 'Lỗi không xác định.'}${details}`;
}

function readReservationPayload(form) {
  const data = new FormData(form);
  const startTime = data.get('startTime');
  const endTime = data.get('endTime');

  if (startTime >= endTime) {
    throw {
      status: 400,
      code: 'CLIENT_INVALID_TIME',
      message: 'Thời gian kết thúc phải sau thời gian bắt đầu.',
    };
  }

  return {
    roomId: Number(data.get('roomId')),
    requesterId: String(data.get('requesterId')).trim(),
    requesterName: String(data.get('requesterName')).trim(),
    purpose: String(data.get('purpose')).trim(),
    startTime,
    endTime,
    attendeeCount: Number(data.get('attendeeCount')),
  };
}

function attachEventHandlers() {
  elements.roomFilterForm.addEventListener('submit', (event) => {
    event.preventDefault();
    state.rooms.page = 0;
    loadRooms();
  });

  elements.resetRoomFilter.addEventListener('click', () => {
    elements.roomFilterForm.reset();
    state.rooms.page = 0;
    loadRooms();
  });

  elements.roomPrevious.addEventListener('click', () => {
    if (state.rooms.page > 0) {
      state.rooms.page -= 1;
      loadRooms();
    }
  });

  elements.roomNext.addEventListener('click', () => {
    if (state.rooms.page + 1 < state.rooms.totalPages) {
      state.rooms.page += 1;
      loadRooms();
    }
  });

  elements.reservationForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideReservationResult();

    if (!elements.reservationForm.reportValidity()) {
      return;
    }

    let payload;
    try {
      payload = readReservationPayload(elements.reservationForm);
    } catch (error) {
      showReservationResult('error', 'Chưa gửi request', [formatError('Kiểm tra dữ liệu', error)]);
      return;
    }

    elements.reservationSubmit.disabled = true;
    elements.reservationSubmit.textContent = 'Đang gửi...';

    try {
      const reservation = await requestJson(
        buildUrl(RESERVATION_API_BASE_URL, '/reservations'),
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        },
      );

      showReservationResult('success', 'Đăng ký đã được xác nhận', [
        `Reservation ID: ${reservation.id}`,
        `Phòng: ${reservation.roomCode} (ID ${reservation.roomId})`,
        `Trạng thái: ${reservation.status}`,
      ]);
      elements.reservationForm.reset();
      state.reservations.page = 0;
      await loadReservations();
    } catch (error) {
      showReservationResult('error', 'Đăng ký chưa được tạo', [formatError('Reservation Service trả lỗi', error)]);
    } finally {
      elements.reservationSubmit.disabled = false;
      elements.reservationSubmit.textContent = 'Gửi yêu cầu đăng ký';
    }
  });

  elements.reservationFilterForm.addEventListener('submit', (event) => {
    event.preventDefault();
    state.reservations.page = 0;
    loadReservations();
  });

  elements.resetReservationFilter.addEventListener('click', () => {
    elements.reservationFilterForm.reset();
    state.reservations.page = 0;
    loadReservations();
  });

  elements.reservationPrevious.addEventListener('click', () => {
    if (state.reservations.page > 0) {
      state.reservations.page -= 1;
      loadReservations();
    }
  });

  elements.reservationNext.addEventListener('click', () => {
    if (state.reservations.page + 1 < state.reservations.totalPages) {
      state.reservations.page += 1;
      loadReservations();
    }
  });
}

attachEventHandlers();
Promise.all([loadRooms(), loadReservations()]);
