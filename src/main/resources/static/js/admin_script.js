let selectedCell = null;

window.onload = () => {
    loadDashboardStats();
    loadGroupsToSelect();
    showSection('dashboard-admin');
};

// Приховування контекстного меню при кліку будь-де
window.onclick = () => {
    const menu = document.getElementById('ctx-menu');
    if (menu) menu.style.display = 'none';
};

// 1. Завантаження статистики для дашборду
async function loadDashboardStats() {
    try {
        const response = await fetch('/api/admin/stats');
        const data = await response.json();
        document.getElementById('stat-total').innerText = data.totalDuties || 0;
        document.getElementById('stat-extra').innerText = data.extraDuties || 0;
        document.getElementById('stat-users').innerText = data.totalUsers || 0;
    } catch (e) { console.error("Stats fail:", e); }
}

// 2. Завантаження списку груп у селект
async function loadGroupsToSelect() {
    const groupSelect = document.getElementById('group-select');
    if (!groupSelect) return;
    try {
        const response = await fetch('/api/admin/groups');
        const groups = await response.json();
        groupSelect.innerHTML = '<option value="">-- Виберіть групу --</option>';
        groups.forEach(g => {
            let opt = document.createElement('option');
            opt.value = g; opt.innerText = "Група " + g;
            groupSelect.appendChild(opt);
        });
    } catch (e) { console.error("Groups fail:", e); }
}

// 3. Перемикання секцій (Дашборд / Керування)
function showSection(id) {
    document.querySelectorAll('.page-section').forEach(s => s.style.display = 'none');
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const target = document.getElementById(id);
    if (target) target.style.display = 'block';

    const navItem = document.querySelector(`.nav-item[onclick*="${id}"]`);
    if (navItem) navItem.classList.add('active');

    if (id === 'manage-duties') loadTableData();
}

// 4. Основна функція побудови таблиці
async function loadTableData() {
    const groupSelect = document.getElementById('group-select');
    const groupName = groupSelect.value;
    const body = document.getElementById('excel-body');
    const head = document.getElementById('excel-head');

    if (!groupName) {
        body.innerHTML = '<tr><td colspan="35" style="padding:50px; color:gray; text-align:center;">Виберіть групу для відображення графіка</td></tr>';
        return;
    }

    const monthVal = document.getElementById('month-select').value;
    const [year, month] = monthVal.split('-').map(Number);
    const daysInMonth = new Date(year, month, 0).getDate();

    // Малюємо шапку
    head.innerHTML = `<th>ПІБ Курсанта & Ліміт</th><th>∑</th>`;
    for (let i = 1; i <= daysInMonth; i++) {
        let th = document.createElement('th');
        th.innerText = i;
        let date = new Date(year, month - 1, i);
        if (date.getDay() === 0 || date.getDay() === 6) th.classList.add('weekend-header');
        head.appendChild(th);
    }

    try {
        const response = await fetch(`/api/admin/group-data?groupName=${encodeURIComponent(groupName)}&year=${year}&month=${month}`);
        const students = await response.json();

        body.innerHTML = '';
        if (students.length === 0) {
            body.innerHTML = `<tr><td colspan="${daysInMonth + 2}" style="padding:30px; text-align:center;">Курсантів не знайдено</td></tr>`;
            return;
        }

        students.forEach(student => {
            const tr = document.createElement('tr');
            tr.dataset.login = student.login; // Зберігаємо логін для надійності

            const limitLabel = (student.limit !== null && student.limit !== -1) ? ` <small style="color:blue">[${student.limit}]</small>` : "";
            const displayName = (student.pib || `Логін: ${student.login}`) + limitLabel;

            tr.innerHTML = `<td>${displayName}</td><td class="row-sum">0</td>`;

            for (let d = 1; d <= daysInMonth; d++) {
                const td = document.createElement('td');
                td.className = 'cell-duty';

                let date = new Date(year, month - 1, d);
                if (date.getDay() === 0 || date.getDay() === 6) td.classList.add('weekend-cell');

                if (student.days && student.days[d]) {
                    const status = student.days[d];
                    td.innerText = status;
                    td.classList.add(`type-${status.toLowerCase()}`);
                }

                td.oncontextmenu = (e) => openCtxMenu(e, td);
                td.onclick = (e) => openCtxMenu(e, td);
                tr.appendChild(td);
            }
            body.appendChild(tr);
            updateRowStats(tr);
        });
    } catch (e) {
        console.error("Load table error:", e);
        body.innerHTML = '<tr><td colspan="35" style="color:red; padding:20px;">Помилка з\'єднання</td></tr>';
    }
}

// 5. Оновлення суми нарядів у рядку
function updateRowStats(tr) {
    const cells = tr.querySelectorAll('.cell-duty');
    let count = 0;
    cells.forEach(c => {
        const text = c.innerText.toUpperCase();
        if(text === 'П' || text === 'Ш' || (text.length > 0 && text !== 'З')) count++;
    });
    const sumCell = tr.querySelector('.row-sum');
    if (sumCell) sumCell.innerText = count;
}

// 6. Контекстне меню
function openCtxMenu(e, td) {
    e.preventDefault();
    e.stopPropagation();
    selectedCell = td;
    const menu = document.getElementById('ctx-menu');
    menu.style.display = 'block';
    menu.style.left = e.pageX + 'px';
    menu.style.top = e.pageY + 'px';
}

// 7. Призначення статусу клітинці
async function applyStatus(status) {
    if (!selectedCell) return;

    const tr = selectedCell.closest('tr');
    const login = tr.dataset.login;

    // ВАЖЛИВО: day = cellIndex - 1 (якщо ПІБ=0, ∑=1, то перший день це індекс 2. Значить cellIndex - 1)
    // Якщо у вас ПІБ і ∑, то day = selectedCell.cellIndex - 1;
    const day = selectedCell.cellIndex - 1;

    const monthVal = document.getElementById('month-select').value;
    const [year, month] = monthVal.split('-').map(Number);

    try {
        const response = await fetch('/api/admin/update-cell', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                login: login, day: day, year: year, month: month, status: status
            })
        });

        if (response.ok) {
            selectedCell.innerText = status;
            selectedCell.className = 'cell-duty';

            // Повертаємо підсвітку вихідного
            let date = new Date(year, month - 1, day);
            if (date.getDay() === 0 || date.getDay() === 6) selectedCell.classList.add('weekend-cell');

            if (status) selectedCell.classList.add(`type-${status.toLowerCase()}`);

            updateRowStats(tr);
            document.getElementById('ctx-menu').style.display = 'none';
        }
    } catch (e) { console.error("Update cell fail:", e); }
}

// 8. Авторозподіл
async function autoDistribute() {
    const groupName = document.getElementById('group-select').value;
    const monthVal = document.getElementById('month-select').value;
    const globalLimit = document.getElementById('global-limit')?.value || 4;

    if (!groupName) { alert("Виберіть групу!"); return; }
    if (!confirm("Запустити авторозподіл? Існуючі наряди (П) будуть видалені.")) return;

    const [year, month] = monthVal.split('-').map(Number);
    const btn = document.querySelector('button[onclick="autoDistribute()"]');
    btn.disabled = true;
    btn.innerHTML = '...';

    try {
        const response = await fetch('/api/admin/auto-distribute', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ groupName, year, month, globalLimit: parseInt(globalLimit) })
        });

        if (response.ok) {
            alert("Успішно!");
            await loadTableData();
        } else {
            alert("Помилка: " + await response.text());
        }
    } catch (e) { alert("Помилка з'єднання"); }
    finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-magic"></i> Авторозподіл';
    }
}

// 9. Очищення місяця
async function clearMonthData() {
    const groupName = document.getElementById('group-select').value;
    const monthVal = document.getElementById('month-select').value;
    if(!groupName || !confirm("Очистити всі наряди групи за цей місяць?")) return;

    const [year, month] = monthVal.split('-').map(Number);
    try {
        await fetch('/api/admin/clear-month', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ groupName, year, month })
        });
        loadTableData();
    } catch (e) { console.error("Clear error"); }
}

// 10. Модалка лімітів
function openLimitModal() {
    const groupName = document.getElementById('group-select').value;
    const studentSelect = document.getElementById('student-limit-select');
    if (!groupName) { alert("Виберіть групу!"); return; }

    const monthVal = document.getElementById('month-select').value;
    const [year, month] = monthVal.split('-').map(Number);

    fetch(`/api/admin/group-data?groupName=${encodeURIComponent(groupName)}&year=${year}&month=${month}`)
        .then(res => res.json())
        .then(students => {
            studentSelect.innerHTML = '<option value="">-- Оберіть курсанта --</option>';
            students.forEach(s => {
                let opt = document.createElement('option');
                opt.value = s.login;
                opt.innerText = s.pib || s.login;
                opt.dataset.limit = (s.limit !== null) ? s.limit : -1;
                studentSelect.appendChild(opt);
            });
            document.getElementById('limit-modal').style.display = 'flex';
        });

    studentSelect.onchange = () => {
        const selected = studentSelect.options[studentSelect.selectedIndex];
        document.getElementById('limit-value').value = selected.dataset.limit;
    };
}

function closeLimitModal() { document.getElementById('limit-modal').style.display = 'none'; }

async function saveStudentLimit() {
    const login = document.getElementById('student-limit-select').value;
    const limit = document.getElementById('limit-value').value;
    if (!login) return;

    try {
        const response = await fetch('/api/admin/update-limit', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ login: login, limit: parseInt(limit) })
        });
        if (response.ok) { closeLimitModal(); loadTableData(); }
    } catch (e) { alert("Помилка збереження"); }
}

// 11. Власний статус
function promptCustomStatus() {
    const status = prompt("Введіть абревіатуру (напр. КПП, ЧД, ПАТ):");
    if (status) applyStatus(status.toUpperCase().substring(0, 3));
}