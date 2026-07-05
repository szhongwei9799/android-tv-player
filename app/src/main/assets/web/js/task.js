// 定时任务管理

let taskList = [];

// 加载任务列表
async function loadTaskList() {
    try {
        const response = await utils.request('/tasks');
        if (response.success) {
            taskList = response.data || [];
            renderTaskList();
        }
    } catch (error) {
        console.error('加载任务列表失败:', error);
    }
}

// 渲染任务列表
function renderTaskList() {
    const container = document.getElementById('taskList');
    container.innerHTML = taskList.map(task => `
        <div class="task-item">
            <div class="task-toggle ${task.isEnabled ? 'active' : ''}" 
                 onclick="toggleTask(${task.id})"></div>
            <div class="task-info">
                <div class="task-name">${task.name}</div>
                <div class="task-schedule">
                    ${getTaskTypeLabel(task.type)} | 
                    ${task.timeOfDay || task.cronExpression || '-'}
                </div>
            </div>
            <div class="task-actions">
                <button class="btn btn-secondary btn-small" onclick="editTask(${task.id})">编辑</button>
                <button class="btn btn-danger btn-small" onclick="deleteTask(${task.id})">删除</button>
            </div>
        </div>
    `).join('');
}

// 获取任务类型标签
function getTaskTypeLabel(type) {
    const labels = {
        'PLAY': '播放',
        'STOP': '停止',
        'POWER_OFF': '关机',
        'SWITCH_SOURCE': '切换信号源'
    };
    return labels[type] || type;
}

// 切换任务启用状态
async function toggleTask(id) {
    try {
        await utils.request(`/tasks/${id}/toggle`, { method: 'PUT' });
        loadTaskList();
    } catch (error) {
        showToast('操作失败', 'error');
    }
}

// 显示创建任务模态框
function showAddTaskModal() {
    showModal(`
        <h3>新建定时任务</h3>
        <div class="form-group">
            <label>任务名称</label>
            <input type="text" id="taskName" placeholder="请输入任务名称">
        </div>
        <div class="form-group">
            <label>任务类型</label>
            <select id="taskType" onchange="togglePlaylistSelection()">
                <option value="PLAY">播放</option>
                <option value="STOP">停止</option>
                <option value="POWER_OFF">关机</option>
            </select>
        </div>
        <div id="playlistSelection" class="form-group">
            <label>关联播放列表</label>
            <select id="taskPlaylist">
                <option value="">选择播放列表...</option>
            </select>
        </div>
        <div class="form-group">
            <label>执行时间</label>
            <input type="time" id="taskTime" value="08:00">
        </div>
        <div class="form-group">
            <label>重复</label>
            <div>
                <label style="display:inline-flex;align-items:center;gap:8px;margin-right:16px;cursor:pointer;">
                    <input type="radio" name="taskRepeat" value="daily" checked> 每天
                </label>
                <label style="display:inline-flex;align-items:center;gap:8px;cursor:pointer;">
                    <input type="radio" name="taskRepeat" value="weekly"> 每周
                </label>
            </div>
            <div id="weekdaySelection" style="display:none;margin-top:8px;">
                <label style="display:flex;gap:8px;">
                    <input type="checkbox" value="1"> 周一
                    <input type="checkbox" value="2"> 周二
                    <input type="checkbox" value="3"> 周三
                    <input type="checkbox" value="4"> 周四
                    <input type="checkbox" value="5"> 周五
                    <input type="checkbox" value="6"> 周六
                    <input type="checkbox" value="7"> 周日
                </label>
            </div>
        </div>
        <button class="btn btn-primary" onclick="createTask()">创建</button>
    `);
    
    loadPlaylistsForTask();
    setupRepeatToggle();
}

// 加载播放列表供任务选择
async function loadPlaylistsForTask() {
    try {
        const response = await utils.request('/playlists');
        if (response.success) {
            const playlists = response.data || [];
            document.getElementById('taskPlaylist').innerHTML = 
                '<option value="">选择播放列表...</option>' +
                playlists.map(p => `<option value="${p.id}">${p.name}</option>`).join('');
        }
    } catch (error) {
        console.error('加载播放列表失败:', error);
    }
}

// 设置重复选项切换
function setupRepeatToggle() {
    document.querySelectorAll('input[name="taskRepeat"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            document.getElementById('weekdaySelection').style.display = 
                e.target.value === 'weekly' ? 'block' : 'none';
        });
    });
}

// 切换播放列表选择显示
function togglePlaylistSelection() {
    const type = document.getElementById('taskType').value;
    document.getElementById('playlistSelection').style.display = 
        type === 'PLAY' ? 'block' : 'none';
}

// 创建任务
async function createTask() {
    const name = document.getElementById('taskName').value;
    const type = document.getElementById('taskType').value;
    const playlistId = document.getElementById('taskPlaylist').value;
    const timeOfDay = document.getElementById('taskTime').value;
    const repeatType = document.querySelector('input[name="taskRepeat"]:checked').value;
    
    if (!name) {
        showToast('请输入任务名称', 'error');
        return;
    }
    
    let daysOfWeek = null;
    if (repeatType === 'weekly') {
        const checkedDays = document.querySelectorAll('#weekdaySelection input:checked');
        daysOfWeek = Array.from(checkedDays).map(cb => cb.value).join(',');
    }
    
    try {
        await utils.request('/tasks', {
            method: 'POST',
            body: JSON.stringify({
                name,
                type,
                playlistId: playlistId ? parseInt(playlistId) : null,
                timeOfDay,
                daysOfWeek
            })
        });
        closeModal();
        loadTaskList();
        showToast('创建成功');
    } catch (error) {
        showToast('创建失败', 'error');
    }
}

// 编辑任务
function editTask(id) {
    const task = taskList.find(t => t.id === id);
    if (!task) return;
    
    showModal(`
        <h3>编辑定时任务</h3>
        <div class="form-group">
            <label>任务名称</label>
            <input type="text" id="editTaskName" value="${task.name}">
        </div>
        <div class="form-group">
            <label>任务类型</label>
            <select id="editTaskType" onchange="toggleEditPlaylistSelection()">
                <option value="PLAY" ${task.type === 'PLAY' ? 'selected' : ''}>播放</option>
                <option value="STOP" ${task.type === 'STOP' ? 'selected' : ''}>停止</option>
                <option value="POWER_OFF" ${task.type === 'POWER_OFF' ? 'selected' : ''}>关机</option>
            </select>
        </div>
        <div id="editPlaylistSelection" class="form-group" style="display:${task.type === 'PLAY' ? 'block' : 'none'}">
            <label>关联播放列表</label>
            <select id="editTaskPlaylist">
                <option value="">选择播放列表...</option>
            </select>
        </div>
        <div class="form-group">
            <label>执行时间</label>
            <input type="time" id="editTaskTime" value="${task.timeOfDay || '08:00'}">
        </div>
        <button class="btn btn-primary" onclick="updateTask(${id})">保存</button>
    `);
    
    loadPlaylistsForEditTask(task.playlistId);
}

// 加载播放列表供编辑任务选择
async function loadPlaylistsForEditTask(selectedPlaylistId) {
    try {
        const response = await utils.request('/playlists');
        if (response.success) {
            const playlists = response.data || [];
            document.getElementById('editTaskPlaylist').innerHTML = 
                '<option value="">选择播放列表...</option>' +
                playlists.map(p => 
                    `<option value="${p.id}" ${p.id === selectedPlaylistId ? 'selected' : ''}>${p.name}</option>`
                ).join('');
        }
    } catch (error) {
        console.error('加载播放列表失败:', error);
    }
}

// 切换编辑时播放列表选择显示
function toggleEditPlaylistSelection() {
    const type = document.getElementById('editTaskType').value;
    document.getElementById('editPlaylistSelection').style.display = 
        type === 'PLAY' ? 'block' : 'none';
}

// 更新任务
async function updateTask(id) {
    const name = document.getElementById('editTaskName').value;
    const type = document.getElementById('editTaskType').value;
    const playlistId = document.getElementById('editTaskPlaylist').value;
    const timeOfDay = document.getElementById('editTaskTime').value;
    
    if (!name) {
        showToast('请输入任务名称', 'error');
        return;
    }
    
    try {
        await utils.request(`/tasks/${id}`, {
            method: 'PUT',
            body: JSON.stringify({
                name,
                type,
                playlistId: playlistId ? parseInt(playlistId) : null,
                timeOfDay
            })
        });
        closeModal();
        loadTaskList();
        showToast('更新成功');
    } catch (error) {
        showToast('更新失败', 'error');
    }
}

// 删除任务
async function deleteTask(id) {
    if (!confirm('确定要删除这个任务吗？')) {
        return;
    }
    
    try {
        await utils.request(`/tasks/${id}`, { method: 'DELETE' });
        loadTaskList();
        showToast('删除成功');
    } catch (error) {
        showToast('删除失败', 'error');
    }
}
