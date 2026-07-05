// 标签管理

let tagList = [];

// 加载标签列表
async function loadTagList() {
    try {
        const response = await utils.request('/tags');
        if (response.success) {
            tagList = response.data || [];
            renderTagList();
        }
    } catch (error) {
        console.error('加载标签列表失败:', error);
    }
}

// 渲染标签列表
function renderTagList() {
    const container = document.getElementById('tagList');
    container.innerHTML = tagList.map(tag => `
        <div class="tag-item">
            <div class="tag-color" style="background-color: ${tag.color}"></div>
            <div class="tag-name">${tag.name}</div>
            <div class="tag-count">${tag.mediaCount || 0} 个媒体</div>
            <div class="tag-actions">
                <button class="btn btn-secondary btn-small" onclick="editTag(${tag.id})">编辑</button>
                <button class="btn btn-danger btn-small" onclick="deleteTag(${tag.id})">删除</button>
            </div>
        </div>
    `).join('');
}

// 显示添加标签模态框
function showAddTagModal() {
    showModal(`
        <h3>新建标签</h3>
        <div class="form-group">
            <label>标签名称</label>
            <input type="text" id="tagName" placeholder="请输入标签名称">
        </div>
        <div class="form-group">
            <label>标签颜色</label>
            <input type="color" id="tagColor" value="#1976d2">
        </div>
        <button class="btn btn-primary" onclick="createTag()">创建</button>
    `);
}

// 创建标签
async function createTag() {
    const name = document.getElementById('tagName').value;
    const color = document.getElementById('tagColor').value;
    
    if (!name) {
        showToast('请输入标签名称', 'error');
        return;
    }
    
    try {
        await utils.request('/tags', {
            method: 'POST',
            body: JSON.stringify({ name, color })
        });
        closeModal();
        loadTagList();
        showToast('创建成功');
    } catch (error) {
        showToast('创建失败', 'error');
    }
}

// 编辑标签
function editTag(id) {
    const tag = tagList.find(t => t.id === id);
    if (!tag) return;
    
    showModal(`
        <h3>编辑标签</h3>
        <div class="form-group">
            <label>标签名称</label>
            <input type="text" id="editTagName" value="${tag.name}">
        </div>
        <div class="form-group">
            <label>标签颜色</label>
            <input type="color" id="editTagColor" value="${tag.color}">
        </div>
        <button class="btn btn-primary" onclick="updateTag(${id})">保存</button>
    `);
}

// 更新标签
async function updateTag(id) {
    const name = document.getElementById('editTagName').value;
    const color = document.getElementById('editTagColor').value;
    
    if (!name) {
        showToast('请输入标签名称', 'error');
        return;
    }
    
    try {
        await utils.request(`/tags/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ name, color })
        });
        closeModal();
        loadTagList();
        showToast('更新成功');
    } catch (error) {
        showToast('更新失败', 'error');
    }
}

// 删除标签
async function deleteTag(id) {
    if (!confirm('确定要删除这个标签吗？标签关联的媒体不会被删除。')) {
        return;
    }
    
    try {
        await utils.request(`/tags/${id}`, { method: 'DELETE' });
        loadTagList();
        showToast('删除成功');
    } catch (error) {
        showToast('删除失败', 'error');
    }
}
