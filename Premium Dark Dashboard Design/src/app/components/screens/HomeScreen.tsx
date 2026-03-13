import { Settings, ShoppingBag, Receipt, TrendingUp, Wallet, Palette } from "lucide-react";
import { useOutletContext, useNavigate } from "react-router";
import { KPICard } from "../KPICard";
import { ChartCard } from "../ChartCard";
import { KPISkeleton } from "../KPISkeleton";

interface HomeScreenContext {
  onShowDesignSystem: () => void;
  isLoading?: boolean;
}

export function HomeScreen() {
  const { onShowDesignSystem, isLoading = false } = useOutletContext<HomeScreenContext>();
  const navigate = useNavigate();
  return (
    <div className="pb-6 px-5">
      {/* Header */}
      <header className="pt-8 pb-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 
              className="text-[#F5F5F7] mb-1"
              style={{
                fontSize: "1.75rem",
                fontWeight: 600,
                letterSpacing: "-0.02em",
                lineHeight: 1.2
              }}
            >
              Добро пожаловать
            </h1>
            <p 
              className="text-[#9CA3AF]"
              style={{
                fontSize: "0.875rem",
                fontWeight: 400,
                letterSpacing: "0.01em"
              }}
            >
              VapeStore Dashboard
            </p>
          </div>
          <div className="flex gap-2">
            <button 
              className="p-2.5 hover:bg-[#151922] rounded-full transition-colors"
              onClick={onShowDesignSystem}
              aria-label="Дизайн-система"
              title="Посмотреть дизайн-систему"
            >
              <Palette 
                size={22} 
                className="text-[#9CA3AF]" 
                strokeWidth={1.5}
              />
            </button>
            <button 
              className="p-2.5 hover:bg-[#151922] rounded-full transition-colors"
              aria-label="Настройки"
            >
              <Settings 
                size={22} 
                className="text-[#9CA3AF]" 
                strokeWidth={1.5}
              />
            </button>
          </div>
        </div>
      </header>

      {/* KPI Cards Grid */}
      <section className="mb-6">
        {isLoading ? (
          <KPISkeleton />
        ) : (
          <div className="grid grid-cols-2 gap-4">
            <KPICard
              title="Продано сегодня"
              value="142"
              icon={ShoppingBag}
              color="mint"
              delay={0.1}
            />
            <KPICard
              title="Чеков сегодня"
              value="87"
              icon={Receipt}
              color="blue"
              delay={0.2}
            />
            <KPICard
              title="Выручка"
              value="234k"
              icon={TrendingUp}
              color="lavender"
              delay={0.3}
            />
            <KPICard
              title="Прибыль"
              value="89k"
              icon={Wallet}
              color="pink"
              delay={0.4}
            />
          </div>
        )}
      </section>

      {/* Chart Section */}
      <section className="mb-6">
        <ChartCard delay={0.5} />
      </section>

      {/* Quick Actions */}
      <section className="mb-6">
        <div className="bg-[#151922] rounded-[20px] p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 
              className="text-[#F5F5F7]"
              style={{
                fontSize: "0.875rem",
                fontWeight: 600,
                letterSpacing: "-0.01em"
              }}
            >
              Быстрые действия
            </h3>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: "Новая продажа", color: "#BFE7E5", path: "/sell" },
              { label: "Приемка товара", color: "#CFE6F2", path: "/accept" },
              { label: "Отчеты", color: "#DED8F6", path: "/cabinet" },
              { label: "Инвентаризация", color: "#F2D6DE", path: "/cabinet" },
            ].map((action) => (
              <button
                key={action.label}
                className="bg-[#1B2030] hover:bg-[#1F2537] rounded-[16px] p-4 text-left transition-colors"
                style={{
                  borderLeft: `3px solid ${action.color}`,
                }}
                onClick={() => navigate(action.path)}
              >
                <span 
                  className="text-[#F5F5F7]"
                  style={{
                    fontSize: "0.875rem",
                    fontWeight: 500,
                  }}
                >
                  {action.label}
                </span>
              </button>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}