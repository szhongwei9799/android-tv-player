# 安卓TV多媒体展示系统

一个功能强大的安卓TV多媒体展示应用，支持视频、图片、PPT、PDF等多种媒体格式播放，具备标签管理、定时任务、转场特效、附加音频等功能。

## 功能特性

### 媒体支持
- **视频**: MP4, MKV, AVI, FLV, TS, MOV, WEBM
- **音频**: MP3, WAV, AAC, FLAC, OGG, M4A
- **图片**: JPG, JPEG, PNG, GIF, BMP, WEBP, SVG
- **文档**: PPT, PPTX, PDF
- **流媒体**: M3U8, RTMP, HTTP-FLV

### 核心功能
- 全屏播放，支持遥控器控制
- 标签管理系统
- 播放列表管理（手动/基于标签）
- 定时任务调度
- 转场特效（12种效果）
- 附加音频功能
- 内置Web后台管理系统

### 遥控器支持
- OK键: 播放/暂停
- 左/右: 上一个/下一个 或 快退/快进
- 上/下: 音量调节
- 返回: 退出播放
- 菜单: 显示控制面板

## 安装

### 从源码构建

1. 克隆项目
```bash
git clone https://github.com/yourusername/android-tv-player.git
cd android-tv-player
```

2. 使用Android Studio打开项目

3. 构建并安装到TV设备

## 使用方法

### 首次启动

1. 启动应用后会显示二维码
2. 使用手机扫描二维码打开Web后台
3. 在Web后台上传媒体文件
4. 创建播放列表
5. 开始播放

### Web后台访问

- 地址: `http://<TV设备IP>:8080`
- 二维码: 首页显示

### 后台功能

- **媒体库**: 上传、管理媒体文件
- **标签**: 创建和管理标签
- **播放列表**: 创建和管理播放列表
- **定时任务**: 设置定时播放任务
- **设置**: 配置显示间隔、转场效果等

## 技术栈

- **TV应用**: Kotlin + Jetpack Compose for TV
- **视频播放**: Media3 (ExoPlayer)
- **Web服务**: NanoHTTPD
- **数据库**: Room
- **定时任务**: WorkManager
- **网页后台**: HTML/CSS/JavaScript

## 项目结构

```
android-tv-player/
├── app/
│   ├── src/main/
│   │   ├── java/com/multimediaplayer/
│   │   │   ├── MainActivity.kt
│   │   │   ├── PlayerActivity.kt
│   │   │   ├── ui/
│   │   │   ├── player/
│   │   │   ├── server/
│   │   │   ├── scheduler/
│   │   │   ├── data/
│   │   │   └── utils/
│   │   ├── assets/web/
│   │   └── res/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 权限

- 网络访问
- 存储读写
- 前台服务
- 开机启动
- 精确闹钟

## 开发计划

- [x] 基础框架
- [x] 播放器模块
- [x] 转场效果
- [x] 附加音频功能
- [x] Web服务
- [x] 网页后台
- [x] 定时任务
- [ ] 测试优化

## 许可证

MIT License
