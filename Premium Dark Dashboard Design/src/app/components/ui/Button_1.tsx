import { motion } from "motion/react";
import { LucideIcon } from "lucide-react";

interface ButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: "primary" | "secondary" | "tonal" | "outlined";
  size?: "sm" | "md" | "lg";
  icon?: LucideIcon;
  fullWidth?: boolean;
  disabled?: boolean;
}

export function Button({
  children,
  onClick,
  variant = "primary",
  size = "md",
  icon: Icon,
  fullWidth = false,
  disabled = false,
}: ButtonProps) {
  const baseClasses = "rounded-[18px] transition-all font-medium flex items-center justify-center gap-2";
  
  const variants = {
    primary: "bg-[#BFE7E5] text-[#111111] hover:bg-[#A5D4D2]",
    secondary: "bg-[#151922] text-[#F5F5F7] hover:bg-[#1B2030]",
    tonal: "bg-[#1B2030] text-[#F5F5F7] hover:bg-[#1F2537]",
    outlined: "border border-white/10 text-[#F5F5F7] hover:bg-[#151922]",
  };

  const sizes = {
    sm: "px-4 py-2 text-sm",
    md: "px-6 py-3 text-base",
    lg: "px-8 py-4 text-lg",
  };

  return (
    <motion.button
      onClick={onClick}
      disabled={disabled}
      whileTap={{ scale: 0.98 }}
      className={`
        ${baseClasses}
        ${variants[variant]}
        ${sizes[size]}
        ${fullWidth ? "w-full" : ""}
        ${disabled ? "opacity-50 cursor-not-allowed" : ""}
      `}
    >
      {Icon && <Icon size={20} strokeWidth={1.5} />}
      {children}
    </motion.button>
  );
}
