let tagList = [];

async function loadTagList() {
    const el = document.getElementById('tagList');
    el.innerHTML = '<div class="loading">加载标签...</div>';
    try {
        const r = await utils.request('/tags');
        tagList = r.data || [];
        if (!tagList.length) el.innerHTML = '<div class="empty-state"><div class="empty-icon">🏷️</div><p>暂无标签</p></div>';
        else renderTagList();
    } catch (e) { el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${e.message}</p></div>`; showToast(e.message,'error'); }
}

function renderTagList() {
    document.getElementById('tagList').innerHTML = tagList.map(t => `<div class="list-item">
        <div class="dot" style="background:${t.color}"></div>
        <div class="name">${escHtml(t.name)}</div>
        <div class="meta">${t.mediaCount||0} 个媒体</div>
        <div class="actions">
            <button class="btn-ghost" onclick="viewTagMedia(${t.id})">查看媒体</button>
            <button class="btn-ghost" onclick="editTag(${t.id})">编辑</button>
            <button class="btn btn-danger btn-sm" onclick="deleteTag(${t.id})">删除</button>
        </div>
    </div>`).join('');
}

async function viewTagMedia(id) {
    try {
        const [tr, mr] = await Promise.all([
            utils.request(`/tags`),
            utils.request(`/media?tagId=${id}`)
        ]);
        const tag = (tr.data || []).find(t => t.id === id);
        const media = mr.data || [];
        showModal('标签: ' + escHtml(tag ? tag.name : ''), 
            `<div class="field"><label>媒体列表 (${media.length})</label>
            <div class="scroll" style="max-height:300px">${
                media.length ? media.map(m => `<div class="item-row"><span>${utils.mediaIcon(m.type)}</span><span style="margin-left:6px">${escHtml(m.name)}</span></div>`).join('')
                : '<span style="color:var(--muted);font-size:12px;">暂无媒体</span>'
            }</div></div>
            <button class="btn btn-primary" onclick="closeModal()" style="width:100%">关闭</button>`);
    } catch(e) {
        showToast('加载失败: ' + e.message, 'error');
    }
}

function showAddTagModal() {
    showModal('新建标签', `<div class="field"><label>名称</label><input type="text" id="tagName" placeholder="例如: 风景"></div>
        <div class="field"><label>颜色</label><input type="color" id="tagColor" value="#4dabf7"></div>
        <button class="btn btn-primary" onclick="createTag()">创建</button>`);
}

async function createTag() {
    const n = document.getElementById('tagName').value.trim();
    if (!n) { showToast('请输入名称','error'); return; }
    try { await utils.request('/tags',{method:'POST',body:JSON.stringify({name:n,color:document.getElementById('tagColor').value})}); closeModal(); loadTagList(); showToast('创建成功'); }
    catch(e) { showToast('创建失败: '+e.message,'error'); }
}

function editTag(id) {
    const t = tagList.find(x=>x.id===id); if(!t) return;
    showModal('编辑标签', `<div class="field"><label>名称</label><input type="text" id="editTagName" value="${escHtml(t.name)}"></div>
        <div class="field"><label>颜色</label><input type="color" id="editTagColor" value="${t.color}"></div>
        <button class="btn btn-primary" onclick="updateTag(${id})">保存</button>`);
}

async function updateTag(id) {
    const n = document.getElementById('editTagName').value.trim();
    if (!n) { showToast('请输入名称','error'); return; }
    try { await utils.request(`/tags/${id}`,{method:'PUT',body:JSON.stringify({name:n,color:document.getElementById('editTagColor').value})}); closeModal(); loadTagList(); showToast('更新成功'); }
    catch(e) { showToast('更新失败: '+e.message,'error'); }
}

async function deleteTag(id) {
    if(!await showConfirm('确定删除此标签？媒体文件不会被删除。')) return;
    try { await utils.request(`/tags/${id}`,{method:'DELETE'}); loadTagList(); showToast('删除成功'); }
    catch(e) { showToast('删除失败: '+e.message,'error'); }
}
