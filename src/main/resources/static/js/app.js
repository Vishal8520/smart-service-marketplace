let activeCat = null;
    let activeCity = null;
    let selectedService = null;
    let createdBooking = null;
    let adminJwt = sessionStorage.getItem('adminToken') || null;

    const CATEGORY_ICONS = { 1: '🔧', 2: '⚡', 3: '🧹', 4: '📚', 5: '🎨', 6: '💻', 7: '🪚', 8: '🐛' };
    const CATEGORY_NAMES = { 1: 'Plumbing', 2: 'Electrical', 3: 'Home Cleaning', 4: 'Tutoring', 5: 'Graphic Design', 6: 'Web Development', 7: 'Carpentry', 8: 'Pest Control' };

    document.addEventListener('DOMContentLoaded', () => {
      loadCities();
      loadServices();
      buildChips();
      if (adminJwt) showAdminConsole();
    });

    async function loadCities() {
      try {
        const r = await fetch('/api/cities');
        if (r.ok) {
          const cities = await r.json();
          const sel = document.getElementById('citySelector');
          sel.innerHTML = '<option value="">All Cities</option>' +
            cities.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
      } catch (e) { }
    }

    function buildChips() {
      const row = document.getElementById('chips');
      Object.entries(CATEGORY_NAMES).forEach(([id, name]) => {
        const d = document.createElement('div');
        d.className = 'chip';
        d.textContent = CATEGORY_ICONS[id] + ' ' + name;
        d.onclick = () => filterCat(parseInt(id), d);
        row.appendChild(d);
      });
    }

    function showView(name) {
      document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
      document.querySelectorAll('.sub-link').forEach(l => l.classList.remove('active'));
      document.getElementById('view-' + name).classList.add('active');
      const tab = document.getElementById('tab-' + name);
      if (tab) tab.classList.add('active');
    }

    function onCityChanged() {
      activeCity = document.getElementById('citySelector').value || null;
      loadServices(null, activeCat, activeCity);
    }

    async function loadServices(keyword, catId, cityId) {
      const grid = document.getElementById('servicesGrid');
      let url = '/api/services?page=0&size=24';
      if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
      if (catId) url += `&categoryId=${catId}`;
      if (cityId) url += `&cityId=${cityId}`;

      try {
        const r = await fetch(url);
        if (!r.ok) throw new Error('HTTP ' + r.status);
        const data = await r.json();
        renderServices(data.content || []);
      } catch (err) {
        grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><h3>Unable to load services</h3></div>`;
      }
    }

    function renderServices(services) {
      const grid = document.getElementById('servicesGrid');
      if (!services.length) {
        grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><h3>No approved services found</h3></div>`;
        return;
      }
      grid.innerHTML = services.map(s => {
        const icon = CATEGORY_ICONS[s.categoryId] || '🛠';
        return `<div class="card">
          <div class="card-icon">
            ${icon}
            <div class="city-badge-chip">📍 ${s.cityName || 'All Cities'}</div>
          </div>
          <div class="card-body">
            <div class="card-cat">${s.categoryName || 'General'}</div>
            <div class="card-title">${esc(s.title)}</div>
            <div class="card-desc">${esc(s.description || '')}</div>
          </div>
          <div class="card-footer">
            <span class="price">₹${Number(s.price).toLocaleString('en-IN')}</span>
            <button class="btn btn-amber btn-sm" onclick="startCheckout(${s.id},'${escJs(s.title)}',${s.price})">Book Now</button>
          </div>
        </div>`;
      }).join('');
    }

    function filterCat(id, el) {
      document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      el.classList.add('active');
      activeCat = id;
      loadServices(null, id, activeCity);
    }

    function doSearch() {
      const kw = document.getElementById('searchInput').value.trim();
      const cat = document.getElementById('searchCat').value || activeCat;
      loadServices(kw, cat, activeCity);
    }

    // ── CHECKOUT FLOW ──
    function startCheckout(id, title, price) {
      selectedService = { id, title, price };
      showView('checkout');
      goStep(1);
    }

    function goStep(n) {
      [1, 2, 3, 4].forEach(i => {
        const el = document.getElementById('step' + i);
        if (el) el.style.display = i === n ? 'block' : 'none';
        const s = document.getElementById('s' + i);
        if (s) { s.classList.remove('active', 'done'); if (i < n) s.classList.add('done'); else if (i === n) s.classList.add('active'); }
      });
      if (n === 3) {
        setTimeout(() => {
          const input = document.getElementById('upi_ref_input');
          if (input) { input.focus(); validateUpiRefInline(); }
        }, 100);
      }
    }

    let upiValTimer = null;
    function validateUpiRefInline() {
      clearTimeout(upiValTimer);
      upiValTimer = setTimeout(() => {
        const input = document.getElementById('upi_ref_input');
        const feedback = document.getElementById('upi_ref_feedback');
        if (!input || !feedback) return;
        const val = input.value.trim();
        if (!val) {
          feedback.textContent = '';
          return;
        }
        const upiRegex = /^[\w.\-]{2,49}@[a-zA-Z]{2,49}$/;
        const utrRegex = /^\d{12}$/;
        if (upiRegex.test(val) || utrRegex.test(val)) {
          feedback.style.color = '#137333';
          feedback.textContent = '✓ Valid UPI ID / UTR reference format';
        } else {
          feedback.style.color = '#c5221f';
          feedback.textContent = '✕ Enter a valid UPI ID (e.g. user@upi) or 12-digit UTR string';
        }
      }, 200);
    }

    async function submitBookingAndProceedToPay() {
      const nm = document.getElementById('c_name').value.trim();
      const ph = document.getElementById('c_phone').value.trim();
      const em = document.getElementById('c_email').value.trim();
      const addr = document.getElementById('c_address').value.trim();
      const sched = document.getElementById('c_schedule').value;
      if (!nm || !ph || !em || !addr || !sched) { toast('Please fill all fields', true); return; }

      try {
        const r = await fetch('/api/bookings', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            serviceId: selectedService.id,
            customerName: nm, customerEmail: em, customerPhone: ph,
            address: addr, notes: document.getElementById('c_notes').value.trim(),
            scheduledAt: new Date(sched).toISOString()
          })
        });
        if (!r.ok) throw new Error('Booking failed');
        createdBooking = await r.json();

        document.getElementById('orderSummary').innerHTML = `
          <div class="order-row"><span>Booking Reference</span><span>#${createdBooking.id}</span></div>
          <div class="order-row"><span>Service</span><span>${esc(selectedService.title)}</span></div>
          <div class="order-row"><span>Total Amount</span><span style="color:var(--warn);font-weight:800">₹${Number(selectedService.price).toLocaleString('en-IN')}</span></div>
        `;
        goStep(3);
      } catch (err) { toast(err.message, true); }
    }

    async function submitUpiPaymentReference() {
      const ref = document.getElementById('upi_ref_input').value.trim();
      if (!ref || ref.length < 3) { toast('Please enter a valid UPI ID or reference string', true); return; }

      const btn = document.getElementById('paySubmitBtn');
      btn.disabled = true; btn.textContent = '⏳ Processing Payment...';

      try {
        const r = await fetch('/api/payments', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ bookingId: createdBooking.id, upiReferenceId: ref })
        });
        if (!r.ok) { const data = await r.json(); throw new Error(data.message || 'Payment submission failed'); }

        const paymentRes = await r.json();
        document.getElementById('confirmSummary').innerHTML = `
          <div class="order-row"><span>Booking ID</span><span>#${createdBooking.id}</span></div>
          <div class="order-row"><span>Payment Status</span><span style="color:#137333;font-weight:700">AUTO-CONFIRMED (Demo)</span></div>
          <div class="order-row"><span>UPI Reference</span><span>${esc(paymentRes.upiReferenceId || ref)}</span></div>
        `;

        goStep(4);
        toast('🎉 Booking & Payment Confirmed!');
      } catch (err) {
        toast(err.message, true);
        btn.disabled = false; btn.textContent = '✓ Submit & Confirm Payment';
      }
    }

    // ── MY BOOKINGS TRACKER & PAGINATION ──
    let myBookingsPage = 0;
    let myBookingsTotalPages = 1;

    function changeMyBookingFilter() {
      fetchMyBookings(0);
    }

    function changeMyBookingPage(delta) {
      const targetPage = myBookingsPage + delta;
      if (targetPage >= 0 && targetPage < myBookingsTotalPages) {
        fetchMyBookings(targetPage);
      }
    }

    async function fetchMyBookings(page = 0) {
      myBookingsPage = page;
      const email = document.getElementById('myEmail').value.trim();
      const status = document.getElementById('myBookingStatus').value;
      const tbody = document.getElementById('bookings-body');
      const pagBar = document.getElementById('bookings-pagination');

      if (!email) {
        toast('Enter your email to fetch bookings', true);
        return;
      }

      updateUrlState('bookings', { email, status, page });

      try {
        let url = `/api/bookings/me?email=${encodeURIComponent(email)}&page=${page}&size=10`;
        if (status) url += `&status=${encodeURIComponent(status)}`;

        const r = await fetch(url);
        if (!r.ok) throw new Error('Failed to fetch bookings');
        const data = await r.json();
        const rows = data.content || [];
        myBookingsTotalPages = data.totalPages || 1;

        if (!rows.length) {
          tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:1.5rem">No bookings found for ${esc(email)}</td></tr>`;
          pagBar.style.display = 'none';
          return;
        }

        tbody.innerHTML = rows.map(b => {
          const isPaid = b.status === 'CONFIRMED' || b.status === 'COMPLETED';
          const isCancelled = b.status === 'CANCELLED';

          // Visual Stepper derivation
          const s1Class = 'b-step done';
          let s2Class = 'b-step';
          let s3Class = 'b-step';

          if (isCancelled) {
            s2Class = 'b-step cancelled';
            s3Class = 'b-step cancelled';
          } else if (b.status === 'COMPLETED') {
            s2Class = 'b-step done';
            s3Class = 'b-step done';
          } else if (isPaid) {
            s2Class = 'b-step done';
            s3Class = 'b-step active';
          } else {
            s2Class = 'b-step active';
          }

          const stepperHtml = `
            <div class="booking-stepper">
              <span class="${s1Class}">1. Booked ✓</span>
              <span class="b-arrow">→</span>
              <span class="${s2Class}">${isPaid ? '2. Payment Verified ✓' : '2. Payment Pending'}</span>
              <span class="b-arrow">→</span>
              <span class="${s3Class}">${b.status === 'COMPLETED' ? '3. Completed ✓' : '3. Fulfilled'}</span>
            </div>
          `;

          return `<tr id="brow-${b.id}">
            <td>#${b.id}</td>
            <td><strong>${esc(b.serviceTitle)}</strong><br><small>Provider: ${esc(b.providerName || 'Assigned')}</small></td>
            <td>${esc(b.address || 'N/A')}<br><small>${b.scheduledAt ? new Date(b.scheduledAt).toLocaleString() : ''}</small></td>
            <td>${stepperHtml}</td>
            <td><span class="badge ${isPaid ? 'badge-confirmed' : (isCancelled ? 'badge-cancelled' : 'badge-pending')}">${b.status}</span></td>
            <td>${isPaid ? `📞 ${esc(b.providerPhone || 'N/A')}<br>✉ ${esc(b.providerEmail || 'N/A')}` : '🔒 Contact Hidden Until Verification'}</td>
            <td>${!isPaid && !isCancelled ? `<button class="btn btn-amber btn-sm" onclick="promptPayRef(${b.id})">Submit UPI Ref</button>` : '—'}</td>
          </tr>`;
        }).join('');

        // Pagination controls update
        pagBar.style.display = 'flex';
        document.getElementById('b-page-info').textContent = `Page ${page + 1} of ${myBookingsTotalPages}`;
        document.getElementById('b-prev-btn').disabled = page <= 0;
        document.getElementById('b-next-btn').disabled = page >= myBookingsTotalPages - 1;

      } catch (err) { toast(err.message, true); }
    }

    function promptPayRef(bookingId) {
      const ref = prompt(`Enter UPI Reference Transaction ID for Booking #${bookingId}:`);
      if (ref && ref.trim()) {
        fetch('/api/payments', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ bookingId: bookingId, upiReferenceId: ref.trim() })
        }).then(r => r.json()).then(data => {
          toast('UPI reference submitted for review!');
          fetchMyBookings(myBookingsPage);
        });
      }
    }

    // ── ADMIN PORTAL & ONE-CLICK UPI VERIFICATION ──
    async function adminLogin(e) {
      e.preventDefault();
      const email = document.getElementById('admin_email').value.trim();
      const password = document.getElementById('admin_pass').value.trim();
      try {
        const r = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });
        const data = await r.json();
        if (!r.ok) throw new Error(data.message || 'Admin login failed');

        adminJwt = data.accessToken;
        sessionStorage.setItem('adminToken', adminJwt);
        showAdminConsole();
        toast('Admin logged in successfully!');
      } catch (err) { toast(err.message, true); }
    }

    function showAdminConsole() {
      document.getElementById('admin-login-box').style.display = 'none';
      document.getElementById('admin-dashboard-box').style.display = 'block';
      loadAdminPendingServices();
    }

    function adminLogout() {
      adminJwt = null;
      sessionStorage.removeItem('adminToken');
      document.getElementById('admin-login-box').style.display = 'block';
      document.getElementById('admin-dashboard-box').style.display = 'none';
    }

    function switchAdminTab(name) {
      document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
      document.getElementById('atab-' + name).classList.add('active');
      ['services', 'payments', 'cities'].forEach(s => {
        document.getElementById('admin-sec-' + s).style.display = s === name ? 'block' : 'none';
      });
      updateUrlState('admin', { tab: name });
      if (name === 'services') loadAdminPendingServices();
      if (name === 'payments') loadAdminPendingPayments();
      if (name === 'cities') loadAdminCities();
    }

    async function loadAdminPendingServices() {
      const tbody = document.getElementById('admin-services-body');
      try {
        const r = await fetch('/api/admin/services?status=PENDING_REVIEW', {
          headers: { 'Authorization': 'Bearer ' + adminJwt }
        });
        const data = await r.json();
        const rows = data.content || [];
        if (!rows.length) {
          tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:1.5rem">No service listings pending approval</td></tr>`;
          return;
        }
        tbody.innerHTML = rows.map(s => `<tr>
          <td>#${s.id}</td>
          <td><strong>${esc(s.title)}</strong><br><small>${s.categoryName}</small></td>
          <td>${s.cityName || 'All Cities'}</td>
          <td>${esc(s.providerName)}</td>
          <td>₹${s.price}</td>
          <td>
            <button class="btn btn-success btn-sm" onclick="approveService(${s.id})">Approve</button>
            <button class="btn btn-danger btn-sm" onclick="rejectService(${s.id})">Reject</button>
          </td>
        </tr>`).join('');
      } catch (e) { }
    }

    async function approveService(id) {
      await fetch(`/api/admin/services/${id}/approve`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + adminJwt }
      });
      toast(`Service #${id} approved!`);
      loadAdminPendingServices();
    }

    async function rejectService(id) {
      await fetch(`/api/admin/services/${id}/reject`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + adminJwt }
      });
      toast(`Service #${id} rejected.`);
      loadAdminPendingServices();
    }

    // ── ADMIN PAYMENT AUDIT & REVERSAL ──
    async function loadAdminPendingPayments() {
      const tbody = document.getElementById('admin-payments-body');
      try {
        const r = await fetch('/api/admin/payments', {
          headers: { 'Authorization': 'Bearer ' + adminJwt }
        });
        const data = await r.json();
        const rows = data.content || [];
        if (!rows.length) {
          tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:1.5rem">No payment audit records found</td></tr>`;
          return;
        }
        tbody.innerHTML = rows.map(p => {
          const isReversed = p.status === 'REVERSED' || p.status === 'REJECTED';
          const badgeClass = isReversed ? 'badge-cancelled' : 'badge-confirmed';
          const autoBadge = p.autoConfirmed ? '<span style="font-size:.65rem;background:#e8f0fe;color:#1a73e8;padding:1px 4px;border-radius:3px;margin-left:4px">AUTO</span>' : '';

          return `<tr id="pay-row-${p.id}">
            <td>#${p.id}</td>
            <td>#${p.bookingId}</td>
            <td><strong>${esc(p.customerName || 'Customer')}</strong></td>
            <td>₹${p.amount}</td>
            <td><code>${esc(p.upiReferenceId || 'N/A')}</code><br><small style="color:var(--gray-500)">Self-reported (unverified)</small></td>
            <td><span class="badge ${badgeClass}">${p.status}</span>${autoBadge}</td>
            <td>${new Date(p.createdAt).toLocaleString()}</td>
            <td style="display:flex;gap:.4rem;align-items:center">
              ${!isReversed ? `
                <select class="form-control" style="width:140px;padding:.2rem .4rem;font-size:.75rem" onchange="oneClickReversePayment(${p.id}, this.value)">
                  <option value="">Reverse Payment…</option>
                  <option value="UPI ref not found">Ref Not Found</option>
                  <option value="Amount mismatch">Amount Mismatch</option>
                  <option value="Duplicate reference">Duplicate Ref</option>
                  <option value="OTHER">Custom Reason…</option>
                </select>
              ` : `<small style="color:var(--gray-500)">Reversed: ${esc(p.reversalReason || 'Audit Action')}</small>`}
            </td>
          </tr>`;
        }).join('');
      } catch (e) { }
    }

    async function oneClickReversePayment(id, chosenReason) {
      if (!chosenReason) return;
      let reason = chosenReason;
      if (chosenReason === 'OTHER') {
        reason = prompt('Enter payment reversal audit reason:') || 'Reversed by admin audit';
      }

      try {
        const r = await fetch(`/api/admin/payments/${id}/reverse`, {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + adminJwt, 'Content-Type': 'application/json' },
          body: JSON.stringify({ notes: reason })
        });
        if (!r.ok) throw new Error('Reversal failed');
        toast(`Payment #${id} reversed (${reason}). Customer notified.`);
        loadAdminPendingPayments();
      } catch (err) { toast(err.message, true); }
    }

    // ── URL PERSISTENT STATE MANAGEMENT ──
    function updateUrlState(view, params = {}) {
      const hashParams = new URLSearchParams();
      hashParams.set('view', view);
      Object.keys(params).forEach(k => {
        if (params[k] !== null && params[k] !== undefined && params[k] !== '') {
          hashParams.set(k, params[k]);
        }
      });
      window.location.hash = hashParams.toString();
    }

    function syncStateFromUrl() {
      if (!window.location.hash) return;
      const hashStr = window.location.hash.substring(1);
      const params = new URLSearchParams(hashStr);
      const view = params.get('view');

      if (view) {
        showView(view);
        if (view === 'bookings') {
          const email = params.get('email');
          const status = params.get('status');
          const page = parseInt(params.get('page') || '0', 10);
          if (email) document.getElementById('myEmail').value = email;
          if (status) document.getElementById('myBookingStatus').value = status;
          if (email) fetchMyBookings(page);
        } else if (view === 'admin') {
          const tab = params.get('tab');
          if (tab) switchAdminTab(tab);
        }
      }
    }

    window.addEventListener('hashchange', syncStateFromUrl);
    window.addEventListener('DOMContentLoaded', () => {
      syncStateFromUrl();
    });

    async function loadAdminCities() {
      const tbody = document.getElementById('admin-cities-body');
      try {
        const r = await fetch('/api/admin/cities', {
          headers: { 'Authorization': 'Bearer ' + adminJwt }
        });
        const cities = await r.json();
        tbody.innerHTML = cities.map(c => `<tr>
          <td>#${c.id}</td>
          <td><strong>${esc(c.name)}</strong></td>
          <td>${esc(c.state)}</td>
          <td><span class="badge ${c.active ? 'badge-confirmed' : 'badge-cancelled'}">${c.active ? 'ACTIVE' : 'INACTIVE'}</span></td>
          <td><button class="btn btn-outline btn-sm" onclick="toggleCity(${c.id})">Toggle Active</button></td>
        </tr>`).join('');
      } catch (e) { }
    }

    async function addCity() {
      const name = document.getElementById('new_city_name').value.trim();
      const state = document.getElementById('new_city_state').value.trim();
      if (!name || !state) return;
      await fetch('/api/admin/cities', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + adminJwt, 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, state })
      });
      toast(`City ${name} created!`);
      loadAdminCities();
      loadCities();
    }

    async function toggleCity(id) {
      await fetch(`/api/admin/cities/${id}/toggle`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + adminJwt }
      });
      loadAdminCities();
      loadCities();
    }

    function toast(msg, isErr = false) {
      const wrap = document.getElementById('toast-wrap');
      const t = document.createElement('div');
      t.className = 'toast' + (isErr ? ' err' : '');
      t.innerHTML = `<span>${isErr ? '⚠' : '✓'}</span><span>${esc(msg)}</span>`;
      wrap.appendChild(t);
      setTimeout(() => t.remove(), 3500);
    }

    function esc(s) { return s ? String(s).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m])) : ''; }
    function escJs(s) { return s ? String(s).replace(/'/g, "\\'").replace(/"/g, '\\"') : ''; }


/* ── AI INTELLIGENCE & CHATBOT FEATURES ── */
let aiChatHistory = [];

async function doSmartSearch() {
  const q = document.getElementById('searchInput').value.trim();
  if (!q) {
    toast('Please enter a natural language search query (e.g. "pipe is leaking")', true);
    return;
  }
  const citySelect = document.getElementById('cityFilter');
  const city = citySelect && citySelect.selectedIndex > 0 ? citySelect.options[citySelect.selectedIndex].text : '';

  showSpinner(true);
  try {
    let url = `/api/services/smart-search?q=${encodeURIComponent(q)}`;
    if (city) url += `&city=${encodeURIComponent(city)}`;

    const res = await fetch(url);
    const data = await res.json();

    const grid = document.getElementById('services-grid');
    if (!data.results || data.results.length === 0) {
      grid.innerHTML = `<div style="grid-column:1/-1;text-align:center;padding:3rem;background:#fff;border-radius:8px">
        <h3>No direct AI matches found</h3>
        <p>Falling back to regular keyword search...</p>
      </div>`;
      setTimeout(doSearch, 1500);
      return;
    }

    let headerHtml = `<div style="grid-column:1/-1;background:#eef2ff;border:1px solid #c7d2fe;padding:1rem;border-radius:8px;margin-bottom:1rem">
      <div style="font-weight:700;color:#3730a3">✨ AI Semantic Search · Query: "${esc(data.query)}"</div>
      ${data.detected_category ? `<div style="font-size:0.85rem;color:#4f46e5;margin-top:0.25rem">Detected Intent Category: <strong>${esc(data.detected_category)}</strong></div>` : ''}
    </div>`;

    grid.innerHTML = headerHtml + data.results.map(s => `
      <div class="service-card">
        <div class="service-card-body">
          <div style="display:flex;justify-content:space-between;align-items:start">
            <span class="badge" style="background:#e0e7ff;color:#3730a3">${esc(s.category)}</span>
            <span style="font-size:0.75rem;background:#fef3c7;color:#92400e;padding:2px 6px;border-radius:4px;font-weight:600">Relevance: ${Math.round(s.relevance_score * 100)}%</span>
          </div>
          <h4 style="margin:0.75rem 0 0.5rem 0">${esc(s.title)}</h4>
          <p style="color:#555;font-size:0.85rem;line-height:1.4">${esc(s.description)}</p>
          <div style="margin-top:auto;padding-top:1rem;display:flex;justify-content:space-between;align-items:center">
            <div>
              <div style="font-size:1.15rem;font-weight:700;color:#0f1111">₹${s.price}</div>
              <div style="font-size:0.8rem;color:#666">📍 ${esc(s.city)} · ⭐ ${s.rating || 'New'}</div>
            </div>
            <button class="btn btn-amber" onclick="openBookingModal(${s.id}, '${escJs(s.title)}', ${s.price})">Book Now</button>
          </div>
        </div>
      </div>
    `).join('');
  } catch (err) {
    toast('AI Search service currently offline, using standard keyword search', true);
    doSearch();
  } finally {
    showSpinner(false);
  }
}

function toggleAiChat() {
  const win = document.getElementById('aiChatWindow');
  const isHidden = win.style.display === 'none' || win.style.display === '';
  win.style.display = isHidden ? 'flex' : 'none';
  if (isHidden) {
    document.getElementById('aiChatInput').focus();
  }
}

async function sendAiMessage() {
  const input = document.getElementById('aiChatInput');
  const msg = input.value.trim();
  if (!msg) return;

  const body = document.getElementById('aiChatBody');

  // Append user bubble
  const userBubble = document.createElement('div');
  userBubble.className = 'chat-bubble bubble-user';
  userBubble.innerText = msg;
  body.appendChild(userBubble);
  input.value = '';
  body.scrollTop = body.scrollHeight;

  // Append typing indicator
  const botBubble = document.createElement('div');
  botBubble.className = 'chat-bubble bubble-bot';
  botBubble.innerHTML = '<em>Thinking…</em>';
  body.appendChild(botBubble);
  body.scrollTop = body.scrollHeight;

  try {
    const token = sessionStorage.getItem('token') || '';
    const res = await fetch('/api/chat/message', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': 'Bearer ' + token } : {})
      },
      body: JSON.stringify({
        message: msg,
        conversation_history: aiChatHistory
      })
    });

    const data = await res.json();
    botBubble.innerText = data.reply;

    // Handle draft booking confirmation action
    if (data.action_required === 'CONFIRM_BOOKING' && data.draft_booking) {
      const confirmPill = document.createElement('div');
      confirmPill.style.marginTop = '0.5rem';
      confirmPill.innerHTML = `
        <div style="background:#fef3c7;border:1px solid #fde68a;padding:0.6rem;border-radius:6px;font-size:0.8rem">
          <strong>Draft Booking Confirmation:</strong><br>
          Service #${data.draft_booking.service_id} on ${data.draft_booking.booking_date}<br>
          <button class="btn btn-amber" style="margin-top:0.4rem;padding:0.2rem 0.6rem;font-size:0.75rem" onclick="confirmDraftBooking(${data.draft_booking.service_id}, '${escJs(data.draft_booking.booking_date)}')">Confirm & Proceed to Payment</button>
        </div>
      `;
      botBubble.appendChild(confirmPill);
    }

    aiChatHistory.push({ role: 'user', content: msg });
    aiChatHistory.push({ role: 'assistant', content: data.reply });
  } catch (err) {
    botBubble.innerText = "I'm having trouble communicating with the server. Please try again or browse services directly.";
  } finally {
    body.scrollTop = body.scrollHeight;
  }
}

function confirmDraftBooking(serviceId, date) {
  openBookingModal(serviceId, `Service #${serviceId}`, 499.0);
  const dateInput = document.getElementById('booking_date');
  if (dateInput) dateInput.value = date;
  toggleAiChat();
}
