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
                    const text = await response.text();
                    throw new Error(text || "Invalid email or password.");
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