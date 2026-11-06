// Huddle client: one STOMP connection, one room subscription at a time.
// All user-supplied text is rendered with textContent (never innerHTML) to prevent XSS.
(() => {
  'use strict';

  const DEFAULT_ROOMS = ['general', 'standup', 'random'];
  const $ = (id) => document.getElementById(id);

  const state = {
    room: null,
    subscription: null,
    rooms: new Set(DEFAULT_ROOMS),
  };

  // ---- Connection --------------------------------------------------------
  const wsUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;
  const client = new StompJs.Client({
    brokerURL: wsUrl,
    reconnectDelay: 2000, // auto-reconnect if the server restarts
  });

  client.onConnect = () => {
    setStatus('online', 'Connected');
    client.subscribe('/user/queue/errors', (frame) => showError(JSON.parse(frame.body).error));
    joinRoom(state.room ?? roomFromHash() ?? 'general');
  };
  client.onWebSocketClose = () => setStatus('offline', 'Disconnected, retrying…');
  client.onStompError = (frame) => showError(frame.headers.message || 'Connection error');

  // ---- Rooms -------------------------------------------------------------
  async function joinRoom(room) {
    state.subscription?.unsubscribe();
    state.room = room;
    state.rooms.add(room);
    history.replaceState(null, '', `#${room}`);
    renderRooms();
    $('room-title').textContent = `#${room}`;
    $('content').placeholder = `Message #${room}`;
    $('summary').hidden = true;
    clearError();

    // Subscribe first, then load history, so nothing sent in between is missed.
    const seen = new Set();
    const list = $('messages');
    list.replaceChildren();
    state.subscription = client.subscribe(`/topic/rooms/${room}`, (frame) => {
      const message = JSON.parse(frame.body);
      if (!seen.has(message.id)) { seen.add(message.id); appendMessage(message); }
    });

    try {
      const res = await fetch(`/api/rooms/${encodeURIComponent(room)}/messages?limit=100`);
      if (!res.ok) throw new Error((await res.json()).detail);
      for (const message of await res.json()) {
        if (!seen.has(message.id)) { seen.add(message.id); appendMessage(message, false); }
      }
      sortMessages();
    } catch (e) {
      showError(`Could not load history: ${e.message}`);
    }
  }

  function renderRooms() {
    const ul = $('rooms');
    ul.replaceChildren(...[...state.rooms].map((room) => {
      const li = document.createElement('li');
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.textContent = `# ${room}`;
      btn.className = room === state.room ? 'active' : '';
      btn.addEventListener('click', () => joinRoom(room));
      li.append(btn);
      return li;
    }));
  }

  function roomFromHash() {
    const room = decodeURIComponent(location.hash.slice(1));
    return /^[a-z0-9](?:[a-z0-9-]{0,30}[a-z0-9])?$/.test(room) ? room : null;
  }

  // ---- Messages ----------------------------------------------------------
  function appendMessage(message, scroll = true) {
    const li = document.createElement('li');
    li.className = 'message';
    li.dataset.sentAt = message.sentAt;

    const meta = document.createElement('div');
    meta.className = 'meta';
    const author = document.createElement('strong');
    author.textContent = message.author;
    const time = document.createElement('time');
    time.dateTime = message.sentAt;
    time.textContent = new Date(message.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    meta.append(author, time);

    const body = document.createElement('p');
    body.textContent = message.content;

    li.append(meta, body);
    $('messages').append(li);
    if (scroll) li.scrollIntoView({ block: 'end' });
  }

  function sortMessages() {
    const list = $('messages');
    const items = [...list.children].sort((a, b) => a.dataset.sentAt.localeCompare(b.dataset.sentAt));
    list.replaceChildren(...items);
    list.lastElementChild?.scrollIntoView({ block: 'end' });
  }

  $('composer').addEventListener('submit', (event) => {
    event.preventDefault();
    const author = $('author').value.trim();
    const content = $('content').value.trim();
    if (!author) { showError('Enter your name first.'); $('author').focus(); return; }
    if (!content || !client.connected) return;
    client.publish({
      destination: `/app/rooms/${state.room}/messages`,
      body: JSON.stringify({ author, content }),
    });
    $('content').value = '';
    clearError();
  });

  $('join-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const input = $('join-input');
    const room = input.value.trim().toLowerCase();
    if (input.checkValidity() && room) { input.value = ''; joinRoom(room); }
  });

  // ---- Summary -----------------------------------------------------------
  $('summarise').addEventListener('click', async () => {
    const button = $('summarise');
    button.disabled = true;
    button.textContent = 'Summarising…';
    try {
      const res = await fetch(`/api/rooms/${encodeURIComponent(state.room)}/summary`, { method: 'POST' });
      const body = await res.json();
      if (!res.ok) throw new Error(body.detail || 'Summary failed');
      renderSummary(body);
    } catch (e) {
      showError(e.message);
    } finally {
      button.disabled = false;
      button.textContent = 'Summarise';
    }
  });

  $('summary-close').addEventListener('click', () => { $('summary').hidden = true; });

  function renderSummary({ summary, actionItems }) {
    $('summary-text').textContent = summary;
    const ul = $('summary-actions');
    ul.replaceChildren(...(actionItems.length ? actionItems : [{ owner: '', task: 'None' }]).map(({ owner, task }) => {
      const li = document.createElement('li');
      if (owner) {
        const who = document.createElement('strong');
        who.textContent = `${owner}: `;
        li.append(who);
      }
      li.append(document.createTextNode(task));
      return li;
    }));
    $('summary').hidden = false;
  }

  // ---- Status / errors ---------------------------------------------------
  function setStatus(stateName, text) {
    const el = $('status');
    el.dataset.state = stateName;
    el.textContent = text;
  }
  function showError(text) { const el = $('error'); el.textContent = text; el.hidden = false; }
  function clearError() { $('error').hidden = true; }

  // ---- Boot --------------------------------------------------------------
  const author = $('author');
  try { author.value = localStorage.getItem('huddle.author') ?? ''; } catch { /* storage blocked */ }
  author.addEventListener('change', () => {
    try { localStorage.setItem('huddle.author', author.value.trim()); } catch { /* ignore */ }
  });
  window.addEventListener('hashchange', () => { const r = roomFromHash(); if (r && r !== state.room) joinRoom(r); });

  renderRooms();
  client.activate();
})();
