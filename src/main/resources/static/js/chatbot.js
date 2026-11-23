(() => {
    "use strict";

    const panel = document.querySelector("[data-chatbot]");
    const launcher = document.querySelector("[data-chatbot-launch]");
    if (!panel || !launcher) return;

    // El topbar usa backdrop-filter y crea un containing block. Sacar el panel
    // al body conserva su posicionamiento fixed respecto del viewport.
    document.body.append(panel);

    const closeButton = panel.querySelector("[data-chatbot-close]");
    const form = panel.querySelector("[data-chatbot-form]");
    const input = panel.querySelector("[data-chatbot-input]");
    const sendButton = panel.querySelector("[data-chatbot-send]");
    const messages = panel.querySelector("[data-chatbot-messages]");
    const suggestions = panel.querySelector("[data-chatbot-suggestions]");
    const status = panel.querySelector("[data-chatbot-status]");
    const csrfToken = panel.dataset.csrfToken;
    const csrfHeader = panel.dataset.csrfHeader;
    const modeLabel = panel.querySelector("[data-chatbot-mode]");
    fetch(panel.dataset.statusUrl, { headers: { Accept: "application/json" } })
        .then(response => response.ok ? response.json() : Promise.reject())
        .then(({ modo }) => {
            modeLabel.textContent = modo === "gemini" ? "Gemini" : "Guía local";
        })
        .catch(() => {});
    let historyLoaded = false;
    let busy = false;

    const requestHeaders = () => {
        const headers = { "Content-Type": "application/json", "Accept": "application/json" };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        return headers;
    };

    const setButtonState = (state) => {
        sendButton.dataset.state = state;
        sendButton.disabled = state === "loading";
        sendButton.setAttribute("aria-busy", String(state === "loading"));
    };

    const scrollToLatest = () => {
        messages.scrollTop = messages.scrollHeight;
    };

    const appendInlineMarkdown = (parent, source) => {
        const tokenPattern = /(`([^`\n]+)`|\*\*([^*\n]+)\*\*|__([^_\n]+)__|\*([^*\n]+)\*|_([^_\n]+)_|\[([^\]\n]+)]\((https?:\/\/[^)\s]+|\/[^)\s]*)\))/g;
        let cursor = 0;

        for (const match of source.matchAll(tokenPattern)) {
            if (match.index > cursor) {
                parent.append(document.createTextNode(source.slice(cursor, match.index)));
            }

            if (match[2] !== undefined) {
                const code = document.createElement("code");
                code.textContent = match[2];
                parent.append(code);
            } else if (match[3] !== undefined || match[4] !== undefined) {
                const strong = document.createElement("strong");
                appendInlineMarkdown(strong, match[3] ?? match[4]);
                parent.append(strong);
            } else if (match[5] !== undefined || match[6] !== undefined) {
                const emphasis = document.createElement("em");
                appendInlineMarkdown(emphasis, match[5] ?? match[6]);
                parent.append(emphasis);
            } else {
                const link = document.createElement("a");
                link.href = match[8];
                appendInlineMarkdown(link, match[7]);
                if (match[8].startsWith("http")) {
                    link.target = "_blank";
                    link.rel = "noopener noreferrer";
                }
                parent.append(link);
            }

            cursor = match.index + match[0].length;
        }

        if (cursor < source.length) {
            parent.append(document.createTextNode(source.slice(cursor)));
        }
    };

    const isBlockStart = (line) => /^(#{1,3}\s+|```|>\s?|\s*[-+*]\s+|\s*\d+\.\s+)/.test(line);

    const renderMarkdown = (source) => {
        const container = document.createElement("div");
        container.className = "chatbot-message-content";
        const lines = String(source ?? "").replace(/\r\n?/g, "\n").split("\n");
        let index = 0;

        while (index < lines.length) {
            const line = lines[index];
            if (!line.trim()) {
                index += 1;
                continue;
            }

            const fence = line.match(/^```([\w-]*)\s*$/);
            if (fence) {
                const codeLines = [];
                index += 1;
                while (index < lines.length && !/^```\s*$/.test(lines[index])) {
                    codeLines.push(lines[index]);
                    index += 1;
                }
                if (index < lines.length) index += 1;

                const pre = document.createElement("pre");
                const code = document.createElement("code");
                if (fence[1]) code.dataset.language = fence[1];
                code.textContent = codeLines.join("\n");
                pre.append(code);
                container.append(pre);
                continue;
            }

            const heading = line.match(/^(#{1,3})\s+(.+)$/);
            if (heading) {
                const title = document.createElement(`h${heading[1].length + 2}`);
                appendInlineMarkdown(title, heading[2]);
                container.append(title);
                index += 1;
                continue;
            }

            const unordered = line.match(/^\s*[-+*]\s+(.+)$/);
            const ordered = line.match(/^\s*\d+\.\s+(.+)$/);
            if (unordered || ordered) {
                const list = document.createElement(unordered ? "ul" : "ol");
                const pattern = unordered ? /^\s*[-+*]\s+(.+)$/ : /^\s*\d+\.\s+(.+)$/;
                while (index < lines.length) {
                    const itemMatch = lines[index].match(pattern);
                    if (!itemMatch) break;
                    const item = document.createElement("li");
                    appendInlineMarkdown(item, itemMatch[1]);
                    list.append(item);
                    index += 1;
                }
                container.append(list);
                continue;
            }

            if (/^>\s?/.test(line)) {
                const quoteLines = [];
                while (index < lines.length && /^>\s?/.test(lines[index])) {
                    quoteLines.push(lines[index].replace(/^>\s?/, ""));
                    index += 1;
                }
                const quote = document.createElement("blockquote");
                appendInlineMarkdown(quote, quoteLines.join(" "));
                container.append(quote);
                continue;
            }

            const paragraphLines = [line.trim()];
            index += 1;
            while (index < lines.length && lines[index].trim() && !isBlockStart(lines[index])) {
                paragraphLines.push(lines[index].trim());
                index += 1;
            }
            const paragraph = document.createElement("p");
            appendInlineMarkdown(paragraph, paragraphLines.join(" "));
            container.append(paragraph);
        }

        return container;
    };

    const appendMessage = (role, content, extraClass = "") => {
        const article = document.createElement("article");
        article.className = `chatbot-message chatbot-message-${role} ${extraClass}`.trim();

        const author = document.createElement("span");
        author.className = "chatbot-message-author";
        author.textContent = role === "user" ? "Tú" : "SkillBridge AI";

        let copy;
        if (role === "assistant") {
            copy = renderMarkdown(content);
        } else {
            copy = document.createElement("p");
            copy.textContent = content;
        }

        article.append(author, copy);
        messages.append(article);
        scrollToLatest();
        return article;
    };

    const showTyping = () => {
        const article = document.createElement("article");
        article.className = "chatbot-message chatbot-message-assistant chatbot-message-typing";
        article.dataset.chatbotTyping = "true";

        const spinner = document.createElement("span");
        spinner.className = "chatbot-typing-dots";
        spinner.setAttribute("aria-hidden", "true");

        const copy = document.createElement("span");
        copy.textContent = "Consultando a Gemini…";

        article.append(spinner, copy);
        messages.append(article);
        scrollToLatest();
    };

    const hideTyping = () => {
        messages.querySelector("[data-chatbot-typing]")?.remove();
    };

    const loadHistory = async () => {
        if (historyLoaded) return;
        historyLoaded = true;
        try {
            const response = await fetch("/api/chatbot/historial", {
                headers: { "Accept": "application/json" }
            });
            if (!response.ok) {
                historyLoaded = false;
                return;
            }

            const history = await response.json();
            if (!Array.isArray(history) || history.length === 0) return;

            messages.querySelector("[data-chatbot-welcome]")?.remove();
            history.forEach((message) => appendMessage(
                message.remitente === "usuario" ? "user" : "assistant",
                message.contenido
            ));
        } catch {
            historyLoaded = false;
            status.textContent = "No se pudo recuperar el historial";
        }
    };

    const openPanel = async () => {
        panel.hidden = false;
        panel.classList.remove("is-opening");
        void panel.offsetWidth;
        panel.classList.add("is-opening");
        launcher.setAttribute("aria-expanded", "true");
        await loadHistory();
        input.focus({ preventScroll: true });
        scrollToLatest();
    };

    const closePanel = () => {
        panel.hidden = true;
        panel.classList.remove("is-opening");
        launcher.setAttribute("aria-expanded", "false");
        launcher.focus({ preventScroll: true });
    };

    const sendQuestion = async (question) => {
        if (busy) return;
        const cleanQuestion = question.trim();
        if (!cleanQuestion) {
            input.setAttribute("aria-invalid", "true");
            status.textContent = "Escribe una pregunta antes de enviar";
            input.focus();
            return;
        }

        busy = true;
        input.removeAttribute("aria-invalid");
        input.value = "";
        appendMessage("user", cleanQuestion);
        setButtonState("loading");
        status.textContent = "Generando respuesta";
        suggestions.hidden = true;
        const typingTimer = window.setTimeout(showTyping, 150);

        try {
            const response = await fetch("/api/chatbot", {
                method: "POST",
                headers: requestHeaders(),
                body: JSON.stringify({ mensaje: cleanQuestion })
            });
            const body = await response.json().catch(() => ({}));
            if (!response.ok || !body.respuesta) {
                throw new Error(body.error || "El asistente no pudo responder.");
            }

            window.clearTimeout(typingTimer);
            hideTyping();
            appendMessage("assistant", body.respuesta);
            setButtonState("success");
            status.textContent = "Respuesta recibida";
            window.setTimeout(() => setButtonState("default"), 800);
        } catch (error) {
            window.clearTimeout(typingTimer);
            hideTyping();
            appendMessage("error", error.message, "chatbot-message-error");
            input.value = cleanQuestion;
            setButtonState("error");
            status.textContent = "Revisa la configuración e inténtalo nuevamente";
        } finally {
            busy = false;
            sendButton.disabled = false;
        }
    };

    launcher.addEventListener("click", () => panel.hidden ? openPanel() : closePanel());
    closeButton.addEventListener("click", closePanel);

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        sendQuestion(input.value);
    });

    input.addEventListener("input", () => {
        if (input.value.trim()) {
            input.removeAttribute("aria-invalid");
            if (!busy) {
                setButtonState("default");
                status.textContent = "Listo";
            }
        }
    });

    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    suggestions.addEventListener("click", (event) => {
        const button = event.target.closest("[data-chatbot-prompt]");
        if (!button) return;
        input.value = button.dataset.chatbotPrompt;
        form.requestSubmit();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !panel.hidden) closePanel();
    });
})();
