import './style.css';
import {
  describeApiError,
  reservationApi,
  roomApi,
  serviceUrls,
} from './core/api.js';
import {
  clearSession,
  currentSession,
  hasRole,
  redirectToLogin,
} from './core/session.js';

const session = currentSession();
if (!session) {
  redirectToLogin();
}

// The replacement navigation above exits the page in a browser. The fallback
// keeps module evaluation safe for static tooling during that transition.
const activeSession = session ?? {
  accessToken: '',
  profile: { username: '', displayName: '', requesterId: '', roles: [], expiresAt: null },
};

const state = {
  accessToken: activeSession.accessToken,
  profile: activeSession.profile,
  currentView: 'dashboard',
  rooms: { page: 0, totalPages: 0, size: 12, content: [] },
  reservations: { page: 0, totalPages: 0, size: 8, content: [] },
  operationRooms: [],
  availability: null,
  selectedRoom: null,
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

const elements = {
  sidebar: $('.sidebar'),
  mascot: $('#operations-mascot'),
  assistantLine: $('#assistant-line'),
  mobileMenu: $('#mobile-menu-button'),
  navButtons: $$('[data-view-target]'),
  views: $$('[data-view]'),
  viewTitle: $('#view-title'),
  viewKicker: $('#view-kicker'),
  logout: $('#logout-button'),
  toastRegion: $('#toast-region'),
  dashboardGreeting: $('#dashboard-greeting'),
  dashboardCopy: $('#dashboard-copy'),
  dashboardMetrics: $('#dashboard-metrics'),
  dashboardReservations: $('#dashboard-reservations'),
  dashboardReservationsTitle: $('#dashboard-reservations-title'),
  countdown: $('#session-countdown'),
  availabilityForm: $('#availability-form'),
  availabilityFrom: $('#availability-from'),
  availabilityTo: $('#availability-to'),
  availabilityAttendees: $('#availability-attendee-count'),
  availabilityType: $('#availability-type'),
  availabilityResults: $('#availability-results'),
  availabilitySummary: $('#availability-summary'),
  availabilityDescription: $('#availability-description'),
  availableRoomGrid: $('#available-room-grid'),
  confirmation: $('#booking-confirmation'),
  selectedRoomId: $('#selected-room-id'),
  selectedRoomSummary: $('#selected-room-summary'),
  reservationForm: $('#reservation-form'),
  reservationPurpose: $('#reservation-purpose'),
  reservationAttendees: $('#reservation-attendee-count'),
  reservationRequesterId: $('#reservation-requester-id'),
  reservationRequesterName: $('#reservation-requester-name'),
  reservationSubmit: $('#reservation-submit'),
  changeRoom: $('#change-room-button'),
  reservationViewTitle: $('#reservation-view-title'),
  reservationViewCopy: $('#reservation-view-copy'),
  reservationFilterForm: $('#reservation-filter-form'),
  reservationFilterRoomId: $('#reservation-filter-room-id'),
  reservationFilterStatus: $('#reservation-filter-status'),
  reservationFilterFrom: $('#reservation-filter-from'),
  reservationFilterTo: $('#reservation-filter-to'),
  resetReservationFilter: $('#reset-reservation-filter'),
  reservationTable: $('#reservation-table-wrap'),
  reservationPrevious: $('#reservation-previous'),
  reservationNext: $('#reservation-next'),
  reservationPage: $('#reservation-page-info'),
  roomFilterForm: $('#room-filter-form'),
  roomKeyword: $('#room-keyword'),
  roomStatus: $('#room-status'),
  roomType: $('#room-type'),
  roomMinCapacity: $('#room-min-capacity'),
  resetRoomFilter: $('#reset-room-filter'),
  roomGrid: $('#room-directory-grid'),
  roomPrevious: $('#room-previous'),
  roomNext: $('#room-next'),
  roomPage: $('#room-page-info'),
  operationForm: $('#room-operation-form'),
  operationFormTitle: $('#room-form-title'),
  operationRoomId: $('#operation-room-id'),
  operationCode: $('#operation-room-code'),
  operationBuilding: $('#operation-room-building'),
  operationFloor: $('#operation-room-floor'),
  operationType: $('#operation-room-type'),
  operationCapacity: $('#operation-room-capacity'),
  operationStatus: $('#operation-room-status'),
  operationTotalComputers: $('#operation-total-computers'),
  operationWorkingComputers: $('#operation-working-computers'),
  operationBrokenComputers: $('#operation-broken-computers'),
  operationSubmit: $('#room-operation-submit'),
  clearOperation: $('#clear-room-form'),
  operationRoomList: $('#operation-room-list'),
  operationRoomCount: $('#operation-room-count'),
};

const isStudent = () => hasRole(state.profile, 'STUDENT');
const isAdmin = () => hasRole(state.profile, 'ADMIN');
const isOperator = () => hasRole(state.profile, 'ROOM_MANAGER') || isAdmin();
const canCreateReservation = () => isStudent() || isAdmin();

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function toLocalInputValue(date) {
  const pad = (number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function initials(name) {
  return String(name ?? 'U').split(/\s+/).filter(Boolean).slice(0, 2)
    .map((part) => part[0]).join('').toUpperCase();
}

function statusPill(status) {
  const normalized = String(status ?? '').toLowerCase().replaceAll('_', '-');
  return `<span class="status-pill ${escapeHtml(normalized)}">${escapeHtml(status)}</span>`;
}

function setMascotMood(mood = 'idle', message) {
  elements.mascot.className = `operations-mascot ${mood}`;
  if (message) elements.assistantLine.textContent = message;
}

function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '!' : 'i'}</span><p>${escapeHtml(message)}</p><button type="button" aria-label="Đóng thông báo">×</button>`;
  toast.querySelector('button').addEventListener('click', () => toast.remove());
  elements.toastRegion.append(toast);
  window.setTimeout(() => toast.remove(), 6000);
}

function setButtonBusy(button, busy, busyLabel) {
  if (!button.dataset.defaultLabel) button.dataset.defaultLabel = button.innerHTML;
  button.disabled = busy;
  button.innerHTML = busy ? `${escapeHtml(busyLabel)} <span class="spinner"></span>` : button.dataset.defaultLabel;
}

function profileRoleLabel() {
  if (isAdmin()) return 'ADMIN · System Administrator';
  if (hasRole(state.profile, 'ROOM_MANAGER')) return 'ROOM_MANAGER · Space Operations';
  return 'STUDENT · Personal Workspace';
}

function initializeProfile() {
  const avatarText = initials(state.profile.displayName);
  $('#sidebar-avatar').textContent = avatarText;
  $('#topbar-avatar').textContent = avatarText;
  $('#sidebar-name').textContent = state.profile.displayName;
  $('#sidebar-role').textContent = profileRoleLabel();
  $('#topbar-name').textContent = state.profile.displayName;

  $$('.student-only').forEach((node) => node.classList.toggle('hidden', !isStudent()));
  $$('.manager-only').forEach((node) => node.classList.toggle('hidden', isStudent()));
  $$('.operator-only').forEach((node) => node.classList.toggle('hidden', !isOperator()));
  const bookingNav = $('[data-view-target="booking"]');
  bookingNav.classList.toggle('hidden', !canCreateReservation());
  $$('[data-open-booking]').forEach((button) => button.classList.toggle('hidden', !canCreateReservation()));

  elements.reservationViewTitle.textContent = isStudent() ? 'Đăng ký của tôi' : 'Điều phối đăng ký';
  elements.reservationViewCopy.textContent = isStudent()
    ? `Identity ${state.profile.requesterId} được áp dụng ở server; không thể đổi qua query của browser.`
    : 'Bạn đang xem read model Reservation theo quyền vai trò hiện tại.';

  elements.reservationRequesterId.value = isStudent() ? state.profile.requesterId : '';
  elements.reservationRequesterName.value = isStudent() ? state.profile.displayName : '';
  elements.reservationRequesterId.readOnly = isStudent();
  elements.reservationRequesterName.readOnly = isStudent();
  if (!isStudent()) {
    elements.reservationRequesterId.placeholder = 'Ví dụ: STUDENT-001';
    elements.reservationRequesterName.placeholder = 'Tên người đăng ký';
  }
}

function updateCountdown() {
  if (!state.profile.expiresAt) {
    elements.countdown.textContent = 'Phiên active';
    return;
  }
  const remaining = state.profile.expiresAt - Date.now();
  if (remaining <= 0) {
    clearSession();
    redirectToLogin();
    return;
  }
  const minutes = Math.floor(remaining / 60000);
  const seconds = Math.floor((remaining % 60000) / 1000);
  elements.countdown.textContent = `JWT còn ${minutes}:${String(seconds).padStart(2, '0')}`;
  elements.countdown.classList.toggle('warning', remaining < 120000);
}

function viewMetadata(view) {
  const metadata = {
    dashboard: ['WORKSPACE', 'Tổng quan'],
    booking: ['RESERVATION SERVICE', 'Tìm & đặt phòng'],
    reservations: ['RESERVATION SERVICE', isStudent() ? 'Đăng ký của tôi' : 'Điều phối đăng ký'],
    rooms: ['ROOM SERVICE', 'Danh mục phòng'],
    'room-operations': ['ROOM SERVICE', 'Vận hành phòng'],
  };
  return metadata[view] ?? ['WORKSPACE', 'Campus Space'];
}

function openView(view) {
  const target = document.getElementById(view);
  if (!target) return;
  state.currentView = view;
  elements.views.forEach((node) => node.classList.toggle('active-view', node.id === view));
  elements.navButtons.forEach((button) => button.classList.toggle('active', button.dataset.viewTarget === view));
  const [kicker, title] = viewMetadata(view);
  elements.viewKicker.textContent = kicker;
  elements.viewTitle.textContent = title;
  const mascotMessages = {
    dashboard: 'Tổng quan đã sẵn sàng. Hôm nay mình hỗ trợ gì đây?',
    booking: 'Tôi đang kết nối Reservation và Room để tìm không gian hợp lý.',
    reservations: 'Mỗi Reservation đều có owner và lifecycle riêng.',
    rooms: 'Room Service đang giữ danh mục không gian cho chúng ta.',
    'room-operations': 'Chế độ vận hành: thay đổi nào cũng đi qua Room Service.',
  };
  setMascotMood(view === 'booking' ? 'thinking' : 'idle', mascotMessages[view]);
  elements.sidebar.classList.remove('mobile-open');
  window.location.hash = view;

  if (view === 'reservations') loadReservations();
  if (view === 'rooms') loadRooms();
  if (view === 'room-operations' && isOperator()) loadOperationRooms();
  if (view === 'dashboard') loadDashboard();
}

async function loadDashboard() {
  elements.dashboardGreeting.textContent = `Chào ${state.profile.displayName.split(' ')[0] ?? state.profile.displayName}`;
  elements.dashboardCopy.textContent = isStudent()
    ? 'Theo dõi nhu cầu không gian và Reservation thuộc về bạn.'
    : 'Theo dõi trạng thái không gian và lịch điều phối từ các capability độc lập.';

  try {
    const [roomPage, reservationPage] = await Promise.all([
      roomApi.list({ page: 0, size: 50, sort: 'code,asc' }, state.accessToken),
      reservationApi.list({ page: 0, size: 5, sort: 'startTime,asc' }, state.accessToken),
    ]);
    renderDashboardMetrics(roomPage.content ?? [], roomPage.totalElements ?? 0, reservationPage);
    renderCompactReservations(reservationPage.content ?? []);
  } catch (error) {
    elements.dashboardMetrics.innerHTML = `<div class="empty-state">Không tải được dashboard: ${escapeHtml(describeApiError(error))}</div>`;
    elements.dashboardReservations.innerHTML = '<div class="empty-state">Dữ liệu Reservation chưa sẵn sàng.</div>';
  }
}

function renderDashboardMetrics(rooms, totalRooms, reservationPage) {
  const available = rooms.filter((room) => room.status === 'AVAILABLE').length;
  const activeReservations = (reservationPage.content ?? []).filter((item) => item.status === 'CONFIRMED').length;
  const firstLabel = isStudent() ? 'Không gian có thể khai thác' : 'Phòng trong danh mục';
  const thirdLabel = isStudent() ? 'Đăng ký hiển thị gần nhất' : 'Đăng ký hiển thị gần nhất';
  elements.dashboardMetrics.innerHTML = `
    <article class="metric-card accent-blue"><span>${firstLabel}</span><strong>${isStudent() ? available : totalRooms}</strong><p>${isStudent() ? `${available} Room đang AVAILABLE trong catalog` : `${available} phòng đang AVAILABLE`}</p></article>
    <article class="metric-card accent-teal"><span>Room Service</span><strong>${available}</strong><p>Provider rooms ở operational state AVAILABLE</p></article>
    <article class="metric-card accent-orange"><span>${thirdLabel}</span><strong>${activeReservations}</strong><p>Reservation Service áp dụng ownership và status</p></article>
  `;
}

function renderCompactReservations(reservations) {
  elements.dashboardReservationsTitle.textContent = isStudent() ? 'Các đăng ký của bạn' : 'Đăng ký cần theo dõi';
  if (!reservations.length) {
    elements.dashboardReservations.innerHTML = '<div class="empty-state">Chưa có Reservation phù hợp để hiển thị.</div>';
    return;
  }
  elements.dashboardReservations.innerHTML = reservations.map((reservation) => `
    <article class="compact-reservation">
      <div class="calendar-mark"><strong>${new Date(reservation.startTime).getDate() || '—'}</strong><span>${new Intl.DateTimeFormat('vi-VN', { month: 'short' }).format(new Date(reservation.startTime))}</span></div>
      <div><strong>${escapeHtml(reservation.roomCode)}</strong><p>${escapeHtml(reservation.purpose)}</p><small>${escapeHtml(formatDateTime(reservation.startTime))} · ${escapeHtml(reservation.attendeeCount)} người</small></div>
      ${statusPill(reservation.status)}
    </article>
  `).join('');
}

async function findAvailability(event) {
  event.preventDefault();
  if (!elements.availabilityForm.reportValidity()) return;
  if (elements.availabilityFrom.value >= elements.availabilityTo.value) {
    showToast('Thời điểm kết thúc phải sau thời điểm bắt đầu.', 'error');
    return;
  }

  const submit = elements.availabilityForm.querySelector('button[type="submit"]');
  setMascotMood('thinking', 'Đang hỏi Room Provider và kiểm lịch Reservation…');
  setButtonBusy(submit, true, 'Đang hỏi Availability API');
  try {
    const response = await reservationApi.availability({
      from: elements.availabilityFrom.value,
      to: elements.availabilityTo.value,
      attendeeCount: elements.availabilityAttendees.value,
      type: elements.availabilityType.value,
    }, state.accessToken);
    state.availability = response;
    state.selectedRoom = null;
    elements.confirmation.classList.add('hidden');
    renderAvailability(response);
    setMascotMood(response.availableCount ? 'happy' : 'alert', response.availableCount
      ? `Tôi tìm thấy ${response.availableCount} không gian có thể xem xét!`
      : 'Chưa có candidate phù hợp. Mình thử đổi thời gian hoặc quy mô nhé.');
  } catch (error) {
    setMascotMood('alert', 'Có một service đang cần được kiểm tra.');
    showToast(describeApiError(error), 'error');
  } finally {
    setButtonBusy(submit, false);
  }
}

function renderAvailability(response) {
  elements.availabilityResults.classList.remove('hidden');
  elements.availabilitySummary.textContent = `${response.availableCount} phòng phù hợp trong khung giờ đã chọn`;
  elements.availabilityDescription.textContent = `${response.candidateCount} candidate được Room Provider trả về; Reservation Service đối chiếu riêng với lịch CONFIRMED mà không công khai booking của người khác.`;

  if (!response.rooms?.length) {
    elements.availableRoomGrid.innerHTML = '<div class="empty-state">Không có Room nào đạt trạng thái AVAILABLE và sức chứa yêu cầu.</div>';
    return;
  }
  elements.availableRoomGrid.innerHTML = response.rooms.map((room) => `
    <article class="room-card ${room.availability === 'AVAILABLE' ? '' : 'unavailable-card'}">
      <div class="room-card-top"><span class="room-type">${escapeHtml(room.roomType)}</span>${statusPill(room.availability)}</div>
      <h3>${escapeHtml(room.roomCode)}</h3><p>${escapeHtml(room.building)} · Tầng ${escapeHtml(room.floor)}</p>
      <div class="room-facts"><span>◉ ${escapeHtml(room.capacity)} chỗ</span>${room.roomType === 'LAB' ? `<span>▦ ${escapeHtml(room.workingComputers)}/${escapeHtml(room.totalComputers)} máy</span>` : '<span>◌ Không gian lý thuyết</span>'}</div>
      <button class="button ${room.availability === 'AVAILABLE' ? 'primary' : 'quiet'} wide choose-available-room" data-room-id="${escapeHtml(room.roomId)}" ${room.availability === 'AVAILABLE' ? '' : 'disabled'} type="button">${room.availability === 'AVAILABLE' ? 'Chọn không gian này' : 'Đã có lịch trùng'}</button>
    </article>
  `).join('');
  $$('.choose-available-room').forEach((button) => button.addEventListener('click', () => {
    const room = response.rooms.find((item) => String(item.roomId) === button.dataset.roomId);
    selectAvailableRoom(room);
  }));
}

function selectAvailableRoom(room) {
  state.selectedRoom = room;
  elements.selectedRoomId.value = room.roomId;
  elements.reservationAttendees.value = elements.availabilityAttendees.value;
  elements.selectedRoomSummary.textContent = `${room.roomCode} · ${room.building}, tầng ${room.floor} · ${room.capacity} chỗ · ${formatDateTime(elements.availabilityFrom.value)} — ${formatDateTime(elements.availabilityTo.value)}`;
  elements.confirmation.classList.remove('hidden');
  elements.confirmation.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function createReservation(event) {
  event.preventDefault();
  if (!state.selectedRoom) {
    showToast('Hãy chọn một phòng từ Availability API trước.', 'error');
    return;
  }
  if (!elements.reservationForm.reportValidity()) return;

  const submit = elements.reservationSubmit;
  setButtonBusy(submit, true, 'Đang xác nhận');
  try {
    const reservation = await reservationApi.create({
      roomId: Number(state.selectedRoom.roomId),
      requesterId: elements.reservationRequesterId.value.trim(),
      requesterName: elements.reservationRequesterName.value.trim(),
      purpose: elements.reservationPurpose.value.trim(),
      startTime: elements.availabilityFrom.value,
      endTime: elements.availabilityTo.value,
      attendeeCount: Number(elements.reservationAttendees.value),
    }, state.accessToken);
    setMascotMood('happy', `Tuyệt! Reservation #${reservation.id} đã được xác nhận.`);
    showToast(`Reservation #${reservation.id} đã CONFIRMED. Audit event được publish sau local commit.`, 'success');
    elements.reservationForm.reset();
    state.selectedRoom = null;
    elements.confirmation.classList.add('hidden');
    elements.availabilityResults.classList.add('hidden');
    if (isStudent()) {
      elements.reservationRequesterId.value = state.profile.requesterId;
      elements.reservationRequesterName.value = state.profile.displayName;
    }
    loadDashboard();
  } catch (error) {
    setMascotMood('alert', 'Chưa thể xác nhận. Mình kiểm tra lại điều kiện nhé.');
    showToast(describeApiError(error), 'error');
    if (error?.code === 'ROOM_SCHEDULE_CONFLICT') elements.confirmation.classList.add('hidden');
  } finally {
    setButtonBusy(submit, false);
  }
}

function reservationFilters() {
  return {
    roomId: elements.reservationFilterRoomId.value,
    status: elements.reservationFilterStatus.value,
    from: elements.reservationFilterFrom.value,
    to: elements.reservationFilterTo.value,
    page: state.reservations.page,
    size: state.reservations.size,
    sort: 'startTime,desc',
  };
}

async function loadReservations() {
  elements.reservationTable.innerHTML = '<div class="loading-block">Đang tải Reservation Service…</div>';
  try {
    const page = await reservationApi.list(reservationFilters(), state.accessToken);
    state.reservations = { ...state.reservations, page: page.page, totalPages: page.totalPages, content: page.content ?? [] };
    renderReservations(page);
  } catch (error) {
    elements.reservationTable.innerHTML = `<div class="empty-state">${escapeHtml(describeApiError(error))}</div>`;
  }
}

function canCancel(reservation) {
  return reservation.status === 'CONFIRMED' && (isStudent() || isAdmin());
}

function renderReservations(page) {
  const reservations = page.content ?? [];
  if (!reservations.length) {
    elements.reservationTable.innerHTML = '<div class="empty-state">Không có Reservation phù hợp với bộ lọc hiện tại.</div>';
  } else {
    elements.reservationTable.innerHTML = `<table><thead><tr><th>Reservation</th><th>Thời gian</th><th>Người dùng</th><th>Quy mô</th><th>Trạng thái</th><th></th></tr></thead><tbody>${reservations.map((reservation) => `
      <tr>
        <td><strong>${escapeHtml(reservation.roomCode)}</strong><small>#${escapeHtml(reservation.id)} · ${escapeHtml(reservation.purpose)}</small></td>
        <td>${escapeHtml(formatDateTime(reservation.startTime))}<small>đến ${escapeHtml(formatDateTime(reservation.endTime))}</small></td>
        <td>${escapeHtml(reservation.requesterName)}<small>${escapeHtml(reservation.requesterId)}</small></td>
        <td>${escapeHtml(reservation.attendeeCount)} người</td>
        <td>${statusPill(reservation.status)}</td>
        <td>${canCancel(reservation) ? `<button class="text-button cancel-reservation" data-reservation-id="${escapeHtml(reservation.id)}" type="button">Hủy</button>` : '—'}</td>
      </tr>`).join('')}</tbody></table>`;
    $$('.cancel-reservation').forEach((button) => button.addEventListener('click', () => cancelReservation(button.dataset.reservationId)));
  }
  elements.reservationPage.textContent = page.totalPages ? `Trang ${page.page + 1} / ${page.totalPages}` : 'Không có kết quả';
  elements.reservationPrevious.disabled = (page.page ?? 0) <= 0;
  elements.reservationNext.disabled = !page.totalPages || page.page + 1 >= page.totalPages;
}

async function cancelReservation(reservationId) {
  if (!window.confirm(`Hủy Reservation #${reservationId}? Thao tác này giữ lại lịch sử và phát event reservation.cancelled.`)) return;
  try {
    const reservation = await reservationApi.cancel(reservationId, state.accessToken);
    showToast(`Reservation #${reservation.id} đã chuyển sang CANCELLED.`, 'success');
    loadReservations();
    loadDashboard();
  } catch (error) {
    showToast(describeApiError(error), 'error');
  }
}

function roomFilters() {
  return {
    keyword: elements.roomKeyword.value,
    status: elements.roomStatus.value,
    type: elements.roomType.value,
    minCapacity: elements.roomMinCapacity.value,
    page: state.rooms.page,
    size: state.rooms.size,
    sort: 'code,asc',
  };
}

async function loadRooms() {
  elements.roomGrid.innerHTML = '<div class="loading-block">Đang tải Room Service…</div>';
  try {
    const page = await roomApi.list(roomFilters(), state.accessToken);
    state.rooms = { ...state.rooms, page: page.page, totalPages: page.totalPages, content: page.content ?? [] };
    renderRoomDirectory(page);
  } catch (error) {
    elements.roomGrid.innerHTML = `<div class="empty-state">${escapeHtml(describeApiError(error))}</div>`;
  }
}

function renderRoomDirectory(page) {
  const rooms = page.content ?? [];
  if (!rooms.length) {
    elements.roomGrid.innerHTML = '<div class="empty-state">Không có phòng phù hợp trong Room Service.</div>';
  } else {
    elements.roomGrid.innerHTML = rooms.map((room) => `
      <article class="room-card">
        <div class="room-card-top"><span class="room-type">${escapeHtml(room.type)}</span>${statusPill(room.status)}</div>
        <h3>${escapeHtml(room.code)}</h3><p>${escapeHtml(room.building)} · Tầng ${escapeHtml(room.floor)}</p>
        <div class="room-facts"><span>◉ ${escapeHtml(room.capacity)} chỗ</span>${room.type === 'LAB' ? `<span>▦ ${escapeHtml(room.workingComputers)}/${escapeHtml(room.totalComputers)} máy tốt</span>` : '<span>◌ Không gian lý thuyết</span>'}</div>
        ${canCreateReservation() && room.status === 'AVAILABLE' ? `<button class="button secondary wide book-from-directory" data-room-id="${escapeHtml(room.id)}" type="button">Đặt phòng này</button>` : ''}
      </article>`).join('');
    $$('.book-from-directory').forEach((button) => button.addEventListener('click', () => {
      openView('booking');
      showToast('Hãy chọn thời gian và số người; hệ thống sẽ xác nhận availability trước khi đặt.', 'info');
    }));
  }
  elements.roomPage.textContent = page.totalPages ? `Trang ${page.page + 1} / ${page.totalPages}` : 'Không có kết quả';
  elements.roomPrevious.disabled = (page.page ?? 0) <= 0;
  elements.roomNext.disabled = !page.totalPages || page.page + 1 >= page.totalPages;
}

async function loadOperationRooms() {
  elements.operationRoomList.innerHTML = '<div class="loading-block">Đang nạp inventory…</div>';
  try {
    const page = await roomApi.list({ page: 0, size: 50, sort: 'code,asc' }, state.accessToken);
    state.operationRooms = page.content ?? [];
    elements.operationRoomCount.textContent = `${page.totalElements} phòng`;
    elements.operationRoomList.innerHTML = state.operationRooms.map((room) => `
      <article class="operation-room-item">
        <div><strong>${escapeHtml(room.code)}</strong><p>${escapeHtml(room.building)} · tầng ${escapeHtml(room.floor)} · ${escapeHtml(room.capacity)} chỗ</p></div>
        <div class="operation-actions">${statusPill(room.status)}<button class="text-button edit-room" data-room-id="${escapeHtml(room.id)}" type="button">Sửa</button><button class="text-button danger delete-room" data-room-id="${escapeHtml(room.id)}" type="button">Xóa</button></div>
      </article>`).join('');
    $$('.edit-room').forEach((button) => button.addEventListener('click', () => editRoom(button.dataset.roomId)));
    $$('.delete-room').forEach((button) => button.addEventListener('click', () => deleteRoom(button.dataset.roomId)));
  } catch (error) {
    elements.operationRoomList.innerHTML = `<div class="empty-state">${escapeHtml(describeApiError(error))}</div>`;
  }
}

function clearRoomForm() {
  elements.operationForm.reset();
  elements.operationRoomId.value = '';
  elements.operationTotalComputers.value = '0';
  elements.operationWorkingComputers.value = '0';
  elements.operationBrokenComputers.value = '0';
  elements.operationStatus.value = 'AVAILABLE';
  elements.operationFormTitle.textContent = 'Tạo phòng mới';
  elements.operationSubmit.dataset.defaultLabel = 'Lưu thay đổi <span>→</span>';
  elements.operationSubmit.innerHTML = elements.operationSubmit.dataset.defaultLabel;
}

function editRoom(roomId) {
  const room = state.operationRooms.find((item) => String(item.id) === String(roomId));
  if (!room) return;
  elements.operationRoomId.value = room.id;
  elements.operationCode.value = room.code;
  elements.operationBuilding.value = room.building;
  elements.operationFloor.value = room.floor;
  elements.operationType.value = room.type;
  elements.operationCapacity.value = room.capacity;
  elements.operationStatus.value = room.status;
  elements.operationTotalComputers.value = room.totalComputers;
  elements.operationWorkingComputers.value = room.workingComputers;
  elements.operationBrokenComputers.value = room.brokenComputers;
  elements.operationFormTitle.textContent = `Chỉnh sửa ${room.code}`;
  elements.operationSubmit.dataset.defaultLabel = 'Cập nhật phòng <span>→</span>';
  elements.operationSubmit.innerHTML = elements.operationSubmit.dataset.defaultLabel;
  elements.operationForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function roomPayloadFromForm() {
  return {
    code: elements.operationCode.value.trim(),
    building: elements.operationBuilding.value.trim(),
    floor: Number(elements.operationFloor.value),
    type: elements.operationType.value,
    capacity: Number(elements.operationCapacity.value),
    totalComputers: Number(elements.operationTotalComputers.value),
    workingComputers: Number(elements.operationWorkingComputers.value),
    brokenComputers: Number(elements.operationBrokenComputers.value),
    status: elements.operationStatus.value,
  };
}

async function saveRoom(event) {
  event.preventDefault();
  if (!elements.operationForm.reportValidity()) return;
  const roomId = elements.operationRoomId.value;
  setButtonBusy(elements.operationSubmit, true, roomId ? 'Đang cập nhật' : 'Đang tạo');
  try {
    const payload = roomPayloadFromForm();
    const room = roomId
      ? await roomApi.update(roomId, payload, state.accessToken)
      : await roomApi.create(payload, state.accessToken);
    showToast(`Room ${room.code} đã được ${roomId ? 'cập nhật' : 'tạo'} bởi Room Service.`, 'success');
    clearRoomForm();
    loadOperationRooms();
    loadDashboard();
  } catch (error) {
    showToast(describeApiError(error), 'error');
  } finally {
    setButtonBusy(elements.operationSubmit, false);
  }
}

async function deleteRoom(roomId) {
  const room = state.operationRooms.find((item) => String(item.id) === String(roomId));
  if (!window.confirm(`Xóa Room ${room?.code ?? `#${roomId}`} khỏi Room Service?`)) return;
  try {
    await roomApi.remove(roomId, state.accessToken);
    showToast(`Room ${room?.code ?? `#${roomId}`} đã bị xóa.`, 'success');
    loadOperationRooms();
    loadDashboard();
  } catch (error) {
    showToast(describeApiError(error), 'error');
  }
}

function attachHandlers() {
  elements.navButtons.forEach((button) => button.addEventListener('click', () => openView(button.dataset.viewTarget)));
  $$('[data-open-booking]').forEach((button) => button.addEventListener('click', () => openView('booking')));
  elements.mobileMenu.addEventListener('click', () => elements.sidebar.classList.toggle('mobile-open'));
  elements.logout.addEventListener('click', () => { clearSession(); redirectToLogin(); });

  elements.availabilityForm.addEventListener('submit', findAvailability);
  elements.changeRoom.addEventListener('click', () => {
    elements.confirmation.classList.add('hidden');
    elements.availabilityResults.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
  elements.reservationForm.addEventListener('submit', createReservation);

  elements.reservationFilterForm.addEventListener('submit', (event) => { event.preventDefault(); state.reservations.page = 0; loadReservations(); });
  elements.resetReservationFilter.addEventListener('click', () => { elements.reservationFilterForm.reset(); state.reservations.page = 0; loadReservations(); });
  elements.reservationPrevious.addEventListener('click', () => { if (state.reservations.page > 0) { state.reservations.page -= 1; loadReservations(); } });
  elements.reservationNext.addEventListener('click', () => { if (state.reservations.page + 1 < state.reservations.totalPages) { state.reservations.page += 1; loadReservations(); } });

  elements.roomFilterForm.addEventListener('submit', (event) => { event.preventDefault(); state.rooms.page = 0; loadRooms(); });
  elements.resetRoomFilter.addEventListener('click', () => { elements.roomFilterForm.reset(); state.rooms.page = 0; loadRooms(); });
  elements.roomPrevious.addEventListener('click', () => { if (state.rooms.page > 0) { state.rooms.page -= 1; loadRooms(); } });
  elements.roomNext.addEventListener('click', () => { if (state.rooms.page + 1 < state.rooms.totalPages) { state.rooms.page += 1; loadRooms(); } });

  elements.operationForm.addEventListener('submit', saveRoom);
  elements.clearOperation.addEventListener('click', clearRoomForm);
}

function setBookingDefaults() {
  const start = new Date();
  start.setMinutes(0, 0, 0);
  start.setHours(start.getHours() + 1);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  elements.availabilityFrom.value = toLocalInputValue(start);
  elements.availabilityTo.value = toLocalInputValue(end);
}

function setupDocsLinks() {
  // Keep service documentation available from the browser console without making it part of the business UI.
  window.__campusSpaceServiceDocs = serviceUrls;
}

initializeProfile();
setBookingDefaults();
attachHandlers();
setupDocsLinks();
updateCountdown();
window.setInterval(updateCountdown, 1000);
const initialView = window.location.hash.slice(1);
openView(document.getElementById(initialView) ? initialView : 'dashboard');
