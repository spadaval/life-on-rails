import { SignUp } from "@clerk/nextjs";

export default function Page() {
  return (
    <div className="flex h-screen w-screen items-center justify-center bg-gradient-to-b from-[#5ef4ca5a] to-[#4389faab]">
      <SignUp redirectUrl="/journal" />;
    </div>
  );
}
