# Техническое задание: Нативное Android-приложение «Pinduoduo KZ Calc»

> **Версия спецификации:** 1.0  
> **Целевая платформа:** Android (Kotlin, Jetpack Compose, Edge-to-Edge)  
> **Соответствие стандартам:** [`suldev-guidelines`](../suldev-guidelines/README.md) · Движок стекла: [`Material Fluid`](../material-fluid/README.md) (`2.1.0`) · Движки и UI-кит: [`suldev-kit`](../suldev-kit/README.md), [`suldev-ui`](../suldev-ui/README.md)

---

## 1. Назначение продукта и паспорт проекта

**Pinduoduo KZ Calc** — нативный Android-калькулятор для расчёта и наглядного сравнения итоговой стоимости и сроков доставки товаров с маркетплейса Pinduoduo (拼多多) в Казахстан двумя способами:
1. **Казпочта (Прямая / Официальная)** — расчёт с учётом НДС и комиссии, без доплаты за вес при получении в отделении.
2. **Карго Компания (Посредник)** — расчёт с учётом тарифа доставки за вес (`$/кг`) и актуального курса доллара, с включёнными налогами и таможней.

Приложение воспроизводит всю доменную логику, тарифную модель, форматы отображения и работу с курсами Национального Банка РК эталонного веб-калькулятора `https://pdd-kz-calc.netlify.app/`, переводя интерфейс на нативную архитектуру Jetpack Compose по дизайн-парадигме **iOS HIG × Material 3** и стандартам **suldev-guidelines**.

### 1.1. Технический паспорт

| Параметр | Требование |
|---|---|
| **Application ID / Namespace** | `com.suldev.pddkz` |
| **Язык и UI** | Kotlin, Jetpack Compose (`androidx.compose:compose-bom:2024.12.01`), Material 3 (`MaterialTheme`), Edge-to-Edge |
| **SDK** | `minSdk = 26` (совместимость с `:suldev-ui`), `compileSdk = 36`, `targetSdk = 36`, JVM `17` |
| **Шейдерное стекло** | `com.materialfluid:material-fluid:2.1.0` (AGSL-преломление на **API 33+**, честный плоский фолбэк на **API 26–32**) |
| **Модули экосистемы** | `com.suldev.kit:touch-overscroll:1.0.0`, `com.suldev.kit:haptic-engine:1.0.0`, `com.suldev:suldev-ui:1.0.0` |
| **Сеть и сериализация** | OkHttp + `kotlinx.serialization` (или Ktor Client `OkHttp` engine) |
| **Локальное хранение** | Jetpack DataStore (`Preferences`) для настроек, кэша курсов НБ РК и имени файла фона |
| **Тестирование** | JUnit 4 + `kotlinx-coroutines-test` для доменных формул, парсинга API и кэширования |

### 1.2. Подключение композитных сборок

В `settings.gradle.kts` целевого проекта подключаются локальные модули экосистемы без дублирования их исходного кода:

```kotlin
includeBuild("../material-fluid")
includeBuild("../suldev-kit")
includeBuild("../suldev-ui")
```

В `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.materialfluid:material-fluid:2.1.0")
    implementation("com.suldev.kit:touch-overscroll:1.0.0")
    implementation("com.suldev.kit:haptic-engine:1.0.0")
    implementation("com.suldev:suldev-ui:1.0.0")
}
```

---

## 2. Функциональные требования и доменная логика

### 2.1. Входные параметры и значения по умолчанию

Все расчёты пересчитываются **реактивно (в реальном времени)** при изменении любого поля ввода, переключении валюты или обновлении курсов валют — без кнопки «Рассчитать».

| Параметр | Идентификатор поля | Подпись в UI | Дефолт | Тип и ограничения |
|---|---|---|---|---|
| **Отображаемая валюта** | `currency` | `₸ KZT` / `$ USD` | `KZT` | `DisplayCurrency { KZT, USD }` |
| **Цена товара (1 шт)** | `itemYuan` | Стоимость товара в Пиндодо (¥ Yuan) | `180` | `Double`, `>= 0.0`, шаг ориентира `1` |
| **Вес 1 шт** | `itemWeightKg` | Вес 1 шт (кг) | `1.2` | `Double`, `>= 0.0`, рекомендуемый min `0.05`, шаг `0.1` |
| **Количество** | `itemQty` | Количество (шт) | `1` | `Int`, `>= 1` (при пустом поле в вычислении безопасный фолбэк `1` с inline-подсказкой) |
| **НДС Казпочты** | `kazpostVatPct` | НДС (%) | `16` | `Double`, `>= 0.0`, шаг `1` |
| **Комиссия Казпочты** | `kazpostFeePct` | Комиссия (%) | `3` | `Double`, `>= 0.0`, шаг `0.5` |
| **Срок Казпочты** | `kazpostDays` | Сроки (дней) | `"10–18"` | `String`, непустая строка; при пустом вводе подпись `"10–18"` |
| **Тариф Карго** | `cargoRateUsdPerKg` | Тариф ($/кг) | `3.5` | `Double`, `>= 0.0`, шаг `0.1` |
| **Срок Карго** | `cargoDays` | Сроки (дней) | `"12–25"` | `String`, непустая строка; при пустом вводе подпись `"12–25"` |

#### Сброс к значениям по умолчанию (`resetDefaults`)
При подтверждении сброса:
- Параметры посылки сбрасываются к `180`, `1.2`, `1`.
- Тарифы и сроки сбрасываются к `16`, `3`, `"10–18"`, `3.5`, `"12–25"`.
- Выбранная валюта сбрасывается в `KZT`.
- **Актуальные курсы НБ РК при этом сохраняются** (не откатываются на хардкодные фолбэки, если свежие курсы уже получены).

### 2.2. Сетевой контракт и работа с курсами НБ РК

| Параметр | Спецификация |
|---|---|
| **Endpoint** | `GET https://pdd-kz-calc.netlify.app/api/rates` |
| **Таймауты** | Connect `10 s`, Read `10 s` |
| **Ответ `200 OK`** | `{"usd": 444.76, "cny": 66.68, "date": "12.11.2026", "source": "Национальный Банк РК"}` |

#### Модель ответа (`NbkRatesDto`)
- `usd: Double` — официальный курс `1 $` в тенге (`₸`).
- `cny: Double` — официальный курс `1 ¥` в тенге (`₸`).
- `date: String` — дата актуальности курса в формате `dd.MM.yyyy`.
- `source: String` — источник (`"Национальный Банк РК"`).

#### Логика состояний и оффлайн-фолбэка
1. **Стартовые дефолты до ответа сети (и если кэш пуст):**
   - `rateCnyKzt = 68.11`
   - `rateUsdKzt = 453.71`
2. **Первичная загрузка при старте экрана:**
   - При наличии сохранённого кэша в `DataStore` UI сразу стартует с последними успешными курсами (`rateCnyKzt`, `rateUsdKzt`, `date`) и параллельно запрашивает свежие данные с сервера.
   - Статусная метка в заголовке карточки курсов:
     - во время запроса: `"загрузка…"`
     - при успехе: `"на ${rates.date}"` (например, `"на 12.11.2026"`)
     - при ошибке сети / невалидном JSON / HTTP ≠ 2xx и отсутствии кэша: `"нет связи с НБ РК"`
     - при ошибке сети при наличии кэша: `"кэш от ${cached.date} · нет связи с НБ РК"`
3. **Поля курсов в UI:**
   - Отображаются с двумя знаками после запятой (`66,68 ₸` / `444,76 ₸`, форматирование `%.2f`).
   - Являются нередактируемыми (`readOnly = true`), так как отражают официальный курс НБ РК.
4. **Повторное обновление:**
   - Жест **pull-to-refresh** в главном списке через `RefreshPuck` в `SulTopBar` (связанный с `IosOverscrollEffect.rawPullY`).
   - Отдельная компактная иконочная кнопка обновления в заголовке блока курсов (с `contentDescription = "Обновить курсы НБ РК"`).

### 2.3. Математическая модель и формулы расчёта

Все вычисления выполняются в `Double` (либо `BigDecimal` на этапе финального округления, с обязательной защитой от деления на ноль и нечисловых строк ввода — запятая `,` и точка `.` одинаково принимаются при парсинге локального ввода).

#### Промежуточные величины
$$\text{totalYuan} = \text{itemYuan} \times \text{itemQty}$$
$$\text{totalWeightKg} = \text{itemWeightKg} \times \text{itemQty}$$
$$\text{baseKzt} = \text{totalYuan} \times \text{rateCnyKzt}$$

#### Ветка 1: Казпочта (Прямая / Официальная)
$$\text{kazpostVatAmtKzt} = \text{baseKzt} \times \frac{\text{kazpostVatPct}}{100}$$
$$\text{kazpostFeeAmtKzt} = \text{baseKzt} \times \frac{\text{kazpostFeePct}}{100}$$
$$\text{kazpostTotalKzt} = \text{baseKzt} + \text{kazpostVatAmtKzt} + \text{kazpostFeeAmtKzt}$$

- Доставка в отделение: фиксированно `0` с подписью `"0 (Бесплатно)"`.
- Срок доставки: `"$kazpostDays дней"`.
- Вторичный эквивалент в юанях (`kazpostTotalYuan`):
  $$\text{ratio} = \begin{cases} \dfrac{\text{kazpostTotalKzt}}{\text{baseKzt}}, & \text{если } \text{baseKzt} > 0 \\ 1, & \text{иначе} \end{cases}$$
  $$\text{kazpostTotalYuan} = \text{totalYuan} \times \text{ratio}$$

#### Ветка 2: Карго Компания (Посредник)
$$\text{cargoShippingUsd} = \text{totalWeightKg} \times \text{cargoRateUsdPerKg}$$
$$\text{cargoShippingKzt} = \text{cargoShippingUsd} \times \text{rateUsdKzt}$$
$$\text{cargoTotalKzt} = \text{baseKzt} + \text{cargoShippingKzt}$$

- Налоги / Таможня: фиксированно `0` с подписью `"0 (Включено)"`.
- Срок доставки: `"$cargoDays дней"`.
- Вторичный эквивалент в юанях (`cargoTotalYuan`):
  - Базовая модель (прямое соответствие стоимости товара): `totalYuan` (при цене `180 ¥` показывает `≈ 180 ¥`).
  - *Примечание для этапа доработки (опционально по согласованию):* при желании показывать полный эквивалент с учётом доставки используется `if (rateCnyKzt > 0) cargoTotalKzt / rateCnyKzt else 0.0`. По умолчанию в ТЗ закладывается точный паритет расчёта с базовой моделью (`totalYuan`).

### 2.4. Форматирование итогов и валют

| Тип значения | Валюта `KZT` | Валюта `USD` |
|---|---|---|
| **Основная сумма** | `Math.round(kztValue)` с группировкой тысяч (`Locale("ru", "RU")`) + `" ₸"`, например `14 283 ₸` | `val usd = if (rateUsdKzt > 0) kztValue / rateUsdKzt else 0.0`, формат `"$ %.2f"` (`Locale.US`), например `$ 178.41` |
| **Вторичный эквивалент** | `"≈ ${groupedRound(yuanValue)} ¥"` | `"≈ ${groupedRound(yuanValue)} ¥"` |
| **Процент выгоды** | `Locale.US`, 1 знак после запятой (`"%.1f"`), например `2.9%`, `50.4%` | `Locale.US`, 1 знак после запятой (`"%.1f"`), например `13.6%` |

### 2.5. Анализ победителя (`ComparisonOutcome`)

Сравнение строится строго по `kazpostTotalKzt` и `cargoTotalKzt`:

1. **`totalYuan <= 0.0` (Цена не указана):**
   - Тег: `"Анализ вариантов"`
   - Заголовок: `"Укажите стоимость товара"`
   - Подзаголовок: `"Введите цену в юанях для начала расчета"`
   - Бейдж экономии: `formatCurrency(0.0)` (`"0 ₸"` или `"$ 0.00"`)
   - Процент: `"0% экономии"`
   - Карточки сравнения: обе в нейтральном состоянии (`alpha = 1f`, стандартная кромка).

2. **`kazpostTotalKzt < cargoTotalKzt` (Выгоднее Казпочта):**
   - $\text{diffKzt} = \text{cargoTotalKzt} - \text{kazpostTotalKzt}$
   - $\text{pct} = \dfrac{\text{diffKzt}}{\text{cargoTotalKzt}} \times 100$
   - Тег: `"Оптимальный выбор"`
   - Заголовок: `"Выгоднее через Казпочту!"`
   - Подзаголовок: `"Официальная доставка с учетом НДС и комиссии дешевле"`
   - Бейдж экономии: `"Экономия ${formatCurrency(diffKzt)}"` (акцент `kazpostAccent`)
   - Процент: `"на ${formatPct(pct)}% дешевле Карго"`
   - Карточки сравнения: карточка **Казпочты** выделена акцентом `kazpostAccent`, карточка **Карго** визуально приглушена (`alpha = 0.70f`).

3. **`cargoTotalKzt < kazpostTotalKzt` (Выгоднее Карго):**
   - $\text{diffKzt} = \text{kazpostTotalKzt} - \text{cargoTotalKzt}$
   - $\text{pct} = \dfrac{\text{diffKzt}}{\text{kazpostTotalKzt}} \times 100$
   - Тег: `"Оптимальный выбор"`
   - Заголовок: `"Выгоднее через Карго!"`
   - Подзаголовок: `"Доставка через посредника с учетом веса выходит выгоднее"`
   - Бейдж экономии: `"Экономия ${formatCurrency(diffKzt)}"` (акцент `cargoAccent`)
   - Процент: `"на ${formatPct(pct)}% дешевле Казпочты"`
   - Карточки сравнения: карточка **Карго** выделена акцентом `cargoAccent`, карточка **Казпочты** визуально приглушена (`alpha = 0.70f`).

4. **`kazpostTotalKzt == cargoTotalKzt` при `totalYuan > 0.0` (Равные условия):**
   - Тег: `"Равные условия"`
   - Заголовок: `"Стоимость одинакова!"`
   - Подзаголовок: `"Оба варианта выходят в абсолютно равную сумму"`
   - Бейдж экономии: `formatCurrency(0.0)`
   - Процент: `"0% разница"`
   - Карточки сравнения: обе в нейтральном состоянии (`alpha = 1f`).

### 2.6. Блок подсказок выбора («Как быстро понять, что выбрать?»)

- **Карточка-подсказка 1 — «Тяжелые и недорогие товары»** (акцент иконки `kazpostAccent`):  
  `«Одежда, обувь, постельное белье. Налог Казпочты зависит от цены товара, поэтому везти тяжёлое напрямую часто дешевле.»` — слово **«дешевле»** выделяется полужирным (`FontWeight.SemiBold`) через `buildAnnotatedString` (сырые символы Markdown `**` не отображаются).
- **Карточка-подсказка 2 — «Легкие и дорогие товары»** (акцент иконки `cargoAccent`):  
  `«Часы, электроника, гаджеты. Карго берет только за вес ($/кг). Для легких ценных вещей Карго почти всегда выгоднее.»`

---

## 3. Архитектура интерфейса и соответствие `suldev-guidelines`

### 3.1. Иерархия корневых слоёв (`Material Fluid` — два источника)

Категорически запрещено использовать один `materialFluidSource` одновременно для контента и для карточек внутри него (иначе цикл `RenderNode` и падение `SIGSEGV` в `RenderNode::prepareTreeImpl`), а также запрещено вкладывать стекло в стекло (за исключением экспортного слоя `SulGlassTabBar`, где пилюля-капля сиблингом читает `exportedLayer`).

Корневая композиция экрана (`MainActivity` / `PddAppRoot`):

```text
Box(Modifier.fillMaxSize().materialFluidPointer(glassState))
 ├── [1] Источник A (inlineGlassState): Box(Modifier.matchParentSize().materialFluidSource(inlineGlassState))
 │        └── AppBackground(backgroundStyle, customImagePath, dimOpacity)   // только фон, без контента
 ├── [2] Источник B (glassState): Box(Modifier.matchParentSize().materialFluidSource(glassState))
 │        ├── AppBackground(...)                                          // фон под контентом
 │        └── AnimatedContent(activeTab) { CalculatorScreen / SettingsScreen }
 │             └── LazyColumn (iosOverscroll + rememberMaterialFluidScrollConnection)
 │                  └── Карточки контента (сэмплят LocalInlineGlassState = Источник A)
 ├── [3] FloatingBarsTapGuard(totalHeight = 64.dp + navBottom + 10.dp, margin = 16.dp) // перехват промахов
 ├── [4] SulTopBar(...)                   // сиблинг Источника B, сэмплит LocalGlassState
 ├── [5] SulGlassTabBar(...)              // сиблинг Источника B, exportBackdrop = true
 ├── [6] GlassSnackbarHost(...)           // над таб-баром (96.dp + navBottom)
 └── [7] SheetPortal / SwipeDismissSheet  // последним сиблингом НАД хромом + IOSAlertDialog (deep = true)
```

**Ключевые инварианты слоёв:**
- В проекте **не используется** библиотека Haze; весь стеклянный рендеринг строится только на `Material Fluid`.
- **Никакого `BottomFadeScrim` поверх источника под `SulGlassTabBar`:** тёмный градиент в слое-источнике преломляется пластиной бара и делает стекло мутным. Читаемость обеспечивается параметром `contentScrim` в стиле стекла и зоной `FloatingBarsTapGuard` (`16.dp`).
- `LocalGlassState` и `LocalInlineGlassState` объявляются через `compositionLocalOf<MaterialFluidState?> { null }`. При значении `null` (превью, тесты) компоненты корректно рисуют плоский фолбэк (`clip + background(glassDeep) + border`).
- На `Android 12+` стандартный системный `StretchOverscrollEffect` отключается на списке, уступая место `rememberIosOverscrollEffect()` + `rememberIosOverscrollConnection()` из `:touch-overscroll`, чтобы `positionInWindow()` карточек и буфер шейдера были синхронизированы до пикселя.

### 3.2. Единое стекло и спецификация `Material Fluid`

1. **Глобальный материал:** `MaterialFluidMaterial.Liquid` (по умолчанию) или `MaterialFluidMaterial.Frosted` хранится в `DataStore` и применяется **сразу к обоим состояниям** (`glassState` и `inlineGlassState`). На отдельных компонентах `style.material` **никогда не переопределяется**.
2. **Обязательное заполнение обоих наборов параметров стиля:** в каждом `MaterialFluidStyle` заполняются и поля `Frosted` (`refraction`, `edgeWidth`, `blur`, `aberration`), и `liquid = MaterialFluidRefraction(...)`, чтобы переключение `Liquid ↔ Frosted` в настройках не схлопывало эффект в плоскую плашку.
3. **Запрет теней и совпадение радиусов:**
   - Ни одна стеклянная карточка, панель бара или снекбар **не отбрасывает `Modifier.shadow`**.
   - `style.cornerRadius` всегда строго равен радиусу `Modifier.clip(RoundedCornerShape(r))` и `Modifier.border(..., RoundedCornerShape(r))`.
   - Кромка стекла: `1.dp`, строго вертикальный градиент `Brush.verticalGradient(listOf(c.glassHighlight, c.glassBorder))`.

#### Таблица профилей стекла в приложении

| Элемент | Источник | Стиль и ключевые значения | Радиус |
|---|---|---|---|
| **Пластина таб-бара (`SulGlassTabBar` / `BarPill`)** | `glassState` | **Канон `SulBarGlass` / `food-track`:** `exportBackdrop = true`, `refractExport = false`, `backdropScale = 1.0f`, `lens = 12.dp`, `bezel = 14.dp`, `magnify = 1.05f`, `liquid.blur = 0.8.dp` (`Frosted blur = 24.dp`), `rim = 0.40f`, `specular = 0.85f`, `pointerResponse = 0.70f`, `flowResponse = 0.45f`, `fog = 1.dp`, `adaptToBackdrop = 0.8f`, `dispersion = 0.24f`, `tintStrength = 0.45f`, `saturation = 1.30f`, `contrast = 1.05f`, `lightSpin = 5f`, `tint = c.liquidTint`. **Запрещено** обнулять `refraction`/`bezel`/`rim`/`specular` или ставить `liquid.blur = 24.dp` в режиме `Liquid`. | `32.dp` |
| **Капсула выбора таб-бара (`Capsule`)** | `glassState` (через `exportedLayer`) | В покое — лёгкий `FlatSelectionPill`. В движении / удержании — `GlassSelectionPill` (`refractExport = true`, `lens = 14.dp`, `bezel = 14.dp`, `magnify = 1.08f`, `liquid.blur = 0.dp`, `rim = 0.50f`, `specular = 1.0f`, `pointerResponse = 0.85f`). Собственный frame (`offset` + `size`). | `100.dp` (капсула) |
| **Верхний бар (`SulTopBar` / `TopBarPanel`)** | `glassState` | Высота `52.dp` под `statusBarsPadding()`, `refractExport = false`, `lens = 8.dp`, `bezel = 10.dp`, `magnify = 1.03f`, `liquid.blur = 0.8.dp`, action-pill в роли `Capsule` с `readsExport = false`. Hairline `0.5.dp` (`outline` alpha `0.6`). | `24.dp` (панель), `CircleShape` / `50%` (действия) |
| **Стеклянный снекбар (`GlassSnackbar`)** | `glassState` | `MaterialFluidStyle.Default.copy(cornerRadius = 18.dp, blur = 24.dp, refractExport = false, ...)` — тёмное стекло в обеих темах, текст `14/19` (`#F2F2F7` / токен в `ui/components/`). | `18.dp` |
| **Карточки контента (`GlassCard`)** | `inlineGlassState` | `MaterialFluidStyle.Default.copy(cornerRadius = 22.dp, refraction = 6.dp, edgeWidth = 10.dp, blur = 18.dp, tint = c.liquidTint, refractExport = false, liquid = MaterialFluidRefraction.Default)` | `22.dp` |
| **Вложенные акцентные блоки и подсказки** | **БЕЗ стекла** | Внутри `GlassCard` (например, подблоки «Казпочта», «Карго», «Тяжелые / Легкие товары») стекло **не вкладывается** — используются плоские тонированные контейнеры с радиусом `16.dp` и тонкой кромкой `0.5.dp`. | `16.dp` |
| **Диалоги (`IOSAlertDialog`)** | **БЕЗ стекла** | `deep = true`: плотная плашка (`surfaceStrong` + `glassDeep`), `1.dp` градиентная кромка, хайрлайны `0.5.dp`. | `22.dp` |
| **Шторки (`SwipeDismissSheet`)** | **БЕЗ стекла внутри источника** | Рисуется над хромом в портальном слое; плотная плашка `surfaceStrong` + `glassDeep` (или отдельный безопасный фон с кромкой `1.dp`), радиус верхних углов `24.dp`. | `24.dp` |

### 3.3. Деградация, доступность стекла и автоконтраст

- **API < 33 (`!MaterialFluidCapabilities.isShaderSupported`) или `reduceTransparency == true`:**
  - Рисуется честный плоский фолбэк: `clip(shape) + background(c.glassDeep) + border(1.dp, verticalGradient)` (при `reduceTransparency = true` — плотная непрозрачная подложка `surfaceStrong` / `backdropColor`).
  - В разделе настроек выводится явное информационное сообщение: *«Шейдерное стекло доступно на Android 13+, на этом устройстве используется упрощённая заливка»* (при `API < 33`).
- **`increaseContrast`:**
  - Читается системный флаг `Settings.Secure.getInt(contentResolver, "high_text_contrast_enabled", 0) == 1` в `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` и передаётся в `glassState.increaseContrast` и `inlineGlassState.increaseContrast`.
- **Замер яркости подложки (`sampleLuminance`):**
  - При включении фона `MESH` или `IMAGE` активируется `glassState.sampleLuminance = true`.
  - Контраст иконок/подписей плавающего хрома и цвет иконок статус-бара (`WindowInsetsControllerCompat.isAppearanceLightStatusBars`) адаптируются по фактической яркости подложки (`backdropIsDark`), а пока замер не пришёл (`!hasBackdropLuminance`) — по текущей теме приложения.

---

## 4. Фон приложения, цветовые токены и типографика

### 4.1. Однослойный фон (`AppBackground`)

Фон рисуется **единожды** на уровне `AppBackground`, без собственных градиентов внутри карточек. Поддерживаются три режима (`BackgroundStyle { PLAIN, MESH, IMAGE }`):

1. **`PLAIN` (строгий дефолт при первом запуске):**
   - Сплошной цвет темы (`PddTheme.colors.background`).
   - В этом режиме стекло честно рендерится аккуратной полупрозрачной плашкой с кромкой.
2. **`MESH` (только по явному выбору пользователя в Настройках):**
   - `Canvas` на весь экран с базовым цветом и 4 статичными радиальными пятнами по углам (без анимации и без замены на `Modifier.background(Brush.radialGradient)`).
   - Цветовые центры (согласованы с акцентами и не дают грязного смешения под таб-баром):
     - Пятно 1 `(15%, 10%, R = 0.90 × min)`: Light `#BFF0CC` (85%) / Dark `#0B3D24` (75%) — спокойный верхний левый угол под Large Title и статус-баром.
     - Пятно 2 `(95%, 28%, R = 1.05 × min)`: Light `#CDE7FF` (85%) / Dark `#0A2647` (65%).
     - Пятно 3 `(25%, 85%, R = 0.95 × min)`: Light `#E7D8FF` (80%) / Dark `#3A1350` (55%).
     - Пятно 4 `(85%, 92%, R = 1.00 × min)`: Light `#CFF6EF` (80%) / Dark `#123B3F` (55%).
3. **`IMAGE` (пользовательское фото через `PickVisualMedia`):**
   - Чтение EXIF-ориентации и поворот `Bitmap`.
   - Даунсэмплинг через `inSampleSize` до максимальной стороны `≤ 2048 px`.
   - Сохранение JPEG `q = 88` в `filesDir/backgrounds/custom_bg_<timestamp>.jpg` под `Mutex`, **удаление предыдущего файла**, хранение в `DataStore` **только имени файла**.
   - Рендер `ContentScale.Crop` (bitmap декодируется и кешируется через `remember(customBackgroundFileName)`, не на каждом кадре) + чёрная вуаль `dimOpacity` (дефолт `0.40f`, диапазон `0.15f..0.75f` по пресетам `15% / 30% / 45% / 60% / 75%` или слайдером в `0f..0.9f`).
   - Ошибка декодирования оборачивается в `runCatching` с безопасным откатом к `PLAIN`.

### 4.2. Цветовая система (`PddColors` + `MaterialTheme.colorScheme`)

**Абсолютное правило:** в файлах экранов **запрещено** использовать `Color(0xFF...)`. Все цвета берутся строго из `PddTheme.colors` или `MaterialTheme.colorScheme`. Исключения допускаются только в `ui/theme/` и в `AppBackground` / `GlassSnackbar` (`ui/components/`).

| Токен `PddColors` | Light | Dark | Назначение |
|---|---|---|---|
| `background` | `#F2F2F7` | `#000000` | Основной фон (`PLAIN`) |
| `surface` | `#FFFFFF` | `#1C1C1E` | Поверхность пилюли сегмента / плотных блоков |
| `surfaceStrong` | `#FFFFFF` | `#2C2C2E` | `IOSAlertDialog`, шторки, фолбэк при `reduceTransparency` |
| `label` | `#000000` | `#FFFFFF` | Основной текст |
| `secondaryLabel` | `#993C3C43` (60%) | `#99EBEBF5` (60%) | Вторичный текст, подписи невыбранных вкладок, секции |
| `tertiaryLabel` | `#4D3C3C43` (30%) | `#4DEBEBF5` (30%) | Placeholder полей ввода (не для текста `< 13.sp`) |
| `separator` | `#333C3C43` (20%) | `#33EBEBF5` (20%) | Хайрлайны `0.5.dp`, фон полей ввода (`alpha = 0.22f`) |
| `tint` / `onTint` | `#E02E24` / `#FFFFFF` | `#FF453A` / `#FFFFFF` | Главный акцент приложения (бренд Pinduoduo), курсор, активные кнопки |
| `kazpost` / `kazpostContainer` | `#0048B3` / `#140048B3` | `#0A84FF` / `#1F0A84FF` | Акцент, обводка победителя и бейджи Казпочты |
| `cargo` / `cargoContainer` | `#B45309` / `#14D97706` | `#FF9F0A` / `#1FFF9F0A` | Акцент, обводка победителя и бейджи Карго (тёмный янтарь в Light для WCAG ≥ 4.5:1) |
| `positive` | `#047857` | `#30D158` | Бесплатная доставка / включённые налоги (`0 (Бесплатно)`, `0 (Включено)`) |
| `danger` | `#FF3B30` | `#FF453A` | Ошибки валидации и деструктивные действия |
| `warn` | `#CC9000` | `#FFD60A` | Предупреждения (затемнён в Light для контраста) |
| `glass` | `#99FFFFFF` (60%) | `#2EEBEBF5` (18%) | Фолбэк-заливка карточек на `API < 33` |
| `glassDeep` | `#C8FFFFFF` | `#661C1C1E` | Плотный фолбэк хрома, алертов и диалогов |
| `glassHighlight` | `#B3FFFFFF` | `#40FFFFFF` | Верх кромки стекла (`1.dp`) |
| `glassBorder` | `#66FFFFFF` | `#20FFFFFF` | Низ кромки стекла (`1.dp`) |
| `liquidTint` | `#40FFFFFF` (25%) | `#401C1C1E` (25%) | Тинт AGSL-шейдера `Material Fluid` |

**Темы:** `ThemeMode { SYSTEM, LIGHT, DARK }` (по умолчанию `SYSTEM`). Проверка контраста WCAG `≥ 4.5:1` для мелкого текста и `≥ 3:1` для крупного обязательна в обеих темах на всех 3 типах фона.

### 4.3. Типографика (iOS-шкала в слотах `MaterialTheme.typography`)

В экранах запрещены произвольные `fontSize = ...sp` — используются только слоты `MaterialTheme.typography`:

| Роль iOS | Размер / интерлиньяж | Начертание | Трекинг | Слот `MaterialTheme.typography` | Применение в приложении |
|---|---|---|---|---|---|
| **Large Title** | `34 / 40` | Bold | `−0.4` | `displayLarge` | Заголовок экрана в теле списка («Pinduoduo KZ Calc», «Настройки») |
| **Title 1** | `28 / 33` | Bold | `−0.3` | `displayMedium` | Крупные итоговые суммы `Итого к оплате` в карточках сравнения |
| **Title 2** | `22 / 27` | Bold | `−0.25` | `displaySmall`, `headlineLarge` | Заголовок-победитель в Hero-баннере, крупное число в поле цены `¥` |
| **Title 3** | `20 / 24` | SemiBold | `−0.2` | `headlineMedium`, `titleLarge` | Заголовки карточек и модальных шторок |
| **Headline** | `17 / 21` | SemiBold | `−0.1` | `headlineSmall`, `titleMedium` | Центрированный заголовок в `SulTopBar` при скролле, названия «Казпочта» / «Карго» |
| **Body** | `17 / 22` | Normal | `0` | `bodyLarge` | Основной текст в числовых полях ввода и диалогах |
| **Callout** | `16 / 20` | Normal | `0` | `bodyMedium` | Подзаголовки, описания опций настроек |
| **Subheadline** | `15 / 19` | Normal | `0` | `bodySmall` | Строки разбивки стоимости, текст подсказок |
| **Label Large** | `15 / 19` | SemiBold | `0` | `labelLarge` | Капсульные кнопки, бейдж экономии |
| **Title Small** | `15 / 19` | SemiBold | `0` | `titleSmall` | Значения в сегмент-контроле, бейджи сроков |
| **Footnote** | `13 / 17` | Normal | `−0.05` | `labelMedium` | `SectionHeader` (`uppercase`, `letterSpacing = 0.6.sp`), подписи над полями |
| **Caption** | `12 / 15` | Normal | `0` | `labelSmall` | Вторичный эквивалент `≈ 214 ¥`, дата курсов НБ РК, теги «Официальная» / «Посредник» |

---

## 5. Сетка, компоненты и UX-паттерны

### 5.1. Отступы, радиусы и строгий стандарт `16 dp`

| Параметр | Значение | Правило |
|---|---|---|
| **Боковые поля экрана** | `16.dp` | Единообразно для всего контента и баров (`BAR_GUTTER = 16.dp`) |
| **Поля внутри `GlassCard`** | `16.dp` (`14.dp` для компактных мини-плиток) | Единый внутренний отступ |
| **Интервал между карточками** | `14.dp` | `Arrangement.spacedBy(14.dp)` в `LazyColumn` |
| **Отступы всех элементов интеракции и интеграции** | **СТРОГО `16.dp`** (вместо `8.dp`) | Кнопки в ряду, поля ввода в сетке (вес/кол-во, НДС/комиссия/сроки), сегмент-контролы, тумблеры, слоты таб-бара, элементы шторок, `FloatingBarsTapGuard` (`TAP_GUARD_MARGIN = 16.dp`) |
| **Верхний отступ контента** | `sulTopBarContentPadding()` | Статус-бар + `52.dp` + `12.dp` (контент свободно уходит под плавающий бар и статус-бар) |
| **Нижний отступ контента** | `122.dp + navigationBars` | Гарантирует, что последняя карточка и снекбар не перекрываются таб-баром |
| **Минимальная тач-цель** | `48.dp × 48.dp` | Плотные строки и сегменты добирают площадь внутренним `padding` |
| **Хайрлайны** | `0.5.dp`, цвет `separator` | Разделители в карточках разбивки и алертах |
| **Кромка текстового поля** | `0.5.dp`, `separator.copy(alpha = 0.4f)` | Без толстых рамок |

### 5.2. Иконки

Используются исключительно заполненные векторные иконки из `material-icons-extended` (`Icons.Filled.*` / `Icons.AutoMirrored.Filled.*` или `Rounded` без смешивания контурных `Outlined` и залитых `Filled`).
- Декоративные иконки рядом с текстом: `contentDescription = null`.
- Иконочные кнопки без текста: обязательный осмысленный `contentDescription`.
- Размеры: `15.dp` (малая кнопка), `18.dp` (кнопка/бейдж), `20.dp` (кнопка хрома), `25.dp` (иконка вкладки `SulGlassTabBar`).

### 5.3. Навигация и экраны

В приложении предусмотрены **2 вкладки** в нижнем плавающем `SulGlassTabBar` (иконка + подпись, размер шрифта подписи `10.sp`):

1. **«Калькулятор»** (`Icons.Filled.Calculate`)
2. **«Настройки»** (`Icons.Filled.Tune`)

#### Состав экрана 1: «Калькулятор»

Порядок блоков в `LazyColumn` (с `rememberIosOverscrollEffect()` + `iosOverscroll` + `materialFluidScroll`):

1. **Заголовочный блок (Large Title):**
   - `Large Title` (`displayLarge`): `"Pinduoduo KZ Calc"` + подпись `bodySmall` (`secondaryLabel`): `"Сравнение доставки в Казахстан"`.
   - Сразу под ним — `SegmentedControl` выбора валюты отображения: **`₸ KZT`** | **`$ USD`** (высота визуальной капсулы `30.dp`, внешний отступ `3.dp`, радиус `50`, тач-область `48.dp`, `Tick` haptic при смене).
   - *Примечание:* в `SulTopBar` (сверху) при скролле ниже заголовка плавно (`tween(220)`) проявляется центрированное название `"Pinduoduo KZ Calc"`, а справа в плавающей action-пилюле (`Capsule`, `readsExport = false`) располагаются кнопка **Сброс** (`Icons.Filled.RestartAlt`) и опционально кнопка **Формулы** (`Icons.Filled.Info` → открывает шторку).

2. **Hero-баннер «Результат сравнения» (`GlassCard`):**
   - Динамически окрашенный бейдж с иконкой состояния (калькулятор / Казпочта / Карго / весы), тег (`"ОПТИМАЛЬНЫЙ ВЫБОР"` / `"АНАЛИЗ ВАРИАНТОВ"` / `"РАВНЫЕ УСЛОВИЯ"`), заголовок результата, поясняющий подзаголовок.
   - Блок экономии: капсульный бейдж (`"Экономия 412 ₸"` / `"Экономия $ 24.29"` / `"0 ₸"`) и подпись процента (`"на 2.9% дешевле Карго"`).
   - Вместо грубой смены цветов всей рамки — деликатная внутренняя акцентная плашка/индикатор цветом `kazpost` или `cargo` поверх единого `GlassCard(cornerRadius = 22.dp)`.

3. **Карточки сравнения: «Казпочта» и «Карго Компания»:**
   - Адаптивная раскладка: на узких экранах (`width < 600.dp`) — последовательно в колонке с шагом `14.dp`; на планшетах и в альбомной ориентации (`width >= 600.dp`) — две равные колонки с шагом `16.dp`.
   - **Карточка «Казпочта» (`Прямая`):**
     - Шапка: иконка + «Казпочта» + тег «Прямая».
     - Плашка срока доставки: «Срок доставки» → «10–18 дней».
     - Крупный итог (`displayMedium`, цвет `kazpost`): `14 283 ₸` (или `$ 32.11`), под ним `≈ 214 ¥` (`labelSmall`).
     - Разделитель `0.5.dp`, постатейная детализация:
       - `Товар (перевод):` `12 002 ₸`
       - `НДС (16%):` `1 920 ₸`
       - `Комиссия (3%):` `360 ₸`
       - `Доставка в отделение:` `0 (Бесплатно)` (цвет `positive`).
   - **Карточка «Карго Компания» (`Посредник`):**
     - Шапка: иконка + «Карго Компания» + тег «Посредник».
     - Плашка срока доставки: «Срок доставки» → «12–25 дней».
     - Крупный итог (`displayMedium`, цвет `cargo`): `13 870 ₸` (или `$ 31.19`), под ним `≈ 180 ¥` (`labelSmall`).
     - Разделитель `0.5.dp`, постатейная детализация:
       - `Товар (перевод):` `12 002 ₸`
       - `Доставка за вес:` `1 868 ₸`
       - `Налоги / Таможня:` `0 (Включено)` (цвет `positive`).
   - Невыгодный вариант плавно приглушается через `alpha` (`0.70f`, анимация `tween(200)`), выгодный подсвечивается `1.5.dp` акцентным индикатором/бейджем «Выгоднее». Тап по карточке (опционально) открывает шторку подробного расчёта по этой ветке.

4. **Карточка «Параметры посылки» (`Pinduoduo / 拼多多`):**
   - `SectionHeader`: `ПАРАМЕТРЫ ПОСЫЛКИ` + правая метка `PINDUODUO / 拼多多`.
   - Поле «Стоимость товара в Пиндодо (¥ Yuan)»: заполненный iOS-стиль (`14.dp`, фон `separator.copy(alpha = 0.22f)`, кромка `0.5.dp`), префикс `¥`, `KeyboardType.Decimal`.
   - Нижний ряд из 2 полей с промежутком **строго `16.dp`**:
     - «Вес 1 шт (кг)» (`KeyboardType.Decimal`, суффикс `кг`)
     - «Количество (шт)» (`KeyboardType.Number`, суффикс `шт`)
   - Ошибки валидации (например, отрицательное значение или `qty < 1`) выводятся текстом цветом `danger` под соответствующим полем, без красной обводки.

5. **Карточка «Курсы Нац. Банка РК»:**
   - `SectionHeader`: `КУРСЫ НАЦ. БАНКА РК` + правый статус (`на 12.11.2026` / `загрузка…` / `нет связи с НБ РК`) + кнопка обновления.
   - Два read-only поля с шагом **`16.dp`**: «Курс 1 ¥ (Юань)» и «Курс 1 $ (Доллар)» с суффиксом `₸`.

6. **Карточка «Настройка тарифов и сроков»:**
   - Внутренний подблок «Казпочта (Прямая) · Официальная» (тонированный фон `kazpostContainer`, радиус `16.dp`): 3 поля в ряд (или 2+1 на узких экранах < `360.dp`) с отступом **`16.dp`** — `НДС (%)`, `Комиссия (%)`, `Сроки (дней)`.
   - Внутренний подблок «Карго Доставка · Посредник» (тонированный фон `cargoContainer`, радиус `16.dp`): 2 поля с отступом **`16.dp`** — `Тариф ($/кг)`, `Сроки (дней)`.

7. **Карточка «Как быстро понять, что выбрать?»:**
   - Заголовок + две информационные подкарточки (радиус `16.dp`) про тяжёлые недорогие товары (Казпочта) и лёгкие дорогие товары (Карго).

#### Шторка «Детали расчёта и формулы» (`SwipeDismissSheet` в `SheetPortal`)
- Открывается по кнопке информации в `SulTopBar` или по нажатию на карточку варианта доставки.
- Внутри: подробная пошаговая формула расчёта для текущих введённых чисел, точка безубыточности (при какой цене товара в `¥` при текущем весе Казпочта сравнивается с Карго), и кнопка «Закрыть» (капсула с отступами `16.dp`).
- **Обязательное поведение жеста:** свайп вниз закрывает шторку (порог **`120.dp`**, пружина `spring(dampingRatio = 1f, stiffness = 400f)`, если порог не набран — пружинный возврат, если набран — тактильный `reject()` / `Error` и закрытие). Внутренний скролл шторки использует `IosOverscrollEffect`, свайп вверх не перехватывается шторкой. Системная кнопка «Назад» перехватывается через `BackHandler`.

#### Диалог подтверждения сброса (`IOSAlertDialog`)
- Вызывается по кнопке сброса в `SulTopBar`: плотная плашка `deep = true`, `cornerRadius = 22.dp` (без стекла и без крестика `X`).
- Заголовок: `"Сбросить настройки?"`, описание: `"Параметры посылки, тарифы и сроки вернутся к исходным значениям. Курсы НБ РК сохранятся."`
- Две текстовые кнопки высотой `46.dp` через хайрлайн `0.5.dp`: слева `"Отмена"` (`secondaryLabel`), справа `"Сбросить"` (`danger`, `SemiBold`).

#### Состав экрана 2: «Настройки»

1. `Large Title`: `"Настройки"`.
2. **Блок «Внешний вид» (`GlassCard`):**
   - Тема оформления (`SegmentedControl`): `Система` | `Светлая` | `Тёмная` (`ThemeMode`).
   - Материал стекла (`SegmentedControl`): `Жидкое` | `Матовое` (`MaterialFluidMaterial`).
   - При `!MaterialFluidCapabilities.isShaderSupported` (`API < 33`): информационная строка `labelMedium` (`secondaryLabel`): *«Шейдерное стекло доступно на Android 13+, на этом устройстве используется упрощённая заливка»*.
   - Фон экрана (`SegmentedControl`): `Однотон` | `Меш` | `Фото` (`BackgroundStyle`).
   - При выборе `Фото`: кнопки-капсулы (`Arrangement.spacedBy(16.dp)`) «Выбрать фото» (`PickVisualMedia`) и «Удалить фото» (если выбрано), плюс выбор затемнения (`SegmentedControl` или ряд с отступом `16.dp`: `15%`, `30%`, `45%`, `60%`, `75%`, дефолт `40%` на слайдере).
3. **Блок «Доступность» (`GlassCard`):**
   - Тумблер `Уменьшить прозрачность` (`reduceTransparency`, с `TOGGLE_ON` / `TOGGLE_OFF` тактильностью).
   - Информационные строки состояния системных флагов (read-only): `Повышенный контраст` (`high_text_contrast_enabled`) и `Уменьшение движения` (`animator_duration_scale == 0`).
4. **Блок «О данных и расчётах» (`GlassCard`):**
   - Описание источника курсов («Национальный Банк РК»), время/дата последнего успешного обновления, кнопка «Обновить курсы сейчас» (`Confirm`).

---

## 6. Движение и тактильная отдача

### 6.1. Спецификация анимаций (`motion.md`)

Все переходы работают без игрушечного отскока (`dampingRatio = 1f`), кроме жестов инерции/линзы.

| Элемент | Спека |
|---|---|
| **Переключение вкладок (`AnimatedContent`)** | `slideInHorizontally(spring(1f, 380f)) { if (forward) it / 14 else -it / 14 } + fadeIn(tween(180)) + scaleIn(0.985f, spring(1f, 380f))` ↔ `fadeOut(tween(120)) + scaleOut(0.99f, spring(1f, 380f))` |
| **Пилюля `SulGlassTabBar`** | Константы `SulPillMotion` (`travel 280/31.4`, `lift 250/19`, `250/22.1`, `1000/63.3`, `PillSquashModel` `300 ms`, `±12%`) |
| **Пилюля `SegmentedControl`** | `spring(dampingRatio = 1f, stiffness = 500f)` |
| **Шторка `SwipeDismissSheet`** | `spring(dampingRatio = 1f, stiffness = 400f)` |
| **Заголовок `SulTopBar` при скролле** | `animateFloatAsState(tween(220))` |
| **Смена цветов и `alpha` карточек** | `tween(200)` |

**Запреты анимаций:**
- Не анимировать положение/размер самого шейдерного стекла по кадрам (двигается непрозрачная пилюля или `SulPillMotion` капли в пределах бара).
- Не анимировать пятна `MESH`-фона.
- Не добавлять `animateItem` при начальной загрузке списка.
- **Reduced Motion:** при `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` отключать слайды/масштаб и передавать `reduceMotion = true` в `SulGlassTabBar`.

### 6.2. Тактильный отклик (`HapticDevice` / `View.performHapticFeedback`)

Используется `HapticDevice` из `:haptic-engine` (с уважением системного `HAPTIC_FEEDBACK_ENABLED` через `USAGE_TOUCH` и предсборкой эффектов) либо `View.performHapticFeedback`. Запрещено создавать собственный тумблер отключения вибрации в приложении и запрещено вызывать вибрацию дважды (и в компоненте, и в обработчике `onClick`).

| Событие | Паттерн | Условие |
|---|---|---|
| Переключение вкладки таб-бара | `HapticPattern.Click` (`CLOCK_TICK` / `4`) | Встроено в `SulGlassTabBar`; молчит при повторном тапе на активную вкладку |
| Переключение `SegmentedControl` (валюта, тема, материал, фон) | `HapticPattern.Tick` (`CLOCK_TICK`) | Только при фактической смене индекса |
| Нажатие кнопки действия (обновить курсы, сохранить/выбрать фото) | `Confirm` (`CONFIRM` API 30+ / `VIRTUAL_KEY`) | Однократно при клике |
| Переключатель `reduceTransparency` | `TOGGLE_ON` / `TOGGLE_OFF` | Только при смене значения |
| Подтверждение сброса / закрытие шторки свайпом | `HapticPattern.Error` / `REJECT` (`17`) | Только после прохождения порога `120.dp` или нажатия «Сбросить» |
| Ввод символов, ошибки валидации, скролл | **Тишина** | Не вибрировать на набор цифр и валидацию |

---

## 7. Структура проекта (пакеты и классы)

```text
app/src/main/java/com/suldev/pddkz/
├── MainActivity.kt                      // enableEdgeToEdge, инициализация состояний, корень PddAppRoot
├── domain/
│   ├── model/
│   │   ├── CalculatorInput.kt           // входные поля, Currency (KZT/USD), пресеты по умолчанию
│   │   ├── NbkRates.kt                  // курс CNY, USD, дата, источник, статус загрузки
│   │   ├── CalculationResult.kt         // итоги Казпочты, Карго, ComparisonOutcome (4 состояния)
│   │   └── AppSettings.kt               // ThemeMode, BackgroundStyle, MaterialFluidMaterial, reduceTransparency, dimOpacity
│   └── usecase/
│       └── CalculateDeliveryUseCase.kt  // чистая детерминированная математика + расчёт точки безубыточности
├── data/
│   ├── network/
│   │   └── NbkRatesApi.kt               // GET https://pdd-kz-calc.netlify.app/api/rates
│   ├── repository/
│   │   └── RatesRepository.kt           // загрузка + оффлайн-кэш + фолбэк 68.11 / 453.71
│   └── storage/
│       ├── PreferencesStorage.kt        // DataStore (настройки, кэш курсов, имя файла фона)
│       └── BackgroundImageStorage.kt    // EXIF-поворот, downsample <= 2048px, JPEG q=88, атомарная замена под Mutex
├── ui/
│   ├── theme/
│   │   └── Theme.kt                     // PddColors (light/dark), IOSTypography, LocalGlassState, LocalInlineGlassState, PddTheme
│   ├── components/
│   │   ├── AppBackground.kt             // PLAIN + 4-точечный Canvas MESH + IMAGE с вуалью
│   │   ├── GlassCard.kt                 // контейнер Material Fluid (inlineGlassState) + плоский фолбэк + deep-режим
│   │   ├── IosControls.kt               // SegmentedControl, IosNumberField, IosButton, SectionHeader, IOSAlertDialog, GlassSnackbar
│   │   ├── SwipeDismissSheet.kt         // SheetPortal + жест вниз (120 dp) + BackHandler
│   │   └── ComparisonComponents.kt      // WinnerBanner, DeliveryMethodCard, HintSection
│   ├── screens/
│   │   ├── CalculatorScreen.kt          // главный экран калькулятора
│   │   ├── SettingsScreen.kt            // экран настроек темы, стекла, фона и доступности
│   │   └── FormulaDetailsSheet.kt       // шторка детальной разбивки и точки безубыточности
│   └── viewmodel/
│       └── PddCalculatorViewModel.kt    // StateFlow UI-состояния, валидация, дебаунс-ненужен (мгновенный расчёт)
└── app/src/test/java/com/suldev/pddkz/
    ├── CalculateDeliveryUseCaseTest.kt  // unit-тесты всех формул, округлений, USD/KZT и 4 исходов
    └── NbkRatesParsingTest.kt           // unit-тесты парсинга JSON и фолбэков
```

---

## 8. Чек-лист приёмки и верификации

Перед сдачей каждого экрана и релиза обязательна проверка по следующему регламенту:

### 8.1. Автоматические проверки и сборка
- [ ] `./gradlew test` — 100% зелёные unit-тесты доменных формул (`180 ¥ / 1.2 кг / 1 шт → Карго выгоднее`, `50 ¥ / 3.0 кг / 1 шт → Казпочта выгоднее`, `0 ¥ → нейтральный баннер`, равенство сумм, переключение `KZT ↔ USD`, парсинг `api/rates` и оффлайн-фолбэк).
- [ ] `./gradlew assembleDebug` — чистая сборка без ошибок и предупреждений о неиспользуемых импортах.
- [ ] В директории `ui/screens/` нет ни одного `Color(0xFF...)`, `TODO` и произвольных `fontSize = ...sp`.

### 8.2. Проверка стекла и фона (API 33+ и API < 33)
- [ ] Два источника (`inlineGlassState` на `AppBackground`, `glassState` на `AppBackground + Content`); источник никогда не является предком стекла; стекло не вкладывается в стекло.
- [ ] `SulGlassTabBar` и `SulTopBar` используют единый `SulBarGlass` без обнуления линзы (`lens 12dp`, `bezel 14dp`, `magnify 1.05`, `liquidBlur 0.8dp`) и меняют материал синхронно при переключении `Жидкое ↔ Матовое`.
- [ ] `cornerRadius` каждого стекла совпадает с радиусом `clip` и `border`; у стеклянных карточек и бара нет `shadow`.
- [ ] На устройстве/эмуляторе **API < 33** отображается аккуратная плоская заливка (`glassDeep` + кромка) и честная подсказка в настройках.
- [ ] Проверены комбинации тем (`Light` / `Dark`) × фонов (`Plain` / `Mesh` / `Image 15%` / `Image 75%`); текст, статус-бар и карточки читаются на всём диапазоне яркостей.
- [ ] Смена и удаление пользовательского фото удаляют старый файл из `filesDir/backgrounds/`.

### 8.3. UX, движение, тактильность и доступность
- [ ] Отступы интеракции (между полями в сетках, кнопками, элементами шторок и вокруг плавающих баров в `FloatingBarsTapGuard`) строго **`16.dp`**, тач-цели `≥ 48.dp`.
- [ ] Свайп вниз (`120.dp`) закрывает шторку; `BackHandler` закрывает шторку перед выходом; шторка рендерится поверх хрома в `SheetPortal`.
- [ ] `adb shell settings put global animator_duration_scale 0` — переключение вкладок и расчёты работают корректно без артефактов.
- [ ] `adb shell dumpsys vibrator_manager | grep com.suldev.pddkz` — фиксирует переключение вкладок/сегментов (`CLOCK_TICK`), подтверждение действий (`CONFIRM`), свайп-закрытие (`REJECT`); при повторном тапе на активную вкладку и при системном отключении вибрации (`haptic_feedback_enabled 0`) приложение молчит.
- [ ] Проверена работа `reduceTransparency` и автообновление `increaseContrast` при возврате в приложение (`ON_RESUME`).
