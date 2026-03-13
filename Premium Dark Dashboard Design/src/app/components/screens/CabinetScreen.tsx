import { useState } from "react";
import { motion } from "motion/react";
import { 
  Package, 
  TrendingUp, 
  ShoppingBag,
  Filter,
  Plus,
  Edit2,
  Archive,
  ChevronRight,
  Calendar
} from "lucide-react";
import { ScreenHeader, IconButton } from "../ui/ScreenHeader";
import { StatCard } from "../ui/StatCard";
import { toast } from "sonner";

type TabType = "stock" | "sales" | "products";

interface Product {
  id: string;
  brand: string;
  flavor: string;
  category: string;
  stock: number;
  buyPrice: number;
  sellPrice: number;
}

interface SaleRecord {
  id: string;
  date: Date;
  revenue: number;
  profit: number;
  itemsSold: number;
}

const mockProducts: Product[] = [
  { id: "1", brand: "ELFBAR", flavor: "Watermelon Ice", category: "Одноразки", stock: 12, buyPrice: 350, sellPrice: 550 },
  { id: "2", brand: "ELFBAR", flavor: "Blueberry", category: "Одноразки", stock: 0, buyPrice: 350, sellPrice: 550 },
  { id: "3", brand: "HQD", flavor: "Mango Ice", category: "Одноразки", stock: 15, buyPrice: 300, sellPrice: 500 },
];

const mockSales: SaleRecord[] = [
  { id: "1", date: new Date(), revenue: 12500, profit: 4200, itemsSold: 24 },
  { id: "2", date: new Date(Date.now() - 86400000), revenue: 15300, profit: 5100, itemsSold: 31 },
  { id: "3", date: new Date(Date.now() - 86400000 * 2), revenue: 9800, profit: 3300, itemsSold: 19 },
];

const tabs: { id: TabType; label: string; icon: typeof Package }[] = [
  { id: "stock", label: "Склад", icon: Package },
  { id: "sales", label: "Продажи", icon: TrendingUp },
  { id: "products", label: "Товары", icon: ShoppingBag },
];

export function CabinetScreen() {
  const [activeTab, setActiveTab] = useState<TabType>("stock");

  const totalItems = mockProducts.length;
  const zeroStock = mockProducts.filter(p => p.stock === 0).length;
  const totalRevenue = mockSales.reduce((sum, s) => sum + s.revenue, 0);
  const totalProfit = mockSales.reduce((sum, s) => sum + s.profit, 0);

  return (
    <div className="pb-6">
      <ScreenHeader 
        title="Кабинет" 
        subtitle="Аналитика и управление"
      />

      {/* Tabs */}
      <section className="px-5 mb-6">
        <div className="bg-[#151922] rounded-[18px] p-1 flex gap-1">
          {tabs.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              className="relative flex-1 flex items-center justify-center gap-2 py-3 rounded-[14px] transition-colors"
            >
              {activeTab === id && (
                <motion.div
                  layoutId="activeTabIndicator"
                  className="absolute inset-0 bg-[#1B2030] rounded-[14px]"
                  transition={{
                    type: "spring",
                    stiffness: 380,
                    damping: 30,
                  }}
                />
              )}
              <Icon 
                size={18} 
                className={`relative z-10 ${activeTab === id ? "text-[#BFE7E5]" : "text-[#9CA3AF]"}`}
                strokeWidth={1.5}
              />
              <span
                className={`relative z-10 text-sm font-medium ${
                  activeTab === id ? "text-[#F5F5F7]" : "text-[#9CA3AF]"
                }`}
              >
                {label}
              </span>
            </button>
          ))}
        </div>
      </section>

      {/* Tab Content */}
      <div className="px-5">
        {activeTab === "stock" && <StockTab totalItems={totalItems} zeroStock={zeroStock} products={mockProducts} />}
        {activeTab === "sales" && <SalesTab sales={mockSales} totalRevenue={totalRevenue} totalProfit={totalProfit} />}
        {activeTab === "products" && <ProductsTab products={mockProducts} />}
      </div>
    </div>
  );
}

// Stock Tab Component
function StockTab({ totalItems, zeroStock, products }: { 
  totalItems: number; 
  zeroStock: number;
  products: Product[];
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      {/* Summary Cards */}
      <div className="grid grid-cols-2 gap-3 mb-6">
        <div className="bg-[#BFE7E5] rounded-[20px] p-5">
          <div className="text-xs text-[#111111] opacity-60 mb-2">Всего позиций</div>
          <div className="text-3xl text-[#111111] font-semibold">{totalItems}</div>
        </div>
        <div className="bg-[#F2D6DE] rounded-[20px] p-5">
          <div className="text-xs text-[#111111] opacity-60 mb-2">Нет в наличии</div>
          <div className="text-3xl text-[#111111] font-semibold">{zeroStock}</div>
        </div>
      </div>

      {/* Filter Button */}
      <div className="mb-4">
        <button
          onClick={() => toast.info("Фильтры в разработке")}
          className="flex items-center gap-2 px-4 py-2 bg-[#151922] rounded-[14px] text-[#F5F5F7] hover:bg-[#1B2030] transition-colors"
        >
          <Filter size={18} strokeWidth={1.5} />
          <span className="text-sm font-medium">Фильтры</span>
        </button>
      </div>

      {/* Products by Category */}
      <div className="space-y-4">
        <h4 className="text-[#F5F5F7] text-sm font-semibold">По категориям</h4>
        
        {["Одноразки", "Жидкости", "Комплектующие"].map((category, index) => {
          const categoryProducts = products.filter(p => p.category === category);
          if (categoryProducts.length === 0) return null;

          return (
            <motion.div
              key={category}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: index * 0.1 }}
            >
              <button
                onClick={() => toast.info(`Открыть ${category}`)}
                className="w-full bg-[#151922] rounded-[18px] p-4 hover:bg-[#1B2030] transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="text-left">
                    <div className="text-[#F5F5F7] font-medium mb-1">{category}</div>
                    <div className="text-xs text-[#9CA3AF]">{categoryProducts.length} товаров</div>
                  </div>
                  <ChevronRight size={20} className="text-[#6B7280]" strokeWidth={1.5} />
                </div>
              </button>
            </motion.div>
          );
        })}
      </div>
    </motion.div>
  );
}

// Sales Tab Component
function SalesTab({ sales, totalRevenue, totalProfit }: {
  sales: SaleRecord[];
  totalRevenue: number;
  totalProfit: number;
}) {
  const [period, setPeriod] = useState("week");

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      {/* Period Selector */}
      <div className="mb-4 flex gap-2 overflow-x-auto pb-2">
        {["Сегодня", "Неделя", "Месяц", "Период"].map((p) => (
          <button
            key={p}
            onClick={() => setPeriod(p.toLowerCase())}
            className={`
              px-4 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-colors
              ${period === p.toLowerCase()
                ? "bg-[#CFE6F2] text-[#111111]"
                : "bg-[#151922] text-[#9CA3AF] hover:bg-[#1B2030]"
              }
            `}
          >
            {p}
          </button>
        ))}
      </div>

      {/* Summary */}
      <div className="grid grid-cols-2 gap-3 mb-6">
        <div className="bg-[#CFE6F2] rounded-[20px] p-5">
          <div className="text-xs text-[#111111] opacity-60 mb-2">Выручка</div>
          <div className="text-2xl text-[#111111] font-semibold">
            {totalRevenue.toLocaleString()}₽
          </div>
        </div>
        <div className="bg-[#DED8F6] rounded-[20px] p-5">
          <div className="text-xs text-[#111111] opacity-60 mb-2">Прибыль</div>
          <div className="text-2xl text-[#111111] font-semibold">
            {totalProfit.toLocaleString()}₽
          </div>
        </div>
      </div>

      {/* Sales by Day */}
      <div className="space-y-3">
        <h4 className="text-[#F5F5F7] text-sm font-semibold">По дням</h4>
        {sales.map((sale, index) => (
          <motion.button
            key={sale.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25, delay: index * 0.1 }}
            onClick={() => toast.info("Детали дня")}
            className="w-full bg-[#151922] rounded-[18px] p-4 hover:bg-[#1B2030] transition-colors"
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2 text-[#F5F5F7]">
                <Calendar size={16} className="text-[#9CA3AF]" strokeWidth={1.5} />
                <span className="font-medium">
                  {sale.date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' })}
                </span>
              </div>
              <span className="text-[#BFE7E5] font-semibold">
                {sale.revenue.toLocaleString()}₽
              </span>
            </div>
            <div className="flex justify-between text-xs text-[#9CA3AF]">
              <span>Прибыль: {sale.profit.toLocaleString()}₽</span>
              <span>{sale.itemsSold} товаров</span>
            </div>
          </motion.button>
        ))}
      </div>
    </motion.div>
  );
}

// Products Tab Component
function ProductsTab({ products }: { products: Product[] }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
    >
      {/* Add Product Button */}
      <div className="mb-4">
        <button
          onClick={() => toast.info("Добавление товара в разработке")}
          className="flex items-center gap-2 px-4 py-3 bg-[#BFE7E5] rounded-[16px] text-[#111111] hover:bg-[#A5D4D2] transition-colors font-medium"
        >
          <Plus size={20} strokeWidth={2} />
          <span>Добавить товар</span>
        </button>
      </div>

      {/* Products List */}
      <div className="space-y-3">
        {products.map((product, index) => (
          <motion.div
            key={product.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25, delay: index * 0.05 }}
            className="bg-[#151922] rounded-[18px] p-4"
          >
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1">
                <h4 className="text-[#F5F5F7] font-semibold mb-1">{product.brand}</h4>
                <p className="text-[#9CA3AF] text-sm mb-1">{product.flavor}</p>
                <span className="text-xs text-[#6B7280]">{product.category}</span>
              </div>
              <span 
                className={`px-3 py-1 rounded-full text-xs font-medium ${
                  product.stock === 0 
                    ? "bg-[#F2D6DE] text-[#111111]" 
                    : "bg-[#1B2030] text-[#BFE7E5]"
                }`}
              >
                {product.stock} шт
              </span>
            </div>

            <div className="flex items-center justify-between mb-3 pb-3 border-b border-white/5">
              <div className="text-xs text-[#9CA3AF]">
                Закуп: {product.buyPrice}₽
              </div>
              <div className="text-[#BFE7E5] font-semibold">
                Цена: {product.sellPrice}₽
              </div>
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => toast.info("Редактирование товара")}
                className="flex-1 flex items-center justify-center gap-2 py-2 bg-[#1B2030] rounded-[12px] text-[#F5F5F7] hover:bg-[#1F2537] transition-colors"
              >
                <Edit2 size={16} strokeWidth={1.5} />
                <span className="text-sm">Редактировать</span>
              </button>
              <button
                onClick={() => toast.info("Архивация товара")}
                className="px-4 py-2 bg-[#1B2030] rounded-[12px] text-[#F5F5F7] hover:bg-[#1F2537] transition-colors"
              >
                <Archive size={16} strokeWidth={1.5} />
              </button>
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}