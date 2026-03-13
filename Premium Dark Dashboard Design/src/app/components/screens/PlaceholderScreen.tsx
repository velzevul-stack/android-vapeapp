import { motion } from "motion/react";
import { LucideIcon } from "lucide-react";

interface PlaceholderScreenProps {
  title: string;
  description: string;
  icon: LucideIcon;
}

export function PlaceholderScreen({ title, description, icon: Icon }: PlaceholderScreenProps) {
  return (
    <div className="pb-6 px-5">
      <header className="pt-8 pb-6">
        <h1 
          className="text-[#F5F5F7] mb-1"
          style={{
            fontSize: "1.75rem",
            fontWeight: 600,
            letterSpacing: "-0.02em",
            lineHeight: 1.2
          }}
        >
          {title}
        </h1>
        <p 
          className="text-[#9CA3AF]"
          style={{
            fontSize: "0.875rem",
            fontWeight: 400,
            letterSpacing: "0.01em"
          }}
        >
          {description}
        </p>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="bg-[#151922] rounded-[24px] p-12 flex flex-col items-center justify-center text-center min-h-[400px]"
      >
        <div className="w-20 h-20 rounded-full bg-[#1B2030] flex items-center justify-center mb-6">
          <Icon size={40} className="text-[#BFE7E5]" strokeWidth={1.5} />
        </div>
        <p 
          className="text-[#F5F5F7] mb-2"
          style={{
            fontSize: "1.25rem",
            fontWeight: 600,
          }}
        >
          Экран в разработке
        </p>
        <p 
          className="text-[#9CA3AF] max-w-sm"
          style={{
            fontSize: "0.875rem",
            fontWeight: 400,
          }}
        >
          Этот раздел появится в следующих обновлениях приложения
        </p>
      </motion.div>
    </div>
  );
}
