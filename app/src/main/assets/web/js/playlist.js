let playlistList = [];

async function loadPlaylistList() {
    const el = document.getElementById('playlistList');
    el.innerHTML = '<div class="loading">加载播放列表...</div>';
    try {
        const r = await utils.request('/playlists');
        playlistList = r.data || [];
        if (!playlistList.length) el.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><p>暂无播放列表</p></div>';
        else renderPlaylistList();
    } catch (e) { el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${e.message}</p></div>`; showToast(e.message,'error'); }
}

function renderPlaylistList() {
    document.getElementById('playlistList').innerHTML = playlistList.map(p => `<div class="list-item">
        <div class="pl-icon">▶</div>
        <div class="name">${escHtml(p.name)}</div>
        <div class="meta">${p.type==='TAG_BASED'?'基于标签':'手动'} · ${p.mediaCount||0} 个媒体 · ${trLabel(p.transitionEffect)}</div>
        <div class="actions">
            <button class="btn btn-primary btn-sm" onclick="playPlaylist(${p.id})">播放</button>
            <button class="btn-ghost" onclick="editPlaylist(${p.id})">编辑</button>
            <button class="btn-ghost" onclick="managePlaylistItems(${p.id})">管理</button>
            <button class="btn btn-danger btn-sm" onclick="deletePlaylist(${p.id})">删除</button>
        </div>
    </div>`).join('');
}

function trLabel(t) { return {NONE:'无',FADE:'淡入淡出',SLIDE_LEFT:'左滑',SLIDE_RIGHT:'右滑',SLIDE_UP:'上滑',SLIDE_DOWN:'下滑',ZOOM_IN:'放大',ZOOM_OUT:'缩小',WIPE_LEFT:'左擦除',WIPE_RIGHT:'右擦除',DISSOLVE:'溶解',BLUR:'模糊',RANDOM:'随机'}[t]||t; }

function showAddPlaylistModal() {
    showModal('新建播放列表', `<div class="field"><label>名称</label><input type="text" id="playlistName" placeholder="例如: 上午轮播"></div>
        <div class="field"><label>描述</label><input type="text" id="playlistDesc" placeholder="可选"></div>
        <div class="field"><label>类型</label><select id="playlistType" onchange="toggleTagSelection()">
            <option value="MANUAL">手动列表</option><option value="TAG_BASED">基于标签</option></select></div>
        <div id="tagSelection" class="field" style="display:none;"><label>标签</label><div class="check-group" id="tagCheckboxes">加载中...</div></div>
        <div class="field"><label>转场</label><select id="playlistTransition">
            ${['FADE','SLIDE_LEFT','SLIDE_RIGHT','ZOOM_IN','ZOOM_OUT','DISSOLVE','RANDOM','NONE'].map(v => `<option value="${v}">${trLabel(v)}</option>`).join('')}</select></div>
        <div class="field"><label>间隔</label><input type="number" id="playlistInterval" value="10" min="1" max="60"><span class="unit">秒</span></div>
        <button class="btn btn-primary" onclick="createPlaylist()">创建</button>`);
}

function toggleTagSelection() {
    const s = document.getElementById('playlistType').value === 'TAG_BASED';
    document.getElementById('tagSelection').style.display = s ? 'flex' : 'none';
    if (s) loadTagsForPlaylist();
}

async function loadTagsForPlaylist() {
    try { const r = await utils.request('/tags'); const tags = r.data || [];
        document.getElementById('tagCheckboxes').innerHTML = tags.length ? tags.map(t => `<label><input type="checkbox" value="${t.id}"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('') : '<span style="color:var(--muted);font-size:12px;">暂无标签</span>'; }
    catch(_) {}
}

async function createPlaylist() {
    const n = document.getElementById('playlistName').value.trim();
    if (!n) { showToast('请输入名称','error'); return; }
    const type = document.getElementById('playlistType').value;
    let tagIds = [];
    if (type === 'TAG_BASED') tagIds = Array.from(document.querySelectorAll('#tagCheckboxes input:checked')).map(c => parseInt(c.value));
    try {
        await utils.request('/playlists', { method: 'POST', body: JSON.stringify({
            name: n, description: document.getElementById('playlistDesc').value, type,
            transitionEffect: document.getElementById('playlistTransition').value,
            defaultInterval: parseInt(document.getElementById('playlistInterval').value), tagIds }) });
        closeModal(); loadPlaylistList(); showToast('创建成功');
    } catch(e) { showToast('创建失败: '+e.message,'error'); }
}

function editPlaylist(id) {
    const p = playlistList.find(x => x.id === id);
    if (!p) return;
    showModal('编辑播放列表', `<div class="field"><label>名称</label><input type="text" id="editPlaylistName" value="${escHtml(p.name)}"></div>
        <div class="field"><label>描述</label><input type="text" id="editPlaylistDesc" value="${escHtml(p.description||'')}"></div>
        <div class="field"><label>转场</label><select id="editPlaylistTransition">
            ${['FADE','SLIDE_LEFT','SLIDE_RIGHT','ZOOM_IN','ZOOM_OUT','DISSOLVE','RANDOM','NONE'].map(v => `<option value="${v}" ${p.transitionEffect===v?'selected':''}>${trLabel(v)}</option>`).join('')}</select></div>
        <div class="field"><label>间隔</label><input type="number" id="editPlaylistInterval" value="${p.defaultInterval}" min="1" max="60"><span class="unit">秒</span></div>
        <button class="btn btn-primary" onclick="updatePlaylist(${id})">保存</button>`);
}

async function updatePlaylist(id) {
    const n = document.getElementById('editPlaylistName').value.trim();
    if (!n) { showToast('请输入名称','error'); return; }
    try {
        await utils.request(`/playlists/${id}`, { method: 'PUT', body: JSON.stringify({ name: n, description: document.getElementById('editPlaylistDesc').value, transitionEffect: document.getElementById('editPlaylistTransition').value, defaultInterval: parseInt(document.getElementById('editPlaylistInterval').value) }) });
        closeModal(); loadPlaylistList(); showToast('更新成功');
    } catch(e) { showToast('更新失败: '+e.message,'error'); }
}

async function managePlaylistItems(id) {
    try {
        const [pr, ir, mr] = await Promise.all([utils.request(`/playlists/${id}`), utils.request(`/playlists/${id}/items`), utils.request('/media')]);
        const pl = pr.data, items = ir.data || [], allMedia = mr.data || [];
        showModal('管理: ' + escHtml(pl.name), `<div class="field"><label>当前 (${items.length})</label>
            <div class="scroll">${items.length ? items.map((it,i) => `<div class="item-row"><span class="idx">${i+1}</span><span class="iname">${escHtml(it.name)}</span><button class="btn btn-danger btn-sm" onclick="removeFromPlaylist(${id},${it.id})">移除</button></div>`).join('') : '<span style="color:var(--muted);font-size:12px;">暂无媒体</span>'}</div></div>
            <div class="field"><label>添加</label><select id="addMediaSelect" style="margin-bottom:6px;">
                <option value="">选择媒体...</option>
                ${allMedia.filter(m => !items.find(i=>i.id===m.id)).map(m => `<option value="${m.id}">${escHtml(m.name)}</option>`).join('')}</select>
                <button class="btn btn-secondary" onclick="addToPlaylist(${id})" style="width:100%;">添加</button></div>`);
    } catch(e) { showToast('加载失败: '+e.message,'error'); }
}

async function addToPlaylist(id) {
    const mid = document.getElementById('addMediaSelect').value;
    if (!mid) { showToast('请选择媒体','error'); return; }
    try { await utils.request(`/playlists/${id}/items`,{method:'POST',body:JSON.stringify({mediaId:parseInt(mid)})}); managePlaylistItems(id); loadPlaylistList(); showToast('添加成功'); }
    catch(e) { showToast('添加失败: '+e.message,'error'); }
}

async function removeFromPlaylist(pid, mid) {
    try { await utils.request(`/playlists/${pid}/items/${mid}`,{method:'DELETE'}); managePlaylistItems(pid); loadPlaylistList(); showToast('移除成功'); }
    catch(e) { showToast('移除失败: '+e.message,'error'); }
}

async function playPlaylist(id) {
    try {
        await utils.request(`/playlists/${id}/play`, { method: 'POST' });
        showToast('播放指令已发送');
    } catch(e) {
        showToast('播放失败: ' + e.message, 'error');
    }
}

async function deletePlaylist(id) {
    if (!await showConfirm('确定删除此播放列表？媒体文件不会被删除。')) return;
    try { await utils.request(`/playlists/${id}`,{method:'DELETE'}); loadPlaylistList(); showToast('删除成功'); }
    catch(e) { showToast('删除失败: '+e.message,'error'); }
}
