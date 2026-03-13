import { useState } from "react";
import { Search, Scan, Check, Package as PackageIcon, Clock } from "lucide-react";
import { motion } from "motion/react";
import { ScreenHeader, IconButton } from "../ui/ScreenHeader";
import { Button } from "../ui/Button";
import { Stepper } from "../ui/Stepper";
import { PageTransition } from "../ui/PageTransition";
import { toast } from "sonner";

interface Product {
  id: string;
  brand: string;
  flavor: string;
  category: string;
  stock: number;
  buyPrice: number;
  sellPrice: number;
}

interface AcceptanceRecord {
  id: string;
  product: Product;
  quantity: number;
  timestamp: Date;
}

const mockProduct: Product = {
  id: "1",
  brand: "ELFBAR",
  flavor: "Watermelon Ice",
  category: "Одноразки",
  stock: 12,
  buyPrice: 350,
  sellPrice: 550,
};

const mockRecords: AcceptanceRecord[] = [
  {
    id: "1",
    product: mockProduct,
    quantity: 20,
    timestamp: new Date(Date.now() - 1000 * 60 * 30),
  },
  {
    id: "2",
    product: { ...mockProduct, flavor: "Blueberry Raspberry" },
    quantity: 15,
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2),
  },
];

export function AcceptScreen() {
  const [scannedProduct, setScannedProduct] = useState<Product | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [isScanning, setIsScanning] = useState(false);

  const handleScan = () => {
    setIsScanning(true);
    // Simulate scanning
    setTimeout(() => {
      setScannedProduct(mockProduct);
      setIsScanning(false);
      toast.success("Товар найден");
    }, 1500);
  };

  const handleManualInput = () => {
    toast.info("Функция в разработке");
  };

  const handleAccept = () => {
    if (!scannedProduct) return;
    toast.success(`Принято +${quantity} шт`);
    setScannedProduct(null);
    setQuantity(1);
  };

  return (
    <div className="pb-6">
      <ScreenHeader 
        title="Приёмка" 
        subtitle="Добавление товара на склад"
        actions={
          <>
            <IconButton icon={Search} label="Поиск" />
          </>
        }
      />

      {/* Main Actions */}
      <section className="px-5 mb-6">
        <div className="space-y-3">
          <Button
            variant="primary"
            size="lg"
            icon={Scan}
            fullWidth
            onClick={handleScan}
            disabled={isScanning}
          >
            {isScanning ? "Сканирование..." : "Сканировать штрихкод"}
          </Button>
          
          <Button
            variant="secondary"
            size="md"
            fullWidth
            onClick={handleManualInput}
          >
            Ввести вручную
          </Button>
        </div>
      </section>

      {/* Scanned Product Result */}
      {scannedProduct && (
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
          className="px-5 mb-6"
        >
          <div className="bg-[#BFE7E5] rounded-[24px] p-6">
            {/* Product Info */}
            <div className="mb-4">
              <h3 
                className="text-[#111111] mb-1"
                style={{
                  fontSize: "1.25rem",
                  fontWeight: 600,
                  letterSpacing: "-0.01em"
                }}
              >
                {scannedProduct.brand}
              </h3>
              <p 
                className="text-[#1A1A1A] mb-2"
                style={{ fontSize: "0.875rem" }}
              >
                {scannedProduct.flavor}
              </p>
              <div className="flex items-center gap-4 text-xs text-[#111111] opacity-70">
                <span>{scannedProduct.category}</span>
                <span>•</span>
                <span>В наличии: {scannedProduct.stock} шт</span>
              </div>
            </div>

            {/* Prices */}
            <div className="flex gap-4 mb-4 pb-4 border-b border-[#111111]/10">
              <div>
                <div className="text-xs text-[#111111] opacity-60 mb-1">Закупка</div>
                <div className="text-[#111111] font-semibold">{scannedProduct.buyPrice}₽</div>
              </div>
              <div>
                <div className="text-xs text-[#111111] opacity-60 mb-1">Розница</div>
                <div className="text-[#111111] font-semibold">{scannedProduct.sellPrice}₽</div>
              </div>
            </div>

            {/* Quantity Stepper */}
            <div className="mb-4">
              <label className="block text-xs text-[#111111] opacity-70 mb-2">
                Количество для приёмки
              </label>
              <Stepper value={quantity} onChange={setQuantity} min={1} max={999} />
            </div>

            {/* Accept Button */}
            <Button
              variant="secondary"
              size="lg"
              icon={Check}
              fullWidth
              onClick={handleAccept}
            >
              Принять товар
            </Button>
          </div>
        </motion.section>
      )}

      {/* Recent Acceptances */}
      <section className="px-5">
        <h3 
          className="text-[#F5F5F7] mb-4"
          style={{
            fontSize: "0.875rem",
            fontWeight: 600,
            letterSpacing: "-0.01em"
          }}
        >
          Последние приёмки
        </h3>
        
        <div className="space-y-3">
          {mockRecords.map((record, index) => (
            <motion.div
              key={record.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: index * 0.1 }}
              className="bg-[#151922] rounded-[18px] p-4 flex items-center gap-4"
            >
              <div className="p-3 bg-[#1B2030] rounded-[14px]">
                <PackageIcon size={20} className="text-[#BFE7E5]" strokeWidth={1.5} />
              </div>
              
              <div className="flex-1">
                <div className="flex items-start justify-between mb-1">
                  <h4 className="text-[#F5F5F7] font-medium text-sm">
                    {record.product.brand}
                  </h4>
                  <span className="text-[#BFE7E5] font-semibold text-sm">
                    +{record.quantity} шт
                  </span>
                </div>
                <p className="text-[#9CA3AF] text-xs mb-1">
                  {record.product.flavor}
                </p>
                <div className="flex items-center gap-2 text-xs text-[#6B7280]">
                  <Clock size={12} strokeWidth={1.5} />
                  <span>
                    {record.timestamp.toLocaleTimeString('ru-RU', { 
                      hour: '2-digit', 
                      minute: '2-digit' 
                    })}
                  </span>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  );
}