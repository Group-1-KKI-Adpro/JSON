(function () {
    const TOKEN_KEY = "json_token";
    const CART_PREFIX = "json_cart";
    const ORDER_FLOW = ["PAID", "PURCHASED", "SHIPPED", "COMPLETED"];

    let currentUser = null;

    document.addEventListener("DOMContentLoaded", initOrdersPage);

    async function initOrdersPage() {
        console.log("orders.js loaded");

        if (document.body?.dataset?.page !== "orders") return;

        if (!getToken()) {
            window.location.href = "/login";
            return;
        }

        setupCheckoutForm();
        setupCartActions();

        const refreshButton = document.getElementById("refreshOrders");
        if (refreshButton) {
            refreshButton.addEventListener("click", loadOrdersPage);
        }

        await loadOrdersPage();
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function getCartKey() {
        if (!currentUser || !currentUser.id) {
            return CART_PREFIX + "_guest";
        }

        return CART_PREFIX + "_" + currentUser.id;
    }

    async function authFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        headers.set("Authorization", "Bearer " + getToken());

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        return fetch(url, { ...options, headers });
    }

    async function requestJson(url, options = {}) {
        const response = await authFetch(url, options);

        if (!response.ok) {
            const message = await readError(response);
            throw new Error(message || "Request failed with status " + response.status);
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

    async function loadOrdersPage() {
        setNotice("info", "Loading orders...");

        try {
            currentUser = await requestJson("/api/auth/me");

            renderRoleCard(currentUser);
            renderRoleSections(currentUser);
            renderCart();

            await loadBuyerOrders();

            if (currentUser.role === "JASTIPER") {
                await loadJastiperOrders();
            }

            setNotice("", "");
        } catch (error) {
            setNotice("error", error.message || "Failed to load orders.");
        }
    }

    function renderRoleCard(user) {
        const roleCard = document.getElementById("orderRoleCard");
        const accountWidget = document.getElementById("orderAccountWidget");

        if (accountWidget) {
            accountWidget.innerHTML = `
                <span>${escapeHtml(user.fullName || user.username || user.email || "User")}</span>
                <button class="btn ghost small" type="button" id="logoutButton">Logout</button>
            `;

            document.getElementById("logoutButton")?.addEventListener("click", () => {
                localStorage.removeItem(TOKEN_KEY);
                localStorage.removeItem("json_profile");
                window.location.href = "/login";
            });
        }

        if (!roleCard) return;

        roleCard.innerHTML = `
            <span class="pill">Signed in</span>
            <h2>${escapeHtml(user.fullName || user.username || "User")}</h2>
            <p class="muted">${escapeHtml(user.email || "-")}</p>
            <div class="order-role-row">
                <span class="order-role-pill role-${escapeHtml(String(user.role || "").toLowerCase())}">
                    ${escapeHtml(user.role || "-")}
                </span>
                <span class="order-role-pill status-${escapeHtml(String(user.status || "").toLowerCase())}">
                    ${escapeHtml(user.status || "-")}
                </span>
            </div>
        `;
    }

    function renderRoleSections(user) {
        document.getElementById("buyerSection")?.classList.remove("hidden");
        document.getElementById("jastiperSection")?.classList.add("hidden");
        document.getElementById("adminOrderInfo")?.classList.add("hidden");

        if (user.role === "JASTIPER") {
            document.getElementById("jastiperSection")?.classList.remove("hidden");
        }

        if (user.role === "ADMIN") {
            document.getElementById("adminOrderInfo")?.classList.remove("hidden");
        }
    }

    function readCart() {
        try {
            const parsed = JSON.parse(localStorage.getItem(getCartKey()) || "[]");
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    }

    function writeCart(cart) {
        localStorage.setItem(getCartKey(), JSON.stringify(cart));
    }

    function clearCart() {
        localStorage.removeItem(getCartKey());
    }

    function renderCart() {
        const cart = readCart();
        const cartEl = document.getElementById("orderCart");
        const placeButton = document.getElementById("placeOrderButton");

        if (!cartEl) return;

        if (!cart.length) {
            cartEl.innerHTML = `
                <div class="empty-state">
                    <h3>Your cart is empty</h3>
                    <p class="muted">Browse the catalog and add items before checkout.</p>
                    <a class="btn primary" href="/catalog">Open catalog</a>
                </div>
            `;

            if (placeButton) placeButton.disabled = true;
            return;
        }

        if (placeButton) placeButton.disabled = false;

        cartEl.innerHTML = cart.map((item, index) => `
            <article class="order-view-card">
                <div class="order-view-header">
                    <div>
                        <span class="pill">Catalog item #${escapeHtml(item.catalogItemId)}</span>
                        <h3>${escapeHtml(item.name || "Catalog item")}</h3>
                        <p class="muted">Jastiper ID: ${escapeHtml(item.jastiperId ?? "-")}</p>
                    </div>

                    <strong class="order-total">
                        ${formatCurrency(Number(item.priceSnapshot || 0) * Number(item.qty || 0))}
                    </strong>
                </div>

                <div class="order-meta-grid">
                    <span>Price: ${formatCurrency(item.priceSnapshot)}</span>
                    <span>Quantity: ${escapeHtml(item.qty || 1)}</span>
                    <span>Subtotal: ${formatCurrency(Number(item.priceSnapshot || 0) * Number(item.qty || 0))}</span>
                </div>

                <div class="order-action-row">
                    <label>
                        Update qty
                        <input class="order-qty-input" type="number" min="1" value="${escapeHtml(item.qty || 1)}" data-cart-index="${index}" />
                    </label>

                    <button class="btn ghost" type="button" data-action="update-cart-item" data-cart-index="${index}">
                        Update
                    </button>

                    <button class="btn danger" type="button" data-action="remove-cart-item" data-cart-index="${index}">
                        Remove
                    </button>
                </div>
            </article>
        `).join("");

        cartEl.querySelectorAll("[data-action='update-cart-item']").forEach((button) => {
            button.addEventListener("click", () => updateCartItem(Number(button.dataset.cartIndex)));
        });

        cartEl.querySelectorAll("[data-action='remove-cart-item']").forEach((button) => {
            button.addEventListener("click", () => removeCartItem(Number(button.dataset.cartIndex)));
        });
    }

    function updateCartItem(index) {
        const cart = readCart();
        const input = document.querySelector(`input[data-cart-index="${index}"]`);

        if (!cart[index]) return;

        cart[index].qty = Math.max(1, Number(input?.value || 1));
        writeCart(cart);
        renderCart();
        setNotice("success", "Cart updated.");
    }

    function removeCartItem(index) {
        const cart = readCart();

        if (!cart[index]) return;

        cart.splice(index, 1);
        writeCart(cart);
        renderCart();
        setNotice("success", "Item removed.");
    }

    function setupCartActions() {
        document.getElementById("clearOrderCart")?.addEventListener("click", () => {
            clearCart();
            renderCart();
            setNotice("success", "Cart cleared.");
        });
    }

    function setupCheckoutForm() {
        const form = document.getElementById("checkoutForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const cart = readCart();

            if (!cart.length) {
                setNotice("error", "Your cart is empty. Add items from catalog first.");
                return;
            }

            const shippingAddress = document.getElementById("checkoutShippingAddress")?.value?.trim();
            const voucherCode = document.getElementById("checkoutVoucherCode")?.value?.trim();

            if (!shippingAddress) {
                setNotice("error", "Shipping address is required.");
                return;
            }

            const payload = {
                shippingAddress,
                voucherCode: voucherCode || null,
                items: cart.map((item) => ({
                    catalogItemId: Number(item.catalogItemId),
                    qty: Number(item.qty),
                    priceSnapshot: Number(item.priceSnapshot || 0)
                }))
            };

            try {
                setNotice("info", "Placing order...");

                await requestJson("/api/orders", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                clearCart();
                form.reset();
                renderCart();
                await loadBuyerOrders();

                setNotice("success", "Order placed successfully.");
            } catch (error) {
                setNotice("error", error.message || "Failed to place order.");
            }
        });
    }

    async function loadBuyerOrders() {
        const list = document.getElementById("buyerOrdersList");
        if (!list) return;

        try {
            const orders = await requestJson("/api/orders/me");
            renderBuyerOrders(Array.isArray(orders) ? orders : []);
        } catch (error) {
            list.innerHTML = `<div class="notice error">${escapeHtml(error.message || "Failed to load buyer orders.")}</div>`;
        }
    }

    function renderBuyerOrders(orders) {
        const list = document.getElementById("buyerOrdersList");
        if (!list) return;

        if (!orders.length) {
            list.innerHTML = `
                <div class="empty-state">
                    <h3>No buyer orders yet</h3>
                    <p class="muted">After checkout, your purchase history will appear here.</p>
                    <a class="btn primary" href="/catalog">Open catalog</a>
                </div>
            `;
            return;
        }

        list.innerHTML = orders.map((order) => `
            <article class="order-view-card">
                <div class="order-view-header">
                    <div>
                        <span class="pill">${escapeHtml(order.status || "UNKNOWN")}</span>
                        <h3>Order #${escapeHtml(shortId(order.id))}</h3>
                        <p class="muted">
                            Jastiper ID: ${escapeHtml(order.jastiperId ?? "-")} ·
                            Buyer ID: ${escapeHtml(order.buyerId ?? "-")}
                        </p>
                    </div>

                    <strong class="order-total">${formatCurrency(order.totalPrice)}</strong>
                </div>

                <div class="order-meta-grid">
                    <span>Created: ${escapeHtml(formatDate(order.createdAt))}</span>
                    <span>Updated: ${escapeHtml(formatDate(order.updatedAt))}</span>
                    <span>Shipping: ${escapeHtml(order.shippingAddress || "-")}</span>
                </div>

                ${renderRatingArea(order)}
            </article>
        `).join("");

        list.querySelectorAll("[data-action='rate-order']").forEach((button) => {
            button.addEventListener("click", async () => {
                await submitRating(button.dataset.orderId);
            });
        });
    }

    function renderRatingArea(order) {
        if (order.status !== "COMPLETED") {
            return `
                <div class="order-state-box">
                    <strong>Rating locked</strong>
                    <p class="muted">You can rate this order after it becomes COMPLETED.</p>
                </div>
            `;
        }

        if (order.jastiperRating || order.productRating || order.review) {
            return `
                <div class="order-state-box">
                    <strong>Rating submitted</strong>
                    <p class="muted">
                        Jastiper: ${escapeHtml(order.jastiperRating ?? "-")} / 5 ·
                        Product: ${escapeHtml(order.productRating ?? "-")} / 5
                    </p>
                </div>
            `;
        }

        return `
            <form class="rating-form">
                <label>
                    Jastiper rating
                    <select data-jastiper-rating="${escapeHtml(order.id)}">
                        <option value="5">5 - Excellent</option>
                        <option value="4">4 - Good</option>
                        <option value="3">3 - Okay</option>
                        <option value="2">2 - Poor</option>
                        <option value="1">1 - Bad</option>
                    </select>
                </label>

                <label>
                    Product rating
                    <select data-product-rating="${escapeHtml(order.id)}">
                        <option value="5">5 - Excellent</option>
                        <option value="4">4 - Good</option>
                        <option value="3">3 - Okay</option>
                        <option value="2">2 - Poor</option>
                        <option value="1">1 - Bad</option>
                    </select>
                </label>

                <label>
                    Review
                    <textarea data-review="${escapeHtml(order.id)}" rows="3" placeholder="Optional review"></textarea>
                </label>

                <button class="btn primary" type="button" data-action="rate-order" data-order-id="${escapeHtml(order.id)}">
                    Submit rating
                </button>
            </form>
        `;
    }

    async function submitRating(orderId) {
        const jastiperRating = Number(document.querySelector(`[data-jastiper-rating="${cssEscape(orderId)}"]`)?.value || 5);
        const productRating = Number(document.querySelector(`[data-product-rating="${cssEscape(orderId)}"]`)?.value || 5);
        const review = document.querySelector(`[data-review="${cssEscape(orderId)}"]`)?.value?.trim() || "";

        try {
            await requestJson(`/api/orders/${encodeURIComponent(orderId)}/rating`, {
                method: "POST",
                body: JSON.stringify({ jastiperRating, productRating, review })
            });

            setNotice("success", "Rating submitted.");
            await loadBuyerOrders();
        } catch (error) {
            setNotice("error", error.message || "Failed to submit rating.");
        }
    }

    async function loadJastiperOrders() {
        const list = document.getElementById("jastiperOrdersList");
        if (!list) return;

        try {
            const orders = await requestJson("/api/orders/jastiper/me");
            renderJastiperOrders(Array.isArray(orders) ? orders : []);
        } catch (error) {
            list.innerHTML = `<div class="notice error">${escapeHtml(error.message || "Failed to load Jastiper orders.")}</div>`;
        }
    }

    function renderJastiperOrders(orders) {
        const list = document.getElementById("jastiperOrdersList");
        if (!list) return;

        if (!orders.length) {
            list.innerHTML = `
                <div class="empty-state">
                    <h3>No incoming orders yet</h3>
                    <p class="muted">Orders for your catalog items will appear here.</p>
                </div>
            `;
            return;
        }

        list.innerHTML = orders.map((order) => {
            const nextStatus = getNextStatus(order.status);
            const canCancel = order.status !== "COMPLETED" && order.status !== "CANCELLED";

            return `
                <article class="order-view-card">
                    <div class="order-view-header">
                        <div>
                            <span class="pill">${escapeHtml(order.status || "UNKNOWN")}</span>
                            <h3>Order #${escapeHtml(shortId(order.id))}</h3>
                            <p class="muted">
                                Buyer ID: ${escapeHtml(order.buyerId ?? "-")} ·
                                Jastiper ID: ${escapeHtml(order.jastiperId ?? "-")}
                            </p>
                        </div>

                        <strong class="order-total">${formatCurrency(order.totalPrice)}</strong>
                    </div>

                    <div class="order-meta-grid">
                        <span>Created: ${escapeHtml(formatDate(order.createdAt))}</span>
                        <span>Updated: ${escapeHtml(formatDate(order.updatedAt))}</span>
                        <span>Shipping: ${escapeHtml(order.shippingAddress || "-")}</span>
                    </div>

                    <div class="order-action-row">
                        ${nextStatus ? `
                            <button class="btn primary" type="button" data-action="advance-status" data-order-id="${escapeHtml(order.id)}" data-next-status="${escapeHtml(nextStatus)}">
                                Mark as ${escapeHtml(nextStatus)}
                            </button>
                        ` : ""}

                        ${canCancel ? `
                            <button class="btn danger" type="button" data-action="cancel-order" data-order-id="${escapeHtml(order.id)}">
                                Cancel order
                            </button>
                        ` : ""}
                    </div>
                </article>
            `;
        }).join("");

        list.querySelectorAll("[data-action='advance-status']").forEach((button) => {
            button.addEventListener("click", async () => {
                await updateOrderStatus(button.dataset.orderId, button.dataset.nextStatus);
            });
        });

        list.querySelectorAll("[data-action='cancel-order']").forEach((button) => {
            button.addEventListener("click", async () => {
                await cancelOrder(button.dataset.orderId);
            });
        });
    }

    function getNextStatus(status) {
        const index = ORDER_FLOW.indexOf(status);
        if (index === -1 || index >= ORDER_FLOW.length - 1) return null;
        return ORDER_FLOW[index + 1];
    }

    async function updateOrderStatus(orderId, nextStatus) {
        try {
            await requestJson(`/api/orders/${encodeURIComponent(orderId)}/status?status=${encodeURIComponent(nextStatus)}`, {
                method: "PATCH"
            });

            setNotice("success", "Order status updated.");
            await loadJastiperOrders();
        } catch (error) {
            setNotice("error", error.message || "Failed to update order status.");
        }
    }

    async function cancelOrder(orderId) {
        try {
            await requestJson(`/api/orders/${encodeURIComponent(orderId)}/cancel`, {
                method: "POST"
            });

            setNotice("success", "Order cancelled.");
            await loadJastiperOrders();
        } catch (error) {
            setNotice("error", error.message || "Failed to cancel order.");
        }
    }

    function setNotice(type, message) {
        const notice = document.getElementById("orderNotice");
        if (!notice) return;

        if (!message) {
            notice.className = "hidden";
            notice.textContent = "";
            return;
        }

        notice.className = `notice ${type || "info"}`;
        notice.textContent = message;
    }

    function formatCurrency(value) {
        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "IDR",
            maximumFractionDigits: 0
        }).format(Number(value || 0));
    }

    function formatDate(value) {
        if (!value) return "-";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return String(value);

        return new Intl.DateTimeFormat("en-GB", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(date);
    }

    function shortId(id) {
        if (!id) return "-";
        return String(id).slice(0, 8);
    }

    function cssEscape(value) {
        if (window.CSS && CSS.escape) return CSS.escape(String(value));
        return String(value).replace(/"/g, '\\"');
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();