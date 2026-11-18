// What the comparison page says (lor-glev). build.mjs turns it into index.html.
// Facts are from docs/design/lab/brief.md and the concepts' item Reports. Quotes are the
// critics' own words from ../critiques/, and build.mjs fails on any that is not word for word.

export const CRITICS = {
  "gpt-6-astra": { name: "GPT-6 Astra" },
  "grok-4.6": { name: "Grok 4.6" },
  "kimi-k3": {
    name: "Kimi K3",
    open: "It was asked for the read from a model made outside the US, and it is still owed. On 17 November OpenCode Go's 5-hour usage limit stopped it partway, with nothing written, and OpenCode's pay-as-you-go balance refused it; on 18 November, after the 5-hour window reset, Go's weekly limit stopped it again."
  },
};

export const PAGE = {
  title: "Pick a direction",
  intro: [
    "Four ways to organise the dashboard, the launcher and the app, each built on the same made-up day: Tuesday 17 November at 15:00, a timer 47 minutes in, one T3 approval, one question and two flags. **Use them in the viewers before you read the critics.**",
    "Two models from two labs, neither of which built any of them, used each one and wrote what it gets right, what would annoy, what it makes harder than today, and the one idea worth stealing. Their words are quoted as written; their full notes are under each concept.",
    "A third seat, Kimi K3's, is open; the read from a model made outside the US is still owed. Pick one, or a mix, in item `lor-dinh`. The rounded corners, transparency, separation by depth and the type scale are common ground, so they are not what to choose between.",
  ],
  defects: [
    "The critics were asked for any defect that breaks one of the interactions the brief requires. Astra found none in what it used. Grok 4.6 reported one: on the inner screen in Timeline, Panes and Context, the Approve bar seemed to cover the start of a long thread.",
    "It did not hold. The thread opens at its newest message, as a chat does, so the message at the top is cut by the scroll, not by the bar. Scrolled up in the published viewer on 18 November, all three show step 1 in full beneath the header. Queue's still happened to stop on a whole message. Nothing was changed, and Grok's notes stand as written.",
  ],
  footer: "Made by lor-glev from `docs/design/lab/compare/` and `docs/design/lab/critiques/`. The brief is `docs/design/lab/brief.md`.",
};

export const CONCEPTS = [
  {
    id: "queue",
    name: "Queue",
    sentence: "One ranked list of what wants you, read top to bottom and cleared.",
    facts: [
      ["Leads with", "The one thing as a large card on the dashboard; home's icons on the phone, with the queue one swipe left."],
      ["Timer", "A bar at the top of Today, and a line on home."],
      ["Details open", "In place: a row expands in the column; a card opens to a sheet on the phone."],
      ["Needs from LoR", "Nothing new: it draws what LoR sends the phone today."],
    ],
    for: [
      {"by": "gpt-6-astra", "text": "In Today, afternoon, at 1500 and 412, “Find a physio for the left knee” is unmistakably the thing to read.", "on": "dashboard"},
      {"by": "gpt-6-astra", "text": "Home/demo leaves the icons and search unobstructed; the single timer line is exceptionally economical.", "on": "phone"},,
      {"by": "grok-4.6", "text": "Quiet at 1500 is the most beautiful empty of the four: \"ALL CLEAR / Nothing wants you.\"", "on": "dashboard"},
      {"by": "grok-4.6", "text": "Compared with current's cover, where a huge TODAY widget eats the grid and the icons are grey discs, this is the first home that looks like a phone.", "on": "phone"}
    ],
    against: [
      {"by": "gpt-6-astra", "text": "Queue's confident recommendation is now competing with my actual work.", "on": "dashboard"},
      {"by": "gpt-6-astra", "text": "“11 waiting” recruits every unlock into clearing a backlog.", "on": "phone"},,
      {"by": "grok-4.6", "text": "You will know the dripping tap is four days late before you know you have an hour and a half until the dentist.", "on": "dashboard"},
      {"by": "grok-4.6", "text": "You came to open WhatsApp and the phone told you the bathroom tap is late.", "on": "phone"}
    ],
    steal: [
      {"by": "gpt-6-astra", "text": "Give Context Queue's inline single-choice answers with descriptions.", "on": "phone"},,
      {"by": "grok-4.6", "text": "Inline question options on the cover card (MapTiler / Stadia / Self-host / Other), into Timeline.", "on": "phone"}
    ],
  },
  {
    id: "timeline",
    name: "Timeline",
    sentence: "The day is the spine: a route from waking to sleep, with now a third of the way down.",
    facts: [
      ["Leads with", "Now: the running block, the time left, the next event."],
      ["Timer", "The now line itself, and the day strip on home."],
      ["Details open", "In a popover on the route; a bottom sheet at phone width."],
      ["Needs from LoR", "The day's recorded blocks sent to the phone. `/device/home` sends events and the timer, not the blocks before now."],
    ],
    for: [
      {"by": "gpt-6-astra", "text": "This is a more actionable version of current's attractive but comparatively remote arc.", "on": "dashboard"},
      {"by": "gpt-6-astra", "text": "Home/demo is my favourite glance at the day: next event, time until it, remaining day and timer share one calm card.", "on": "phone"},,
      {"by": "grok-4.6", "text": "Current's arc is the same information as jewellery. This is the same information as a page.", "on": "dashboard"},
      {"by": "grok-4.6", "text": "A clear queue does not erase the day. I like that.", "on": "phone"}
    ],
    against: [
      {"by": "gpt-6-astra", "text": "The report's worry is visible: an automatic account of the day starts to resemble homework.", "on": "dashboard"},
      {"by": "gpt-6-astra", "text": "The very concept about “now” makes me scroll to now.", "on": "dashboard"},,
      {"by": "grok-4.6", "text": "Until you hit Confirm all, the route is a questionnaire.", "on": "dashboard"},
      {"by": "grok-4.6", "text": "The cover is still a lot of LoR for a phone you unlock to open Gmail.", "on": "phone"}
    ],
    steal: [
      {"by": "gpt-6-astra", "text": "Give Queue Timeline's home summary of time remaining and the next fixed event.", "on": "phone"},,
      {"by": "grok-4.6", "text": "Confirm all 8, into Queue and into Context.", "on": "dashboard"}
    ],
  },
  {
    id: "panes",
    name: "Panes",
    sentence: "A list and its detail side by side, so opening something never takes you away.",
    facts: [
      ["Leads with", "The list of the page you are on."],
      ["Timer", "A row at the top of Today's list, and a chip in the nav that stops or switches it from any page."],
      ["Details open", "In the right pane on the desktop and the inner screen; in a sheet on the cover screen."],
      ["Needs from LoR", "Nothing new: it draws what LoR sends the phone today."],
    ],
    for: [
      {"by": "gpt-6-astra", "text": "Panes is the best foundation for an instrument you sit down to use."},
      {"by": "gpt-6-astra", "text": "The named Agent/Home/Today switcher is far more discoverable than current's page dots.", "on": "phone"},,
      {"by": "grok-4.6", "text": "It is the one that feels like software you already know how to operate."},
      {"by": "grok-4.6", "text": "Panes is the one that made failure look designed.", "on": "phone"}
    ],
    against: [
      {"by": "gpt-6-astra", "text": "That is consistency becoming repetition: the layout looks settled before it has earned the division of space.", "on": "dashboard"},
      {"by": "gpt-6-astra", "text": "I want more transcript and less repeated scaffolding.", "on": "phone"},,
      {"by": "grok-4.6", "text": "You are in Queue with extra chrome.", "on": "dashboard"},
      {"by": "grok-4.6", "text": "Quiet Today's people list will be the identity of a clear afternoon.", "on": "phone"}
    ],
    steal: [
      {"by": "gpt-6-astra", "text": "Give Queue Panes' search field immediately above the keyboard with app results just above it.", "on": "phone"},,
      {"by": "grok-4.6", "text": "Search at the thumb with apps nearest the field, into Queue and Timeline.", "on": "phone"}
    ],
  },
  {
    id: "context",
    name: "Context",
    sentence: "Everything about what you are doing now, gathered in one place: its tasks, page, people, agent work and time.",
    facts: [
      ["Leads with", "The current context, chosen by the running timer or the agent's one thing."],
      ["Timer", "The context's own header; starting a timer on something makes it the context."],
      ["Details open", "Within the context: a task in place, a page or thread in the right half."],
      ["Needs from LoR", "Goals, pages, week hours, task estimates and the agent's suggested links sent to the phone, and the agent recording which context a T3 thread belongs to. The prototypes make these up by title."],
    ],
    for: [
      {"by": "gpt-6-astra", "text": "Context is the direction I most want LoR to grow into."},
      {"by": "gpt-6-astra", "text": "That is a materially better answer to “help me resume” than current's separate lists.", "on": "dashboard"},,
      {"by": "grok-4.6", "text": "This is the organisation LoR actually has, and the dashboard is the first time I have seen it drawn.", "on": "dashboard"},
      {"by": "grok-4.6", "text": "This one is the one I would actually use, because the next task is ticked from home.", "on": "phone"}
    ],
    against: [
      {"by": "gpt-6-astra", "text": "Its danger is that keeping the meaning organized becomes another job."},
      {"by": "gpt-6-astra", "text": "The design promises concentration but keeps advertising other obligations.", "on": "dashboard"},,
      {"by": "grok-4.6", "text": "You will scroll inside the thing you are supposed to be in.", "on": "dashboard"},
      {"by": "grok-4.6", "text": "You came to approve a migration and you are holding a project.", "on": "phone"}
    ],
    steal: [
      {"by": "gpt-6-astra", "text": "Give Queue Context's one directly tickable home task.", "on": "phone"},,
      {"by": "grok-4.6", "text": "Next context plus \"fits before it\", into Timeline.", "on": "dashboard"}
    ],
  },
];

export const COUNT = {
  title: "11 waiting, or 4?",
  question: "On the same demo data, Queue's home says 11 waiting: T3's cards, LoR's flags and the tasks due today. Timeline's and Context's say 4: T3's cards and LoR's flags, without the tasks. Each critic was asked which count Sumeet would want on home.",
  answers: [
    {"by": "gpt-6-astra", "text": "I would put **4 waiting** on home."},
    {"by": "gpt-6-astra", "text": "On a launcher, “waiting” should mean that opening it will reveal something requesting a decision or acknowledgement."},,
    {"by": "grok-4.6", "text": "Sumeet wants **4** on home."},
    {"by": "grok-4.6", "text": "A home chip that says 11 every unlock will be ignored, and then the 4 that matter will be ignored with it."},
    {"by": "grok-4.6", "text": "Panes' split is the one I would ship: Agent 4 on the switcher, Today 10 one tap away, home still a grid."}
  ],
};

export const BASELINE = {
  title: "Current, for comparison",
  text: "What ships today, in the same four screens and at the same moment.",
};
