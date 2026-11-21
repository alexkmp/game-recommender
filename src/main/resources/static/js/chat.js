document.addEventListener("DOMContentLoaded", function () {
    const sendButton = document.getElementById("send-button");
    const chatInput = document.getElementById("chat-input");
    const messagesContainer = document.getElementById("messages");
    const themeToggle = document.getElementById("theme-toggle");

    /* ---------- Theme toggle (persisted) ---------- */
    const storedTheme = localStorage.getItem("theme");
    if (storedTheme === "light" || storedTheme === "dark") {
        document.body.dataset.theme = storedTheme;
    }

    if (themeToggle) {
        themeToggle.addEventListener("click", function () {
            const next = document.body.dataset.theme === "dark" ? "light" : "dark";
            document.body.dataset.theme = next;
            localStorage.setItem("theme", next);
        });
    }

    /* ---------- Responsive sidebar ---------- */
    function closeSidebar() {
        document.body.classList.remove("sidebar-open");
    }

    const mobileMenuBtn = document.getElementById("mobile-menu-btn");
    const sidebarBackdrop = document.getElementById("sidebar-backdrop");
    if (mobileMenuBtn) mobileMenuBtn.addEventListener("click", () => document.body.classList.add("sidebar-open"));
    if (sidebarBackdrop) sidebarBackdrop.addEventListener("click", closeSidebar);
    window.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeSidebar();
    });

    /* ---------- Auto-resizing textarea ---------- */
    if (chatInput) {
        const resize = () => {
            chatInput.style.height = "auto";
            chatInput.style.height = Math.min(chatInput.scrollHeight, 160) + "px";
        };
        chatInput.addEventListener("input", resize);
        resize();
    }

    /* ---------- Send message (SSE streaming) ---------- */
    function isStreaming() {
        return sendButton && sendButton.disabled;
    }

    function scrollToEnd() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    function createMentorMessage() {
        const aiDiv = document.createElement("div");
        aiDiv.className = "message mentor";
        // Add assistant avatar
        aiDiv.innerHTML = `<img class="avatar" src="/images/mentor.png" alt="Mentor">`;
        // Create a content element where we will insert the response.
        const aiBubble = document.createElement("div");
        aiBubble.className = "bubble";
        aiBubble.innerHTML = `<span class="typing-dots"><span></span><span></span><span></span></span>`;
        aiDiv.appendChild(aiBubble);
        messagesContainer.appendChild(aiDiv);
        scrollToEnd();
        return aiBubble;
    }

    function sendMessage() {
        if (!chatInput || isStreaming()) return;
        const prompt = chatInput.value.trim();
        if (!prompt) return;
        chatInput.value = "";
        chatInput.dispatchEvent(new Event("input"));

        // Add user message to the chat
        const userDiv = document.createElement("div");
        userDiv.className = "message user";
        userDiv.innerHTML = `<img class="avatar" src="/images/user.png" alt="User"><div class="bubble"></div>`;
        userDiv.querySelector(".bubble").textContent = prompt;
        messagesContainer.appendChild(userDiv);
        scrollToEnd();

        // Remove empty-state placeholder if present
        const emptyState = messagesContainer.querySelector(".empty-state");
        if (emptyState) emptyState.remove();

        const aiBubble = createMentorMessage();

        sendButton.disabled = true;

        const pathParts = window.location.pathname.split("/");
        const chatId = pathParts[pathParts.length - 1];
        const url = `/chat-stream/${chatId}?userPrompt=${encodeURIComponent(prompt)}`;

        const eventSource = new EventSource(url);
        let fullText = "";

        eventSource.onmessage = function (event) {
            const data = JSON.parse(event.data);
            const token = data.text;
            fullText += token;
            // Markdown to HTML (required marked.js)
            aiBubble.innerHTML = marked.parse(fullText);
            scrollToEnd();
        };

        eventSource.onerror = function () {
            eventSource.close();
            if (!fullText) {
                aiBubble.textContent = "Sorry, the connection was interrupted. Please try again.";
            }
            sendButton.disabled = false;
            chatInput.focus();
        };
    }

    if (sendButton) {
        sendButton.addEventListener("click", sendMessage);
    }

    if (chatInput) {
        chatInput.addEventListener("keydown", function (e) {
            if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }
});
