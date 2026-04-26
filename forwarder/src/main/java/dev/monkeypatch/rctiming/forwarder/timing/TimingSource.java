package dev.monkeypatch.rctiming.forwarder.timing;

/**
 * Timing data source interface (TIMING-05).
 *
 * <p>Implementations connect to an AMB decoder (or simulator) and push parsed passings
 * to the registered callback. Two implementations can be swapped without changes to
 * downstream race control or timing logic.
 *
 * <p>Lifecycle: call {@link #start()} once; call {@link #stop()} to release resources.
 * Both methods are idempotent.
 */
public interface TimingSource {
    /** Start consuming passings; non-blocking. */
    void start();

    /** Stop and release all resources. Idempotent. */
    void stop();
}
