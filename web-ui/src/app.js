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
  return new Intl.DateTimeFormat('en-GB', {
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
  toast.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '!' : 'i'}</span><p>${escapeHtml(message)}</p><button type="button" aria-label="Dismiss notification">×</button>`;
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

  elements.reservationViewTitle.textContent = isStudent() ? 'My reservations' : 'Reservation desk';
  elements.reservationViewCopy.textContent = isStudent()
    ? `Identity ${state.profile.requesterId} is enforced server-side and cannot be changed through a browser query.`
    : 'You are viewing the Reservation read model according to your current role.';

  elements.reservationRequesterId.value = isStudent() ? state.profile.requesterId : '';
  elements.reservationRequesterName.value = isStudent() ? state.profile.displayName : '';
  elements.reservationRequesterId.readOnly = isStudent();
  elements.reservationRequesterName.readOnly = isStudent();
  if (!isStudent()) {
    elements.reservationRequesterId.placeholder = 'e.g. STUDENT-001';
    elements.reservationRequesterName.placeholder = 'Requester name';
  }
}

function updateCountdown() {
  if (!state.profile.expiresAt) {
    elements.countdown.textContent = 'Session active';
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
  elements.countdown.textContent = `JWT ${minutes}:${String(seconds).padStart(2, '0')} left`;
  elements.countdown.classList.toggle('warning', remaining < 120000);
}

function viewMetadata(view) {
  const metadata = {
    dashboard: ['WORKSPACE', 'Overview'],
    booking: ['RESERVATION SERVICE', 'Find & book a room'],
    reservations: ['RESERVATION SERVICE', isStudent() ? 'My reservations' : 'Reservation desk'],
    rooms: ['ROOM SERVICE', 'Room directory'],
    'room-operations': ['ROOM SERVICE', 'Room operations'],
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
    dashboard: 'Overview is ready. What can I help coordinate today?',
    booking: 'I am connecting Reservation and Room to find the right space.',
    reservations: 'Every Reservation has its own owner and lifecycle.',
    rooms: 'Room Service is holding the space directory for us.',
    'room-operations': 'Operations mode: every change goes through Room Service.',
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
  elements.dashboardGreeting.textContent = `Hello, ${state.profile.displayName.split(' ')[0] ?? state.profile.displayName}`;
  elements.dashboardCopy.textContent = isStudent()
    ? 'Track space needs and the Reservations that belong to you.'
    : 'Track space state and booking activity from independent capabilities.';

  try {
    const [roomPage, reservationPage] = await Promise.all([
      roomApi.list({ page: 0, size: 50, sort: 'code,asc' }, state.accessToken),
      reservationApi.list({ page: 0, size: 5, sort: 'startTime,asc' }, state.accessToken),
    ]);
    renderDashboardMetrics(roomPage.content ?? [], roomPage.totalElements ?? 0, reservationPage);
    renderCompactReservations(reservationPage.content ?? []);
  } catch (error) {
    elements.dashboardMetrics.innerHTML = `<div class="empty-state">Could not load the dashboard: ${escapeHtml(describeApiError(error))}</div>`;
    elements.dashboardReservations.innerHTML = '<div class="empty-state">Reservation data is not ready.</div>';
  }
}

function renderDashboardMetrics(rooms, totalRooms, reservationPage) {
  const available = rooms.filter((room) => room.status === 'AVAILABLE').length;
  const activeReservations = (reservationPage.content ?? []).filter((item) => item.status === 'CONFIRMED').length;
  const firstLabel = isStudent() ? 'Usable spaces' : 'Rooms in the directory';
  const thirdLabel = isStudent() ? 'Recent reservations shown' : 'Recent reservations shown';
  elements.dashboardMetrics.innerHTML = `
    <article class="metric-card accent-blue"><span>${firstLabel}</span><strong>${isStudent() ? available : totalRooms}</strong><p>${isStudent() ? `${available} rooms are AVAILABLE in the catalog` : `${available} rooms are AVAILABLE`}</p></article>
    <article class="metric-card accent-teal"><span>Room Service</span><strong>${available}</strong><p>Provider rooms in AVAILABLE operational state</p></article>
    <article class="metric-card accent-orange"><span>${thirdLabel}</span><strong>${activeReservations}</strong><p>Reservation Service applies ownership and status</p></article>
  `;
}

function renderCompactReservations(reservations) {
  elements.dashboardReservationsTitle.textContent = isStudent() ? 'Your reservations' : 'Reservations to watch';
  if (!reservations.length) {
    elements.dashboardReservations.innerHTML = '<div class="empty-state">Chưa có Reservation phù hợp để hiển thị.</div>';
    return;
  }
  elements.dashboardReservations.innerHTML = reservations.map((reservation) => `
    <article class="compact-reservation">
      <div class="calendar-mark"><strong>${new Date(reservation.startTime).getDate() || '—'}</strong><span>${new Intl.DateTimeFormat('en-GB', { month: 'short' }).format(new Date(reservation.startTime))}</span></div>
      <div><strong>${escapeHtml(reservation.roomCode)}</strong><p>${escapeHtml(reservation.purpose)}</p><small>${escapeHtml(formatDateTime(reservation.startTime))} · ${escapeHtml(reservation.attendeeCount)} people</small></div>
      ${statusPill(reservation.status)}
    </article>
  `).join('');
}

async function findAvailability(event) {
  event.preventDefault();
  if (!elements.availabilityForm.reportValidity()) return;
  if (elements.availabilityFrom.value >= elements.availabilityTo.value) {
    showToast('End time must be after start time.', 'error');
    return;
  }

  const submit = elements.availabilityForm.querySelector('button[type="submit"]');
  setMascotMood('thinking', 'Checking Room Provider and Reservation schedule…');
  setButtonBusy(submit, true, 'Checking Availability API');
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
      ? `I found ${response.availableCount} spaces to consider!`
      : 'No candidate fits yet. Let’s try another time or group size.');
  } catch (error) {
    setMascotMood('alert', 'One service needs a quick check.');
    showToast(describeApiError(error), 'error');
  } finally {
    setButtonBusy(submit, false);
  }
}

function renderAvailability(response) {
  elements.availabilityResults.classList.remove('hidden');
  elements.availabilitySummary.textContent = `${response.availableCount} rooms fit the selected time window`;
  elements.availabilityDescription.textContent = `${response.candidateCount} candidates came from Room Provider; Reservation Service checks CONFIRMED schedule privately without exposing another person’s booking.`;

  if (!response.rooms?.length) {
    elements.availableRoomGrid.innerHTML = '<div class="empty-state">No Room meets both AVAILABLE state and the required capacity.</div>';
    return;
  }
  elements.availableRoomGrid.innerHTML = response.rooms.map((room) => `
    <article class="room-card ${room.availability === 'AVAILABLE' ? '' : 'unavailable-card'}">
      <div class="room-card-top"><span class="room-type">${escapeHtml(room.roomType)}</span>${statusPill(room.availability)}</div>
      <h3>${escapeHtml(room.roomCode)}</h3><p>${escapeHtml(room.building)} · Floor ${escapeHtml(room.floor)}</p>
      <div class="room-facts"><span>◉ ${escapeHtml(room.capacity)} seats</span>${room.roomType === 'LAB' ? `<span>▦ ${escapeHtml(room.workingComputers)}/${escapeHtml(room.totalComputers)} computers</span>` : '<span>◌ Teaching space</span>'}</div>
      <button class="button ${room.availability === 'AVAILABLE' ? 'primary' : 'quiet'} wide choose-available-room" data-room-id="${escapeHtml(room.roomId)}" ${room.availability === 'AVAILABLE' ? '' : 'disabled'} type="button">${room.availability === 'AVAILABLE' ? 'Choose this space' : 'Schedule conflict'}</button>
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
  elements.selectedRoomSummary.textContent = `${room.roomCode} · ${room.building}, Floor ${room.floor} · ${room.capacity} seats · ${formatDateTime(elements.availabilityFrom.value)} — ${formatDateTime(elements.availabilityTo.value)}`;
  elements.confirmation.classList.remove('hidden');
  elements.confirmation.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function createReservation(event) {
  event.preventDefault();
  if (!state.selectedRoom) {
    showToast('Choose a room from the Availability API first.', 'error');
    return;
  }
  if (!elements.reservationForm.reportValidity()) return;

  const submit = elements.reservationSubmit;
  setButtonBusy(submit, true, 'Confirming');
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
    setMascotMood('happy', `Great! Reservation #${reservation.id} is confirmed.`);
    showToast(`Reservation #${reservation.id} is CONFIRMED. An Audit event is published after local commit.`, 'success');
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
    setMascotMood('alert', 'Could not confirm yet. Let’s check the conditions.');
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
  elements.reservationTable.innerHTML = '<div class="loading-block">Loading Reservation Service…</div>';
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
    elements.reservationTable.innerHTML = '<div class="empty-state">No Reservations match the current filter.</div>';
  } else {
    elements.reservationTable.innerHTML = `<table><thead><tr><th>Reservation</th><th>Time</th><th>Requester</th><th>Size</th><th>Status</th><th></th></tr></thead><tbody>${reservations.map((reservation) => `
      <tr>
        <td><strong>${escapeHtml(reservation.roomCode)}</strong><small>#${escapeHtml(reservation.id)} · ${escapeHtml(reservation.purpose)}</small></td>
        <td>${escapeHtml(formatDateTime(reservation.startTime))}<small>to ${escapeHtml(formatDateTime(reservation.endTime))}</small></td>
        <td>${escapeHtml(reservation.requesterName)}<small>${escapeHtml(reservation.requesterId)}</small></td>
        <td>${escapeHtml(reservation.attendeeCount)} people</td>
        <td>${statusPill(reservation.status)}</td>
        <td>${canCancel(reservation) ? `<button class="text-button cancel-reservation" data-reservation-id="${escapeHtml(reservation.id)}" type="button">Cancel</button>` : '—'}</td>
      </tr>`).join('')}</tbody></table>`;
    $$('.cancel-reservation').forEach((button) => button.addEventListener('click', () => cancelReservation(button.dataset.reservationId)));
  }
  elements.reservationPage.textContent = page.totalPages ? `Page ${page.page + 1} / ${page.totalPages}` : 'No results';
  elements.reservationPrevious.disabled = (page.page ?? 0) <= 0;
  elements.reservationNext.disabled = !page.totalPages || page.page + 1 >= page.totalPages;
}

async function cancelReservation(reservationId) {
  if (!window.confirm(`Cancel Reservation #${reservationId}? This keeps the history and publishes a reservation.cancelled event.`)) return;
  try {
    const reservation = await reservationApi.cancel(reservationId, state.accessToken);
    showToast(`Reservation #${reservation.id} is now CANCELLED.`, 'success');
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
  elements.roomGrid.innerHTML = '<div class="loading-block">Loading Room Service…</div>';
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
    elements.roomGrid.innerHTML = '<div class="empty-state">No matching rooms exist in Room Service.</div>';
  } else {
    elements.roomGrid.innerHTML = rooms.map((room) => `
      <article class="room-card">
        <div class="room-card-top"><span class="room-type">${escapeHtml(room.type)}</span>${statusPill(room.status)}</div>
        <h3>${escapeHtml(room.code)}</h3><p>${escapeHtml(room.building)} · Floor ${escapeHtml(room.floor)}</p>
        <div class="room-facts"><span>◉ ${escapeHtml(room.capacity)} seats</span>${room.type === 'LAB' ? `<span>▦ ${escapeHtml(room.workingComputers)}/${escapeHtml(room.totalComputers)} working computers</span>` : '<span>◌ Teaching space</span>'}</div>
        ${canCreateReservation() && room.status === 'AVAILABLE' ? `<button class="button secondary wide book-from-directory" data-room-id="${escapeHtml(room.id)}" type="button">Book this room</button>` : ''}
      </article>`).join('');
    $$('.book-from-directory').forEach((button) => button.addEventListener('click', () => {
      openView('booking');
      showToast('Choose time and group size; the system confirms availability before booking.', 'info');
    }));
  }
  elements.roomPage.textContent = page.totalPages ? `Page ${page.page + 1} / ${page.totalPages}` : 'No results';
  elements.roomPrevious.disabled = (page.page ?? 0) <= 0;
  elements.roomNext.disabled = !page.totalPages || page.page + 1 >= page.totalPages;
}

async function loadOperationRooms() {
  elements.operationRoomList.innerHTML = '<div class="loading-block">Loading inventory…</div>';
  try {
    const page = await roomApi.list({ page: 0, size: 50, sort: 'code,asc' }, state.accessToken);
    state.operationRooms = page.content ?? [];
    elements.operationRoomCount.textContent = `${page.totalElements} rooms`;
    elements.operationRoomList.innerHTML = state.operationRooms.map((room) => `
      <article class="operation-room-item">
        <div><strong>${escapeHtml(room.code)}</strong><p>${escapeHtml(room.building)} · Floor ${escapeHtml(room.floor)} · ${escapeHtml(room.capacity)} seats</p></div>
        <div class="operation-actions">${statusPill(room.status)}<button class="text-button edit-room" data-room-id="${escapeHtml(room.id)}" type="button">Edit</button><button class="text-button danger delete-room" data-room-id="${escapeHtml(room.id)}" type="button">Delete</button></div>
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
  elements.operationFormTitle.textContent = 'Create a room';
  elements.operationSubmit.dataset.defaultLabel = 'Save changes <span>→</span>';
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
  elements.operationFormTitle.textContent = `Edit ${room.code}`;
  elements.operationSubmit.dataset.defaultLabel = 'Update room <span>→</span>';
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
  setButtonBusy(elements.operationSubmit, true, roomId ? 'Updating' : 'Creating');
  try {
    const payload = roomPayloadFromForm();
    const room = roomId
      ? await roomApi.update(roomId, payload, state.accessToken)
      : await roomApi.create(payload, state.accessToken);
    showToast(`Room ${room.code} was ${roomId ? 'updated' : 'created'} by Room Service.`, 'success');
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
  if (!window.confirm(`Delete Room ${room?.code ?? `#${roomId}`} from Room Service?`)) return;
  try {
    await roomApi.remove(roomId, state.accessToken);
    showToast(`Room ${room?.code ?? `#${roomId}`} was deleted.`, 'success');
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
