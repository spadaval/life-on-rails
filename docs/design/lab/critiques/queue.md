# Queue: the critics' notes

Item lor-glev, 17 and 18 November 2026. Each critic was given docs/design/lab/brief.md, both galleries, the gesture recordings and the live viewers, without seeing the others' notes, and says in its notes what it used and what it saw only as a still. At review each critic was asked to watch the recordings its first notes had not used, and its addendum on motion follows its first notes. None of them built a concept. Each critic's notes are below its marker exactly as it wrote them; the comparison page (docs/design/lab/compare/) quotes from them. GPT-6 Astra and Grok 4.6 wrote here; the third seat, Kimi K3, is open because OpenCode Go's usage limits and OpenCode's balance refused it on 17 and 18 November.

<!-- critic: gpt-6-astra -->

## GPT-6 Astra (Codex, gpt-6-astra, reasoning effort high)

Queue has the strongest immediate hierarchy, and the greatest risk of making LoR feel like a debt collector. I like its restraint more than its ranking.

**Dashboard, what works.** In Today, afternoon, at 1500 and 412, “Find a physio for the left knee” is unmistakably the thing to read. The explanation and Done/Seen actions belong together. Compared with current's competing arc, brief and small controls, this is a substantial improvement. I used Done in the live viewer; “Reply to the App Store review notes” became the lead. That is an intelligible transition between jobs.

**Dashboard, daily irritation.** The afternoon lead is the physio while the timer says I am drafting the maps spec. Both get amber emphasis. Queue's confident recommendation is now competing with my actual work. “Then 8” makes a miscellaneous backlog sound like an ordered plan. The quiet typography of “was due” is welcome, but the page still spends nearly all its attention on unfinished things.

**Dashboard, harder than current.** Understanding the remaining day at a glance. Current's arc names the events; Queue's little strip offers anonymous shapes. At 412, I can read the physio's explanation without difficulty, but cannot read the upcoming calendar from the strip. The Tasks and Maya detail stills also show the price of generous inline expansion: very long lists. Current's dense task list is less pleasant, but faster to scan across many dates.

**Dashboard, steal.** Give Panes Queue's restrained “was due” metadata. It communicates age without lighting up an entire list of routine tasks as trouble.

**Phone, what works.** Home/demo leaves the icons and search unobstructed; the single timer line is exceptionally economical. In agent-waiting, the approval has a clear title and generous buttons, and the next question exposes the option descriptions directly. This is much easier to parse than current's narrow, similarly framed cards. The sync-failed home still also puts a recognizable repair entry in an everyday location.

**Phone, daily irritation.** “11 waiting” recruits every unlock into clearing a backlog. On agent-waiting, even a failed receipts run and ordinary tasks join the same procession. The reticle can make administrative clearance feel like the purpose of the phone. Search-mixed also puts the apps near the top, a long reach from the keyboard on this tall cover screen.

**Phone, harder than current.** Seeing the whole evening from home: current lists Dentist, Portuguese and Focus time; Queue names only Dentist. Its inner thread still is readable, but keeps a large, repeated approval card beside the same approval heading in the thread. The extra space has not entirely become extra context.

**Phone, steal.** Give Context Queue's inline single-choice answers with descriptions. The tile-provider question is answerable where it appears instead of opening another layer first.

Evidence: dashboard Today was used only for the lead Done transition. Dashboard Tasks at 1100, Today at 1500/412 and Maya at 1500 were stills. Phone home, agent-waiting, search-mixed, sync-failed and thread-long/inner were stills only; I did not test their gestures.

GPT-6 Astra

Queue's motion makes me warmer toward its single-list idea. It gives clearing work a readable rhythm, although I still dislike turning the whole backlog into a home badge.

**What it gets right on the phone.** In approve-agent-waiting, the approval stays put during the cancellation window. Its buttons become “Approving…” and Cancel, with a shrinking progress line. After roughly three seconds, the approval leaves and the tile-provider question rises into the reticle. The important continuity is where to look next: the same place. The 50 ms strip shows the handover around 3.3–3.5 seconds rather than an unexplained replacement of the entire page. This is a useful bit of personality, not just ornament.

The move-demo recording is also reassuring. Camera first has a recognizable menu, then lifts into a drag while neighbouring icons make room, and finishes with “Moved Camera” and Undo. Arranging home looks like manipulating objects in place. It no longer looks like the invisible mode described in the current-design audit.

**What would annoy daily.** The three-second approval window dominates an otherwise quick exchange. I understand the safety it buys, and Cancel is honest, but repeated approvals could feel like repeatedly waiting for a lift door to close. Once the card clears, everything below it moves too. That is excellent when working from the top and distracting when already reading the next question. The reticle's insistence is more palpable in motion than in a still.

**What becomes harder than current.** Queue's replacement of current's compact card overview with a procession makes comparing multiple requests less stable: their positions change as the lead clears. The fold recording also shows a cost that the final inner screenshot hides. The cover sheet drops away as the inner composition takes over; folding back briefly exposes the queue before the sheet returns. The draft survives, but the reading surface is reconstructed rather than visibly carried across. I have not reviewed an equivalent current fold recording, so I cannot claim a measured motion regression there.

**The idea to steal.** Give Panes Queue's handover from a completed lead item to the next actionable item. The spatial continuity matters more than the reticle itself. It would make clearing an agent queue feel purposeful without adding another page.

**Recordings reviewed.** From `lor-5fk6/gestures`: [approve-agent-waiting](/home/supadava/.local/state/lor-evidence/lor-5fk6/gestures/approve-agent-waiting/recording.webm), [move-demo](/home/supadava/.local/state/lor-evidence/lor-5fk6/gestures/move-demo/recording.webm), and [fold-thread-long](/home/supadava/.local/state/lor-evidence/lor-5fk6/gestures/fold-thread-long/recording.webm). I reviewed timestamped frames extracted across each video, the approval's original 50 ms strip, and the fold's full-size final frame. “half-written” remains in the reply field after the round trip. This is recorded browser evidence, not a judgment of physical Fold latency or touch feel. These phone recordings add no dashboard motion evidence.

GPT-6 Astra

<!-- critic: grok-4.6 -->

## Grok 4.6 (Grok, grok-4.6, reasoning effort high)

# Queue

The phone glance is the one I would keep. The dashboard is the one I would tire of. Queue knows how to stop talking when the list is empty, and it is the only concept that treats home as a launcher. Every other look at Today is a look at overdue life admin.

I used the dashboard live at 1280 (`#/queue/?scene=afternoon`): Done on the physio lead, People, Maya open. Phone: live Queue `demo` home in the lab viewer (the 1280×800 fill crops the icon grid; the 11-waiting chip is on screen). Cover, queue page, agent-waiting, search, sync-fail, quiet, and inner thread I saw as stills. Approve I saw in `lor-5fk6/gestures/approve-agent-waiting/final.png`.

## Dashboard

### What it gets right

One primary. Afternoon at 1500: the physio is a raised card with a reticle, Done / Seen / Start timer / Open on it, then "Then 8". Current's afternoon still is a decorative day arc and a morning brief; Needs you is below the fold, and the gallery tile even duplicates the whole page. Queue puts the thing that wants you in the middle of the screen.

The day strip is wordless and honest. Amber block at 15:00, empty boxes for the evening, "12 to label" as a chip. Relabel lives there. The timer is a bar you can Stop from People, which I used.

Quiet at 1500 is the most beautiful empty of the four: "ALL CLEAR / Nothing wants you." Next on the calendar is Dentist 16:30, and one soft link to the maps spec. The reticle goes grey. That is delight.

At 412 the same column still works. Actions grow. The strip still reads. This is the dashboard that survives a phone.

### What would annoy in daily use

Every visit is a list of obligations. Afternoon 1500, after the lead: App Store flag, then seven due rows, four of them late, then the brief, then this week as a footnote. There is no picture of the day beyond a strip of rectangles. You will know the dripping tap is four days late before you know you have an hour and a half until the dentist.

The lead card is large. Physio copy, goal line, four actions. At 412, Open sits on its own row under the buttons. That is a lot of chrome for one flag.

People, used live after clicking Maya: the top-bar tabs sit on top of the person name. The notes and cadence below are readable. Opening a person works. The collision is ugly every time.

### What it makes harder than current

Seeing the day's shape. Current's arc is theatre, and it still prints "Dentist 16:30" on a curve you cannot miss. Queue compresses the day into a strip you decode. Goals this week are four grey lines under the fold of the list. Current at least pretends the day is the subject of Today.

Ticking is easy. Understanding where you are in the afternoon is work.

### Steal

The all-clear lead card, into Panes. Panes' quiet Today at 1500 still leads with "12 stretches of time to label" and a list of unnamed phone blocks. Queue knows the page is allowed to be empty.

## Phone

### What it gets right

Home is a launcher. Demo cover still, and live: 15:00, weather, Dentist in 1 h 29, a timer chip, **11 waiting**, then icons, search at the thumb. Compared with current's cover, where a huge TODAY widget eats the grid and the icons are grey discs, this is the first home that looks like a phone.

The queue page (demo still, "11 for you") is the concept done properly: approval in the reticle, then flags with Done / Seen, then overdue. Agent-waiting cover adds the question with MapTiler / Stadia / Self-host / Other on the card, and the failed receipts turn. One tap for Approve. Three for a question, on the card, no sheet.

Inner thread-long still: list on the left, transcript on the right, Approve / Deny / Open in the thread header. Step 18 is fully readable. The other three concepts fail this.

Quiet queue page still: a tick, "Nothing wants you", Next Dentist 16:30, then what the agent is on, then later today. The empty is a place.

Sync-failed cover still: the 11-waiting slot becomes "Sync failed Fix". Icons untouched. Frequency and urgency are separated, as the brief asked.

Search-mixed cover still: field at the top, Play Store and Strava first, keyboard below. Apps stay visible while you type.

### What would annoy in daily use

**11 waiting** on every unlock. That number is T3 plus flags plus due. 252 of 298 launches are search and icons. You came to open WhatsApp and the phone told you the bathroom tap is late. The badge will go stale in your attention, which is the opposite of a queue.

Approve toast in `lor-5fk6/.../final.png` covers the next overdue row: "Approved. T3 runs the command." over "Fix the dripping bathroom tap". The next thing in the reticle is the question, which is correct. The toast still sits on the list.

Search at the top of the overlay is a reach on a Fold cover. The brief put the field at the thumb. Panes did that.

### What it makes harder than current

Current already hid Today in a widget, so Queue's home is easier for launching. The cost is the count. Opening the queue to tick a due task is a swipe (correct). Living with 11 on the chip is the daily tax current did not levy, because current did not count.

### Steal

Inline question options on the cover card (MapTiler / Stadia / Self-host / Other), into Timeline. Timeline's agent-waiting still puts an Answer button on the question and hides the rest behind "3 more waiting". Queue lets you answer without opening anything.

Grok 4.6

# Queue, in motion

Phone first. These recordings are the cover, the queue page, and the Fold. The dashboard stills from the first notes still stand; the gesture lab did not record the 1280 column moving.

I watched every run under `~/.local/state/lor-evidence/lor-5fk6/gestures/`, from the 50 ms strips and from frames pulled out of each `recording.webm`. The ones that change the stills are `approve-agent-waiting`, `tick-demo`, `answer-agent-waiting`, `fold-thread-long`, `fold-inner-thread-long`, `reply-thread-long`, `drawer-demo`, `search-demo`, `sync-sync-failed`, `timer-demo`, and `move-demo`.

## Phone

### What it gets right

The card clear is the concept. In `approve-agent-waiting` you tap Approve at 0 ms. The reticle stays. The card turns into Approving… with a Cancel hold for about three seconds. At 3.15 s the header still says 12 for you. At 3.35 s it already says 11, while the card is still Approving and the question is already sitting under it, waiting. At 4.4 s the question occupies the reticle, MapTiler / Stadia / Self-host / Other on the card, and a toast "Approved. T3 runs the command." The next thing in the gun-sight is the next thing that wants you. That is the move a final frame cannot show.

`answer-agent-waiting` keeps the approval in the reticle and answers below it. Other… opens a field on the card. Sending “MapTiler for now”… Cancel runs in the question card. The lead never jumps.

`tick-demo` swipes home onto the queue with the icon grid still visible on the right, ticks Finish unit 8, greys the row, and offers Undo. The reticle stays on the approval. Due work is a swipe away from a launcher.

`drawer-demo` pulls Used most now and All apps 29 up from the thumb. Search sits at the top of that overlay. `sync-sync-failed` swaps the 11-waiting chip for "Sync failed Fix" and leaves the icons alone.

Fold is the required interaction, and it holds. `fold-thread-long`: cover sheet, type "half-written", unfold to the queue, fold back, the draft is still there. `fold-inner-thread-long`: list on the left, transcript on the right, Approve / Deny / Open in the thread header, Step 18 readable. At 8 s Approving… and Cancel appear on the list card and on the thread at the same time. `reply-thread-long` puts the keyboard under an inner thread whose header still holds Approve.

### What would annoy in daily use

The Approving… hold is every approval. Three seconds of a progress bar on a phone you unlocked to clear one tap. Cancel is the reason it exists. Living with it on the tenth approval of the afternoon is the cost.

The settle toast in `approve-agent-waiting` sits on the first overdue row. "Approved. T3 runs the command." covers "Fix the dripping bathroom tap". The reticle has already moved to the question, which is correct. The toast still eats the list you are about to read.

The cover thread sheet in `fold-thread-long` puts Approve / Deny / Open over the start of the transcript. You type under a clipped Step 18. Inner is the working version of this. Cover is a sheet that talks over the thread.

Search in `drawer-demo` and `search-demo` puts the field at the top. On a Fold cover that is a reach. The drawer itself is the right motion.

### What it makes harder than current

Launching is easier than current. The cost that motion adds is the hold. Current's Today widget did not make you wait three seconds to confirm a command. Queue does, on every approve and on every send.

Ticking unit 8 is a swipe plus a tick plus a toast. Current hid Today. Queue found it. The swipe in `tick-demo` is honest: home stays a launcher until you ask for the list.

### Steal

The Approving… Cancel hold before the card clears, into Timeline and into Context. Timeline's approve in `lor-j1fe` already uses the same hold on the pinned card, with the route still visible under it. Queue is the one that then lets the next card occupy the same reticle. Put that occupy-in-place on Timeline's waiting stack: the question should slide into the pin when the approval settles, instead of the pin collapsing and the route jumping up.

## Dashboard

The gesture recordings are phone. Nothing here changes the afternoon column I used live: one lead, Then 8, a wordless strip. The card-clear on a 1280 screen would be the same reticle move with more air around it. I would still tire of the list. I would still keep the all-clear card.

Grok 4.6
