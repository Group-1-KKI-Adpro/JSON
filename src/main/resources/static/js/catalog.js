(function () {
    const CART_PREFIX = "json_cart";

    let currentUser = null;
    let catalogItems = [];

    document.addEventListener("DOMContentLoaded", initCatalogPage);

    async function initCatalogPage() {
        if (document.body?.dataset?.page !== "catalog") return;

        if (!getCatalogToken()) {
            window.location.href = "/login";
            return;
        }

        setupCatalogForm();
        setupSearch();
        setupCartButtons();

        const refreshButton = document.getElementById("refreshCatalog");
        if (refreshButton) {
            refreshButton.addEventListener("click", async () => {
                await loadCatalogPage();
            });
        }

        await loadCatalogPage();
    }

    function getCatalogToken() {
        if (window.getToken) return window.getToken();
        return localStorage.getItem("json_token");
    }

    async function catalogFetch(url, options = {}) {
        if (window.authFetch) {
            return window.authFetch(url, options);
        }

        const headers = new Headers(options.headers || {});
        const token = getCatalogToken();

        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        return fetch(url, { ...options, headers });
    }

    async function requestJson(url, options = {}) {
        const response = await catalogFetch(url, options);

        if (!response.ok) {
            const message = await readError(response);
            throw new Error(message || `Request failed with status ${response.status}`);
        }

        if (response.status === 204) return null;

        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    async function readError(response) {
        try {
            const text = await response.text();
            if (!text) return "";

            try {
                const json = JSON.parse(text);
                return json.error || json.message || text;
            } catch {
                return text;
            }
        } catch {
            return "";
        }
    }

    async function loadCatalogPage() {
        setCatalogNotice("info", "Loading catalog...");

        try {
            currentUser = await requestJson("/api/auth/me");
            renderRoleAccess(currentUser);

            catalogItems = await requestJson("/api/catalog");
            renderCatalogItems(catalogItems || []);
            updateCartPreview();

            setCatalogNotice("", "");
        } catch (error) {
            setCatalogNotice("error", error.message || "Failed to load catalog.");
        }
    }

    function renderRoleAccess(user) {
        const accessBox = document.getElementById("catalogAccessBox");
        const managePanel = document.getElementById("catalogManagePanel");

        const isApprovedJastiper = user.role === "JASTIPER" && user.status === "ACTIVE";
        const isPending = user.status === "PENDING_VERIFICATION";
        const isBanned = user.status === "BANNED";

        if (isApprovedJastiper) {
            if (accessBox) {
                accessBox.innerHTML = `
                    <h2>Jastiper tools enabled</h2>
                    <p class="muted">
                        You are an active Jastiper. You can add catalog items without typing any Jastiper ID.
                    </p>
                    <a class="button full-action" href="/inventory">Open inventory</a>
                `;
            }
            managePanel?.classList.remove("hidden");
            return;
        }

        managePanel?.classList.add("hidden");

        if (accessBox) {
            if (isPending) {
                accessBox.innerHTML = `
                    <h2>KYC pending</h2>
                    <p class="muted">
                        Your Jastiper application is waiting for admin approval. The add-item form is disabled until approval.
                    </p>
                    <a class="btn-ghost full-action" href="/profile">Check profile status</a>
                `;
            } else if (isBanned) {
                accessBox.innerHTML = `
                    <h2>Account banned</h2>
                    <p class="muted">
                        This account cannot create catalog items. Contact an admin if this is unexpected.
                    </p>
                `;
            } else if (user.role === "ADMIN") {
                accessBox.innerHTML = `
                    <h2>Admin account</h2>
                    <p class="muted">
                        Admins monitor users and catalog data. Create catalog items using an approved Jastiper account.
                    </p>
                    <a class="btn-ghost full-action" href="/admin">Open admin dashboard</a>
                `;
            } else {
                accessBox.innerHTML = `
                    <h2>Apply as Jastiper first</h2>
                    <p class="muted">
                        Your current role is TITIPER. You can shop and add items to cart, but catalog creation requires KYC approval.
                    </p>
                    <a class="button full-action" href="/profile">Apply for KYC</a>
                `;
            }
        }
    }

    function setupCatalogForm() {
        const form = document.getElementById("catalogForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            if (!currentUser || currentUser.role !== "JASTIPER" || currentUser.status !== "ACTIVE") {
                setCatalogNotice("error", "Only approved active Jastipers can create catalog items.");
                return;
            }

            const payload = {
                name: getValue("catalogName"),
                description: getValue("catalogDescription"),
                price: Number(getValue("catalogPrice") || 0),
                stock: Number(getValue("catalogStock") || 0),
                origin: getValue("catalogOrigin"),
                purchaseDate: getValue("catalogPurchaseDate")
            };

            try {
                await requestJson("/api/catalog", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                form.reset();
                setCatalogNotice("success", "Catalog item created successfully.");
                await loadCatalogPage();
            } catch (error) {
                setCatalogNotice("error", error.message || "Failed to create catalog item.");
            }
        });
    }

    function getValue(id) {
        return document.getElementById(id)?.value?.trim() || "";
    }

    function setupSearch() {
        const input = document.getElementById("catalogSearch");
        if (!input) return;

        input.addEventListener("input", () => {
            renderCatalogItems(catalogItems);
        });
    }

    function renderCatalogItems(items) {
        const grid = document.getElementById("catalogGrid");
        const empty = document.getElementById("catalogEmpty");
        if (!grid) return;

        const query = document.getElementById("catalogSearch")?.value?.trim().toLowerCase() || "";
        const filtered = (items || []).filter((item) => {
            const haystack = [
                item.name,
                item.description,
                item.origin,
                item.jastiperId
            ].join(" ").toLowerCase();

            return !query || haystack.includes(query);
        });

        if (!filtered.length) {
            grid.innerHTML = "";
            empty?.classList.remove("hidden");
            return;
        }

        empty?.classList.add("hidden");

        grid.innerHTML = filtered.map((item) => `
            <article class="catalog-item-card">
                <div>
                    
                    <a class="pill profile-link-pill" href="/users?userId=${encodeURIComponent(item.jastiperId)}"> View Jastiper profile #${escapeCatalogHtml(item.jastiperId ?? "-")} </a>
                    <h3>${escapeCatalogHtml(item.name || "Unnamed item")}</h3>
                    <p class="muted">${escapeCatalogHtml(item.description || "No description provided.")}</p>
                </div>

                <div class="catalog-item-meta">
                    <span>${formatCatalogCurrency(item.price)}</span>
                    <span>Stock: ${escapeCatalogHtml(item.stock ?? 0)}</span>
                    <span>Origin: ${escapeCatalogHtml(item.origin || "-")}</span>
                    <span>Purchase: ${escapeCatalogHtml(item.purchaseDate || "-")}</span>
                </div>

                <div class="catalog-card-actions">
                    <label>
                        Qty
                        <input
                            class="catalog-qty-input"
                            type="number"
                            min="1"
                            max="${escapeCatalogHtml(item.stock ?? 1)}"
                            value="1"
                            data-qty-for="${escapeCatalogHtml(item.id)}"
                        />
                    </label>

                    <button
                        class="button"
                        type="button"
                        data-action="add-to-cart"
                        data-item-id="${escapeCatalogHtml(item.id)}"
                        ${Number(item.stock || 0) <= 0 ? "disabled" : ""}
                    >
                        ${Number(item.stock || 0) <= 0 ? "Out of stock" : "Add to cart"}
                    </button>
                </div>
            </article>
        `).join("");

        grid.querySelectorAll("[data-action='add-to-cart']").forEach((button) => {
            button.addEventListener("click", () => {
                addToCart(Number(button.dataset.itemId));
            });
        });
    }

    function addToCart(itemId) {
        const item = catalogItems.find((candidate) => Number(candidate.id) === Number(itemId));
        if (!item) {
            setCatalogNotice("error", "Catalog item not found.");
            return;
        }

        const qtyInput = document.querySelector(`[data-qty-for="${CSS.escape(String(itemId))}"]`);
        const qty = Math.max(1, Number(qtyInput?.value || 1));

        if (qty > Number(item.stock || 0)) {
            setCatalogNotice("error", "Quantity cannot exceed current stock.");
            return;
        }

        const cart = readCart();
        const existing = cart.find((cartItem) => Number(cartItem.catalogItemId) === Number(itemId));

        if (existing) {
            existing.qty += qty;
        } else {
            cart.push({
                catalogItemId: item.id,
                qty: qty,
                priceSnapshot: Number(item.price || 0),
                name: item.name,
                jastiperId: item.jastiperId
            });
        }

        localStorage.setItem(getCartKey(), JSON.stringify(cart));
        updateCartPreview();
        setCatalogNotice("success", "Item added to cart.");
    }

    function setupCartButtons() {
        const clearButton = document.getElementById("clearCatalogCart");
        if (!clearButton) return;

        clearButton.addEventListener("click", () => {
            localStorage.removeItem(getCartKey());
            updateCartPreview();
            setCatalogNotice("success", "Cart cleared.");
        });
    }

    function updateCartPreview() {
        const el = document.getElementById("catalogCartPreview");
        if (!el) return;

        const cart = readCart();
        const totalQty = cart.reduce((sum, item) => sum + Number(item.qty || 0), 0);
        const totalPrice = cart.reduce(
            (sum, item) => sum + Number(item.qty || 0) * Number(item.priceSnapshot || 0),
            0
        );

        el.innerHTML = `
            <strong>Cart items: ${escapeCatalogHtml(totalQty)}</strong>
            <span class="muted">Estimated total: ${formatCatalogCurrency(totalPrice)}</span>
        `;
    }

    function readCart() {
        try {
            return JSON.parse(localStorage.getItem(getCartKey()) || "[]");
        } catch {
            return [];
        }
    }

    function setCatalogNotice(type, message) {
        const el = document.getElementById("catalogNotice");
        if (!el) return;

        if (!message) {
            el.className = "hidden";
            el.textContent = "";
            return;
        }

        el.className = `notice ${type || "info"}`;
        el.textContent = message;
    }

    function formatCatalogCurrency(value) {
        if (window.formatCurrency) return window.formatCurrency(value);

        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "IDR",
            maximumFractionDigits: 0
        }).format(Number(value || 0));
    }

    function escapeCatalogHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function getCartKey() {
        if (!currentUser || !currentUser.id) {
            return CART_PREFIX + "_guest";
        }

        return CART_PREFIX + "_" + currentUser.id;
    }
})();
