/**
 * Vanilla Cave shell for Minecraftuuuum UCC (does not vendor log-view-machine npm).
 * Sends X-Tenant-ID and POSTs envelope v2 to /cave/route.
 */
(function (global) {
  'use strict';

  var API_BASE = (global.MINECRAFTUUUUM_API || '').replace(/\/$/, '');
  var tenantId = 'minecraftuuuum';
  var patched = false;

  function apiUrl(path) {
    if (!path) return API_BASE || '/';
    if (/^https?:/i.test(path)) return path;
    return API_BASE + path;
  }

  function traceId() {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID();
    return 'trace-' + Date.now() + '-' + Math.random().toString(16).slice(2);
  }

  function headers(extra) {
    var h = { 'Content-Type': 'application/json', 'X-Tenant-ID': tenantId };
    if (extra) Object.assign(h, extra);
    return h;
  }

  function api(path, opts) {
    opts = opts || {};
    return fetch(apiUrl(path), Object.assign({
      credentials: 'include',
      headers: headers(opts.headers)
    }, opts)).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (data) {
        if (!r.ok) {
          var err = new Error(data.error || r.statusText);
          err.status = r.status;
          err.body = data;
          throw err;
        }
        return data;
      });
    });
  }

  function patchFetch() {
    if (patched || typeof fetch !== 'function') return;
    patched = true;
    var orig = global.fetch;
    global.fetch = function (input, init) {
      init = init || {};
      var nextHeaders = new Headers(init.headers || (input && input.headers) || undefined);
      if (!nextHeaders.has('X-Tenant-ID')) nextHeaders.set('X-Tenant-ID', tenantId);
      init = Object.assign({}, init, { headers: nextHeaders });
      return orig.call(this, input, init);
    };
  }

  function caveRoute(route, payload, opts) {
    opts = opts || {};
    var body = {
      schema_version: '2.0',
      route: route,
      payload: payload || {},
      trace_id: opts.traceId || traceId(),
      reply_mode: 'sync_http',
      tenant: opts.tenant || tenantId
    };
    return api('/cave/route', { method: 'POST', body: JSON.stringify(body) });
  }

  function mountShell(host) {
    if (!host) return;
    host.innerHTML =
      '<div class="cave-shell">' +
      '<strong>Cave</strong> tenant <code id="cave-tenant"></code> ' +
      '<button type="button" id="cave-ping">Ping</button> ' +
      '<button type="button" id="cave-pages">Pages</button> ' +
      '<button type="button" id="cave-lib">Library search</button>' +
      '<pre id="cave-out" class="cave-out"></pre></div>';
    host.querySelector('#cave-tenant').textContent = tenantId;
    function show(obj) {
      host.querySelector('#cave-out').textContent = JSON.stringify(obj, null, 2);
    }
    host.querySelector('#cave-ping').onclick = function () {
      caveRoute('minecraftuuuum:ping').then(show).catch(function (e) { show({ error: e.message, body: e.body }); });
    };
    host.querySelector('#cave-pages').onclick = function () {
      caveRoute('minecraftuuuum:pages').then(show).catch(function (e) { show({ error: e.message, body: e.body }); });
    };
    host.querySelector('#cave-lib').onclick = function () {
      caveRoute('minecraftuuuum:library/search', { q: '', limit: 10 }).then(show).catch(function (e) { show({ error: e.message, body: e.body }); });
    };
  }

  function boot() {
    patchFetch();
    api('/api/settings').then(function (s) {
      if (s && s.tenantId) tenantId = s.tenantId;
      var bar = document.getElementById('cave-shell') || document.querySelector('[data-cave-shell]');
      if (bar) mountShell(bar);
      var nav = document.querySelector('.nav');
      if (nav && !nav.querySelector('a[href="/cave"]')) {
        var a = document.createElement('a');
        a.href = '/cave';
        a.textContent = 'Cave';
        nav.appendChild(a);
      }
    }).catch(function () {
      var bar = document.getElementById('cave-shell');
      if (bar) mountShell(bar);
    });
  }

  global.MinecraftuuuumCave = { caveRoute: caveRoute, api: api, tenantId: function () { return tenantId; } };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})(typeof window !== 'undefined' ? window : this);
