const NS = {
    token: () => localStorage.getItem('ns_token'),

    esc: (s) =>
        String(s ?? '').replace(
            /[&<>"']/g,
            (c) => ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            }[c])
        ),

    async api(path, opts = {}) {
        const h = {};

        if (NS.token()) {
            h.Authorization = 'Bearer ' + NS.token();
        }

        let body = opts.body;

        if (body && !(body instanceof FormData)) {
            h['Content-Type'] = 'application/json';
            body = JSON.stringify(body);
        }

        const r = await fetch('/api' + path, {
            method: opts.method || 'GET',
            headers: h,
            body
        });

        const d = await r.json().catch(() => ({}));

        if (r.status === 401 && NS.token()) {
            NS.logout();
        }

        if (!r.ok) {
            throw new Error(d.error || 'Something went wrong');
        }

        return d;
    },

    logout() {
        localStorage.removeItem('ns_token');
        location.href = '/login.html';
    },

    toast(msg, err) {
        const t = document.createElement('div');

        t.className = 'toast' + (err ? ' err' : '');

        t.textContent = msg;

        document.body.appendChild(t);

        setTimeout(() => t.remove(), 2800);
    },

    nav: [
        ['dashboard', '🏠', 'Home'],
        ['scan', '📸', 'Scan'],
        ['history', '🕘', 'History'],
        ['progress', '📈', 'Progress'],
        ['mealplan', '🍽️', 'Meal plan'],
        ['water', '💧', 'Water'],
        ['goals', '🎯', 'Goals'],
        ['reviews', '⭐', 'Reviews'],
        ['profile', '👤', 'Profile'],
        ['settings', '⚙️', 'Settings'],
        ['help', '💬', 'Help']
    ],

    async shell(active, title, subtitle = '') {
        if (!NS.token()) {
            location.href = '/login.html';
            return;
        }

        const me = await NS.api('/me');

        NS.me = me;

        const view = document.getElementById('view');

        const links = NS.nav
            .map(
                ([p, i, l]) =>
                    `<a href="/${p}.html" class="${p === active ? 'on' : ''}">
                        <span>${i}</span>
                        ${l}
                    </a>`
            )
            .join('');

        const scripts = [...document.querySelectorAll('script')];

        document.body.insertAdjacentHTML(
            'afterbegin',
            `<div class="shell">

                <aside class="side">

                    <a class="brand" href="/dashboard.html">
                        <i>🌿</i>
                        NutriScan AI
                    </a>

                    <nav class="nav">
                        ${links}
                    </nav>

                    <div class="grow"></div>

                    <button class="btn danger sm lo" id="lo">
                        Log out
                    </button>

                </aside>

                <main class="main">

                    <header class="top">

                        <div>
                            <h1>${title}</h1>
                            <p>${subtitle}</p>
                        </div>

                        <a
                            href="/profile.html"
                            class="avatar"
                            title="${NS.esc(me.name)}"
                        >
                            ${NS.esc(me.name[0].toUpperCase())}
                        </a>

                    </header>

                    <div id="slot"></div>

                </main>

            </div>`
        );

        document
            .getElementById('slot')
            .appendChild(view);

        view.classList.remove('hide');

        document.getElementById('lo').onclick = NS.logout;

        return me;
    },

    bars(el, days, key, target) {
        const max =
            Math.max(
                target || 0,
                ...days.map((d) => d[key]),
                1
            ) * 1.1;

        el.className = 'chart';

        el.innerHTML = days
            .map(
                (d, i) =>
                    `<div class="col">

                        <small>
                            ${d[key] || ''}
                        </small>

                        <div class="plot">

                            <div
                                class="b ${i === days.length - 1 ? 'today' : ''}"
                                style="height:${(d[key] / max) * 100}%"
                            ></div>

                        </div>

                        <span>
                            ${d.label}
                        </span>

                    </div>`
            )
            .join('');
    },

    donut(el, p, c, f) {
        const t =
            p * 4 +
            c * 4 +
            f * 9 ||
            1;

        const a =
            (p * 4 / t) * 100;

        const b =
            a +
            (c * 4 / t) * 100;

        el.innerHTML =
            `<div
                class="donut"
                style="
                    background:
                    conic-gradient(
                        var(--coral) 0 ${a}%,
                        var(--sky) ${a}% ${b}%,
                        var(--leaf) ${b}% 100%
                    )
                "
            >
                <div>
                    ${Math.round(
                        p * 4 +
                        c * 4 +
                        f * 9
                    )}
                    <br>
                    <small>kcal</small>
                </div>
            </div>

            <div class="legend">

                <span>
                    🔴 Protein
                </span>

                <span>
                    🔵 Carbs
                </span>

                <span>
                    🟢 Fat
                </span>

            </div>`;
    },

    scanRow(s) {
        return `
            <a
                class="row"
                href="/result.html?id=${s.id}"
            >

                ${
                    s.image
                        ? `<img src="${s.image}" alt="">`
                        : `<div class="thumb">
                            ${s.emoji}
                           </div>`
                }

                <div class="grow">

                    <b>
                        ${NS.esc(s.name)}
                    </b>

                    <br>

                    <small>
                        ${
                            new Date(
                                s.createdAt
                            ).toLocaleString(
                                [],
                                {
                                    dateStyle: 'medium',
                                    timeStyle: 'short'
                                }
                            )
                        }
                    </small>

                </div>

                <span class="pill">
                    ${s.calories} kcal
                </span>

            </a>
        `;
    }
};