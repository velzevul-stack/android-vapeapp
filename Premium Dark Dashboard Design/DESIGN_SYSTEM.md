# VapeStore Premium Dark Dashboard - Дизайн-система

## 📱 Обзор

Премиальный темный дашборд для Android-приложения VapeStore, выполненный в стиле современных минималистичных финансовых приложений (вдохновлен Cuberto, Ghulam Rasool). Основная идея: тёмный фон с пастельными акцентами, тонкие outline-иконки, мягкие скругления и много воздуха.

---

## 🎨 Цветовая палитра

### Background Colors (Фоновые цвета)

| Название | Hex | CSS Variable | Использование |
|----------|-----|--------------|---------------|
| Primary Background | `#0F1115` | `--dark-bg` | Основной фон приложения |
| Surface | `#151922` | `--dark-surface` | Карточки, поверхности |
| Surface Elevated | `#1B2030` | `--dark-surface-elevated` | Приподнятые элементы |

### Pastel Accent Colors (Пастельные акценты)

Используются для KPI карточек - светлые пастельные цвета с тёмным текстом:

| Название | Hex | CSS Variable | Использование |
|----------|-----|--------------|---------------|
| Mint | `#BFE7E5` | `--pastel-mint` | KPI карточка 1, акценты |
| Light Blue | `#CFE6F2` | `--pastel-blue` | KPI карточка 2 |
| Lavender | `#DED8F6` | `--pastel-lavender` | KPI карточка 3 |
| Soft Pink | `#F2D6DE` | `--pastel-pink` | KPI карточка 4 |

### Text Colors (Цвета текста)

**На пастельных фонах:**
- Primary: `#111111` (`--text-on-pastel`)
- Secondary: `#1A1A1A` (`--text-on-pastel-secondary`)

**На тёмных фонах:**
- Primary: `#F5F5F7` (`--text-dark-primary`)
- Secondary: `#9CA3AF` (`--text-dark-secondary`)
- Tertiary: `#6B7280` (`--text-dark-tertiary`)

---

## 📐 Типографика

### Размеры шрифтов

| Размер | rem | px | CSS Variable | Использование |
|--------|-----|----|--------------|--------------| 
| xs | 0.75 | 12 | `--text-xs` | Подписи, лейблы |
| sm | 0.875 | 14 | `--text-sm` | Второстепенный текст |
| base | 1 | 16 | `--text-base` | Основной текст |
| lg | 1.125 | 18 | `--text-lg` | Подзаголовки |
| xl | 1.25 | 20 | `--text-xl` | Заголовки карточек |
| 2xl | 1.5 | 24 | `--text-2xl` | Крупные заголовки |
| 3xl | 2 | 32 | `--text-3xl` | График: итоговое значение |
| 4xl | 2.5 | 40 | `--text-4xl` | KPI: главное значение |
| 5xl | 3 | 48 | `--text-5xl` | Особо крупные значения |

### Начертание (Font Weights)

| Название | Значение | CSS Variable | Использование |
|----------|----------|--------------|---------------|
| Normal | 400 | `--font-weight-normal` | Обычный текст |
| Medium | 500 | `--font-weight-medium` | Лейблы, кнопки |
| Semibold | 600 | `--font-weight-semibold` | Заголовки, числа |
| Bold | 700 | `--font-weight-bold` | Особые акценты |

### Примеры использования

```css
/* KPI числа */
font-size: 2.5rem; /* 40px */
font-weight: 600;
letter-spacing: -0.02em;
line-height: 1;

/* Заголовок страницы */
font-size: 1.75rem; /* 28px */
font-weight: 600;
letter-spacing: -0.02em;
line-height: 1.2;

/* Заголовок карточки */
font-size: 1rem; /* 16px */
font-weight: 600;
letter-spacing: -0.01em;

/* Лейбл карточки */
font-size: 0.75rem; /* 12px */
font-weight: 500;
letter-spacing: 0.01em;
```

---

## 📏 Отступы (Spacing)

| Размер | Значение | CSS Variable | Использование |
|--------|----------|--------------|---------------|
| xs | 4px | `--spacing-xs` | Микро-отступы |
| sm | 8px | `--spacing-sm` | Малые отступы |
| md | 12px | `--spacing-md` | Средние отступы |
| lg | 16px | `--spacing-lg` | Стандартные отступы |
| xl | 24px | `--spacing-xl` | Большие отступы (padding карточек) |
| 2xl | 32px | `--spacing-2xl` | Очень большие отступы |
| 3xl | 48px | `--spacing-3xl` | Секционные отступы |

---

## ⭕ Скругления (Border Radius)

| Размер | Значение | CSS Variable | Использование |
|--------|----------|--------------|---------------|
| sm | 8px | `--radius-sm` | Малые элементы |
| md | 12px | `--radius-md` | Средние элементы |
| lg | 18px | `--radius-lg` | Большие элементы |
| xl | 24px | `--radius-xl` | **KPI карточки, Chart карточка** |
| 2xl | 28px | `--radius-2xl` | Особо большие элементы |

### Применение в компонентах

- **KPI Cards**: `24px`
- **Chart Card**: `24px`
- **Quick Action Buttons**: `16px`
- **Bottom Nav Pill**: `16px`
- **Nav Container**: top corners `20px`

---

## 🎯 Иконки

### Размеры

| Размер | Значение | CSS Variable | Использование |
|--------|----------|--------------|---------------|
| xs | 16px | `--icon-xs` | Микро-иконки |
| sm | 20px | `--icon-sm` | **KPI карточки** |
| md | 24px | `--icon-md` | **Навигация (Home)**, заголовки |
| lg | 32px | `--icon-lg` | Крупные иконки |

### Стиль иконок

- **Библиотека**: `lucide-react`
- **Тип**: Outline (обводка, без заливки)
- **Stroke Width**: `1.5` (стандарт), `2` (для акцента)
- **Цвета**: Наследуют от текста, либо `#9CA3AF` (secondary), `#BFE7E5` (accent)

### Примеры

```tsx
import { ShoppingBag } from "lucide-react";

<ShoppingBag 
  size={20} 
  strokeWidth={1.5} 
  className="text-[#111111] opacity-40" 
/>
```

---

## 🌑 Тени (Shadows)

| Название | Значение | CSS Variable | Использование |
|----------|----------|--------------|---------------|
| Small | `0 1px 2px rgba(0,0,0,0.05)` | `--shadow-sm` | Лёгкие тени |
| Medium | `0 4px 6px -1px rgba(0,0,0,0.1)` | `--shadow-md` | Средние тени |
| Large | `0 10px 15px -3px rgba(0,0,0,0.1)` | `--shadow-lg` | Крупные тени |
| Card | `0 2px 8px rgba(0,0,0,0.15)` | `--shadow-card` | **Карточки** (основная) |

Тени используются минимально для сохранения минимализма.

---

## ⚡ Анимации

### Длительность

| Размер | Значение | CSS Variable | Использование |
|--------|----------|--------------|---------------|
| Fast | 150ms | `--duration-fast` | Быстрые hover эффекты |
| Normal | 250ms | `--duration-normal` | **Карточки fade/slide** |
| Slow | 350ms | `--duration-slow` | Медленные переходы |

### Easing

```javascript
// Стандартный cubic-bezier
cubic-bezier(0.25, 0.1, 0.25, 1.0)

// Spring (для Motion/Framer Motion)
{
  type: "spring",
  stiffness: 380,
  damping: 30
}
```

### Типы анимаций

1. **Card Entry** (появление карточек):
   ```tsx
   initial={{ opacity: 0, y: 20 }}
   animate={{ opacity: 1, y: 0 }}
   transition={{ duration: 0.25, delay: 0.1 }}
   ```

2. **Chart Bars** (рост столбиков):
   - Duration: `800ms`
   - Анимация высоты от 0 до значения

3. **Nav Indicator** (индикатор навигации):
   ```tsx
   <motion.div layoutId="activeTab" />
   transition={{ type: "spring", stiffness: 380, damping: 30 }}
   ```

4. **Button Press**:
   ```tsx
   whileTap={{ scale: 0.98 }}
   ```

---

## 🧩 Компоненты

### KPI Card

**Описание**: Почти квадратная карточка с пастельным фоном, крупной цифрой и маленькой подписью.

**Характеристики**:
- Padding: `24px`
- Border Radius: `24px`
- Background: Один из 4 пастельных цветов
- Shadow: `0 2px 8px rgba(0,0,0,0.15)`
- Hover: `shadow-lg`
- Press: `scale: 0.98`

**Структура**:
```tsx
<div className="bg-[pastel-color] rounded-[24px] p-6">
  {/* Header: Label + Icon */}
  <div className="flex justify-between">
    <p className="text-xs opacity-70">{label}</p>
    <Icon size={20} strokeWidth={1.5} />
  </div>
  
  {/* Value */}
  <p className="text-4xl font-semibold">{value}</p>
</div>
```

**Цвета фонов**:
- `#BFE7E5` (mint)
- `#CFE6F2` (blue)
- `#DED8F6` (lavender)
- `#F2D6DE` (pink)

**Текст**: `#111111` / `#1A1A1A`

---

### Новые UI компоненты

#### Button
Универсальная кнопка с 4 вариантами стиля:
- **primary**: Пастельный mint фон (#BFE7E5)
- **secondary**: Тёмный фон (#151922)
- **tonal**: Приподнятый фон (#1B2030)
- **outlined**: С обводкой

Размеры: sm, md, lg

#### ScreenHeader
Универсальный заголовок экрана с subtitle и кнопками действий.

#### Stepper
Компонент увеличения/уменьшения количества с кнопками +/-.

#### SearchBar
Поисковая строка с иконкой поиска и кнопкой очистки.

#### StatCard
Карточка статистики с пастельным фоном для отображения метрик.

#### SuccessModal
Модальное окно успешного действия с анимированной иконкой галочки.

---

### Chart Card

**Описание**: Карточка с заголовком, итоговым значением и гистограммой на 7 дней.

**Характеристики**:
- Padding: `24px`
- Border Radius: `24px`
- Background: `#151922`
- Shadow: `0 2px 8px rgba(0,0,0,0.15)`

**Структура**:
```tsx
<div className="bg-[#151922] rounded-[24px] p-6">
  {/* Header */}
  <div className="flex justify-between">
    <h3>Прибыль за неделю</h3>
    <Badge>+12.5%</Badge>
  </div>
  
  {/* Total */}
  <p className="text-3xl">234,000 ₽</p>
  
  {/* Chart */}
  <BarChart>
    <Bar dataKey="value" radius={[8,8,0,0]} fill={pastel} />
  </BarChart>
  
  {/* Legend */}
  <div className="flex gap-4">
    <Legend color="mint">Обычный день</Legend>
    <Legend color="teal">Лучший день</Legend>
  </div>
</div>
```

**Цвета столбцов**:
- Обычные дни: Пастельные (`#BFE7E5`, `#CFE6F2`, `#DED8F6`, `#F2D6DE`)
- Лучший день: `#A5D4D2` (более яркий teal)

**Подписи дней**: 12px, `#6B7280`

---

### Bottom Navigation

**Описание**: Минималистичная нижняя навигация с 5 пунктами и плавным индикатором.

**Характеристики**:
- Background: `#151922`
- Border Top: `rgba(255,255,255,0.08)` 1px
- Safe Area: `padding-bottom: env(safe-area-inset-bottom, 0)`

**Пункты**:
1. Приемка
2. Продажа
3. **Главная** (центральная, более крупная иконка 24px)
4. Кабинет
5. Управление

**Active State**:
- Background pill: `#1B2030`, radius `16px`
- Icon color: `#BFE7E5`
- Text color: `#F5F5F7`
- Icon stroke: `2`

**Inactive State**:
- Icon color: `#9CA3AF`
- Text color: `#6B7280`
- Icon stroke: `1.5`

**Анимация индикатора**:
```tsx
<motion.div layoutId="activeTab" />
transition={{ type: "spring", stiffness: 380, damping: 30 }}
```

**Размеры**:
- Icon: 22px (стандарт), 24px (Home)
- Label: 10px (0.625rem)
- Padding: `py-2 px-3`

---

## 📱 Layout & Spacing

### Mobile Container

- Max Width: `28rem` (`448px`, `max-w-md`)
- Background: `#0F1115`
- Padding X: `20px` (`px-5`)
- Padding Bottom: `96px` (`pb-24`) - для навигации

### Header

- Padding Top: `32px` (`pt-8`)
- Padding Bottom: `24px` (`pb-6`)
- Title: `1.75rem` (28px), weight 600
- Subtitle: `0.875rem` (14px), `#9CA3AF`

### Sections

- Margin Bottom: `24px` (`mb-6`)
- Grid Gap (KPI): `16px` (`gap-4`)

---

## 🎨 States (Состояния)

### Loading State (Скелетоны)

Для KPI карточек и графика используются скелетоны:
- Background: `#151922`
- Animated elements: `#1B2030`
- Animation: `pulse`

### Empty State

- Background: `#151922`
- Icon: Database, 32px, `#9CA3AF`
- Icon container: 64px circle, `#1B2030`
- Text: `#9CA3AF` / `#6B7280`

### Hover State

- Button background: `#151922` → `#1F2537`
- Card shadow: default → `shadow-lg`

### Press State

- Scale: `0.98`
- Transition: `150ms`

---

## 🌐 Адаптивность

### Размеры экранов

Основные варианты:
- **360x800** (стандарт)
- **411x891** (большие экраны)

### Safe Areas

Учитываются для нижней навигации:
```css
padding-bottom: env(safe-area-inset-bottom, 0px);
```

---

## ♿ Доступность (Accessibility)

### Контрастность

Все цветовые пары проверены на соответствие WCAG AA:
- `#111111` на `#BFE7E5` ✅
- `#F5F5F7` на `#151922` ✅
- `#9CA3AF` на `#0F1115` ✅

### Touch Targets

Минимальный размер: `44x44px`

### ARIA Labels

На всех icon-only кнопках:
```tsx
<button aria-label="Настройки">
  <Settings />
</button>
```

### Focus Indicators

```css
button:focus-visible {
  outline: 2px solid var(--pastel-mint);
  outline-offset: 2px;
}
```

---

## 🛠️ Технологии

- **React**: 18.3.1
- **Motion** (Framer Motion): 12.23.24
- **Tailwind CSS**: 4.x
- **Lucide React**: 0.487.0 (иконки)
- **Recharts**: 2.15.2 (графики)

---

## 📦 Файловая структура

```
src/
├── app/
│   ├── App.tsx                      # Главный компонент
│   └── components/
│       ├── KPICard.tsx              # KPI карточка
│       ├── ChartCard.tsx            # Карточка с графиком
│       ├── BottomNav.tsx            # Нижняя навигация
│       ├── KPISkeleton.tsx          # Скелетон для загрузки
│       ├── EmptyState.tsx           # Пустое состояние
│       ├── DesignSystem.tsx         # Документация дизайн-системы
│       └── screens/
│           ├── HomeScreen.tsx       # Главный экран
│           └── PlaceholderScreen.tsx # Заглушка для других экранов
└── styles/
    ├── theme.css                    # Дизайн-токены
    ├── index.css                    # Глобальные стили
    └── tailwind.css                 # Tailwind imports
```

---

## 🎯 Best Practices

1. **Минимализм**: Много воздуха, мало элементов на экране
2. **Контраст**: Тёмный текст на светлых карточках, светлый на тёмном фоне
3. **Консистентность**: Одинаковые отступы, радиусы, анимации
4. **Плавность**: Все переходы анимированы (150-250ms)
5. **Иконки**: Только outline, strokeWidth 1.5-2
6. **Типографика**: Чёткая иерархия размеров и весов
7. **Цвет**: Пастельные акценты, избегать ярких неоновых цветов

---

## 📋 Checklist для новых экранов

- [ ] Используется тёмный фон `#0F1115`
- [ ] Карточки со скруглением `24px`
- [ ] Все иконки outline, lucide-react
- [ ] Анимации fade/slide при появлении
- [ ] Пастельные акценты для KPI
- [ ] Тёмный текст на пастельных фонах
- [ ] Светлый текст на тёмных фонах
- [ ] Padding карточек `24px`
- [ ] Grid gap `16px`
- [ ] Bottom nav padding `pb-24`
- [ ] ARIA labels на icon buttons
- [ ] Focus indicators настроены

---

## 📄 Лицензия

Дизайн-система создана для VapeStore Android приложения.