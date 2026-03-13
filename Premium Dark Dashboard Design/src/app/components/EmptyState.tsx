import { motion } from "motion/react";
import { Database } from "lucide-react";

interface EmptyStateProps {
  message?: string;
}

export function EmptyState({ message = "Нет данных для отображения" }: EmptyStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      className="bg-[#151922] rounded-[24px] p-12 flex flex-col items-center justify-center text-center"
    >
      <div className="w-16 h-16 rounded-full bg-[#1B2030] flex items-center justify-center mb-4">
        <Database size={32} className="text-[#9CA3AF]" strokeWidth={1.5} />
      </div>
      <p 
        className="text-[#9CA3AF] mb-2"
        style={{
          fontSize: "1rem",
          fontWeight: 500,
        }}
      >
        {message}
      </p>
      <p 
        className="text-[#6B7280]"
        style={{
          fontSize: "0.875rem",
          fontWeight: 400,
        }}
      >
        Данные появятся после первой транзакции
      </p>
    </motion.div>
  );
}
