package com.spadaval.lor.lab

// Written by scripts/lab/demo-fixture.mjs from the demo set (seeds/demo/) at 2026-11-17T15:00, the moment
// the dashboard's design tools open on. Don't edit by hand: load the demo set and run the script.
// The weather is pinned there (the test server's is a live call), and the ids are renumbered.

/** LoR's replies on the demo day, as the server sends them. */
object DemoDay {
    const val AT = "2026-11-17T15:00"

    /** `GET /device/home`. */
    val home = """
{
  "now": "2026-11-17T20:00:00.000Z",
  "timezone": "America/New_York",
  "one": {
    "kicker": "From this morning's brief",
    "text": "Finish the offline maps spec before the design review at 14:00.",
    "why": "Lena is waiting on the icon sizes, and the App Store review notes need your answers."
  },
  "brief": "Finish the offline maps spec before the design review at 14:00. Lena is waiting on the icon sizes, and the App Store review notes need your answers.",
  "needs_you": [
    {
      "id": "00000000-0000-7000-8000-000000000001",
      "title": "Organise Sam's birthday dinner",
      "note": "Sam's birthday is Friday and the table for eight isn't booked yet.",
      "flagged_at": "2026-11-16T23:30:00.000Z",
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000002",
      "title": "Find a physio for the left knee",
      "note": "Two physios near the studio take your insurance; pick one and I'll book.",
      "flagged_at": "2026-11-17T12:55:00.000Z",
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000003",
      "title": "Reply to the App Store review notes",
      "note": "Apple asked two questions about location use; your answers are in the draft.",
      "flagged_at": "2026-11-17T13:10:00.000Z",
      "google": false
    }
  ],
  "day": {
    "wake_start": "2026-11-17T13:00:00.000Z",
    "wake_end": "2026-11-18T03:00:00.000Z",
    "remaining_s": 25200,
    "remaining_fraction": 0.5,
    "running": null,
    "events": [
      {
        "title": "Dentist",
        "start": "2026-11-17T21:30:00.000Z",
        "end": "2026-11-17T22:15:00.000Z"
      },
      {
        "title": "Portuguese with Inês",
        "start": "2026-11-17T23:00:00.000Z",
        "end": "2026-11-17T23:45:00.000Z"
      },
      {
        "title": "Focus time",
        "start": "2026-11-18T00:30:00.000Z",
        "end": "2026-11-18T02:00:00.000Z"
      }
    ]
  },
  "agent_queue": [
    {
      "id": "00000000-0000-7000-8000-000000000004",
      "title": "Benchmark tile cache sizes",
      "queued_at": "2026-11-17T12:40:00.000Z",
      "thread": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000005",
      "title": "Plan the long run route for Sunday",
      "queued_at": "2026-11-17T12:45:00.000Z",
      "thread": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000006",
      "title": "Back up the laptop",
      "queued_at": "2026-11-17T12:50:00.000Z",
      "thread": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000007",
      "title": "Cancel the unused streaming subscription",
      "queued_at": null,
      "thread": "demo-thread-subs"
    },
    {
      "id": "00000000-0000-7000-8000-000000000008",
      "title": "Pick a vector tile provider",
      "queued_at": null,
      "thread": "demo-thread-tiles"
    },
    {
      "id": "00000000-0000-7000-8000-000000000009",
      "title": "Find an apartment in Alfama for six nights",
      "queued_at": null,
      "thread": null
    }
  ],
  "weather": {
    "temp_c": 11.6,
    "condition": "Partly cloudy",
    "is_day": true,
    "observed_at": "2026-11-17T19:45:00.000Z"
  }
}
"""

    /** `GET /device/tasks`. */
    val tasks = """
{
  "now": "2026-11-17T20:00:00.000Z",
  "timezone": "America/New_York",
  "today": "2026-11-17",
  "open": 33,
  "tasks": [
    {
      "id": "00000000-0000-7000-8000-000000000010",
      "title": "Fix the dripping bathroom tap",
      "due": "2026-11-13",
      "overdue": true,
      "waiting_on": null,
      "parent": "Keep the house running",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000011",
      "title": "Move the emergency fund to the high-yield account",
      "due": "2026-11-14",
      "overdue": true,
      "waiting_on": null,
      "parent": "Get the finances in order",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000012",
      "title": "Fix the crash on rotate in the map view",
      "due": "2026-11-15",
      "overdue": true,
      "waiting_on": null,
      "parent": "Public beta",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000013",
      "title": "Finish unit 8 of the course",
      "due": "2026-11-16",
      "overdue": true,
      "waiting_on": null,
      "parent": "Hold a conversation in Portuguese",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000014",
      "title": "Renew the parking permit",
      "due": "2026-11-16",
      "overdue": true,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": false,
      "google": true
    },
    {
      "id": "00000000-0000-7000-8000-000000000015",
      "title": "Return the library books",
      "due": "2026-11-17",
      "overdue": false,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000016",
      "title": "Write release notes for beta 0.4",
      "due": "2026-11-17",
      "overdue": false,
      "waiting_on": null,
      "parent": "Public beta",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000017",
      "title": "Draft the offline maps spec",
      "due": "2026-11-18",
      "overdue": false,
      "waiting_on": null,
      "parent": "Offline maps",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000018",
      "title": "Pick up dry cleaning",
      "due": "2026-11-18",
      "overdue": false,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": false,
      "google": true
    },
    {
      "id": "00000000-0000-7000-8000-000000000019",
      "title": "Book a sports massage",
      "due": "2026-11-19",
      "overdue": false,
      "waiting_on": null,
      "parent": "Run a half marathon in March",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000020",
      "title": "Buy a birthday card for Sam",
      "due": "2026-11-19",
      "overdue": false,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": false,
      "google": true
    },
    {
      "id": "00000000-0000-7000-8000-000000000004",
      "title": "Benchmark tile cache sizes",
      "due": "2026-11-20",
      "overdue": false,
      "waiting_on": null,
      "parent": "Offline maps",
      "flagged": false,
      "agent": true,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000001",
      "title": "Organise Sam's birthday dinner",
      "due": "2026-11-20",
      "overdue": false,
      "waiting_on": null,
      "parent": "Be a good friend",
      "flagged": true,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000005",
      "title": "Plan the long run route for Sunday",
      "due": "2026-11-21",
      "overdue": false,
      "waiting_on": null,
      "parent": "Training block two",
      "flagged": false,
      "agent": true,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000021",
      "title": "Invite twenty beta testers",
      "due": "2026-11-22",
      "overdue": false,
      "waiting_on": null,
      "parent": "Public beta",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000022",
      "title": "Choose the kitchen paint colour",
      "due": "2026-11-23",
      "overdue": false,
      "waiting_on": null,
      "parent": "Repaint the kitchen",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000023",
      "title": "Book flights to Lisbon",
      "due": "2026-11-26",
      "overdue": false,
      "waiting_on": null,
      "parent": "Lisbon trip in April",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000024",
      "title": "Renew the developer account",
      "due": "2026-11-29",
      "overdue": false,
      "waiting_on": null,
      "parent": "Ship the Field Guide app",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000025",
      "title": "Gather 1099s and receipts",
      "due": "2026-12-07",
      "overdue": false,
      "waiting_on": null,
      "parent": "2026 taxes",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000026",
      "title": "Ask Inês for a weekly conversation slot",
      "due": null,
      "overdue": false,
      "waiting_on": "Inês Carvalho",
      "parent": "Hold a conversation in Portuguese",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000006",
      "title": "Back up the laptop",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": true,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000007",
      "title": "Cancel the unused streaming subscription",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Get the finances in order",
      "flagged": false,
      "agent": true,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000027",
      "title": "Cut the walnut boards to length",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Walnut bookshelf",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000002",
      "title": "Find a physio for the left knee",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Run a half marathon in March",
      "flagged": true,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000009",
      "title": "Find an apartment in Alfama for six nights",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Lisbon trip in April",
      "flagged": false,
      "agent": true,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000028",
      "title": "Look into a standing desk",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000008",
      "title": "Pick a vector tile provider",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Offline maps",
      "flagged": false,
      "agent": true,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000029",
      "title": "Read the essay Daniel sent",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": null,
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000030",
      "title": "Replace the hallway bulb",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Keep the house running",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000003",
      "title": "Reply to the App Store review notes",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Public beta",
      "flagged": true,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000031",
      "title": "Send Lena the icon sizes",
      "due": null,
      "overdue": false,
      "waiting_on": "Lena Brandt",
      "parent": "Ship the Field Guide app",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000032",
      "title": "Send Noor the photos from the hike",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Be a good friend",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000033",
      "title": "Waiting on the accountant's engagement letter",
      "due": null,
      "overdue": false,
      "waiting_on": "Northwind Accounting",
      "parent": "2026 taxes",
      "flagged": false,
      "agent": false,
      "google": false
    }
  ],
  "done_today": [
    {
      "id": "00000000-0000-7000-8000-000000000034",
      "title": "Buy new running shoes",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Training block two",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000035",
      "title": "Call the landlord about the heating",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Keep the house running",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000036",
      "title": "Check the credit card statement",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Get the finances in order",
      "flagged": false,
      "agent": false,
      "google": false
    },
    {
      "id": "00000000-0000-7000-8000-000000000037",
      "title": "Take the recycling out",
      "due": null,
      "overdue": false,
      "waiting_on": null,
      "parent": "Keep the house running",
      "flagged": false,
      "agent": false,
      "google": false
    }
  ]
}
"""

    /** `GET /device/people`. */
    val people = """
{
  "today": "2026-11-17",
  "overdue": [
    {
      "id": "00000000-0000-7000-8000-000000000038",
      "name": "Marcus Bell",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 60,
      "keep_in_touch_days": 60,
      "birthday_in": null,
      "turning": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000039",
      "name": "Aisha Mensah",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 45,
      "keep_in_touch_days": 45,
      "birthday_in": null,
      "turning": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000040",
      "name": "Rosa Delgado",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 30,
      "keep_in_touch_days": 30,
      "birthday_in": null,
      "turning": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000041",
      "name": "Sam Whitfield",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 21,
      "keep_in_touch_days": 21,
      "birthday_in": 3,
      "turning": 34
    },
    {
      "id": "00000000-0000-7000-8000-000000000042",
      "name": "Grandpa Joe",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 14,
      "keep_in_touch_days": 14,
      "birthday_in": 41,
      "turning": 82
    },
    {
      "id": "00000000-0000-7000-8000-000000000043",
      "name": "Inês Carvalho",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 14,
      "keep_in_touch_days": 14,
      "birthday_in": null,
      "turning": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000044",
      "name": "Noor Haddad",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 10,
      "keep_in_touch_days": 10,
      "birthday_in": null,
      "turning": null
    }
  ],
  "birthdays": [
    {
      "id": "00000000-0000-7000-8000-000000000041",
      "name": "Sam Whitfield",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": 21,
      "keep_in_touch_days": 21,
      "birthday_in": 3,
      "turning": 34
    },
    {
      "id": "00000000-0000-7000-8000-000000000045",
      "name": "Theo Lindqvist",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": null,
      "keep_in_touch_days": null,
      "birthday_in": 12,
      "turning": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000046",
      "name": "Clara Novak",
      "kind": "person",
      "last_at": null,
      "last_via": null,
      "overdue_by": null,
      "keep_in_touch_days": null,
      "birthday_in": 18,
      "turning": null
    },
    {
      "id": "00000000-0000-7000-8000-000000000047",
      "name": "Priya Raman",
      "kind": "person",
      "last_at": "2026-11-17T18:30:00.000Z",
      "last_via": "meeting",
      "overdue_by": -30,
      "keep_in_touch_days": 30,
      "birthday_in": 26,
      "turning": 37
    }
  ]
}
"""
}
