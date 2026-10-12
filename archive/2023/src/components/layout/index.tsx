"use client";
import { useIsFetching } from "@tanstack/react-query";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/router";
import type { ReactNode } from "react";
import React, { useEffect } from "react";

import {
  Book,
  BookOpen,
  ChevronLeft,
  HeartPulse,
  HelpCircle,
  History,
  LayoutDashboard,
  ToyBrick,
} from "lucide-react";

import { UserButton } from "@clerk/nextjs";
import classNames from "classnames";
import type { LucideIcon } from "lucide-react";
import Head from "next/head";
import { GiHamburgerMenu } from "react-icons/gi";
import { cn } from "../../lib/utils";
import { useSidebarStore } from "./state";

interface MenuItem {
  name: string;
  link: string;
  icon: LucideIcon;
  gap?: boolean;
}

function Sidebar(props: {
  open: boolean;
  setOpen: (open: boolean) => void;
  Menus: MenuItem[];
}) {
  const router = useRouter();
  const path = router.asPath;
  return (
    <div
      className={`${
        props.open ? "w-[14rem]" : "w-0 lg:w-20"
      } fixed bottom-0 left-0 top-0 z-50 h-screen bg-gradient-to-b from-[#00164d] to-[#3c118b] duration-200 md:p-5 md:pt-8`}
    >
      <div
        className={`absolute top-2 h-8 w-8 cursor-pointer ${
          props.open ? "-right-4" : "-rotate-180"
        }`}
        onClick={() => props.setOpen(!props.open)}
      >
        {props.open ? (
          <ChevronLeft className="absolute left-1/2 top-1/2 h-8 w-8 -translate-x-1/2 -translate-y-1/2 transform rounded-full border-2 border-black bg-white"></ChevronLeft>
        ) : (
          <>
            <ChevronLeft className="absolute left-1/2 top-1/2 hidden h-8 w-8 -translate-x-1/2 -translate-y-1/2 transform rounded-full border-black bg-white  lg:visible"></ChevronLeft>
            <GiHamburgerMenu className="absolute visible h-8 w-8 -left-3 opacity-50 lg:hidden" />
          </>
        )}
      </div>

      <div className={props.open ? "" : "max-lg:hidden"}>
        <div
          className={cn("flex items-center gap-x-4", {
            hidden: !props.open,
          })}
        >
          <Link href="/journal">
            <Image
              src="/lor-logo-new.png"
              alt=""
              width="40"
              height="40"
              className="cursor-pointer rounded-lg bg-gray-100 opacity-90 duration-100"
            />
          </Link>
          <h1
            className={`origin-left text-white ${!props.open ? "hidden" : ""}`}
          >
            Life on Rails
          </h1>
          d
        </div>
        {props.Menus.map((menu, index) => (
          <Link
            href={menu.link}
            key={index}
            className={classNames(
              "flex cursor-pointer items-center gap-x-4 rounded-md p-2 text-sm text-white hover:bg-slate-200 hover:bg-opacity-10",
              menu?.gap ? "mt-9" : "mt-2",
              path.includes(menu.link) ? "bg-slate-200 bg-opacity-10" : ""
            )}
          >
            <div>
              {React.createElement(menu?.icon, {
                size: 20,
              })}
            </div>
            <span
              className={`${
                !props.open ? `hidden` : ""
              } origin-left duration-200`}
            >
              {menu.name}
            </span>
          </Link>
        ))}

        <hr className="mx-2 my-8 h-px border-0 bg-gray-500 dark:bg-gray-700"></hr>

        <div className="items-right mt-10 flex w-full gap-x-4">
          <UserButton
            afterSignOutUrl="/journal"
            userProfileMode="navigation"
            userProfileUrl="/profile"
          />
        </div>
      </div>
    </div>
  );
}

const menus = [
  { name: "Journal", link: "/journal", icon: Book, gap: true },
  // { name: "Legacy Journal", link: "/journal", icon: CalendarRange },
  { name: "Dashboard", link: "/dashboard", icon: HeartPulse },
  { name: "All Items", link: "/overview", icon: LayoutDashboard },
  { name: "Timeline", link: "/timeline", icon: History },
  { name: "Templates", link: "/templates", icon: ToyBrick },
  { name: "Blog", link: "/blog/intro", icon: BookOpen },
  { name: "Help", link: "/help", icon: HelpCircle },
  // { name: "Metric Modal", link: "/metric_modal", icon: HelpCircle },
  // { name: "delete account", link: "/deactivate", icon: HelpCircle },
];

function Layout({ children }: { children: ReactNode }) {
  const { open, setOpen } = useSidebarStore();

  const isFetching = useIsFetching();

  // Auto close menu when on mobile (only runs once on layout mount)
  useEffect(() => {
    if (window.innerWidth < 720) {
      setOpen(false);
    } else {
      setOpen(true);
    }
  }, [setOpen]);

  return (
    <>
      <Head>
        <title>Life on Rails</title>
      </Head>

      <Sidebar open={open} setOpen={setOpen} Menus={menus}></Sidebar>
      <div
        className={classNames("h-1 w-full", {
          "animate-pulse bg-green-600": isFetching,
        })}
      ></div>
      <div
        className={classNames(
          "flex-1 overflow-scroll pt-5 scrollbar-none",
          open ? "md:ml-[14rem]" : "md:ml-20"
        )}
      >
        <main
          className={classNames(
            "h-full overflow-scroll pt-5 scrollbar-none md:container md:mx-auto"
          )}
        >
          {children}
        </main>
      </div>
    </>
  );
}
export default Layout;
