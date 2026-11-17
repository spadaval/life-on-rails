// Just enough of @tanstack/react-router for the copied routes to run unchanged on the
// prototype's hash: Link, useNavigate, useRouter and createFileRoute, over src/shell/nav.ts.
// Loaders read the store, synchronously; nothing here makes a request.
import type { AnchorHTMLAttributes, ReactNode } from "react";
import { useStore } from "../../data/store.js";
import { href, navigate as go, type Params, useRoute } from "../../shell/nav.js";

type Search = Record<string, unknown>;

/** The page's parameters, as the routes' zod schemas want them: "true" and "false" are booleans. */
function searchOf(params: Record<string, string>): Search {
  const out: Search = {};
  for (const [k, v] of Object.entries(params)) out[k] = v === "true" ? true : v === "false" ? false : v;
  return out;
}

const toParams = (s: Search | undefined): Params => {
  const out: Params = {};
  for (const [k, v] of Object.entries(s ?? {})) if (v !== undefined && v !== null) out[k] = typeof v === "object" ? JSON.stringify(v) : (v as string | number | boolean);
  return out;
};

export function Link({ to = "/", search, className = "", activeProps, activeOptions, children, ...rest }: {
  to?: string;
  search?: Search;
  className?: string;
  activeProps?: { className?: string };
  activeOptions?: { exact?: boolean; includeSearch?: boolean };
  children?: ReactNode;
} & Omit<AnchorHTMLAttributes<HTMLAnchorElement>, "href" | "className">) {
  const route = useRoute();
  const active = activeOptions?.exact ? route.path === to : route.path === to || route.path.startsWith(`${to}/`);
  return (
    <a href={href(to, toParams(search))} className={`${className} ${active && activeProps?.className ? activeProps.className : ""}`} aria-current={active ? "page" : undefined} {...rest}>
      {children}
    </a>
  );
}

export function useNavigate(_opts?: { from?: string }) {
  const route = useRoute();
  return (o: { to?: string; search?: Search | ((s: Search) => Search); replace?: boolean }) => {
    const cur = searchOf(route.params);
    const next = typeof o.search === "function" ? o.search(cur) : (o.search ?? {});
    go(o.to ?? route.path, toParams(next), { replace: o.replace });
    return Promise.resolve();
  };
}

/** The store re-renders every screen on a change, so there is nothing to invalidate. */
export function useRouter() {
  return { invalidate: () => Promise.resolve() };
}

interface RouteOptions<S, D, L> {
  validateSearch?: (search: Search) => S;
  loaderDeps?: (o: { search: S }) => D;
  loader?: (o: { deps: D }) => L;
  component: () => ReactNode;
}

export function createFileRoute(_path: string) {
  return <S, D = undefined, L = undefined>(opts: RouteOptions<S, D, L>) => {
    const useSearch = (): S => {
      const route = useRoute();
      const raw = searchOf(route.params);
      try {
        return opts.validateSearch ? opts.validateSearch(raw) : (raw as S);
      } catch {
        return (opts.validateSearch ? opts.validateSearch({}) : {}) as S;
      }
    };
    return {
      options: opts,
      useSearch,
      useLoaderData: (): L => {
        useStore();
        const search = useSearch();
        const deps = (opts.loaderDeps ? opts.loaderDeps({ search }) : undefined) as D;
        return opts.loader!({ deps });
      },
    };
  };
}
