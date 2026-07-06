let mediaList = [];
let selectedMediaIds = new Set();

async function loadMediaList() {
    const grid = document.getElementById('mediaGrid');
    grid.innerHTML = '<div class="loading">加载媒体库...</div>';
    try {
        const res = await utils.request('/media');
        mediaList = res.data || [];
        if (mediaList.length === 0) {
            grid.innerHTML = '<div class="empty-state"><div class="empty-icon">📂</div><p>暂无媒体文件</p><p style="font-size:12px;margin-top:4px;">点击右上角「上传文件」或「添加网络媒体」开始</p></div>';
        } else {
            renderMediaGrid();
            loadTagsForFilter();
        }
    } catch (e) {
        grid.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>加载失败: ${e.message}</p></div>`;
        showToast(e.message, 'error');
    }
}

function renderMediaGrid() {
    const grid = document.getElementById('mediaGrid');
    grid.innerHTML = mediaList.map(m => `
        <div class="media-item ${selectedMediaIds.has(m.id) ? 'selected' : ''}"
             onclick="selectMedia(${m.id})" ondblclick="editMedia(${m.id})">
            <div class="media-thumbnail">${m.thumbnail ? `<img src="${m.thumbnail}" alt="${m.name}">` : utils.getMediaTypeIcon(m.type)}</div>
            <div class="media-info">
                <div class="media-name" title="${m.name.replace(/"/g,'&quot;')}">${escHtml(m.name)}</div>
                <div class="media-meta">${utils.getMediaTypeLabel(m.type)} · ${utils.formatFileSize(m.fileSize)}${m.duration ? ' · ' + utils.formatDuration(m.duration) : ''}</div>
                <div class="media-tags" id="tags-${m.id}"></div>
            </div>
        </div>
    `).join('');
    mediaList.forEach(m => loadMediaTags(m.id));
    updateMediaControls();
}

function escHtml(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}

async function loadMediaTags(mediaId) {
    try {
        const res = await utils.request(`/media/${mediaId}/tags`);
        if (res.data) {
            const el = document.getElementById(`tags-${mediaId}`);
            if (el) el.innerHTML = res.data.map(t =>
                `<span class="tag-badge" style="background:${t.color}22;color:${t.color}">${escHtml(t.name)}</span>`
            ).join('');
        }
    } catch (e) { /* silent */ }
}

function selectMedia(id) {
    selectedMediaIds.has(id) ? selectedMediaIds.delete(id) : selectedMediaIds.add(id);
    renderMediaGrid();
}

function updateMediaControls() {
    const el = document.getElementById('mediaControls');
    if (selectedMediaIds.size > 0) {
        el.style.display = 'flex';
        el.querySelector('#selectedCount').textContent = `已选择 ${selectedMediaIds.size} 项`;
    } else {
        el.style.display = 'none';
    }
}

function editMedia(id) {
    const media = mediaList.find(m => m.id === id);
    if (!media) return;
    showModal('编辑媒体', `
        <div class="form-row"><label>名称</label><input type="text" id="editMediaName" value="${escHtml(media.name)}"></div>
        <div class="form-row"><label>标签</label><div class="modal-checkbox-group" id="editMediaTags">加载中...</div></div>
        <button class="btn btn-primary" onclick="saveMedia(${id})">保存</button>
    `);
    loadMediaTagsForEdit(id);
}

async function loadMediaTagsForEdit(mediaId) {
    try {
        const [tagRes, allRes] = await Promise.all([
            utils.request(`/media/${mediaId}/tags`),
            utils.request('/tags')
        ]);
        const mediaTagIds = (tagRes.data || []).map(t => t.id);
        const allTags = allRes.data || [];
        document.getElementById('editMediaTags').innerHTML = allTags.map(t =>
            `<label><input type="checkbox" value="${t.id}" ${mediaTagIds.includes(t.id) ? 'checked' : ''}><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${t.color}"></span> ${escHtml(t.name)}</label>`
        ).join('');
    } catch (e) {
        document.getElementById('editMediaTags').innerHTML = '<span style="color:var(--danger)">加载标签失败</span>';
    }
}

async function saveMedia(id) {
    const name = document.getElementById('editMediaName').value.trim();
    if (!name) { showToast('请输入名称', 'error'); return; }
    const tagCbs = document.querySelectorAll('#editMediaTags input:checked');
    const tagIds = Array.from(tagCbs).map(c => parseInt(c.value));
    try {
        await utils.request(`/media/${id}`, { method: 'PUT', body: JSON.stringify({ name }) });
        for (const tagId of tagIds) {
            try { await utils.request(`/media/${id}/tags`, { method: 'POST', body: JSON.stringify({ tagId }) }); } catch (e) { /* ignore tag errors */ }
        }
        closeModal();
        loadMediaList();
        showToast('保存成功');
    } catch (e) {
        showToast('保存失败: ' + e.message, 'error');
    }
}

async function deleteSelectedMedia() {
    if (selectedMediaIds.size === 0) return;
    const ok = await showConfirm(`确定要删除选中的 ${selectedMediaIds.size} 个媒体文件吗？此操作不可撤销。`);
    if (!ok) return;
    try {
        for (const id of selectedMediaIds) {
            await utils.request(`/media/${id}`, { method: 'DELETE' });
        }
        selectedMediaIds.clear();
        loadMediaList();
        showToast('删除成功');
    } catch (e) {
        showToast('删除失败: ' + e.message, 'error');
    }
}

function filterMedia() {
    const type = document.getElementById('mediaTypeFilter').value;
    const tagId = document.getElementById('mediaTagFilter').value;
    const query = document.getElementById('mediaSearch').value.toLowerCase();
    let filtered = mediaList;
    if (type) filtered = filtered.filter(m => m.type === type);
    if (query) filtered = filtered.filter(m => m.name.toLowerCase().includes(query));
    const grid = document.getElementById('mediaGrid');
    if (filtered.length === 0) {
        grid.innerHTML = '<div class="empty-state"><div class="empty-icon">🔍</div><p>没有匹配的媒体</p></div>';
        return;
    }
    grid.innerHTML = filtered.map(m => `
        <div class="media-item ${selectedMediaIds.has(m.id) ? 'selected' : ''}"
             onclick="selectMedia(${m.id})" ondblclick="editMedia(${m.id})">
            <div class="media-thumbnail">${m.thumbnail ? `<img src="${m.thumbnail}" alt="${m.name}">` : utils.getMediaTypeIcon(m.type)}</div>
            <div class="media-info">
                <div class="media-name" title="${m.name.replace(/"/g,'&quot;')}">${escHtml(m.name)}</div>
                <div class="media-meta">${utils.getMediaTypeLabel(m.type)} · ${utils.formatFileSize(m.fileSize)}</div>
            </div>
        </div>
    `).join('');
}

async function loadTagsForFilter() {
    try {
        const res = await utils.request('/tags');
        const tags = res.data || [];
        document.getElementById('mediaTagFilter').innerHTML = '<option value="">全部标签</option>' +
            tags.map(t => `<option value="${t.id}">${escHtml(t.name)}</option>`).join('');
    } catch (e) { /* silent */ }
}

function showUploadModal() {
    const el = document.getElementById('uploadArea');
    el.style.display = el.style.display === 'none' ? 'block' : 'none';
    if (el.style.display === 'block') el.scrollIntoView({ behavior: 'smooth' });
}

function showAddMediaModal() {
    showModal('添加网络媒体', `
        <div class="form-row"><label>媒体名称</label><input type="text" id="networkMediaName" placeholder="示例: 电影《阿凡达》"></div>
        <div class="form-row"><label>媒体 URL</label><input type="url" id="networkMediaUrl" placeholder="https://example.com/video.mp4"></div>
        <div class="form-row"><label>媒体类型</label>
            <select id="networkMediaType">
                <option value="VIDEO">视频</option>
                <option value="IMAGE">图片</option>
                <option value="AUDIO">音频</option>
                <option value="STREAM">流媒体</option>
            </select>
        </div>
        <button class="btn btn-primary" onclick="addNetworkMedia()">添加</button>
    `);
}

async function addNetworkMedia() {
    const name = document.getElementById('networkMediaName').value.trim();
    const url = document.getElementById('networkMediaUrl').value.trim();
    const type = document.getElementById('networkMediaType').value;
    if (!name || !url) { showToast('请填写完整信息', 'error'); return; }
    try {
        await utils.request('/media', { method: 'POST', body: JSON.stringify({ name, path: url, type, source: 'NETWORK' }) });
        closeModal();
        loadMediaList();
        showToast('添加成功');
    } catch (e) {
        showToast('添加失败: ' + e.message, 'error');
    }
}
