import { motion } from "motion/react";
import { BarChart, Bar, XAxis, ResponsiveContainer, Cell } from "recharts";
import { TrendingUp } from "lucide-react";

interface ChartCardProps {
  delay?: number;
}

const weekData = [
  { day: "Пн", value: 45000 },
  { day: "Вт", value: 52000 },
  { day: "Ср", value: 48000 },
  { day: "Чт", value: 61000 },
  { day: "Пт", value: 55000 },
  { day: "Сб", value: 67000 },
  { day: "Вс", value: 59000 },
];

const pastelColors = ["#BFE7E5", "#CFE6F2", "#DED8F6", "#F2D6DE", "#BFE7E5", "#A5D4D2", "#CFE6F2"];

export function ChartCard({ delay = 0 }: ChartCardProps) {
  const totalWeek = weekData.reduce((sum, item) => sum + item.value, 0);
  const maxIndex = weekData.findIndex(item => item.value === Math.max(...weekData.map(d => d.value)));

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ 
        duration: 0.25, 
        delay,
        ease: [0.25, 0.1, 0.25, 1.0]
      }}
      className="bg-[#151922] rounded-[24px] p-6"
      style={{
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.15)",
      }}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h3 
            className="text-[#F5F5F7] mb-1"
            style={{
              fontSize: "1rem",
              fontWeight: 600,
              letterSpacing: "-0.01em"
            }}
          >
            Прибыль за неделю
          </h3>
          <p 
            className="text-[#9CA3AF]"
            style={{
              fontSize: "0.75rem",
              fontWeight: 400
            }}
          >
            {new Date().toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' })}
          </p>
        </div>
        <div className="flex items-center gap-2 bg-[#1B2030] px-3 py-1.5 rounded-full">
          <TrendingUp size={14} className="text-[#BFE7E5]" strokeWidth={2} />
          <span 
            className="text-[#BFE7E5]"
            style={{
              fontSize: "0.75rem",
              fontWeight: 600
            }}
          >
            +12.5%
          </span>
        </div>
      </div>

      {/* Total */}
      <div className="mb-6">
        <p 
          className="text-[#F5F5F7]"
          style={{
            fontSize: "2rem",
            fontWeight: 600,
            lineHeight: 1,
            letterSpacing: "-0.02em"
          }}
        >
          {totalWeek.toLocaleString('ru-RU')} ₽
        </p>
      </div>

      {/* Chart */}
      <div className="h-[180px] -mx-2">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={weekData}>
            <XAxis 
              dataKey="day" 
              axisLine={false}
              tickLine={false}
              tick={{ 
                fill: '#6B7280', 
                fontSize: 12,
                fontWeight: 500
              }}
            />
            <Bar 
              dataKey="value" 
              radius={[8, 8, 0, 0]}
              animationDuration={800}
              animationBegin={delay * 1000 + 200}
            >
              {weekData.map((entry, index) => (
                <Cell 
                  key={`cell-${index}`} 
                  fill={index === maxIndex ? "#A5D4D2" : pastelColors[index]}
                  opacity={index === maxIndex ? 1 : 0.85}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Legend */}
      <div className="flex items-center justify-center gap-4 mt-4">
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-[#BFE7E5]"></div>
          <span 
            className="text-[#9CA3AF]"
            style={{
              fontSize: "0.75rem",
              fontWeight: 400
            }}
          >
            Обычный день
          </span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-[#A5D4D2]"></div>
          <span 
            className="text-[#9CA3AF]"
            style={{
              fontSize: "0.75rem",
              fontWeight: 400
            }}
          >
            Лучший день
          </span>
        </div>
      </div>
    </motion.div>
  );
}
