(function () {
    const ROLE_OPTIONS = ["TITIPER", "JASTIPER"];
    const STATUS_OPTIONS = ["ACTIVE", "BANNED", "PENDING_VERIFICATION"];

    let currentAdmin = null;

    document.addEventListener("DOMContentLoaded", initAdminPage);

    async function initAdminPage() {
        const page = document.body?.dataset?.page;
        if (page !== "admin") return;

        const token = getAdminToken();
        if (!token) {
            window.location.href = "/login";
            return;
        }

        const refreshButton = document.getElementById("refreshAdminData");
        if (refreshButton) {
            refreshButton.addEventListener("click", async () => {
                await refreshAdminData();
            });
        }

        try {
            currentAdmin = await requestJson("/api/auth/me");
            renderAdminSummary(currentAdmin);

            if (currentAdmin.role !== "ADMIN") {
                showAccessDenied();
                return;
            }

            document.getElementById("adminDashboard")?.classList.remove("hidden");
            await refreshAdminData();
        } catch (error) {
            showNotice("error", error.message || "Failed to load admin dashboard.");
            showAccessDenied();
        }
    }

    function getAdminToken() {
        if (window.getToken) return window.getToken();
        return localStorage.getItem("json_token");
    }

    async function fallbackAuthFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        const token = getAdminToken();

        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        return fetch(url, { ...options, headers });
    }

    async function adminFetch(url, options = {}) {
        if (window.authFetch) {
            return window.authFetch(url, options);
        }
        return fallbackAuthFetch(url, options);
    }

    async function requestJson(url, options = {}) {
        const response = await adminFetch(url, options);

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
                return json.message || json.error || text;
            } catch {
                return text;
            }
        } catch {
            return "";
        }
    }

    async function refreshAdminData() {
        showNotice("info", "Loading admin data...");

        try {
            const [users, pendingKyc] = await Promise.all([
                requestJson("/api/admin/users"),
                requestJson("/api/admin/kyc/pending")
            ]);

            renderAdminStats(users || [], pendingKyc || []);
            renderPendingKyc(pendingKyc || [], users || []);
            renderUsersTable(users || []);

            showNotice("success", "Admin dashboard updated.");
        } catch (error) {
            showNotice("error", error.message || "Failed to refresh admin data.");
        }
    }

    function renderAdminSummary(admin) {
        const el = document.getElementById("adminSummary");
        if (!el) return;

        el.innerHTML = `
            <span class="pill">Signed in as admin</span>
            <h2>${escapeAdminHtml(admin.fullName || admin.username || "Admin")}</h2>
            <p class="muted">${escapeAdminHtml(admin.email || "-")}</p>
            <div class="admin-mini-grid">
                <div>
                    <span class="admin-label">Role</span>
                    <strong>${escapeAdminHtml(admin.role || "-")}</strong>
                </div>
                <div>
                    <span class="admin-label">Status</span>
                    <strong>${escapeAdminHtml(admin.status || "-")}</strong>
                </div>
            </div>
        `;
    }

    function showAccessDenied() {
        document.getElementById("adminDashboard")?.classList.add("hidden");
        document.getElementById("adminAccessDenied")?.classList.remove("hidden");
    }

    function showNotice(type, message) {
        const el = document.getElementById("adminNotice");
        if (!el) return;

        if (!message) {
            el.className = "hidden";
            el.textContent = "";
            return;
        }

        el.className = `notice ${type || "info"}`;
        el.textContent = message;
    }

    function renderAdminStats(users, pendingKyc) {
        const el = document.getElementById("adminStats");
        if (!el) return;

        const totalUsers = users.length;
        const activeUsers = users.filter((user) => user.status === "ACTIVE").length;
        const bannedUsers = users.filter((user) => user.status === "BANNED").length;
        const jastipers = users.filter((user) => user.role === "JASTIPER").length;
        const pendingCount = pendingKyc.length;

        el.innerHTML = `
            ${statCard("Total users", totalUsers, "Registered accounts")}
            ${statCard("Pending KYC", pendingCount, "Waiting for admin review")}
            ${statCard("Jastipers", jastipers, "Approved seller accounts")}
            ${statCard("Active / Banned", `${activeUsers} / ${bannedUsers}`, "Account health")}
        `;
    }

    function statCard(title, value, text) {
        return `
            <article class="admin-stat-card">
                <span>${escapeAdminHtml(title)}</span>
                <strong>${escapeAdminHtml(value)}</strong>
                <p>${escapeAdminHtml(text)}</p>
            </article>
        `;
    }

    function renderPendingKyc(pendingKyc, users) {
        const el = document.getElementById("pendingKycList");
        if (!el) return;

        if (!pendingKyc.length) {
            el.innerHTML = `
                <div class="admin-empty">
                    <h3>No pending KYC applications</h3>
                    <p class="muted">When Titipers apply to become Jastipers, their requests will appear here.</p>
                </div>
            `;
            return;
        }

        const usersById = new Map(users.map((user) => [String(user.id), user]));

        el.innerHTML = pendingKyc.map((kyc) => {
            const user = usersById.get(String(kyc.userId));
            const email = user?.email || "Email not loaded";
            const username = user?.username || "-";

            return `
                <article class="admin-kyc-card">
                    <div>
                        <span class="pill warning">${escapeAdminHtml(kyc.status || "PENDING")}</span>
                        <h3>${escapeAdminHtml(kyc.fullName || "Unnamed applicant")}</h3>
                        <p class="muted">
                            User ID: ${escapeAdminHtml(kyc.userId)} · 
                            ${escapeAdminHtml(email)} · 
                            @${escapeAdminHtml(username)}
                        </p>
                        <p>
                            <strong>Social media:</strong>
                            ${kyc.socialMediaLink
                ? `<a href="${escapeAttribute(kyc.socialMediaLink)}" target="_blank" rel="noreferrer">${escapeAdminHtml(kyc.socialMediaLink)}</a>`
                : "<span class='muted'>Not provided</span>"
            }
                        </p>
                        <p class="muted">
                            Submitted: ${escapeAdminHtml(formatAdminDate(kyc.submittedAt))}
                        </p>
                    </div>

                    <div class="admin-actions">
                        <button class="btn primary" type="button" data-action="approve-kyc" data-user-id="${escapeAttribute(kyc.userId)}">
                            Approve
                        </button>
                        <button class="btn danger" type="button" data-action="reject-kyc" data-user-id="${escapeAttribute(kyc.userId)}">
                            Reject
                        </button>
                    </div>
                </article>
            `;
        }).join("");

        el.querySelectorAll("[data-action='approve-kyc']").forEach((button) => {
            button.addEventListener("click", async () => {
                await reviewKyc(button.dataset.userId, "approve");
            });
        });

        el.querySelectorAll("[data-action='reject-kyc']").forEach((button) => {
            button.addEventListener("click", async () => {
                await reviewKyc(button.dataset.userId, "reject");
            });
        });
    }

    async function reviewKyc(userId, action) {
        const label = action === "approve" ? "approve" : "reject";
        const confirmed = window.confirm(`Are you sure you want to ${label} this KYC application?`);
        if (!confirmed) return;

        try {
            await requestJson(`/api/admin/kyc/${encodeURIComponent(userId)}/${action}`, {
                method: "POST"
            });

            showNotice("success", `KYC ${label}d successfully.`);
            await refreshAdminData();
        } catch (error) {
            showNotice("error", error.message || `Failed to ${label} KYC.`);
        }
    }

    function renderUsersTable(users) {
        const tbody = document.getElementById("adminUsersTableBody");
        if (!tbody) return;

        if (!users.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7">No users found.</td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = users.map((user) => {
            const isCurrentAdmin = currentAdmin && String(currentAdmin.id) === String(user.id);
            const isAdminRole = user.role === "ADMIN";
            const lockRole = isCurrentAdmin || isAdminRole;
            const lockAction = isCurrentAdmin;

            return `
                <tr>
                    <td>${escapeAdminHtml(user.id)}</td>
                    <td>
                        <strong>${escapeAdminHtml(user.fullName || user.username || "Unnamed user")}</strong>
                        <span class="admin-user-subtext">
                            ${escapeAdminHtml(user.email || "-")}
                            ${user.username ? ` · @${escapeAdminHtml(user.username)}` : ""}
                            ${isCurrentAdmin ? " · current admin" : ""}
                        </span>
                    </td>
                    <td>
                        <span class="admin-status-pill role-${escapeAdminHtml(user.role || "UNKNOWN").toLowerCase()}">
                            ${escapeAdminHtml(user.role || "-")}
                        </span>
                    </td>
                    <td>
                        <span class="admin-status-pill status-${escapeAdminHtml(user.status || "UNKNOWN").toLowerCase()}">
                            ${escapeAdminHtml(user.status || "-")}
                        </span>
                    </td>
                    <td>
                        <select class="admin-select" data-role-select="${escapeAttribute(user.id)}" ${lockRole ? "disabled" : ""}>
                            ${renderRoleOptions(user.role)}
                        </select>
                        ${isAdminRole ? "<span class='admin-help-text'>ADMIN cannot be assigned here.</span>" : ""}
                    </td>
                    <td>
                        <select class="admin-select" data-status-select="${escapeAttribute(user.id)}" ${lockAction ? "disabled" : ""}>
                            ${renderStatusOptions(user.status)}
                        </select>
                    </td>
                    <td>
                        <button
                            class="btn small"
                            type="button"
                            data-action="save-user"
                            data-user-id="${escapeAttribute(user.id)}"
                            ${lockAction ? "disabled" : ""}
                        >
                            Save
                        </button>
                    </td>
                </tr>
            `;
        }).join("");

        tbody.querySelectorAll("[data-action='save-user']").forEach((button) => {
            button.addEventListener("click", async () => {
                await updateUser(button.dataset.userId);
            });
        });
    }

    function renderRoleOptions(currentRole) {
        if (currentRole === "ADMIN") {
            return `<option value="ADMIN" selected>ADMIN</option>`;
        }

        return ROLE_OPTIONS.map((role) => `
            <option value="${role}" ${role === currentRole ? "selected" : ""}>
                ${role}
            </option>
        `).join("");
    }

    function renderStatusOptions(currentStatus) {
        return STATUS_OPTIONS.map((status) => `
            <option value="${status}" ${status === currentStatus ? "selected" : ""}>
                ${status}
            </option>
        `).join("");
    }

    async function updateUser(userId) {
        const roleSelect = document.querySelector(`[data-role-select="${CSS.escape(String(userId))}"]`);
        const statusSelect = document.querySelector(`[data-status-select="${CSS.escape(String(userId))}"]`);

        const role = roleSelect && !roleSelect.disabled ? roleSelect.value : null;
        const status = statusSelect && !statusSelect.disabled ? statusSelect.value : null;

        if (!role && !status) {
            showNotice("info", "Nothing to update for this user.");
            return;
        }

        try {
            await requestJson(`/api/admin/users/${encodeURIComponent(userId)}`, {
                method: "PATCH",
                body: JSON.stringify({ role, status })
            });

            showNotice("success", "User updated successfully.");
            await refreshAdminData();
        } catch (error) {
            showNotice("error", error.message || "Failed to update user.");
        }
    }

    function formatAdminDate(value) {
        if (!value) return "-";

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return String(value);

        return new Intl.DateTimeFormat("en-GB", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(date);
    }

    function escapeAdminHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function escapeAttribute(value) {
        return escapeAdminHtml(value);
    }
})();