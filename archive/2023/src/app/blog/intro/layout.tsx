import "@fontsource/raleway/400.css";
import "@fontsource/raleway/600.css";
import "@fontsource/raleway/800.css";
import classNames from "classnames";
import Image from "next/image";
import Link from "next/link";
import "../../../styles/globals.css";
import { ClerkProvider } from "@clerk/nextjs";

const TopNav = () => {
  return (
    <nav className="relative flex h-20">
      <div className="flex w-full flex-wrap items-center justify-between pr-6 shadow">
        <div className="flex items-center">
          <Image
            src="/lor-logo-new.png"
            className="bg-transparent px-2.5 py-2"
            alt=""
            width={70}
            height={70}
          />
          <div className="text-md font-bold text-blue-600">Life on Rails</div>
        </div>

        <div
          className="flex-grow items-center"
          id="navbarSupportedContent"
        ></div>

        <div className="relative flex items-center">
          <button className="rounded-full bg-gray-500 px-4 py-2 text-sm font-semibold text-white no-underline transition hover:bg-black/20">
            <Link href="/journal">Go to app</Link>
          </button>
        </div>
      </div>
    </nav>
  );
};

function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <ClerkProvider>
      <html lang="en">
        <head>
          <title>Life on Rails</title>
        </head>
        <body className="relative">
          <div className="flex flex-col">
            <TopNav></TopNav>
            <main
              className={classNames(
                "container prose prose-slate mx-auto mb-20 mt-10 h-full w-full overflow-scroll bg-white pt-5 scrollbar-none"
              )}
            >
              {children}
            </main>
          </div>
        </body>
      </html>
    </ClerkProvider>
  );
}
export default RootLayout;
