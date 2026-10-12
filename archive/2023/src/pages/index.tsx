import { type NextPage } from "next";
import Head from "next/head";

import {
  CloudArrowUpIcon,
  LockClosedIcon,
  ServerIcon,
} from "@heroicons/react/20/solid";

import { Dialog } from "@headlessui/react";
import { Bars3Icon, XMarkIcon } from "@heroicons/react/24/outline";
import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import AuthShowcase from "../components/authbox";

function Hero() {
  const navigation = [
    { name: "Home", href: "#" },
    { name: "Features", href: "#features" },
    { name: "Pricing", href: "#pricing" },
    { name: "Team", href: "#team" },
  ];
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-white">
      <header className="absolute inset-x-0 top-0 z-50">
        <nav
          className="flex items-center justify-between p-6 lg:px-8"
          aria-label="Global"
        >
          <div className="flex lg:flex-1">
            <a href="#" className="-m-1.5 p-1.5">
              <span className="sr-only">Hybrid Studios</span>
              <Image src="/lor-logo-new.png" alt="" height={32} width={32} />
            </a>
            <h1 className="p-1.5 font-extrabold text-gray-800">
              Life on Rails
            </h1>
          </div>
          <div className="flex lg:hidden">
            <button
              type="button"
              className="-m-2.5 inline-flex items-center justify-center rounded-md p-2.5 text-gray-700"
              onClick={() => setMobileMenuOpen(true)}
            >
              <span className="sr-only">Open main menu</span>
              <Bars3Icon className="h-6 w-6" aria-hidden="true" />
            </button>
          </div>
          <div className="hidden lg:flex lg:gap-x-12">
            {navigation.map((item) => (
              <a
                key={item.name}
                href={item.href}
                className="text-sm font-semibold leading-6 text-gray-900"
              >
                {item.name}
              </a>
            ))}
          </div>
          <div className="hidden lg:flex lg:flex-1 lg:justify-end">
            <Link
              href="/journal"
              className="text-sm font-semibold leading-6 text-gray-900"
            >
              Go to app <span aria-hidden="true">&rarr;</span>
            </Link>
          </div>
        </nav>
        <Dialog
          as="div"
          className="lg:hidden"
          open={mobileMenuOpen}
          onClose={setMobileMenuOpen}
        >
          <div className="fixed inset-0 z-50" />
          <Dialog.Panel className="fixed inset-y-0 right-0 z-50 w-full overflow-y-auto bg-white px-6 py-6 sm:max-w-sm sm:ring-1 sm:ring-gray-900/10">
            <div className="flex items-center justify-between">
              <a href="#" className="-m-1.5 p-1.5">
                <span className="sr-only">TP6</span>
                <Image
                  width={32}
                  height={32}
                  className="h-8 w-auto"
                  src="img/lor-logo-2.png"
                  alt=""
                />
              </a>
              <button
                type="button"
                className="-m-2.5 rounded-md p-2.5 text-gray-700"
                onClick={() => setMobileMenuOpen(false)}
              >
                <span className="sr-only">Close menu</span>
                <XMarkIcon className="h-6 w-6" aria-hidden="true" />
              </button>
            </div>
            <div className="mt-6 flow-root">
              <div className="-my-6 divide-y divide-gray-500/10">
                <div className="space-y-2 py-6">
                  {navigation.map((item) => (
                    <a
                      key={item.name}
                      href={item.href}
                      className="-mx-3 block rounded-lg px-3 py-2 text-base font-semibold leading-7 text-gray-900 hover:bg-gray-50"
                    >
                      {item.name}
                    </a>
                  ))}
                </div>
                <div className="py-6">
                  <Link
                    href="/login"
                    className="-mx-3 block rounded-lg px-3 py-2.5 text-base font-semibold leading-7 text-gray-900 hover:bg-gray-50"
                  >
                    Log in
                  </Link>
                </div>
              </div>
            </div>
          </Dialog.Panel>
        </Dialog>
      </header>

      <div className="relative isolate px-6 pt-14 lg:px-8">
        <div
          className="absolute inset-x-0 -top-40 -z-10 transform-gpu overflow-hidden blur-3xl sm:-top-80"
          aria-hidden="true"
        >
          <div
            className="relative left-[calc(50%-11rem)] aspect-[1155/678] w-[36.125rem] -translate-x-1/2 rotate-[30deg] bg-gradient-to-tr from-[#ff80b5] to-[#9089fc] opacity-30 sm:left-[calc(50%-30rem)] sm:w-[72.1875rem]"
            style={{
              clipPath:
                "polygon(74.1% 44.1%, 100% 61.6%, 97.5% 26.9%, 85.5% 0.1%, 80.7% 2%, 72.5% 32.5%, 60.2% 62.4%, 52.4% 68.1%, 47.5% 58.3%, 45.2% 34.5%, 27.5% 76.7%, 0.1% 64.9%, 17.9% 100%, 27.6% 76.8%, 76.1% 97.7%, 74.1% 44.1%)",
            }}
          />
        </div>
        <div className="mx-auto max-w-2xl py-32 sm:py-48 lg:py-56">
          <div className="text-center">
            <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-6xl">
              Ride the rails to success
            </h1>
            <p className="mt-6 text-lg leading-8 text-gray-600">
              Empower your journey to success with Life on Rails - the app that
              tracks your goals, habits, and progress.
            </p>
            <div className="mt-10 flex items-center justify-center gap-x-6">
              <AuthShowcase></AuthShowcase>
              <a
                href="https://www.youtube.com/watch?v=ja0sEM5C8yc"
                className="text-sm font-semibold leading-6 text-gray-900"
              >
                Learn more <span aria-hidden="true">→</span>
              </a>
            </div>
          </div>
        </div>
        <div
          className="absolute inset-x-0 top-[calc(100%-13rem)] -z-10 transform-gpu overflow-hidden blur-3xl sm:top-[calc(100%-30rem)]"
          aria-hidden="true"
        >
          <div
            className="relative left-[calc(50%+3rem)] aspect-[1155/678] w-[36.125rem] -translate-x-1/2 bg-gradient-to-tr from-[#ff80b5] to-[#9089fc] opacity-30 sm:left-[calc(50%+36rem)] sm:w-[72.1875rem]"
            style={{
              clipPath:
                "polygon(74.1% 44.1%, 100% 61.6%, 97.5% 26.9%, 85.5% 0.1%, 80.7% 2%, 72.5% 32.5%, 60.2% 62.4%, 52.4% 68.1%, 47.5% 58.3%, 45.2% 34.5%, 27.5% 76.7%, 0.1% 64.9%, 17.9% 100%, 27.6% 76.8%, 76.1% 97.7%, 74.1% 44.1%)",
            }}
          />
        </div>
      </div>
    </div>
  );
}

function Features() {
  const features = [
    {
      name: "Track with confidence.",
      description:
        "Stay motivated and accountable with our intuitive habit tracking system, designed to help you stay on course and celebrate your achievements.",
      icon: CloudArrowUpIcon,
    },
    {
      name: "Secure and reliable.",
      description:
        "Rest easy knowing your data is protected with SSL certificates and regular database backups. Focus on your habits while we handle the technical details.",
      icon: LockClosedIcon,
    },
    {
      name: "Supercharge your productivity.",
      description:
        "Experience a streamlined workflow with our push-to-deploy feature, ensuring your progress is always up to date and accessible wherever you go.",
      icon: ServerIcon,
    },
  ];
  return (
    <div className="overflow-hidden bg-white py-24 sm:py-32" id="features">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mx-auto grid max-w-2xl grid-cols-1 gap-x-8 gap-y-16 sm:gap-y-20 lg:mx-0 lg:max-w-none lg:grid-cols-2">
          <div className="lg:pr-8 lg:pt-4">
            <div className="lg:max-w-lg">
              <h2 className="text-base font-semibold leading-7 text-indigo-600">
                Achieve more
              </h2>
              <p className="mt-2 text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
                A better habit tracker
              </p>
              <p className="mt-6 text-lg leading-8 text-gray-600">
                Take control of your goals and make progress every day. Our app
                provides a seamless habit tracking experience, empowering you to
                build positive routines and reach new heights.
              </p>
              <dl className="mt-10 max-w-xl space-y-8 text-base leading-7 text-gray-600 lg:max-w-none">
                {features.map((feature) => (
                  <div key={feature.name} className="relative pl-9">
                    <dt className="inline font-semibold text-gray-900">
                      <feature.icon
                        className="absolute left-1 top-1 h-5 w-5 text-indigo-600"
                        aria-hidden="true"
                      />
                      {feature.name}
                    </dt>{" "}
                    <dd className="inline">{feature.description}</dd>
                  </div>
                ))}
              </dl>
            </div>
          </div>
          <Image
            src="/img/journal.png"
            alt="Product screenshot"
            className="w-[48rem] max-w-none rounded-xl shadow-xl ring-1 ring-gray-400/10 sm:w-[57rem] md:-ml-4 lg:-ml-0"
            width={2432}
            height={1442}
          />
        </div>
      </div>
    </div>
  );
}

function Pricing() {
  return (
    <div className="bg-white py-24 sm:py-32" id="pricing">
      <div className="mx-auto max-w-7xl px-6 lg:px-8">
        <div className="mx-auto max-w-2xl sm:text-center">
          <h2 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
            Simple no-tricks pricing
          </h2>
          <p className="mt-6 text-lg leading-8 text-gray-600">
            Unlock your full potential with our premium plan.
          </p>
        </div>
        <div className="mx-auto mt-16 max-w-2xl rounded-3xl sm:mt-10 lg:mx-0 lg:flex lg:max-w-none">
          <PricingCards></PricingCards>
        </div>
      </div>
    </div>
  );
}

function PricingCards() {
  return (
    <div className="relative xl:mx-auto">
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3 lg:gap-8">
        {/* Free Card */}
        <div>
          <div className="relative z-10 rounded-xl border bg-white p-4 dark:border-gray-700 dark:bg-slate-900 md:p-10">
            <h3 className="text-xl font-bold text-gray-800 dark:text-gray-200">
              Basic
            </h3>
            <div className="text-sm text-gray-500">
              Everything you need to get started.
            </div>

            <div className="mt-5">
              <span className="text-3xl font-bold text-gray-800 dark:text-gray-200">
                Free
              </span>
            </div>

            <div className="mt-5 grid gap-y-2 py-4 first:pt-0 last:pb-0 sm:grid-cols-1 sm:gap-x-6 sm:gap-y-0">
              <ul role="list" className="space-y-2 text-sm sm:text-base">
                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Basic goal and habit tracking
                  </span>
                </li>

                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Limited access to habit insights and progress tracking
                  </span>
                </li>

                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Standard customer support
                  </span>
                </li>
              </ul>
            </div>

            <div className="mt-5 grid grid-cols-2 gap-x-4 py-4 first:pt-0 last:pb-0">
              <div>
                <p className="text-sm text-gray-500">Cancel anytime.</p>
                <p className="text-sm text-gray-500">No card required.</p>
              </div>

              <div className="flex justify-end">
                <Link
                  type="button"
                  href="/login"
                  className="inline-flex items-center justify-center gap-2 rounded-md border bg-white px-4 py-3 align-middle text-sm font-medium text-gray-700 shadow-sm transition-all hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-600 focus:ring-offset-2 focus:ring-offset-white dark:border-gray-700 dark:bg-slate-900 dark:text-gray-400 dark:hover:bg-slate-800 dark:hover:text-white dark:focus:ring-offset-gray-800"
                >
                  Start free trial
                </Link>
              </div>
            </div>
          </div>
        </div>
        {/* Premium Card */}
        <div>
          <div className="relative z-10 rounded-xl border bg-white p-5 shadow-xl shadow-gray-200 dark:border-gray-700 dark:bg-slate-900 dark:shadow-gray-900/[.2] md:p-10">
            <h3 className="text-xl font-bold text-gray-800 dark:text-gray-200">
              Premium
            </h3>
            <div className="text-sm text-gray-500">For advanced features.</div>
            <span className="absolute right-0 top-0 rounded-bl-xl rounded-tr-xl bg-gray-800 px-3 py-1.5 text-xs font-medium text-white dark:bg-gray-900">
              Most popular
            </span>

            <div className="mt-5">
              <span className="text-6xl font-bold text-gray-800 dark:text-gray-200">
                $9
              </span>
              <span className="text-lg font-bold text-gray-800 dark:text-gray-200">
                .99
              </span>
              <span className="ml-3 text-gray-500">USD / monthly</span>
            </div>

            <div className="mt-5 grid gap-y-2 py-4 first:pt-0 last:pb-0 sm:grid-cols-1 sm:gap-x-6 sm:gap-y-0">
              <ul role="list" className="space-y-2 text-sm sm:text-base">
                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Advanced goal and habit tracking with unlimited entries
                  </span>
                </li>

                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Customizable dashboards and visualizations
                  </span>
                </li>

                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Integration with popular productivity tools
                  </span>
                </li>
              </ul>
            </div>

            <div className="mt-5 grid grid-cols-2 gap-x-4 py-4 first:pt-0 last:pb-0">
              <div>
                <p className="text-sm text-gray-500">Cancel anytime.</p>
                <p className="text-sm text-gray-500">No card required.</p>
              </div>

              <div className="flex justify-end">
                <Link
                  type="button"
                  href="/login"
                  className="inline-flex items-center justify-center gap-2 rounded-md border border-transparent bg-blue-500 px-4 py-3 text-sm font-semibold text-white transition-all hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
                >
                  Start free trial
                </Link>
              </div>
            </div>
          </div>
        </div>
        {/* Enterpise Card */}
        <div>
          <div className="relative z-10 rounded-xl border bg-white p-4 dark:border-gray-700 dark:bg-slate-900 md:p-10">
            <h3 className="text-xl font-bold text-gray-800 dark:text-gray-200">
              Enterprise
            </h3>
            <div className="text-sm text-gray-500">
              Everything a business team needs.
            </div>

            <div className="mt-5">
              <span className="text-3xl font-bold text-gray-800 dark:text-gray-200">
                Contact us
              </span>
            </div>
            <div className="mt-5 grid gap-y-2 py-4 first:pt-0 last:pb-0 sm:grid-cols-1 sm:gap-x-6 sm:gap-y-0">
              <ul role="list" className="space-y-2 text-sm sm:text-base">
                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Team collaboration and management capabilities
                  </span>
                </li>

                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Enhanced data security and privacy measures
                  </span>
                </li>

                <li className="flex space-x-3">
                  <svg
                    className="h-5 w-5 flex-shrink-0 text-blue-500"
                    width="18"
                    height="18"
                    viewBox="0 0 18 18"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <rect
                      width="18"
                      height="18"
                      rx="9"
                      fill="currentColor"
                      fill-opacity="0.1"
                    />
                    <path
                      d="M12.0603 5.78792C12.2511 5.56349 12.5876 5.5362 12.8121 5.72697C13.0365 5.91774 13.0638 6.25432 12.873 6.47875L8.3397 11.8121C8.14594 12.04 7.80261 12.064 7.57901 11.8653L5.17901 9.73195C4.95886 9.53626 4.93903 9.19915 5.13472 8.979C5.33041 8.75885 5.66751 8.73902 5.88766 8.93471L7.88011 10.7058L12.0603 5.78792Z"
                      fill="currentColor"
                    />
                  </svg>
                  <span className="text-gray-800 dark:text-gray-200">
                    Customizable branding and white-label options
                  </span>
                </li>
              </ul>
            </div>

            <div className="mt-5 grid grid-cols-2 gap-x-4 py-4 first:pt-0 last:pb-0">
              <div>
                <p className="text-sm text-gray-500">Cancel anytime.</p>
                <p className="text-sm text-gray-500">No card required.</p>
              </div>

              <div className="flex justify-end">
                <Link
                  type="button"
                  href="/login"
                  className="inline-flex items-center justify-center gap-2 rounded-md border bg-white px-4 py-3 align-middle text-sm font-medium text-gray-700 shadow-sm transition-all hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-600 focus:ring-offset-2 focus:ring-offset-white dark:border-gray-700 dark:bg-slate-900 dark:text-gray-400 dark:hover:bg-slate-800 dark:hover:text-white dark:focus:ring-offset-gray-800"
                >
                  Start free trial
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function CTA() {
  return (
    <div className="bg-white">
      <div className="mx-auto max-w-7xl py-24 sm:px-6 sm:py-32 lg:px-8">
        <div className="relative isolate overflow-hidden bg-gray-900 px-6 pt-16 shadow-2xl sm:rounded-3xl sm:px-16 md:pt-24 lg:flex lg:gap-x-20 lg:px-24 lg:pt-0">
          <svg
            viewBox="0 0 1024 1024"
            className="absolute left-1/2 top-1/2 -z-10 h-[64rem] w-[64rem] -translate-y-1/2 [mask-image:radial-gradient(closest-side,white,transparent)] sm:left-full sm:-ml-80 lg:left-1/2 lg:ml-0 lg:-translate-x-1/2 lg:translate-y-0"
            aria-hidden="true"
          >
            <circle
              cx={512}
              cy={512}
              r={512}
              fill="url(#759c1415-0410-454c-8f7c-9a820de03641)"
              fillOpacity="0.7"
            />
            <defs>
              <radialGradient id="759c1415-0410-454c-8f7c-9a820de03641">
                <stop stopColor="#7775D6" />
                <stop offset={1} stopColor="#E935C1" />
              </radialGradient>
            </defs>
          </svg>
          <div className="mx-auto max-w-md text-center lg:mx-0 lg:flex-auto lg:py-32 lg:text-left">
            <h2 className="text-3xl font-bold tracking-tight text-white sm:text-4xl">
              Boost your productivity.
              <br />
              Start using our app today.
            </h2>
            <p className="mt-6 text-lg leading-8 text-gray-300">
              Accelerate your workflow and achieve more with ease. Seamlessly
              manage your tasks, track progress, and stay organized.
            </p>
            <div className="mt-10 flex items-center justify-center gap-x-6 lg:justify-start">
              <Link
                href="/login"
                className="rounded-md bg-white px-3.5 py-2.5 text-sm font-semibold text-gray-900 shadow-sm hover:bg-gray-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
              >
                Get started
              </Link>
            </div>
          </div>
          <div className="relative mt-16 h-80 lg:mt-8">
            <Image
              className="absolute left-0 top-0 w-[34rem] max-w-none rounded-md bg-white/5 ring-1 ring-white/10"
              src="/img/overview.png"
              alt="App screenshot"
              width={1824}
              height={1080}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function Team() {
  const people = [
    {
      name: "Brian Ha",
      role: "Product Manager",
      desc: `bha6@uci.edu`,
      link: "https://www.linkedin.com/in/brian-ha/",
      imageUrl: "img/bh.jpg",
    },
    {
      name: "Sumeet Padalava",
      role: "Principle Software Engineer",
      desc: `padavals@uci.edu`,
      link: "https://www.linkedin.com/in/sumeet-padavala/",
      imageUrl: "img/sp.jpg",
    },
    {
      name: "Sabina Yang",
      role: "Backend Software Engineer",
      desc: `sabinay@uci.edu`,
      link: "https://www.linkedin.com/in/sabinafly23/",
      imageUrl: "img/sy.jpg",
    },
  ];
  return (
    <div className="bg-white py-24 sm:py-32" id="team">
      <div className="mx-auto grid max-w-7xl gap-x-8 gap-y-20 px-6 lg:px-8 xl:grid-cols-3">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
            Meet the team
          </h2>
          <p className="mt-6 text-lg leading-8 text-gray-600">
            Our team comprises of diverse individuals eager to make a difference
            in the lives of others.
          </p>
        </div>
        <ul
          role="list"
          className="grid gap-x-8 gap-y-12 sm:grid-cols-3 sm:gap-y-16 xl:col-span-2"
        >
          {people.map((person) => (
            <li key={person.name}>
              <div className="flex items-center gap-x-6">
                <a href={person.link}>
                  <img
                    className="h-16 w-16 rounded-full"
                    src={person.imageUrl}
                    alt=""
                  />
                </a>
                <div>
                  <h3 className="text-base font-semibold leading-7 tracking-tight text-gray-900">
                    {person.name}
                  </h3>
                  <p className="text-sm font-semibold leading-6 text-indigo-600">
                    {person.role}
                  </p>
                  <p className="text-sm font-semibold leading-6 text-gray-500">
                    {person.desc}
                  </p>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function Footer() {
  return (
    <section className="bg-white">
      <div className="mx-auto max-w-screen-xl space-y-8 overflow-hidden px-4 py-12 sm:px-6 lg:px-8">
        <nav className="-mx-5 -my-2 flex flex-wrap justify-center">
          <div className="px-5 py-2">
            <a
              href="#"
              className="text-base leading-6 text-gray-500 hover:text-gray-900"
            >
              Home
            </a>
          </div>
          <div className="px-5 py-2">
            <a
              href="#features"
              className="text-base leading-6 text-gray-500 hover:text-gray-900"
            >
              Features
            </a>
          </div>
          <div className="px-5 py-2">
            <a
              href="#pricing"
              className="text-base leading-6 text-gray-500 hover:text-gray-900"
            >
              Pricing
            </a>
          </div>
          <div className="px-5 py-2">
            <a
              href="#team"
              className="text-base leading-6 text-gray-500 hover:text-gray-900"
            >
              Team
            </a>
          </div>
        </nav>
        <div className="mt-8 flex justify-center space-x-6">
          {/* <a href="#" className="text-gray-400 hover:text-gray-500">
            <span className="sr-only">Facebook</span>
            <svg className="w-6 h-6" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24">
              <path fill-rule="evenodd" d="M22 12c0-5.523-4.477-10-10-10S2 6.477 2 12c0 4.991 3.657 9.128 8.438 9.878v-6.987h-2.54V12h2.54V9.797c0-2.506 1.492-3.89 3.777-3.89 1.094 0 2.238.195 2.238.195v2.46h-1.26c-1.243 0-1.63.771-1.63 1.562V12h2.773l-.443 2.89h-2.33v6.988C18.343 21.128 22 16.991 22 12z" clip-rule="evenodd"></path>
            </svg>
          </a>
          <a href="#" className="text-gray-400 hover:text-gray-500">
            <span className="sr-only">Instagram</span>
            <svg className="w-6 h-6" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24">
              <path fill-rule="evenodd" d="M12.315 2c2.43 0 2.784.013 3.808.06 1.064.049 1.791.218 2.427.465a4.902 4.902 0 011.772 1.153 4.902 4.902 0 011.153 1.772c.247.636.416 1.363.465 2.427.048 1.067.06 1.407.06 4.123v.08c0 2.643-.012 2.987-.06 4.043-.049 1.064-.218 1.791-.465 2.427a4.902 4.902 0 01-1.153 1.772 4.902 4.902 0 01-1.772 1.153c-.636.247-1.363.416-2.427.465-1.067.048-1.407.06-4.123.06h-.08c-2.643 0-2.987-.012-4.043-.06-1.064-.049-1.791-.218-2.427-.465a4.902 4.902 0 01-1.772-1.153 4.902 4.902 0 01-1.153-1.772c-.247-.636-.416-1.363-.465-2.427-.047-1.024-.06-1.379-.06-3.808v-.63c0-2.43.013-2.784.06-3.808.049-1.064.218-1.791.465-2.427a4.902 4.902 0 011.153-1.772A4.902 4.902 0 015.45 2.525c.636-.247 1.363-.416 2.427-.465C8.901 2.013 9.256 2 11.685 2h.63zm-.081 1.802h-.468c-2.456 0-2.784.011-3.807.058-.975.045-1.504.207-1.857.344-.467.182-.8.398-1.15.748-.35.35-.566.683-.748 1.15-.137.353-.3.882-.344 1.857-.047 1.023-.058 1.351-.058 3.807v.468c0 2.456.011 2.784.058 3.807.045.975.207 1.504.344 1.857.182.466.399.8.748 1.15.35.35.683.566 1.15.748.353.137.882.3 1.857.344 1.054.048 1.37.058 4.041.058h.08c2.597 0 2.917-.01 3.96-.058.976-.045 1.505-.207 1.858-.344.466-.182.8-.398 1.15-.748.35-.35.566-.683.748-1.15.137-.353.3-.882.344-1.857.048-1.055.058-1.37.058-4.041v-.08c0-2.597-.01-2.917-.058-3.96-.045-.976-.207-1.505-.344-1.858a3.097 3.097 0 00-.748-1.15 3.098 3.098 0 00-1.15-.748c-.353-.137-.882-.3-1.857-.344-1.023-.047-1.351-.058-3.807-.058zM12 6.865a5.135 5.135 0 110 10.27 5.135 5.135 0 010-10.27zm0 1.802a3.333 3.333 0 100 6.666 3.333 3.333 0 000-6.666zm5.338-3.205a1.2 1.2 0 110 2.4 1.2 1.2 0 010-2.4z" clip-rule="evenodd"></path>
            </svg>
          </a>
          <a href="#" className="text-gray-400 hover:text-gray-500">
            <span className="sr-only">Twitter</span>
            <svg className="w-6 h-6" aria-hidden="true" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8.29 20.251c7.547 0 11.675-6.253 11.675-11.675 0-.178 0-.355-.012-.53A8.348 8.348 0 0022 5.92a8.19 8.19 0 01-2.357.646 4.118 4.118 0 001.804-2.27 8.224 8.224 0 01-2.605.996 4.107 4.107 0 00-6.993 3.743 11.65 11.65 0 01-8.457-4.287 4.106 4.106 0 001.27 5.477A4.072 4.072 0 012.8 9.713v.052a4.105 4.105 0 003.292 4.022 4.095 4.095 0 01-1.853.07 4.108 4.108 0 003.834 2.85A8.233 8.233 0 012 18.407a11.616 11.616 0 006.29 1.84"></path>
            </svg>
          </a> */}
          <a
            href="https://github.com/Quantified-Destiny/life-on-rails"
            className="text-gray-400 hover:text-gray-500"
          >
            <span className="sr-only">GitHub</span>
            <svg
              className="h-6 w-6"
              aria-hidden="true"
              fill="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                fillRule="evenodd"
                d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                clipRule="evenodd"
              ></path>
            </svg>
          </a>
        </div>
        <p className="mt-8 text-center text-base leading-6 text-gray-400">
          © 2023 Hybrid Studios, Inc. All rights reserved.
        </p>
      </div>
    </section>
  );
}

const Home: NextPage = () => {
  // const hello = api.example.hello.useQuery({ text: "from tRPC" });

  return (
    <>
      <Head>
        <title>Life on Rails</title>
        <meta name="description" content="" />
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <Hero></Hero>
      <Features></Features>
      <Pricing></Pricing>
      <CTA></CTA>
      <Team></Team>
      <Footer></Footer>
    </>
  );
};

export default Home;
