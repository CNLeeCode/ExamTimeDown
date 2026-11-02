# 变更日志 (CHANGELOG)

本项目所有功能开发与代码变更均记录于此，按时间倒序排列。

---

## [1.0.0-阶段6] - 2026-11-02

### 新增 - 设置窗口（ui/settings）
- `SettingsScreen`：编辑 AppSettings 并实时写回仓库，含挂件样式/透明度/显示秒/最多条目数/置顶/锁定/主题/开机自启
- `AutoStartExpect`：开机自启 common↔jvm 平台桥接（expect/actual），jvm actual 委托 AutoStartManager

### 变更 - 管理窗口
- `ManagerScreen`：TopAppBar 增加"设置"图标入口，点击打开独立设置窗口（onOpenSettings 回调）

### 变更 - 入口
- `main.kt`：新增设置窗口（480x640），从管理窗口入口打开，关闭即返回

### 新增 - 文档
- `README.md`：功能、技术栈、项目结构、开发命令、数据存储位置、设计要点

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/settings/SettingsScreen.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/settings/AutoStartExpect.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/ui/settings/AutoStartExpect.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/manager/ManagerScreen.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/main.kt`
- `README.md`

## [1.0.0-阶段5] - 2026-11-02

### 新增 - 到期提醒（core/time + jvmMain/platform）
- `ReminderScanner`：纯函数跨越检测扫描器，提前提醒(offset>0)与到期提醒(offset=0)分离，用 firedReminderKeys 去重防重复触发
- `ReminderScannerTest`：6 个单测覆盖提前跨越/到期跨越/已触发去重/archived 跳过/markFired 合并
- `ReminderNotifier`：jvm 通知器，Toolkit.beep 蜂鸣 + TrayIcon 系统通知，到期/即将到期文案区分
- `ReminderScheduler`：订阅 AppState.now 心跳，扫描→触发通知→合并写回 firedReminderKeys 持久化

### 新增 - 系统托盘 + 开机自启（jvmMain/platform）
- `TrayController`：系统托盘图标 + 弹出菜单（显示管理窗口/锁定挂件切换/开机自启切换/退出），双击托盘显示管理窗口
- `AutoStartManager`：三平台开机自启（macOS LaunchAgents plist / Linux autostart desktop / Windows 注册表 Run 键）

### 变更 - 入口
- `main.kt`：接入 ReminderScheduler、TrayController；管理窗口关闭改为隐藏（托盘"退出"才真正退出）

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/core/time/ReminderScanner.kt`
- `composeApp/src/commonTest/kotlin/com/examtimedown/app/core/time/ReminderScannerTest.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/platform/ReminderNotifier.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/platform/ReminderScheduler.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/platform/TrayController.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/platform/AutoStartManager.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/main.kt`

## [1.0.0-阶段4] - 2026-11-02

### 新增 - 桌面挂件窗口（ui/widget）
- `WidgetCard`：单条挂件卡片，紧凑模式（标题+大字天数）/ 详细模式（天时分秒四格+备注），已到期统一"已结束"
- `WidgetScreen`：挂件窗口内容，过滤 visibleOnWidget 项按到期升序取前 N 条，透明背景纵向排列
- 双窗口并存：管理窗口（普通）+ 挂件窗口（无边框/透明/置顶/可拖动）
- AWT 级鼠标拖动实现（透明窗口下不被 Compose 手势拦截）
- 位置记忆：启动读 settings.windowX/Y，窗口销毁写回；锁定时禁用拖动

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/widget/WidgetCard.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/widget/WidgetScreen.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/main.kt`

## [1.0.0-阶段3] - 2026-11-02

### 新增 - 管理主窗口 UI（ui/manager + ui/theme）
- `AppState`：应用共享状态容器，集中持有仓库/心跳/状态流，构造即自动启动秒心跳
- `AppTheme`：Material3 深浅色主题包装
- `CountdownColors`：colorKey → Compose Color 映射，统一挂件与列表取色
- `IdGenerator`：倒计时项 ID 生成（epoch 毫秒 + 随机串）
- `DateTimePicker`：年月日 + 时分数字输入选择器（秒默认 00，实时校验合法性回调）
- `CountdownEditorDialog`：新增/编辑对话框（标题≤50、备注≤200、目标时间必须晚于当前、颜色选择、提醒提前量、挂件可见开关）
- `ManagerScreen`：管理主窗口（列表按到期升序、每条展示标题/备注/目标时间/剩余/状态、编辑/删除带二次确认、空状态引导、新建 FAB）

### 变更 - 构建配置
- commonMain 追加 `compose.materialIconsExtended` 依赖

### 变更 - 入口
- `main.kt` 替换占位心跳演示，改为启动 AppState + ManagerScreen（窗口 900x620）

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/AppState.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/theme/AppTheme.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/theme/CountdownColors.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/core/IdGenerator.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/manager/DateTimePicker.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/manager/CountdownEditorDialog.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/ui/manager/ManagerScreen.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/main.kt`
- `composeApp/build.gradle.kts`

## [1.0.0-阶段2] - 2026-11-02

### 新增 - 数据持久化层（data）
- `JsonStorage`：expect/actual class，common 定义读写契约，jvm 用文件系统实现
- 原子写入策略：临时文件 .tmp + 重命名，并保留 .bak 备份，防崩溃/断电写坏 JSON
- 读取回退：目标文件不存在时尝试 .bak 备份
- `JsonCodec`：JSON 编解码器（prettyPrint + ignoreUnknownKeys + encodeDefaults）
- `CountdownRepository`：倒计时项仓库，StateFlow 暴露列表，增删改自动持久化
- `SettingsRepository`：设置仓库，StateFlow 暴露设置

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/data/JsonStorage.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/data/JsonStorage.kt`
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/platform/AppDirs.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/data/JsonCodec.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/data/CountdownRepository.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/data/SettingsRepository.kt`

### 新增 - 序列化往返测试
- `JsonCodecTest`：CountdownItem 列表与 AppSettings 的 JSON 编解码往返一致

涉及文件：
- `composeApp/src/commonTest/kotlin/com/examtimedown/app/data/JsonCodecTest.kt`

### 变更 - 构建配置
- compilerOptions 追加 `-Xexpect-actual-classes` 抑制 expect/actual class Beta 警告

## [1.0.0-阶段1] - 2026-11-02

### 新增 - 项目骨架
- 初始化 Gradle 多模块项目结构，KMP 目标仅 `jvm()`，`jvmToolchain(21)`
- 配置腾讯云 Gradle 镜像源、foojay toolchain 自动下载、Compose Multiplatform 1.9.3
- 版本目录 `gradle/libs.versions.toml` 集中管理依赖
- 复制 gradle-wrapper(jar/gradlew) 至项目，锁定 Gradle 8.14.3
  
涉及文件：
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties` + `gradle-wrapper.jar` + `gradlew`/`gradlew.bat`
- `composeApp/build.gradle.kts`

### 新增 - 数据模型层（model）
- `CountdownItem`：单个倒计时项持久化模型，存储墙上时间（LocalDateTime + zoneId）
- `AppSettings`：应用设置（挂件样式/透明度/置顶/锁定/穿透/主题等）+ WidgetStyle/ThemeMode 枚举
- `CountdownSnapshot`：倒计时实时快照（运行时计算值）+ CountdownStatus 枚举

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/model/CountdownItem.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/model/AppSettings.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/model/CountdownSnapshot.kt`

### 新增 - 倒计时核心算法（core/time）
- `CountdownCalculator`：纯函数计算器，每帧重算剩余时长，绝不递减累加
- `SecondTicker`：全应用共享秒级心跳源，秒边界对齐，单调时钟调度 + 墙钟取值
- `DurationFormatter`：时长格式化器（详细/紧凑/到期文案）

涉及文件：
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/core/time/CountdownCalculator.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/core/time/SecondTicker.kt`
- `composeApp/src/commonMain/kotlin/com/examtimedown/app/core/time/DurationFormatter.kt`

### 新增 - 单元测试
- `CountdownCalculatorTest`：覆盖未来/过期/当天/跨年/闰年/月末/精确秒/时区/快照拆分
- `DurationFormatterTest`：覆盖补零/详细/紧凑/到期文案

涉及文件：
- `composeApp/src/commonTest/kotlin/com/examtimedown/app/core/time/CountdownCalculatorTest.kt`
- `composeApp/src/commonTest/kotlin/com/examtimedown/app/core/time/DurationFormatterTest.kt`

### 新增 - 桌面端入口
- `main.kt`：最小可运行窗口，实时显示当前时刻以验证心跳跳动

涉及文件：
- `composeApp/src/jvmMain/kotlin/com/examtimedown/app/main.kt`

### 新增 - 文档
- `README.md`：项目说明、运行方式、目录结构、技术栈
- `docs/开发计划.md`：完整实施方案与阶段划分