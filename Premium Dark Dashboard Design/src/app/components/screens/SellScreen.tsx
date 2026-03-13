import { useState } from "react";
import { Scan, CreditCard, Wallet, SplitSquareHorizontal, Check } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { ScreenHeader, IconButton } from "../ui/ScreenHeader";
import { Button } from "../ui/Button";
import { Stepper } from "../ui/Stepper";
import { SearchBar } from "../ui/SearchBar";
import { SuccessModal } from "../ui/SuccessModal";
import { toast } from "sonner";

interface Product {
  id: string;
  brand: string;
  flavor: string;
  category: string;
  stock: number;
  price: number;
}

interface CartItem {
  product: Product;
  quantity: number;
}

const mockProducts: Product[] = [
  { id: "1", brand: "ELFBAR", flavor: "Watermelon Ice", category: "Одноразки", stock: 12, price: 550 },
  { id: "2", brand: "ELFBAR", flavor: "Blueberry Raspberry", category: "Одноразки", stock: 8, price: 550 },
  { id: "3", brand: "HQD", flavor: "Mango Ice", category: "Одноразки", stock: 15, price: 500 },
  { id: "4", brand: "VAPORESSO", flavor: "Mesh Coil", category: "Комплектующие", stock: 25, price: 350 },
];

const categories = ["Все", "Одноразки", "Жидкости", "Комплектующие"];

export function SellScreen() {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("Все");
  const [cart, setCart] = useState<CartItem[]>([]);
  const [discount, setDiscount] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState<"cash" | "card" | "split">("cash");
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);

  const filteredProducts = mockProducts.filter(p => {
    const matchesSearch = p.brand.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          p.flavor.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === "Все" || p.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const addToCart = (product: Product) => {
    const existingItem = cart.find(item => item.product.id === product.id);
    if (existingItem) {
      setCart(cart.map(item =>
        item.product.id === product.id
          ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) }
          : item
      ));
    } else {
      setCart([...cart, { product, quantity: 1 }]);
    }
  };

  const updateQuantity = (productId: string, quantity: number) => {
    if (quantity === 0) {
      setCart(cart.filter(item => item.product.id !== productId));
    } else {
      setCart(cart.map(item =>
        item.product.id === productId ? { ...item, quantity } : item
      ));
    }
  };

  const subtotal = cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  const total = subtotal - discount;

  const handleSell = () => {
    if (cart.length === 0) {
      toast.error("Корзина пуста");
      return;
    }
    toast.success("Продажа оформлена!");
    setCart([]);
    setDiscount(0);
    setIsSuccessModalOpen(true);
  };

  return (
    <div className="pb-6">
      <ScreenHeader 
        title="Продажа" 
        subtitle="Оформление чека"
        actions={
          <>
            <IconButton icon={Scan} label="Сканер" />
          </>
        }
      />

      {/* Search */}
      <section className="px-5 mb-4">
        <SearchBar
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="Поиск по названию, бренду..."
        />
      </section>

      {/* Category Chips */}
      <section className="px-5 mb-4">
        <div className="flex gap-2 overflow-x-auto pb-2">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => setSelectedCategory(category)}
              className={`
                px-4 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-colors
                ${selectedCategory === category
                  ? "bg-[#BFE7E5] text-[#111111]"
                  : "bg-[#151922] text-[#9CA3AF] hover:bg-[#1B2030]"
                }
              `}
            >
              {category}
            </button>
          ))}
        </div>
      </section>

      {/* Products Grid */}
      <section className="px-5 mb-6">
        <div className="grid grid-cols-2 gap-3">
          {filteredProducts.map((product, index) => (
            <motion.button
              key={product.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: index * 0.05 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => addToCart(product)}
              className="bg-[#151922] rounded-[18px] p-4 text-left hover:bg-[#1B2030] transition-colors"
            >
              <div className="mb-2">
                <h4 className="text-[#F5F5F7] font-semibold text-sm mb-1">
                  {product.brand}
                </h4>
                <p className="text-[#9CA3AF] text-xs line-clamp-1">
                  {product.flavor}
                </p>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-[#BFE7E5] font-semibold">
                  {product.price}₽
                </span>
                <span className="text-xs text-[#6B7280]">
                  {product.stock} шт
                </span>
              </div>
            </motion.button>
          ))}
        </div>
      </section>

      {/* Cart Panel */}
      <AnimatePresence>
        {cart.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 100 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 100 }}
            className="fixed bottom-20 left-0 right-0 px-5 max-w-md mx-auto"
          >
            <div className="bg-[#151922] rounded-[24px] p-5 border border-white/10">
              {/* Cart Items */}
              <div className="mb-4 max-h-40 overflow-y-auto space-y-3">
                {cart.map((item) => (
                  <div key={item.product.id} className="flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="text-[#F5F5F7] text-sm font-medium truncate">
                        {item.product.brand}
                      </div>
                      <div className="text-[#9CA3AF] text-xs">
                        {item.product.price}₽ × {item.quantity}
                      </div>
                    </div>
                    <Stepper
                      value={item.quantity}
                      onChange={(value) => updateQuantity(item.product.id, value)}
                      min={0}
                      max={item.product.stock}
                    />
                  </div>
                ))}
              </div>

              {/* Discount */}
              <div className="mb-4 pb-4 border-b border-white/10">
                <label className="block text-xs text-[#9CA3AF] mb-2">Скидка (₽)</label>
                <input
                  type="number"
                  value={discount}
                  onChange={(e) => setDiscount(Math.max(0, Number(e.target.value)))}
                  className="w-full bg-[#1B2030] rounded-[12px] px-4 py-2 text-[#F5F5F7] focus:outline-none focus:ring-2 focus:ring-[#BFE7E5]/30"
                  style={{ fontSize: "0.875rem" }}
                />
              </div>

              {/* Payment Method */}
              <div className="mb-4">
                <label className="block text-xs text-[#9CA3AF] mb-2">Способ оплаты</label>
                <div className="flex gap-2">
                  {[
                    { id: "cash", label: "Наличные", icon: Wallet },
                    { id: "card", label: "Карта", icon: CreditCard },
                    { id: "split", label: "Раздельно", icon: SplitSquareHorizontal },
                  ].map(({ id, label, icon: Icon }) => (
                    <button
                      key={id}
                      onClick={() => setPaymentMethod(id as typeof paymentMethod)}
                      className={`
                        flex-1 flex flex-col items-center gap-1 p-3 rounded-[14px] transition-colors
                        ${paymentMethod === id
                          ? "bg-[#BFE7E5] text-[#111111]"
                          : "bg-[#1B2030] text-[#9CA3AF] hover:bg-[#1F2537]"
                        }
                      `}
                    >
                      <Icon size={18} strokeWidth={1.5} />
                      <span className="text-xs font-medium">{label}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Total */}
              <div className="flex items-center justify-between mb-4">
                <span className="text-[#9CA3AF]">Итого:</span>
                <span 
                  className="text-[#BFE7E5]"
                  style={{
                    fontSize: "1.5rem",
                    fontWeight: 600
                  }}
                >
                  {total}₽
                </span>
              </div>

              {/* Sell Button */}
              <Button
                variant="primary"
                size="lg"
                icon={Check}
                fullWidth
                onClick={handleSell}
              >
                Продать
              </Button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Success Modal */}
      <SuccessModal
        isOpen={isSuccessModalOpen}
        onClose={() => setIsSuccessModalOpen(false)}
        title="Продажа оформлена!"
        message="Продажа успешно оформлена. Вы можете продолжить продажу или закрыть окно."
      />
    </div>
  );
}