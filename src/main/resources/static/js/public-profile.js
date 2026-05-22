(function () {
    document.addEventListener("DOMContentLoaded", initPublicProfilePage);

    async function initPublicProfilePage() {
        if (document.body?.dataset?.page !== "public-profile") return;

        renderAccountWidget();
        setupSearchForm();

        const params = new URLSearchParams(window.location.search);
        const username = params.get("username");
        const userId = params.get("userId");

        if (userId) {
            await loadProfileByUserId(userId);
            return;
        }

        if (username) {
            await loadProfileByUsername(username);
            return;
        }

        renderEmptyState();
    }

    function renderAccountWidget() {
        const widget = document.getElementById("publicProfileAccountWidget");
        if (!widget) return;

        const token = localStorage.getItem("json_token");

        if (!token) {
            widget.innerHTML = `<a class="btn ghost small" href="/login">Login</a>`;
            return;
        }

        widget.innerHTML = `
            <a class="btn ghost small" href="/profile">My profile</a>
        `;
    }

    function setupSearchForm() {
        const form = document.getElementById("publicProfileSearchForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const username = document.getElementById("publicProfileSearchInput")?.value?.trim();

            if (!username) {
                showNotice("error", "Please enter a username.");
                return;
            }

            window.history.pushState({}, "", `/users?username=${encodeURIComponent(username)}`);
            await loadProfileByUsername(username);
        });
    }

    async function loadProfileByUsername(username) {
        try {
            showNotice("info", "Loading public profile...");

            const profile = await requestJson(`/api/users/${encodeURIComponent(username)}`);
            renderProfile(profile);
            showNotice("", "");
        } catch (error) {
            renderError(error.message || "Profile not found.");
        }
    }

    async function loadProfileByUserId(userId) {
        try {
            showNotice("info", "Loading public profile...");

            const profile = await requestJson(`/api/users/id/${encodeURIComponent(userId)}`);
            renderProfile(profile);
            showNotice("", "");
        } catch (error) {
            renderError(error.message || "Profile not found.");
        }
    }

    async function requestJson(url) {
        const response = await fetch(url);

        if (!response.ok) {
            const text = await response.text();
            throw new Error(readErrorText(text) || `Request failed with status ${response.status}`);
        }

        return response.json();
    }

    function readErrorText(text) {
        if (!text) return "";

        try {
            const json = JSON.parse(text);
            return json.error || json.message || text;
        } catch {
            return text;
        }
    }

    function renderProfile(profile) {
        const summary = document.getElementById("publicProfileSummary");
        const result = document.getElementById("publicProfileResult");
        const searchInput = document.getElementById("publicProfileSearchInput");

        if (searchInput && profile.username) {
            searchInput.value = profile.username;
        }

        const isJastiper = profile.role === "JASTIPER";

        if (summary) {
            summary.innerHTML = `
                <span class="pill">${escapeHtml(profile.role || "USER")}</span>
                <h2>${escapeHtml(profile.fullName || profile.username || "User")}</h2>
                <p class="muted">@${escapeHtml(profile.username || "-")}</p>
                <div class="public-profile-badge-row">
                    <span class="profile-badge role-${escapeHtml(String(profile.role || "").toLowerCase())}">
                        ${escapeHtml(profile.role || "-")}
                    </span>
                    <span class="profile-badge status-${escapeHtml(String(profile.status || "").toLowerCase())}">
                        ${escapeHtml(profile.status || "-")}
                    </span>
                </div>
            `;
        }

        if (!result) return;

        result.innerHTML = `
            <div class="section-heading">
                <div>
                    <p class="eyebrow">Profile detail</p>
                    <h2>${escapeHtml(profile.fullName || profile.username || "User profile")}</h2>
                    <p class="muted">
                        Public information visible to other JSON users.
                    </p>
                </div>
            </div>

            <div class="profile-detail-grid">
                ${detailItem("User ID", profile.id)}
                ${detailItem("Username", "@" + (profile.username || "-"))}
                ${detailItem("Full name", profile.fullName || "-")}
                ${detailItem("Role", profile.role || "-")}
                ${detailItem("Status", profile.status || "-")}
            </div>

            ${isJastiper ? `
                <div class="public-reputation-block">
                    <h3>Jastiper reputation</h3>
                    <p class="muted">
                        These stats are updated after completed orders and submitted ratings.
                    </p>

                    <div class="profile-stat-grid">
                        ${statItem("Average rating", formatRating(profile.averageRating))}
                        ${statItem("Rating count", pick(profile.ratingCount, "-"))}
                        ${statItem("Successful transactions", pick(profile.successfulTransactions, "0"))}
                        ${statItem("Total transactions", pick(profile.totalTransactions, "-"))}
                        ${statItem("Success rate", formatSuccessRate(profile.successRate))}
                    </div>
                </div>
            ` : `
                <div class="profile-state-box">
                    <strong>Titiper profile</strong>
                    <p class="muted">
                        This user is a Titiper. Jastiper reputation appears only after KYC approval.
                    </p>
                </div>
            `}
        `;
    }

    function renderEmptyState() {
        const summary = document.getElementById("publicProfileSummary");
        const result = document.getElementById("publicProfileResult");

        if (summary) {
            summary.innerHTML = `
                <span class="pill">Search</span>
                <h2>No profile selected</h2>
                <p class="muted">Search a username or open a Jastiper profile from catalog.</p>
            `;
        }

        if (result) {
            result.innerHTML = `
                <div class="empty-state">
                    <h3>No profile selected</h3>
                    <p class="muted">Enter a username above to view a public profile.</p>
                </div>
            `;
        }
    }

    function renderError(message) {
        const summary = document.getElementById("publicProfileSummary");
        const result = document.getElementById("publicProfileResult");

        if (summary) {
            summary.innerHTML = `
                <span class="pill danger">Not found</span>
                <h2>Profile unavailable</h2>
                <p class="muted">${escapeHtml(message)}</p>
            `;
        }

        if (result) {
            result.innerHTML = `
                <div class="notice error">${escapeHtml(message)}</div>
            `;
        }

        showNotice("error", message);
    }

    function detailItem(label, value) {
        return `
            <div class="profile-detail-item">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value ?? "-")}</strong>
            </div>
        `;
    }

    function statItem(label, value) {
        return `
            <div class="profile-stat-item">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value ?? "-")}</strong>
            </div>
        `;
    }

    function pick(value, fallback) {
        return value === null || value === undefined ? fallback : value;
    }

    function formatRating(value) {
        if (value === null || value === undefined) return "-";

        const number = Number(value);
        if (!Number.isFinite(number)) return String(value);

        return number.toFixed(1) + " / 5";
    }

    function formatSuccessRate(value) {
        if (value === null || value === undefined) return "-";

        const number = Number(value);
        if (!Number.isFinite(number)) return String(value);

        return number.toFixed(0) + "%";
    }

    function showNotice(type, message) {
        const notice = document.getElementById("publicProfileNotice");
        if (!notice) return;

        if (!message) {
            notice.className = "hidden";
            notice.textContent = "";
            return;
        }

        notice.className = `notice ${type || "info"}`;
        notice.textContent = message;
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