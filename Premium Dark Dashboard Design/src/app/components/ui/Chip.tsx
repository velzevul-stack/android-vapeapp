import { motion } from "motion/react";

interface ChipProps {
  children: React.ReactNode;
  variant?: "default" | "mint" | "blue" | "lavender" | "pink";
  selected?: boolean;
  onClick?: () => void;
}

const variantColors = {
  default: {
    bg: "#151922",
    text: "#9CA3AF",
    selectedBg: "#BFE7E5",
    selectedText: "#111111",
  },
  mint: {
    bg: "#BFE7E5",
    text: "#111111",
    selectedBg: "#A5D4D2",
    selectedText: "#111111",
  },
  blue: {
    bg: "#CFE6F2",
    text: "#111111",
    selectedBg: "#B8D9ED",
    selectedText: "#111111",
  },
  lavender: {
    bg: "#DED8F6",
    text: "#111111",
    selectedBg: "#CFC8F1",
    selectedText: "#111111",
  },
  pink: {
    bg: "#F2D6DE",
    text: "#111111",
    selectedBg: "#EDC1CB",
    selectedText: "#111111",
  },
};

export function Chip({ children, variant = "default", selected = false, onClick }: ChipProps) {
  const colors = variantColors[variant];
  const isClickable = !!onClick;

  const Component = isClickable ? motion.button : motion.div;

  return (
    <Component
      onClick={onClick}
      whileTap={isClickable ? { scale: 0.95 } : undefined}
      className={`
        inline-flex items-center px-4 py-2 rounded-full text-xs font-medium transition-colors
        ${isClickable ? "cursor-pointer" : ""}
      `}
      style={{
        backgroundColor: selected ? colors.selectedBg : colors.bg,
        color: selected ? colors.selectedText : colors.text,
      }}
    >
      {children}
    </Component>
  );
}
