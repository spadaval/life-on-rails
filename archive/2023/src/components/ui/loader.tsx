import { RxRocket } from "react-icons/rx";

export function Loader() {
  return (
    <div className="flex h-full w-full items-center justify-center">
      <RxRocket className="duration-400 h-40 w-40 animate-pulse stroke-gray-500 px-10"></RxRocket>
    </div>
  );
}
