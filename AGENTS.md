# AGENTS.md — 智能体项目上下文

## 这是什么

Kotlin Multiplatform 跨端「手机宠物」App（宠物名：小咪）。Android 为主战场，桌面/Web/iOS 共用同一套 UI。**所有业务逻辑和 UI 都在 `shared/src/commonMain`**，平台差异通过 `expect/actual` 注入。

## 架构要点

- `pet/GrowthEngine.kt`：全部成长规则都是**纯函数**（步数换算、睡眠分档、天气→心情、离线衰减、升级曲线、金币产出、商店购买校验）。新玩法规则先进这里，并配 `commonTest` 单测。
- `ui/PetViewModel.kt`：唯一的状态聚合者（StateFlow），通过 `AppServices`（commonMain 里的 lazy 单例）装配，不走 DI 框架。
- `data/PetRepository.kt`：multiplatform-settings KV 持久化。**按日计数（喂食/抚摸/聊天 XP/步数金币/聊天金币）统一用「date + count」双 key 模式**（参照 `feedDate/feedCount`）。
- `data/PlatformServices.kt`：`expect fun createSettings()` / `expect fun createStepDataSource()`，各平台 actual 在同名后缀文件里。
- `chat/LlmChatService.kt`：OpenAI 兼容协议。**降级契约不能破坏**：任何失败（无 Key/断网/空响应）都要返回 `ChatReply(fromLlm = false)`，UI 靠它显示「AI 回复/离线回复」标签。
- `ui/cat/CatPet.kt`：精灵图（`composeResources/drawable/cat_*`）+ 程序化呼吸/跳跃/粒子动效。配饰通过 `accessories: Set<String>`（已装备 id）叠加绘制，背景画在猫身后，默认空集合，老调用方不必改。`level` 决定成长阶段，只影响精灵图缩放与毛色矩阵，配饰和阴影跟随同一形变。
- 背景配饰分两层：主页 / 装扮页的舞台用 `AccessoryBackground(id, Modifier.matchParentSize())` 铺满整张 Card（圆角由 Card 裁切），同时给 `CatPet` 传 `drawBackground = false`；只有独立渲染（预览图、单猫组件）才让 `CatPet` 自己画背景。不要让背景只铺在 210dp 精灵图方框里，否则会出现「框中框」。
- `pet/Accessory.kt`：装扮目录与规则（槽位、解锁途径、装备读写、旧存档迁移），全部纯函数，配 `commonTest`。
- `pet/Shop.kt` + `ui/ShopScreen.kt`：商店商品目录与界面。食物购买后立即喂食，玩具/配饰一次性永久解锁；商品 `id` 是持久化键，改名会导致老用户已购记录失效。
- `pet/GrowthStage.kt`：成长阶段规则（幼猫 / 青年猫 / 成年猫）。**阶段由 `PetState.xp` 派生，不单独持久化**；`CatPet(level = pet.level)` 消费。

## 金币与商店

- 产出：每 1000 步 +1（按日结算）、每次喂食 +1、每句聊天 +1（每日上限 5）、完成每日任务 +5~8。
- 消耗：高级猫粮（罐头 +50 / 小鱼干 +70）购买后立即喂食，仍计入每日 3 次喂食上限；毛线球（永久陪玩，每天 +5 XP）；配饰见下节「配饰 / 装扮」。
- 余额存 `PetState.coins`，已购玩具/配饰存 `PetState.ownedItemIds`；按日产出用 `stepCoinDate/stepCoinToday`、`chatCoinDate/chatCoinToday` 双 key，避免重复发放。
- 新商品加进 `ShopCatalog.items`；购买校验统一走 `GrowthEngine.canPurchase / purchase`，不要在 UI 里手算金币。
- 商店里的配饰商品 id 必须与 `AccessoryCatalog` 同名条目一致，购买后由 ViewModel 自动装备到对应槽位。

## 配饰 / 装扮

- `pet/Accessory.kt` 是唯一配饰目录：`AccessorySlot`（HEAD / NECK / BACKGROUND）+ `AccessoryUnlock`（`Shop` / `AchievementReward` / `LevelGift`）。每个槽位最多一件，背景层绘制在猫咪身后。
- 解锁判定统一走 `isAccessoryUnlocked / unlockedAccessoryIds`：商店看 `ownedItemIds`、成就看已解锁成就集合、等级礼包看 `PetState.level`，不要在 UI 里散落判断。
- 装备状态存 `PetState.equippedHead / equippedNeck / equippedBackground`（空串 = 该槽空置）；读写用 `equippedId / withEquipped / equippedAccessoryIds` 扩展，`CatPet(accessories = pet.equippedAccessoryIds)` 直接消费。
- 旧存档迁移：`PetState.accessoriesInitialized = false` 时，`PetRepository.loadPet()` 调 `migrateEquippedAccessories` 把已购商店配饰补装到对应槽位；迁移标记置位后，用户取下的配饰不会被重新装上。
- 新配饰三步：① 在 `AccessoryCatalog` 加条目（id 是持久化键，别改名）；② 商店出售的话在 `ShopCatalog` 加同 id、同价格商品；③ 在 `CatPet.drawAccessory`（帽子/项圈）或 `drawAccessoryBackground`（背景）加绘制分支。改完补 `AccessoryTest` 与 `CatPreviewRender.renderAccessoryShowcase`，跑渲染目检对位。
- 购买配饰后 `PetViewModel.buyShopItem` 自动装备；升级 / 成就解锁的配饰只在 Snackbar 提示，是否穿戴由用户在 `ui/WardrobeScreen.kt` 决定。

## 成长阶段 / 外观进化

- 路线选择：**单宠物外观进化**，不做多宠物并存（TODO 中复杂度更低的一档）。等级阈值：幼猫 Lv.1-4 → 青年猫 Lv.5-9 → 成年猫 Lv.10+。
- `pet/GrowthStage.kt` 是唯一阶段规则：`growthStageForLevel / growthStageForXp`、`PetState.growthStage`、`GrowthStage.next`、`levelsToNextStage`、`stageProgress`、`levelRangeLabel`，全部纯函数，配 `GrowthStageTest`。
- 阶段只由 xp 派生，**不要往 `PetState` 加阶段字段或额外 KV**；改等级曲线时阶段会自然跟着变。
- `CatPet(level = ...)` 按阶段切换：体型缩放（0.82 / 0.92 / 1.0，跨阶段用 spring 动画过渡）、毛色饱和度矩阵、阴影大小。配饰 Canvas 复用同一个 `imageModifier`，不要给阶段单独加配饰坐标偏移。
- 界面：主页名字下方显示「阶段 · 再升 N 级进化为 X」；`StatsScreen` 的 `GrowthStageCard` 展示阶段时间线 + 进度条；`PetViewModel.addXp` 跨越阶段时在升级 Snackbar 里带出「长成了青年猫」。
- 聊天人设：`PetPersona.stage` 会传给 LLM system prompt 和离线回复，语气随阶段变化。
- 新增阶段三步：① 在 `GrowthStage` 枚举加条目（保持 minLevel 递增）；② 在 `CatPet` 的 `bodyScale / furFilter` 加分支；③ 补 `GrowthStageTest` 与 `CatPreviewRender.renderGrowthStages`，跑渲染目检体型和毛色。

## 常用命令（Windows + Git Bash）

```bash
./gradlew :androidApp:assembleDebug       # 打 APK
./gradlew :androidApp:installDebug        # 装到连接的设备/模拟器
./gradlew :shared:jvmTest                 # 跑单测（最快）
./gradlew :shared:testAndroidHostTest     # Android 宿主单测（注意：不叫 testDebugUnitTest）
./gradlew :desktopApp:run                 # 桌面预览（功能完整，无步数）
./gradlew :shared:jvmTest --tests "*CatPreviewRender*"  # 渲染猫咪/配饰/成长阶段/聊天/商店/主页/装扮 PNG 到 shared/build/catpreviews/
```

改完代码的最低验证：`:shared:jvmTest` + `:androidApp:assembleDebug` + `:shared:compileKotlinJs :shared:compileKotlinWasmJs`（后两个用来兜住 expect/actual 完整性）。

## 已踩过的坑（不要再踩）

1. **Ktor Android 引擎必须用 CIO**。OkHttp 5.5+ 要求 compileSdk 37，项目 compileSdk 36，换回 OkHttp 会直接构建失败。
2. **kotlinx-datetime 用 0.6.2 的稳定 API**（`kotlinx.datetime.Clock`）；0.8 会迁移到 `kotlin.time.Instant`，升级前先改代码。
3. **不要动 `local.properties`**（IDE 生成的 SDK 路径）；lint 的 `PropertyEscape` 已在 `androidApp/build.gradle.kts` 里禁用，别恢复。
4. 新版 AGP 9 KMP 库插件的 Android 单测任务是 `testAndroidHostTest`，**不是** `testDebugUnitTest`。
5. 模拟器黑屏 ≠ App 崩溃：先开系统设置验证是否全局黑屏，是则 AVD 图形改 Software（或 `emulator -gpu swiftshader_indirect` 启动）。
6. 环境路径：SDK 在 `D:\Android\Sdk`（`adb` 需 `export PATH="/d/Android/Sdk/platform-tools:$PATH"`）；adb 操作安卓路径时加 `export MSYS_NO_PATHCONV=1` 防止 Git Bash 篡改 `/sdcard/...`。
7. **桌面正常但只有 App 黑屏** = `hw.gpu.mode=auto` 落到了 SwiftShader 软渲染（特征：进程存活、Activity resumed、`dumpsys gfxinfo` 显示已渲染数千帧 Skia(OpenGL) 但画面全黑）。修复：把 `~/.android/avd/<AVD>.avd/config.ini` 的 `hw.gpu.mode` 改成 `host`（已改，Pixel_9_Pro）。诊断时用 `screencap` 截图 + `dumpsys gfxinfo` 判断是"没画"还是"画了没上屏"。

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
