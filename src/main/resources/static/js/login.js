(function () {
    const TOKEN_KEY = "json_token";
    const PROFILE_KEY = "json_profile";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("loginForm");
        if (!form) return;

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const email = document.getElementById("loginEmail")?.value?.trim();
            const password = document.getElementById("loginPassword")?.value;

            if (!email || !password) {
                showLoginNotice("error", "Please enter both email and password.");
                return;
            }

            showLoginNotice("info", "Logging in...");

            try {
                const response = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ email, password })
                });

                if (!response.ok) {
                    const message = await readLoginError(response);
                    throw new Error(message || "Invalid email or password.");
                }

                const data = await response.json();

                if (!data.token) {
                    throw new Error("Login response did not contain a token.");
                }

                localStorage.setItem(TOKEN_KEY, data.token);
                localStorage.removeItem(PROFILE_KEY);

                showLoginNotice("success", "Login successful! Redirecting...");

                setTimeout(function () {
                    window.location.href = "/catalog";
                }, 500);
            } catch (error) {
                localStorage.removeItem(TOKEN_KEY);
                localStorage.removeItem(PROFILE_KEY);
                showLoginNotice("error", error.message || "Login failed.");
            }
        });
    });

    async function readLoginError(response) {
        const fallbackByStatus = {
            400: "Invalid login request.",
            401: "Invalid email or password.",
            403: "Account is banned.",
            404: "Account not found.",
            409: "Login conflict. Please try again."
        };

        try {
            const text = await response.text();

            if (!text) {
                return fallbackByStatus[response.status] || "Login failed.";
            }

            try {
                const json = JSON.parse(text);

                if (json.message) return cleanErrorMessage(json.message);
                if (json.error && json.error !== "Forbidden" && json.error !== "Unauthorized") {
                    return cleanErrorMessage(json.error);
                }

                return fallbackByStatus[response.status] || "Login failed.";
            } catch {
                return cleanErrorMessage(text);
            }
        } catch {
            return fallbackByStatus[response.status] || "Login failed.";
        }
    }

    function cleanErrorMessage(message) {
        if (!message) return "Login failed.";

        const text = String(message);

        if (text.includes("Account is banned")) {
            return "Account is banned.";
        }

        if (text.includes("Invalid email or password")) {
            return "Invalid email or password.";
        }

        if (text.includes("403")) {
            return "Account is banned.";
        }

        if (text.length > 160) {
            return "Login failed. Please check your account status or credentials.";
        }

        return text;
    }

    function showLoginNotice(type, message) {
        const el = document.getElementById("authNotice");
        if (!el) return;

        if (!message) {
            el.className = "hidden";
            el.textContent = "";
            return;
        }

        el.className = `notice ${type || "info"}`;
        el.textContent = message;
    }
})();