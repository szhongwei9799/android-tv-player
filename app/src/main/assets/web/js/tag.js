let tagList = [];

async function loadTagList() {
    const el = document.getElementById('tagList');
    el.innerHTML = '<div class="loading">加载标签...</div>';
    try {
        const res = await utils.request('/tags');
        tagList = res.data || [];
        if (tagList.length === 0) {
            el.innerHTML = '<div class="empty-state"><div class="empty-icon">🏷️</div><p>暂无标签</p><p style="font-size:12px;margin-top:4px;">点击右上角「新建标签」创建</p></div>';
        } else {
            renderTagList();
        }
    } catch (e) {
        el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>加载失败: ${e.message}</p></div>`;
        showToast(e.message, 'error');
    }
}

function renderTagList() {
    const el = document.getElementById('tagList');
    el.innerHTML = tagList.map(t => `
        <div class="tag-item">
            <div class="tag-color-dot" style="background:${t.color}"></div>
            <div class="tag-name">${escHtml(t.name)}</div>
            <div class="tag-count">${t.mediaCount || 0} 个媒体</div>
            <div class="tag-actions">
                <button class="btn btn-ghost" onclick="editTag(${t.id})">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="deleteTag(${t.id})">删除</button>
            </div>
        </div>
    `).join('');
}

function showAddTagModal() {
    showModal('新建标签', `
        <div class="form-row"><label>标签名称</label><input type="text" id="tagName" placeholder="例如: 风景、新闻"></div>
        <div class="form-row"><label>标签颜色</label><input type="color" id="tagColor" value="#4dabf7"></div>
        <button class="btn btn-primary" onclick="createTag()">创建</button>
    `);
}

async function createTag() {
    const name = document.getElementById('tagName').value.trim();
    const color = document.getElementById('tagColor').value;
    if (!name) { showToast('请输入标签名称', 'error'); return; }
    try {
        await utils.request('/tags', { method: 'POST', body: JSON.stringify({ name, color }) });
        closeModal();
        loadTagList();
        showToast('创建成功');
    } catch (e) {
        showToast('创建失败: ' + e.message, 'error');
    }
}

function editTag(id) {
    const tag = tagList.find(t => t.id === id);
    if (!tag) return;
    showModal('编辑标签', `
        <div class="form-row"><label>标签名称</label><input type="text" id="editTagName" value="${escHtml(tag.name)}"></div>
        <div class="form-row"><label>标签颜色</label><input type="color" id="editTagColor" value="${tag.color}"></div>
        <button class="btn btn-primary" onclick="updateTag(${id})">保存</button>
    `);
}

async function updateTag(id) {
    const name = document.getElementById('editTagName').value.trim();
    const color = document.getElementById('editTagColor').value;
    if (!name) { showToast('请输入标签名称', 'error'); return; }
    try {
        await utils.request(`/tags/${id}`, { method: 'PUT', body: JSON.stringify({ name, color }) });
        closeModal();
        loadTagList();
        showToast('更新成功');
    } catch (e) {
        showToast('更新失败: ' + e.message, 'error');
    }
}

async function deleteTag(id) {
    const ok = await showConfirm('确定要删除这个标签吗？媒体文件不会被删除。');
    if (!ok) return;
    try {
        await utils.request(`/tags/${id}`, { method: 'DELETE' });
        loadTagList();
        showToast('删除成功');
    } catch (e) {
        showToast('删除失败: ' + e.message, 'error');
    }
}
