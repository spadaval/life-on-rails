# The count on home, and defects: the critics' notes

Item lor-glev, 17 and 18 November 2026. Each critic was given docs/design/lab/brief.md, both galleries, the gesture recordings and the live viewers, without seeing the others' notes, and says in its notes what it used and what it saw only as a still. GPT-6 Astra's first notes did not use the recordings; at review it was asked to watch them, and its addendum on motion follows its first notes. None of them built a concept. Each critic's notes are below its marker exactly as it wrote them; the comparison page (docs/design/lab/compare/) quotes from them. GPT-6 Astra and Grok 4.6 wrote here; the third seat, Kimi K3, is open because OpenCode Go's usage limits and OpenCode's balance refused it on 17 and 18 November.

<!-- critic: gpt-6-astra -->

## GPT-6 Astra (Codex, gpt-6-astra, reasoning effort high)

I would put **4 waiting** on home. This is my recommendation from the stated aim, not a claim that Sumeet has already chosen it.

Queue's 11 combines the four attention items with seven ordinary due tasks. Its queue code explicitly appends due tasks to T3 cards and LoR flags. Timeline and Context's four describes the smaller set asking for attention. Both numbers can be arithmetically defensible; they make different promises.

On a launcher, “waiting” should mean that opening it will reveal something requesting a decision or acknowledgement. A bathroom tap becoming another day overdue has not created another interruption. Counting the backlog makes the badge sticky: it can remain large even after every agent has been unblocked. Eventually it teaches the owner not to look. Four preserves the usefulness of that signal. Keep the seven tasks reachable in Today, and allow one chosen task on home as Context does. Do not hide them to manufacture calm, or label the four “all tasks”.

The different count in agent-waiting is not contradictory evidence: that is another fixture. The stills there show Queue at 12 and Timeline/Panes at five. The comparison above is home/demo, as asked.

**Required-interaction defects encountered:** none confirmed in my limited hands-on sample.

I used Queue dashboard Today/afternoon → lead Done; the App Store item replaced the physio. I used Timeline dashboard Today/afternoon → Stop; the running Stop control cleared. I used Panes phone thread-long/inner → Fold → Unfold; the open migration thread returned in the inner detail pane. I did not type a draft or test scroll persistence, so that is not a pass for the full fold requirement. The other interaction claims in the implementation Reports are not my test results. I did not run the full required-interaction matrix, watch the gesture recordings, or operate a physical phone.

One evidence limitation: current's 1500px afternoon dashboard capture visibly repeats a band of the page. The reports identify repeated-band captures as a screenshot-tool problem. I used the intact upper portion for comparison and have not classified that as a product interaction defect.

GPT-6 Astra

<!-- critic: grok-4.6 -->

## Grok 4.6 (Grok, grok-4.6, reasoning effort high)

# The home count

Sumeet wants **4** on home.

4 is the T3 cards and the people-waiting that change what he does before he opens an app: an approval, a question, a failed turn, a flag the agent put in front of him. That is the count Timeline and Context put on the cover (Timeline: "4 waiting" on the day strip; Context: "4 waiting" next to Now). It is also Panes' Agent 4.

11 is Queue mixing those with due tasks (the dripping tap, the emergency fund, the crash, unit 8, the parking permit, the library books, release notes). Those are real. They do not belong on the launcher. 252 of 298 launches in the measured week were search and icons. A home chip that says 11 every unlock will be ignored, and then the 4 that matter will be ignored with it.

11 belongs on the queue page, where Queue already prints "11 for you". Panes' split is the one I would ship: Agent 4 on the switcher, Today 10 one tap away, home still a grid. Timeline's 4 on the strip is the same idea with more taste.

Stills are the source of truth for these numbers. The live dashboard store in this session had already taken physio to Done and confirmed eight guesses, so later live scenes showed smaller counts.

# Defects that break a required interaction

Only breaks of "Interactions that must work in every concept". Everything else stayed in the concept notes.

## 1. Read a thread on the inner screen: Approve bar covers the start of the transcript

- **Concepts:** Timeline, Panes, Context. Queue is clear.
- **Surface:** Phone, Fold inner screen, state `thread-long`.
- **Interaction:** "Read and reply to a thread on the inner screen."
- **Steps:** Open `thread-long` inner. Look at the right-hand transcript without scrolling.
- **What happened:** I saw this in stills, not as a live tap: `timeline-thread-long-inner@1000.png`, `panes-thread-long-inner@1000.png`, `context-thread-long-inner@1000.png`. Approve / Deny (and Open, on Timeline) sit on top of the first agent message. The first visible text is a clipped continuation ("so it runs in well under a second…", "column and an index…"). The reply field is present and a half-written draft survived Context's fold recording. Reading the beginning of the thread is what fails.
- **Queue, same state:** `queue-thread-long-inner@1000.png`. Approve / Deny / Open are in the thread header. Step 18 is fully readable. That is the working version of the interaction.

No other required interaction broke in what I used or in the stills I opened. Fold keeping a half-written answer held in Context's `lor-8aqh/gestures/fold/final.png`. Sync-fail was visible on Queue and Panes covers. Approve, search, and tick had working surfaces in the stills and in the dashboard live pass.

Grok 4.6
