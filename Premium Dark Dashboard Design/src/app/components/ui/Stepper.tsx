import { Minus, Plus } from "lucide-react";

interface StepperProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
}

export function Stepper({ value, onChange, min = 0, max = 999 }: StepperProps) {
  const handleDecrement = () => {
    if (value > min) {
      onChange(value - 1);
    }
  };

  const handleIncrement = () => {
    if (value < max) {
      onChange(value + 1);
    }
  };

  return (
    <div className="inline-flex items-center gap-3 bg-[#1B2030] rounded-[16px] p-2">
      <button
        onClick={handleDecrement}
        disabled={value <= min}
        className="p-2 rounded-lg hover:bg-[#151922] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      >
        <Minus size={18} className="text-[#F5F5F7]" strokeWidth={2} />
      </button>
      
      <span 
        className="text-[#F5F5F7] min-w-[3ch] text-center"
        style={{
          fontSize: "1.125rem",
          fontWeight: 600
        }}
      >
        {value}
      </span>
      
      <button
        onClick={handleIncrement}
        disabled={value >= max}
        className="p-2 rounded-lg hover:bg-[#151922] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      >
        <Plus size={18} className="text-[#F5F5F7]" strokeWidth={2} />
      </button>
    </div>
  );
}
