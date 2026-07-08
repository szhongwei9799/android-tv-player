let playlistData = null;

async function loadPlaylistList() {
    const el = document.getElementById('playlistList');
    el.innerHTML = '<div class="loading">加载播放项目...</div>';
    try {
        const r = await utils.request('/playlist');
        playlistData = r.data;
        if (!playlistData || !playlistData.items || !playlistData.items.length) {
            el.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><p>暂无播放项目</p><p style="font-size:12px;margin-top:6px;">点击右上角「新建播放项目」添加</p></div>';
        } else renderPlaylist();
    } catch(e) { el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${e.message}</p></div>`; showToast(e.message,'error'); }
}

function pmLabel(t) { return {SEQUENTIAL:'顺序',RANDOM:'随机',SHUFFLE:'洗牌'}[t]||t; }
function loopLabel(c) { return c===0?'无限':c===1?'1次(不循环)':`${c}次`; }
function trLabel(t) { return {NONE:'无',FADE:'淡入淡出',SLIDE_LEFT:'左滑',SLIDE_RIGHT:'右滑',SLIDE_UP:'上滑',SLIDE_DOWN:'下滑',ZOOM_IN:'放大',ZOOM_OUT:'缩小',WIPE_LEFT:'左擦除',WIPE_RIGHT:'右擦除',DISSOLVE:'溶解',BLUR:'模糊',RANDOM:'随机'}[t]||t; }
const transitions = ['FADE','SLIDE_LEFT','SLIDE_RIGHT','ZOOM_IN','ZOOM_OUT','DISSOLVE','RANDOM','NONE'];
const playModes = ['SEQUENTIAL','RANDOM','SHUFFLE'];

function renderPlaylist() {
    const pl = playlistData.playlist;
    const items = playlistData.items || [];
    document.getElementById('playlistList').innerHTML = `
        <div style="margin-bottom:16px;padding:14px;background:var(--surface2);border-radius:10px;">
            <div style="display:flex;align-items:center;gap:6px;margin-bottom:10px;">
                <button class="btn btn-primary btn-sm" onclick="playPlaylist()">播放</button>
                <button class="btn btn-warning btn-sm" onclick="stopPlaylist()">停止</button>
            </div>
            <div style="font-size:12px;display:flex;gap:6px;flex-wrap:wrap;align-items:center;">
                <span class="setting-item">转场 <select class="setting-select" onchange="updateSetting('transitionEffect',this.value)">
                    ${transitions.map(v => `<option value="${v}" ${pl.transitionEffect===v?'selected':''}>${trLabel(v)}</option>`).join('')}</select></span>
                <span class="setting-item">间隔 <input type="number" class="setting-num" value="${pl.defaultInterval}" min="1" max="60" onchange="updateSetting('defaultInterval',parseInt(this.value)||10)"><span class="unit">s</span></span>
                <span class="setting-item">项目 <select class="setting-select" onchange="updateSetting('itemPlayMode',this.value)">
                    ${playModes.map(v => `<option value="${v}" ${pl.itemPlayMode===v?'selected':''}>${pmLabel(v)}</option>`).join('')}</select></span>
                <span class="setting-item">循环 <input type="number" class="setting-num" value="${pl.itemLoopCount}" min="0" max="999" onchange="updateSetting('itemLoopCount',Math.max(0,parseInt(this.value)||1))" title="0=无限 1=不循环"><span class="unit" style="font-size:10px;">0=无限</span></span>
            </div>
        </div>
        <div style="font-size:13px;font-weight:600;margin:8px 0;color:var(--text2);">播放项目</div>
        ${items.map((item, i) => renderItem(item, i)).join('')}
    `;
}

function renderItem(item, i) {
    return `<div class="pl-item">
        <div class="pl-item-head">
            <span class="pl-item-name">${escHtml(item.name)}</span>
            <div class="actions">
                <button class="btn-ghost" onclick="showEditItemModal(${item.id})">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="deleteItem(${item.id})">删除</button>
            </div>
        </div>
        ${item.tags && item.tags.length ? item.tags.map(t => renderItemTag(t)).join('') : '<div style="font-size:11px;color:var(--muted);padding:4px 10px 6px;">暂无标签</div>'}
    </div>`;
}

function renderItemTag(t) {
    return `<div class="pl-tag" style="border-left-color:${t.color};">
        <span class="pl-tag-order">${t.sortOrder+1}</span>
        <span class="pl-tag-name">${escHtml(t.name)}</span>
        <span class="pl-tag-meta">${t.mediaCount}个媒体 · ${pmLabel(t.playMode)} · ${loopLabel(t.loopCount)}</span>
    </div>`;
}

async function updateSetting(field, value) {
    try {
        await utils.request('/playlist', { method: 'PUT', body: JSON.stringify({ [field]: value }) });
        showToast('已更新');
    } catch(e) { showToast('保存失败: '+e.message,'error'); }
}

function showAddItemModal() {
    showModal('新建播放项目', `<div class="field"><label>项目名称</label><input type="text" id="newItemName" placeholder="例如: 上午时段"></div>
        <div class="field"><label>选择标签</label><div class="check-group" id="addTagSelect">加载中...</div></div>
        <button class="btn btn-primary" onclick="createItem()">创建</button>`);
    loadAllTags();
}

async function loadAllTags() {
    try {
        const tr = await utils.request('/tags');
        const allTags = tr.data || [];
        document.getElementById('addTagSelect').innerHTML = allTags.length
            ? allTags.map(t => `<label><input type="checkbox" value="${t.id}" data-name="${escHtml(t.name)}" data-color="${t.color}"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('')
            : '<span style="color:var(--muted);font-size:12px;">暂无标签，请先在标签管理创建</span>';
    } catch(_) { document.getElementById('addTagSelect').innerHTML = '<span style="color:var(--red);font-size:12px;">加载失败</span>'; }
}

async function createItem() {
    const name = document.getElementById('newItemName').value.trim();
    if (!name) { showToast('请输入项目名称','error'); return; }
    const checked = Array.from(document.querySelectorAll('#addTagSelect input:checked'));
    const tagIds = checked.map(c => parseInt(c.value));
    if (!tagIds.length) { showToast('请至少选择一个标签','error'); return; }
    try {
        await utils.request('/playlist/items', { method: 'POST', body: JSON.stringify({ name, tagIds }) });
        closeModal(); loadPlaylistList(); showToast('创建成功');
    } catch(e) { showToast('创建失败: '+e.message,'error'); }
}

function showEditItemModal(itemId) {
    const item = playlistData.items.find(x => x.id === itemId);
    if (!item) return;
    const tagsHtml = item.tags && item.tags.length
        ? item.tags.map((t, i) => `<div style="display:flex;align-items:center;gap:4px;margin-bottom:4px;font-size:12px;">
            <span style="flex:0 0 20px;color:var(--muted);">${i+1}.</span>
            <span style="flex:0 0 12px;height:12px;border-radius:50%;background:${t.color};display:inline-block;"></span>
            <span style="flex:1;">${escHtml(t.name)}</span>
            <select onchange="updateItemTag(${itemId},${t.tagId},'playMode',this.value)" style="font-size:11px;padding:1px 3px;background:var(--elev);color:var(--text);border:none;border-radius:3px;">
                ${playModes.map(v => `<option value="${v}" ${t.playMode===v?'selected':''}>${pmLabel(v)}</option>`).join('')}</select>
            <input type="number" value="${t.loopCount}" min="0" max="999" onchange="updateItemTag(${itemId},${t.tagId},'loopCount',Math.max(0,parseInt(this.value)||1))" title="0=无限 1=不循环" style="width:50px;font-size:11px;padding:1px 3px;background:var(--elev);color:var(--text);border:none;border-radius:3px;text-align:center;">
            <button class="btn btn-danger btn-sm" style="font-size:10px;padding:1px 6px;" onclick="removeItemTag(${itemId},${t.tagId})">移除</button>
        </div>`).join('')
        : '<span style="color:var(--muted);font-size:12px;">暂无标签</span>';

    showModal('编辑项目', `<div class="field"><label>项目名称</label><input type="text" id="editItemName" value="${escHtml(item.name)}"></div>
        <div class="field"><label>标签列表（可修改次序和循环模式）</label><div style="max-height:200px;overflow-y:auto;">${tagsHtml}</div></div>
        <div class="field"><label>添加标签</label><div class="check-group" id="editAddTagSelect">加载中...</div></div>
        <button class="btn btn-primary" onclick="updateItem(${itemId})">保存</button>`);
    loadEditTags(item);
}

async function loadEditTags(item) {
    try {
        const tr = await utils.request('/tags');
        const allTags = tr.data || [];
        const existingIds = new Set((item.tags||[]).map(t => t.tagId));
        const avail = allTags.filter(t => !existingIds.has(t.id));
        document.getElementById('editAddTagSelect').innerHTML = avail.length
            ? avail.map(t => `<label><input type="checkbox" value="${t.id}"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('')
            : '<span style="color:var(--muted);font-size:12px;">所有标签已在列表中</span>';
    } catch(_) { document.getElementById('editAddTagSelect').innerHTML = '<span style="color:var(--red);font-size:12px;">加载失败</span>'; }
}

async function updateItem(itemId) {
    const name = document.getElementById('editItemName').value.trim();
    if (!name) { showToast('请输入项目名称','error'); return; }
    // 更新名称
    await utils.request(`/playlist/items/${itemId}`, { method: 'PUT', body: JSON.stringify({ name }) });
    // 添加新选中的标签
    const checked = Array.from(document.querySelectorAll('#editAddTagSelect input:checked'));
    const item = playlistData.items.find(x => x.id === itemId);
    const existing = (item.tags||[]).map(t => t.tagId);
    const newTagIds = checked.map(c => parseInt(c.value)).filter(id => !existing.includes(id));
    if (newTagIds.length) {
        const allTags = item.tags || [];
        const maxOrder = allTags.reduce((m, t) => Math.max(m, t.sortOrder), -1);
        const tagData = newTagIds.map((id, i) => ({ tagId: id, sortOrder: maxOrder + 1 + i, playMode: 'SEQUENTIAL', loopCount: -1 }));
        const updatedTags = [...allTags.map(t => ({ tagId: t.tagId, sortOrder: t.sortOrder, playMode: t.playMode, loopCount: t.loopCount })), ...tagData];
        await utils.request(`/playlist/items/${itemId}/tags`, { method: 'PUT', body: JSON.stringify({ tags: updatedTags }) });
    }
    closeModal(); loadPlaylistList(); showToast('保存成功');
}

async function updateItemTag(itemId, tagId, field, value) {
    const item = playlistData.items.find(x => x.id === itemId);
    if (!item) return;
    const allTags = item.tags || [];
    const tags = allTags.map(t => ({
        tagId: t.tagId,
        sortOrder: t.sortOrder,
        playMode: t.tagId === tagId && field === 'playMode' ? value : t.playMode,
        loopCount: t.tagId === tagId && field === 'loopCount' ? value : t.loopCount
    }));
    try {
        await utils.request(`/playlist/items/${itemId}/tags`, { method: 'PUT', body: JSON.stringify({ tags }) });
    } catch(e) { showToast('更新失败: '+e.message,'error'); }
}

async function removeItemTag(itemId, tagId) {
    const item = playlistData.items.find(x => x.id === itemId);
    if (!item) return;
    const tags = (item.tags||[]).filter(t => t.tagId !== tagId).map(t => ({
        tagId: t.tagId, sortOrder: t.sortOrder, playMode: t.playMode, loopCount: t.loopCount
    }));
    try {
        await utils.request(`/playlist/items/${itemId}/tags`, { method: 'PUT', body: JSON.stringify({ tags }) });
        showToast('已移除');
        loadPlaylistList();
    } catch(e) { showToast('移除失败: '+e.message,'error'); }
}

async function deleteItem(itemId) {
    if (!await showConfirm('确定删除此项目？')) return;
    try { await utils.request(`/playlist/items/${itemId}`,{method:'DELETE'}); loadPlaylistList(); showToast('删除成功'); }
    catch(e) { showToast('删除失败: '+e.message,'error'); }
}

async function playPlaylist() {
    try { await utils.request('/playlist/play', { method: 'POST' }); showToast('播放指令已发送'); }
    catch(e) { showToast('播放失败: '+e.message,'error'); }
}

async function stopPlaylist() {
    try { await utils.request('/playlist/stop', { method: 'POST' }); showToast('已发送停止指令'); }
    catch(e) { showToast('停止失败: '+e.message,'error'); }
}