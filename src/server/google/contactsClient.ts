// Google Contacts through the People API, read-only. Only "My Contacts" (the
// people the owner saved), not "Other contacts", which is every address the
// owner has ever emailed.
import { z } from "zod";
import { googleGet } from "./client.js";

/** The sync token is older than Google keeps (about a week): start again from a full listing. */
export class SyncTokenExpiredError extends Error {
  constructor() {
    super("Google Contacts sync token expired");
    this.name = "SyncTokenExpiredError";
  }
}

const Primary = z.object({ primary: z.boolean().optional() }).optional();

export const GPerson = z.object({
  resourceName: z.string(),
  metadata: z.object({ deleted: z.boolean().optional() }).optional(),
  names: z.array(z.object({ displayName: z.string().optional(), givenName: z.string().optional(), familyName: z.string().optional(), metadata: Primary })).optional(),
  nicknames: z.array(z.object({ value: z.string() })).optional(),
  emailAddresses: z.array(z.object({ value: z.string(), metadata: Primary })).optional(),
  phoneNumbers: z.array(z.object({ value: z.string().optional(), canonicalForm: z.string().optional() })).optional(),
  organizations: z.array(z.object({ name: z.string().optional(), title: z.string().optional() })).optional(),
  birthdays: z.array(z.object({ date: z.object({ year: z.number().optional(), month: z.number().optional(), day: z.number().optional() }).optional(), text: z.string().optional() })).optional(),
});
export type GPerson = z.infer<typeof GPerson>;

export interface ContactsPage {
  people: GPerson[];
  nextSyncToken: string | null;
  /** True when this was a full listing (no sync token, or it had expired). */
  full: boolean;
}

export interface ContactsPort {
  /** Everything (no token), or only what changed since the token, deletions included. */
  connections(syncToken: string | null): Promise<ContactsPage>;
}

const FIELDS = "names,nicknames,emailAddresses,phoneNumbers,organizations,birthdays,metadata";

export const googleContacts: ContactsPort = {
  async connections(syncToken) {
    const people: GPerson[] = [];
    let pageToken: string | undefined;
    let nextSyncToken: string | null = null;
    do {
      const qs = new URLSearchParams({
        personFields: FIELDS,
        pageSize: "1000",
        requestSyncToken: "true",
        ...(syncToken ? { syncToken } : {}),
        ...(pageToken ? { pageToken } : {}),
      });
      let raw: unknown;
      try {
        raw = await googleGet(`https://people.googleapis.com/v1/people/me/connections?${qs}`);
      } catch (err) {
        if (syncToken && err instanceof Error && /Google 410|EXPIRED_SYNC_TOKEN/.test(err.message)) throw new SyncTokenExpiredError();
        throw err;
      }
      const page = z
        .object({ connections: z.array(z.unknown()).default([]), nextPageToken: z.string().optional(), nextSyncToken: z.string().optional() })
        .parse(raw);
      for (const p of page.connections) people.push(GPerson.parse(p));
      pageToken = page.nextPageToken;
      nextSyncToken = page.nextSyncToken ?? nextSyncToken;
    } while (pageToken);
    return { people, nextSyncToken, full: !syncToken };
  },
};
