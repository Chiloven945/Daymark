# Daymark Architecture

## Overview

Daymark is a local-first countdown, count-up, and anniversary app. The first release uses Jetpack
Compose with Material 3 Expressive. Nothing OS styling and widgets are intentionally kept behind
separate seams so they can be added without changing domain logic.

## Module graph

```text
:app
 ├── :feature:home
 ├── :feature:events
 ├── :feature:calendar
 ├── :feature:editor
 ├── :feature:settings
 ├── :core:data
 ├── :core:designsystem
 └── :widget

:feature:*        -> :core:model, :core:data, :core:designsystem
:core:data        -> :core:model
:core:designsystem-> :core:model
:widget           -> :core:model, :core:data
```

### Module responsibilities

| Module               | Responsibility                                                                   |
|----------------------|----------------------------------------------------------------------------------|
| `:app`               | Application, activity, root navigation, Hilt aggregation, app-level theme wiring |
| `:core:model`        | Pure domain models, recurrence, `DateCalculator`, `TimeProvider`                 |
| `:core:data`         | Room, DataStore, repositories, export/import, AlarmManager reminders             |
| `:core:designsystem` | Material 3 theme, tokens, shared Compose components, date/category formatting    |
| `:feature:home`      | Next event, today, upcoming                                                      |
| `:feature:events`    | Search, filters, grouped event list, pin/delete                                  |
| `:feature:calendar`  | Month grid and selected-day events                                               |
| `:feature:editor`    | Event detail and create/edit form                                                |
| `:feature:settings`  | Appearance, calendar, notifications, data, about                                 |
| `:widget`            | Reserved for M6 RemoteViews implementation                                       |

## Date model

- Events store a calendar date, not an instant.
- `Event.date` is a `java.time.LocalDate`.
- Room stores `dateEpochDay` as a `Long`.
- `DateCalculator` owns all recurrence and day-count logic.
- `TimeProvider` makes “today” injectable for tests and midnight refresh.
- Recurrence is index-anchored:
    - weekly: `event.date.plusWeeks(index * interval)`
    - monthly: `event.date.plusMonths(index * interval)`
    - yearly: `event.date.plusYears(index * interval)`
- This keeps month-end recurrence stable (for example Jan 31 → Feb 28 → Mar 31).
- February 29 yearly recurrence resolves to February 28 in non-leap years.

## Persistence

### Room

`DaymarkDatabase` currently has one table:

```text
events
- id
- title
- dateEpochDay
- direction
- recurrenceFrequency
- recurrenceInterval
- recurrenceEndEpochDay
- categoryId
- note
- isPinned
- reminderOffsetDays
- reminderTimeMinutes
- createdAt
- updatedAt
```

The schema is version 1. Destructive migration is enabled for the first release; a real migration
policy should be added before shipping a production upgrade.

### DataStore

`SettingsRepositoryImpl` stores:

- theme mode
- dynamic color
- first day of week
- notifications enabled
- default widget style

## State flow

```text
Room / DataStore
      │
      ▼
Repository (Flow)
      │
      ▼
ViewModel (StateFlow<UiState>)
      │
      ▼
Compose screen (collectAsStateWithLifecycle)
```

- Features never touch Room directly.
- UI receives immutable state and callbacks.
- Date logic lives in `DateCalculator`, not in composables.
- One-off side effects (snackbars, navigation) are modeled as state flags or
  `SharedFlow` events.

## Navigation

Type-safe Navigation Compose routes:

```text
MainRoute
 ├── HomeRoute
 ├── EventsRoute
 ├── CalendarRoute
 └── SettingsRoute

EventDetailRoute(eventId)
EventEditorRoute(eventId?)
```

`MainShell` owns the four-tab nested `NavHost` inside a custom adaptive shell:

- compact windows use `FloatingNavigationBar` built from Material 3
  `ShortNavigationBar` inside a translucent, shadowed Surface
- medium and expanded windows use `FloatingNavigationRail`
- detail and editor live in the root `NavHost`, so the floating navigation disappears for
  full-screen editing

Root navigation uses horizontal slide/fade transitions. Predictive back overrides Navigation
Compose's default `scaleOut(0.7f)` with edge-aware horizontal slide transitions. The nested tab host
uses a short fade-through transition and never scales.

## Theme

`DaymarkTheme` uses:

- `MaterialExpressiveTheme`
- `MotionScheme.expressive()`
- Material 3 dynamic color on API 31+
- day/night fallback schemes
- custom 4/8/12/20/28 dp shape scale
- tabular figures for countdown numerals
- `DaymarkBackground` gradient plus soft primary/tertiary glows
- `DaymarkSurfaceCard` tonal surface with a subtle outline

Feature code uses `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and
`MaterialTheme.shapes` only. A future Nothing theme replaces `DaymarkTheme`
without changing feature screens.

## Reminders

`AlarmReminderScheduler`:

1. Reads the next occurrence from `DateCalculator`.
2. Subtracts the reminder offset.
3. Schedules an exact alarm when `canScheduleExactAlarms()` is true.
4. Falls back to an inexact alarm otherwise.
5. Cancels the alarm when the reminder is removed or the event is deleted.

`ReminderReceiver` creates the notification channel and posts the reminder.
`RescheduleReceiver` reschedules all reminders after boot, time changes, timezone changes, and
package replacement.

Permissions:

- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- `RECEIVE_BOOT_COMPLETED`

## Export / import

`EventExportService` serializes a versioned JSON payload:

```json
{
  "version": 1,
  "exportedAt": "2026-11-08T00:00:00Z",
  "events": []
}
```

Settings uses the Storage Access Framework (`CreateDocument` / `OpenDocument`). Import parses the
full payload before writing to Room.

## Build

- AGP 9.3.3
- Gradle 9.8.1
- Kotlin 2.4.21
- JDK 21 runtime, Java 17 bytecode target
- compileSdk / targetSdk 37
- minSdk 31
- Compose BOM 2026.09.00
- Material3 1.5.0-rc01
- Hilt 2.60.1 + KSP 2.3.13
- Room 2.8.5
- DataStore 1.3.0

```bash
./gradlew test :app:assembleDebug
```

## Testing

Current unit tests:

- `DateCalculatorTest`: 9 cases covering today, future, past, count-up, auto, yearly recurrence,
  February 29, month-end recurrence, end dates, and ranges.
- `EventExportServiceTest`: export/import round trip and unsupported version.

## Future seams

- `WidgetRefreshScheduler`: replace the no-op with `AppWidgetManager` updates.
- `:widget`: add RemoteViews providers, configuration activity, and previews.
- `DaymarkTheme`: add a Nothing brand variant.
- `RescheduleReceiver` and notification intents: add event deep links.
