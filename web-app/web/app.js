
const $ = s => document.querySelector(s);
const content = $('#content');

const titles = {
    dashboard: 'Overview',
    farmers: 'Farmers',
    farms: 'Farms',
    crops: 'Crops',
    cultivations: 'Cultivations',
    fertilizers: 'Fertilizer inputs',
    irrigation: 'Irrigation',
    harvests: 'Harvests',
    reports: 'Reports'
};

const configs = {
    farmers: {
        title: 'Farmers',
        desc: 'Keep farmer profiles and contact details organised.',
        endpoint: 'farmers',
        cols: ['farmer_id', 'name', 'contact_number', 'address'],
        fields: [
            ['name', 'Farmer name', 'text'],
            ['contactNumber', 'Contact number', 'tel'],
            ['address', 'Address', 'text', 'full']
        ]
    },

    farms: {
        title: 'Farms',
        desc: 'Manage farm locations and their total area.',
        endpoint: 'farms',
        cols: ['farm_id', 'farmer_id', 'location', 'total_area'],
        fields: [
            ['farmerId', 'Farmer', 'select', '', 'farmers'],
            ['location', 'Farm location', 'text'],
            ['totalArea', 'Total area (acres)', 'number']
        ]
    },

    crops: {
        title: 'Crops',
        desc: 'Maintain the crops grown and their expected duration.',
        endpoint: 'crops',
        cols: ['crop_id', 'crop_name', 'crop_category', 'duration_days'],
        fields: [
            ['cropName', 'Crop name', 'text'],
            ['category', 'Category', 'select', '', 'Cereal|Pulse|Oilseed|Cash Crop|Vegetable|Millet'],
            ['durationDays', 'Duration (days)', 'number']
        ]
    },

    cultivations: {
        title: 'Cultivations',
        desc: 'Track crop cultivation by farm, area and season.',
        endpoint: 'cultivations',
        cols: ['cultivation_id', 'farm_id', 'crop_id', 'cultivation_date', 'cultivated_area', 'season'],
        fields: [
            ['farmId', 'Farm', 'select', '', 'farms'],
            ['cropId', 'Crop', 'select', '', 'crops'],
            ['cultivationDate', 'Cultivation date', 'date'],
            ['cultivatedArea', 'Cultivated area (acres)', 'number'],
            ['season', 'Season', 'select', '', 'Kharif|Rabi|Summer']
        ]
    },

    fertilizers: {
        title: 'Fertilizer inputs',
        desc: 'Record fertilizer type, quantity, cost and application date.',
        endpoint: 'fertilizers',
        cols: ['input_id', 'cultivation_id', 'fertilizer_type', 'quantity_kg', 'input_cost', 'application_date'],
        fields: [
            ['cultivationId', 'Cultivation', 'select', '', 'cultivations'],
            ['fertilizerType', 'Fertilizer type', 'text'],
            ['quantityKg', 'Quantity (kg)', 'number'],
            ['inputCost', 'Input cost (₹)', 'number'],
            ['applicationDate', 'Application date', 'date']
        ]
    },

    irrigation: {
        title: 'Irrigation',
        desc: 'Monitor water usage and irrigation methods.',
        endpoint: 'irrigation',
        cols: ['irrigation_id', 'cultivation_id', 'irrigation_date', 'water_quantity_l', 'irrigation_method'],
        fields: [
            ['cultivationId', 'Cultivation', 'select', '', 'cultivations'],
            ['irrigationDate', 'Irrigation date', 'date'],
            ['waterQuantity', 'Water quantity (litres)', 'number'],
            ['method', 'Method', 'select', '', 'Drip|Sprinkler|Flood|Furrow']
        ]
    },

    harvests: {
        title: 'Harvests',
        desc: 'Record harvested quantity, quality and selling price.',
        endpoint: 'harvests',
        cols: ['harvest_id', 'cultivation_id', 'harvest_date', 'quantity_kg', 'quality', 'selling_price'],
        fields: [
            ['cultivationId', 'Cultivation', 'select', '', 'cultivations'],
            ['harvestDate', 'Harvest date', 'date'],
            ['quantityKg', 'Quantity harvested (kg)', 'number'],
            ['quality', 'Quality', 'select', '', 'A|B|C'],
            ['sellingPrice', 'Selling price (₹ per kg)', 'number']
        ]
    }
};

let current = 'dashboard';
let allRows = [];

const API_BASE = 'https://agri-crop-system.onrender.com/api/';

$('#today').textContent = new Date().toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric'
});

$('#navigation').addEventListener('click', e => {
    const b = e.target.closest('[data-page]');

    if (!b) return;

    document.querySelectorAll('.nav-item').forEach(x => {
        x.classList.toggle('active', x === b);
    });

    loadPage(b.dataset.page);
});

/* ---------------- API FUNCTION ---------------- */

async function api(path, opts = {}) {
    const headers = { ...(opts.headers || {}) };

    // Add JSON Content-Type only when sending a request body.
    if (opts.body) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(API_BASE + path, {
        ...opts,
        headers
    });

    const responseText = await response.text();

    let data;

    try {
        data = JSON.parse(responseText);
    } catch {
        throw new Error(
            'Backend returned a non-JSON response: ' +
            responseText.slice(0, 150)
        );
    }

    if (!response.ok) {
        throw new Error(data.error || 'Request failed');
    }

    return data;
}

/* ---------------- FORMATTING ---------------- */

function money(v) {
    return '₹' + Number(v || 0).toLocaleString('en-IN', {
        maximumFractionDigits: 2
    });
}

function num(v) {
    return Number(v || 0).toLocaleString('en-IN', {
        maximumFractionDigits: 2
    });
}

function esc(v) {
    return String(v ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[c]));
}

function pretty(k) {
    return k.replaceAll('_', ' ').replace(/\b\w/g, x => x.toUpperCase());
}

function formatCell(k, v) {
    if (v == null) return '—';

    if (k.includes('cost') || k.includes('price')) {
        return money(v);
    }

    if (
        k.includes('area') ||
        k.includes('quantity') ||
        k.includes('water')
    ) {
        return num(v);
    }

    return esc(v);
}

/* ---------------- PAGE LOADING ---------------- */

async function loadPage(page) {
    if (!titles[page]) return;

    current = page;

    $('#page-title').textContent = titles[page];

    content.innerHTML = '<div class="loading">Loading…</div>';

    try {
        if (page === 'dashboard') {
            await dashboard();
        } else if (page === 'reports') {
            await reportsPage();
        } else {
            await tablePage(page);
        }
    } catch (e) {
        content.innerHTML = `
            <div class="panel empty">
                ${esc(e.message)}
                <br><br>
                Check the backend response and try again.
            </div>
        `;
    }
}

/* ---------------- DASHBOARD ---------------- */

async function dashboard() {
    const d = await api('dashboard');

    content.innerHTML = `
        <div class="page-intro">
            <div>
                <h2>Good farming starts with good data.</h2>
                <p>Here’s a snapshot of your agriculture operations.</p>
            </div>
            <button class="btn primary" id="quick-add">
                ＋ Add farmer
            </button>
        </div>

        <div class="stats">
            ${stat('♙', 'Total farmers', d.farmers, 'Registered profiles')}
            ${stat('▦', 'Total farms', d.farms, 'Managed farm locations')}
            ${stat('♧', 'Cultivations', d.cultivations, 'Registered crop cycles')}
            ${stat('▧', 'Harvested quantity', num(d.harvestKg) + ' kg', 'Total recorded production')}
        </div>

        <div class="dashboard-grid">
            <div class="panel">
                <div class="panel-head">
                    <div>
                        <h3>Yield by crop</h3>
                        <div class="panel-sub">
                            Harvested quantity per cultivated acre
                        </div>
                    </div>
                    <span class="stat-icon">↗</span>
                </div>

                <div class="chart-bars">
                    ${(d.cropYield || []).length
                        ? d.cropYield.map(x => `
                            <div class="bar-wrap">
                                <div
                                    class="bar"
                                    title="${num(x.yield)} kg/acre"
                                    style="height:${
                                        Math.max(
                                            4,
                                            Number(x.yield) /
                                            Math.max(
                                                ...d.cropYield.map(z => Number(z.yield) || 0),
                                                1
                                            ) * 100
                                        )
                                    }%"
                                ></div>
                                <div class="bar-label">${esc(x.crop)}</div>
                            </div>
                        `).join('')
                        : '<div class="empty">No harvest data yet</div>'
                    }
                </div>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <div>
                        <h3>Recent cultivation activity</h3>
                        <div class="panel-sub">
                            Latest registered cultivation records
                        </div>
                    </div>
                </div>

                <div class="recent-list">
                    ${(d.recent || []).length
                        ? d.recent.map(x => `
                            <div class="recent-row">
                                <div class="crop-symbol">♧</div>
                                <div>
                                    <b>${esc(x.crop_name)}</b>
                                    <small>
                                        ${esc(x.farmer_name)} · ${esc(x.season)}
                                    </small>
                                </div>
                                <strong>
                                    ${x.quantity_kg == null
                                        ? '—'
                                        : num(x.quantity_kg) + ' kg'
                                    }
                                </strong>
                            </div>
                        `).join('')
                        : '<div class="empty">No cultivation records yet</div>'
                    }
                </div>
            </div>
        </div>

        <div
            class="panel"
            style="margin-top:16px;display:flex;align-items:center;justify-content:space-between;gap:15px"
        >
            <div>
                <h3>Estimated recorded profit</h3>
                <div class="panel-sub">
                    Revenue less fertilizer input costs for harvested cultivations
                </div>
            </div>

            <div class="stat-value" style="color:#28734c">
                ${money(d.estimatedProfit)}
            </div>
        </div>
    `;

    $('#quick-add').onclick = () => openModal('farmers');
}

function stat(icon, label, value, note) {
    return `
        <div class="stat-card">
            <div class="stat-top">
                ${label}
                <span class="stat-icon">${icon}</span>
            </div>
            <div class="stat-value">${value}</div>
            <div class="stat-note">${note}</div>
        </div>
    `;
}

/* ---------------- TABLE PAGES ---------------- */

async function tablePage(page) {
    const c = configs[page];

    allRows = await api(c.endpoint);

    content.innerHTML = `
        <div class="page-intro">
            <div>
                <h2>${c.title}</h2>
                <p>${c.desc}</p>
            </div>

            <button class="btn primary" id="add-record">
                ＋ Add ${c.title.toLowerCase().replace(/s$/, '')}
            </button>
        </div>

        <div class="table-panel">
            <div class="table-toolbar">
                <input
                    class="search"
                    id="search"
                    placeholder="⌕  Search records…"
                >

                <span class="table-count">
                    ${allRows.length} record${allRows.length === 1 ? '' : 's'}
                </span>
            </div>

            <div class="table-scroll">
                <table>
                    <thead>
                        <tr>
                            ${c.cols.map(k => `<th>${pretty(k)}</th>`).join('')}
                            ${page === 'farmers' ? '<th>Actions</th>' : ''}
                        </tr>
                    </thead>

                    <tbody id="tbody"></tbody>
                </table>
            </div>
        </div>
    `;

    renderRows(page, allRows);

    $('#search').oninput = e => {
        const searchText = e.target.value.toLowerCase();

        const filtered = allRows.filter(r =>
            Object.values(r).some(v =>
                String(v ?? '').toLowerCase().includes(searchText)
            )
        );

        renderRows(page, filtered);
    };

    $('#add-record').onclick = () => openModal(page);
}

function renderRows(page, rows) {
    const c = configs[page];
    const body = $('#tbody');

    if (!rows.length) {
        body.innerHTML = `
            <tr>
                <td colspan="${c.cols.length + 1}" class="empty">
                    No records found.
                </td>
            </tr>
        `;
        return;
    }

    body.innerHTML = rows.map(r => `
        <tr>
            ${c.cols.map(k => `
                <td>${formatCell(k, r[k])}</td>
            `).join('')}

            ${page === 'farmers'
                ? `
                    <td>
                        <button
                            class="btn secondary"
                            data-delete="${r.farmer_id}"
                            style="padding:6px 9px"
                        >
                            Delete
                        </button>
                    </td>
                `
                : ''
            }
        </tr>
    `).join('');

    body.querySelectorAll('[data-delete]').forEach(b => {
        b.onclick = async () => {
            if (!confirm('Delete this farmer and all related farm records?')) {
                return;
            }

            try {
                await api('farmers/' + b.dataset.delete, {
                    method: 'DELETE'
                });

                toast('Farmer deleted');
                loadPage(page);
            } catch (e) {
                toast(e.message);
            }
        };
    });
}

/* ---------------- REPORTS ---------------- */

async function reportsPage() {
    const reports = [
        ['crop-yield', 'Crop-wise yield', 'Compare harvest volume and yield per acre.'],
        ['farm-production', 'Farm-wise production', 'Review production and revenue by farm.'],
        ['input-costs', 'Input costs', 'Review fertilizer quantities and costs by crop.'],
        ['seasonal', 'Seasonal summary', 'Compare harvest, revenue and profit by season.']
    ];

    content.innerHTML = `
        <div class="page-intro">
            <div>
                <h2>Performance reports</h2>
                <p>
                    Explore crop productivity, farm output and seasonal performance.
                </p>
            </div>
        </div>

        <div class="stats" style="grid-template-columns:repeat(2,minmax(0,1fr))">
            ${reports.map((r, i) => `
                <button
                    class="panel report-card"
                    data-report="${r[0]}"
                    style="text-align:left;cursor:pointer;border:1px solid var(--line)"
                >
                    <div class="stat-icon">
                        ${['♧', '▦', '◈', '◷'][i]}
                    </div>

                    <h3 style="margin:13px 0 5px">${r[1]}</h3>

                    <div class="panel-sub">${r[2]}</div>

                    <div
                        style="color:#28734c;font-size:11px;font-weight:700;margin-top:16px"
                    >
                        View report →
                    </div>
                </button>
            `).join('')}
        </div>

        <div id="report-result"></div>
    `;

    document.querySelectorAll('[data-report]').forEach(b => {
        b.onclick = () => showReport(b.dataset.report);
    });
}

async function showReport(name) {
    try {
        const rows = await api('reports/' + name);
        const target = $('#report-result');

        if (!Array.isArray(rows)) {
            throw new Error('Unexpected report data received from backend.');
        }

        if (!rows.length) {
            target.innerHTML = `
                <div class="panel empty" style="margin-top:16px">
                    No data available for this report yet.
                </div>
            `;
            return;
        }

        const cols = Object.keys(rows[0]);

        target.innerHTML = `
            <div class="table-panel" style="margin-top:17px">
                <div class="table-toolbar">
                    <b style="font:700 13px Manrope">
                        ${pretty(name)} report
                    </b>

                    <span class="table-count">
                        ${rows.length} result(s)
                    </span>
                </div>

                <div class="table-scroll">
                    <table>
                        <thead>
                            <tr>
                                ${cols.map(k => `<th>${pretty(k)}</th>`).join('')}
                            </tr>
                        </thead>

                        <tbody>
                            ${rows.map(r => `
                                <tr>
                                    ${cols.map(k => `
                                        <td>${formatCell(k, r[k])}</td>
                                    `).join('')}
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (e) {
        toast(e.message);
    }
}

/* ---------------- ADD RECORD MODAL ---------------- */

async function openModal(page) {
    const c = configs[page];

    $('#modal-title').textContent =
        'Add ' + c.title.toLowerCase().replace(/s$/, '');

    const holder = $('#form-fields');

    holder.innerHTML = '';

    for (const f of c.fields) {
        const [key, label, type, wide, opts] = f;

        let input = '';

        if (type === 'select') {
            let choices = [];

            if (opts && configs[opts]) {
                const data = await api(configs[opts].endpoint);

                choices = data.map(x => {
                    const idKey = Object.keys(x)[0];

                    return {
                        value: x[idKey],
                        label: Object.values(x).slice(0, 3).join(' · ')
                    };
                });
            } else {
                choices = (opts || '').split('|').map(x => ({
                    value: x,
                    label: x
                }));
            }

            input = `
                <select name="${key}" required>
                    <option value="">
                        Select ${label.toLowerCase()}
                    </option>

                    ${choices.map(x => `
                        <option value="${esc(x.value)}">
                            ${esc(x.label)}
                        </option>
                    `).join('')}
                </select>
            `;
        } else {
            input = `
                <input
                    name="${key}"
                    type="${type}"
                    ${type === 'number' ? 'min="0.01" step="any"' : ''}
                    ${type === 'tel' ? 'pattern="[6-9][0-9]{9}" maxlength="10"' : ''}
                    required
                >
            `;
        }

        holder.insertAdjacentHTML('beforeend', `
            <div class="field ${wide === 'full' ? 'full' : ''}">
                <label>${label}</label>
                ${input}
            </div>
        `);
    }

    $('#modal').classList.remove('hidden');

    $('#record-form').onsubmit = async e => {
        e.preventDefault();

        const data = Object.fromEntries(
            new FormData(e.target).entries()
        );

        for (const f of c.fields) {
            const key = f[0];

            if (f[2] === 'number') {
                data[key] = Number(data[key]);
            }

            if (f[2] === 'select' && configs[f[4]]) {
                data[key] = Number(data[key]);
            }
        }

        try {
            await api(c.endpoint, {
                method: 'POST',
                body: JSON.stringify(data)
            });

            closeModal();

            toast('Record saved successfully');

            loadPage(page);
        } catch (err) {
            toast(err.message);
        }
    };
}

/* ---------------- MODAL CONTROLS ---------------- */

function closeModal() {
    $('#modal').classList.add('hidden');
}

$('#close-modal').onclick = closeModal;
$('#cancel-modal').onclick = closeModal;

$('#modal').onclick = e => {
    if (e.target.id === 'modal') {
        closeModal();
    }
};

/* ---------------- TOAST MESSAGE ---------------- */

function toast(msg) {
    const t = $('#toast');

    t.textContent = msg;
    t.classList.add('show');

    setTimeout(() => {
        t.classList.remove('show');
    }, 2800);
}

/* ---------------- START APPLICATION ---------------- */

loadPage('dashboard');
