
const TOKEN_KEY = "json_token";
const PROFILE_KEY = "json_profile";
const PUBLIC_PAGES = new Set(["home", "login", "register"]);
const PROTECTED_PAGES = new Set(["catalog", "catalog-add", "orders", "profile", "wallet", "vouchers", "transactions"]);

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(PROFILE_KEY);
}

function getStoredProfile() {
  try {
    const raw = localStorage.getItem(PROFILE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function setStoredProfile(profile) {
  if (!profile) {
    localStorage.removeItem(PROFILE_KEY);
    return;
  }
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
}

function showToken(elId = "tokenOut") {
  const el = document.getElementById(elId);
  if (el) el.textContent = getToken() ?? "(none)";
}

function formatCurrency(n) {
  const value = Number(n ?? 0);
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0
  }).format(Number.isFinite(value) ? value : 0);
}

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function authFetch(url, options = {}) {
  const token = getToken();
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  return fetch(url, { ...options, headers });
}

function decodeJwtPayload(token) {
  try {
    const parts = String(token).split(".");
    if (parts.length < 2) return null;
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = payload + "=".repeat((4 - (payload.length % 4)) % 4);
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

function getDisplayName(profile) {
  if (!profile) return "Guest";
  return profile.username || profile.fullName || profile.email || "Signed in user";
}

function getSupportText(profile) {
  if (!profile) return "Sign in to unlock the protected pages.";
  const parts = [];
  if (profile.role) parts.push(profile.role);
  if (profile.status) parts.push(profile.status);
  return parts.join(" · ") || "Logged in";
}

function setNotice(id, type, message) {
  const el = document.getElementById(id);
  if (!el) return;
  if (!message) {
    el.className = "hidden";
    el.textContent = "";
    return;
  }
  el.className = `notice ${type || "info"}`;
  el.textContent = message;
}

function activePath() {
  return window.location.pathname.replace(/\/$/, "") || "/";
}

function setActiveNavLink() {
  const path = activePath();
  document.querySelectorAll(".nav-links a").forEach((link) => {
    try {
      const href = new URL(link.getAttribute("href"), window.location.origin).pathname.replace(/\/$/, "") || "/";
      link.classList.toggle("active", href === path);
    } catch {
      // ignore invalid URLs
    }
  });
}

function getAccountWidgetElement() {
  return document.getElementById("accountWidget");
}

function renderAccountWidget(profile) {
  const widget = getAccountWidgetElement();
  if (!widget) return;

  if (!getToken()) {
    widget.innerHTML = `
      <a class="btn-ghost" href="/login">Log in</a>
      <a class="button" href="/register">Register</a>
    `;
    return;
  }

  const identity = profile || getStoredProfile() || {};
  const title = escapeHtml(getDisplayName(identity));
  const subtitle = escapeHtml(getSupportText(identity));
  const email = identity.email ? `<small>${escapeHtml(identity.email)}</small>` : "<small>Signed in</small>";

  widget.innerHTML = `
    <div class="user-chip" title="Signed in account">
      <span class="avatar-dot"></span>
      <div>
        <strong>${title}</strong>
        ${email}
        <small>${subtitle}</small>
      </div>
    </div>
    <button id="logoutButton" class="btn-ghost" type="button">Log out</button>
  `;

  const logoutButton = document.getElementById("logoutButton");
  if (logoutButton) {
    logoutButton.addEventListener("click", () => {
      clearToken();
      renderAccountWidget(null);
      const page = document.body.dataset.page;
      if (PROTECTED_PAGES.has(page)) {
        window.location.href = "/login";
      } else {
        window.location.href = "/";
      }
    });
  }
}

async function loadCurrentUser() {
  const token = getToken();
  if (!token) {
    renderAccountWidget(null);
    return null;
  }

  const cached = getStoredProfile();
  if (cached) {
    renderAccountWidget(cached);
  }

  try {
    const response = await authFetch("/api/auth/me");
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const me = await response.json();
    setStoredProfile(me);
    renderAccountWidget(me);
    return me;
  } catch {
    const payload = decodeJwtPayload(token);
    if (payload) {
      const fallback = {
        email: payload.sub || payload.email || payload.username || "signed-in user",
        username: payload.username || payload.sub || payload.email || "Signed in",
        fullName: payload.fullName || payload.name || payload.username || "Signed in user"
      };
      renderAccountWidget(fallback);
      return fallback;
    }

    clearToken();
    renderAccountWidget(null);
    return null;
  }
}

function ensureProtectedPage() {
  const page = document.body.dataset.page;
  if (PROTECTED_PAGES.has(page) && !getToken()) {
    window.location.replace("/login");
    return true;
  }
  return false;
}

function renderCards(el, cards) {
  if (!el || el.children.length || el.innerHTML.trim()) return;
  el.innerHTML = cards.map((card) => `
    <article class="feature-tile">
      <div class="feature-icon">${escapeHtml(card.icon || "✦")}</div>
      <h3>${escapeHtml(card.title)}</h3>
      <p>${escapeHtml(card.text)}</p>
    </article>
  `).join("");
}

function renderStats(el, items) {
  if (!el || el.children.length || el.innerHTML.trim()) return;
  el.innerHTML = items.map((item) => `
    <article class="mini-stat">
      <strong>${escapeHtml(item.value)}</strong>
      <small>${escapeHtml(item.label)}</small>
      <small>${escapeHtml(item.text || "")}</small>
    </article>
  `).join("");
}

function renderEmptyState(el, title, text, actionHtml = "") {
  if (!el || el.children.length || el.innerHTML.trim()) return;
  el.innerHTML = `
    <div class="empty-state">
      <strong>${escapeHtml(title)}</strong>
      <p style="margin:0 0 14px; line-height:1.6;">${escapeHtml(text)}</p>
      ${actionHtml}
    </div>
  `;
}

function getCatalogItemsCache() {
  if (!window.__catalogItemsCache) {
    window.__catalogItemsCache = [];
  }
  return window.__catalogItemsCache;
}

function setCatalogItemsCache(items) {
  window.__catalogItemsCache = Array.isArray(items) ? items : [];
}

function catalogSearchText(item) {
  return [
    item?.name,
    item?.description,
    item?.origin,
    item?.jastiperId != null ? `jastiper ${item.jastiperId}` : "",
    item?.price != null ? `price ${item.price}` : "",
    item?.stock != null ? `stock ${item.stock}` : ""
  ].join(" ").toLowerCase();
}

function renderCatalogStatsFromItems(items) {
  const totalItems = items.length;
  const totalStock = items.reduce((sum, item) => sum + Number(item?.stock || 0), 0);
  const avgPrice = totalItems
    ? Math.round(items.reduce((sum, item) => sum + Number(item?.price || 0), 0) / totalItems)
    : 0;
  const uniqueJastipers = new Set(items.map((item) => item?.jastiperId).filter((value) => value !== undefined && value !== null)).size;

  renderStats(document.getElementById("catalogStats"), [
    { value: `${totalItems}`, label: "Items in catalog", text: "Loaded directly from the backend" },
    { value: `${totalStock}`, label: "Total stock", text: "Across all visible products" },
    { value: formatCurrency(avgPrice), label: "Average price", text: "Simple snapshot for browsing" },
    { value: `${uniqueJastipers}`, label: "Jastiper owners", text: "Unique sellers in this catalog" }
  ]);
}

function renderCatalogEmptyState() {
  const empty = document.getElementById("catalogEmpty");
  const grid = document.getElementById("catalogGrid");
  if (grid) grid.classList.add("hidden");
  if (!empty) return;

  empty.classList.remove("hidden");
  empty.innerHTML = `
    <div class="empty-state">
      <strong>No catalog items yet</strong>
      <p style="margin:0 0 14px; line-height:1.6;">
        Start by adding the first item on a separate page, then come back here to browse and search.
      </p>
      <a class="button" href="/catalog/new">Add the first item</a>
    </div>
  `;
}

function renderCatalogCards(items) {
  const grid = document.getElementById("catalogGrid");
  const empty = document.getElementById("catalogEmpty");
  if (!grid) return;

  if (!items.length) {
    renderCatalogEmptyState();
    if (empty) {
      empty.classList.remove("hidden");
    }
    return;
  }

  if (empty) empty.classList.add("hidden");
  grid.classList.remove("hidden");

  grid.innerHTML = items.map((item) => {
    const searchText = escapeHtml(catalogSearchText(item));
    return `
      <article class="card catalog-card" data-search="${searchText}">
        <div class="card-inner">
          <div class="card-top">
            <div>
              <h3 class="card-title">${escapeHtml(item.name || "Unnamed item")}</h3>
              <div class="card-subtitle">Jastiper #${escapeHtml(item.jastiperId ?? "-")} · ${escapeHtml(item.origin || "Unknown origin")}</div>
            </div>
            <span class="badge">${escapeHtml(String(item.stock ?? 0))} stock</span>
          </div>

          <p class="muted" style="margin:0; line-height:1.65;">
            ${escapeHtml(item.description || "No description provided.")}
          </p>

          <div class="badges">
            <span class="badge gray">${escapeHtml(formatCurrency(item.price || 0))}</span>
            <span class="badge gray">Purchased: ${escapeHtml(item.purchaseDate || "-")}</span>
            <span class="badge gray">Updated: ${escapeHtml(formatTimestamp(item.updatedAt))}</span>
          </div>

          <div class="card-actions">
            <span class="pill">Item ID ${escapeHtml(item.id ?? "-")}</span>
            <span class="pill">Created ${escapeHtml(formatTimestamp(item.createdAt))}</span>
          </div>
        </div>
      </article>
    `;
  }).join("");
}

async function loadCatalogPageData() {
  const noticeId = "catalogNotice";
  const grid = document.getElementById("catalogGrid");
  const empty = document.getElementById("catalogEmpty");

  if (!grid || !empty) return;

  try {
    const response = await authFetch("/api/catalog");
    if (!response.ok) {
      throw new Error("Failed to load catalog items");
    }

    const items = await response.json();
    const normalized = Array.isArray(items) ? items : [];
    setCatalogItemsCache(normalized);
    renderCatalogStatsFromItems(normalized);

    if (!normalized.length) {
      renderCatalogEmptyState();
      setNotice(noticeId, null, "");
      return;
    }

    empty.classList.add("hidden");
    grid.classList.remove("hidden");
    renderCatalogCards(normalized);
    setNotice(noticeId, null, "");
  } catch (err) {
    renderCatalogEmptyState();
    setNotice(noticeId, "error", err.message || "Failed to load catalog");
  }
}

function applyCatalogFilter() {
  const input = document.getElementById("catalogSearch");
  const noticeId = "catalogNotice";
  const query = (input?.value || "").trim().toLowerCase();
  const cards = document.querySelectorAll("#catalogGrid .catalog-card");
  let visible = 0;

  cards.forEach((card) => {
    const haystack = card.dataset.search || card.textContent.toLowerCase();
    const match = !query || haystack.includes(query);
    card.style.display = match ? "" : "none";
    if (match) visible += 1;
  });

  const grid = document.getElementById("catalogGrid");
  const empty = document.getElementById("catalogEmpty");
  const hasItems = getCatalogItemsCache().length > 0;

  if (!hasItems) {
    if (empty) empty.classList.remove("hidden");
    if (grid) grid.classList.add("hidden");
    setNotice(noticeId, null, "");
    return;
  }

  if (visible === 0) {
    if (empty) {
      empty.classList.remove("hidden");
      empty.innerHTML = `
        <div class="empty-state">
          <strong>No matching items</strong>
          <p style="margin:0 0 14px; line-height:1.6;">
            Try a different keyword or clear the search box to see every item again.
          </p>
          <button class="btn-ghost" type="button" id="catalogClearSearch">Clear search</button>
        </div>
      `;
      const clearButton = document.getElementById("catalogClearSearch");
      clearButton?.addEventListener("click", () => {
        if (input) input.value = "";
        applyCatalogFilter();
      });
    }
    if (grid) grid.classList.add("hidden");
    setNotice(noticeId, "info", "No items match your search.");
  } else {
    if (empty) empty.classList.add("hidden");
    if (grid) grid.classList.remove("hidden");
    setNotice(noticeId, null, "");
  }
}

function setupCatalogSearch() {
  const input = document.getElementById("catalogSearch");
  if (!input) return;

  input.addEventListener("input", applyCatalogFilter);
}

function setupSimpleRefresh(buttonId) {

  const button = document.getElementById(buttonId);
  if (!button) return;
  button.addEventListener("click", () => window.location.reload());
}

function formatTimestamp(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function statusClass(status) {
  const value = String(status || "").toUpperCase();
  if (value === "SUCCESS") return "badge";
  if (value === "PENDING") return "badge alt";
  if (value === "FAILED") return "badge gray";
  return "badge gray";
}

function renderWalletBalance(balance) {
  const el = document.getElementById("walletBalance");
  if (!el) return;

  el.innerHTML = `
    <div class="balance-card">
      <strong>Current balance</strong>
      <div class="balance-amount">${formatCurrency(balance ?? 0)}</div>
      <p class="muted" style="margin:10px 0 0;">Available for checkout, refunds, and withdrawals.</p>
    </div>
  `;
}

function renderWalletTransactions(items) {
  const el = document.getElementById("walletTransactions");
  if (!el) return;

  if (!items.length) {
    el.innerHTML = `
      <div class="empty-state">
        <strong>No transactions yet</strong>
        <p style="margin:0 0 14px; line-height:1.6;">Top ups, deductions, refunds, and withdrawals will appear here in order.</p>
        <a class="btn-ghost" href="/transactions">Open transactions</a>
      </div>
    `;
    return;
  }

  el.innerHTML = items.map((tx) => `
    <article class="card" style="margin-bottom:12px;">
      <div class="card-inner">
        <div class="card-top">
          <div>
            <h3 class="card-title">${escapeHtml(tx.type || "TRANSACTION")}</h3>
            <div class="card-subtitle">${escapeHtml(formatTimestamp(tx.timestamp))}</div>
          </div>
          <span class="${statusClass(tx.status)}">${escapeHtml(tx.status || "UNKNOWN")}</span>
        </div>
        <p class="muted" style="margin:10px 0 8px;">${escapeHtml(tx.description || "No description")}</p>
        <div class="badges">
          <span class="badge gray">${escapeHtml(formatCurrency(tx.amount))}</span>
          <span class="badge gray">Ref: ${escapeHtml(tx.referenceId || "-")}</span>
        </div>
      </div>
    </article>
  `).join("");
}

function renderTransactionsTable(items) {
  const tbody = document.getElementById("transactionsTableBody");
  if (!tbody) return;

  if (!items.length) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6">
          <div class="empty-state">
            <strong>No transaction data</strong>
            <p style="margin:0; line-height:1.6;">Try changing the filter or create a wallet transaction first.</p>
          </div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = items.map((tx) => `
    <tr>
      <td>${escapeHtml(formatTimestamp(tx.timestamp))}</td>
      <td>${escapeHtml(tx.type || "-")}</td>
      <td>${escapeHtml(formatCurrency(tx.amount))}</td>
      <td><span class="${statusClass(tx.status)}">${escapeHtml(tx.status || "UNKNOWN")}</span></td>
      <td>${escapeHtml(tx.description || "-")}</td>
      <td>${escapeHtml(tx.referenceId || "-")}</td>
    </tr>
  `).join("");
}

async function fetchWalletTransactions(limit = 10) {
  const response = await authFetch(`/api/wallet/transactions?limit=${limit}`);
  if (!response.ok) {
    throw new Error(`Failed to load transactions (${response.status})`);
  }
  return response.json();
}

async function loadWalletPageData() {
  const balanceResponse = await authFetch("/api/wallet/balance");
  if (!balanceResponse.ok) {
    throw new Error(`Failed to load balance (${balanceResponse.status})`);
  }
  const balance = await balanceResponse.json();
  renderWalletBalance(balance.balance);

  const transactions = await fetchWalletTransactions(5);
  renderWalletTransactions(transactions);
}

async function loadTransactionsPageData() {
  const filter = document.getElementById("transactionFilter")?.value || "ALL";
  const transactions = await fetchWalletTransactions(100);
  const filtered = filter === "ALL"
    ? transactions
    : transactions.filter((tx) => String(tx.type).toUpperCase() === filter);

  renderTransactionsTable(filtered);
}

function setupWalletForms() {
  const topupForm = document.getElementById("topupForm");
  const withdrawForm = document.getElementById("withdrawForm");

  if (topupForm) {
    topupForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const amount = Number(document.getElementById("topupAmount")?.value || 0);
      const description = document.getElementById("topupDescription")?.value?.trim() || "";

      try {
        const response = await authFetch("/api/wallet/topup", {
          method: "POST",
          body: JSON.stringify({ amount, description })
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || "Top up failed");
        }

        const result = await response.json();
        renderWalletBalance(result.balance?.balance ?? 0);
        await loadWalletPageData();
        topupForm.reset();
        setNotice("walletNotice", "success", "Top up successful.");
      } catch (err) {
        setNotice("walletNotice", "error", err.message || "Top up failed");
      }
    });
  }

  if (withdrawForm) {
    withdrawForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const amount = Number(document.getElementById("withdrawAmount")?.value || 0);
      const description = document.getElementById("withdrawDescription")?.value?.trim() || "";

      try {
        const response = await authFetch("/api/wallet/withdraw", {
          method: "POST",
          body: JSON.stringify({ amount, description })
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || "Withdrawal request failed");
        }

        await loadWalletPageData();
        withdrawForm.reset();
        setNotice("walletNotice", "success", "Withdrawal request submitted.");
      } catch (err) {
        setNotice("walletNotice", "error", err.message || "Withdrawal request failed");
      }
    });
  }
}

function setupTransactionsPage() {
  const filter = document.getElementById("transactionFilter");
  const refresh = document.getElementById("refreshTransactions");

  if (filter) {
    filter.addEventListener("change", async () => {
      try {
        await loadTransactionsPageData();
        setNotice("transactionsNotice", null, "");
      } catch (err) {
        setNotice("transactionsNotice", "error", err.message || "Failed to load transactions");
      }
    });
  }

  if (refresh) {
    refresh.addEventListener("click", async () => {
      try {
        await loadTransactionsPageData();
        setNotice("transactionsNotice", "success", "Transactions refreshed.");
      } catch (err) {
        setNotice("transactionsNotice", "error", err.message || "Failed to refresh transactions");
      }
    });
  }
}

function initPlaceholders() {
  const page = document.body.dataset.page;

  if (page === "home") {
    renderCards(document.getElementById("highlights"), [
      { icon: "🔒", title: "Login first", text: "Shopping pages are gated so the app opens with a proper sign-in flow." },
      { icon: "🛒", title: "Catalog & items", text: "Search, review, and add catalog items in a cleaner layout." },
      { icon: "📦", title: "Orders page", text: "Checkout and order history are separated into one focused page." },
      { icon: "💳", title: "Wallet & vouchers", text: "Support services stay available as matching companion pages." }
    ]);

    const heroMeta = document.getElementById("heroMeta");
    if (heroMeta && !heroMeta.children.length && !heroMeta.innerHTML.trim()) {
      heroMeta.innerHTML = [
        '<span class="badge">✓ Protected</span>',
        '<span class="badge">✦ Consistent</span>',
        '<span class="badge">⌕ Search-ready</span>'
      ].join('');
    }
  }

  if (page === "catalog") {
    renderStats(document.getElementById("catalogStats"), [
      { value: "Search", label: "Browse quickly", text: "Filter by name, origin, or Jastiper." },
      { value: "Items", label: "Backend connected", text: "Catalog data comes straight from /api/catalog." },
      { value: "Add", label: "Separate page", text: "Create new items on their own screen." },
      { value: "Stock", label: "Inventory ready", text: "See stock and price at a glance." }
    ]);
  }

  if (page === "catalog-add") {
    renderStats(document.getElementById("catalogFormStats"), [
      { value: "New item", label: "Dedicated form", text: "Keep adding items out of the browse view." },
      { value: "Save", label: "Backend POST", text: "Submits directly to /api/catalog." },
      { value: "Redirect", label: "Back to browse", text: "Returns you to the catalogue after save." }
    ]);
  }

  if (page === "orders") {
    renderStats(document.getElementById("orderSummary"), [
      { value: "Login", label: "Protected checkout", text: "Only signed-in users can place orders." },
      { value: "Cart", label: "Prepared flow", text: "Review selected items before paying." },
      { value: "History", label: "Tracked status", text: "Monitor your order lifecycle in one place." },
      { value: "Cancel", label: "Safer checkout", text: "Active orders stay manageable." }
    ]);

    renderEmptyState(
      document.getElementById("orderCart"),
      "Your cart is empty",
      "Browse the catalog to add items before placing an order."
    );
    renderEmptyState(
      document.getElementById("orderHistory"),
      "No orders yet",
      "Your previous purchases will appear here once checkout happens."
    );
  }

  if (page === "profile") {
    renderStats(document.getElementById("profileCard"), [
      { value: "Username", label: "Shown in header", text: "Display your account name after login." },
      { value: "Full name", label: "Profile details", text: "Keep your public name readable." },
      { value: "Status", label: "Account state", text: "See your current account status here." }
    ]);
  }

  if (page === "wallet") {
    renderStats(document.getElementById("walletMeta"), [
      { value: "Balance", label: "Track funds", text: "See your wallet state at a glance." },
      { value: "Top up", label: "Quick add", text: "Deposit funds in a simple card." },
      { value: "Withdraw", label: "Money out", text: "Request a withdrawal cleanly." }
    ]);

    const balance = document.getElementById("walletBalance");
    if (balance && !balance.children.length && !balance.innerHTML.trim()) {
      balance.innerHTML = `
        <div class="balance-card">
          <strong>Current balance</strong>
          <div class="balance-amount">${formatCurrency(0)}</div>
          <p class="muted" style="margin:10px 0 0;">Wallet data will appear here when the backend returns a balance.</p>
        </div>
      `;
    }

    renderEmptyState(
      document.getElementById("walletTransactions"),
      "No transactions yet",
      "Top ups, deductions, refunds, and withdrawals will appear here in order.",
      '<a class="btn-ghost" href="/transactions">Open transactions</a>'
    );
  }

  if (page === "vouchers") {
    renderEmptyState(
      document.getElementById("voucherList"),
      "No vouchers available right now",
      "This section is ready for voucher cards, validation, and admin creation when the service is connected."
    );

    const result = document.getElementById("voucherValidateResult");
    if (result && !result.children.length && !result.innerHTML.trim()) {
      result.innerHTML = `
        <div class="empty-state">
          <strong>Validate a voucher</strong>
          <p style="margin:0; line-height:1.6;">Enter a voucher code and order total to preview the discount result.</p>
        </div>
      `;
    }
  }

  if (page === "transactions") {
    const tbody = document.getElementById("transactionsTableBody");
    if (tbody && !tbody.children.length) {
      tbody.innerHTML = `
        <tr>
          <td colspan="6">
            <div class="empty-state">
              <strong>No transaction data yet</strong>
              <p style="margin:0; line-height:1.6;">Once wallet actions happen, the table will show date, type, status, description, and reference.</p>
            </div>
          </td>
        </tr>
      `;
    }
  }
}

async function loadVouchersPageData() {
  const listEl = document.getElementById("voucherList");
  if (!listEl) return;

  try {
    const res = await authFetch("/api/vouchers");
    if (!res.ok) throw new Error("Failed to fetch active vouchers");
    const vouchers = await res.json();
    
    if (!vouchers || vouchers.length === 0) {
      renderEmptyState(listEl, "No active vouchers", "Check back later for new promos.");
      return;
    }
    
    listEl.innerHTML = vouchers.map(v => `
      <article class="card">
        <div class="card-inner">
          <div class="card-top">
            <h3 class="card-title">${escapeHtml(v.code)}</h3>
            <span class="badge alt">${escapeHtml(v.discountType)}</span>
          </div>
          <p class="muted" style="margin: 8px 0;">Discount Value: ${v.discountType === 'FLAT' ? formatCurrency(v.discountValue) : v.discountValue + '%'}</p>
          <div class="badges">
            <span class="badge gray">Quota: ${v.quota}</span>
            <span class="badge gray">Valid till: ${formatTimestamp(v.endAt)}</span>
          </div>
        </div>
      </article>
    `).join("");
  } catch (err) {
    listEl.innerHTML = `<div class="notice error">Failed to load vouchers.</div>`;
  }
}

function setupVoucherForms() {
  const validateForm = document.getElementById("voucherValidateForm");
  const createForm = document.getElementById("voucherCreateForm");

  if (validateForm) {
    validateForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const code = document.getElementById("validateCode").value.trim();
      const orderTotal = Number(document.getElementById("validateTotal").value);
      const resultDiv = document.getElementById("voucherValidateResult");

      try {
        const res = await authFetch("/api/vouchers/validate", {
          method: "POST",
          body: JSON.stringify({ code, orderTotal })
        });
        
        if (!res.ok) {
          const text = await res.text();
          throw new Error(text || "Voucher validation failed");
        }
        
        const data = await res.json();
        resultDiv.innerHTML = `
          <div class="notice success" style="margin-top: 10px;">
            <strong>Valid!</strong> Discount applied: ${formatCurrency(data.discountApplied)}<br>
            <strong>Final Total:</strong> ${formatCurrency(data.finalTotal)}
          </div>
        `;
      } catch (err) {
        resultDiv.innerHTML = `<div class="notice error" style="margin-top: 10px;">${escapeHtml(err.message)}</div>`;
      }
    });
  }

  if (createForm) {
    createForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const code = document.getElementById("createCode").value.trim();
      const quota = Number(document.getElementById("createQuota").value);
      const startAt = document.getElementById("createStartAt").value;
      const endAt = document.getElementById("createEndAt").value;
      const discountType = document.getElementById("createDiscountType").value;
      const discountValue = Number(document.getElementById("createDiscountValue").value);
      const terms = document.getElementById("createTerms").value.trim();

      try {
        const res = await authFetch("/api/admin/vouchers", {
          method: "POST",
          body: JSON.stringify({ code, quota, startAt, endAt, discountType, discountValue, terms })
        });
        
        if (!res.ok) {
          const text = await res.text();
          throw new Error(text || "Failed to create voucher");
        }
        
        setNotice("voucherNotice", "success", "Voucher created successfully!");
        createForm.reset();
        await loadVouchersPageData();
      } catch (err) {
        setNotice("voucherNotice", "error", err.message);
      }
    });
  }
}


function setupOrderCheckout() {
  const orderForm = document.getElementById("orderForm");
  
  if (orderForm) {
    orderForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      
      const shippingAddress = document.getElementById("shippingAddress").value.trim();
      const voucherCode = document.getElementById("voucherCode").value.trim() || null;
      
      const cartItems = JSON.parse(localStorage.getItem("json_cart") || "[]");
      
      if (cartItems.length === 0) {
        setNotice("orderNotice", "error", "Your cart is empty. Please add items from the catalog.");
        return;
      }

      const orderRequest = {
        shippingAddress: shippingAddress,
        items: cartItems,
        voucherCode: voucherCode
      };

      try {
        const res = await authFetch("/api/orders", {
          method: "POST",
          body: JSON.stringify(orderRequest)
        });

        if (!res.ok) {
          const text = await res.text();
          throw new Error(text || "Failed to place order.");
        }

        setNotice("orderNotice", "success", "Order placed successfully! Voucher applied if valid.");
        localStorage.removeItem("json_cart"); // Clear cart
        orderForm.reset();
        
      } catch (err) {
        setNotice("orderNotice", "error", err.message);
      }
    });
  }
}

function setupPageInteractions() {
  const page = document.body.dataset.page;

  if (page === "catalog") {
    setupCatalogSearch();
  }

  if (page === "catalog-add") {
    setupCatalogCreateForm();
  }

  if (page === "transactions") {
    setupTransactionsPage();
  }

  if (page === "wallet") {
    setupWalletForms();
  }

  if (page === "vouchers") {
    setupVoucherForms();
  }

  if (page === "orders") {
    setupOrderCheckout();
  }
}

async function initApp() {
  setActiveNavLink();

  if (ensureProtectedPage()) {
    return;
  }

  initPlaceholders();
  setupPageInteractions();
  setupAuthForms();
  showToken();
  const currentUser = await loadCurrentUser();

  const page = document.body.dataset.page;
  if (page === "catalog") {
    try {
      await loadCatalogPageData();
    } catch (err) {
      setNotice("catalogNotice", "error", err.message || "Failed to load catalog items");
    }
  }

  if (page === "wallet") {
    try {
      await loadWalletPageData();
      setNotice("walletNotice", null, "");
    } catch (err) {
      setNotice("walletNotice", "error", err.message || "Failed to load wallet data");
    }
  }

  if (page === "transactions") {
    try {
      await loadTransactionsPageData();
      setNotice("transactionsNotice", null, "");
    } catch (err) {
      setNotice("transactionsNotice", "error", err.message || "Failed to load transactions");
    }
  }

  if (page === "vouchers") {
    const adminSection = document.getElementById("adminVoucherSection");
    if (adminSection && currentUser && currentUser.role && currentUser.role.includes("ADMIN")) {
      adminSection.classList.remove("hidden");
    }
    
    try {
      await loadVouchersPageData();
      setNotice("voucherNotice", null, "");
    } catch (err) {
      setNotice("voucherNotice", "error", err.message || "Failed to load active vouchers");
    }
  }
}

function setupAuthForms() {
  const loginForm = document.getElementById("loginForm");

  if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const email = document.getElementById("loginEmail")?.value?.trim();
      const password = document.getElementById("loginPassword")?.value;

      try {
        const response = await fetch("/api/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            email,
            password
          })
        });

        if (!response.ok) {
          throw new Error("Invalid email or password");
        }

        const data = await response.json();

        setToken(data.token);

        setNotice("authNotice", "success", "Login successful! Redirecting...");

        setTimeout(() => {
          window.location.href = "/catalog";
        }, 1000);

      } catch (err) {
        setNotice(
          "authNotice",
          "error",
          err.message || "Login failed"
        );
      }
    });
  }

  const registerForm = document.getElementById("registerForm");

  if (registerForm) {
    registerForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const fullName = document.getElementById("registerFullName")?.value?.trim();
      const email = document.getElementById("registerEmail")?.value?.trim();
      const password = document.getElementById("registerPassword")?.value;

      try {
        const response = await fetch("/api/auth/register", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            fullName,
            email,
            password
          })
        });

        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || "Registration failed");
        }

        setNotice(
          "authNotice",
          "success",
          "Registration successful! Redirecting to login..."
        );

        setTimeout(() => {
          window.location.href = "/login";
        }, 1200);

      } catch (err) {
        setNotice(
          "authNotice",
          "error",
          err.message || "Registration failed"
        );
      }
    });
  }
}



function setupCatalogCreateForm() {
  const form = document.getElementById("catalogCreateForm");
  if (!form) return;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
      jastiperId: Number(document.getElementById("catalogJastiperId")?.value),
      name: document.getElementById("catalogName")?.value?.trim(),
      description: document.getElementById("catalogDescription")?.value?.trim() || "",
      price: Number(document.getElementById("catalogPrice")?.value),
      stock: Number(document.getElementById("catalogStock")?.value),
      origin: document.getElementById("catalogOrigin")?.value?.trim() || "",
      purchaseDate: document.getElementById("catalogPurchaseDate")?.value || null
    };

    try {
      const response = await authFetch("/api/catalog", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Failed to save catalog item");
      }

      setNotice("catalogFormNotice", "success", "Item saved successfully. Redirecting back to the catalogue...");
      form.reset();
      setTimeout(() => {
        window.location.href = "/catalog";
      }, 900);
    } catch (err) {
      setNotice("catalogFormNotice", "error", err.message || "Failed to save catalog item");
    }
  });
}

window.addEventListener("DOMContentLoaded", initApp);
window.getToken = getToken;
window.setToken = setToken;
window.clearToken = clearToken;
window.authFetch = authFetch;
window.formatCurrency = formatCurrency;
window.escapeHtml = escapeHtml;
