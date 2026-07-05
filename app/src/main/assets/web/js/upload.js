// 文件上传

const uploadManager = {
    queue: [],
    uploading: false,
    maxConcurrent: 3,
    
    init() {
        this.setupDropzone();
        this.setupFileInput();
    },
    
    setupDropzone() {
        const dropzone = document.getElementById('dropzone');
        if (!dropzone) return;
        
        dropzone.addEventListener('click', () => {
            document.getElementById('fileInput').click();
        });
        
        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });
        
        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('dragover');
        });
        
        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            this.addFiles(e.dataTransfer.files);
        });
    },
    
    setupFileInput() {
        const fileInput = document.getElementById('fileInput');
        if (!fileInput) return;
        
        fileInput.addEventListener('change', (e) => {
            this.addFiles(e.target.files);
            fileInput.value = '';
        });
    },
    
    addFiles(files) {
        for (const file of files) {
            this.queue.push({
                file,
                status: 'pending',
                progress: 0
            });
        }
        this.renderProgress();
        this.processQueue();
    },
    
    async processQueue() {
        if (this.uploading) return;
        
        const pendingItems = this.queue.filter(item => item.status === 'pending');
        if (pendingItems.length === 0) return;
        
        this.uploading = true;
        
        const uploadingItems = pendingItems.slice(0, this.maxConcurrent);
        await Promise.all(uploadingItems.map(item => this.uploadFile(item)));
        
        this.uploading = false;
        this.processQueue();
    },
    
    async uploadFile(item) {
        item.status = 'uploading';
        this.renderProgress();
        
        const formData = new FormData();
        formData.append('file', item.file);
        formData.append('filename', item.file.name);
        
        try {
            const xhr = new XMLHttpRequest();
            
            xhr.upload.addEventListener('progress', (e) => {
                if (e.lengthComputable) {
                    item.progress = Math.round((e.loaded / e.total) * 100);
                    this.renderProgress();
                }
            });
            
            await new Promise((resolve, reject) => {
                xhr.onload = () => {
                    if (xhr.status === 200) {
                        item.status = 'success';
                        resolve();
                    } else {
                        item.status = 'error';
                        reject(new Error('Upload failed'));
                    }
                };
                xhr.onerror = () => {
                    item.status = 'error';
                    reject(new Error('Network error'));
                };
                
                xhr.open('POST', '/api/media/upload');
                xhr.send(formData);
            });
        } catch (error) {
            item.status = 'error';
            console.error('上传失败:', error);
        }
        
        this.renderProgress();
        
        // 上传成功后刷新媒体列表
        if (item.status === 'success') {
            loadMediaList();
        }
    },
    
    renderProgress() {
        const container = document.getElementById('uploadProgress');
        if (!container) return;
        
        container.innerHTML = this.queue.map((item, index) => `
            <div class="upload-item">
                <span class="name">${item.file.name}</span>
                <div class="progress">
                    <div class="progress-bar" style="width: ${item.progress}%"></div>
                </div>
                <span class="status">${this.getStatusText(item.status, item.progress)}</span>
            </div>
        `).join('');
    },
    
    getStatusText(status, progress) {
        switch (status) {
            case 'pending':
                return '等待中';
            case 'uploading':
                return `${progress}%`;
            case 'success':
                return '完成';
            case 'error':
                return '失败';
            default:
                return '';
        }
    },
    
    clearCompleted() {
        this.queue = this.queue.filter(item => item.status !== 'success');
        this.renderProgress();
    }
};

// 初始化上传管理器
document.addEventListener('DOMContentLoaded', () => {
    uploadManager.init();
});

// 显示设置
async function loadSettings() {
    try {
        const response = await utils.request('/config');
        if (response.success && response.data) {
            const config = response.data;
            document.getElementById('imageInterval').value = config.imageInterval || 5;
            document.getElementById('pptInterval').value = config.pptInterval || 10;
            document.getElementById('pdfInterval').value = config.pdfInterval || 10;
            document.getElementById('transitionDuration').value = config.transitionDuration || 500;
        }
    } catch (error) {
        console.error('加载设置失败:', error);
    }
}

// 保存设置
async function saveSettings() {
    const config = {
        imageInterval: parseInt(document.getElementById('imageInterval').value),
        pptInterval: parseInt(document.getElementById('pptInterval').value),
        pdfInterval: parseInt(document.getElementById('pdfInterval').value),
        transitionDuration: parseInt(document.getElementById('transitionDuration').value)
    };
    
    try {
        await utils.request('/config', {
            method: 'PUT',
            body: JSON.stringify(config)
        });
        showToast('保存成功');
    } catch (error) {
        showToast('保存失败', 'error');
    }
}

// 系统信息
async function loadSystemInfo() {
    try {
        const response = await utils.request('/system/info');
        if (response.success && response.data) {
            const info = response.data;
            document.getElementById('systemInfo').innerHTML = `
                <div class="info-row">
                    <span class="info-label">设备名称</span>
                    <span>${info.deviceName}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">制造商</span>
                    <span>${info.manufacturer}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">型号</span>
                    <span>${info.model}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Android版本</span>
                    <span>${info.sdkVersion}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">IP地址</span>
                    <span>${info.ipAddress}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">媒体数量</span>
                    <span>${info.mediaCount}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">播放列表数量</span>
                    <span>${info.playlistCount}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">定时任务数量</span>
                    <span>${info.taskCount}</span>
                </div>
            `;
        }
    } catch (error) {
        console.error('加载系统信息失败:', error);
    }
}
