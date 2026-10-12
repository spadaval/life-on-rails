import type { Metric } from "@prisma/client";
import { Dialog, DialogContent } from "../components/ui/dialog";
import { HabitIcon, MetricIcon } from "../components/ui/icons";
import { Slider } from "../components/ui/slider";

function MetricModal({
  open,
  onOpenChange,
  metrics,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  metrics?: Metric[];
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <div className="mx-auto mt-10 w-10/12">
          <div className="flex items-center gap-5 bg-gray-100 px-4 py-7 md:px-8 md:py-4">
            <HabitIcon/>
            <p className="text-base font-semibold">Go jogging for an hour.</p>
          </div>
          <div className="px-4 pb-7 pt-6 md:px-10 md:pb-4 md:pt-12">
            <div className="flex items-center justify-center  text-sm text-gray-400">
              <h2>Answer your linked metrics to finish the habit.</h2>
            </div>

            <form className="mt-11">
              <div className="flex items-center">
                <MetricIcon/>
                <p className="w-1/2 px-3 py-3 text-sm leading-none text-gray-800 focus:outline-none ">
                  How was your jog?
                </p>
                {/* <input placeholder="Full Name" className=" w-1/2 focus:outline-none placeholder-gray-500 py-3 px-3 text-sm leading-none text-gray-800 bg-white border rounded border-gray-200" /> */}
                <Slider
                  value={[0]}
                  min={0}
                  max={5}
                  step={1}
                  className="w-1/2"
                ></Slider>
              </div>
              <div className="flex items-center">
                <MetricIcon/>
                <p className=" w-1/2 px-3  py-3 text-sm leading-none text-gray-800 focus:outline-none ">
                  How long was your jog How long was your jog ?
                </p>
                {/* <input placeholder="Full Name" className=" w-1/2 focus:outline-none placeholder-gray-500 py-3 px-3 text-sm leading-none text-gray-800 bg-white border rounded border-gray-200" /> */}
                <Slider
                  value={[0]}
                  min={0}
                  max={5}
                  step={1}
                  className="w-1/2"
                ></Slider>
              </div>
              {/* <div className="flex items-center space-x-9 mt-8">
                                        <input placeholder="Email" type="email" className="w-1/2 focus:outline-none placeholder-gray-500 py-3 px-3 text-sm leading-none text-gray-800 bg-white border rounded border-gray-200" />
                                        <div className="w-1/2 bg-white border rounded border-gray-200 py-2.5 px-3">
                                            <select className="text-sm text-gray-500 w-full focus:outline-none">
                                                <option selected disabled value>
                                                    Category
                                                </option>
                                                <option>Designer</option>
                                                <option>Developer</option>
                                            </select>
                                        </div>
                                    </div> */}
              <div className="mt-8">
                <textarea
                  placeholder="Habit Note"
                  className="h-24 w-full resize-none overflow-y-auto rounded border border-gray-200 py-3 pl-3 focus:outline-none"
                  defaultValue={""}
                />
              </div>
            </form>
            <div className="mt-9 flex items-center justify-between">
              <button className="rounded bg-gray-400 px-6 py-3 text-sm text-white shadow hover:bg-gray-500">
                Cancel
              </button>
              <button className="rounded bg-indigo-700 px-6 py-3 text-sm text-white shadow hover:bg-opacity-80">
                Done
              </button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default MetricModal;
