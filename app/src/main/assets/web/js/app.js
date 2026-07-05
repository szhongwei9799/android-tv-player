// API基础URL
const API_BASE = '/api';

// 通用工具函数
const utils = {
    async request(url, options = {}) {
        try {
            const response = await fetch(`${API_BASE}${url}`, {
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                },
                ...options
            });
            return await response.json();
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    },

    formatDate(timestamp) {
        if (!timestamp) return '-';
        const date = new Date(timestamp);
        return date.toLocaleString('zh-CN');
    },

    formatFileSize(bytes) {
        if (!bytes) return '-';
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return `${size.toFixed(1)} ${units[unitIndex]}`;
    },

    formatDuration(ms) {
        if (!ms) return '-';
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        if (hours > 0) {
            return `${hours}:${String(minutes % 60).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
        }
        return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
    },

    getMediaTypeIcon(type) {
        const icons = {
            'VIDEO': '🎬',
            'IMAGE': '🖼️',
            'PPT': '📊',
            'PDF': '📄',
            'AUDIO': '🎵',
            'STREAM': '📹'
        };
        return icons[type] || '📁';
    },

    getMediaTypeLabel(type) {
        const labels = {
            'VIDEO': '视频',
            'IMAGE': '图片',
            'PPT': 'PPT',
            'PDF': 'PDF',
            'AUDIO': '音频',
            'STREAM': '流媒体'
        };
        return labels[type] || type;
    }
};

// 导航切换
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const page = e.target.dataset.page;
        
        // 更新导航状态
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        e.target.classList.add('active');
        
        // 切换页面
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.getElementById(page).classList.add('active');
        
        // 加载页面数据
        loadPageData(page);
    });
});

// 加载页面数据
function loadPageData(page) {
    switch (page) {
        case 'media':
            loadMediaList();
            break;
        case 'tags':
            loadTagList();
            break;
        case 'playlists':
            loadPlaylistList();
            break;
        case 'tasks':
            loadTaskList();
            break;
        case 'settings':
            loadSettings();
            loadSystemInfo();
            break;
    }
}

// 模态框
function showModal(content) {
    document.getElementById('modalBody').innerHTML = content;
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

// 点击模态框外部关闭
document.getElementById('modal').addEventListener('click', (e) => {
    if (e.target === document.getElementById('modal')) {
        closeModal();
    }
});

// Toast通知
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 24px;
        background-color: ${type === 'success' ? '#4caf50' : '#f44336'};
        color: white;
        border-radius: 8px;
        z-index: 2000;
        animation: slideIn 0.3s ease;
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    loadPageData('media');
});
