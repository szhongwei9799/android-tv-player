let netState = { protocol: 'smb', host: '', port: 21, username: '', password: '', path: '', url: '', baseUrl: '' };

function switchProtocol() {
    netState.protocol = document.getElementById('netProtocol').value;
}

function showConnectModal() {
    const p = netState.protocol;
    let fields = '';
    if (p === 'smb') {
        fields = `<div class="field"><label>共享路径</label><input type="text" id="netPath" placeholder="//server/share" value="${netState.path}"></div>
            <div class="field"><label>用户名（可选）</label><input type="text" id="netUser" value="${netState.username}"></div>
            <div class="field"><label>密码（可选）</label><input type="password" id="netPass" value="${netState.password}"></div>`;
    } else if (p === 'ftp') {
        fields = `<div class="field"><label>主机</label><input type="text" id="netHost" value="${netState.host}" placeholder="192.168.1.100"></div>
            <div class="field"><label>端口</label><input type="number" id="netPort" value="${netState.port||21}" min="1" max="65535"></div>
            <div class="field"><label>路径</label><input type="text" id="netPath" value="${netState.path||'/'}" placeholder="/video"></div>
            <div class="field"><label>用户名</label><input type="text" id="netUser" value="${netState.username}" placeholder="anonymous"></div>
            <div class="field"><label>密码</label><input type="password" id="netPass" value="${netState.password}" placeholder="anonymous@"></div>`;
    } else if (p === 'webdav') {
        fields = `<div class="field"><label>服务器URL</label><input type="url" id="netUrl" value="${netState.url}" placeholder="https://webdav.example.com/"></div>
            <div class="field"><label>用户名</label><input type="text" id="netUser" value="${netState.username}"></div>
            <div class="field"><label>密码</label><input type="password" id="netPass" value="${netState.password}"></div>`;
    }
    showModal('连接 ' + document.getElementById('netProtocol').selectedOptions[0].text, fields +
        `<div style="margin-top:10px;"><button class="btn btn-primary" onclick="connectServer()" style="width:100%;">浏览</button></div>`);
}

async function connectServer() {
    const p = netState.protocol;
    netState.username = document.getElementById('netUser')?.value || '';
    netState.password = document.getElementById('netPass')?.value || '';

    let url;
    if (p === 'smb') {
        netState.path = document.getElementById('netPath').value;
        const cleanPath = netState.path.replace(/^smb:\/\//, '').replace(/\/$/, '');
        const creds = netState.username ? `${netState.username}:${netState.password}@` : '';
        url = `/api/network/smb?path=smb://${creds}${cleanPath}/`;
    } else if (p === 'ftp') {
        netState.host = document.getElementById('netHost').value;
        netState.port = parseInt(document.getElementById('netPort').value) || 21;
        netState.path = document.getElementById('netPath').value || '/';
        url = `/api/network/ftp?host=${encodeURIComponent(netState.host)}&port=${netState.port}&path=${encodeURIComponent(netState.path)}&username=${encodeURIComponent(netState.username)}&password=${encodeURIComponent(netState.password)}`;
    } else {
        netState.url = document.getElementById('netUrl').value.replace(/\/$/, '');
        url = `/api/network/webdav?url=${encodeURIComponent(netState.url)}&username=${encodeURIComponent(netState.username)}&password=${encodeURIComponent(netState.password)}`;
    }

    closeModal();
    const el = document.getElementById('netBrowser');
    el.innerHTML = '<div class="loading">正在连接...</div>';
    try {
        const r = await utils.request(url);
        netState.baseUrl = url;
        renderNetBrowser(r.data || []);
    } catch (e) {
        el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>连接失败: ${escHtml(e.message)}</p>
            <button class="btn btn-outline" onclick="showConnectModal()" style="margin-top:10px;">重试</button></div>`;
    }
}

async function navigateTo(subpath) {
    const p = netState.protocol;
    let url;
    if (p === 'smb') {
        const cleanPath = subpath.replace(/^smb:\/\//, '').replace(/\/?$/, '/');
        const creds = netState.username ? `${netState.username}:${netState.password}@` : '';
        url = `/api/network/smb?path=smb://${creds}${cleanPath}`;
    } else if (p === 'ftp') {
        netState.path = subpath;
        url = `/api/network/ftp?host=${encodeURIComponent(netState.host)}&port=${netState.port}&path=${encodeURIComponent(subpath)}&username=${encodeURIComponent(netState.username)}&password=${encodeURIComponent(netState.password)}`;
    } else {
        url = `/api/network/webdav?url=${encodeURIComponent(subpath)}&username=${encodeURIComponent(netState.username)}&password=${encodeURIComponent(netState.password)}`;
    }

    const el = document.getElementById('netBrowser');
    el.innerHTML = '<div class="loading">加载中...</div>';
    try {
        const r = await utils.request(url);
        netState.baseUrl = url;
        renderNetBrowser(r.data || []);
    } catch (e) {
        el.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>加载失败: ${escHtml(e.message)}</p></div>`;
    }
}

function renderNetBrowser(files) {
    const el = document.getElementById('netBrowser');
    if (!files.length) {
        el.innerHTML = '<div class="empty-state"><div class="empty-icon">📁</div><p>目录为空</p></div>';
        return;
    }
    el.innerHTML = `<div class="net-toolbar">${getBreadcrumb()}</div>
        <div class="net-list">${files.map(f => renderNetItem(f)).join('')}</div>`;
}

function getBreadcrumb() {
    let path = '';
    if (netState.protocol === 'smb') path = netState.path || '';
    else if (netState.protocol === 'ftp') path = netState.path || '/';
    else path = netState.url || '';
    const parts = path.replace(/\/$/, '').split('/').filter(Boolean);
    let html = `<a href="#" onclick="showConnectModal()" style="color:var(--accent);">连接</a>`;
    let cum = '';
    for (const p of parts) {
        cum += '/' + p;
        html += ` <span style="color:var(--muted);margin:0 3px;">/</span> <a href="#" onclick="navigateTo('${cum.replace(/'/g, "\\'")}')">${escHtml(p)}</a>`;
    }
    return html;
}

function renderNetItem(f) {
    const isDir = f.isDirectory;
    const icon = isDir ? '📁' : getFileIcon(f.name);
    const size = isDir ? '-' : utils.formatFileSize(f.length);
    const protoPrefix = netState.protocol === 'smb' ? 'smb://' : netState.protocol === 'ftp' ? `ftp://${netState.host}` : '';
    const fullPath = netState.protocol === 'smb' ? f.path : netState.protocol === 'ftp' ? `${protoPrefix}${f.path}` : f.path;

    return `<div class="net-item" ${isDir ? `onclick="navigateTo('${fullPath.replace(/'/g, "\\'")}')"` : ''}>
        <div class="net-icon">${icon}</div>
        <div class="net-info">
            <div class="net-name">${isDir ? escHtml(f.name) : escHtml(f.name)}</div>
            <div class="net-meta">${isDir ? '目录' : size}</div>
        </div>
        <div class="net-actions">
            ${!isDir ? `<button class="btn btn-primary btn-sm" onclick="importNetFile('${escHtml(fullPath)}', '${escHtml(f.name)}')">导入</button>` : ''}
        </div>
    </div>`;
}

function getFileIcon(name) {
    const ext = name.split('.').pop()?.toLowerCase();
    if (['mp4','avi','mkv','mov','wmv','flv','webm','m3u8','ts'].includes(ext)) return '🎬';
    if (['jpg','jpeg','png','gif','bmp','webp'].includes(ext)) return '🖼️';
    if (['mp3','wav','flac','aac','ogg','wma'].includes(ext)) return '🎵';
    if (['ppt','pptx'].includes(ext)) return '📊';
    if (['pdf'].includes(ext)) return '📄';
    return '📄';
}

async function importNetFile(url, name) {
    try {
        const r = await utils.request('/tags');
        const tags = r.data || [];
        const tagHtml = tags.length ? `<div class="field"><label>标签</label><div class="check-group" id="importTags">${
            tags.map(t => `<label><input type="checkbox" value="${t.id}"><span style="display:inline-block;width:9px;height:9px;border-radius:50%;background:${t.color}"></span>${escHtml(t.name)}</label>`).join('')
        }</div></div>` : '';
        showModal('导入媒体', `<div class="field"><label>名称</label><input type="text" id="importName" value="${escHtml(name)}"></div>
            <div class="field"><label>URL</label><div class="val" style="word-break:break-all;font-size:12px;">${escHtml(url)}</div></div>
            ${tagHtml}
            <button class="btn btn-primary" onclick="doImport('${escHtml(url)}')" style="margin-top:6px;width:100%;">导入</button>`);
    } catch(e) {
        showToast('加载标签失败: ' + e.message, 'error');
    }
}

async function doImport(url) {
    const name = document.getElementById('importName').value.trim();
    if (!name) { showToast('请输入名称', 'error'); return; }
    const tagCheckboxes = document.getElementById('importTags');
    const tagIds = tagCheckboxes ? Array.from(tagCheckboxes.querySelectorAll('input:checked')).map(c => parseInt(c.value)) : [];
    try {
        await utils.request('/network/import', { method: 'POST', body: JSON.stringify({ url, name, tagIds }) });
        closeModal(); showToast('导入成功');
    } catch(e) {
        showToast('导入失败: ' + e.message, 'error');
    }
}
