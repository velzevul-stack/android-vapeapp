import { motion } from "motion/react";

export function KPISkeleton() {
  return (
    <div className="grid grid-cols-2 gap-4">
      {[0, 1, 2, 3].map((index) => (
        <motion.div
          key={index}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.3, delay: index * 0.1 }}
          className="bg-[#151922] rounded-[24px] p-6 h-[140px]"
        >
          <div className="flex flex-col h-full">
            <div className="flex items-start justify-between mb-auto">
              <div className="h-3 bg-[#1B2030] rounded w-20 animate-pulse"></div>
              <div className="w-5 h-5 bg-[#1B2030] rounded-full animate-pulse"></div>
            </div>
            <div className="mt-4">
              <div className="h-10 bg-[#1B2030] rounded w-24 animate-pulse"></div>
            </div>
          </div>
        </motion.div>
      ))}
    </div>
  );
}
