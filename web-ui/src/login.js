import './style.css';
import { authApi, describeApiError } from './core/api.js';
import { currentSession, redirectToApp, saveSession } from './core/session.js';

const form = document.querySelector('#login-form');
const username = document.querySelector('#login-username');
const password = document.querySelector('#login-password');
const submit = document.querySelector('#login-submit');
const result = document.querySelector('#login-result');
const togglePassword = document.querySelector('#toggle-password');

if (currentSession()) {
  redirectToApp();
}

togglePassword.addEventListener('click', () => {
  const isPassword = password.type === 'password';
  password.type = isPassword ? 'text' : 'password';
  togglePassword.textContent = isPassword ? 'Ẩn' : 'Hiện';
  togglePassword.setAttribute('aria-label', isPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  result.className = 'inline-alert hidden';

  if (!form.reportValidity()) {
    return;
  }

  submit.disabled = true;
  submit.innerHTML = 'Đang xác thực <span class="spinner"></span>';

  try {
    const response = await authApi.login(username.value.trim(), password.value);
    saveSession(response.accessToken);
    redirectToApp();
  } catch (error) {
    result.textContent = describeApiError(error);
    result.className = 'inline-alert error';
  } finally {
    submit.disabled = false;
    submit.innerHTML = 'Vào Campus Space <span>→</span>';
  }
});
