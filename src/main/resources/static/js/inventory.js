(function () {
    let currentUser = null;
    let inventoryItems = [];
    let editingItemId = null;

    document.addEventListener("DOMContentLoaded", initInventoryPage);

    async function initInventoryPage() {
        if (document.body?.dataset?.page !== "inventory") return;

        if (!getToken()) {
            window.location.href = "/login";
            return;
        }

        setupInventoryForm();
        setupInventorySearch();
        setupRefreshButton();
        setupCancelEditButton();

        await loadInventoryPage();
    }

    function getToken() {
        if (window.getToken) return window.getToken();
        return localStorage.getItem("json_token");
    }

    async function apiFetch(url, options = {}) {
        if (window.authFetch) {
            return window.authFetch(url, options);
        }

        const headers = new Headers(options.headers || {});
        const token = getToken();

        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        return fetch(url, { ...options, headers });
    }

    async function requestJson(url, options = {}) {
        const response = await apiFetch(url, options);

        if (!response.ok) {
            const message = await readError(response);
            throw new Error(message || `Request failed with status ${response.status}`);
        }

        if (response.status === 204) {
            return null;
        }

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

    async function loadInventoryPage() {
        setInventoryNotice("info", "Loading inventory...");

        try {
            currentUser = window.loadCurrentUser ? await window.loadCurrentUser() : await requestJson("/api/auth/me");
            renderRoleState(currentUser);

            if (!isApprovedJastiper(currentUser)) {
                inventoryItems = [];
                renderInventoryItems([]);
                setInventoryNotice("warning", "Inventory is available only for approved active Jastipers.");
                return;
            }

            inventoryItems = await requestJson("/api/catalog/mine");
            renderInventoryItems(inventoryItems || []);
            setInventoryNotice("", "");
        } catch (error) {
            setInventoryNotice("error", error.message || "Failed to load inventory.");
        }
    }

    function isApprovedJastiper(user) {
        return Boolean(user && user.role === "JASTIPER" && user.status === "ACTIVE");
    }

    function renderRoleState(user) {
        const accessBox = document.getElementById("inventoryAccessBox");
        const managePanel = document.getElementById("inventoryManagePanel");

        if (!accessBox || !managePanel) return;

        if (isApprovedJastiper(user)) {
            accessBox.innerHTML = `
                <h2>Inventory unlocked</h2>
                <p class="muted">You can manage your own catalog items, stock, and flash-sale quota here.</p>
            `;
            managePanel.classList.remove("hidden");
            return;
        }

        managePanel.classList.add("hidden");

        if (!user) {
            accessBox.innerHTML = `
                <h2>Sign in required</h2>
                <p class="muted">Log in to see your inventory page.</p>
            `;
            return;
        }

        if (user.status === "PENDING_VERIFICATION") {
            accessBox.innerHTML = `
                <h2>KYC pending</h2>
                <p class="muted">Your Jastiper application is still waiting for admin approval.</p>
                <a class="btn-ghost full-action" href="/profile">Check profile status</a>
            `;
            return;
        }

        accessBox.innerHTML = `
            <h2>Titiper account</h2>
            <p class="muted">Inventory creation is available after KYC approval and Jastiper activation.</p>
            <a class="button full-action" href="/profile">Apply for Jastiper</a>
        `;
    }

    function setupInventoryForm() {
        const form = document.getElementById("inventoryForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            if (!isApprovedJastiper(currentUser)) {
                setInventoryNotice("error", "Only approved active Jastipers can manage inventory.");
                return;
            }

            const payload = {
                name: value("inventoryName"),
                description: value("inventoryDescription"),
                price: Number(value("inventoryPrice") || 0),
                stock: Number(value("inventoryStock") || 0),
                origin: value("inventoryOrigin"),
                purchaseDate: value("inventoryPurchaseDate")
            };

            try {
                if (editingItemId) {
                    await requestJson(`/api/catalog/${editingItemId}`, {
                        method: "PATCH",
                        body: JSON.stringify({
                            description: payload.description,
                            price: payload.price,
                            stock: payload.stock
                        })
                    });
                    setInventoryNotice("success", "Inventory item updated.");
                } else {
                    await requestJson("/api/catalog", {
                        method: "POST",
                        body: JSON.stringify(payload)
                    });
                    setInventoryNotice("success", "Inventory item created.");
                }

                resetForm();
                await loadInventoryPage();
            } catch (error) {
                setInventoryNotice("error", error.message || "Failed to save item.");
            }
        });
    }

    function setupCancelEditButton() {
        const button = document.getElementById("inventoryCancelEdit");
        if (!button) return;

        button.addEventListener("click", () => {
            resetForm();
            setInventoryNotice("info", "Edit cancelled.");
        });
    }

    function setupRefreshButton() {
        const button = document.getElementById("refreshInventory");
        if (!button) return;

        button.addEventListener("click", () => loadInventoryPage());
    }

    function setupInventorySearch() {
        const input = document.getElementById("inventorySearch");
        if (!input) return;

        input.addEventListener("input", () => renderInventoryItems(inventoryItems));
    }

    function renderInventoryItems(items) {
        const grid = document.getElementById("inventoryGrid");
        const empty = document.getElementById("inventoryEmpty");
        if (!grid || !empty) return;

        const query = value("inventorySearch").toLowerCase();
        const filtered = (items || []).filter((item) => {
            const haystack = [
                item.name,
                item.description,
                item.origin,
                item.purchaseDate
            ].join(" ").toLowerCase();
            return !query || haystack.includes(query);
        });

        if (!filtered.length) {
            grid.innerHTML = "";
            empty.classList.remove("hidden");
            return;
        }

        empty.classList.add("hidden");

        grid.innerHTML = filtered.map((item) => `
            <article class="catalog-item-card">
                <div>
                    <p class="eyebrow">Item #${escapeHtml(item.id ?? "-")}</p>
                    <h3>${escapeHtml(item.name || "Unnamed item")}</h3>
                    <p class="muted">${escapeHtml(item.description || "No description provided.")}</p>
                </div>

                <div class="catalog-item-meta">
                    <span>${formatCurrency(item.price)}</span>
                    <span>Stock: ${escapeHtml(item.stock ?? 0)}</span>
                    <span>Origin: ${escapeHtml(item.origin || "-")}</span>
                    <span>Purchase: ${escapeHtml(item.purchaseDate || "-")}</span>
                </div>

                <div class="catalog-card-actions">
                    <button class="button" type="button" data-edit-id="${escapeHtml(item.id)}">Edit</button>
                    <button class="btn-ghost" type="button" data-delete-id="${escapeHtml(item.id)}">Delete</button>
                </div>

                <div class="catalog-card-actions">
                    <label>
                        Stock qty
                        <input class="catalog-qty-input" type="number" min="1" value="1" data-qty-for="${escapeHtml(item.id)}" />
                    </label>
                    <button class="btn-ghost" type="button" data-reserve-id="${escapeHtml(item.id)}">Reserve</button>
                    <button class="btn-ghost" type="button" data-release-id="${escapeHtml(item.id)}">Release</button>
                </div>
            </article>
        `).join("");

        grid.querySelectorAll("[data-edit-id]").forEach((button) => {
            button.addEventListener("click", () => startEdit(Number(button.dataset.editId)));
        });

        grid.querySelectorAll("[data-delete-id]").forEach((button) => {
            button.addEventListener("click", () => deleteItem(Number(button.dataset.deleteId)));
        });

        grid.querySelectorAll("[data-reserve-id]").forEach((button) => {
            button.addEventListener("click", () => changeStock(Number(button.dataset.reserveId), "reserve"));
        });

        grid.querySelectorAll("[data-release-id]").forEach((button) => {
            button.addEventListener("click", () => changeStock(Number(button.dataset.releaseId), "release"));
        });
    }

    function startEdit(itemId) {
        const item = inventoryItems.find((candidate) => Number(candidate.id) === Number(itemId));
        if (!item) return;

        editingItemId = Number(itemId);
        setValue("inventoryName", item.name || "");
        setValue("inventoryDescription", item.description || "");
        setValue("inventoryPrice", item.price ?? 0);
        setValue("inventoryStock", item.stock ?? 0);
        setValue("inventoryOrigin", item.origin || "");
        setValue("inventoryPurchaseDate", item.purchaseDate || "");

        document.getElementById("inventoryFormTitle").textContent = `Edit item #${itemId}`;
        document.getElementById("inventorySubmit").textContent = "Update item";
        document.getElementById("inventoryCancelEdit")?.classList.remove("hidden");
        setInventoryNotice("info", `Editing item #${itemId}.`);
    }

    async function deleteItem(itemId) {
        if (!isApprovedJastiper(currentUser)) {
            setInventoryNotice("error", "Only approved active Jastipers can delete inventory items.");
            return;
        }

        if (!window.confirm(`Delete item #${itemId}?`)) return;

        try {
            await requestJson(`/api/catalog/${itemId}`, { method: "DELETE" });
            setInventoryNotice("success", "Inventory item deleted.");
            if (editingItemId === itemId) {
                resetForm();
            }
            await loadInventoryPage();
        } catch (error) {
            setInventoryNotice("error", error.message || "Failed to delete item.");
        }
    }

    async function changeStock(itemId, action) {
        const input = document.querySelector(`[data-qty-for="${CSS.escape(String(itemId))}"]`);
        const qty = Math.max(1, Number(input?.value || 1));

        try {
            await requestJson(`/api/catalog/${itemId}/${action}`, {
                method: "POST",
                body: JSON.stringify({ quantity: qty })
            });
            setInventoryNotice("success", `Stock ${action}d successfully.`);
            await loadInventoryPage();
        } catch (error) {
            setInventoryNotice("error", error.message || `Failed to ${action} stock.`);
        }
    }

    function resetForm() {
        editingItemId = null;
        document.getElementById("inventoryForm")?.reset();
        document.getElementById("inventoryFormTitle").textContent = "Add new item";
        document.getElementById("inventorySubmit").textContent = "Save item";
        document.getElementById("inventoryCancelEdit")?.classList.add("hidden");
    }

    function setInventoryNotice(type, message) {
        const el = document.getElementById("inventoryNotice");
        if (!el) return;

        if (!message) {
            el.className = "hidden";
            el.textContent = "";
            return;
        }

        el.className = `notice ${type || "info"}`;
        el.textContent = message;
    }

    function value(id) {
        return document.getElementById(id)?.value?.trim() || "";
    }

    function setValue(id, val) {
        const el = document.getElementById(id);
        if (el) {
            el.value = String(val ?? "");
        }
    }

    function formatCurrency(value) {
        if (window.formatCurrency) return window.formatCurrency(value);

        return new Intl.NumberFormat("en-US", {
            style: "currency",
            currency: "IDR",
            maximumFractionDigits: 0
        }).format(Number(value || 0));
    }

    function escapeHtml(value) {
        if (window.escapeHtml) return window.escapeHtml(value);

        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();
