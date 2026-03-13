import { useState } from "react";
import { Outlet } from "react-router";
import { BottomNav } from "./BottomNav";
import { DesignSystem } from "./DesignSystem";
import { DemoControls } from "./DemoControls";

export function AppLayout() {
  const [showDesignSystem, setShowDesignSystem] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  if (showDesignSystem) {
    return (
      <div className="min-h-screen bg-[#0F1115]">
        <div className="max-w-6xl mx-auto">
          <div className="sticky top-0 z-50 bg-[#0F1115] border-b border-white/10 p-4">
            <button
              onClick={() => setShowDesignSystem(false)}
              className="text-[#BFE7E5] hover:text-[#A5D4D2] transition-colors"
              style={{ fontSize: "0.875rem", fontWeight: 500 }}
            >
              ← Назад к приложению
            </button>
          </div>
          <DesignSystem />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0F1115] text-[#F5F5F7] overflow-hidden">
      {/* Mobile Container */}
      <div className="max-w-md mx-auto min-h-screen relative bg-[#0F1115]">
        {/* Main Content with Bottom Padding for Nav */}
        <div className="pb-24">
          <Outlet context={{ onShowDesignSystem: () => setShowDesignSystem(true), isLoading }} />
        </div>

        {/* Bottom Navigation */}
        <BottomNav />

        {/* Demo Controls (Floating FAB) */}
        <DemoControls
          isLoading={isLoading}
          onToggleLoading={() => setIsLoading(!isLoading)}
          onShowDesignSystem={() => setShowDesignSystem(true)}
        />
      </div>
    </div>
  );
}
