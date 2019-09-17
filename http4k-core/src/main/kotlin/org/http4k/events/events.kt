package org.http4k.events

typealias Events = (Event) -> Unit

/**
 * Represents a meaningful "happening" in an app.
 */
interface Event {
    companion object {
        data class Error(val message: String, val cause: Throwable? = null) : Event {
            val category = EventCategory("error")
        }
    }
}

fun Events.then(next: Events): Events = { it.also(this).also(next) }

/**
 * An EventFilter is used to create pipelines for Event processing.
 */
interface EventFilter : (Events) -> Events {
    companion object {
        operator fun invoke(fn: (Events) -> Events) = object : EventFilter {
            override operator fun invoke(next: Events): Events = fn(next)
        }
    }
}

fun EventFilter.then(next: EventFilter): EventFilter = EventFilter { this(next(it)) }
fun EventFilter.then(next: Events): Events = { this(next)(it) }

data class EventCategory(private val name: String) {
    override fun toString(): String = name
}

/**
 * Attach some metadata to this event
 */
operator fun Event.plus(that: Pair<String, Any>): Event = when (this) {
    is MetadataEvent -> MetadataEvent(event, metadata + that)
    else -> MetadataEvent(this, mapOf(that))
}

class MetadataEvent(val event: Event, val metadata: Map<String, Any> = emptyMap()) : Event by event {
    override fun toString() = "MetadataEvent(event=$event, metadata=$metadata)"

    override fun hashCode(): Int {
        var result = event.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetadataEvent

        if (event != other.event) return false
        if (metadata != other.metadata) return false

        return true
    }
}