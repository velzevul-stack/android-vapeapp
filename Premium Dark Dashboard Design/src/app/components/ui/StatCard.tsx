import { motion } from "motion/react";

interface StatCardProps {
  label: string;
  value: string | number;
  color?: "mint" | "blue" | "lavender" | "pink";
  delay?: number;
}

const colorMap = {
  mint: "#BFE7E5",
  blue: "#CFE6F2",
  lavender: "#DED8F6",
  pink: "#F2D6DE",
};

export function StatCard({ label, value, color = "mint", delay = 0 }: StatCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, delay }}
      className="rounded-[20px] p-5"
      style={{ backgroundColor: colorMap[color] }}
    >
      <div className="text-xs text-[#111111] opacity-60 mb-2">{label}</div>
      <div 
        className="text-[#111111]"
        style={{
          fontSize: typeof value === 'number' && value > 999 ? '1.5rem' : '2rem',
          fontWeight: 600,
          letterSpacing: "-0.01em"
        }}
      >
        {typeof value === 'number' ? value.toLocaleString() : value}
      </div>
    </motion.div>
  );
}
