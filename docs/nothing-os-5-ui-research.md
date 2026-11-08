# Nothing OS 5.0 UI / Widget 接入研究

> 研究日期：2026-11-08  
> 目标设备：Nothing Phone (4a) Pro `A069P`  
> 系统：Nothing OS 5.0.6 `C5.0-261021-1625` / Android 17 (SDK 37)  
> 研究目的：为 Daymark（倒计时 / 纪念日 / Count Up / Count Down）确定 Nothing OS 5.0 的第一方级 UI 与
> Widget 接入方式，并把最新 Material 3 作为非 Nothing 设备的 fallback。

---

## 0. 结论摘要

1. **Nothing 没有公开的第三方 Widget SDK。** Nothing 自家的 Widget 使用私有 `com.nothing.card` 框架（
   `android.nothing.action.APPCARD_UPDATE` + `com.nothing.permission.BIND_CARD_SERVICE` 签名权限 +
   自定义 View）。第三方无法注册，也不应尝试伪装。
2. **标准第三方 Widget 有一条真实的“第一方样式通道”：** Nothing Launcher 会按每个 Widget
   所在矩形区域采样壁纸，并通过 Android 公开的 `AppWidgetHostView.setColorResources()` 重写
   `android.R.color.system_neutral1_*`、`system_neutral2_*`、`system_accent1/2/3_*`。只要 Widget 的
   `RemoteViews` 使用这些系统颜色角色，就能自动获得 Nothing 的逐 Widget 壁纸自适应着色。
3. **Lucent 官方色板可读取：** `com.nothing.dynamiccolor` 是一个 shared library，包含
   `container_primary`、`content_on_translucent_primary` 等 Lucent 颜色令牌。Nothing Launcher 自己通过
   `getIdentifier(name, "color", "com.nothing.dynamiccolor")` 读取。Daymark 可以在 Nothing
   设备上做同样的运行时读取，并以内置常量作为 fallback。
4. **实时壁纸模糊不可用于第三方 Widget。** Nothing 的磨砂效果由私有 `FrostedCardWidgetHostView` /
   `FrostedBackdropDrawable` 实现，只服务于 card 框架。第三方 Widget 只能做透明/半透明色层，不能做真实壁纸模糊。
5. **Widget 层建议使用 XML `RemoteViews`，而不是 Jetpack Glance。** Glance 不暴露
   `setLightBackgroundLayoutId()`，对自定义 drawable、letterSpacing、lineHeight、maxLines
   的支持也受限；而这些恰好是复刻 Nothing 静态视觉的关键。
6. **Nothing 已有官方 Countdown Widget（2×2，单事件，液态形状，支持重复/提醒/Count Up）。** Daymark
   的差异化必须放在多事件、Upcoming 列表、Dashboard、周年/Count Up、分类和逐 Widget 配置上。

---

## 1. 设备与样本

| 项目          | 值                                                               |
|---------------|------------------------------------------------------------------|
| 设备          | Nothing Phone (4a) Pro                                           |
| 型号          | `A069P`                                                          |
| 系统          | Nothing OS 5.0.6                                                 |
| Build         | `C5.0-261021-1625`                                               |
| Android       | 17 / SDK 37                                                      |
| 屏幕          | 1260 × 2800，480 dpi                                             |
| 默认 Launcher | `com.nothing.launcher/com.android.searchlauncher.SearchLauncher` |

已拉取并分析的 APK：

| 包名                           | 版本             | 文件                      |
|--------------------------------|------------------|---------------------------|
| `com.nothing.launcher`         | 5.0.6 (50006009) | `NothingLauncher3.apk`    |
| `com.nothing.hearthstone`      | 5.0.0 (5000000)  | `NothingCardLab.apk`      |
| `com.nothing.dynamiccolor`     | 1.0              | `NothingDynamicColor.apk` |
| `com.nothing.aiwidget`         | 1.0.31           | `Aiwidget.apk`            |
| `com.nothing.communitywidgets` | —                | Play 安装的社区 Widget    |

> 提取文件仅用于互操作性研究，未纳入仓库。不要分发 Nothing 的 APK、drawable、字体或其他专有资源。

---

## 2. Nothing 自家 Widget 的私有实现

### 2.1 Manifest 证据

`com.nothing.hearthstone` 的官方 Countdown Widget：

```xml

<provider android:name="com.nothing.hearthstone.countdown.provider.CountdownWidgetProvider"
        android:authorities="com.nothing.hearthstone.authority.APP_CARD.countdown"
        android:exported="true">
    <intent-filter>
        <action android:name="android.nothing.action.APPCARD_UPDATE" />
    </intent-filter>
    <meta-data android:name="com.nothing.card" android:resource="@xml/countdown_card_config" />
</provider>
```

部分 Provider 声明：

```xml
android:permission="com.nothing.permission.BIND_CARD_SERVICE"
```

`BIND_CARD_SERVICE` 是签名级权限。

### 2.2 私有 View 体系

官方 Widget 布局使用：

- `com.nothing.commBase.frosted.stdview.NTFrostedFrameLayout`
- `com.nothing.commBase.frosted.stdview.NTFrostedLinearLayout`
- `com.nothing.commBase.frosted.stdview.NTFrostedRelativeLayout`
- `com.nothing.hearthstone.countdown.views.ShapeProgressView`
- `androidx.viewpager2.widget.ViewPager2`
- `com.nothing.cardwidget.NTPageIndicatorDot`

这些都不是 `RemoteViews` 支持的 View，说明 Nothing Widgets 走的是 Launcher 内的原生 card host：

- `com.nothing.cardhost.CardWidgetHostView`
- `com.nothing.cardhost.FrostedCardWidgetHostView`
- `com.nothing.launcher.card.LauncherCardWidgetHostView`

### 2.3 结论

- 第三方应用无法注册 `com.nothing.card` Provider。
- 不要伪造包名、签名或权限。
- 不要依赖 Nothing 私有 card 框架。Daymark 的 Widget 必须使用标准 AppWidget / RemoteViews。

---

## 3. 第三方 Widget 的官方样式通道

### 3.1 Launcher 的逐 Widget 壁纸采样

`com.nothing.launcher` 中：

```java
public class LauncherAppWidgetHostView
        extends BaseLauncherAppWidgetHostView
        implements LocalColorExtractor.Listener {
```

实际实现：

```java
public final class e
        extends LocalColorExtractor
        implements WallpaperManager.LocalWallpaperColorConsumer {
```

流程：

1. `LauncherAppWidgetHostView.onLayout()` 计算 Widget 在桌面上的矩形。
2. `LocalColorExtractor.setWorkspaceLocation(rect, parent, screenId)` 把矩形换算成壁纸坐标。
3. `WallpaperManager.addOnColorsChangedListener(this, List<RectF>, FLAG_SYSTEM)` 注册逐区域壁纸颜色监听。
4. 壁纸颜色变化时，`nd.a.b(WallpaperColors)` 生成 `SparseIntArray`。
5. `LauncherAppWidgetHostView.onColorsChanged()` 调用：

```java
super.setColorResources(sparseIntArray);
```

### 3.2 被重写的资源 ID

`nd.a` 中硬编码了两组 Android 系统颜色资源：

```java
R.color.system_neutral1_10 ...system_neutral1_1000
R.color.system_neutral2_10 ...system_neutral2_1000

R.color.system_accent1_10 ...system_accent1_1000
R.color.system_accent2_10 ...system_accent2_1000
R.color.system_accent3_10 ...system_accent3_1000
```

这意味着：

> **Widget 布局中直接使用 `@android:color/system_neutral1_*` 和 `@android:color/system_accent*`
，Nothing Launcher 就会按 Widget 所在壁纸区域重写它们。**

这是 Android 公开 API `AppWidgetHostView.setColorResources()` 的标准行为。

### 3.3 亮壁纸适配

`LauncherAppWidgetHostView` 构造函数：

```java
if(Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText)){

setOnLightBackground(true);
}
```

对应标准 Widget API：

```java
RemoteViews.setLightBackgroundLayoutId(R.layout.widget_on_light)
```

所以 Widget 应当：

- 主布局：适合深色壁纸 / 浅色文字；
- `setLightBackgroundLayoutId()`：适合亮色壁纸 / 深色文字。

这样 Launcher 可以在不重新请求 Provider 的情况下切换文字颜色。

### 3.4 Lucent 色板共享库

`pm list libraries`：

```
library:com.nothing.dynamiccolor
```

`com.nothing.dynamiccolor` 的 Manifest：

```xml

<manifest package="com.nothing.dynamiccolor" coreApp="true">
    <application android:hasCode="true">
        <library android:name="com.nothing.dynamiccolor" />
    </application>
</manifest>
```

Nothing Launcher 读取方式（`LucentColorUtils`）：

```java
int id = context.getResources().getIdentifier(
        lucentColorName,
        "color",
        "com.nothing.dynamiccolor"
);
return context.

getResources().

getColor(id, context.getTheme());
```

Daymark 可以：

```xml

<application>
    <uses-library android:name="com.nothing.dynamiccolor" android:required="false" />
</application>
```

然后按名字读取，读不到就 fallback 到内置常量。

### 3.5 逐 Widget 着色何时启用

`com.nothing.launcher.widget.e` 的开关：

```java
this.enabled =qd.a.

a(context);
```

```java
Settings.Secure.getString(
        resolver,
    "theme_customization_overlay_packages"
)
```

解析 JSON：

- 如果存在 `android.theme.customization.color_source`
- 且值不是 `"preset"`

则启用逐 Widget 壁纸颜色采样。

当前设备：

```json
{
  "android.theme.customization.color_source": "preset",
  "android.theme.customization.system_palette": "333333",
  "android.theme.customization.theme_style": "MONOCHROMATIC"
}
```

因此当前是 preset 主题，逐 Widget 壁纸采样关闭。验证时需要把系统颜色来源切换为壁纸，或至少测试两种状态。

### 3.6 实时模糊的边界

Nothing 私有 card host：

- `com.nothing.cardhost.FrostedCardWidgetHostView`
- `com.nothing.commBase.frosted.stdview.FrostedBackdropDrawable`
- `com.nothing.commBase.frosted.stdview.FrostedBackdropHelper`

这些只对 `com.nothing.card` 生效。标准 `LauncherAppWidgetHostView` 没有注入 `FrostedBackdrop`。

结论：

- Widget 内不能实时模糊壁纸。
- 可以做半透明 scrim、静态渐变、形状填充。
- App 内可以使用 `Modifier.blur()` / `RenderEffect` 做真正的磨砂。

---

## 4. 设计令牌

### 4.1 中性色

| Token        | 值        |
|--------------|-----------|
| neutral_0    | `#ffffff` |
| neutral_10   | `#fcfcfc` |
| neutral_50   | `#f2f2f2` |
| neutral_75   | `#ebebeb` |
| neutral_100  | `#e3e3e3` |
| neutral_200  | `#c8c8c8` |
| neutral_300  | `#adadad` |
| neutral_400  | `#929292` |
| neutral_500  | `#777777` |
| neutral_600  | `#606060` |
| neutral_700  | `#484848` |
| neutral_800  | `#323232` |
| neutral_850  | `#292929` |
| neutral_900  | `#1c1c1c` |
| neutral_950  | `#121212` |
| neutral_1000 | `#000000` |

### 4.2 Alpha 层

| Token                 | 值          |
|-----------------------|-------------|
| neutral_1000_alpha_4  | `#0a000000` |
| neutral_1000_alpha_8  | `#14000000` |
| neutral_1000_alpha_12 | `#1f000000` |
| neutral_1000_alpha_16 | `#29000000` |
| neutral_1000_alpha_40 | `#66000000` |
| neutral_1000_alpha_60 | `#99000000` |
| neutral_0_alpha_4     | `#0affffff` |
| neutral_0_alpha_8     | `#14ffffff` |
| neutral_0_alpha_12    | `#1fffffff` |
| neutral_0_alpha_16    | `#29ffffff` |
| neutral_0_alpha_40    | `#66ffffff` |

### 4.3 Lucent 语义色

来源：`com.nothing.dynamiccolor/res/values/colors.xml` 与 `values-night/colors.xml`。

| Token                            | Light       | Dark        |
|----------------------------------|-------------|-------------|
| container_primary                | `#f9f9f9`   | `#181818`   |
| container_secondary              | `#ffffff`   | `#000000`   |
| container_tertiary               | `#52ffffff` | `#3d000000` |
| container_accent                 | `#303030`   | `#474747`   |
| container_accent_alert           | `#cb2117`   | `#c31d15`   |
| container_accent_caution         | `#ffc700`   | `#ffc700`   |
| container_frosted_backdrop       | `#66ffffff` | `#66000000` |
| content_on_solid_primary         | `#e6131313` | `#ffffffff` |
| content_on_solid_secondary       | `#80131313` | `#99ffffff` |
| content_on_solid_tertiary        | `#4c131313` | `#4cffffff` |
| content_on_translucent_primary   | `#cc070707` | `#f5f3f3f3` |
| content_on_translucent_secondary | `#66070707` | `#99f3f3f3` |
| content_on_translucent_tertiary  | `#33070707` | `#4cf3f3f3` |
| content_on_accent_primary        | `#f5ffffff` | `#f5ffffff` |
| content_on_accent_secondary      | `#99ffffff` | `#99ffffff` |
| content_on_accent_tertiary       | `#4cffffff` | `#4cffffff` |
| content_accent                   | `#5e5e5e`   | `#c6c6c6`   |
| content_accent_alert             | `#c53241`   | `#d12929`   |
| content_accent_1                 | `#d71921`   | `#d71921`   |
| content_accent_2                 | `#484848`   | `#484848`   |
| content_accent_3                 | `#4c94eb`   | `#4c94eb`   |
| content_accent_4                 | `#ffc700`   | `#ffc700`   |

### 4.4 圆角

| Token                              | 值       |
|------------------------------------|----------|
| border_radius_2xs                  | 4dp      |
| border_radius_xs                   | 8dp      |
| border_radius_s                    | 16dp     |
| border_radius_m                    | **20dp** |
| border_radius_l                    | 24dp     |
| border_radius_xl                   | 32dp     |
| border_radius_full                 | 9999dp   |
| dialog_corner_radius               | 20dp     |
| datepicker_dialog_corner_radius    | 28dp     |
| collector_widget_aod_corner_radius | 16dp     |

### 4.5 间距

| Token         | 值   |
|---------------|------|
| ref_spacer_1  | 1dp  |
| ref_spacer_2  | 2dp  |
| ref_spacer_4  | 4dp  |
| ref_spacer_8  | 8dp  |
| ref_spacer_12 | 12dp |
| ref_spacer_16 | 16dp |
| ref_spacer_20 | 20dp |
| ref_spacer_24 | 24dp |
| ref_spacer_28 | 28dp |
| ref_spacer_32 | 32dp |
| ref_spacer_40 | 40dp |
| ref_spacer_48 | 48dp |
| ref_spacer_52 | 52dp |
| ref_spacer_56 | 56dp |
| ref_spacer_64 | 64dp |
| ref_spacer_96 | 96dp |

列表项：

- 最小高度：52dp
- 水平内边距：16dp
- 垂直内边距：12dp

### 4.6 官方 Countdown Widget 参考

`com.nothing.hearthstone`：

- 2×2
- 20dp 圆角
- 16dp 内边距
- 日期 20sp
- 支持 Days / Months / Years
- 支持 Never / Weekly / Monthly / Yearly
- 支持提醒
- 支持过期后 Count Up
- 6 种液态形状进度（`ShapeProgressView`，带重力感应）
- 使用 `@android:color/system_neutral1_*`、`system_accent1_*` 作为语义色

Daymark 不需要复刻液态重力；Widget 内用静态形状填充即可，App 内可以用 Compose Canvas 实现液态效果。

---

## 5. 推荐的 Widget 架构

### 5.1 为什么用 RemoteViews 而不是 Glance

| 能力                                        | Glance                | RemoteViews                                   |
|---------------------------------------------|-----------------------|-----------------------------------------------|
| `setLightBackgroundLayoutId`                | 不暴露                | 支持                                          |
| 自定义 shape / vector / gradient 背景       | 受限                  | 支持                                          |
| `letterSpacing` / `lineHeight` / `maxLines` | 受限                  | TextView 原生支持                             |
| 自定义字体                                  | 不支持                | 不支持（字体不是重点）                        |
| 响应式多布局                                | `SizeMode.Responsive` | `RemoteViews(Map<SizeF, RemoteViews>)` API 31 |
| 逐实例状态                                  | 可以                  | 标准 AppWidgetProvider + DataStore            |
| 实时模糊 / 液态重力                         | 不支持                | 不支持                                        |

结论：

- Widget：XML RemoteViews + Kotlin/Java builder。
- App 内：Jetpack Compose。
- 不为 Widget 引入 Glance。

### 5.2 Widget 规格

| Widget        | 尺寸      | 内容                                      |
|---------------|-----------|-------------------------------------------|
| HeroCountdown | 2×2 / 4×2 | 大数字、单位、事件名、日期、可选形状进度  |
| EventCard     | 2×2 / 3×2 | 事件名、剩余/已过天数、目标日期、周年徽标 |
| Upcoming      | 4×2       | 最近 3 个事件，标题 + 天数 + 细分割线     |
| Dashboard     | 4×4       | 主事件 + 3 个 upcoming + Count Up 摘要    |

### 5.3 主题

```kotlin
enum class WidgetStyle {
    LUCENT,   // 透明 + Lucent 文字色
    FROSTED,  // 半透明 scrim
    SOLID,    // 实色 Nothing 表面
    MATERIAL  // M3 动态色
}
```

默认：

- Nothing 设备：`LUCENT`
- 其他 Android 12+：`MATERIAL`
- 用户可在每个 Widget 配置中覆盖

### 5.4 颜色策略

Widget XML 直接使用系统角色：

```xml

<TextView android:textColor="@android:color/system_neutral1_900" />

<ProgressBar android:progressTint="@android:color/system_accent1_500" />
```

半透明背景使用固定 alpha：

```xml

<shape>
    <corners android:radius="20dp" />
    <solid android:color="#29ffffff" />
</shape>
```

亮壁纸：

```kotlin
val views = RemoteViews(pkg, R.layout.widget_countdown_on_dark)
views.setLightBackgroundLayoutId(R.layout.widget_countdown_on_light)
```

壁纸亮度判断：

```kotlin
val colors = WallpaperManager.getInstance(context)
        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)

val prefersDarkText = colors?.colorHints
        ?.and(WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
```

### 5.5 进度形状

Widget 内：

- `ProgressBar` + 自定义 `progressDrawable`
- 或 `ImageView` + `ClipDrawable` + `setImageLevel`
- 或静态 vector shape + 高度裁剪

不要尝试在 Widget 内做液态/重力动画。

App 内：

- Compose `Canvas` + `Path`
- 可选重力传感器
- `RenderEffect` / `Modifier.blur` 做磨砂

### 5.6 更新策略

- `android:updatePeriodMillis="0"`
- 事件增删改：立即 `updateAppWidget`
- 跨午夜：`AlarmManager` 非精确 + `WorkManager` 兜底
- `ACTION_DATE_CHANGED`
- `ACTION_TIME_CHANGED`
- `ACTION_TIMEZONE_CHANGED`
- `BOOT_COMPLETED`
- `ACTION_WALLPAPER_CHANGED`（如果使用壁纸亮度判断）
- Widget resize / options change：重新构建布局

### 5.7 逐实例配置

```kotlin
data class WidgetConfig(
    val eventId: Long?,
    val style: WidgetStyle,
    val showDate: Boolean,
    val countUp: Boolean,
    val accent: Accent
)
```

以 `appWidgetId` 为 key 存 DataStore。

`appwidget-provider`：

```xml
android:configure=".widget.WidgetConfigActivity"android:widgetFeatures="reconfigurable|configuration_optional"android:targetCellWidth="2"android:targetCellHeight="2"android:previewLayout="@layout/widget_preview"android:resizeMode="horizontal|vertical"
```

---

## 6. App 内 UI 架构

### 6.1 主题切换

```kotlin
@Composable
fun DaymarkTheme(content: @Composable () -> Unit) {
    when (themeMode) {
        NOTHING -> NothingTheme(content)
        MATERIAL -> MaterialExpressiveTheme(
            colorScheme = dynamicColorScheme(),
            content = content
        )
        SYSTEM -> if (isNothingDevice) NothingTheme(content)
        else MaterialExpressiveTheme(...
            )
    }
}
```

### 6.2 Nothing 组件规范

| 组件                 | 规格                                    |
|----------------------|-----------------------------------------|
| NothingCard          | 20dp 圆角，1dp `#0a000000` 边框，无阴影 |
| NothingListItem      | 最小 52dp，水平 16dp，垂直 12dp         |
| NothingSectionHeader | 14sp，medium，secondary                 |
| NothingButton        | 40dp 高，黑/白填充，20dp 或 8dp 圆角    |
| NothingDivider       | 1dp，4% 对比度                          |
| NothingSwitch        | 28dp track，24dp thumb                  |
| Hero 数字            | 64–96sp，weight 500，tabular            |

### 6.3 页面

- Home：主事件大数字 + 最近事件
- Events：LazyColumn，52dp 行高，细分割线
- Calendar：月视图，1dp 网格
- Editor：日期、重复、提醒、分类
- Settings：主题、Widget 默认值、通知、备份

### 6.4 App 内 Lucent

- API 31+：`Modifier.blur(24.dp)` + 半透明色层
- API 26–30：半透明 scrim 降级
- 不要用壁纸截图冒充实时磨砂

### 6.5 M3 fallback 版本

- Compose BOM：`2026.09.00`
- `material3`：建议 `1.5.0-rc01`
    - `1.5.0` 的 Expressive API 已基本稳定
    - `1.4.0` 中 `MaterialExpressiveTheme` 是 internal，只有基础 M3
- 非 Nothing：`dynamicLightColorScheme` / `dynamicDarkColorScheme`
- Nothing：`NothingTheme` 覆盖 M3 的颜色/形状/排版，但复用 M3 组件行为

---

## 7. 验证计划

### 7.1 待验证问题

1. Nothing Launcher 是否会对我们自己的 `@android:color/system_*` 引用执行 `setColorResources` 重写？
2. `setLightBackgroundLayoutId` 在 Nothing Launcher 上是否真的会切换？
3. Lucent 开启/关闭时，第三方 Widget 是否会被额外加背景？
4. `color_source=preset` 时逐 Widget 壁纸采样是否关闭？（代码显示会关闭）
5. `targetCellWidth/Height`、`previewLayout` 在 Nothing Launcher 上的实际表现？
6. 透明根布局是否真的透出壁纸，且没有 Launcher 额外背景？
7. 20dp 圆角是否与 Nothing 官方 Widget 视觉一致？

### 7.2 NothingUiProbe

`lab/nothing-ui-probe/` 是一个最小验证 APK，包含 6 个测试 Widget：

1. Completely Transparent
2. 16% White Tint
3. 40% Black Tint
4. Solid Nothing Surface
5. System Colors (direct `@android:color/system_*`)
6. Light Background Layout (`setLightBackgroundLayoutId`)

测试矩阵：

| 环境                       | 目标                   |
|----------------------------|------------------------|
| Nothing OS 5.0 + Lucent 开 | 透明/半透明/文字可读性 |
| Nothing OS 5.0 + Lucent 关 | 是否仍正常             |
| 亮壁纸                     | 深色文字布局           |
| 暗壁纸                     | 浅色文字布局           |
| 高对比壁纸                 | 半透明 scrim 是否足够  |
| color_source = wallpaper   | 逐 Widget 着色         |
| color_source = preset      | 静态主题               |
| 不同 Widget 尺寸           | responsive 行为        |
| Pixel Launcher（可选）     | 标准 Android 兼容性    |

### 7.3 验证结果（2026-11-08）

已构建并安装 `lab/nothing-ui-probe`，六个 2×2 Widget 全部固定在 Nothing Launcher 最后一页。

#### 已验证

| # | 项目                                | 结果                                                                                                             |
|---|-------------------------------------|------------------------------------------------------------------------------------------------------------------|
| 1 | 透明根布局                          | **成立**。壁纸完全透出，Launcher 没有额外添加背景或圆角。                                                        |
| 2 | 16% 白色半透明                      | **成立**。20dp 圆角、半透明层正常渲染；在白字场景下对比度一般。                                                  |
| 3 | 40% 黑色半透明                      | **成立**。可读性明显好于透明；在暖色壁纸上呈暖棕色，接近官方但偏亮。                                             |
| 4 | 实色表面                            | **成立**。深色实色卡片的对比度最好，最接近官方 Widget 的稳定观感。                                               |
| 5 | 直接 `@android:color/system_*`      | **能渲染**。系统 accent 颜色（青蓝色）生效；但当前 `color_source=preset`，未触发 Launcher 的逐 Widget 壁纸重写。 |
| 6 | `setLightBackgroundLayoutId`        | **已接入**，但当前壁纸为暗色，未触发深色文字布局；需要亮色壁纸继续验证。                                         |
| 7 | Widget 预览                         | **成立**。`previewLayout` 在 Nothing Launcher 的添加面板正常显示。                                               |
| 8 | `RemoteViews.setBackgroundResource` | **成立**。六个 Widget 共用布局，仅通过该方法切换背景。                                                           |
| 9 | 20dp 圆角                           | **成立**。与官方 Widget 的圆角观感一致。                                                                         |

#### 主题切换观察

在 `Theme & Style` 中切换：

- **Translucent / Lucent**
- **Classic**
- **Dynamic**

观察结果：

> **第三方 Widget 的渲染不随 Launcher 主题变化。**  
> 切换 Classic 后，图标变为单色，Nothing 官方 Widget 有变化；我们的六个探针 Widget 完全不变。  
> 切换到 Dynamic 后，第三方 Widget 也没有变化。

结论：

- Nothing Launcher **不会**给第三方 Widget 自动加 Lucent 背景、模糊或圆角。
- 第三方 Widget 的 Lucent 观感必须完全由 Provider 自己绘制。
- `setColorResources` 的逐 Widget 壁纸重写是另一条独立机制，受 `color_source` 控制；本次未成功触发。

#### 官方 Countdown 对比

官方 Countdown（用户提供的截图）特征：

- 深色、带壁纸色调的半透明/磨砂卡片，而不是纯透明。
- 20dp 圆角。
- 大尺寸形状 + 蓝色液体填充。
- 底部 `20D` 使用点阵字体。
- 整体比探针的 40% 黑层更暗、更有层次。

对 Daymark 的直接影响：

1. **默认 Widget 不应使用纯透明。**  
   纯透明在当前复杂壁纸下白字对比度不足。默认应为 **深色 tinted translucent**（约 50–60%
   黑）或实色；透明作为可选模式。
2. **40% 黑层偏亮。**  
   在暖色壁纸上会变棕，和官方仍有差距。可测试 50% / 60% / 70% 黑层，或使用 `container_secondary` 深色 +
   alpha。
3. **官方是“形状 + 小标签”结构，不是“大字 + 多行文本”。**  
   Daymark 可以保留大字可读性，但应提供一套 `Shape` 风格：形状进度 + 小号 `128D` 标签，作为 Nothing
   默认样式。
4. **点阵字体是强识别信号。**  
   字体不是硬性要求，但 `20D` 这种点阵小标签可以作为可选主题，不影响默认 sans 方案。

#### 当前限制

- 当前系统颜色来源为 `preset`，`com.nothing.launcher.widget.e` 的逐 Widget 壁纸采样处于关闭状态。
- 尝试通过 `adb settings put` 切换到 `home_wallpaper` 时，JSON 被设备端 shell 的引号解析破坏；已恢复原值，未完成该项验证。
- 尚未测试亮色壁纸下的 `setLightBackgroundLayoutId` 切换。
- 尚未测试 Widget 放大到 4×2 / 4×4 后的 responsive 行为。

#### 下一步验证

1. 用 Launcher 的 Wallpaper & style UI 切换到 `home_wallpaper` 颜色来源，确认系统颜色 Widget 的
   accent 是否随壁纸变化。
2. 切换到亮色壁纸，确认 Widget 6 是否切换为 `widget_probe_dark_text`。
3. 测试 50% / 60% / 70% 黑层，选择最接近官方 Countdown 的默认值。
4. 测试 4×2 / 4×4 尺寸与内容裁剪。
5. 把验证结果回填到本文档，并据此冻结 `NothingTokens` 的 Widget 默认值。

---

## 8. 路线图

| 阶段    | 内容                                  | 交付               |
|---------|---------------------------------------|--------------------|
| Phase 0 | NothingUiProbe 六 Widget 验证         | 确认 Launcher 行为 |
| Phase 1 | NothingTokens + NothingTheme + 组件库 | App 内设计系统     |
| Phase 2 | RemoteViews Widget 框架 + 三套主题    | 4 个 Widget        |
| Phase 3 | Widget 配置、预览、逐实例状态         | 可发布 Widget      |
| Phase 4 | Home / Events / Calendar / Editor     | App 主界面         |
| Phase 5 | 日期引擎、提醒、备份                  | 完整产品           |

---

## 9. 法律与品牌注意事项

- 不要分发 Nothing 的 APK、drawable、字体或修改后的资源。
- 不要把应用命名为 “Nothing 官方” 或使用 Nothing 商标作为产品名。
- 可以使用 “Designed for Nothing OS” / “Nothing OS compatible” 这类兼容性描述，但要避免暗示官方背书。
- 不要声明 `com.nothing.permission.BIND_CARD_SERVICE` 或尝试注册 `com.nothing.card` Provider。
- `com.nothing.dynamiccolor` 是未文档化 shared library，使用时必须有 fallback，并接受 OTA 后失效的风险。
- 提取的颜色/尺寸数值用于互操作性研究；正式发布时以自己的资源文件重新实现。

---

## 10. 参考来源

- Nothing OS 5.0 官方页面：<https://nothing.tech/nothing-os>
- Android App Widgets：<https://developer.android.com/develop/ui/views/appwidgets>
-
`AppWidgetHostView.setColorResources()`：<https://developer.android.com/reference/android/appwidget/AppWidgetHostView>
-
`RemoteViews.setLightBackgroundLayoutId()`：<https://developer.android.com/reference/android/widget/RemoteViews#setLightBackgroundLayoutId(int)>
- Android Dynamic Color：<https://developer.android.com/develop/ui/views/theming/dynamic-colors>
- 本机提取：`NothingLauncher3.apk` 5.0.6、`NothingCardLab.apk` 5.0.0、`NothingDynamicColor.apk` 1.0
