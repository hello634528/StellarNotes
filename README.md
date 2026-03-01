# 🌌 NebulaNotes（星澜笔记）

高审美 3D 深蓝星空笔记应用（Android, Kotlin + Compose）。

## 特色
- 笔记以“星体”方式分布在 3D 星空中
- 200⭐ 背景星星闪烁 + 星云辉光 + 流星动画
- **越早创建越靠近中心**，**置顶笔记最近**
- 星标笔记呈现**金色高亮** + 十字光芒
- 搜索支持关键词 + 模糊匹配 + 权重排序
- 点击搜索结果可触发**丝滑运镜**定位到对应笔记
- 接入陀螺仪 + 加速度计，轻微晃动带来空间漂移感
- 每颗星旁边显示笔记标题
- 自定义应用图标（深蓝星空 + 金色星星）
- 首次启动介绍页

## 构建
```bash
gradle assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## CI/CD
GitHub Actions 自动构建 Debug APK，可在 Actions Artifacts 下载。
