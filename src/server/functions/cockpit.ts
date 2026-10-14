import { createServerFn } from "@tanstack/react-start";
import { getStatus } from "../status.js";

export const getCaptureStatus = createServerFn({ method: "GET" }).handler(async () => getStatus());
