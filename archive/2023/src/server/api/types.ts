export enum TimelineEventType {
  GOAL_ADDED,
  GOAL_ARCHIVED,
  HABIT_ADDED,
  HABIT_ARCHIVED,
}

interface TimelineEventBase {
  eventType: TimelineEventType;
  date: Date;
}

export interface GoalAddedEvent extends TimelineEventBase {
  eventType: TimelineEventType.GOAL_ADDED;
  goal: {
    id: string;
    name: string;
  };
  habits: {
    id: string;
    description: string;
  }[];
  metrics: {
    id: string;
    prompt: string;
  }[];
}

export interface GoalArchivedEvent extends TimelineEventBase {
  eventType: TimelineEventType.GOAL_ARCHIVED;
  created: Date;
  goal: {
    id: string;
    name: string;
  };
  habits: {
    id: string;
    description: string;
  }[];
  metrics: {
    id: string;
    prompt: string;
  }[];
}

export interface HabitAddedEvent extends TimelineEventBase {
  eventType: TimelineEventType.HABIT_ADDED;
  habit: {
    id: string;
    description: string;
  };
  goals: {
    id: string;
    name: string;
  }[];
  metrics: {
    id: string;
    prompt: string;
  }[];
}

export interface HabitArchivedEvent extends TimelineEventBase {
  eventType: TimelineEventType.HABIT_ARCHIVED;
  created: Date;
  habit: {
    id: string;
    description: string;
  };
  goals: {
    id: string;
    name: string;
  }[];
  metrics: {
    id: string;
    prompt: string;
  }[];
}

export type TimelineEvent =
  | GoalAddedEvent
  | GoalArchivedEvent
  | HabitAddedEvent
  | HabitArchivedEvent;
