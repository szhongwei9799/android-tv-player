let taskList = [];

async function loadTaskList() {
    const el = document.getElementById('taskList');
    el.innerHTML = '<div class="loading">加载定时任务...</div>';
    try {
        const res = await utils.request('/tasks');
        taskList = res.data || [];
        if (taskList.length === 0) {
            el.innerHTML = '<div class="empty-state"><div class="empty-icon">⏰</div><p>暂无定时任务</p><p style="font-size:12px;margin-top:4px;">点击右上角「新建任务」创建</p></div>';
        } else {
            renderTaskList();
        }
    } catch (e) {
        el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>加载失败: ${e.message}</p></div>`;
        showToast(e.message, 'error');
    }
}

function renderTaskList() {
    const el = document.getElementById('taskList');
    el.innerHTML = taskList.map(t => `
        <div class="task-item">
            <div class="task-toggle ${t.isEnabled ? 'active' : ''}" onclick="toggleTask(${t.id})"></div>
            <div class="task-info">
                <div class="task-name">${escHtml(t.name)}</div>
                <div class="task-schedule">${getTaskTypeLabel(t.type)} · ${t.timeOfDay || '-'}</div>
            </div>
            <div class="task-actions">
                <button class="btn btn-ghost" onclick="editTask(${t.id})">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="deleteTask(${t.id})">删除</button>
            </div>
        </div>
    `).join('');
}

function getTaskTypeLabel(type) {
    return { 'PLAY':'播放', 'STOP':'停止', 'POWER_OFF':'关机', 'SWITCH_SOURCE':'切换信号源' }[type] || type;
}

async function toggleTask(id) {
    try {
        await utils.request(`/tasks/${id}/toggle`, { method: 'PUT' });
        loadTaskList();
    } catch (e) {
        showToast('操作失败: ' + e.message, 'error');
    }
}

function showAddTaskModal() {
    showModal('新建定时任务', `
        <div class="form-row"><label>任务名称</label><input type="text" id="taskName" placeholder="例如: 上午播放"></div>
        <div class="form-row"><label>任务类型</label>
            <select id="taskType" onchange="togglePlaylistSelection()">
                <option value="PLAY">播放</option><option value="STOP">停止</option><option value="POWER_OFF">关机</option>
            </select>
        </div>
        <div id="playlistSelection" class="form-row"><label>关联播放列表</label><select id="taskPlaylist"><option value="">选择播放列表...</option></select></div>
        <div class="form-row"><label>执行时间</label><input type="time" id="taskTime" value="08:00"></div>
        <div class="form-row"><label>重复</label>
            <div style="display:flex;gap:16px;">
                <label style="display:flex;align-items:center;gap:4px;cursor:pointer;"><input type="radio" name="taskRepeat" value="daily" checked> 每天</label>
                <label style="display:flex;align-items:center;gap:4px;cursor:pointer;"><input type="radio" name="taskRepeat" value="weekly"> 每周</label>
            </div>
            <div id="weekdaySelection" style="display:none;margin-top:8px;display:flex;gap:6px;flex-wrap:wrap;">
                ${['周一','周二','周三','周四','周五','周六','周日'].map((d,i) =>
                    `<label style="display:flex;align-items:center;gap:3px;cursor:pointer;font-size:13px;font-weight:400;"><input type="checkbox" value="${i+1}"> ${d}</label>`
                ).join('')}
            </div>
        </div>
        <button class="btn btn-primary" onclick="createTask()">创建</button>
    `);
    loadPlaylistsForTask();
    document.querySelectorAll('input[name="taskRepeat"]').forEach(r => {
        r.addEventListener('change', () => {
            document.getElementById('weekdaySelection').style.display = document.querySelector('input[name="taskRepeat"]:checked').value === 'weekly' ? 'flex' : 'none';
        });
    });
    document.getElementById('weekdaySelection').style.display = 'none';
}

function togglePlaylistSelection() {
    document.getElementById('playlistSelection').style.display = document.getElementById('taskType').value === 'PLAY' ? 'block' : 'none';
}

async function loadPlaylistsForTask() {
    try {
        const res = await utils.request('/playlists');
        const pl = res.data || [];
        document.getElementById('taskPlaylist').innerHTML = '<option value="">选择播放列表...</option>' +
            pl.map(p => `<option value="${p.id}">${escHtml(p.name)}</option>`).join('');
    } catch (e) { /* silent */ }
}

async function createTask() {
    const name = document.getElementById('taskName').value.trim();
    if (!name) { showToast('请输入任务名称', 'error'); return; }
    const type = document.getElementById('taskType').value;
    const playlistId = document.getElementById('taskPlaylist').value;
    const timeOfDay = document.getElementById('taskTime').value;
    const repeatType = document.querySelector('input[name="taskRepeat"]:checked').value;
    let daysOfWeek = null;
    if (repeatType === 'weekly') {
        daysOfWeek = Array.from(document.querySelectorAll('#weekdaySelection input:checked')).map(c => c.value).join(',');
    }
    try {
        await utils.request('/tasks', { method: 'POST', body: JSON.stringify({
            name, type, playlistId: playlistId ? parseInt(playlistId) : null,
            timeOfDay, daysOfWeek
        })});
        closeModal();
        loadTaskList();
        showToast('创建成功');
    } catch (e) {
        showToast('创建失败: ' + e.message, 'error');
    }
}

function editTask(id) {
    const t = taskList.find(x => x.id === id);
    if (!t) return;
    showModal('编辑定时任务', `
        <div class="form-row"><label>任务名称</label><input type="text" id="editTaskName" value="${escHtml(t.name)}"></div>
        <div class="form-row"><label>任务类型</label>
            <select id="editTaskType" onchange="toggleEditPlaylistSelection()">
                ${['PLAY','STOP','POWER_OFF'].map(v => `<option value="${v}" ${t.type===v?'selected':''}>${getTaskTypeLabel(v)}</option>`).join('')}
            </select>
        </div>
        <div id="editPlaylistSelection" class="form-row" style="display:${t.type==='PLAY'?'block':'none'}"><label>关联播放列表</label><select id="editTaskPlaylist"><option value="">选择播放列表...</option></select></div>
        <div class="form-row"><label>执行时间</label><input type="time" id="editTaskTime" value="${t.timeOfDay || '08:00'}"></div>
        <button class="btn btn-primary" onclick="updateTask(${id})">保存</button>
    `);
    loadPlaylistsForEditTask(t.playlistId);
}

async function loadPlaylistsForEditTask(selectedId) {
    try {
        const res = await utils.request('/playlists');
        const pl = res.data || [];
        document.getElementById('editTaskPlaylist').innerHTML = '<option value="">选择播放列表...</option>' +
            pl.map(p => `<option value="${p.id}" ${p.id===selectedId?'selected':''}>${escHtml(p.name)}</option>`).join('');
    } catch (e) { /* silent */ }
}

function toggleEditPlaylistSelection() {
    document.getElementById('editPlaylistSelection').style.display = document.getElementById('editTaskType').value === 'PLAY' ? 'block' : 'none';
}

async function updateTask(id) {
    const name = document.getElementById('editTaskName').value.trim();
    if (!name) { showToast('请输入任务名称', 'error'); return; }
    try {
        await utils.request(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify({
            name, type: document.getElementById('editTaskType').value,
            playlistId: parseInt(document.getElementById('editTaskPlaylist').value) || null,
            timeOfDay: document.getElementById('editTaskTime').value
        })});
        closeModal();
        loadTaskList();
        showToast('更新成功');
    } catch (e) {
        showToast('更新失败: ' + e.message, 'error');
    }
}

async function deleteTask(id) {
    const ok = await showConfirm('确定要删除这个任务吗？');
    if (!ok) return;
    try {
        await utils.request(`/tasks/${id}`, { method: 'DELETE' });
        loadTaskList();
        showToast('删除成功');
    } catch (e) {
        showToast('删除失败: ' + e.message, 'error');
    }
}
