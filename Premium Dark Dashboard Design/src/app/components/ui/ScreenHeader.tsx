import { LucideIcon } from "lucide-react";

interface ScreenHeaderProps {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}

export function ScreenHeader({ title, subtitle, actions }: ScreenHeaderProps) {
  return (
    <header className="pt-8 pb-6 px-5">
      <div className="flex items-start justify-between">
        <div>
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
          {subtitle && (
            <p 
              className="text-[#9CA3AF]"
              style={{
                fontSize: "0.875rem",
                fontWeight: 400,
                letterSpacing: "0.01em"
              }}
            >
              {subtitle}
            </p>
          )}
        </div>
        {actions && (
          <div className="flex gap-2">
            {actions}
          </div>
        )}
      </div>
    </header>
  );
}

interface IconButtonProps {
  icon: LucideIcon;
  onClick?: () => void;
  label: string;
}

export function IconButton({ icon: Icon, onClick, label }: IconButtonProps) {
  return (
    <button 
      className="p-2.5 hover:bg-[#151922] rounded-full transition-colors"
      onClick={onClick}
      aria-label={label}
    >
      <Icon 
        size={22} 
        className="text-[#9CA3AF]" 
        strokeWidth={1.5}
      />
    </button>
  );
}
