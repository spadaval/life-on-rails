package com.spadaval.lor.concepts

import com.spadaval.lor.concepts.queue.Queue
import com.spadaval.lor.lab.Version

/*
 * The design concepts (mission lor-2803, docs/design/lab/brief.md), one version each, in the order
 * the Version dropdown lists them after `current`. A new direction adds its line here; the lab
 * page hands the list to the registry (Lab.concepts) before anything reads it. They live in
 * wasmJsMain, so none of them ships in the app.
 */
object Concepts {
    val all: List<Version> = listOf(Queue.version)
}
