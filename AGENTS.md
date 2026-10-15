# AGENTS.md — 智能体项目上下文

## 这是什么

Kotlin Multiplatform 跨端「手机宠物」App（宠物名：小咪）。Android 为主战场，桌面/Web/iOS 共用同一套 UI。**所有业务逻辑和 UI 都在 `shared/src/commonMain`**，平台差异通过 `expect/actual` 注入。

## 架构要点

- `pet/GrowthEngine.kt`：全部成长规则都是**纯函数**（步数换算、睡眠分档、天气→心情、离线衰减、升级曲线）。新玩法规则先进这里，并配 `commonTest` 单测。
- `ui/PetViewModel.kt`：唯一的状态聚合者（StateFlow），通过 `AppServices`（commonMain 里的 lazy 单例）装配，不走 DI 框架。
- `data/PetRepository.kt`：multiplatform-settings KV 持久化。**按日计数（喂食/抚摸/聊天 XP）统一用「date + count」双 key 模式**（参照 `feedDate/feedCount`）。
- `data/PlatformServices.kt`：`expect fun createSettings()` / `expect fun createStepDataSource()`，各平台 actual 在同名后缀文件里。
- `chat/LlmChatService.kt`：OpenAI 兼容协议。**降级契约不能破坏**：任何失败（无 Key/断网/空响应）都要返回 `ChatReply(fromLlm = false)`，UI 靠它显示「AI 回复/离线回复」标签。
- `ui/cat/CatPet.kt`：Canvas 程序化绘制的猫，虚拟坐标系 240×240，光源固定左上方。`CatPet(mood, onTap)` 签名即接口——将来换 Lottie/精灵图实现时调用方不动。

## 常用命令（Windows + Git Bash）

```bash
./gradlew :androidApp:assembleDebug       # 打 APK
./gradlew :androidApp:installDebug        # 装到连接的设备/模拟器
./gradlew :shared:jvmTest                 # 跑单测（最快）
./gradlew :shared:testAndroidHostTest     # Android 宿主单测（注意：不叫 testDebugUnitTest）
./gradlew :desktopApp:run                 # 桌面预览（功能完整，无步数）
./gradlew :shared:jvmTest --tests "*CatPreviewRender*"  # 渲染猫咪/聊天页 PNG 到 shared/build/catpreviews/
```

改完代码的最低验证：`:shared:jvmTest` + `:androidApp:assembleDebug` + `:shared:compileKotlinJs :shared:compileKotlinWasmJs`（后两个用来兜住 expect/actual 完整性）。

## 已踩过的坑（不要再踩）

1. **Ktor Android 引擎必须用 CIO**。OkHttp 5.5+ 要求 compileSdk 37，项目 compileSdk 36，换回 OkHttp 会直接构建失败。
2. **kotlinx-datetime 用 0.6.2 的稳定 API**（`kotlinx.datetime.Clock`）；0.8 会迁移到 `kotlin.time.Instant`，升级前先改代码。
3. **不要动 `local.properties`**（IDE 生成的 SDK 路径）；lint 的 `PropertyEscape` 已在 `androidApp/build.gradle.kts` 里禁用，别恢复。
4. 新版 AGP 9 KMP 库插件的 Android 单测任务是 `testAndroidHostTest`，**不是** `testDebugUnitTest`。
5. 模拟器黑屏 ≠ App 崩溃：先开系统设置验证是否全局黑屏，是则 AVD 图形改 Software（或 `emulator -gpu swiftshader_indirect` 启动）。
6. 环境路径：SDK 在 `D:\Android\Sdk`（`adb` 需 `export PATH="/d/Android/Sdk/platform-tools:$PATH"`）；adb 操作安卓路径时加 `export MSYS_NO_PATHCONV=1` 防止 Git Bash 篡改 `/sdcard/...`。

## UI 约定

- 主题：`ui/Theme.kt` 的暖橘色系 `AppTheme`（亮/暗两套）；**不要**退回默认紫色 MaterialTheme。
- 图标：一律用 `material-icons-extended` 的矢量图标，**不要用 emoji 充当功能图标**（emoji 只允许出现在宠物粒子、头像装饰等拟物场景）。
- 弹通知走 `Scaffold` 的 `SnackbarHost`（`PetUiState.notice` → App.kt 统一消费），不要自造提示条。
- 进度条：`strokeCap = Round + drawStopIndicator = {}`（去掉 MD3 末端小圆点）。

## 数据源替换路线（已预留接口）

MVP 的简化实现，升级时只换实现、不动业务层：

| 功能 | 现在 | 将来 |
|---|---|---|
| 步数 | `AndroidStepDataSource`（传感器 + 当日基线） | Health Connect |
| 睡眠 | 手动打卡 | Health Connect 睡眠会话 |
| 使用习惯 | 应用内互动计数 | UsageStatsManager |
| 天气城市 | 设置页手选 | 定位权限 |
