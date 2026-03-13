import { useState } from "react";
import { motion } from "motion/react";
import { 
  Receipt,
  DollarSign,
  Clock,
  CreditCard,
  Database,
  Edit2,
  X,
  Check,
  AlertTriangle,
  Download,
  Upload
} from "lucide-react";
import { ScreenHeader } from "../ui/ScreenHeader";
import { Button } from "../ui/Button";
import { toast } from "sonner";

interface Sale {
  id: string;
  date: Date;
  product: string;
  quantity: number;
  total: number;
  payment: "cash" | "card";
}

interface Debt {
  id: string;
  client: string;
  amount: number;
  comment: string;
  status: "active" | "paid";
}

interface Reserve {
  id: string;
  product: string;
  client: string;
  quantity: number;
  expiresAt: Date;
}

interface PaymentCard {
  id: string;
  bank: string;
  lastFour: string;
  isActive: boolean;
}

const mockSales: Sale[] = [
  { id: "1", date: new Date(), product: "ELFBAR Watermelon Ice", quantity: 2, total: 1100, payment: "cash" },
  { id: "2", date: new Date(Date.now() - 3600000), product: "HQD Mango Ice", quantity: 1, total: 500, payment: "card" },
];

const mockDebts: Debt[] = [
  { id: "1", client: "Иван И.", amount: 2500, comment: "3 одноразки", status: "active" },
  { id: "2", client: "Мария К.", amount: 1500, comment: "Жидкость 100мл", status: "active" },
];

const mockReserves: Reserve[] = [
  { 
    id: "1", 
    product: "ELFBAR Blueberry", 
    client: "Алексей П.", 
    quantity: 2,
    expiresAt: new Date(Date.now() + 86400000)
  },
];

const mockCards: PaymentCard[] = [
  { id: "1", bank: "Сбербанк", lastFour: "4285", isActive: true },
  { id: "2", bank: "Тинькофф", lastFour: "7732", isActive: true },
];

export function ManagementScreen() {
  return (
    <div className="pb-6">
      <ScreenHeader 
        title="Управление" 
        subtitle="Администрирование магазина"
      />

      <div className="px-5 space-y-6">
        {/* Sales Management */}
        <Section
          title="Управление продажами"
          icon={Receipt}
          color="#BFE7E5"
        >
          <SalesManagement sales={mockSales} />
        </Section>

        {/* Debts */}
        <Section
          title="Долги"
          icon={DollarSign}
          color="#F2D6DE"
        >
          <DebtsManagement debts={mockDebts} />
        </Section>

        {/* Reserves */}
        <Section
          title="Резервы"
          icon={Clock}
          color="#DED8F6"
        >
          <ReservesManagement reserves={mockReserves} />
        </Section>

        {/* Payment Cards */}
        <Section
          title="Платёжные карты"
          icon={CreditCard}
          color="#CFE6F2"
        >
          <CardsManagement cards={mockCards} />
        </Section>

        {/* Backup */}
        <Section
          title="Резервное копирование"
          icon={Database}
          color="#BFE7E5"
        >
          <BackupManagement />
        </Section>
      </div>
    </div>
  );
}

// Section Wrapper Component
function Section({ 
  title, 
  icon: Icon, 
  color, 
  children 
}: { 
  title: string; 
  icon: typeof Receipt; 
  color: string; 
  children: React.ReactNode;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="bg-[#151922] rounded-[24px] p-5"
    >
      <div className="flex items-center gap-3 mb-4">
        <div 
          className="p-2.5 rounded-[14px]" 
          style={{ backgroundColor: `${color}20` }}
        >
          <Icon size={20} style={{ color }} strokeWidth={1.5} />
        </div>
        <h3 
          className="text-[#F5F5F7]"
          style={{
            fontSize: "1rem",
            fontWeight: 600,
            letterSpacing: "-0.01em"
          }}
        >
          {title}
        </h3>
      </div>
      {children}
    </motion.div>
  );
}

// Sales Management Component
function SalesManagement({ sales }: { sales: Sale[] }) {
  const handleEdit = (saleId: string) => {
    toast.info("Редактирование продажи");
  };

  const handleCancel = (saleId: string) => {
    toast.error("Продажа отменена");
  };

  return (
    <div className="space-y-3">
      {sales.map((sale, index) => (
        <motion.div
          key={sale.id}
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.25, delay: index * 0.05 }}
          className="bg-[#1B2030] rounded-[16px] p-4"
        >
          <div className="flex items-start justify-between mb-3">
            <div className="flex-1">
              <div className="text-[#F5F5F7] font-medium mb-1">{sale.product}</div>
              <div className="text-xs text-[#9CA3AF]">
                {sale.date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })} • 
                {sale.quantity} шт • {sale.payment === "cash" ? "Наличные" : "Карта"}
              </div>
            </div>
            <div className="text-[#BFE7E5] font-semibold">
              {sale.total}₽
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => handleEdit(sale.id)}
              className="flex-1 flex items-center justify-center gap-2 py-2 bg-[#151922] rounded-[12px] text-[#F5F5F7] hover:bg-[#0F1115] transition-colors text-sm"
            >
              <Edit2 size={14} strokeWidth={1.5} />
              Исправить
            </button>
            <button
              onClick={() => handleCancel(sale.id)}
              className="flex-1 flex items-center justify-center gap-2 py-2 bg-[#151922] rounded-[12px] text-[#F2D6DE] hover:bg-[#0F1115] transition-colors text-sm"
            >
              <X size={14} strokeWidth={1.5} />
              Отменить
            </button>
          </div>
        </motion.div>
      ))}
    </div>
  );
}

// Debts Management Component
function DebtsManagement({ debts }: { debts: Debt[] }) {
  const handlePay = (debtId: string) => {
    toast.success("Долг погашен");
  };

  return (
    <div className="space-y-3">
      {debts.map((debt, index) => (
        <motion.div
          key={debt.id}
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.25, delay: index * 0.05 }}
          className="bg-[#1B2030] rounded-[16px] p-4"
        >
          <div className="flex items-start justify-between mb-3">
            <div className="flex-1">
              <div className="text-[#F5F5F7] font-medium mb-1">{debt.client}</div>
              <div className="text-xs text-[#9CA3AF]">{debt.comment}</div>
            </div>
            <div className="text-[#F2D6DE] font-semibold">
              {debt.amount}₽
            </div>
          </div>
          <Button
            variant="primary"
            size="sm"
            icon={Check}
            fullWidth
            onClick={() => handlePay(debt.id)}
          >
            Оплатить
          </Button>
        </motion.div>
      ))}
      {debts.length === 0 && (
        <div className="text-center text-[#6B7280] text-sm py-4">
          Нет активных долгов
        </div>
      )}
    </div>
  );
}

// Reserves Management Component
function ReservesManagement({ reserves }: { reserves: Reserve[] }) {
  const handleSell = (reserveId: string) => {
    toast.success("Резерв продан");
  };

  const handleCancel = (reserveId: string) => {
    toast.info("Резерв отменён");
  };

  return (
    <div className="space-y-3">
      {reserves.map((reserve, index) => {
        const hoursLeft = Math.floor((reserve.expiresAt.getTime() - Date.now()) / (1000 * 60 * 60));
        
        return (
          <motion.div
            key={reserve.id}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.25, delay: index * 0.05 }}
            className="bg-[#1B2030] rounded-[16px] p-4"
          >
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1">
                <div className="text-[#F5F5F7] font-medium mb-1">{reserve.product}</div>
                <div className="text-xs text-[#9CA3AF] mb-1">{reserve.client}</div>
                <div className="flex items-center gap-1 text-xs text-[#DED8F6]">
                  <Clock size={12} strokeWidth={1.5} />
                  <span>Истекает через {hoursLeft}ч</span>
                </div>
              </div>
              <div className="text-[#BFE7E5] font-semibold">
                {reserve.quantity} шт
              </div>
            </div>
            <div className="flex gap-2">
              <Button
                variant="primary"
                size="sm"
                fullWidth
                onClick={() => handleSell(reserve.id)}
              >
                Продать
              </Button>
              <button
                onClick={() => handleCancel(reserve.id)}
                className="px-4 py-2 bg-[#151922] rounded-[12px] text-[#9CA3AF] hover:bg-[#0F1115] transition-colors"
              >
                <X size={16} strokeWidth={1.5} />
              </button>
            </div>
          </motion.div>
        );
      })}
      {reserves.length === 0 && (
        <div className="text-center text-[#6B7280] text-sm py-4">
          Нет активных резервов
        </div>
      )}
    </div>
  );
}

// Cards Management Component
function CardsManagement({ cards }: { cards: PaymentCard[] }) {
  const handleAddCard = () => {
    toast.info("Добавление карты в разработке");
  };

  const handleToggleCard = (cardId: string) => {
    toast.info("Статус карты изменён");
  };

  return (
    <div className="space-y-3">
      {cards.map((card, index) => (
        <motion.div
          key={card.id}
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.25, delay: index * 0.05 }}
          className="bg-[#1B2030] rounded-[16px] p-4 flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-[#151922] rounded-[12px]">
              <CreditCard size={18} className="text-[#CFE6F2]" strokeWidth={1.5} />
            </div>
            <div>
              <div className="text-[#F5F5F7] font-medium">{card.bank}</div>
              <div className="text-xs text-[#9CA3AF]">•••• {card.lastFour}</div>
            </div>
          </div>
          <button
            onClick={() => handleToggleCard(card.id)}
            className={`px-3 py-1 rounded-full text-xs font-medium ${
              card.isActive
                ? "bg-[#BFE7E5] text-[#111111]"
                : "bg-[#151922] text-[#6B7280]"
            }`}
          >
            {card.isActive ? "Активна" : "Неактивна"}
          </button>
        </motion.div>
      ))}
      <Button
        variant="outlined"
        size="md"
        icon={CreditCard}
        fullWidth
        onClick={handleAddCard}
      >
        Добавить карту
      </Button>
    </div>
  );
}

// Backup Management Component
function BackupManagement() {
  const [isExporting, setIsExporting] = useState(false);

  const handleExport = () => {
    setIsExporting(true);
    setTimeout(() => {
      setIsExporting(false);
      toast.success("Резервная копия создана");
    }, 2000);
  };

  const handleImport = () => {
    toast.warning("Внимание! Импорт заменит текущие данные");
  };

  return (
    <div className="space-y-3">
      <div className="bg-[#1B2030] rounded-[16px] p-4">
        <div className="flex items-start gap-3 mb-3">
          <div className="p-2 bg-[#F2D6DE]/20 rounded-[10px]">
            <AlertTriangle size={18} className="text-[#F2D6DE]" strokeWidth={1.5} />
          </div>
          <div className="flex-1">
            <div className="text-[#F5F5F7] text-sm font-medium mb-1">
              Резервное копирование
            </div>
            <div className="text-xs text-[#9CA3AF]">
              Регулярно создавайте резервные копии для защиты данных
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <Button
          variant="primary"
          size="md"
          icon={Download}
          fullWidth
          onClick={handleExport}
          disabled={isExporting}
        >
          {isExporting ? "Экспорт..." : "Экспорт"}
        </Button>
        <Button
          variant="outlined"
          size="md"
          icon={Upload}
          fullWidth
          onClick={handleImport}
        >
          Импорт
        </Button>
      </div>

      <div className="text-xs text-[#6B7280] text-center">
        Последняя копия: Сегодня, 14:32
      </div>
    </div>
  );
}
