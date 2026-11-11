# План разработки «Pinduoduo KZ Calc»

Источник требований: [TZ.md](TZ.md). Статус: этапы 0–7 реализованы (unit-тесты, debug и release сборки зелёные; финальная ручная проверка §8.2–8.3 на физическом устройстве/эмуляторе — по факту установки release APK).

## Принятые решения

| Вопрос | Решение |
|---|---|
| Затемнение фото-фона | Один слайдер `0.15..0.75`, дефолт `0.40`, без пресетов |
| `cargoTotalYuan` | Базовая модель `totalYuan` (паритет с веб-эталоном) |
| Недостающие компоненты (`GlassCard`, `SegmentedControl`, `IOSAlertDialog`, `GlassSnackbar`, `AppBackground`) | Добавляются в общий кит `../suldev-ui`, портируются из `food-track/app/.../ui/components/`. Цвета приходят параметрами/токенами, без зависимости кита от `PddColors` |
| Git | `git init` + `.gitignore` на этапе 0; коммиты только по просьбе |
| Сеть | OkHttp + `kotlinx.serialization` (без Ktor) |
| Кнопка «Формулы» и тап по карточке варианта | Делаются (шторка обязательна по §8.3) |
| Примеры `$ 178.41` (§2.4) и `Экономия $ 24.29` (§5.3) | Иллюстрации; истина — формулы §2.3–2.5 |
| Расположение тестов (§7) | `app/src/test/java/com/suldev/pddkz/` (в ТЗ опечатка) |
| Версии сборки | Как в `suldev-ui`: Gradle 8.14.3, AGP 8.13.2, Kotlin 2.4.21; wrapper копируется оттуда |

## Что уже есть в экосистеме

- `suldev-ui`: `SulTopBar`, `sulTopBarContentPadding()`, `SulGlassTabBar`, `SulTab`, `SulBarGlass`, `SulPillMotion`, `rememberSulBarScrollConnection`.
- `suldev-kit`: `FloatingBarsTapGuard`, `IosOverscrollEffect`, `rememberIosOverscrollConnection`, `Modifier.iosOverscroll`, `HapticDevice`, `HapticPattern`.

## Этапы

Каждый этап заканчивается проверяемым результатом; следующий не начинается, пока предыдущий не зелёный.

### 0. Каркас
- `git init`, `.gitignore`, Gradle wrapper.
- `settings.gradle.kts` с тремя `includeBuild`, `app/build.gradle.kts` по §1.1–1.2.
- Пустая `MainActivity` с `enableEdgeToEdge`.
- **Готово, когда:** `./gradlew assembleDebug` проходит.

### 1. Домен (без Android)
- `CalculatorInput`, `NbkRates`, `CalculationResult` + `ComparisonOutcome` (4 исхода), `AppSettings`.
- `CalculateDeliveryUseCase`: формулы §2.3, точка безубыточности.
- Форматтеры §2.4, парсинг ввода (`,` и `.`, защита от нечисел и деления на ноль).
- **Готово, когда:** `CalculateDeliveryUseCaseTest` зелёный — 180/1.2/1 → Карго, 50/3.0/1 → Казпочта, 0 ¥ → нейтральный, равенство сумм, KZT↔USD.

Точка безубыточности: `yuan = cargoShippingKzt × 100 / ((vatPct + feePct) × rateCnyKzt × qty)`, при `vatPct + feePct = 0` не определена.

### 2. Данные
- `NbkRatesApi`: `GET /api/rates`, таймауты 10 с, `NbkRatesDto`.
- `PreferencesStorage` (DataStore): настройки, кэш курсов, имя файла фона.
- `RatesRepository`: кэш → сеть, фолбэк `68.11 / 453.71`, четыре статусные метки §2.2.
- **Готово, когда:** `NbkRatesParsingTest` зелёный (валидный JSON, битый JSON, HTTP ≠ 2xx, ошибка при кэше и без него).

### 3. Компоненты кита, тема и корень
- В `suldev-ui`: `GlassCard` (стекло + плоский фолбэк + deep), `SegmentedControl`, `IOSAlertDialog`, `GlassSnackbar`, `AppBackground` (PLAIN, MESH, IMAGE с вуалью).
- В приложении: `PddColors` light/dark, типографика §4.3, `LocalGlassState` / `LocalInlineGlassState`, `PddTheme`.
- `PddAppRoot`: 7 слоёв §3.1, два источника Material Fluid, бары, tap guard, `AnimatedContent` вкладок.
- **Готово, когда:** две пустые вкладки переключаются; стекло работает на API 33+ и даёт плоский фолбэк на API < 33; демо `suldev-ui` и `food-track` по-прежнему собираются.

### 4. Экран «Калькулятор»
- `PddCalculatorViewModel` (`StateFlow`, валидация, мгновенный пересчёт).
- `IosNumberField`, `IosButton`, `SectionHeader`.
- 7 блоков §5.3, адаптив 600 dp и 360 dp, pull-to-refresh, диалог сброса (курсы сохраняются).
- **Готово, когда:** цифры совпадают с `https://pdd-kz-calc.netlify.app/` на дефолтах и на 2–3 произвольных наборах.

### 5. Шторка «Детали расчёта и формулы»
- `SwipeDismissSheet` + `SheetPortal`: порог 120 dp, `spring(1f, 400f)`, `BackHandler`, внутренний overscroll.
- `FormulaDetailsSheet`: пошаговые формулы на текущих числах, точка безубыточности, «Закрыть».
- **Готово, когда:** открывается из бара и по тапу на карточку, закрывается свайпом и «Назад».

### 6. Экран «Настройки»
- Тема, материал стекла (сразу в оба состояния), фон, слайдер затемнения.
- `BackgroundImageStorage`: EXIF-поворот, downsample ≤ 2048 px, JPEG q=88 под `Mutex`, удаление старого файла, откат к PLAIN при ошибке.
- Блоки «Доступность» и «О данных и расчётах».
- **Готово, когда:** настройки переживают перезапуск; смена и удаление фото не оставляют файлов в `filesDir/backgrounds/`.

### 7. Полировка и приёмка
- `increaseContrast` на `ON_RESUME`, `sampleLuminance` и цвет иконок статус-бара, reduced motion.
- Сверка тактильности по таблице §6.2 (без двойных срабатываний).
- Проверки §8.1: нет `Color(0xFF...)`, `TODO`, `fontSize = ...sp` в `ui/screens/`.
- **Готово, когда:** чек-лист §8 пройден на эмуляторах API 33+ и API < 33, темы × фоны.

## Не проверено до старта

- ~~`RefreshPuck` в `SulTopBar`~~ — есть: параметры `refreshing` и `pullFraction`.
- ~~Есть ли `SwipeDismissSheet` / `SheetPortal` в `food-track`~~ — нет; реализованы с нуля в `suldev-ui` (`SulSheet.kt`), демо `suldev-ui` и `food-track` собираются.
- ~~`TOGGLE_ON` / `TOGGLE_OFF` в `HapticPattern`~~ — нет (есть `Click`, `Tick`, `LowTick`, `HeavyClick`, `DoubleTick`, `Error`); на этапе 6 — `View.performHapticFeedback`.
- ~~Поведение веб-эталона~~ — сверено на этапе 4: дефолты и набор 50 ¥ / 3 кг / 2 шт в KZT и USD совпали до тенге и цента.

## Риски

- Этап 3 меняет публичный API `suldev-ui`, от которого зависят другие приложения: только добавление новых файлов, существующие сигнатуры не трогаются.
- Цикл `RenderNode` (SIGSEGV) при неверной иерархии источников — инварианты §3.1 проверяются сразу на этапе 3, до наполнения экранов.
