# EndfieldHUD-Android

《明日方舟：终末地》风格的 HUD 工具箱 Android 移植版 —— 让你成为真正的管理员（Enfieldministrator）。

移植自桌面版 [EndfieldHUD](https://github.com/DDDuoDuo/EndfieldHUD)（macOS / Windows），技术栈 **Kotlin + Jetpack Compose**，最低支持 **Android 10 (API 29)**。

## 功能一览

| # | 模块 | 说明 | 对应桌面版 |
|---|------|------|-----------|
| 01 | 便笺 | 文字便笺 + 待办清单，自动持久化 | Notes |
| 02 | 剪贴板 | HUD 运行期间自动捕获复制内容，一键回拷，**退出服务即清空** | Clipboard Cache |
| 03 | 计时 | 倒计时（结束通知 + 震动）/ 秒表计次 | Calendar |
| 04 | 监视 | 电池（电量/充电/温度/电压）、内存、存储、CPU 占用，每秒刷新 | Battery, Storage & Activity Monitor |
| 05 | 个人名片 | 自定义代号/编号/职务/所属的身份卡 | Personal ID |
| 06 | 事件日志 | 管理员风格的事件时间线 | Event Log |
| 07 | 应用快捷方式 | 搜索/收藏/启动已安装应用 | App Shortcuts |
| 08 | 融合小游戏 | 2048 式数字融合，致敬「融合！山团团！」 | Closure's Minigame |
| 09 | 设置 | 面板尺寸 / 气泡透明度 / 震动开关 / 关于 | Settings |

## Android 化的交互设计

桌面版的「菜单栏常驻 + Ctrl+\` 全局热键」在手机上替换为：

- **Foreground Service**：常驻通知承载 HUD，含「切换 HUD / 退出」按钮
- **快捷设置磁贴（TileService）**：下拉快捷设置，一键切换悬浮窗 —— 手机上的“全局热键”
- **SYSTEM_ALERT_WINDOW 悬浮窗**：可拖动的 HUD 面板，收起后变成常驻小气泡
- 面板可获取焦点直接打字（键盘可弹出），气泡模式不抢焦点，不影响游戏

## 构建

```bash
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

要求：JDK 17+，Android SDK 34。首次构建会自动下载依赖。

## 权限说明

| 权限 | 用途 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 显示悬浮 HUD |
| `FOREGROUND_SERVICE(_SPECIAL_USE)` | 常驻服务承载悬浮窗 |
| `POST_NOTIFICATIONS` | 服务常驻通知与计时提醒（Android 13+） |
| `VIBRATE` | 计时结束震动 |

不申请网络、定位、通讯录等任何敏感权限；剪贴板仅在本地缓存、退出即清。

## 许可与致谢

- 代码：MIT License
- 原项目：[DDDuoDuo/EndfieldHUD](https://github.com/DDDuoDuo/EndfieldHUD)
- 本项目为**非官方同人作品**，《明日方舟：终末地》相关素材、名称与品牌归 **鹰角网络** 及其权利人所有
