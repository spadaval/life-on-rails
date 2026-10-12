import Image from "next/image";
import Link from "next/link";

const TopNav = () => {
  return (
    <nav
      className="
 relative
  flex
  h-full
  "
    >
      <div className="flex w-full flex-wrap items-center justify-between pr-6 shadow">
        <button
          className="
      navbar-toggler
      border-0
      bg-transparent
      px-2.5 py-2
      text-gray-500
      hover:no-underline
      hover:shadow-none
      focus:no-underline focus:shadow-none focus:outline-none focus:ring-0
    "
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarSupportedContent"
          aria-controls="navbarSupportedContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <Image src="/lor-logo.png" className="w-9" alt="" />
          {/* <svg
            aria-hidden="true"
            focusable="false"
            data-prefix="fas"
            data-icon="bars"
            className="w-6"
            role="img"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 448 512"
          >
            <path
              fill="currentColor"
              d="M16 132h416c8.837 0 16-7.163 16-16V76c0-8.837-7.163-16-16-16H16C7.163 60 0 67.163 0 76v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16zm0 160h416c8.837 0 16-7.163 16-16v-40c0-8.837-7.163-16-16-16H16c-8.837 0-16 7.163-16 16v40c0 8.837 7.163 16 16 16z"
            ></path>
          </svg> */}
        </button>
        <div className="text-sm  text-blue-600">
          {/* text-[#21A8F6] */}
          Life on Rails
        </div>
        <div className="flex-grow items-center" id="navbarSupportedContent">
          <a
            className="
        mr-1
        mt-2
        flex
        items-center
        text-gray-900
        hover:text-gray-900
        focus:text-gray-900
        lg:mt-0
      "
            href="#"
          ></a>
        </div>

        <div className="relative flex items-center">
          <Link
            className="
        dropdown-item
        block
        w-full
        whitespace-nowrap
        bg-transparent
        px-4
        py-2
        text-sm
        font-normal
        text-gray-700
        hover:bg-gray-100
      "
            href="/profile"
          >
            My Profile
          </Link>
        </div>
      </div>
    </nav>
  );
};

export default TopNav;
