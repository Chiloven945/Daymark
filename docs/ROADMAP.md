# Daymark — M0 → M5 实施路线图

> 状态： **M0 → M5 已实现**（2026-11-08）。用户已确认：minSdk 31、Material3 1.5.0-rc01、多模块、Hilt、
> `top.chiloven.daymark`、四个底部 Tab。
>
> 实际构建使用 Gradle 9.8.1（9.7.0 下载超时，已切换到本机缓存的 9.8.1；AGP 9.3.3 + Kotlin 2.4.21
> 构建通过）。首轮实现使用逐模块 build 文件；`build-logic` convention plugins 留作后续重构。

---

## 0. 目标

先完成一个纯 M3、可运行、可增删改查的 Daymark：

- 4 个顶层页面：Home / Events / Calendar / Settings
- Event Detail / Event Editor
- Room + DataStore
- Hilt + MVVM + StateFlow
- M3 Expressive 主题
- 日期引擎与重复规则
- 提醒与通知
- 数据导出 / 导入

Widget 与 Nothing 主题不在 M0–M5 的实现范围内，但架构必须预留接口。

---

## 1. 版本与兼容性决策

### 1.1 推荐组合

| 组件                    | 版本                                                                         |
|-------------------------|------------------------------------------------------------------------------|
| AGP                     | **9.3.3**                                                                    |
| Gradle                  | **9.8.1**（wrapper；9.7.0 下载超时，使用本机缓存 9.8.1；AGP 9.3 最低 9.5.0） |
| JDK                     | 21（Gradle 运行），target Java 17                                            |
| Kotlin / KGP            | **2.4.21**                                                                   |
| Compose Compiler Plugin | 2.4.21                                                                       |
| KSP                     | 2.3.13                                                                       |
| Hilt                    | 2.60.1                                                                       |
| compileSdk / targetSdk  | 37                                                                           |
| minSdk                  | 31                                                                           |
| Compose BOM             | 2026.09.00                                                                   |
| Material3               | **1.5.0-rc01**（显式 pin，BOM 不能把它带上去）                               |
| Navigation Compose      | 2.10.2                                                                       |
| Lifecycle               | 2.11.0                                                                       |
| Activity Compose        | 1.13.0                                                                       |
| Core KTX                | 1.19.1                                                                       |
| Room                    | 2.8.5                                                                        |
| DataStore Preferences   | 1.3.0                                                                        |
| kotlinx.serialization   | 1.12.0                                                                       |
| kotlinx-coroutines      | 1.11.0                                                                       |
| Turbine                 | 1.2.1                                                                        |
| Truth                   | 1.4.5                                                                        |
| Hilt Navigation Compose | 1.4.0                                                                        |

### 1.2 为什么不用 AGP 9.5.1 + 内置 Kotlin 2.2.10

AGP 9.5.1 的 POM 默认带 KGP 2.2.10，而 Kotlin 2.4.21 的官方测试范围是 AGP 8.5.2–9.3.1、Gradle
7.6.3–9.7.0。

- AGP 9.3 已经支持 API 37。
- AGP 9.3.3 + Gradle 9.7.0 + Kotlin 2.4.21 是更保守的组合。
- 仍使用 AGP 9 的 built-in Kotlin：不要应用 `org.jetbrains.kotlin.android`。
- 通过根 `buildscript` 将 KGP 提升到 2.4.21，并让 Compose Compiler 插件版本匹配 2.4.21。
- M0 必须先做一次空项目 smoke build；如果 AGP 9.3.3 + KGP 2.4.21 有冲突，回退 AGP 9.5.1 + 内置 KGP
  2.2.10，并同步降 Compose compiler。

### 1.3 AGP 9 关键规则

- 不应用 `org.jetbrains.kotlin.android`。
- 使用 AGP built-in Kotlin。
- `compileSdk` 使用新 DSL：
  ```kotlin
  compileSdk { version = release(37) }
  ```
- `android.kotlinOptions {}` 改用：
  ```kotlin
  kotlin {
      compilerOptions {
          jvmTarget.set(JvmTarget.JVM_17)
      }
  }
  ```
- kapt 不可用；全部走 KSP。
- Hilt ≥ 2.59.2；KSP ≥ 2.3.6。
- `kotlin.compilerOptions.jvmTarget` 默认跟随 `android.compileOptions.targetCompatibility`，但仍显式设置
  Java 17。

---

## 2. 模块结构

```text
Daymark/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build-logic/
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── convention/
│       └── src/main/kotlin/top/chiloven/daymark/buildlogic/
├── app/
├── core/
│   ├── model/
│   ├── data/
│   └── designsystem/
├── feature/
│   ├── home/
│   ├── events/
│   ├── calendar/
│   ├── editor/
│   └── settings/
└── widget/
```

### 2.1 依赖方向

```text
app
 ├── feature:home
 ├── feature:events
 ├── feature:calendar
 ├── feature:editor
 ├── feature:settings
 ├── core:data
 ├── core:designsystem
 └── widget

feature:*  → core:model + core:data + core:designsystem
core:data  → core:model
core:model → 无 Android UI 依赖
widget     → core:model + core:data
```

### 2.2 包名

- applicationId：`top.chiloven.daymark`
- root package：`top.chiloven.daymark`
- `core:model`：`top.chiloven.daymark.core.model`
- `core:data`：`top.chiloven.daymark.core.data`
- `core:designsystem`：`top.chiloven.daymark.core.designsystem`
- feature：`top.chiloven.daymark.feature.home` 等

### 2.3 为什么 `core:model` 也用 Android Library

AGP 9 的 built-in Kotlin 只适用于 Android 模块。为了避免单独引入 `org.jetbrains.kotlin.jvm` 的版本组合，
`core:model` 先做成 Android library；它本身不依赖 Android UI，测试仍可在 JVM 上跑。

---

## 3. build-logic 约定插件

> 首轮实现为了降低 AGP 9 + Kotlin 2.4.21 的构建风险，暂未创建 `build-logic`，各模块直接使用
> `build.gradle.kts`。下面是后续重构方案。

```text
build-logic/convention/src/main/kotlin/
├── AndroidApplicationConventionPlugin.kt
├── AndroidLibraryConventionPlugin.kt
├── AndroidComposeConventionPlugin.kt
├── AndroidHiltConventionPlugin.kt
├── AndroidFeatureConventionPlugin.kt
└── DaymarkVersions.kt (可选)
```

职责：

| 插件                          | 作用                                                  |
|-------------------------------|-------------------------------------------------------|
| `daymark.android.application` | applicationId、versionCode、targetSdk、签名、打包     |
| `daymark.android.library`     | namespace、compileSdk、minSdk、Java 17、buildFeatures |
| `daymark.android.compose`     | compose compiler、Compose BOM、UI/test 依赖           |
| `daymark.android.hilt`        | Hilt 插件、KSP、hilt-android、hilt-compiler           |
| `daymark.android.feature`     | 组合 library + compose + hilt + core 依赖             |

根 `build.gradle.kts` 只声明插件 `apply false`，具体配置进入 convention plugin，避免每个模块重复。

---

## 4. 领域模型与日期引擎（M1）

### 4.1 模型

```kotlin
package top.chiloven.daymark.core.model

data class Event(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val direction: CountDirection,
    val recurrence: RecurrenceRule,
    val categoryId: Long?,
    val note: String,
    val isPinned: Boolean,
    val reminder: Reminder?
)

enum class CountDirection {
    COUNT_DOWN,
    COUNT_UP,
    AUTO
}

enum class RecurrenceFrequency {
    NONE,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val interval: Int = 1,
    val endDate: LocalDate? = null
)

data class Reminder(
    val offsetDays: Int,
    val timeMinutes: Int,
    val enabled: Boolean
)

data class EventOccurrence(
    val event: Event,
    val targetDate: LocalDate,
    val days: Long,
    val phase: EventPhase,
    val anniversaryYear: Int?
)

enum class EventPhase {
    UPCOMING,
    TODAY,
    PASSED
}
```

### 4.2 DateCalculator

```kotlin
class DateCalculator(
    private val timeProvider: TimeProvider
) {
    fun occurrence(event: Event): EventOccurrence
    fun nextOccurrence(event: Event, from: LocalDate): LocalDate
    fun daysBetween(from: LocalDate, to: LocalDate): Long
    fun anniversaryYear(event: Event, occurrenceDate: LocalDate): Int?
}
```

规则：

- `Event.date` 是 **日历日期**，不是时间戳。
- 使用 `LocalDate` + `ChronoUnit.DAYS`。
- 今天返回 `TODAY`，而不是 `0 DAYS`。
- 过期且 `COUNT_UP` 或 `AUTO` 时显示已过去天数。
- `AUTO`：未到日期时倒计时，过了日期后正向计时。
- 重复：
    - WEEKLY：每 N 周。
    - MONTHLY：每 N 月；月末按目标月最后一天处理。
    - YEARLY：每年；2 月 29 日策略显式化（默认 2 月 28 日，未来可配置）。
    - `endDate` 之后不再产生 occurrence。
- 全部使用注入的 `ZoneId` / `TimeProvider`，不要直接 `LocalDate.now()`。

### 4.3 测试

`core:model/src/test`：

- 今天 / 明天 / 昨天
- 跨年
- 2 月 29 日
- 月末重复
- `AUTO` 方向切换
- 周年数
- 时区变化（固定 Clock + ZoneId）
- endDate 截止

---

## 5. 数据层（M2）

### 5.1 Room

```text
events
- id INTEGER PK
- title TEXT
- date_epoch_day INTEGER
- direction TEXT
- recurrence_frequency TEXT
- recurrence_interval INTEGER
- recurrence_end_epoch_day INTEGER NULL
- category_id INTEGER NULL
- note TEXT
- is_pinned INTEGER
- created_at INTEGER
- updated_at INTEGER

categories
- id INTEGER PK
- name TEXT
- color_argb INTEGER
- icon_key TEXT
- sort_order INTEGER

reminders
- id INTEGER PK
- event_id INTEGER
- offset_days INTEGER
- time_minutes INTEGER
- enabled INTEGER
```

关系：

- `EventWithCategory`：`@Embedded EventEntity` + `@Relation CategoryEntity`
- `EventWithReminder`：MVP 一个事件一个提醒，也可用 relation 支持多提醒。

DAO：

```kotlin
@Query("SELECT * FROM events ORDER BY date_epoch_day ASC")
fun observeAll(): Flow<List<EventWithCategory>>

@Query("SELECT * FROM events WHERE id = :id")
fun observeById(id: Long): Flow<EventWithCategory?>

@Query("SELECT * FROM events WHERE title LIKE '%' || :query || '%'")
fun search(query: String): Flow<List<EventWithCategory>>

@Upsert
suspend fun upsert(event: EventEntity)
@Delete
suspend fun delete(event: EventEntity)
```

### 5.2 DataStore

```kotlin
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val defaultWidgetStyle: WidgetStyle = WidgetStyle.LUCENT,
    val notificationsEnabled: Boolean = false
)
```

MVP 存 `themeMode`、`dynamicColor`、`firstDayOfWeek`、`notificationsEnabled`。

### 5.3 Repository

```kotlin
interface EventRepository {
    fun observeAll(): Flow<List<Event>>
    fun observeById(id: Long): Flow<Event?>
    fun search(query: String): Flow<List<Event>>
    suspend fun upsert(event: Event): Long
    suspend fun delete(id: Long)
    suspend fun setPinned(id: Long, pinned: Boolean)
}

interface CategoryRepository { ... }
interface SettingsRepository { ... }
```

Repository 负责 Entity ↔ Domain 映射；日期 occurrence 由 `DateCalculator` 计算，不在 Repository
里写日期逻辑。

### 5.4 预留接口

```kotlin
interface ReminderScheduler {
    suspend fun schedule(event: Event)
    suspend fun cancel(eventId: Long)
    suspend fun rescheduleAll()
}

interface WidgetRefreshScheduler {
    fun refreshAll()
    fun refresh(eventId: Long)
}
```

M2 先提供 `NoOpReminderScheduler` / `NoOpWidgetRefreshScheduler`，M5 替换为真实实现。

### 5.5 测试

- In-memory Room：插入、查询、更新、删除、搜索。
- `Flow` 测试用 Turbine。
- DataStore 测试用临时文件 + `PreferenceDataStoreFactory`。

---

## 6. 设计系统（M3）

### 6.1 主题

```kotlin
@Composable
fun DaymarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = DaymarkTypography,
        shapes = DaymarkShapes,
        content = content
    )
}
```

### 6.2 Tokens

`DaymarkShapes`：

- extraSmall 4dp
- small 8dp
- medium 12dp
- large 20dp
- extraLarge 28dp

`DaymarkSpacing`：4 / 8 / 12 / 16 / 24 / 32。

`DaymarkTypography`：

- 默认 M3 Typography。
- `displayLarge` 用于倒计时数字，`fontFeatureSettings = "tnum"`。
- 数字字重 500–600。

### 6.3 组件

`core:designsystem`：

- `CountdownDisplay`
- `EventCard`
- `EventRow`
- `CategoryDot`
- `DaysBadge`
- `SectionHeader`
- `EmptyState`
- `LoadingState`
- `DaymarkTopAppBar`

约束：

- Feature 不直接使用十六进制颜色。
- 所有页面使用 `MaterialTheme.colorScheme` / `MaterialTheme.typography` / `MaterialTheme.shapes`。
- 未来 Nothing 主题只替换 `DaymarkTheme`，不改页面。

---

## 7. App Shell 与导航（M3）

### 7.1 顶层路由

```kotlin
@Serializable
data object HomeRoute
@Serializable
data object EventsRoute
@Serializable
data object CalendarRoute
@Serializable
data object SettingsRoute
@Serializable
data class EventDetailRoute(val eventId: Long)
@Serializable
data class EventEditorRoute(val eventId: Long? = null)
```

### 7.2 导航结构

```text
MainActivity
└── DaymarkApp
    └── NavHost(startDestination = HomeRoute)
        ├── HomeRoute
        ├── EventsRoute
        ├── CalendarRoute
        ├── SettingsRoute
        ├── EventDetailRoute
        └── EventEditorRoute
```

- 顶层 4 个页面显示 `NavigationBar`。
- Detail / Editor 隐藏底部栏。
- `NavigationBar` 4 个 item：Home / Events / Calendar / Settings。
- 使用 `WindowSizeClass`：宽屏时换成 `NavigationRail`。
- 使用 `enableEdgeToEdge()` + `WindowInsets`。
- 支持 predictive back。

### 7.3 App 级 Scaffold

```kotlin
@Composable
fun DaymarkApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = backStackEntry?.destination?.isTopLevel == true

    Scaffold(
        bottomBar = { if (showBottomBar) DaymarkNavigationBar(navController) }
    ) { padding ->
        DaymarkNavHost(navController, Modifier.padding(padding))
    }
}
```

---

## 8. 页面实现（M3–M5）

### 8.1 Home

文件：

```text
feature/home/
├── HomeScreen.kt
├── HomeViewModel.kt
├── HomeUiState.kt
└── components/
    ├── HeroCountdownCard.kt
    ├── TodaySection.kt
    └── UpcomingSection.kt
```

UiState：

```kotlin
data class HomeUiState(
    val nextEvent: EventOccurrence? = null,
    val todayEvents: List<EventOccurrence> = emptyList(),
    val upcoming: List<EventOccurrence> = emptyList(),
    val isLoading: Boolean = true
)
```

布局：

- `Scaffold` + `LazyColumn`
- 顶部日期 + 设置入口
- Hero Countdown Card
- Today 区块（无内容则隐藏）
- Upcoming 区块（3–5 条）
- `ExtendedFloatingActionButton` 新建
- 空状态
- 点击 Hero / Row → Detail

### 8.2 Events

文件：

```text
feature/events/
├── EventsScreen.kt
├── EventsViewModel.kt
├── EventsUiState.kt
└── components/
    ├── EventSearchBar.kt
    ├── EventFilterChips.kt
    └── EventListRow.kt
```

UiState：

```kotlin
data class EventsUiState(
    val query: String = "",
    val filter: EventFilter = EventFilter.ALL,
    val groups: List<EventGroup> = emptyList(),
    val isLoading: Boolean = true
)
```

功能：

- 搜索标题
- 筛选 All / Countdown / Count Up / Anniversaries
- 分组 Pinned / 本月 / 以后 / 已过去
- 左滑删除 + Undo
- 排序菜单
- 点击进入 Detail

### 8.3 Calendar

文件：

```text
feature/calendar/
├── CalendarScreen.kt
├── CalendarViewModel.kt
├── CalendarUiState.kt
└── components/
    ├── MonthHeader.kt
    ├── MonthGrid.kt
    └── DayEventsList.kt
```

UiState：

```kotlin
data class CalendarUiState(
    val yearMonth: YearMonth,
    val selectedDate: LocalDate,
    val eventsByDay: Map<LocalDate, List<EventOccurrence>>,
    val selectedDayEvents: List<EventOccurrence>
)
```

功能：

- 7 列月历
- 今天描边、选中填充
- 每天最多 3 个事件点
- 下方选中日期事件列表
- 月份切换 + Today
- 点击日期过滤下方列表
- 点击事件进入 Detail

### 8.4 Event Detail

文件：

```text
feature/editor/
├── detail/
│   ├── EventDetailScreen.kt
│   ├── EventDetailViewModel.kt
│   └── EventDetailUiState.kt
└── editor/
    ├── EventEditorScreen.kt
    ├── EventEditorViewModel.kt
    └── EventEditorUiState.kt
```

Detail：

- Hero Countdown
- 日期 / 重复 / 分类 / 提醒 / 备注
- 编辑 / 删除
- 删除确认对话框
- 空/错误状态

### 8.5 Event Editor

表单：

1. 预览卡
2. 标题（必填）
3. 日期（M3 DatePickerDialog）
4. 方向（SegmentedButton：Countdown / Count Up / Auto）
5. 重复（BottomSheet：Never / Weekly / Monthly / Yearly）
6. 分类（Chips）
7. 提醒（Switch + 提前天数 + TimePicker）
8. 备注
9. 删除（编辑模式）

ViewModel：

```kotlin
data class EventEditorUiState(
    val draft: EventDraft = EventDraft(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<Field, String> = emptyMap()
)
```

- `SavedStateHandle` 读取 `eventId`
- 保存成功后 `navController.popBackStack()`
- 通过 Snackbar 报错
- 未保存返回时提示（可选）

### 8.6 Settings

文件：

```text
feature/settings/
├── SettingsScreen.kt
├── SettingsViewModel.kt
└── SettingsUiState.kt
```

分组：

- Appearance：System / Light / Dark；Dynamic color
- Widgets：默认样式（Lucent / Frosted / Solid / Material），打开 Widget 选择器（M6）
- Calendar：一周起始日
- Notifications：总开关、权限状态
- Data：导出 JSON / 导入 JSON / 清空
- About：版本、开源许可

组件：`ListItem`、`Switch`、`SegmentedButton`、`AlertDialog`。

---

## 9. 状态管理与 Hilt

### 9.1 ViewModel 规范

- `@HiltViewModel`
- 构造函数注入 Repository / DateCalculator / TimeProvider
- `StateFlow<UiState>`
- `SharingStarted.WhileSubscribed(5_000)`
- 一次性事件用 `Channel<UiEvent>` 或 `MutableSharedFlow`
- `SavedStateHandle` 解析路由参数
- UI 用 `collectAsStateWithLifecycle()`

### 9.2 Hilt 模块

```text
app/di/
├── DatabaseModule.kt
├── RepositoryModule.kt
├── DispatcherModule.kt
├── TimeModule.kt
└── SchedulerModule.kt
```

- `@Singleton` 提供 Room、DataStore、Repository。
- `TimeProvider` 绑定 `SystemTimeProvider`。
- `ReminderScheduler` 先绑定 `NoOpReminderScheduler`。
- `WidgetRefreshScheduler` 先绑定 `NoOpWidgetRefreshScheduler`。

### 9.3 日期刷新

- 前台：`TimeProvider.todayFlow()` 在跨午夜时发射。
- 后台：AlarmManager / 系统日期广播（M5）。
- Widget：独立 `WidgetRefreshScheduler`（M6）。

---

## 10. 提醒与通知（M5）

### 10.1 权限

- `POST_NOTIFICATIONS`（Android 13+）
- `SCHEDULE_EXACT_ALARM`（需要精确提醒时）
- 用户拒绝时降级为非精确提醒

### 10.2 组件

```text
core/data/reminder/
├── ReminderScheduler.kt
├── AlarmReminderScheduler.kt
├── ReminderReceiver.kt
├── BootReceiver.kt
├── TimeChangeReceiver.kt
└── NotificationHelper.kt
```

流程：

```text
Event 保存
  → ReminderScheduler.schedule(event)
  → 计算下一次提醒时间
  → AlarmManager.setExactAndAllowWhileIdle / setAndAllowWhileIdle
  → ReminderReceiver
  → NotificationHelper.show(event)
```

### 10.3 边界

- 设备重启重新注册
- 时区变化重新计算
- 系统时间变化重新计算
- 事件修改/删除取消旧闹钟
- 精确闹钟权限撤销时降级
- 通知频道按分类拆分

### 10.4 测试

- 提醒时间计算（纯 JVM）
- Scheduler 的 PendingIntent 参数
- Notification channel 创建
- 权限拒绝降级

---

## 11. 导出 / 导入（M5）

- `kotlinx.serialization` JSON
- `ActivityResultContracts.CreateDocument` / `OpenDocument`
- 格式：

```json
{
  "version": 1,
  "exportedAt": "2026-11-08T00:00:00Z",
  "categories": [],
  "events": []
}
```

- 导入时校验 version、ID 冲突、日期格式。
- 导入失败显示 Snackbar，不破坏现有数据（先解析到内存，再事务写入）。

---

## 12. 测试与质量门禁

### 12.1 单元测试

- `core:model`：DateCalculator / Recurrence
- `core:data`：DAO / Repository / DataStore
- feature ViewModels：Turbine + coroutines-test
- ReminderScheduler 计算

### 12.2 Compose UI 测试

- Home 空状态
- Events 搜索与筛选
- Editor 校验
- Settings 主题切换

### 12.3 每个里程碑的验收

| 里程碑 | 验收                                                        |
|--------|-------------------------------------------------------------|
| M0     | `./gradlew :app:assembleDebug`；4 Tab 可切换；深色/浅色正常 |
| M1     | 日期测试全绿                                                |
| M2     | Room/DataStore/Repository 测试全绿                          |
| M3     | Home/Events 可读真实数据；空状态正常                        |
| M4     | 新增/编辑/删除/详情/日历闭环                                |
| M5     | 设置持久化；提醒可触发；导出/导入往返成功                   |

---

## 13. 风险与回退

| 风险                               | 应对                                                  |
|------------------------------------|-------------------------------------------------------|
| AGP 9.3.3 + Kotlin 2.4.21 组合问题 | M0 smoke build；失败则 AGP 9.5.1 + 内置 Kotlin 2.2.10 |
| material3 1.5.0-rc01 API 变化      | 主题/组件集中在 designsystem；可回退 1.4.0 基础 M3    |
| Hilt + KSP + AGP 9                 | Hilt 2.60.1、KSP 2.3.13；M0 验证                      |
| 类型安全导航                       | 先用简单 `@Serializable` 路由，跑通后再扩展           |
| 重复/闰年边界                      | 纯 JVM 测试先行                                       |
| 精确闹钟权限                       | 非精确降级                                            |
| Widget 集成                        | 通过 `WidgetRefreshScheduler` 接口隔离；M6 再接       |
| 大数据量                           | MVP 全量列表；后续引入 Paging                         |

---

## 14. M0 执行清单

1. 创建 Gradle wrapper 9.7.0。
2. 创建 `settings.gradle.kts`、根 `build.gradle.kts`、`gradle.properties`。
3. 创建 `build-logic` + convention plugins。
4. 创建 `gradle/libs.versions.toml`。
5. 创建 10 个模块骨架。
6. `app`：
    - `DaymarkApplication` (`@HiltAndroidApp`)
    - `MainActivity` (`@AndroidEntryPoint`, `enableEdgeToEdge`)
    - `DaymarkApp` + `DaymarkNavHost`
    - 4 个占位页面
    - `DaymarkTheme`
7. `core:model`：空模型 + 一个 DateCalculator 占位测试。
8. `core:data`：空 Room/DataStore 骨架 + Hilt module。
9. `core:designsystem`：Theme + 一个 `EmptyState`。
10. 运行：
    ```bash
    ./gradlew :app:assembleDebug
    ./gradlew test
    ```
11. 安装到 Nothing 设备，确认 4 个 Tab 和主题。

完成后进入 M1。

---

## 14.1 当前实现状态

- [x] M0 Gradle 脚手架、10 个模块、Hilt、M3 Expressive 主题、4 Tab 自适应导航
- [x] M1 Event / EventOccurrence / Recurrence / DateCalculator + 9 个日期测试
- [x] M2 Room + DataStore + Repository + 导出服务
- [x] M3 Home + Events（真实数据、搜索、筛选、分组、删除、置顶）
- [x] M4 Event Detail + Event Editor + Calendar
- [x] M5 Settings + AlarmManager 提醒 + 通知 + 导出/导入
- [ ] M6 Widget
- [ ] M7 Nothing 主题

### 已验证

```bash
./gradlew test :app:assembleDebug
```

结果：构建成功；`DateCalculatorTest` 9 项通过，`EventExportServiceTest` 2 项通过。

### 当前已知限制

- Widget 仍是空模块，`WidgetRefreshScheduler` 为 no-op（M6）。
- `build-logic` convention plugins 未实现，模块 build 文件暂时重复。
- Event Detail 的通知点击只打开主界面，尚未深链到具体事件。
- 日历/事件页的“今天”在跨午夜后会等到下次状态刷新，Home 有 60 秒 ticker。
- 设备/模拟器当前未连接，尚未完成真机启动与截图验证。

---

## 15. 未来里程碑

- **M6**：Widget（RemoteViews、四套布局、逐实例配置、Lucent/Frosted/Solid）
- **M7**：Nothing 主题（Lucent Tokens、`com.nothing.dynamiccolor` 读取、字体可选）
- **M8**：备份/同步、农历、桌面快捷方式、AppFunctions（可选）

---

## 16. 文档产出

执行阶段应创建：

- `docs/ROADMAP.md`：本文件
- `docs/ARCHITECTURE.md`：模块图、依赖规则、数据流
- `docs/DESIGN_SYSTEM.md`：M3 tokens、组件规范、Nothing 替换点
- `docs/adr/0001-modularization.md`
- `docs/adr/0002-m3-expressive.md`
- `docs/adr/0003-data-model-and-dates.md`

---

## 17. 待用户确认（执行前最后确认）

1. AGP 9.3.3 + Gradle 9.7.0 + Kotlin 2.4.21 是否接受？  
   备选：AGP 9.5.1 + 内置 Kotlin 2.2.10。
2. 是否使用 `material3-adaptive-navigation-suite`（BOM 已包含）做自适应导航？  
   备选：手写 `NavigationBar` / `NavigationRail`。
3. 是否在 M5 就接入真实 `AlarmManager` 提醒，还是先做 `WorkManager` 近似提醒？
4. 是否现在就规划中文/英文双语资源？默认建议：`values/` 英文，`values-zh-rCN/` 中文。
