package com.hobsojam.simpleestimation.domain.room

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

fun interface ReconnectScheduler {
    /**
     * Schedules [task] to run after [delayMs] milliseconds.
     * Returns a canceller that prevents the task from running if called before it fires.
     */
    fun schedule(delayMs: Long, task: () -> Unit): () -> Unit
}

class ScheduledExecutorReconnectScheduler(
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(),
) : ReconnectScheduler {
    override fun schedule(delayMs: Long, task: () -> Unit): () -> Unit {
        val future = executor.schedule(task, delayMs, TimeUnit.MILLISECONDS)
        return { future.cancel(false) }
    }
}

private const val BASE_DELAY_MS = 1_000L
private const val MAX_DELAY_MS = 30_000L
private const val MAX_SHIFT_BITS = 30
private const val MIN_SHIFT_BITS = 0

/**
 * Exponential backoff capped at 30 seconds.
 * attempt 1 → 1 s, attempt 2 → 2 s, attempt 3 → 4 s, …, attempt 6+ → 30 s.
 */
internal fun reconnectDelayMs(attempt: Int): Long =
    minOf(BASE_DELAY_MS shl (attempt - 1).coerceIn(MIN_SHIFT_BITS, MAX_SHIFT_BITS), MAX_DELAY_MS)
