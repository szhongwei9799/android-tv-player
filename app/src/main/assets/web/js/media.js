let mediaList = [], selectedMediaIds = new Set();

async function loadMediaList() {
    const g = document.getElementById('mediaGrid');
    g.innerHTML = '<div class="loading">加载媒体库...</div>';
    try {
        const r = await utils.request('/media');
        mediaList = r.data || [];
        if (!mediaList.length) {
            g.innerHTML = '<div class="empty-state"><div class="empty-icon">📂</div><p style="font-size:14px;margin-bottom:3px;">暂无媒体文件</p><p style="font-size:12px;">点击右上角「上传」或「网络媒体」添加</p></div>';
        } else { renderMediaGrid(); loadTagsForFilter(); }
    } catch (e) {
        g.innerHTML = `<div class="empty-state"><div class="empty-icon" style="opacity:.5;">⚠️</div><p>${e.message}</p></div>`;
        showToast(e.message, 'error');
    }
}

function renderMediaGrid() {
    const g = document.getElementById('mediaGrid');
    g.innerHTML = mediaList.map(m => `<div class="media-item ${selectedMediaIds.has(m.id)?'selected':''}" onclick="selectMedia(${m.id})" ondblclick="editMedia(${m.id})">
        <div class="media-thumb">${m.thumbnail ? `<img src="${m.thumbnail}" alt="">` : utils.mediaIcon(m.type)}</div>
        <div class="media-info">
            <div class="media-name" title="${escHtml(m.name)}">${escHtml(m.name)}</div>
            <div class="media-meta">${utils.mediaLabel(m.type)} · ${utils.formatFileSize(m.fileSize)}${m.duration ? ' · '+utils.formatDuration(m.duration) : ''}</div>
            <div class="media-tags" id="tags-${m.id}"></div>
        </div>
    </div>`).join('');
    mediaList.forEach(m => loadMediaTags(m.id));
    updateMediaControls();
}

async function loadMediaTags(id) {
    try {
        const r = await utils.request(`/media/${id}/tags`);
        if (r.data) {
            const el = document.getElementById(`tags-${id}`);
            if (el) el.innerHTML = r.data.map(t => `<span class="tag-badge" style="background:${t.color}22;color:${t.color}">${escHtml(t.name)}</span>`).join('');
        }
    } catch (_) {}
}

function selectMedia(id) {
    selectedMediaIds.has(id) ? selectedMediaIds.delete(id) : selectedMediaIds.add(id);
    renderMediaGrid();
}

function updateMediaControls() {
    const el = document.getElementById('mediaControls');
    if (selectedMediaIds.size > 0) { el.style.display = 'flex'; el.querySelector('#selectedCount').textContent = `已选择 ${selectedMediaIds.size} 项`; }
    else el.style.display = 'none';
}

function editMedia(id) {
    const m = mediaList.find(x => x.id === id);
    if (!m) return;
    showModal('编辑媒体', `<div class="field"><label>名称</label><input type="text" id="editMediaName" value="${escHtml(m.name)}"></div>
        <div class="field"><label>标签</label><div class="check-group" id="editMediaTags">加载中...</div></div>
        <button class="btn btn-primary" onclick="saveMedia(${id})">保存</button>`);
    loadMediaTagsForEdit(id);
}

async function loadMediaTagsForEdit(id) {
    try {
        const [tr, ar] = await Promise.all([utils.request(`/media/${id}/tags`), utils.request('/tags')]);
        const mt = (tr.data || []).map(t => t.id), at = ar.data || [];
        document.getElementById('editMediaTags').innerHTML = at.map(t => `<label><input type="checkbox" value="${t.id}" ${mt.includes(t.id)?'checked':''}><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('') || '<span style="color:var(--muted);font-size:12px;">暂无标签</span>';
    } catch (_) { document.getElementById('editMediaTags').innerHTML = '<span style="color:var(--red);font-size:12px;">加载失败</span>'; }
}

async function saveMedia(id) {
    const name = document.getElementById('editMediaName').value.trim();
    if (!name) { showToast('请输入名称', 'error'); return; }
    const checkedTagIds = Array.from(document.querySelectorAll('#editMediaTags input:checked')).map(c => parseInt(c.value));
    try {
        await utils.request(`/media/${id}`, { method: 'PUT', body: JSON.stringify({ name }) });
        const r = await utils.request(`/media/${id}/tags`);
        const currentTagIds = (r.data || []).map(t => t.id);
        const toAdd = checkedTagIds.filter(ti => !currentTagIds.includes(ti));
        const toRemove = currentTagIds.filter(ti => !checkedTagIds.includes(ti));
        for (const ti of toRemove) { try { await utils.request(`/media/${id}/tags/${ti}`, { method: 'DELETE' }); } catch(_) {} }
        for (const ti of toAdd) { try { await utils.request(`/media/${id}/tags`, { method: 'POST', body: JSON.stringify({ tagId: ti }) }); } catch(_) {} }
        closeModal(); loadMediaList(); showToast('保存成功');
    } catch (e) { showToast('保存失败: ' + e.message, 'error'); }
}

async function deleteSelectedMedia() {
    if (!selectedMediaIds.size) return;
    if (!await showConfirm(`确定删除选中的 ${selectedMediaIds.size} 个媒体？不可撤销。`)) return;
    try {
        for (const id of selectedMediaIds) await utils.request(`/media/${id}`, { method: 'DELETE' });
        selectedMediaIds.clear(); loadMediaList(); showToast('删除成功');
    } catch (e) { showToast('删除失败: ' + e.message, 'error'); }
}

async function filterMedia() {
    const type = document.getElementById('mediaTypeFilter').value;
    const tagId = document.getElementById('mediaTagFilter').value;
    const q = document.getElementById('mediaSearch').value.toLowerCase();
    const g = document.getElementById('mediaGrid');
    g.innerHTML = '<div class="loading">筛选...</div>';
    try {
        let url = '/media';
        const params = [];
        if (type) params.push(`type=${type}`);
        if (tagId) params.push(`tagId=${tagId}`);
        if (params.length) url += '?' + params.join('&');
        const r = await utils.request(url);
        let f = r.data || [];
        if (q) f = f.filter(m => m.name.toLowerCase().includes(q));
        if (!f.length) { g.innerHTML = '<div class="empty-state"><div class="empty-icon">🔍</div><p>没有匹配的媒体</p></div>'; return; }
        g.innerHTML = f.map(m => `<div class="media-item ${selectedMediaIds.has(m.id)?'selected':''}" onclick="selectMedia(${m.id})" ondblclick="editMedia(${m.id})">
            <div class="media-thumb">${m.thumbnail ? `<img src="${m.thumbnail}" alt="">` : utils.mediaIcon(m.type)}</div>
            <div class="media-info"><div class="media-name" title="${escHtml(m.name)}">${escHtml(m.name)}</div>
            <div class="media-meta">${utils.mediaLabel(m.type)} · ${utils.formatFileSize(m.fileSize)}</div></div>
        </div>`).join('');
        f.forEach(m => loadMediaTags(m.id));
    } catch (e) { g.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${e.message}</p></div>`; }
}

async function loadTagsForFilter() {
    try {
        const r = await utils.request('/tags');
        const tags = r.data || [];
        document.getElementById('mediaTagFilter').innerHTML = '<option value="">全部标签</option>' + tags.map(t => `<option value="${t.id}">${escHtml(t.name)}</option>`).join('');
    } catch (_) {}
}

async function batchTagMedia() {
    if (!selectedMediaIds.size) return;
    try {
        const r = await utils.request('/tags');
        const tags = r.data || [];
        if (!tags.length) { showToast('暂无标签', 'error'); return; }
        showModal('批量分配标签', 
            `<div class="field"><label>已选择 ${selectedMediaIds.size} 个媒体</label>
            <div class="check-group" id="batchTagCheckboxes">${
                tags.map(t => `<label><input type="checkbox" value="${t.id}"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('')
            }</div></div>
            <button class="btn btn-primary" onclick="applyBatchTags()" style="width:100%">应用</button>`);
        window._batchTagIds = null;
    } catch(e) { showToast('加载标签失败', 'error'); }
}

async function applyBatchTags() {
    const tagIds = Array.from(document.querySelectorAll('#batchTagCheckboxes input:checked')).map(c => parseInt(c.value));
    if (!tagIds.length) { showToast('请选择标签', 'error'); return; }
    closeModal();
    let ok = 0, fail = 0;
    for (const mid of selectedMediaIds) {
        for (const ti of tagIds) {
            try { await utils.request(`/media/${mid}/tags`, { method: 'POST', body: JSON.stringify({ tagId: ti }) }); ok++; }
            catch(_) { fail++; }
        }
    }
    selectedMediaIds.clear(); loadMediaList(); updateMediaControls();
    showToast(`分配完成: ${ok} 成功, ${fail} 失败`);
}

function showUploadModal() {
    const el = document.getElementById('uploadArea');
    if (el.style.display === 'none') { el.style.display = 'block'; el.scrollIntoView({behavior:'smooth'}); }
    else el.style.display = 'none';
}

function showAddMediaModal() {
    showModal('添加网络媒体', `<div class="field"><label>名称</label><input type="text" id="networkMediaName" placeholder="例如: 风景视频"></div>
        <div class="field"><label>URL</label><input type="url" id="networkMediaUrl" placeholder="https://example.com/video.mp4"></div>
        <div class="field"><label>类型</label><select id="networkMediaType">
            <option value="VIDEO">视频</option><option value="IMAGE">图片</option><option value="AUDIO">音频</option><option value="STREAM">流媒体</option>
        </select></div>
        <button class="btn btn-primary" onclick="addNetworkMedia()">添加</button>`);
}

async function addNetworkMedia() {
    const name = document.getElementById('networkMediaName').value.trim();
    const url = document.getElementById('networkMediaUrl').value.trim();
    const type = document.getElementById('networkMediaType').value;
    if (!name || !url) { showToast('请填写完整信息', 'error'); return; }
    try {
        await utils.request('/media', { method: 'POST', body: JSON.stringify({ name, path: url, type, source: 'NETWORK' }) });
        closeModal(); loadMediaList(); showToast('添加成功');
    } catch (e) { showToast('添加失败: ' + e.message, 'error'); }
}
