# 小咪 · 手机宠物（Kotlin Multiplatform）

一只住在手机里的桌面宠物猫。它会根据你的使用习惯成长、随天气变化心情、按运动量升级、听你的睡眠打卡，还能陪你聊天。

## 功能

- **会动的代码绘宠物**：纯 Compose Canvas 程序化绘制的伪 3D 卡通猫（渐变体积光 + 地面软阴影 + 果冻感点击动画），待机呼吸 / 随机眨眼 / 摇尾巴，点击跳起冒爱心
- **运动升级**：读取手机计步传感器，当日步数 ÷ 400 = 经验值（走 8000 步 → +20 XP），每 100 XP 升一级
- **天气联动心情**：Open-Meteo 免费天气 API（无需 Key），晴天开心、下雨慵懒、下雪兴奋、雷暴低落
- **睡眠打卡**：手动记录昨晚睡眠时长，≥7h +15 XP 并恢复精力；<6h 猫咪会变困倦（闭眼 + 冒 💤）
- **喂食与抚摸**：喂食恢复饱食度（每日 3 次），点击抚摸 +1 XP（每日 5 次）
- **聊天**：接入 OpenAI 兼容 LLM（默认 Kimi `https://api.moonshot.cn/v1`，可在设置页改），回复会结合宠物当前心情/天气/步数；未配置 Key 或断网时降级为离线预设回复，每条回复下方有「AI 回复 / 离线回复」来源标签
- **离线衰减**：饱食度每小时 -2、精力每小时 -1，长时间不管它会饿

## 技术栈

| 层 | 技术 |
|---|---|
| 跨端框架 | Kotlin Multiplatform 2.4.0 + Compose Multiplatform 1.11.1（UI 全共享） |
| UI | Material 3 (1.11.0-alpha07) + material-icons-extended 1.7.3，自定义暖色主题 |
| 网络 | Ktor Client 3.6.0（Android/JVM 用 CIO 引擎、JS/Wasm 用 JS 引擎、iOS 用 Darwin） |
| 序列化 | kotlinx-serialization-json 1.11.0 |
| 持久化 | multiplatform-settings 1.3.0（KV 存储，Android=SharedPreferences） |
| 时间 | kotlinx-datetime 0.6.2 |
| 异步 | kotlinx-coroutines 1.11.0 + lifecycle-viewmodel-compose 2.11.0-beta01 |
| 构建 | Gradle 9.1.0 + AGP 9.0.1（新版 `kotlin.multiplatform.android` 库插件） |

## 项目结构

```
├── shared/                  # 全部业务逻辑与 UI（四端共享）
│   └── src/commonMain/kotlin/org/example/project/
│       ├── pet/             # 领域层：PetState、GrowthEngine（纯函数规则，可单测）
│       ├── data/            # 数据源接口与仓库：PetRepository、天气、步数（expect/actual）
│       ├── chat/            # LlmChatService（LLM + 离线降级）、ChatMessage
│       ├── ui/              # PetViewModel + 三个页面 + AppTheme
│       │   └── cat/CatPet.kt  # 代码绘制的动画猫（虚拟坐标系 240×240）
│       ├── App.kt           # 三 Tab 脚手架（宠物 / 聊天 / 我的）
│       └── AppServices.kt   # 简易服务装配单例
├── androidApp/              # Android 入口（MainActivity + 权限）
├── desktopApp/              # 桌面（JVM）入口
├── webApp/                  # Web（JS + Wasm）入口
└── iosApp/                  # iOS 入口（Xcode 工程）
```

## 启动方式

```bash
# Android（连接设备或模拟器）
./gradlew :androidApp:installDebug

# 桌面（JVM）
./gradlew :desktopApp:run

# Web - Wasm 目标（现代浏览器，更快）
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web - JS 目标（兼容旧浏览器）
./gradlew :webApp:jsBrowserDevelopmentRun

# iOS：用 Xcode 打开 iosApp/ 目录运行（需要 macOS）
```

Android 端首次启动会请求「身体活动」权限用于计步；在「我的」页可配置 LLM 的 Base URL / API Key / 模型和天气城市。

## 打包部署

```bash
# Android APK（debug 签名）；release 包未配置签名，需自行配 signingConfig
./gradlew :androidApp:assembleRelease

# 桌面原生安装包（按当前系统产出 Msi/Dmg/Deb）
./gradlew :desktopApp:packageDistributionForCurrentOS

# Web 静态产物（输出到 webApp/build/dist）
./gradlew :webApp:wasmJsBrowserDistribution
```

## 测试

```bash
./gradlew :shared:jvmTest              # 桌面端单测（含 GrowthEngine 12 个规则用例）
./gradlew :shared:testAndroidHostTest  # Android 宿主单测
./gradlew :shared:jsTest :shared:wasmJsTest  # Web 端
```

另有开发期视觉快照工具：改动猫咪造型后跑
`./gradlew :shared:jvmTest --tests "*CatPreviewRender*"`，
会在 `shared/build/catpreviews/` 输出六种心情 + 聊天来源标签的渲染 PNG，无需开模拟器即可检查。

## 注意事项

- **LLM Key 只存本地**：在 App 内「我的」页填写，存于设备 SharedPreferences；不要硬编码进代码或提交 Key。
- **计步需真机**：模拟器没有计步传感器（界面会显示「此设备暂不支持计步」）；真机需授予 ACTIVITY_RECOGNITION 权限。步数传感器返回的是开机累计值，App 用「当日零点基线」差值计算，重启手机后自动校正。
- **模拟器黑屏**：部分模拟器 GPU 渲染异常会导致全局黑屏（系统应用也黑），把 AVD 的 Emulated Performance → Graphics 改成 Software 即可。
- **iOS 需 macOS**：Windows/Linux 上 iOS target 自动跳过（`gradle.properties` 里已设 `kotlin.native.ignoreDisabledTargets=true`）。
- **Ktor Android 引擎固定用 CIO**：不要换回 OkHttp——OkHttp 5.5+ 要求 compileSdk 37，与本项目 compileSdk 36 冲突。
- **lint**：`:androidApp:build` 会跑 lint；`PropertyEscape` 检查已禁用（它误报 Android Studio 自己生成的 `local.properties`）。
- **天气数据**：Open-Meteo 免费 API 无需 Key；城市目前在设置页手选（内置六个城市坐标），尚未做定位。
