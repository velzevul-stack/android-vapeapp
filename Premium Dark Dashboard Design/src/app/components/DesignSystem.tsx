/**
 * VapeStore Premium Dark Dashboard - Design System
 * 
 * ЦВЕТОВАЯ ПАЛИТРА
 * ================
 * 
 * Background Colors:
 * - Primary Background: #0F1115 (var(--dark-bg))
 * - Surface: #151922 (var(--dark-surface))
 * - Surface Elevated: #1B2030 (var(--dark-surface-elevated))
 * 
 * Pastel Accent Colors (для KPI карточек):
 * - Mint: #BFE7E5 (var(--pastel-mint))
 * - Light Blue: #CFE6F2 (var(--pastel-blue))
 * - Lavender: #DED8F6 (var(--pastel-lavender))
 * - Soft Pink: #F2D6DE (var(--pastel-pink))
 * 
 * Text Colors:
 * - On Pastel (Dark): #111111 (var(--text-on-pastel))
 * - On Pastel Secondary: #1A1A1A (var(--text-on-pastel-secondary))
 * - On Dark Primary: #F5F5F7 (var(--text-dark-primary))
 * - On Dark Secondary: #9CA3AF (var(--text-dark-secondary))
 * - On Dark Tertiary: #6B7280 (var(--text-dark-tertiary))
 * 
 * 
 * ТИПОГРАФИКА
 * ===========
 * 
 * Font Sizes:
 * - xs: 0.75rem (12px)
 * - sm: 0.875rem (14px)
 * - base: 1rem (16px)
 * - lg: 1.125rem (18px)
 * - xl: 1.25rem (20px)
 * - 2xl: 1.5rem (24px)
 * - 3xl: 2rem (32px)
 * - 4xl: 2.5rem (40px)
 * - 5xl: 3rem (48px)
 * 
 * Font Weights:
 * - Normal: 400
 * - Medium: 500
 * - Semibold: 600
 * - Bold: 700
 * 
 * Usage Examples:
 * - KPI Numbers: 2.5rem (40px), weight 600, letter-spacing -0.02em
 * - Card Titles: 1rem (16px), weight 600, letter-spacing -0.01em
 * - Card Labels: 0.75rem (12px), weight 500
 * - Page Headers: 1.75rem (28px), weight 600, letter-spacing -0.02em
 * 
 * 
 * ИНТЕРВАЛЫ (SPACING)
 * ===================
 * 
 * - xs: 4px
 * - sm: 8px
 * - md: 12px
 * - lg: 16px
 * - xl: 24px
 * - 2xl: 32px
 * - 3xl: 48px
 * 
 * 
 * СКРУГЛЕНИЯ (BORDER RADIUS)
 * ==========================
 * 
 * - sm: 8px
 * - md: 12px
 * - lg: 18px
 * - xl: 24px
 * - 2xl: 28px
 * 
 * Component Usage:
 * - KPI Cards: 24px
 * - Chart Card: 24px
 * - Bottom Nav Pill: 16px
 * - Action Buttons: 16px
 * 
 * 
 * РАЗМЕРЫ ИКОНОК
 * ==============
 * 
 * - xs: 16px
 * - sm: 20px
 * - md: 24px
 * - lg: 32px
 * 
 * Icon Style: outline (stroke), strokeWidth 1.5-2
 * Library: lucide-react
 * 
 * 
 * ТЕНИ (SHADOWS)
 * ==============
 * 
 * - Card Shadow: 0 2px 8px rgba(0, 0, 0, 0.15)
 * - Hover Shadow: 0 4px 12px rgba(0, 0, 0, 0.2)
 * - Soft Shadow: 0 1px 3px rgba(0, 0, 0, 0.1)
 * 
 * 
 * АНИМАЦИИ
 * ========
 * 
 * Durations:
 * - Fast: 150ms
 * - Normal: 250ms
 * - Slow: 350ms
 * 
 * Easing:
 * - Default: cubic-bezier(0.25, 0.1, 0.25, 1.0)
 * - Spring: type "spring", stiffness 380, damping 30
 * 
 * Animation Types:
 * - Card Entry: fade + translateY (0 to 20px)
 * - Chart Bars: height grow, 800ms duration
 * - Nav Indicator: layoutId spring animation
 * - Button Press: scale 0.98
 * 
 * 
 * КОМПОНЕНТЫ
 * ==========
 * 
 * KPI Card:
 * - Size: flexible (aspect ratio ~1:1)
 * - Padding: 24px
 * - Radius: 24px
 * - Background: pastel colors
 * - Text: dark on light
 * - Icon: outline, 20px, top-right
 * - Number: 40px, weight 600
 * - Label: 12px, weight 500, opacity 0.7
 * 
 * Chart Card:
 * - Padding: 24px
 * - Radius: 24px
 * - Background: #151922
 * - Bar colors: pastel
 * - Highlighted bar: #A5D4D2
 * - Axis labels: 12px, #6B7280
 * 
 * Bottom Navigation:
 * - Background: #151922
 * - Border: rgba(255, 255, 255, 0.08)
 * - Active indicator: #1B2030, radius 16px
 * - Icon size: 22-24px
 * - Label: 10px
 * - Active color: #BFE7E5
 * - Inactive color: #9CA3AF
 * 
 * 
 * ПЛАТФОРМА
 * =========
 * 
 * Target: Android (Jetpack Compose style)
 * Viewport: 360x800 / 411x891
 * Safe areas: bottom navigation padding
 * Theme: Always dark (primary)
 * 
 * 
 * ACCESSIBILITY
 * =============
 * 
 * - Contrast ratios meet WCAG AA standards
 * - Touch targets: minimum 44x44px
 * - Focus indicators: outline with ring color
 * - ARIA labels on icon-only buttons
 * 
 */

export function DesignSystem() {
  return (
    <div className="p-8 bg-[#0F1115] text-[#F5F5F7] min-h-screen">
      <h1 className="text-3xl font-bold mb-8">VapeStore Design System</h1>
      
      {/* Color Palette */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-4">Цветовая палитра</h2>
        
        <div className="mb-6">
          <h3 className="text-lg font-medium mb-3">Backgrounds</h3>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <div className="h-20 bg-[#0F1115] border border-white/10 rounded-lg mb-2"></div>
              <p className="text-sm">Primary BG</p>
              <code className="text-xs text-[#9CA3AF]">#0F1115</code>
            </div>
            <div>
              <div className="h-20 bg-[#151922] rounded-lg mb-2"></div>
              <p className="text-sm">Surface</p>
              <code className="text-xs text-[#9CA3AF]">#151922</code>
            </div>
            <div>
              <div className="h-20 bg-[#1B2030] rounded-lg mb-2"></div>
              <p className="text-sm">Surface Elevated</p>
              <code className="text-xs text-[#9CA3AF]">#1B2030</code>
            </div>
          </div>
        </div>

        <div className="mb-6">
          <h3 className="text-lg font-medium mb-3">Pastel Accents</h3>
          <div className="grid grid-cols-4 gap-4">
            {[
              { name: "Mint", color: "#BFE7E5" },
              { name: "Light Blue", color: "#CFE6F2" },
              { name: "Lavender", color: "#DED8F6" },
              { name: "Soft Pink", color: "#F2D6DE" },
            ].map((color) => (
              <div key={color.name}>
                <div className="h-20 rounded-lg mb-2" style={{ backgroundColor: color.color }}></div>
                <p className="text-sm">{color.name}</p>
                <code className="text-xs text-[#9CA3AF]">{color.color}</code>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Typography */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-4">Типографика</h2>
        <div className="space-y-4 bg-[#151922] p-6 rounded-2xl">
          <div>
            <p style={{ fontSize: "3rem", fontWeight: 600 }}>KPI Number</p>
            <code className="text-xs text-[#9CA3AF]">48px / weight 600</code>
          </div>
          <div>
            <p style={{ fontSize: "1.75rem", fontWeight: 600 }}>Page Header</p>
            <code className="text-xs text-[#9CA3AF]">28px / weight 600</code>
          </div>
          <div>
            <p style={{ fontSize: "1rem", fontWeight: 600 }}>Card Title</p>
            <code className="text-xs text-[#9CA3AF]">16px / weight 600</code>
          </div>
          <div>
            <p style={{ fontSize: "0.75rem", fontWeight: 500 }}>Card Label</p>
            <code className="text-xs text-[#9CA3AF]">12px / weight 500</code>
          </div>
        </div>
      </section>

      {/* Spacing & Radius */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-4">Отступы и скругления</h2>
        <div className="grid grid-cols-2 gap-6">
          <div className="bg-[#151922] p-6 rounded-2xl">
            <h3 className="text-lg font-medium mb-3">Spacing</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span>xs</span><code>4px</code></div>
              <div className="flex justify-between"><span>sm</span><code>8px</code></div>
              <div className="flex justify-between"><span>md</span><code>12px</code></div>
              <div className="flex justify-between"><span>lg</span><code>16px</code></div>
              <div className="flex justify-between"><span>xl</span><code>24px</code></div>
            </div>
          </div>
          <div className="bg-[#151922] p-6 rounded-2xl">
            <h3 className="text-lg font-medium mb-3">Border Radius</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span>sm</span><code>8px</code></div>
              <div className="flex justify-between"><span>md</span><code>12px</code></div>
              <div className="flex justify-between"><span>lg</span><code>18px</code></div>
              <div className="flex justify-between"><span>xl</span><code>24px</code></div>
              <div className="flex justify-between"><span>2xl</span><code>28px</code></div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
