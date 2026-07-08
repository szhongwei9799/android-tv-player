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
function loopLabel(c) { return c===-1?'无限':c===0?'一次':`${c}次`; }
function trLabel(t) { return {NONE:'无',FADE:'淡入淡出',SLIDE_LEFT:'左滑',SLIDE_RIGHT:'右滑',SLIDE_UP:'上滑',SLIDE_DOWN:'下滑',ZOOM_IN:'放大',ZOOM_OUT:'缩小',WIPE_LEFT:'左擦除',WIPE_RIGHT:'右擦除',DISSOLVE:'溶解',BLUR:'模糊',RANDOM:'随机'}[t]||t; }
const transitions = ['FADE','SLIDE_LEFT','SLIDE_RIGHT','ZOOM_IN','ZOOM_OUT','DISSOLVE','RANDOM','NONE'];
const playModes = ['SEQUENTIAL','RANDOM','SHUFFLE'];
const loopOptions = [{-1:'无限'},{0:'一次'},{1:'1次'},{2:'2次'},{3:'3次'},{5:'5次'},{10:'10次'}].reduce((a,b)=>({...a,...b}),{});

function renderPlaylist() {
    const pl = playlistData.playlist;
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
                <span class="setting-item">标签 <select class="setting-select" onchange="updateSetting('tagPlayMode',this.value)">
                    ${playModes.map(v => `<option value="${v}" ${pl.tagPlayMode===v?'selected':''}>${pmLabel(v)}</option>`).join('')}</select></span>
                <span class="setting-item">循环 <select class="setting-select" onchange="updateSetting('tagLoopCount',parseInt(this.value))">
                    ${Object.entries(loopOptions).map(([k,v]) => `<option value="${k}" ${pl.tagLoopCount===parseInt(k)?'selected':''}>${v}</option>`).join('')}</select></span>
            </div>
        </div>
        <div style="font-size:13px;font-weight:600;margin:8px 0;color:var(--text2);">播放项目（标签列表）</div>
        ${playlistData.tags.map((t,i) => renderTagItem(t,i)).join('')}
    `;
}

async function updateSetting(field, value) {
    try {
        await utils.request('/playlist', { method: 'PUT', body: JSON.stringify({ [field]: value }) });
        showToast('已更新');
    } catch(e) { showToast('保存失败: '+e.message,'error'); }
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



function showAddTagModal() {
    showModal('新建播放项目', `<div class="field"><label>项目名称</label><input type="text" id="newTagName" placeholder="例如: 风景片"></div>
        <div class="field"><label>颜色</label><div style="display:flex;align-items:center;gap:8px;"><input type="color" id="newTagColor" value="#4dabf7"><span style="font-size:11px;color:var(--muted);">用于标识</span></div></div>
        <div class="field"><label>内部播放模式</label><select id="addTagPlayMode">
            <option value="SEQUENTIAL">顺序播放</option><option value="RANDOM">随机播放</option><option value="SHUFFLE">洗牌播放</option></select></div>
        <div class="field"><label>内部循环次数</label><select id="addTagLoopCount">
            <option value="-1">无限循环</option><option value="0">播放一次</option>
            <option value="1">循环1次</option><option value="2">循环2次</option><option value="3">循环3次</option>
            <option value="5">循环5次</option><option value="10">循环10次</option></select></div>
        <button class="btn btn-primary" onclick="addTagToPlaylist()">创建并添加</button>`);
}

async function addTagToPlaylist() {
    const name = document.getElementById('newTagName').value.trim();
    if (!name) { showToast('请输入项目名称','error'); return; }
    try {
        const tr = await utils.request('/tags', { method: 'POST', body: JSON.stringify({
            name, color: document.getElementById('newTagColor').value }) });
        const newTagId = tr.data.id;
        await utils.request('/playlist/tags', { method: 'POST', body: JSON.stringify({
            tagId: newTagId,
            playMode: document.getElementById('addTagPlayMode').value,
            loopCount: parseInt(document.getElementById('addTagLoopCount').value) }) });
        closeModal(); loadPlaylistList(); showToast('创建成功');
    } catch(e) { showToast('创建失败: '+e.message,'error'); }
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
