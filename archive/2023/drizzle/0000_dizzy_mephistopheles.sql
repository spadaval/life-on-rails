-- Current sql file was generated after introspecting the database
-- If you want to run this migration please uncomment this code before executing migrations
/*
CREATE TABLE `Account` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`userId` varchar(191) NOT NULL,
	`type` varchar(191) NOT NULL,
	`provider` varchar(191) NOT NULL,
	`providerAccountId` varchar(191) NOT NULL,
	`refresh_token` text,
	`access_token` text,
	`expires_at` int,
	`token_type` varchar(191),
	`scope` varchar(191),
	`id_token` text,
	`session_state` varchar(191),
	`createdAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`updatedAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)));

CREATE TABLE `AnswerFormat` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`format` enum('FIVE_POINT','NUMERIC_INPUT') NOT NULL,
	`start` double,
	`end` double);

CREATE TABLE `Goal` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`name` varchar(191) NOT NULL,
	`createdAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`updatedAt` datetime(3) NOT NULL,
	`ownerId` varchar(191) NOT NULL,
	`archived` tinyint NOT NULL DEFAULT 0,
	`archivedAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)));

CREATE TABLE `GoalTag` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`goalId` varchar(191) NOT NULL,
	`tagId` varchar(191) NOT NULL);

CREATE TABLE `Habit` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`createdAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`updatedAt` datetime(3) NOT NULL,
	`ownerId` varchar(191) NOT NULL,
	`description` varchar(191) NOT NULL,
	`frequency` int NOT NULL DEFAULT 1,
	`frequencyHorizon` enum('DAY','WEEK') NOT NULL DEFAULT 'DAY',
	`completionWeight` double NOT NULL DEFAULT 1,
	`archived` tinyint NOT NULL DEFAULT 0,
	`archivedAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)));

CREATE TABLE `HabitCompletion` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`habitId` varchar(191) NOT NULL,
	`date` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`memo` varchar(191));

CREATE TABLE `HabitMeasuresGoal` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`goalId` varchar(191) NOT NULL,
	`habitId` varchar(191) NOT NULL,
	`weight` int NOT NULL DEFAULT 1);

CREATE TABLE `HabitTag` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`habitId` varchar(191) NOT NULL,
	`tagId` varchar(191) NOT NULL);

CREATE TABLE `LinkedMetric` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`date` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`habitId` varchar(191) NOT NULL,
	`metricId` varchar(191) NOT NULL);

CREATE TABLE `Metric` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`prompt` varchar(191) NOT NULL,
	`createdAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`updatedAt` datetime(3) NOT NULL,
	`ownerId` varchar(191) NOT NULL,
	`formatId` varchar(191) NOT NULL,
	`archived` tinyint NOT NULL DEFAULT 0,
	`archivedAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)));

CREATE TABLE `MetricAnswer` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`value` double NOT NULL,
	`createdAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`updatedAt` datetime(3) NOT NULL,
	`metricId` varchar(191) NOT NULL,
	`habitCompletionId` varchar(191),
	`memo` varchar(191),
	`score` double NOT NULL);

CREATE TABLE `MetricMeasuresGoal` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`goalId` varchar(191) NOT NULL,
	`metricId` varchar(191) NOT NULL,
	`weight` int NOT NULL DEFAULT 1);

CREATE TABLE `MetricTag` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`metricTag` varchar(191) NOT NULL,
	`tagId` varchar(191) NOT NULL);

CREATE TABLE `Session` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`sessionToken` varchar(191) NOT NULL,
	`userId` varchar(191) NOT NULL,
	`expires` datetime(3) NOT NULL);

CREATE TABLE `Tag` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`name` varchar(191) NOT NULL,
	`ownerId` varchar(191) NOT NULL);

CREATE TABLE `User` (
	`id` varchar(191) PRIMARY KEY NOT NULL,
	`name` varchar(191),
	`email` varchar(191),
	`emailVerified` datetime(3),
	`image` varchar(191),
	`createdAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`updatedAt` datetime(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
	`scoringWeeks` int NOT NULL DEFAULT 2,
	`scoringUnit` enum('Percentage','Normalized') NOT NULL DEFAULT 'Normalized');

CREATE TABLE `VerificationToken` (
	`identifier` varchar(191) NOT NULL,
	`token` varchar(191) PRIMARY KEY NOT NULL,
	`expires` datetime(3) NOT NULL);

CREATE UNIQUE INDEX `Account_provider_providerAccountId_key` ON `Account` (`provider`,`providerAccountId`);
CREATE INDEX `Account_userId_idx` ON `Account` (`userId`);
CREATE INDEX `Goal_ownerId_idx` ON `Goal` (`ownerId`);
CREATE UNIQUE INDEX `GoalTag_tagId_goalId_key` ON `GoalTag` (`tagId`,`goalId`);
CREATE INDEX `GoalTag_goalId_idx` ON `GoalTag` (`goalId`);
CREATE INDEX `GoalTag_tagId_idx` ON `GoalTag` (`tagId`);
CREATE INDEX `Habit_ownerId_idx` ON `Habit` (`ownerId`);
CREATE INDEX `HabitCompletion_habitId_idx` ON `HabitCompletion` (`habitId`);
CREATE INDEX `HabitCompletion_date_idx` ON `HabitCompletion` (`date`);
CREATE UNIQUE INDEX `HabitMeasuresGoal_goalId_habitId_key` ON `HabitMeasuresGoal` (`goalId`,`habitId`);
CREATE INDEX `HabitMeasuresGoal_goalId_idx` ON `HabitMeasuresGoal` (`goalId`);
CREATE INDEX `HabitMeasuresGoal_habitId_idx` ON `HabitMeasuresGoal` (`habitId`);
CREATE UNIQUE INDEX `HabitTag_tagId_habitId_key` ON `HabitTag` (`tagId`,`habitId`);
CREATE INDEX `HabitTag_habitId_idx` ON `HabitTag` (`habitId`);
CREATE INDEX `HabitTag_tagId_idx` ON `HabitTag` (`tagId`);
CREATE INDEX `LinkedMetric_habitId_idx` ON `LinkedMetric` (`habitId`);
CREATE INDEX `LinkedMetric_metricId_idx` ON `LinkedMetric` (`metricId`);
CREATE INDEX `Metric_ownerId_idx` ON `Metric` (`ownerId`);
CREATE INDEX `Metric_formatId_idx` ON `Metric` (`formatId`);
CREATE INDEX `MetricAnswer_metricId_idx` ON `MetricAnswer` (`metricId`);
CREATE INDEX `MetricAnswer_habitCompletionId_idx` ON `MetricAnswer` (`habitCompletionId`);
CREATE UNIQUE INDEX `MetricMeasuresGoal_goalId_metricId_key` ON `MetricMeasuresGoal` (`goalId`,`metricId`);
CREATE INDEX `MetricMeasuresGoal_goalId_idx` ON `MetricMeasuresGoal` (`goalId`);
CREATE INDEX `MetricMeasuresGoal_metricId_idx` ON `MetricMeasuresGoal` (`metricId`);
CREATE UNIQUE INDEX `MetricTag_tagId_metricTag_key` ON `MetricTag` (`tagId`,`metricTag`);
CREATE INDEX `MetricTag_metricTag_idx` ON `MetricTag` (`metricTag`);
CREATE INDEX `MetricTag_tagId_idx` ON `MetricTag` (`tagId`);
CREATE UNIQUE INDEX `Session_sessionToken_key` ON `Session` (`sessionToken`);
CREATE INDEX `Session_userId_idx` ON `Session` (`userId`);
CREATE UNIQUE INDEX `Tag_ownerId_name_key` ON `Tag` (`ownerId`,`name`);
CREATE INDEX `Tag_ownerId_idx` ON `Tag` (`ownerId`);
CREATE UNIQUE INDEX `User_email_key` ON `User` (`email`);
CREATE UNIQUE INDEX `VerificationToken_token_key` ON `VerificationToken` (`token`);
CREATE UNIQUE INDEX `VerificationToken_identifier_token_key` ON `VerificationToken` (`identifier`,`token`);
*/