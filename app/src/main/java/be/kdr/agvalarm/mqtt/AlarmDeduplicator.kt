package be.kdr.agvalarm.mqtt

/**
 * Same topic + payload within [windowMs] produces a single notification.
 */
class AlarmDeduplicator(
    private val windowMs: Long = 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private data class Entry(val topic: String, val payload: String, val at: Long)

    private val recent = ArrayDeque<Entry>()

    @Synchronized
    fun shouldNotify(topic: String, payload: String): Boolean {
        val now = clock()
        while (recent.isNotEmpty() && now - recent.first().at > windowMs) {
            recent.removeFirst()
        }
        if (recent.any { it.topic == topic && it.payload == payload }) {
            return false
        }
        recent.addLast(Entry(topic, payload, now))
        return true
    }
}
