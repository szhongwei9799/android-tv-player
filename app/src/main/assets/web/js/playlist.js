// 播放列表管理

let playlistList = [];

// 加载播放列表
async function loadPlaylistList() {
    try {
        const response = await utils.request('/playlists');
        if (response.success) {
            playlistList = response.data || [];
            renderPlaylistList();
        }
    } catch (error) {
        console.error('加载播放列表失败:', error);
    }
}

// 渲染播放列表
function renderPlaylistList() {
    const container = document.getElementById('playlistList');
    container.innerHTML = playlistList.map(playlist => `
        <div class="playlist-item">
            <div class="playlist-icon">▶</div>
            <div class="playlist-info">
                <div class="playlist-name">${playlist.name}</div>
                <div class="playlist-meta">
                    ${playlist.type === 'TAG_BASED' ? '基于标签' : '手动列表'} | 
                    ${playlist.mediaCount || 0} 个媒体 | 
                    转场: ${getTransitionLabel(playlist.transitionEffect)}
                </div>
            </div>
            <div class="playlist-actions">
                <button class="btn btn-primary btn-small" onclick="playPlaylist(${playlist.id})">播放</button>
                <button class="btn btn-secondary btn-small" onclick="editPlaylist(${playlist.id})">编辑</button>
                <button class="btn btn-secondary btn-small" onclick="managePlaylistItems(${playlist.id})">管理</button>
                <button class="btn btn-danger btn-small" onclick="deletePlaylist(${playlist.id})">删除</button>
            </div>
        </div>
    `).join('');
}

// 获取转场效果标签
function getTransitionLabel(transition) {
    const labels = {
        'NONE': '无',
        'FADE': '淡入淡出',
        'SLIDE_LEFT': '左滑',
        'SLIDE_RIGHT': '右滑',
        'SLIDE_UP': '上滑',
        'SLIDE_DOWN': '下滑',
        'ZOOM_IN': '放大',
        'ZOOM_OUT': '缩小',
        'WIPE_LEFT': '左擦除',
        'WIPE_RIGHT': '右擦除',
        'DISSOLVE': '溶解',
        'BLUR': '模糊',
        'RANDOM': '随机'
    };
    return labels[transition] || transition;
}

// 显示创建播放列表模态框
function showAddPlaylistModal() {
    showModal(`
        <h3>新建播放列表</h3>
        <div class="form-group">
            <label>播放列表名称</label>
            <input type="text" id="playlistName" placeholder="请输入播放列表名称">
        </div>
        <div class="form-group">
            <label>描述</label>
            <input type="text" id="playlistDesc" placeholder="可选描述">
        </div>
        <div class="form-group">
            <label>类型</label>
            <select id="playlistType" onchange="toggleTagSelection()">
                <option value="MANUAL">手动列表</option>
                <option value="TAG_BASED">基于标签</option>
            </select>
        </div>
        <div id="tagSelection" class="form-group" style="display:none;">
            <label>选择标签</label>
            <div id="tagCheckboxes">加载中...</div>
        </div>
        <div class="form-group">
            <label>转场效果</label>
            <select id="playlistTransition">
                <option value="FADE">淡入淡出</option>
                <option value="SLIDE_LEFT">左滑</option>
                <option value="SLIDE_RIGHT">右滑</option>
                <option value="ZOOM_IN">放大</option>
                <option value="ZOOM_OUT">缩小</option>
                <option value="DISSOLVE">溶解</option>
                <option value="RANDOM">随机</option>
                <option value="NONE">无</option>
            </select>
        </div>
        <div class="form-group">
            <label>显示间隔（秒）</label>
            <input type="number" id="playlistInterval" value="10" min="1" max="60">
        </div>
        <button class="btn btn-primary" onclick="createPlaylist()">创建</button>
    `);
}

// 切换标签选择显示
function toggleTagSelection() {
    const type = document.getElementById('playlistType').value;
    const tagSelection = document.getElementById('tagSelection');
    tagSelection.style.display = type === 'TAG_BASED' ? 'block' : 'none';
    
    if (type === 'TAG_BASED') {
        loadTagsForPlaylist();
    }
}

// 加载标签供播放列表选择
async function loadTagsForPlaylist() {
    try {
        const response = await utils.request('/tags');
        if (response.success) {
            const tags = response.data || [];
            document.getElementById('tagCheckboxes').innerHTML = tags.map(tag => `
                <label style="display:flex;align-items:center;gap:8px;margin:8px 0;cursor:pointer;">
                    <input type="checkbox" value="${tag.id}">
                    <span style="display:inline-block;width:12px;height:12px;border-radius:50%;background-color:${tag.color}"></span>
                    ${tag.name}
                </label>
            `).join('');
        }
    } catch (error) {
        console.error('加载标签失败:', error);
    }
}

// 创建播放列表
async function createPlaylist() {
    const name = document.getElementById('playlistName').value;
    const description = document.getElementById('playlistDesc').value;
    const type = document.getElementById('playlistType').value;
    const transitionEffect = document.getElementById('playlistTransition').value;
    const defaultInterval = parseInt(document.getElementById('playlistInterval').value);
    
    if (!name) {
        showToast('请输入播放列表名称', 'error');
        return;
    }
    
    let tagIds = [];
    if (type === 'TAG_BASED') {
        const checkboxes = document.querySelectorAll('#tagCheckboxes input[type="checkbox"]:checked');
        tagIds = Array.from(checkboxes).map(cb => parseInt(cb.value));
    }
    
    try {
        await utils.request('/playlists', {
            method: 'POST',
            body: JSON.stringify({
                name,
                description,
                type,
                transitionEffect,
                defaultInterval,
                tagIds
            })
        });
        closeModal();
        loadPlaylistList();
        showToast('创建成功');
    } catch (error) {
        showToast('创建失败', 'error');
    }
}

// 编辑播放列表
function editPlaylist(id) {
    const playlist = playlistList.find(p => p.id === id);
    if (!playlist) return;
    
    showModal(`
        <h3>编辑播放列表</h3>
        <div class="form-group">
            <label>播放列表名称</label>
            <input type="text" id="editPlaylistName" value="${playlist.name}">
        </div>
        <div class="form-group">
            <label>描述</label>
            <input type="text" id="editPlaylistDesc" value="${playlist.description || ''}">
        </div>
        <div class="form-group">
            <label>转场效果</label>
            <select id="editPlaylistTransition">
                <option value="FADE" ${playlist.transitionEffect === 'FADE' ? 'selected' : ''}>淡入淡出</option>
                <option value="SLIDE_LEFT" ${playlist.transitionEffect === 'SLIDE_LEFT' ? 'selected' : ''}>左滑</option>
                <option value="SLIDE_RIGHT" ${playlist.transitionEffect === 'SLIDE_RIGHT' ? 'selected' : ''}>右滑</option>
                <option value="ZOOM_IN" ${playlist.transitionEffect === 'ZOOM_IN' ? 'selected' : ''}>放大</option>
                <option value="ZOOM_OUT" ${playlist.transitionEffect === 'ZOOM_OUT' ? 'selected' : ''}>缩小</option>
                <option value="DISSOLVE" ${playlist.transitionEffect === 'DISSOLVE' ? 'selected' : ''}>溶解</option>
                <option value="RANDOM" ${playlist.transitionEffect === 'RANDOM' ? 'selected' : ''}>随机</option>
                <option value="NONE" ${playlist.transitionEffect === 'NONE' ? 'selected' : ''}>无</option>
            </select>
        </div>
        <div class="form-group">
            <label>显示间隔（秒）</label>
            <input type="number" id="editPlaylistInterval" value="${playlist.defaultInterval}" min="1" max="60">
        </div>
        <button class="btn btn-primary" onclick="updatePlaylist(${id})">保存</button>
    `);
}

// 更新播放列表
async function updatePlaylist(id) {
    const name = document.getElementById('editPlaylistName').value;
    const description = document.getElementById('editPlaylistDesc').value;
    const transitionEffect = document.getElementById('editPlaylistTransition').value;
    const defaultInterval = parseInt(document.getElementById('editPlaylistInterval').value);
    
    if (!name) {
        showToast('请输入播放列表名称', 'error');
        return;
    }
    
    try {
        await utils.request(`/playlists/${id}`, {
            method: 'PUT',
            body: JSON.stringify({
                name,
                description,
                transitionEffect,
                defaultInterval
            })
        });
        closeModal();
        loadPlaylistList();
        showToast('更新成功');
    } catch (error) {
        showToast('更新失败', 'error');
    }
}

// 管理播放列表项
async function managePlaylistItems(id) {
    try {
        const [playlistResponse, itemsResponse, mediaResponse] = await Promise.all([
            utils.request(`/playlists/${id}`),
            utils.request(`/playlists/${id}/items`),
            utils.request('/media')
        ]);
        
        if (playlistResponse.success && itemsResponse.success && mediaResponse.success) {
            const playlist = playlistResponse.data;
            const items = itemsResponse.data || [];
            const allMedia = mediaResponse.data || [];
            
            showModal(`
                <h3>管理播放列表: ${playlist.name}</h3>
                <div class="form-group">
                    <label>当前媒体 (${items.length} 项)</label>
                    <div id="playlistItems" style="max-height:300px;overflow-y:auto;">
                        ${items.map((item, index) => `
                            <div style="display:flex;align-items:center;gap:8px;padding:8px;background:#333;margin-bottom:4px;border-radius:4px;">
                                <span style="color:#b0b0b0;">${index + 1}</span>
                                <span style="flex:1;">${item.name}</span>
                                <button class="btn btn-danger btn-small" onclick="removeFromPlaylist(${id}, ${item.id})">移除</button>
                            </div>
                        `).join('')}
                    </div>
                </div>
                <div class="form-group">
                    <label>添加媒体</label>
                    <select id="addMediaSelect">
                        <option value="">选择媒体...</option>
                        ${allMedia.filter(m => !items.find(i => i.id === m.id)).map(m => `
                            <option value="${m.id}">${m.name} (${utils.getMediaTypeLabel(m.type)})</option>
                        `).join('')}
                    </select>
                    <button class="btn btn-secondary btn-small" onclick="addToPlaylist(${id})" style="margin-top:8px;">添加</button>
                </div>
            `);
        }
    } catch (error) {
        showToast('加载失败', 'error');
    }
}

// 添加媒体到播放列表
async function addToPlaylist(playlistId) {
    const mediaId = document.getElementById('addMediaSelect').value;
    if (!mediaId) {
        showToast('请选择媒体', 'error');
        return;
    }
    
    try {
        await utils.request(`/playlists/${playlistId}/items`, {
            method: 'POST',
            body: JSON.stringify({ mediaId: parseInt(mediaId) })
        });
        managePlaylistItems(playlistId);
        loadPlaylistList();
        showToast('添加成功');
    } catch (error) {
        showToast('添加失败', 'error');
    }
}

// 从播放列表移除媒体
async function removeFromPlaylist(playlistId, mediaId) {
    try {
        await utils.request(`/playlists/${playlistId}/items/${mediaId}`, {
            method: 'DELETE'
        });
        managePlaylistItems(playlistId);
        loadPlaylistList();
        showToast('移除成功');
    } catch (error) {
        showToast('移除失败', 'error');
    }
}

// 播放播放列表
function playPlaylist(id) {
    // 这里可以发送命令给TV端开始播放
    showToast('发送播放命令...');
    // 实际实现需要与TV端通信
}

// 删除播放列表
async function deletePlaylist(id) {
    if (!confirm('确定要删除这个播放列表吗？媒体文件不会被删除。')) {
        return;
    }
    
    try {
        await utils.request(`/playlists/${id}`, { method: 'DELETE' });
        loadPlaylistList();
        showToast('删除成功');
    } catch (error) {
        showToast('删除失败', 'error');
    }
}
