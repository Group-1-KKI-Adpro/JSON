(function () {
    let currentProfile = null;

    document.addEventListener("DOMContentLoaded", initProfilePage);

    async function initProfilePage() {
        const page = document.body?.dataset?.page;
        if (page !== "profile") return;

        if (!getProfileToken()) {
            window.location.href = "/login";
            return;
        }

        setupProfileForm();
        setupKycForm();
        setupPublicProfileForm();

        const refreshButton = document.getElementById("refreshProfile");
        if (refreshButton) {
            refreshButton.addEventListener("click", async () => {
                await loadProfilePage();
            });
        }

        await loadProfilePage();
    }

    function getProfileToken() {
        if (window.getToken) return window.getToken();
        return localStorage.getItem("json_token");
    }

    async function profileFetch(url, options = {}) {
        if (window.authFetch) {
            return window.authFetch(url, options);
        }

        const headers = new Headers(options.headers || {});
        const token = getProfileToken();

        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        return fetch(url, { ...options, headers });
    }

    async function requestJson(url, options = {}) {
        const response = await profileFetch(url, options);

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

    async function loadProfilePage() {
        setProfileNotice("info", "Loading profile...");

        try {
            const profile = await requestJson("/api/auth/me");
            currentProfile = profile;

            saveProfileToLocalStorage(profile);
            renderProfileStatus(profile);
            renderProfileCard(profile);
            fillProfileForm(profile);
            renderKycState(profile);

            if (profile.role === "JASTIPER" && profile.username) {
                await enrichJastiperStats(profile.username);
            }

            setProfileNotice("", "");
        } catch (error) {
            setProfileNotice("error", error.message || "Failed to load profile.");
        }
    }

    function saveProfileToLocalStorage(profile) {
        try {
            localStorage.setItem("json_profile", JSON.stringify(profile));
        } catch {
            // ignore storage errors
        }

        if (window.renderAccountWidget) {
            window.renderAccountWidget(profile);
        }
    }

    function renderProfileStatus(profile) {
        const el = document.getElementById("profileStatusCard");
        if (!el) return;

        const roleClass = String(profile.role || "").toLowerCase();
        const statusClass = String(profile.status || "").toLowerCase();

        el.innerHTML = `
            <span class="pill">Signed in</span>
            <h2>${escapeProfileHtml(profile.fullName || profile.username || "User")}</h2>
            <p class="muted">${escapeProfileHtml(profile.email || "-")}</p>

            <div class="profile-status-row">
                <span class="profile-badge role-${escapeProfileHtml(roleClass)}">${escapeProfileHtml(profile.role || "-")}</span>
                <span class="profile-badge status-${escapeProfileHtml(statusClass)}">${escapeProfileHtml(profile.status || "-")}</span>
            </div>
        `;
    }

    function renderProfileCard(profile, publicProfile = null) {
        const el = document.getElementById("profileCard");
        if (!el) return;

        const isJastiper = profile.role === "JASTIPER";
        const stats = publicProfile || {};

        const averageRating = pickValue(stats, ["averageRating", "avgRating"], "-");
        const ratingCount = pickValue(stats, ["ratingCount", "totalReviews"], "-");
        const successfulTransactions = pickValue(stats, ["successfulTransactions", "successTransactionCount"], "-");
        const totalTransactions = pickValue(stats, ["totalTransactions", "transactionCount"], "-");
        const successRate = pickValue(stats, ["successRate"], "-");

        el.innerHTML = `
            <div class="profile-detail-grid">
                ${detailItem("User ID", profile.id)}
                ${detailItem("Email", profile.email)}
                ${detailItem("Username", profile.username || "-")}
                ${detailItem("Full name", profile.fullName || "-")}
                ${detailItem("Role", profile.role || "-")}
                ${detailItem("Status", profile.status || "-")}
            </div>

            ${isJastiper ? `
                <div class="profile-stats-block">
                    <h3>Jastiper reputation</h3>
                    <div class="profile-stat-grid">
                        ${statItem("Average rating", averageRating)}
                        ${statItem("Rating count", ratingCount)}
                        ${statItem("Successful transactions", successfulTransactions)}
                        ${statItem("Total transactions", totalTransactions)}
                        ${statItem("Success rate", formatSuccessRate(successRate))}
                    </div>
                </div>
            ` : `
                <div class="profile-state-box">
                    <strong>Titiper account</strong>
                    <p class="muted">
                        This account can shop and place orders. Submit KYC to request Jastiper access.
                    </p>
                </div>
            `}
        `;
    }

    async function enrichJastiperStats(username) {
        try {
            const publicProfile = await requestJson(`/api/users/${encodeURIComponent(username)}`);
            renderProfileCard(currentProfile, publicProfile);
        } catch {
            // Keep basic profile card if public profile fetch fails.
        }
    }

    function fillProfileForm(profile) {
        setInputValue("profileEmail", profile.email || "");
        setInputValue("profileUsername", profile.username || "");
        setInputValue("profileFullName", profile.fullName || "");
        setInputValue("kycFullName", profile.fullName || "");
        setInputValue("publicProfileUsername", profile.username || "");
    }

    function setInputValue(id, value) {
        const input = document.getElementById(id);
        if (input) input.value = value;
    }

    function setupProfileForm() {
        const form = document.getElementById("profileForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const username = document.getElementById("profileUsername")?.value?.trim() || "";
            const fullName = document.getElementById("profileFullName")?.value?.trim() || "";

            try {
                const updated = await requestJson("/api/auth/profile", {
                    method: "PATCH",
                    body: JSON.stringify({ username, fullName })
                });

                currentProfile = updated;
                saveProfileToLocalStorage(updated);
                renderProfileStatus(updated);
                renderProfileCard(updated);
                fillProfileForm(updated);
                renderKycState(updated);

                if (updated.role === "JASTIPER" && updated.username) {
                    await enrichJastiperStats(updated.username);
                }

                setProfileNotice("success", "Profile updated successfully.");
            } catch (error) {
                setProfileNotice("error", error.message || "Failed to update profile.");
            }
        });
    }

    function setupKycForm() {
        const form = document.getElementById("kycForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const fullName = document.getElementById("kycFullName")?.value?.trim() || "";
            const socialMediaLink = document.getElementById("kycSocialMediaLink")?.value?.trim() || "";

            try {
                await requestJson("/api/kyc/apply", {
                    method: "POST",
                    body: JSON.stringify({ fullName, socialMediaLink })
                });

                setProfileNotice(
                    "success",
                    "KYC application submitted. Your account is now waiting for admin verification."
                );

                await loadProfilePage();
            } catch (error) {
                setProfileNotice("error", error.message || "Failed to submit KYC application.");
            }
        });
    }

    function renderKycState(profile) {
        const stateBox = document.getElementById("kycStateMessage");
        const form = document.getElementById("kycForm");
        const submitButton = document.getElementById("kycSubmitButton");

        if (!stateBox || !form) return;

        const role = profile.role;
        const status = profile.status;

        if (role === "ADMIN") {
            stateBox.innerHTML = `
                <strong>Admin account</strong>
                <p class="muted">
                    Admins manage KYC requests from the admin dashboard and do not need to apply as Jastiper.
                </p>
                <a class="btn ghost" href="/admin">Open admin dashboard</a>
            `;
            form.classList.add("hidden");
            return;
        }

        if (role === "JASTIPER") {
            stateBox.innerHTML = `
                <strong>Already verified as Jastiper</strong>
                <p class="muted">
                    Your account can act as a Jastiper. You can create catalogs and process incoming orders.
                </p>
            `;
            form.classList.add("hidden");
            return;
        }

        if (status === "PENDING_VERIFICATION") {
            stateBox.innerHTML = `
                <strong>KYC is pending</strong>
                <p class="muted">
                    Your application has been submitted. Please wait for admin approval or rejection.
                </p>
            `;
            form.classList.add("hidden");
            return;
        }

        if (status === "BANNED") {
            stateBox.innerHTML = `
                <strong>Account is banned</strong>
                <p class="muted">
                    This account cannot submit KYC or access normal user features until an admin reactivates it.
                </p>
            `;
            form.classList.add("hidden");
            return;
        }

        stateBox.innerHTML = `
            <strong>Eligible to apply</strong>
            <p class="muted">
                Submit your identity name and optional social media link. Admin approval will upgrade you to JASTIPER.
            </p>
        `;

        form.classList.remove("hidden");

        if (submitButton) {
            submitButton.disabled = false;
        }
    }

    function setupPublicProfileForm() {
        const form = document.getElementById("publicProfileForm");
        if (!form) return;

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const username = document.getElementById("publicProfileUsername")?.value?.trim();
            if (!username) {
                renderPublicProfileError("Please enter a username.");
                return;
            }

            try {
                const publicProfile = await requestJson(`/api/users/${encodeURIComponent(username)}`);
                renderPublicProfile(publicProfile);
            } catch (error) {
                renderPublicProfileError(error.message || "Public profile not found.");
            }
        });
    }

    function renderPublicProfile(profile) {
        const el = document.getElementById("publicProfileResult");
        if (!el) return;

        const role = profile.role || "-";
        const status = profile.status || "-";
        const isJastiper = role === "JASTIPER";

        const averageRating = pickValue(profile, ["averageRating", "avgRating"], "-");
        const ratingCount = pickValue(profile, ["ratingCount", "totalReviews"], "-");
        const successfulTransactions = pickValue(profile, ["successfulTransactions", "successTransactionCount"], "-");
        const totalTransactions = pickValue(profile, ["totalTransactions", "transactionCount"], "-");
        const successRate = pickValue(profile, ["successRate"], "-");

        el.innerHTML = `
            <article class="profile-public-card">
                <span class="pill">${escapeProfileHtml(role)}</span>
                <h3>${escapeProfileHtml(profile.fullName || profile.username || "User profile")}</h3>
                <p class="muted">
                    @${escapeProfileHtml(profile.username || "-")} · ${escapeProfileHtml(status)}
                </p>

                ${isJastiper ? `
                    <div class="profile-stat-grid">
                        ${statItem("Average rating", averageRating)}
                        ${statItem("Rating count", ratingCount)}
                        ${statItem("Successful transactions", successfulTransactions)}
                        ${statItem("Total transactions", totalTransactions)}
                        ${statItem("Success rate", formatSuccessRate(successRate))}
                    </div>
                ` : `
                    <p class="muted">
                        This is a Titiper profile. Jastiper reputation statistics appear after approval and transactions.
                    </p>
                `}
            </article>
        `;
    }

    function renderPublicProfileError(message) {
        const el = document.getElementById("publicProfileResult");
        if (!el) return;

        el.innerHTML = `
            <div class="notice error">
                ${escapeProfileHtml(message)}
            </div>
        `;
    }

    function detailItem(label, value) {
        return `
            <div class="profile-detail-item">
                <span>${escapeProfileHtml(label)}</span>
                <strong>${escapeProfileHtml(value ?? "-")}</strong>
            </div>
        `;
    }

    function statItem(label, value) {
        return `
            <div class="profile-stat-item">
                <span>${escapeProfileHtml(label)}</span>
                <strong>${escapeProfileHtml(value ?? "-")}</strong>
            </div>
        `;
    }

    function pickValue(object, keys, fallback) {
        for (const key of keys) {
            if (object && object[key] !== undefined && object[key] !== null) {
                return object[key];
            }
        }
        return fallback;
    }

    function formatSuccessRate(value) {
        if (value === "-" || value === null || value === undefined) return "-";

        const number = Number(value);
        if (!Number.isFinite(number)) return String(value);

        if (number <= 1) {
            return `${Math.round(number * 100)}%`;
        }

        return `${number}%`;
    }

    function setProfileNotice(type, message) {
        const el = document.getElementById("profileNotice");
        if (!el) return;

        if (!message) {
            el.className = "hidden";
            el.textContent = "";
            return;
        }

        el.className = `notice ${type || "info"}`;
        el.textContent = message;
    }

    function escapeProfileHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();