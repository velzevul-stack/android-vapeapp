import * as React from "react";
import { motion, AnimatePresence } from "motion/react";
import { Loader2, CheckCircle2, X } from "lucide-react";

interface DemoControlsProps {
  isLoading: boolean;
  onToggleLoading: () => void;
  onShowDesignSystem: () => void;
}

export function DemoControls({ isLoading, onToggleLoading, onShowDesignSystem }: DemoControlsProps) {
  const [isOpen, setIsOpen] = React.useState(false);

  return (
    <>
      {/* Floating Action Button */}
      <motion.button
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ delay: 1, type: "spring" }}
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-28 right-6 z-40 w-14 h-14 bg-[#BFE7E5] rounded-full flex items-center justify-center shadow-lg"
        style={{
          boxShadow: "0 4px 12px rgba(191, 231, 229, 0.3)",
        }}
      >
        <motion.div
          animate={{ rotate: isOpen ? 45 : 0 }}
          transition={{ duration: 0.2 }}
        >
          {isOpen ? (
            <X size={24} className="text-[#111111]" strokeWidth={2} />
          ) : (
            <CheckCircle2 size={24} className="text-[#111111]" strokeWidth={2} />
          )}
        </motion.div>
      </motion.button>

      {/* Demo Panel */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className="fixed bottom-44 right-6 z-50 bg-[#151922] rounded-2xl p-4 w-64 shadow-xl"
            style={{
              boxShadow: "0 10px 25px rgba(0, 0, 0, 0.3)",
              border: "1px solid rgba(255, 255, 255, 0.1)",
            }}
          >
            <h3 
              className="text-[#F5F5F7] mb-4"
              style={{
                fontSize: "0.875rem",
                fontWeight: 600,
              }}
            >
              Демо-панель
            </h3>

            <div className="space-y-3">
              {/* Loading Toggle */}
              <button
                onClick={onToggleLoading}
                className="w-full bg-[#1B2030] hover:bg-[#1F2537] rounded-xl p-3 flex items-center gap-3 transition-colors"
              >
                <div className={`w-10 h-6 rounded-full transition-colors ${
                  isLoading ? 'bg-[#BFE7E5]' : 'bg-[#374151]'
                } relative`}>
                  <motion.div
                    animate={{ x: isLoading ? 16 : 2 }}
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                    className={`absolute top-1 w-4 h-4 rounded-full ${
                      isLoading ? 'bg-[#111111]' : 'bg-[#6B7280]'
                    }`}
                  />
                </div>
                <div className="flex-1 text-left">
                  <p 
                    className="text-[#F5F5F7]"
                    style={{ fontSize: "0.875rem", fontWeight: 500 }}
                  >
                    Loading State
                  </p>
                  <p 
                    className="text-[#6B7280]"
                    style={{ fontSize: "0.75rem" }}
                  >
                    {isLoading ? 'Включено' : 'Выключено'}
                  </p>
                </div>
              </button>

              {/* Design System Button */}
              <button
                onClick={() => {
                  onShowDesignSystem();
                  setIsOpen(false);
                }}
                className="w-full bg-[#1B2030] hover:bg-[#1F2537] rounded-xl p-3 flex items-center gap-3 transition-colors"
              >
                <div className="w-10 h-10 rounded-full bg-[#DED8F6] flex items-center justify-center">
                  <span className="text-xl">🎨</span>
                </div>
                <div className="flex-1 text-left">
                  <p 
                    className="text-[#F5F5F7]"
                    style={{ fontSize: "0.875rem", fontWeight: 500 }}
                  >
                    Дизайн-система
                  </p>
                  <p 
                    className="text-[#6B7280]"
                    style={{ fontSize: "0.75rem" }}
                  >
                    Документация
                  </p>
                </div>
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}