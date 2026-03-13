import { motion } from "motion/react";
import { Home, Package, ShoppingCart, User, Settings } from "lucide-react";
import { useNavigate, useLocation } from "react-router";

interface NavItem {
  id: string;
  label: string;
  icon: typeof Home;
  path: string;
}

const navItems: NavItem[] = [
  { id: "receiving", label: "Приемка", icon: Package, path: "/accept" },
  { id: "sale", label: "Продажа", icon: ShoppingCart, path: "/sell" },
  { id: "home", label: "Главная", icon: Home, path: "/" },
  { id: "profile", label: "Кабинет", icon: User, path: "/cabinet" },
  { id: "management", label: "Управление", icon: Settings, path: "/management" },
];

export function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  // Determine active tab based on current path
  const getActiveTab = () => {
    const currentPath = location.pathname;
    if (currentPath === "/") return "home";
    if (currentPath.startsWith("/accept")) return "receiving";
    if (currentPath.startsWith("/sell")) return "sale";
    if (currentPath.startsWith("/cabinet")) return "profile";
    if (currentPath.startsWith("/management")) return "management";
    return "home";
  };

  const activeTab = getActiveTab();

  return (
    <div 
      className="fixed bottom-0 left-0 right-0 bg-[#151922] border-t"
      style={{
        borderColor: "rgba(255, 255, 255, 0.08)",
        paddingBottom: "env(safe-area-inset-bottom, 0px)",
      }}
    >
      <div className="max-w-md mx-auto px-4">
        <div className="flex items-center justify-between py-2">
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            const Icon = item.icon;
            
            return (
              <button
                key={item.id}
                onClick={() => navigate(item.path)}
                className="flex flex-col items-center justify-center gap-1 py-2 px-3 relative flex-1"
              >
                {/* Active indicator */}
                {isActive && (
                  <motion.div
                    layoutId="activeTab"
                    className="absolute inset-0 bg-[#1B2030] rounded-2xl"
                    transition={{
                      type: "spring",
                      stiffness: 380,
                      damping: 30,
                    }}
                    style={{
                      zIndex: 0,
                    }}
                  />
                )}
                
                {/* Icon */}
                <div className="relative z-10">
                  <Icon
                    size={item.id === "home" ? 24 : 22}
                    strokeWidth={isActive ? 2 : 1.5}
                    className={`transition-colors ${
                      isActive ? "text-[#BFE7E5]" : "text-[#9CA3AF]"
                    }`}
                  />
                </div>
                
                {/* Label */}
                <span
                  className={`relative z-10 transition-colors ${
                    isActive ? "text-[#F5F5F7]" : "text-[#6B7280]"
                  }`}
                  style={{
                    fontSize: "0.625rem",
                    fontWeight: isActive ? 600 : 500,
                    letterSpacing: "0.01em"
                  }}
                >
                  {item.label}
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}