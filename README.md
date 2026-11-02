# 考试倒计时（ExamTimeDown）

桌面端倒计时挂件应用 —— 让每一次考试/重要时刻都"看得见剩余"。

基于 **Kotlin Multiplatform + Compose Desktop**，单一代码库产出 macOS / Windows / Linux 桌面应用。

## 功能

- 倒计时管理：新增 / 编辑 / 删除（带二次确认），标题、备注、目标时间、颜色、提前提醒量
- 桌面挂件：无边框 / 透明 / 置顶 / 可拖动 / 位置记忆，紧凑与详细两种样式
- 实时剩余：每秒心跳驱动，天/时/分/秒分解（绝不递减累加），状态判定（未到当天/就在今天/已结束）
- 到期提醒：跨越检测（提前 N 天/小时/分钟 + 到期本身），蜂鸣 + 系统托盘通知，已触发去重
- 系统托盘：显示管理窗口、锁定/解锁挂件、开机自启、退出；双击托盘图标唤起管理窗口
- 设置窗口：挂件样式、透明度、显示秒、最多条目数、置顶、锁定、主题、开机自启
- 持久化：JSON 原子写入（临时文件 + 重命名 + .bak 备份），防崩溃写坏数据
- 跨时区安全：存储墙上时间（LocalDateTime + ZoneId），出差/改时区不会让倒计时错乱

## 技术栈

| 维度 | 选型 |
|------|------|
| 语言 | Kotlin 2.x |
| UI | Compose Multiplatform 1.9.3 + Material 3 |
| 平台 | KMP JVM（桌面） |
| 协程 | kotlinx.coroutines（含 Swing 调度器） |
| 序列化 | kotlinx.serialization.json |
| 时间 | kotlinx-datetime 0.7.x + java.time（jvm actual 绕过 OverloadMarker 歧义） |
| 测试 | kotlin.test |

## 项目结构

```
composeApp/
├── src/
│   ├── commonMain/kotlin/com/examtimedown/app/
│   │   ├── model/            # 数据模型（CountdownItem/AppSettings/CountdownSnapshot）
│   │   ├── core/time/        # 时间计算/格式化/提醒扫描/ID 生成
│   │   ├── data/             # JSON 编解码 + Repository + JsonStorage(expect)
│   │   ├── ui/
│   │   │   ├── theme/        # Material3 主题 + 颜色映射
│   │   │   ├── manager/      # 管理窗口（列表 + 编辑对话框 + 日期时间选择器）
│   │   │   ├── widget/       # 挂件窗口（卡片 + 屏幕）
│   │   │   └── settings/     # 设置窗口
│   │   └── AppState.kt       # 应用共享状态容器
│   ├── jvmMain/kotlin/com/examtimedown/app/
│   │   ├── platform/         # JsonStorage(actual)/AppDirs/提醒通知/调度器/托盘/自启
│   │   ├── data/             # JsonStorage actual
│   │   └── main.kt           # 应用入口（双窗口 + 托盘 + 调度器）
│   └── commonTest/           # 单元测试（计算/格式/扫描/序列化）
└── build.gradle.kts          # KMP + Compose Desktop + 打包配置
```

## 开发

### 环境要求
- JDK 21（mac 系统 `/Library/Java` 仅 11，需用 corretto 21）
- Gradle 通过 foojay-resolver 自动下载工具链

### 常用命令

```bash
# 编译
./gradlew :composeApp:compileKotlinJvm

# 运行测试（KMP jvm target 任务名是 jvmTest）
./gradlew :composeApp:jvmTest

# 开发运行（mac）
./gradlew :composeApp:run

# 打包当前系统分发（mac 产出 dmg）
./gradlew :composeApp:packageDistributionForCurrentOS

# 打包 Windows MSI/EXE（需在 Windows 或 CI 执行）
./gradlew :composeApp:packageMsi :composeApp:packageExe
```

> mac 上启动 Gradle 用 corretto 21：
> `JAVA_HOME=~/.gradle/jdks/amazon_com_inc_-21-aarch64-os_x.2/amazon-corretto-21.jdk/Contents/Home ./gradlew ...`

## 数据存储位置

| 平台 | 路径 |
|------|------|
| macOS | `~/Library/Application Support/ExamTimeDown/` |
| Windows | `%APPDATA%\ExamTimeDown\` |
| Linux | `~/.examtimedown/` |

文件：`countdowns.json`（倒计时列表）、`settings.json`（应用设置）。

## 设计要点

### 为什么存墙上时间而非 epoch
存 `Instant`：用户改时区/出差后倒计时会变成另一时刻（错误）。存 `LocalDateTime + ZoneId`：永远对应当地墙上时刻，跨时区夏令时由 ZoneId 规则自动正确处理。

### 为什么用 expect/actual 做 LocalDateTime→epoch
kotlinx-datetime 0.7.x 处于 `Instant` 迁移到 `kotlin.time` 的过渡期，`LocalDateTime.toInstant(TimeZone)` 存在 OverloadMarker 歧义，编译期选 deprecated 重载会运行时 `NoSuchMethodError`。改用 expect/actual，jvm 用 `java.time` 精准实现 DST。

### 剩余时间为什么每秒重算而非递减累加
递减累加会因调度抖动累积误差，长时间运行后与真实时刻漂移。每秒用当前时刻重算快照，保证永远准确。

### 提醒去重
每条提醒有唯一 key `"${itemId}@$offset"`，触发后写入 `item.firedReminderKeys` 持久化，重启后不会重复触发。

### 原子写入
JSON 写入先写 `.tmp` 临时文件，再重命名为目标文件，并保留 `.bak` 备份；读取时目标文件不存在则回退 `.bak`，防崩溃/断电写坏数据。

## 变更历史

见 [CHANGELOG.md](./CHANGELOG.md)。
