// 媒体库管理

let mediaList = [];
let selectedMediaIds = new Set();

// 加载媒体列表
async function loadMediaList() {
    try {
        const response = await utils.request('/media');
        if (response.success) {
            mediaList = response.data || [];
            renderMediaGrid();
            loadTagsForFilter();
        }
    } catch (error) {
        console.error('加载媒体列表失败:', error);
    }
}

// 渲染媒体网格
function renderMediaGrid() {
    const grid = document.getElementById('mediaGrid');
    grid.innerHTML = mediaList.map(media => `
        <div class="media-item ${selectedMediaIds.has(media.id) ? 'selected' : ''}" 
             onclick="selectMedia(${media.id})" 
             ondblclick="editMedia(${media.id})">
            <div class="media-thumbnail">
                ${media.thumbnail ? 
                    `<img src="${media.thumbnail}" alt="${media.name}" style="width:100%;height:100%;object-fit:cover;">` :
                    utils.getMediaTypeIcon(media.type)
                }
            </div>
            <div class="media-info">
                <div class="media-name" title="${media.name}">${media.name}</div>
                <div class="media-meta">
                    ${utils.getMediaTypeLabel(media.type)} | ${utils.formatFileSize(media.fileSize)}
                    ${media.duration ? ` | ${utils.formatDuration(media.duration)}` : ''}
                </div>
                <div class="media-tags" id="tags-${media.id}"></div>
            </div>
        </div>
    `).join('');
    
    // 加载每个媒体的标签
    mediaList.forEach(media => loadMediaTags(media.id));
}

// 加载媒体标签
async function loadMediaTags(mediaId) {
    try {
        const response = await utils.request(`/media/${mediaId}/tags`);
        if (response.success && response.data) {
            const tagsContainer = document.getElementById(`tags-${mediaId}`);
            if (tagsContainer) {
                tagsContainer.innerHTML = response.data.map(tag => 
                    `<span class="tag-badge" style="background-color:${tag.color}20;color:${tag.color}">${tag.name}</span>`
                ).join('');
            }
        }
    } catch (error) {
        console.error('加载媒体标签失败:', error);
    }
}

// 选择媒体
function selectMedia(id) {
    if (selectedMediaIds.has(id)) {
        selectedMediaIds.delete(id);
    } else {
        selectedMediaIds.add(id);
    }
    renderMediaGrid();
}

// 编辑媒体
function editMedia(id) {
    const media = mediaList.find(m => m.id === id);
    if (!media) return;
    
    showModal(`
        <h3>编辑媒体</h3>
        <div class="form-group">
            <label>名称</label>
            <input type="text" id="editMediaName" value="${media.name}">
        </div>
        <div class="form-group">
            <label>标签</label>
            <div id="editMediaTags">加载中...</div>
        </div>
        <button class="btn btn-primary" onclick="saveMedia(${id})">保存</button>
    `);
    
    loadMediaTagsForEdit(id);
}

// 加载编辑媒体的标签
async function loadMediaTagsForEdit(mediaId) {
    try {
        const [mediaTagsResponse, allTagsResponse] = await Promise.all([
            utils.request(`/media/${mediaId}/tags`),
            utils.request('/tags')
        ]);
        
        if (mediaTagsResponse.success && allTagsResponse.success) {
            const mediaTagIds = (mediaTagsResponse.data || []).map(t => t.id);
            const allTags = allTagsResponse.data || [];
            
            document.getElementById('editMediaTags').innerHTML = allTags.map(tag => `
                <label style="display:flex;align-items:center;gap:8px;margin:8px 0;cursor:pointer;">
                    <input type="checkbox" value="${tag.id}" ${mediaTagIds.includes(tag.id) ? 'checked' : ''}>
                    <span style="display:inline-block;width:12px;height:12px;border-radius:50%;background-color:${tag.color}"></span>
                    ${tag.name}
                </label>
            `).join('');
        }
    } catch (error) {
        console.error('加载标签失败:', error);
    }
}

// 保存媒体
async function saveMedia(id) {
    const name = document.getElementById('editMediaName').value;
    const tagCheckboxes = document.querySelectorAll('#editMediaTags input[type="checkbox"]:checked');
    const tagIds = Array.from(tagCheckboxes).map(cb => parseInt(cb.value));
    
    try {
        // 更新媒体名称
        await utils.request(`/media/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ name })
        });
        
        // 更新标签
        for (const tagId of tagIds) {
            await utils.request(`/media/${id}/tags`, {
                method: 'POST',
                body: JSON.stringify({ tagId })
            });
        }
        
        closeModal();
        loadMediaList();
        showToast('保存成功');
    } catch (error) {
        showToast('保存失败', 'error');
    }
}

// 删除选中的媒体
async function deleteSelectedMedia() {
    if (selectedMediaIds.size === 0) {
        showToast('请先选择要删除的媒体', 'error');
        return;
    }
    
    if (!confirm(`确定要删除选中的 ${selectedMediaIds.size} 个媒体吗？`)) {
        return;
    }
    
    try {
        for (const id of selectedMediaIds) {
            await utils.request(`/media/${id}`, { method: 'DELETE' });
        }
        selectedMediaIds.clear();
        loadMediaList();
        showToast('删除成功');
    } catch (error) {
        showToast('删除失败', 'error');
    }
}

// 筛选媒体
function filterMedia() {
    const type = document.getElementById('mediaTypeFilter').value;
    const tagId = document.getElementById('mediaTagFilter').value;
    const query = document.getElementById('mediaSearch').value;
    
    let filtered = mediaList;
    
    if (type) {
        filtered = filtered.filter(m => m.type === type);
    }
    
    if (tagId) {
        // 需要根据标签过滤，这里简化处理
        filtered = filtered.filter(m => {
            const tagElement = document.getElementById(`tags-${m.id}`);
            return tagElement && tagElement.innerHTML.includes(`data-tag-id="${tagId}"`);
        });
    }
    
    if (query) {
        const lowerQuery = query.toLowerCase();
        filtered = filtered.filter(m => m.name.toLowerCase().includes(lowerQuery));
    }
    
    // 重新渲染
    const grid = document.getElementById('mediaGrid');
    grid.innerHTML = filtered.map(media => `
        <div class="media-item ${selectedMediaIds.has(media.id) ? 'selected' : ''}" 
             onclick="selectMedia(${media.id})" 
             ondblclick="editMedia(${media.id})">
            <div class="media-thumbnail">
                ${media.thumbnail ? 
                    `<img src="${media.thumbnail}" alt="${media.name}" style="width:100%;height:100%;object-fit:cover;">` :
                    utils.getMediaTypeIcon(media.type)
                }
            </div>
            <div class="media-info">
                <div class="media-name" title="${media.name}">${media.name}</div>
                <div class="media-meta">
                    ${utils.getMediaTypeLabel(media.type)} | ${utils.formatFileSize(media.fileSize)}
                </div>
            </div>
        </div>
    `).join('');
}

// 加载标签筛选器
async function loadTagsForFilter() {
    try {
        const response = await utils.request('/tags');
        if (response.success) {
            const tags = response.data || [];
            const select = document.getElementById('mediaTagFilter');
            select.innerHTML = '<option value="">全部标签</option>' + 
                tags.map(tag => `<option value="${tag.id}">${tag.name}</option>`).join('');
        }
    } catch (error) {
        console.error('加载标签失败:', error);
    }
}

// 显示上传模态框
function showUploadModal() {
    const uploadArea = document.getElementById('uploadArea');
    uploadArea.style.display = uploadArea.style.display === 'none' ? 'block' : 'none';
}

// 显示添加网络媒体模态框
function showAddMediaModal() {
    showModal(`
        <h3>添加网络媒体</h3>
        <div class="form-group">
            <label>媒体名称</label>
            <input type="text" id="networkMediaName" placeholder="请输入媒体名称">
        </div>
        <div class="form-group">
            <label>媒体URL</label>
            <input type="url" id="networkMediaUrl" placeholder="https://example.com/video.mp4">
        </div>
        <div class="form-group">
            <label>媒体类型</label>
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

// 添加网络媒体
async function addNetworkMedia() {
    const name = document.getElementById('networkMediaName').value;
    const url = document.getElementById('networkMediaUrl').value;
    const type = document.getElementById('networkMediaType').value;
    
    if (!name || !url) {
        showToast('请填写完整信息', 'error');
        return;
    }
    
    try {
        await utils.request('/media', {
            method: 'POST',
            body: JSON.stringify({
                name,
                path: url,
                type,
                source: 'NETWORK'
            })
        });
        closeModal();
        loadMediaList();
        showToast('添加成功');
    } catch (error) {
        showToast('添加失败', 'error');
    }
}
