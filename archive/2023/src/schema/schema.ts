import { relations, sql } from "drizzle-orm";
import {
  datetime,
  double,
  index,
  int,
  mysqlEnum,
  mysqlTable,
  text,
  tinyint,
  uniqueIndex,
  varchar,
} from "drizzle-orm/mysql-core";

export const answerFormat = mysqlTable("AnswerFormat", {
  id: varchar("id", { length: 191 }).primaryKey().notNull(),
  format: mysqlEnum("format", ["FIVE_POINT", "NUMERIC_INPUT"]).notNull(),
  start: double("start"),
  end: double("end"),
});

export const goal = mysqlTable(
  "Goal",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    name: varchar("name", { length: 191 }).notNull(),
    createdAt: datetime("createdAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    updatedAt: datetime("updatedAt", { mode: "string", fsp: 3 }).notNull(),
    ownerId: varchar("ownerId", { length: 191 }).notNull(),
    archived: tinyint("archived").default(0).notNull(),
    archivedAt: datetime("archivedAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
  },
  (table) => {
    return {
      ownerIdIdx: index("Goal_ownerId_idx").on(table.ownerId),
    };
  }
);

export const goalRelations = relations(goal, ({ many }) => ({
  habits: many(habitMeasuresGoal),
  metrics: many(metricMeasuresGoal),
  tags: many(goalTag),
}));

export const goalTag = mysqlTable(
  "GoalTag",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    goalId: varchar("goalId", { length: 191 }).notNull(),
    tagId: varchar("tagId", { length: 191 }).notNull(),
  },
  (table) => {
    return {
      tagIdGoalIdKey: uniqueIndex("GoalTag_tagId_goalId_key").on(
        table.tagId,
        table.goalId
      ),
      goalIdIdx: index("GoalTag_goalId_idx").on(table.goalId),
      tagIdIdx: index("GoalTag_tagId_idx").on(table.tagId),
    };
  }
);

export const goalTagRelations = relations(goalTag, ({ one }) => ({
  tag: one(tag, { fields: [goalTag.tagId], references: [tag.id] }),
  goal: one(goal, { fields: [goalTag.goalId], references: [goal.id] }),
}));

export const habit = mysqlTable(
  "Habit",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    createdAt: datetime("createdAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    updatedAt: datetime("updatedAt", { mode: "string", fsp: 3 }).notNull(),
    ownerId: varchar("ownerId", { length: 191 }).notNull(),
    description: varchar("description", { length: 191 }).notNull(),
    frequency: int("frequency").default(1).notNull(),
    frequencyHorizon: mysqlEnum("frequencyHorizon", ["DAY", "WEEK"])
      .default("DAY")
      .notNull(),
    completionWeight: double("completionWeight").default(1).notNull(),
    archived: tinyint("archived").default(0).notNull(),
    archivedAt: datetime("archivedAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
  },
  (table) => {
    return {
      ownerIdIdx: index("Habit_ownerId_idx").on(table.ownerId),
    };
  }
);

export const habitRelations = relations(habit, ({ many }) => ({
  goals: many(habitMeasuresGoal),
  metrics: many(linkedMetric),
  completions: many(habitCompletion),
  tags: many(habitTag),
}));

export const habitCompletion = mysqlTable(
  "HabitCompletion",
  {
    id: varchar("id", { length: 191 })
      .primaryKey()
      .default(sql`(UUID())`)
      .notNull(),
    habitId: varchar("habitId", { length: 191 }).notNull(),
    date: datetime("date", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    memo: varchar("memo", { length: 191 }),
  },
  (table) => {
    return {
      habitIdIdx: index("HabitCompletion_habitId_idx").on(table.habitId),
      dateIdx: index("HabitCompletion_date_idx").on(table.date),
    };
  }
);

export const habitCompletionRelations = relations(
  habitCompletion,
  ({ one }) => ({
    habit: one(habit, {
      fields: [habitCompletion.habitId],
      references: [habit.id],
    }),
  })
);

export const habitMeasuresGoal = mysqlTable(
  "HabitMeasuresGoal",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    goalId: varchar("goalId", { length: 191 }).notNull(),
    habitId: varchar("habitId", { length: 191 }).notNull(),
    weight: int("weight").default(1).notNull(),
  },
  (table) => {
    return {
      goalIdHabitIdKey: uniqueIndex("HabitMeasuresGoal_goalId_habitId_key").on(
        table.goalId,
        table.habitId
      ),
      goalIdIdx: index("HabitMeasuresGoal_goalId_idx").on(table.goalId),
      habitIdIdx: index("HabitMeasuresGoal_habitId_idx").on(table.habitId),
    };
  }
);

export const habitMeasuresGoalRelations = relations(
  habitMeasuresGoal,
  ({ one }) => ({
    goal: one(goal, {
      fields: [habitMeasuresGoal.goalId],
      references: [goal.id],
    }),
    habit: one(habit, {
      fields: [habitMeasuresGoal.habitId],
      references: [habit.id],
    }),
  })
);

export const habitTag = mysqlTable(
  "HabitTag",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    habitId: varchar("habitId", { length: 191 }).notNull(),
    tagId: varchar("tagId", { length: 191 }).notNull(),
  },
  (table) => {
    return {
      tagIdHabitIdKey: uniqueIndex("HabitTag_tagId_habitId_key").on(
        table.tagId,
        table.habitId
      ),
      habitIdIdx: index("HabitTag_habitId_idx").on(table.habitId),
      tagIdIdx: index("HabitTag_tagId_idx").on(table.tagId),
    };
  }
);

export const habitTagRelations = relations(habitTag, ({ one }) => ({
  habit: one(habit, {
    fields: [habitTag.habitId],
    references: [habit.id],
  }),
  tag: one(tag, {
    fields: [habitTag.tagId],
    references: [tag.id],
  }),
}));

export const linkedMetric = mysqlTable(
  "LinkedMetric",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    date: datetime("date", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    habitId: varchar("habitId", { length: 191 }).notNull(),
    metricId: varchar("metricId", { length: 191 }).notNull(),
  },
  (table) => {
    return {
      habitIdIdx: index("LinkedMetric_habitId_idx").on(table.habitId),
      metricIdIdx: index("LinkedMetric_metricId_idx").on(table.metricId),
    };
  }
);
export const linkedMetricRelations = relations(linkedMetric, ({ one }) => ({
  metric: one(metric, {
    fields: [linkedMetric.metricId],
    references: [metric.id],
  }),
  habit: one(habit, { fields: [linkedMetric.habitId], references: [habit.id] }),
}));

export const metric = mysqlTable(
  "Metric",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    prompt: varchar("prompt", { length: 191 }).notNull(),
    createdAt: datetime("createdAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    updatedAt: datetime("updatedAt", { mode: "string", fsp: 3 }).notNull(),
    ownerId: varchar("ownerId", { length: 191 }).notNull(),
    formatId: varchar("formatId", { length: 191 }).notNull(),
    archived: tinyint("archived").default(0).notNull(),
    archivedAt: datetime("archivedAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
  },
  (table) => {
    return {
      ownerIdIdx: index("Metric_ownerId_idx").on(table.ownerId),
      formatIdIdx: index("Metric_formatId_idx").on(table.formatId),
    };
  }
);

export const metricRelations = relations(metric, ({ many }) => ({
  answers: many(metricAnswer),
  goals: many(metricMeasuresGoal),
  habits: many(linkedMetric),
  tags: many(metricTag),
}));

export const metricAnswer = mysqlTable(
  "MetricAnswer",
  {
    id: varchar("id", { length: 191 }).primaryKey().default(sql`(UUID())`).notNull(),
    value: double("value").notNull(),
    createdAt: datetime("createdAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    updatedAt: datetime("updatedAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    metricId: varchar("metricId", { length: 191 }).notNull(),
    habitCompletionId: varchar("habitCompletionId", { length: 191 }),
    memo: varchar("memo", { length: 191 }),
    score: double("score").notNull(),
  },
  (table) => {
    return {
      metricIdIdx: index("MetricAnswer_metricId_idx").on(table.metricId),
      habitCompletionIdIdx: index("MetricAnswer_habitCompletionId_idx").on(
        table.habitCompletionId
      ),
    };
  }
);

export const meticAnswerRelations = relations(metricAnswer, ({ one }) => ({
  answers: one(metric, {
    fields: [metricAnswer.metricId],
    references: [metric.id],
  }),
}));

export const metricMeasuresGoal = mysqlTable(
  "MetricMeasuresGoal",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    goalId: varchar("goalId", { length: 191 }).notNull(),
    metricId: varchar("metricId", { length: 191 }).notNull(),
    weight: int("weight").default(1).notNull(),
  },
  (table) => {
    return {
      goalIdMetricIdKey: uniqueIndex(
        "MetricMeasuresGoal_goalId_metricId_key"
      ).on(table.goalId, table.metricId),
      goalIdIdx: index("MetricMeasuresGoal_goalId_idx").on(table.goalId),
      metricIdIdx: index("MetricMeasuresGoal_metricId_idx").on(table.metricId),
    };
  }
);

export const metricMeasuresGoalRelations = relations(
  metricMeasuresGoal,
  ({ one }) => ({
    metric: one(metric, {
      fields: [metricMeasuresGoal.metricId],
      references: [metric.id],
    }),
    goal: one(goal, {
      fields: [metricMeasuresGoal.goalId],
      references: [goal.id],
    }),
  })
);
export const metricTag = mysqlTable(
  "MetricTag",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    metricId: varchar("metricId", { length: 191 }).notNull(),
    tagId: varchar("tagId", { length: 191 }).notNull(),
  },
  (table) => {
    return {
      tagIdMetricTagKey: uniqueIndex("MetricTag_tagId_metricTag_key").on(
        table.tagId,
        table.metricId
      ),
      metricTagIdx: index("MetricTag_metricTag_idx").on(table.metricId),
      tagIdIdx: index("MetricTag_tagId_idx").on(table.tagId),
    };
  }
);

export const metricTagRelations = relations(metricTag, ({ one }) => ({
  metric: one(metric, {
    fields: [metricTag.metricId],
    references: [metric.id],
  }),
  tag: one(tag, {
    fields: [metricTag.tagId],
    references: [tag.id],
  }),
}));

export const tag = mysqlTable(
  "Tag",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    name: varchar("name", { length: 191 }).notNull(),
    ownerId: varchar("ownerId", { length: 191 }).notNull(),
  },
  (table) => {
    return {
      ownerIdNameKey: uniqueIndex("Tag_ownerId_name_key").on(
        table.ownerId,
        table.name
      ),
      ownerIdIdx: index("Tag_ownerId_idx").on(table.ownerId),
    };
  }
);

export const user = mysqlTable(
  "User",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    name: varchar("name", { length: 191 }),
    email: varchar("email", { length: 191 }),
    emailVerified: datetime("emailVerified", { mode: "string", fsp: 3 }),
    image: varchar("image", { length: 191 }),
    createdAt: datetime("createdAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    updatedAt: datetime("updatedAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    scoringWeeks: int("scoringWeeks").default(2).notNull(),
    scoringUnit: mysqlEnum("scoringUnit", ["Percentage", "Normalized"])
      .default("Normalized")
      .notNull(),
  },
  (table) => {
    return {
      emailKey: uniqueIndex("User_email_key").on(table.email),
    };
  }
);

export const ScoringFormat = mysqlEnum("scoringUnit", [
  "Percentage",
  "Normalized",
]);

export const preferences = mysqlTable("Preferences", {
  userId: varchar("userId", { length: 191 }).primaryKey().notNull(),
  scoringWeeks: int("scoringWeeks").default(2).notNull(),
  scoringUnit: ScoringFormat.default("Normalized").notNull(),
});

export const userRelations = relations(user, ({ many }) => ({
  accounts: many(account),
}));

export const account = mysqlTable(
  "Account",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    userId: varchar("userId", { length: 191 }).notNull(),
    type: varchar("type", { length: 191 }).notNull(),
    provider: varchar("provider", { length: 191 }).notNull(),
    providerAccountId: varchar("providerAccountId", { length: 191 }).notNull(),
    refreshToken: text("refresh_token"),
    accessToken: text("access_token"),
    expiresAt: int("expires_at"),
    tokenType: varchar("token_type", { length: 191 }),
    scope: varchar("scope", { length: 191 }),
    idToken: text("id_token"),
    sessionState: varchar("session_state", { length: 191 }),
    createdAt: datetime("createdAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
    updatedAt: datetime("updatedAt", { mode: "string", fsp: 3 })
      .default(sql`(CURRENT_TIMESTAMP(3))`)
      .notNull(),
  },
  (table) => {
    return {
      providerProviderAccountIdKey: uniqueIndex(
        "Account_provider_providerAccountId_key"
      ).on(table.provider, table.providerAccountId),
      userIdIdx: index("Account_userId_idx").on(table.userId),
    };
  }
);

export const accountRelations = relations(account, ({ one }) => ({
  user: one(user, { fields: [account.userId], references: [user.id] }),
}));

export const session = mysqlTable(
  "Session",
  {
    id: varchar("id", { length: 191 }).primaryKey().notNull(),
    sessionToken: varchar("sessionToken", { length: 191 }).notNull(),
    userId: varchar("userId", { length: 191 }).notNull(),
    expires: datetime("expires", { mode: "string", fsp: 3 }).notNull(),
  },
  (table) => {
    return {
      sessionTokenKey: uniqueIndex("Session_sessionToken_key").on(
        table.sessionToken
      ),
      userIdIdx: index("Session_userId_idx").on(table.userId),
    };
  }
);

export const verificationToken = mysqlTable(
  "VerificationToken",
  {
    identifier: varchar("identifier", { length: 191 }).notNull(),
    token: varchar("token", { length: 191 }).primaryKey().notNull(),
    expires: datetime("expires", { mode: "string", fsp: 3 }).notNull(),
  },
  (table) => {
    return {
      tokenKey: uniqueIndex("VerificationToken_token_key").on(table.token),
      identifierTokenKey: uniqueIndex(
        "VerificationToken_identifier_token_key"
      ).on(table.identifier, table.token),
    };
  }
);
