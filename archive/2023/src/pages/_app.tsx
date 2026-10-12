import { type AppType } from "next/app";
import { api } from "../utils/api";

import {
  ClerkProvider,
  RedirectToSignIn,
  SignedIn,
  SignedOut,
} from "@clerk/nextjs";
import "@fontsource/raleway/400.css";
import "@fontsource/raleway/600.css";
import "@fontsource/raleway/800.css";
import { useRouter } from "next/router";
import type { ReactNode } from "react";
import Layout from "../components/layout";
import { Modals } from "../components/modals";
import { TooltipProvider } from "../components/ui/tooltip";
import "../styles/Calendar.css";
import "../styles/globals.css";

const ClerkGuard = ({ children }: { children: ReactNode }) => {
  return (
    <ClerkProvider>
      <SignedIn>{children}</SignedIn>
      <SignedOut>
        <RedirectToSignIn />
      </SignedOut>
    </ClerkProvider>
  );
};

const MyApp: AppType = ({ Component, pageProps: { ...pageProps } }) => {
  const router = useRouter();
  const isIndex = "/" == router.pathname;
  const isSignin = ["/sign-in", "/sign-up"].some(it => it == router.pathname);

  if (isIndex || isSignin) {
    return (
      <ClerkProvider>
        <TooltipProvider delayDuration={400} skipDelayDuration={400}>
          <Component {...pageProps} />
        </TooltipProvider>
      </ClerkProvider>
    );
  }
  return (
    <ClerkGuard>
      <TooltipProvider delayDuration={400} skipDelayDuration={400}>
        <Modals></Modals>
        <Layout>
          <Component {...pageProps} />
        </Layout>
      </TooltipProvider>
    </ClerkGuard>
  );
};

export default api.withTRPC(MyApp);
