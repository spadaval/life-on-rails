import { addDays, subDays } from "date-fns";
import { ChevronLeft, ChevronRight } from "lucide-react";

import dynamic from "next/dynamic";
import "react-datepicker/dist/react-datepicker.css";
//import DatePicker from "react-datepicker";
// defer loading the date picker js
const DatePicker = dynamic(() => import("react-datepicker"), { ssr: false });

interface TimePickerProps {
  date: Date;
  setDate: (date: Date) => void;
}

const today = new Date();
const maxDate = new Date(
  today.getFullYear(),
  today.getMonth(),
  today.getDate()
);

const TimePicker = ({ date, setDate }: TimePickerProps) => {
  const handlePrevDay = () => {
    setDate(subDays(date, 1));
  };

  const handleNextDay = () => {
    setDate(addDays(date, 1));
  };

  const showNextDayButton = date.toDateString() !== maxDate.toDateString();
  return (
    <div
      id="time-selector"
      className="mt-3 flex h-10 w-full flex-col items-center pt-1"
    >
      <div id="selector-controls" className="flex items-center">
        <button onClick={handlePrevDay} className="bg-white bg-opacity-20 px-2">
          <ChevronLeft></ChevronLeft>
        </button>
        <DatePicker
          selected={date}
          onChange={(date) => date instanceof Date && setDate(date)}
          maxDate={maxDate}
          className="w-60 text-center"
          dateFormat="EEEE, MMMM dd, yyyy"
        />

        <button
          onClick={handleNextDay}
          className={`bg-white bg-opacity-20 px-2 ${
            date >= maxDate ? "cursor-default opacity-50" : ""
          }`}
          disabled={date >= maxDate}
        >
          <ChevronRight
            className={
              date >= maxDate
                ? "cursor-not-allowed stroke-[#ccc]"
                : "stroke-[#000]"
            }
          ></ChevronRight>
        </button>
      </div>
    </div>
  );
};

export default TimePicker;
