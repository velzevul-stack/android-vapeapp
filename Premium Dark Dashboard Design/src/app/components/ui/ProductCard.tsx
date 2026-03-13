import { motion } from "motion/react";

interface ProductCardProps {
  brand: string;
  flavor: string;
  category: string;
  stock: number;
  buyPrice: number;
  sellPrice: number;
  onClick?: () => void;
  delay?: number;
}

export function ProductCard({
  brand,
  flavor,
  category,
  stock,
  buyPrice,
  sellPrice,
  onClick,
  delay = 0,
}: ProductCardProps) {
  const isLowStock = stock < 5;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, delay }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className="bg-[#151922] rounded-[20px] p-5 hover:bg-[#1B2030] transition-colors cursor-pointer"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1">
          <h4 
            className="text-[#F5F5F7] mb-1"
            style={{
              fontSize: "1rem",
              fontWeight: 600,
              letterSpacing: "-0.01em"
            }}
          >
            {brand}
          </h4>
          <p 
            className="text-[#9CA3AF]"
            style={{
              fontSize: "0.875rem",
              fontWeight: 400
            }}
          >
            {flavor}
          </p>
        </div>
        <span 
          className={`px-3 py-1 rounded-full text-xs ${
            isLowStock ? "bg-[#F2D6DE] text-[#111111]" : "bg-[#1B2030] text-[#BFE7E5]"
          }`}
          style={{ fontWeight: 500 }}
        >
          {stock} шт
        </span>
      </div>

      <div className="flex items-center justify-between">
        <div className="text-xs text-[#6B7280]">
          {category}
        </div>
        <div className="flex gap-3 text-sm">
          <span className="text-[#9CA3AF]">
            {buyPrice}₽
          </span>
          <span className="text-[#BFE7E5] font-semibold">
            {sellPrice}₽
          </span>
        </div>
      </div>
    </motion.div>
  );
}
