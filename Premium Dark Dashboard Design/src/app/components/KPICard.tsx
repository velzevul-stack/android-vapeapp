import { motion } from "motion/react";
import { LucideIcon } from "lucide-react";

interface KPICardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  color: "mint" | "blue" | "lavender" | "pink";
  delay?: number;
}

const colorClasses = {
  mint: "bg-[#BFE7E5]",
  blue: "bg-[#CFE6F2]",
  lavender: "bg-[#DED8F6]",
  pink: "bg-[#F2D6DE]",
};

export function KPICard({ title, value, icon: Icon, color, delay = 0 }: KPICardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ 
        duration: 0.25, 
        delay,
        ease: [0.25, 0.1, 0.25, 1.0]
      }}
      whileTap={{ scale: 0.98 }}
      className={`${colorClasses[color]} rounded-[24px] p-6 cursor-pointer transition-shadow hover:shadow-lg`}
      style={{
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.15)",
      }}
    >
      <div className="flex flex-col h-full">
        <div className="flex items-start justify-between mb-auto">
          <div className="flex-1">
            <p 
              className="text-[#1A1A1A] mb-1 opacity-70"
              style={{ 
                fontSize: "0.75rem",
                fontWeight: 500,
                letterSpacing: "0.01em"
              }}
            >
              {title}
            </p>
          </div>
          <Icon 
            className="text-[#111111] opacity-40" 
            size={20} 
            strokeWidth={1.5}
          />
        </div>
        
        <div className="mt-4">
          <p 
            className="text-[#111111]"
            style={{
              fontSize: "2.5rem",
              fontWeight: 600,
              lineHeight: 1,
              letterSpacing: "-0.02em"
            }}
          >
            {value}
          </p>
        </div>
      </div>
    </motion.div>
  );
}
