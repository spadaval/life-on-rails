import type { ReactNode } from "react";

export function StatisticsCard({
  color,
  icon,
  title,
  value,
}: {
  color: string;
  icon: ReactNode;
  title: ReactNode;
  value: ReactNode;
}) {
  return (
    <div className="relative flex flex-col rounded-xl bg-white bg-clip-border text-gray-700 shadow-md">
      <div
        className={`${color} absolute mx-4 -mt-4 grid h-16 w-16 place-items-center overflow-hidden rounded-xl bg-clip-border text-white shadow-lg shadow-blue-500/40`}
      >
        {icon}
      </div>
      <div className="p-4 text-right">
        <p className="text-blue-gray-600 block font-sans text-sm font-normal leading-normal antialiased">
          {title}
        </p>
        <h4 className="text-blue-gray-900 block font-sans text-2xl font-semibold leading-snug tracking-normal antialiased">
          {value}
        </h4>
      </div>
      <div className="border-blue-gray-50 border-t p-4">Footer</div>
    </div>
  );
}

export default StatisticsCard;
