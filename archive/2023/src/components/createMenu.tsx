import { useAppState } from "./layout/appState";
import { Button } from "./ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";

function PlusIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className="h-6 w-6 stroke-gray-400"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 4.5v15m7.5-7.5h-15"
      />
    </svg>
  );
}

export function Dropdown({
  trigger,
  options,
  className,
}: {
  trigger: JSX.Element;
  options: { name: string; onClick: () => void }[];
  className?: string;
}) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className={className}>{trigger}</DropdownMenuTrigger>
      <DropdownMenuContent>
        {options.map((option) => (
          <DropdownMenuItem key={option.name} onClick={option.onClick}>
            {option.name}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>

    // <>

    //   <button
    //     {...triggerProps}
    //     onClick={() => setOpen(!isOpen)}
    //     className={className || "rounded hover:bg-gray-200"}
    //   >
    //     {trigger}
    //   </button>
    //   {isOpen &&
    //     renderLayer(
    //       <div
    //         {...layerProps}
    //         className="mt-2 h-fit w-fit"
    //         style={{ ...layerProps.style, zIndex: 50 }}
    //       >
    //         <ul className="z-10 w-fit origin-top-right divide-y divide-gray-100 rounded-md bg-white text-center shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
    //           {options.map((option) => (
    //             <li
    //               key={option.name}
    //               className="block cursor-pointer px-4 py-2 text-sm text-gray-700 hover:bg-slate-200"
    //               onClick={() => {
    //                 setOpen(false);
    //                 option.onClick();
    //               }}
    //             >
    //               {option.name}
    //             </li>
    //           ))}
    //         </ul>
    //         <Arrow {...arrowProps} size={13} roundness={1} />
    //       </div>
    //     )}
    // </>
  );
}

export function CreateMenu({
  className,
  triggerIcon,
}: {
  className?: string;
  triggerIcon?: JSX.Element;
}) {
  const openCreateModal = useAppState((store) => store.openCreateModal);

  return (
    <Button variant="ghost" className={className} onClick={openCreateModal}>
      {triggerIcon || <PlusIcon />}
    </Button>
  );
}
