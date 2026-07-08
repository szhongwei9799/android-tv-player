let playlistData = null;

async function loadPlaylistList() {
    const el = document.getElementById('playlistList');
    el.innerHTML = '<div class="loading">加载播放项目...</div>';
    try {
        const r = await utils.request('/playlist');
        playlistData = r.data;
        if (!playlistData || !playlistData.tags || !playlistData.tags.length) {
            el.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><p>暂无播放项目</p><p style="font-size:12px;margin-top:6px;">点击右上角「新建播放项目」添加标签到播放列表</p></div>';
        } else renderPlaylist();
    } catch(e) { el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${e.message}</p></div>`; showToast(e.message,'error'); }
}

function pmLabel(t) { return {SEQUENTIAL:'顺序',RANDOM:'随机',SHUFFLE:'洗牌'}[t]||t; }
function loopLabel(c) { return c===-1?'无限循环':c===0?'播放一次':`循环${c}次`; }
function trLabel(t) { return {NONE:'无',FADE:'淡入淡出',SLIDE_LEFT:'左滑',SLIDE_RIGHT:'右滑',SLIDE_UP:'上滑',SLIDE_DOWN:'下滑',ZOOM_IN:'放大',ZOOM_OUT:'缩小',WIPE_LEFT:'左擦除',WIPE_RIGHT:'右擦除',DISSOLVE:'溶解',BLUR:'模糊',RANDOM:'随机'}[t]||t; }

function renderPlaylist() {
    const pl = playlistData.playlist;
    document.getElementById('playlistList').innerHTML = `
        <div style="margin-bottom:16px;padding:14px;background:var(--surface2);border-radius:10px;">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
                <strong style="font-size:16px;">${escHtml(pl.name)}</strong>
                <button class="btn btn-ghost btn-sm" onclick="showPlaylistSettings()">设置</button>
            </div>
            <div style="font-size:12px;color:var(--muted);display:flex;gap:12px;flex-wrap:wrap;">
                <span>转场: ${trLabel(pl.transitionEffect)}</span>
                <span>间隔: ${pl.defaultInterval}s</span>
                <span>标签模式: ${pmLabel(pl.tagPlayMode)}</span>
                <span>标签循环: ${loopLabel(pl.tagLoopCount)}</span>
            </div>
            <div style="margin-top:10px;display:flex;gap:6px;">
                <button class="btn btn-primary btn-sm" onclick="playPlaylist()">播放</button>
                <button class="btn btn-warning btn-sm" onclick="stopPlaylist()">停止</button>
            </div>
        </div>
        <div style="font-size:13px;font-weight:600;margin:8px 0;color:var(--text2);">播放项目（标签列表）</div>
        ${playlistData.tags.map((t,i) => renderTagItem(t,i)).join('')}
    `;
}

function renderTagItem(t, i) {
    return `<div class="list-item" style="border-left:3px solid ${t.color};">
        <div class="pl-icon">${i+1}</div>
        <div class="name">${escHtml(t.name)}</div>
        <div class="meta">${t.mediaCount} 个媒体 · ${pmLabel(t.playMode)} · ${loopLabel(t.loopCount)}</div>
        <div class="actions">
            <button class="btn-ghost" onclick="showEditTagModal(${t.tagId})">编辑</button>
            <button class="btn btn-danger btn-sm" onclick="removeTag(${t.tagId})">移除</button>
        </div>
    </div>`;
}

function showPlaylistSettings() {
    const pl = playlistData.playlist;
    showModal('播放列表设置', `<div class="field"><label>名称</label><input type="text" id="plName" value="${escHtml(pl.name)}"></div>
        <div class="field"><label>描述</label><input type="text" id="plDesc" value="${escHtml(pl.description||'')}"></div>
        <div class="field"><label>转场</label><select id="plTransition">
            ${['FADE','SLIDE_LEFT','SLIDE_RIGHT','ZOOM_IN','ZOOM_OUT','DISSOLVE','RANDOM','NONE'].map(v => `<option value="${v}" ${pl.transitionEffect===v?'selected':''}>${trLabel(v)}</option>`).join('')}</select></div>
        <div class="field"><label>间隔</label><input type="number" id="plInterval" value="${pl.defaultInterval}" min="1" max="60"><span class="unit">秒</span></div>
        <div class="field"><label>标签播放模式</label><select id="plTagPlayMode">
            <option value="SEQUENTIAL" ${pl.tagPlayMode==='SEQUENTIAL'?'selected':''}>顺序播放</option>
            <option value="RANDOM" ${pl.tagPlayMode==='RANDOM'?'selected':''}>随机播放</option>
            <option value="SHUFFLE" ${pl.tagPlayMode==='SHUFFLE'?'selected':''}>洗牌播放</option></select></div>
        <div class="field"><label>标签循环次数</label><select id="plTagLoopCount">
            <option value="-1" ${pl.tagLoopCount===-1?'selected':''}>无限循环</option>
            <option value="0" ${pl.tagLoopCount===0?'selected':''}>播放一次</option>
            <option value="1" ${pl.tagLoopCount===1?'selected':''}>循环1次</option>
            <option value="2" ${pl.tagLoopCount===2?'selected':''}>循环2次</option>
            <option value="3" ${pl.tagLoopCount===3?'selected':''}>循环3次</option>
            <option value="5" ${pl.tagLoopCount===5?'selected':''}>循环5次</option>
            <option value="10" ${pl.tagLoopCount===10?'selected':''}>循环10次</option></select></div>
        <button class="btn btn-primary" onclick="updatePlaylistSettings()">保存</button>`);
}

async function updatePlaylistSettings() {
    try {
        await utils.request('/playlist', { method: 'PUT', body: JSON.stringify({
            name: document.getElementById('plName').value.trim(),
            description: document.getElementById('plDesc').value,
            transitionEffect: document.getElementById('plTransition').value,
            defaultInterval: parseInt(document.getElementById('plInterval').value),
            tagPlayMode: document.getElementById('plTagPlayMode').value,
            tagLoopCount: parseInt(document.getElementById('plTagLoopCount').value) }) });
        closeModal(); loadPlaylistList(); showToast('保存成功');
    } catch(e) { showToast('保存失败: '+e.message,'error'); }
}

function showAddTagModal() {
    showModal('新建播放项目', `<div class="field"><label>标签</label><div class="check-group" id="addTagSelect">加载中...</div></div>
        <div class="field"><label>内部播放模式</label><select id="addTagPlayMode">
            <option value="SEQUENTIAL">顺序播放</option><option value="RANDOM">随机播放</option><option value="SHUFFLE">洗牌播放</option></select></div>
        <div class="field"><label>内部循环次数</label><select id="addTagLoopCount">
            <option value="-1">无限循环</option><option value="0">播放一次</option>
            <option value="1">循环1次</option><option value="2">循环2次</option><option value="3">循环3次</option>
            <option value="5">循环5次</option><option value="10">循环10次</option></select></div>
        <button class="btn btn-primary" onclick="addTagToPlaylist()">添加</button>`);
    loadAvailableTags();
}

async function loadAvailableTags() {
    try {
        const [tr, pr] = await Promise.all([utils.request('/tags'), utils.request('/playlist')]);
        const allTags = tr.data || [];
        const existingIds = new Set((pr.data?.tags||[]).map(t => t.tagId));
        const avail = allTags.filter(t => !existingIds.has(t.id));
        document.getElementById('addTagSelect').innerHTML = avail.length
            ? avail.map(t => `<label><input type="radio" name="addTagRadio" value="${t.id}"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('')
            : '<span style="color:var(--muted);font-size:12px;">所有标签已在列表中</span>';
    } catch(_) { document.getElementById('addTagSelect').innerHTML = '<span style="color:var(--red);font-size:12px;">加载失败</span>'; }
}

async function addTagToPlaylist() {
    const sel = document.querySelector('input[name="addTagRadio"]:checked');
    if (!sel) { showToast('请选择一个标签','error'); return; }
    try {
        await utils.request('/playlist/tags', { method: 'POST', body: JSON.stringify({
            tagId: parseInt(sel.value),
            playMode: document.getElementById('addTagPlayMode').value,
            loopCount: parseInt(document.getElementById('addTagLoopCount').value) }) });
        closeModal(); loadPlaylistList(); showToast('添加成功');
    } catch(e) { showToast('添加失败: '+e.message,'error'); }
}

function showEditTagModal(tagId) {
    const t = playlistData.tags.find(x => x.tagId === tagId);
    if (!t) return;
    showModal('编辑播放项目', `<div class="field"><label>标签</label><strong>${escHtml(t.name)}</strong></div>
        <div class="field"><label>播放模式</label><select id="editTagPlayMode">
            <option value="SEQUENTIAL" ${t.playMode==='SEQUENTIAL'?'selected':''}>顺序播放</option>
            <option value="RANDOM" ${t.playMode==='RANDOM'?'selected':''}>随机播放</option>
            <option value="SHUFFLE" ${t.playMode==='SHUFFLE'?'selected':''}>洗牌播放</option></select></div>
        <div class="field"><label>循环次数</label><select id="editTagLoopCount">
            <option value="-1" ${t.loopCount===-1?'selected':''}>无限循环</option>
            <option value="0" ${t.loopCount===0?'selected':''}>播放一次</option>
            <option value="1" ${t.loopCount===1?'selected':''}>循环1次</option>
            <option value="2" ${t.loopCount===2?'selected':''}>循环2次</option>
            <option value="3" ${t.loopCount===3?'selected':''}>循环3次</option>
            <option value="5" ${t.loopCount===5?'selected':''}>循环5次</option>
            <option value="10" ${t.loopCount===10?'selected':''}>循环10次</option></select></div>
        <button class="btn btn-primary" onclick="updateTag(${tagId})">保存</button>`);
}

async function updateTag(tagId) {
    try {
        await utils.request(`/playlist/tags/${tagId}`, { method: 'PUT', body: JSON.stringify({
            playMode: document.getElementById('editTagPlayMode').value,
            loopCount: parseInt(document.getElementById('editTagLoopCount').value) }) });
        closeModal(); loadPlaylistList(); showToast('更新成功');
    } catch(e) { showToast('更新失败: '+e.message,'error'); }
}

async function removeTag(tagId) {
    if (!await showConfirm('确定从播放列表中移除此标签？媒体文件不会被删除。')) return;
    try { await utils.request(`/playlist/tags/${tagId}`,{method:'DELETE'}); loadPlaylistList(); showToast('移除成功'); }
    catch(e) { showToast('移除失败: '+e.message,'error'); }
}

async function playPlaylist() {
    try {
        await utils.request('/playlist/play', { method: 'POST' });
        showToast('播放指令已发送');
    } catch(e) { showToast('播放失败: '+e.message,'error'); }
}

async function stopPlaylist() {
    try {
        await utils.request('/playlist/stop', { method: 'POST' });
        showToast('已发送停止指令');
    } catch(e) { showToast('停止失败: '+e.message,'error'); }
}
