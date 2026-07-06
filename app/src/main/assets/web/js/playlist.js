let playlistList = [];

async function loadPlaylistList() {
    const el = document.getElementById('playlistList');
    el.innerHTML = '<div class="loading">加载播放列表...</div>';
    try {
        const res = await utils.request('/playlists');
        playlistList = res.data || [];
        if (playlistList.length === 0) {
            el.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><p>暂无播放列表</p><p style="font-size:12px;margin-top:4px;">点击右上角「新建播放列表」创建</p></div>';
        } else {
            renderPlaylistList();
        }
    } catch (e) {
        el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>加载失败: ${e.message}</p></div>`;
        showToast(e.message, 'error');
    }
}

function renderPlaylistList() {
    const el = document.getElementById('playlistList');
    el.innerHTML = playlistList.map(p => `
        <div class="playlist-item">
            <div class="playlist-icon">▶</div>
            <div class="playlist-info">
                <div class="playlist-name">${escHtml(p.name)}</div>
                <div class="playlist-meta">${p.type === 'TAG_BASED' ? '基于标签' : '手动列表'} · ${p.mediaCount || 0} 个媒体 · 转场: ${getTransitionLabel(p.transitionEffect)}</div>
            </div>
            <div class="playlist-actions">
                <button class="btn btn-primary btn-sm" onclick="playPlaylist(${p.id})">播放</button>
                <button class="btn btn-ghost" onclick="editPlaylist(${p.id})">编辑</button>
                <button class="btn btn-ghost" onclick="managePlaylistItems(${p.id})">管理</button>
                <button class="btn btn-danger btn-sm" onclick="deletePlaylist(${p.id})">删除</button>
            </div>
        </div>
    `).join('');
}

function getTransitionLabel(t) {
    return { 'NONE':'无', 'FADE':'淡入淡出', 'SLIDE_LEFT':'左滑', 'SLIDE_RIGHT':'右滑', 'SLIDE_UP':'上滑', 'SLIDE_DOWN':'下滑', 'ZOOM_IN':'放大', 'ZOOM_OUT':'缩小', 'WIPE_LEFT':'左擦除', 'WIPE_RIGHT':'右擦除', 'DISSOLVE':'溶解', 'BLUR':'模糊', 'RANDOM':'随机' }[t] || t;
}

function showAddPlaylistModal() {
    showModal('新建播放列表', `
        <div class="form-row"><label>名称</label><input type="text" id="playlistName" placeholder="例如: 上午播放列表"></div>
        <div class="form-row"><label>描述</label><input type="text" id="playlistDesc" placeholder="可选"></div>
        <div class="form-row"><label>类型</label>
            <select id="playlistType" onchange="toggleTagSelection()">
                <option value="MANUAL">手动列表</option>
                <option value="TAG_BASED">基于标签</option>
            </select>
        </div>
        <div id="tagSelection" class="form-row" style="display:none;"><label>选择标签</label><div class="modal-checkbox-group" id="tagCheckboxes">加载中...</div></div>
        <div class="form-row"><label>转场效果</label>
            <select id="playlistTransition">
                <option value="FADE">淡入淡出</option><option value="SLIDE_LEFT">左滑</option><option value="SLIDE_RIGHT">右滑</option>
                <option value="ZOOM_IN">放大</option><option value="ZOOM_OUT">缩小</option><option value="DISSOLVE">溶解</option>
                <option value="RANDOM">随机</option><option value="NONE">无</option>
            </select>
        </div>
        <div class="form-row"><label>显示间隔（秒）</label><input type="number" id="playlistInterval" value="10" min="1" max="60"></div>
        <button class="btn btn-primary" onclick="createPlaylist()">创建</button>
    `);
}

function toggleTagSelection() {
    const show = document.getElementById('playlistType').value === 'TAG_BASED';
    document.getElementById('tagSelection').style.display = show ? 'block' : 'none';
    if (show) loadTagsForPlaylist();
}

async function loadTagsForPlaylist() {
    try {
        const res = await utils.request('/tags');
        const tags = res.data || [];
        document.getElementById('tagCheckboxes').innerHTML = tags.map(t =>
            `<label><input type="checkbox" value="${t.id}"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${t.color}"></span> ${escHtml(t.name)}</label>`
        ).join('') || '<span style="color:var(--text-muted)">暂无标签，请先创建标签</span>';
    } catch (e) { /* silent */ }
}

async function createPlaylist() {
    const name = document.getElementById('playlistName').value.trim();
    if (!name) { showToast('请输入名称', 'error'); return; }
    const type = document.getElementById('playlistType').value;
    const transitionEffect = document.getElementById('playlistTransition').value;
    const defaultInterval = parseInt(document.getElementById('playlistInterval').value);
    let tagIds = [];
    if (type === 'TAG_BASED') {
        tagIds = Array.from(document.querySelectorAll('#tagCheckboxes input:checked')).map(c => parseInt(c.value));
    }
    try {
        await utils.request('/playlists', { method: 'POST', body: JSON.stringify({
            name, description: document.getElementById('playlistDesc').value,
            type, transitionEffect, defaultInterval, tagIds
        })});
        closeModal();
        loadPlaylistList();
        showToast('创建成功');
    } catch (e) {
        showToast('创建失败: ' + e.message, 'error');
    }
}

function editPlaylist(id) {
    const p = playlistList.find(x => x.id === id);
    if (!p) return;
    showModal('编辑播放列表', `
        <div class="form-row"><label>名称</label><input type="text" id="editPlaylistName" value="${escHtml(p.name)}"></div>
        <div class="form-row"><label>描述</label><input type="text" id="editPlaylistDesc" value="${escHtml(p.description || '')}"></div>
        <div class="form-row"><label>转场效果</label>
            <select id="editPlaylistTransition">${['FADE','SLIDE_LEFT','SLIDE_RIGHT','ZOOM_IN','ZOOM_OUT','DISSOLVE','RANDOM','NONE'].map(v =>
                `<option value="${v}" ${p.transitionEffect===v?'selected':''}>${getTransitionLabel(v)}</option>`
            ).join('')}</select>
        </div>
        <div class="form-row"><label>显示间隔（秒）</label><input type="number" id="editPlaylistInterval" value="${p.defaultInterval}" min="1" max="60"></div>
        <button class="btn btn-primary" onclick="updatePlaylist(${id})">保存</button>
    `);
}

async function updatePlaylist(id) {
    const name = document.getElementById('editPlaylistName').value.trim();
    if (!name) { showToast('请输入名称', 'error'); return; }
    try {
        await utils.request(`/playlists/${id}`, { method: 'PUT', body: JSON.stringify({
            name, description: document.getElementById('editPlaylistDesc').value,
            transitionEffect: document.getElementById('editPlaylistTransition').value,
            defaultInterval: parseInt(document.getElementById('editPlaylistInterval').value)
        })});
        closeModal();
        loadPlaylistList();
        showToast('更新成功');
    } catch (e) {
        showToast('更新失败: ' + e.message, 'error');
    }
}

async function managePlaylistItems(id) {
    try {
        const [pRes, iRes, mRes] = await Promise.all([
            utils.request(`/playlists/${id}`),
            utils.request(`/playlists/${id}/items`),
            utils.request('/media')
        ]);
        const pl = pRes.data, items = iRes.data || [], allMedia = mRes.data || [];
        showModal('管理播放列表: ' + escHtml(pl.name), `
            <div class="form-row"><label>当前媒体 (${items.length} 项)</label>
                <div class="scroll-list">${items.length === 0 ? '<span style="color:var(--text-muted);font-size:13px;">暂无媒体</span>' :
                    items.map((item, i) => `<div class="playlist-item-row"><span class="item-index">${i+1}</span><span class="item-name">${escHtml(item.name)}</span><button class="btn btn-danger btn-sm" onclick="removeFromPlaylist(${id}, ${item.id})">移除</button></div>`
                ).join('')}</div>
            </div>
            <div class="form-row"><label>添加媒体</label>
                <select id="addMediaSelect" style="margin-bottom:8px;">
                    <option value="">选择媒体...</option>
                    ${allMedia.filter(m => !items.find(i => i.id === m.id)).map(m => `<option value="${m.id}">${escHtml(m.name)} (${utils.getMediaTypeLabel(m.type)})</option>`).join('')}
                </select>
                <button class="btn btn-secondary" onclick="addToPlaylist(${id})" style="width:100%;">添加</button>
            </div>
        `);
    } catch (e) {
        showToast('加载失败: ' + e.message, 'error');
    }
}

async function addToPlaylist(playlistId) {
    const mediaId = document.getElementById('addMediaSelect').value;
    if (!mediaId) { showToast('请选择媒体', 'error'); return; }
    try {
        await utils.request(`/playlists/${playlistId}/items`, { method: 'POST', body: JSON.stringify({ mediaId: parseInt(mediaId) }) });
        managePlaylistItems(playlistId);
        loadPlaylistList();
        showToast('添加成功');
    } catch (e) {
        showToast('添加失败: ' + e.message, 'error');
    }
}

async function removeFromPlaylist(playlistId, mediaId) {
    try {
        await utils.request(`/playlists/${playlistId}/items/${mediaId}`, { method: 'DELETE' });
        managePlaylistItems(playlistId);
        loadPlaylistList();
        showToast('移除成功');
    } catch (e) {
        showToast('移除失败: ' + e.message, 'error');
    }
}

function playPlaylist(id) {
    showToast('发送播放命令...');
}

async function deletePlaylist(id) {
    const ok = await showConfirm('确定要删除这个播放列表吗？媒体文件不会被删除。');
    if (!ok) return;
    try {
        await utils.request(`/playlists/${id}`, { method: 'DELETE' });
        loadPlaylistList();
        showToast('删除成功');
    } catch (e) {
        showToast('删除失败: ' + e.message, 'error');
    }
}
