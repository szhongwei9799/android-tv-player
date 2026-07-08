let taskList = [];

async function loadTaskList() {
    const el = document.getElementById('taskList');
    el.innerHTML = '<div class="loading">加载定时任务...</div>';
    try {
        const r = await utils.request('/tasks');
        taskList = r.data || [];
        if (!taskList.length) el.innerHTML = '<div class="empty-state"><div class="empty-icon">⏰</div><p>暂无定时任务</p></div>';
        else renderTaskList();
    } catch(e) { el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${e.message}</p></div>`; showToast(e.message,'error'); }
}

function renderTaskList() {
    document.getElementById('taskList').innerHTML = taskList.map(t => `<div class="list-item">
        <div class="toggle ${t.isEnabled?'on':''}" onclick="toggleTask(${t.id})"></div>
        <div class="name">${escHtml(t.name)}</div>
        <div class="meta">${taskLabel(t.type)} · ${t.timeOfDay||'-'}</div>
        <div class="actions">
            <button class="btn-ghost" onclick="editTask(${t.id})">编辑</button>
            <button class="btn btn-danger btn-sm" onclick="deleteTask(${t.id})">删除</button>
        </div>
    </div>`).join('');
}

function taskLabel(t) { return {PLAY:'播放',STOP:'停止',POWER_OFF:'关机',SWITCH_SOURCE:'切换信号源'}[t]||t; }

async function toggleTask(id) {
    try { await utils.request(`/tasks/${id}/toggle`,{method:'PUT'}); loadTaskList(); }
    catch(e) { showToast('操作失败: '+e.message,'error'); }
}

function showAddTaskModal() {
    showModal('新建定时任务', `<div class="field"><label>名称</label><input type="text" id="taskName" placeholder="例如: 上午播放"></div>
        <div class="field"><label>类型</label><select id="taskType" onchange="togglePSelection()">
            <option value="PLAY">播放</option><option value="STOP">停止</option><option value="POWER_OFF">关机</option></select></div>
        <div id="playlistSelection" class="field" style="display:none;"><label>播放</label><span style="font-size:12px;color:var(--muted);">将播放默认播放列表</span></div>
        <div class="field" id="endTimeField"><label>结束时间</label><input type="time" id="taskEndTime"><span style="font-size:11px;color:var(--muted);margin-left:4px;">留空=播放至媒体自然结束</span></div>
        <div class="field"><label>开始时间</label><input type="time" id="taskTime" value="08:00"></div>
        <div class="field"><label>重复</label>
            <div style="display:flex;gap:12px;font-size:13px;"><label style="display:flex;align-items:center;gap:4px;cursor:pointer;"><input type="radio" name="taskRepeat" value="daily" checked>每天</label>
            <label style="display:flex;align-items:center;gap:4px;cursor:pointer;"><input type="radio" name="taskRepeat" value="weekly">每周</label></div></div>
        <div id="weekdaySelection" class="field" style="display:none;"><label>星期</label>
            <div style="display:flex;gap:5px;flex-wrap:wrap;">${['一','二','三','四','五','六','日'].map((d,i)=>`<label style="display:flex;align-items:center;gap:3px;cursor:pointer;font-size:13px;"><input type="checkbox" value="${i+1}">${d}</label>`).join('')}</div></div>
        <button class="btn btn-primary" onclick="createTask()">创建</button>`);
    togglePSelection();
    document.querySelectorAll('input[name="taskRepeat"]').forEach(r => r.addEventListener('change', () => { document.getElementById('weekdaySelection').style.display = document.querySelector('input[name="taskRepeat"]:checked').value === 'weekly' ? 'flex' : 'none'; }));
    document.getElementById('weekdaySelection').style.display = 'none';
}

function togglePSelection() {
    const isPlay = document.getElementById('taskType').value === 'PLAY';
    document.getElementById('playlistSelection').style.display = isPlay ? 'flex' : 'none';
    document.getElementById('endTimeField').style.display = isPlay ? 'flex' : 'none';
}

async function createTask() {
    const n = document.getElementById('taskName').value.trim();
    if (!n) { showToast('请输入名称','error'); return; }
    const type = document.getElementById('taskType').value;
    const time = document.getElementById('taskTime').value;
    const rep = document.querySelector('input[name="taskRepeat"]:checked').value;
    let dow = null;
    if (rep === 'weekly') dow = Array.from(document.querySelectorAll('#weekdaySelection input:checked')).map(c => c.value).join(',');
    const endTime = document.getElementById('taskEndTime').value || null;
    try {
        await utils.request('/tasks', { method: 'POST', body: JSON.stringify({
            name: n, type, playlistId: 1,
            timeOfDay: time, daysOfWeek: dow,
            endTime: endTime }) });
        closeModal(); loadTaskList(); showToast('创建成功');
    } catch(e) { showToast('创建失败: '+e.message,'error'); }
}

function editTask(id) {
    const t = taskList.find(x => x.id === id); if (!t) return;
    showModal('编辑任务', `<div class="field"><label>名称</label><input type="text" id="editTaskName" value="${escHtml(t.name)}"></div>
        <div class="field"><label>类型</label><select id="editTaskType" onchange="toggleEPSelection()">
            ${['PLAY','STOP','POWER_OFF'].map(v => `<option value="${v}" ${t.type===v?'selected':''}>${taskLabel(v)}</option>`).join('')}</select></div>
        <div id="editPlaylistSelection" class="field" style="display:${t.type==='PLAY'?'flex':'none'}"><label>播放</label><span style="font-size:12px;color:var(--muted);">将播放默认播放列表</span></div>
        <div id="editEndTimeField" class="field" style="display:${t.type==='PLAY'?'flex':'none'}"><label>结束时间</label><input type="time" id="editTaskEndTime" value="${t.endTime||''}"><span style="font-size:11px;color:var(--muted);margin-left:4px;">留空=播放至媒体自然结束</span></div>
        <div class="field"><label>开始时间</label><input type="time" id="editTaskTime" value="${t.timeOfDay||'08:00'}"></div>
        <button class="btn btn-primary" onclick="updateTask(${id})">保存</button>`);
}

function toggleEPSelection() {
    const isPlay = document.getElementById('editTaskType').value === 'PLAY';
    document.getElementById('editPlaylistSelection').style.display = isPlay ? 'flex' : 'none';
    document.getElementById('editEndTimeField').style.display = isPlay ? 'flex' : 'none';
}

async function updateTask(id) {
    const n = document.getElementById('editTaskName').value.trim();
    if (!n) { showToast('请输入名称','error'); return; }
    const endTime = document.getElementById('editTaskEndTime').value || null;
    try {
        await utils.request(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify({
            name: n,
            type: document.getElementById('editTaskType').value,
            playlistId: 1,
            timeOfDay: document.getElementById('editTaskTime').value,
            endTime: endTime }) });
        closeModal(); loadTaskList(); showToast('更新成功');
    } catch(e) { showToast('更新失败: '+e.message,'error'); }
}

async function deleteTask(id) {
    if (!await showConfirm('确定删除此任务？')) return;
    try { await utils.request(`/tasks/${id}`,{method:'DELETE'}); loadTaskList(); showToast('删除成功'); }
    catch(e) { showToast('删除失败: '+e.message,'error'); }
}
