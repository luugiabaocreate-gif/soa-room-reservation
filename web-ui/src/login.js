import './robot-auth.css';
import { authApi, describeApiError } from './core/api.js';
import { currentSession, redirectToApp, saveSession } from './core/session.js';

/**
 * Campus Space Robot Login Controller
 * Based on the user-designed expressive robot interaction system. Every
 * success/error state is now driven by the real Auth Service, never demo data.
 */
const cardAssembly = document.querySelector('#cardAssembly');
const robotHead = document.querySelector('#robotHead');
const eyeL = document.querySelector('#eyeL');
const eyeR = document.querySelector('#eyeR');
const eyesNormal = document.querySelector('#eyesNormal');
const eyesHappy = document.querySelector('#eyesHappy');
const eyesError = document.querySelector('#eyesError');
const eyesPeek = document.querySelector('#eyesPeek');
const scanline = document.querySelector('#scanline');
const sweatDrop = document.querySelector('#sweatDrop');
const bubbleText = document.querySelector('#bubbleText');
const mouthDot = document.querySelector('#mouthDot');
const mouthW = document.querySelector('#mouthW');
const mouthO = document.querySelector('#mouthO');
const mouthHappy = document.querySelector('#mouthHappy');
const mouthFrown = document.querySelector('#mouthFrown');
const mouthSmirk = document.querySelector('#mouthSmirk');
const form = document.querySelector('#formLogin');
const username = document.querySelector('#loginName');
const password = document.querySelector('#loginPass');
const loginButton = document.querySelector('#loginBtn');
const loginResult = document.querySelector('#loginResult');
const soundToggle = document.querySelector('#soundToggleBtn');
const soundOnIcon = document.querySelector('#soundIconOn');
const soundOffIcon = document.querySelector('#soundIconOff');
const canvas = document.querySelector('#confettiCanvas');
const context = canvas.getContext('2d');

if (currentSession()) {
  redirectToApp();
}

let soundEnabled = true;
let audioContext;
let blinkTimer;

function getAudioContext() {
  if (!audioContext) {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (AudioContextClass) audioContext = new AudioContextClass();
  }
  if (audioContext?.state === 'suspended') audioContext.resume();
  return audioContext;
}

function playSound(type) {
  if (!soundEnabled) return;
  try {
    const ctx = getAudioContext();
    if (!ctx) return;
    const now = ctx.currentTime;
    const note = (frequency, start, duration, gainValue = 0.04, wave = 'sine') => {
      const oscillator = ctx.createOscillator();
      const gain = ctx.createGain();
      oscillator.type = wave;
      oscillator.frequency.setValueAtTime(frequency, start);
      gain.gain.setValueAtTime(gainValue, start);
      gain.gain.exponentialRampToValueAtTime(0.001, start + duration);
      oscillator.connect(gain);
      gain.connect(ctx.destination);
      oscillator.start(start);
      oscillator.stop(start + duration + 0.01);
    };

    if (type === 'tap') note(590 + Math.random() * 120, now, 0.035, 0.02);
    if (type === 'cover') {
      note(240, now, 0.2, 0.04, 'triangle');
    }
    if (type === 'peek') note(620, now, 0.14, 0.04);
    if (type === 'error') {
      const oscillator = ctx.createOscillator();
      const gain = ctx.createGain();
      oscillator.type = 'triangle';
      oscillator.frequency.setValueAtTime(280, now);
      oscillator.frequency.linearRampToValueAtTime(140, now + 0.25);
      gain.gain.setValueAtTime(0.055, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
      oscillator.connect(gain);
      gain.connect(ctx.destination);
      oscillator.start(now);
      oscillator.stop(now + 0.26);
    }
    if (type === 'success') [523.25, 659.25, 783.99, 1046.5]
      .forEach((frequency, index) => note(frequency, now + index * 0.07, 0.3, 0.05));
  } catch {
    // Audio is a progressive enhancement: login never depends on it.
  }
}

function setMouth(shape) {
  [mouthDot, mouthW, mouthO, mouthHappy, mouthFrown, mouthSmirk]
    .forEach((mouth) => mouth.classList.add('hidden'));
  ({ w: mouthW, o: mouthO, happy: mouthHappy, frown: mouthFrown, smirk: mouthSmirk }[shape] ?? mouthDot)
    .classList.remove('hidden');
}

function setEyes(mode) {
  [eyesNormal, eyesHappy, eyesError, eyesPeek].forEach((eyes) => eyes.classList.add('hidden'));
  ({ happy: eyesHappy, error: eyesError, peek: eyesPeek }[mode] ?? eyesNormal)
    .classList.remove('hidden');
}

function setRobotMessage(message) {
  bubbleText.textContent = message;
}

function resetFace() {
  setEyes('normal');
  setMouth('dot');
  robotHead.classList.remove('has-blush');
  sweatDrop.classList.add('hidden');
  scanline.classList.add('hidden');
}

function showResult(message, type = 'error') {
  loginResult.textContent = message;
  loginResult.className = `login-result ${type}`;
}

function hideResult() {
  loginResult.textContent = '';
  loginResult.className = 'login-result hidden';
}

function startBlinking() {
  window.clearInterval(blinkTimer);
  blinkTimer = window.setInterval(() => {
    if (cardAssembly.classList.contains('covering-eyes') || cardAssembly.classList.contains('celebrating') || cardAssembly.classList.contains('peeking')) return;
    robotHead.classList.add('blinking');
    window.setTimeout(() => robotHead.classList.remove('blinking'), 140);
  }, 3700 + Math.random() * 1800);
}

let mouseX = 0;
let mouseY = 0;
let parallaxFrame = false;

function resetLook() {
  eyeL.style.transform = 'translate3d(0, 0, 0)';
  eyeR.style.transform = 'translate3d(0, 0, 0)';
  robotHead.style.transform = 'rotateY(0deg) rotateX(0deg)';
}

function renderParallax() {
  parallaxFrame = false;
  if (cardAssembly.classList.contains('covering-eyes') || cardAssembly.classList.contains('celebrating')) return;
  const rect = robotHead.getBoundingClientRect();
  const deltaX = (mouseX - (rect.left + rect.width / 2)) / (window.innerWidth / 2);
  const deltaY = (mouseY - (rect.top + rect.height / 2)) / (window.innerHeight / 2);
  const eyeX = Math.min(Math.max(deltaX * 5.5, -5.5), 5.5);
  const eyeY = Math.min(Math.max(deltaY * 4, -4), 4);
  eyeL.style.transform = `translate3d(${eyeX}px, ${eyeY}px, 0)`;
  eyeR.style.transform = `translate3d(${eyeX}px, ${eyeY}px, 0)`;
  robotHead.style.transform = `rotateY(${deltaX * 5}deg) rotateX(${-deltaY * 3.5}deg)`;
}

window.addEventListener('mousemove', (event) => {
  if (cardAssembly.classList.contains('covering-eyes') || cardAssembly.classList.contains('celebrating')) return;
  mouseX = event.clientX;
  mouseY = event.clientY;
  if (!parallaxFrame) {
    parallaxFrame = true;
    requestAnimationFrame(renderParallax);
  }
}, { passive: true });
window.addEventListener('mouseleave', resetLook);

function setButtonBusy(isBusy) {
  const spinner = loginButton.querySelector('.btn-spinner');
  const caption = loginButton.querySelector('.btn-caption');
  loginButton.disabled = isBusy;
  spinner.classList.toggle('hidden', !isBusy);
  caption.style.opacity = isBusy ? '.62' : '1';
}

function updateCaretLook(length) {
  const shiftX = Math.min(Math.max((length - 5) * .6, -5), 5);
  eyeL.style.transform = `translate3d(${shiftX}px, 3.5px, 0)`;
  eyeR.style.transform = `translate3d(${shiftX}px, 3.5px, 0)`;
  robotHead.style.transform = `rotateX(4deg) rotateY(${shiftX * .6}deg)`;
}

function enterPasswordMode() {
  if (password.type === 'password') {
    cardAssembly.classList.add('covering-eyes');
    cardAssembly.classList.remove('peeking');
    setEyes('normal');
    setMouth('o');
    robotHead.classList.add('has-blush');
    setRobotMessage('Tôi sẽ nhìn đi chỗ khác. Bí mật tuyệt đối nhé!');
    playSound('cover');
  } else {
    cardAssembly.classList.add('peeking');
    cardAssembly.classList.remove('covering-eyes');
    setEyes('peek');
    setMouth('w');
    setRobotMessage('Tôi chỉ hiển thị mật khẩu theo yêu cầu của bạn thôi!');
  }
}

username.addEventListener('focus', () => {
  setMouth('w');
  robotHead.classList.add('has-blush');
  setRobotMessage(username.value.trim() ? `Rất vui được gặp lại, ${username.value.trim()}!` : 'Nhập tên đăng nhập của bạn nhé.');
  updateCaretLook(username.value.length);
});
username.addEventListener('input', () => {
  playSound('tap');
  setRobotMessage(username.value.trim() ? `Đã nhận diện: ${username.value.trim()}.` : 'Tôi đang chờ tên đăng nhập.');
  updateCaretLook(username.value.length);
});
username.addEventListener('blur', () => {
  if (document.activeElement !== password) {
    robotHead.classList.remove('has-blush');
    setMouth('dot');
    resetLook();
  }
});

password.addEventListener('focus', enterPasswordMode);
password.addEventListener('input', () => playSound('tap'));
password.addEventListener('blur', () => {
  cardAssembly.classList.remove('covering-eyes', 'peeking');
  resetFace();
  setRobotMessage('Sẵn sàng xác thực với Auth Service.');
});

document.querySelectorAll('.toggle-password').forEach((button) => {
  button.addEventListener('click', () => {
    const input = document.getElementById(button.dataset.target);
    const hidden = input.type === 'password';
    input.type = hidden ? 'text' : 'password';
    button.querySelector('.eye-show').classList.toggle('hidden', hidden);
    button.querySelector('.eye-hide').classList.toggle('hidden', !hidden);
    if (document.activeElement === input) enterPasswordMode();
    else resetFace();
    playSound(hidden ? 'peek' : 'cover');
  });
});

loginButton.addEventListener('mouseenter', () => {
  if (!loginButton.disabled && !cardAssembly.classList.contains('covering-eyes')) {
    cardAssembly.classList.add('hovering-submit');
    setMouth('smirk');
  }
});
loginButton.addEventListener('mouseleave', () => {
  cardAssembly.classList.remove('hovering-submit');
  if (!cardAssembly.classList.contains('celebrating')) setMouth('dot');
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  hideResult();
  if (!form.reportValidity()) {
    triggerError('Tôi cần đủ tên đăng nhập và mật khẩu để kết nối Auth Service.');
    return;
  }

  cardAssembly.classList.remove('covering-eyes', 'peeking', 'state-shake');
  scanline.classList.remove('hidden');
  setEyes('peek');
  setMouth('o');
  setRobotMessage('Đang gửi credential tới Auth Service… beep boop.');
  setButtonBusy(true);

  try {
    const response = await authApi.login(username.value.trim(), password.value);
    saveSession(response.accessToken);
    scanline.classList.add('hidden');
    triggerCelebration(`Đã xác thực rồi, ${username.value.trim()}! Chào mừng vào Campus Space.`);
    showResult('Xác thực thành công. Đang mở không gian làm việc…', 'success');
    window.setTimeout(redirectToApp, 1250);
  } catch (error) {
    scanline.classList.add('hidden');
    triggerError(describeApiError(error));
  } finally {
    setButtonBusy(false);
  }
});

function triggerCelebration(message) {
  cardAssembly.classList.remove('covering-eyes', 'peeking', 'state-shake');
  cardAssembly.classList.add('celebrating');
  setEyes('happy');
  setMouth('happy');
  robotHead.classList.add('has-blush');
  setRobotMessage(message);
  playSound('success');
  triggerConfetti();
}

function triggerError(message) {
  cardAssembly.classList.remove('celebrating', 'covering-eyes', 'peeking');
  cardAssembly.classList.add('state-shake');
  setEyes('error');
  setMouth('frown');
  sweatDrop.classList.remove('hidden');
  robotHead.classList.remove('has-blush');
  setRobotMessage(message);
  showResult(message);
  playSound('error');
  window.setTimeout(() => {
    cardAssembly.classList.remove('state-shake');
    resetFace();
    setRobotMessage('Không sao cả. Kiểm tra lại rồi thử một lần nữa nhé.');
  }, 2600);
}

soundToggle.addEventListener('click', () => {
  soundEnabled = !soundEnabled;
  soundOnIcon.classList.toggle('hidden', !soundEnabled);
  soundOffIcon.classList.toggle('hidden', soundEnabled);
  if (soundEnabled) playSound('peek');
});

function resizeCanvas() {
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
}

const particles = Array.from({ length: 72 }, () => ({}));
const colors = ['#d8654b', '#38bdf8', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6'];
let confettiActive = false;

function triggerConfetti() {
  const originX = canvas.width / 2;
  const originY = canvas.height / 2 - 80;
  particles.forEach((particle, index) => Object.assign(particle, {
    x: originX + Math.random() * 60 - 30,
    y: originY,
    vx: (Math.random() - .5) * 12,
    vy: Math.random() * -10 - 4,
    size: Math.random() * 6 + 4,
    color: colors[index % colors.length],
    rotation: Math.random() * 360,
    spin: (Math.random() - .5) * 9,
    alpha: 1,
    decay: Math.random() * .012 + .008,
  }));
  if (!confettiActive) {
    confettiActive = true;
    requestAnimationFrame(drawConfetti);
  }
}

function drawConfetti() {
  context.clearRect(0, 0, canvas.width, canvas.height);
  let alive = 0;
  particles.forEach((particle) => {
    if (particle.alpha <= 0) return;
    alive += 1;
    particle.x += particle.vx;
    particle.y += particle.vy;
    particle.vy += .23;
    particle.rotation += particle.spin;
    particle.alpha -= particle.decay;
    context.save();
    context.translate(particle.x, particle.y);
    context.rotate(particle.rotation * Math.PI / 180);
    context.fillStyle = particle.color;
    context.globalAlpha = Math.max(0, particle.alpha);
    context.fillRect(-particle.size / 2, -particle.size / 2, particle.size, particle.size * .7);
    context.restore();
  });
  if (alive) requestAnimationFrame(drawConfetti);
  else {
    confettiActive = false;
    context.clearRect(0, 0, canvas.width, canvas.height);
  }
}

window.addEventListener('resize', resizeCanvas, { passive: true });
resizeCanvas();
startBlinking();
