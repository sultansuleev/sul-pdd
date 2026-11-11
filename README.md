# Pinduoduo KZ Calc

Нативное Android-приложение (**Kotlin · Jetpack Compose · Edge-to-Edge**) для расчёта и наглядного сравнения итоговой стоимости и сроков доставки товаров с маркетплейса **Pinduoduo (拼多多)** в Казахстан двумя способами:

1. **Казпочта (Прямая / Официальная)** — расчёт с учётом НДС и комиссии, без доплаты за вес при получении в отделении.
2. **Карго Компания (Посредник)** — расчёт с учётом тарифа за вес (`$/кг`) и курса доллара, со включёнными налогами и таможней.

Приложение воспроизводит доменную модель, форматы и работу с курсами НБ РК веб-калькулятора [`pdd-kz-calc.netlify.app`](https://pdd-kz-calc.netlify.app/), перенося интерфейс в стек экосистемы **suldev** (`Material Fluid` + `suldev-ui` + `suldev-kit`).

> **Текущий статус:** завершён **этап 0** (каркас проекта, `assembleDebug` проходит). Подробные требования — в [`TZ.md`](TZ.md), поэтапный план — в [`PLAN.md`](PLAN.md).

---

## Возможности (по спецификации)

- **Реактивный расчёт без кнопки «Рассчитать»** — мгновенный пересчёт при изменении цены в `¥`, веса, количества, тарифов или переключении валюты `₸ KZT` ↔ `$ USD`.
- **Анализ победителя и точка безубыточности** — 4 состояния сравнения (`ComparisonOutcome`), выделение выгодного варианта, процент экономии и шторка пошаговых формул на текущих числах.
- **Актуальные курсы НБ РК** — загрузка с `GET https://pdd-kz-calc.netlify.app/api/rates`, локальный кэш в `DataStore` и оффлайн-фолбэк (`68.11 ₸` за `¥`, `453.71 ₸` за `$`).
- **Шейдерное стекло [`Material Fluid`](https://github.com/sultansuleev/material-fluid)** — двухисточниковая архитектура без циклов `RenderNode`, синхронное переключение материалов **Жидкое (`Liquid`) / Матовое (`Frosted`)** на Android 13+ (API 33+) и честный плоский фолбэк на API 26–32.
- **Физика и тактильность [`suldev-kit`](https://github.com/sultansuleev/suldev-kit) + [`suldev-ui`](https://github.com/sultansuleev/suldev-ui)** — iOS-оверскролл (`IosOverscrollEffect`), парящие верхний и нижний бары (`SulTopBar`, `SulGlassTabBar`), защита слепых зон (`FloatingBarsTapGuard`) и аппаратно-адаптивный отклик (`HapticDevice`).
- **Гибкое оформление и доступность** — темы (`Система / Светлая / Тёмная`), 3 режима фона (`Однотон / Меш / Фото` с EXIF-поворотом и слайдером затемнения), поддержка системных `increaseContrast`, `reduceMotion` и тумблера уменьшения прозрачности.

---

## Стек и конфигурация

| Параметр | Значение |
|---|---|
| **Application ID / Namespace** | `com.suldev.pddkz` |
| **SDK** | `minSdk = 26` · `compileSdk = 36` · `targetSdk = 36` |
| **Java / Kotlin** | JVM `17` · Kotlin `2.4.21` |
| **Сборка** | Gradle `8.14.3` · AGP `8.13.2` |
| **UI** | Jetpack Compose (`compose-bom:2024.12.01`), Material 3, Edge-to-Edge |
| **Модули экосистемы** | `com.materialfluid:material-fluid:2.1.0`, `com.suldev.kit:touch-overscroll:1.0.0`, `com.suldev.kit:haptic-engine:1.0.0`, `com.suldev:suldev-ui:1.0.0` |

---

## Структура репозитория

```text
sul-pdd/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/suldev/pddkz/MainActivity.kt
│       └── res/values/themes.xml
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts      # includeBuild для ../material-fluid, ../suldev-kit, ../suldev-ui
├── PLAN.md                  # план разработки по этапам (0–7)
└── TZ.md                    # полное техническое задание
```

---

## Локальная сборка

Проект использует **композитные сборки** (`includeBuild` в [`settings.gradle.kts`](settings.gradle.kts)), поэтому библиотеки экосистемы должны находиться рядом с каталогом `sul-pdd`:

```text
OwnProject/
├── material-fluid/
├── suldev-kit/
├── suldev-ui/
└── sul-pdd/
```

Клонирование зависимостей (при необходимости):

```bash
git clone https://github.com/sultansuleev/material-fluid.git ../material-fluid
git clone https://github.com/sultansuleev/suldev-kit.git ../suldev-kit
git clone https://github.com/sultansuleev/suldev-ui.git ../suldev-ui
```

Сборка debug APK и запуск тестов:

```bash
./gradlew assembleDebug
./gradlew test
```

---

## Этапы разработки

- [x] **0. Каркас** — `git`, `.gitignore`, Gradle wrapper, композитные сборки, пустая `MainActivity` с `enableEdgeToEdge`.
- [ ] **1. Домен** — модели, `CalculateDeliveryUseCase`, форматтеры, unit-тесты.
- [ ] **2. Данные** — `NbkRatesApi`, `PreferencesStorage` (DataStore), `RatesRepository` с оффлайн-кэшем.
- [ ] **3. UI-кит, тема и корень** — доработка `suldev-ui`, `PddColors`, `PddAppRoot` (7 слоёв и два источника `Material Fluid`).
- [ ] **4. Экран «Калькулятор»** — `PddCalculatorViewModel`, поля, сравнение, pull-to-refresh, диалог сброса.
- [ ] **5. Шторка формул** — `SwipeDismissSheet` + `SheetPortal`, пошаговый разбор и точка безубыточности.
- [ ] **6. Экран «Настройки»** — тема, материал стекла, фон (включая `BackgroundImageStorage`), доступность.
- [ ] **7. Полировка и приёмка** — автоконтраст, `reduceMotion`, аудит тактильности и чек-лист [`TZ.md`](TZ.md) §8.
