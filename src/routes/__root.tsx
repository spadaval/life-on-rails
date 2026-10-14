import type { ReactNode } from "react";
import { Link, Outlet, createRootRoute, HeadContent, Scripts } from "@tanstack/react-router";
import appCss from "../styles.css?url";

export const Route = createRootRoute({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { title: "Life on Rails" },
    ],
    links: [{ rel: "stylesheet", href: appCss }],
  }),
  component: RootComponent,
  notFoundComponent: () => <p className="p-6">Nothing here.</p>,
});

const NAV = [
  { to: "/", label: "Today" },
  { to: "/capture", label: "Capture" },
] as const;

function RootComponent() {
  return (
    <RootDocument>
      <nav className="mx-auto flex max-w-3xl gap-1 px-4 pt-4 text-base">
        {NAV.map((n) => (
          <Link
            key={n.to}
            to={n.to}
            className="rounded-lg px-3 py-2 font-medium"
            activeProps={{ className: "bg-rail text-white" }}
            activeOptions={{ exact: n.to === "/" }}
          >
            {n.label}
          </Link>
        ))}
      </nav>
      <main className="mx-auto max-w-3xl px-4 pb-24 pt-4">
        <Outlet />
      </main>
    </RootDocument>
  );
}

function RootDocument({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en">
      <head>
        <HeadContent />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}
