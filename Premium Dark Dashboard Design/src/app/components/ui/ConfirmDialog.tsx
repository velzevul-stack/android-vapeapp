import { motion, AnimatePresence } from "motion/react";
import { AlertTriangle, X } from "lucide-react";
import { Button } from "./Button";

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  confirmText?: string;
  cancelText?: string;
  variant?: "danger" | "warning" | "info";
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmText = "Подтвердить",
  cancelText = "Отмена",
  variant = "warning",
}: ConfirmDialogProps) {
  const variantColors = {
    danger: "#F2D6DE",
    warning: "#DED8F6",
    info: "#CFE6F2",
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
          />

          {/* Dialog */}
          <div className="fixed inset-0 flex items-center justify-center z-50 p-5">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="bg-[#151922] rounded-[24px] p-6 max-w-sm w-full border border-white/10"
            >
              {/* Close Button */}
              <button
                onClick={onClose}
                className="absolute top-5 right-5 p-2 rounded-full hover:bg-[#1B2030] transition-colors"
              >
                <X size={20} className="text-[#9CA3AF]" strokeWidth={1.5} />
              </button>

              {/* Icon */}
              <div 
                className="w-14 h-14 mx-auto mb-4 rounded-full flex items-center justify-center"
                style={{ backgroundColor: `${variantColors[variant]}20` }}
              >
                <AlertTriangle 
                  size={28} 
                  style={{ color: variantColors[variant] }}
                  strokeWidth={1.5} 
                />
              </div>

              {/* Content */}
              <h3 
                className="text-[#F5F5F7] text-center mb-2"
                style={{
                  fontSize: "1.25rem",
                  fontWeight: 600,
                  letterSpacing: "-0.01em"
                }}
              >
                {title}
              </h3>
              
              <p 
                className="text-[#9CA3AF] text-center mb-6"
                style={{ fontSize: "0.875rem" }}
              >
                {description}
              </p>

              {/* Actions */}
              <div className="flex gap-3">
                <Button
                  variant="secondary"
                  size="md"
                  fullWidth
                  onClick={onClose}
                >
                  {cancelText}
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  fullWidth
                  onClick={() => {
                    onConfirm();
                    onClose();
                  }}
                >
                  {confirmText}
                </Button>
              </div>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
