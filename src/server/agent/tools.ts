// Model-facing tool definitions (spec: Agent tools). Read tools return
// IDs + summaries. Write tools are confirm-gated; create_calendar_event
// additionally requires user_profile.outbound_enabled (default false).
export const AGENT_TOOLS = [
  { name: "search_observations", sideEffect: false, gate: "none" },
  { name: "get_day", sideEffect: false, gate: "none" },
  { name: "get_graph", sideEffect: false, gate: "none" },
  { name: "explain_score", sideEffect: false, gate: "none" },
  { name: "draft_day_plan", sideEffect: "proposal", gate: "none" },
  { name: "propose_link", sideEffect: "proposal", gate: "none" },
  { name: "apply_commitment", sideEffect: "commitment", gate: "confirm" },
  { name: "reset_visibility", sideEffect: "tasks→later", gate: "confirm" },
  { name: "create_calendar_event", sideEffect: "gog calendar create", gate: "confirm+outbound_enabled" },
] as const;

export type AgentToolName = (typeof AGENT_TOOLS)[number]["name"];

export interface ToolCallRequest {
  callId: string;
  tool: AgentToolName;
  args: Record<string, unknown>;
}

/** Confirm gate: write tools need an explicit per-call accept. */
export function requiresConfirm(tool: AgentToolName): boolean {
  const t = AGENT_TOOLS.find((x) => x.name === tool);
  return !!t && t.gate !== "none";
}

export function requiresOutbound(tool: AgentToolName, outboundEnabled: boolean): boolean {
  if (tool !== "create_calendar_event") return false;
  return !outboundEnabled;
}
