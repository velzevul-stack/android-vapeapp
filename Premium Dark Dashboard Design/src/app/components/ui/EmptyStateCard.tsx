import { motion } from "motion/react";
import { LucideIcon } from "lucide-react";

interface EmptyStateCardProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function EmptyStateCard({ icon: Icon, title, description, action }: EmptyStateCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="bg-[#151922] rounded-[24px] p-8 text-center"
    >
      <div className="w-16 h-16 mx-auto mb-4 bg-[#1B2030] rounded-full flex items-center justify-center">
        <Icon size={32} className="text-[#9CA3AF]" strokeWidth={1.5} />
      </div>
      
      <h3 
        className="text-[#F5F5F7] mb-2"
        style={{
          fontSize: "1.125rem",
          fontWeight: 600,
          letterSpacing: "-0.01em"
        }}
      >
        {title}
      </h3>
      
      <p 
        className="text-[#9CA3AF] mb-6"
        style={{ fontSize: "0.875rem" }}
      >
        {description}
      </p>

      {action && (
        <button
          onClick={action.onClick}
          className="px-6 py-3 bg-[#BFE7E5] text-[#111111] rounded-[16px] font-medium hover:bg-[#A5D4D2] transition-colors"
        >
          {action.label}
        </button>
      )}
    </motion.div>
  );
}
