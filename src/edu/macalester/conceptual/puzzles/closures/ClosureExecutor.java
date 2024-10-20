package edu.macalester.conceptual.puzzles.closures;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Methods called from the closure puzzle code.
 */
public class ClosureExecutor {
    public enum Event { TICK, CLICK, KEY };

    private final Map<Event, List<Runnable>> callbacks = new EnumMap<>(Event.class);
    private final Collection<Event> eventQueue = new LinkedList<>();

    public void onEvent(Event event, Runnable action) {
        getCallbacks(event).add(action);
    }

    public void generateEvent(Event event) {
        List.copyOf(getCallbacks(event)).forEach(Runnable::run);
    }

    public void generateEvents(PrintWriter out, Collection<Event> eventQueue) {
        for (var event : eventQueue) {
            out.println();
            out.println(event);
            generateEvent(event);
        }
    }

    private List<Runnable> getCallbacks(Event event) {
        return callbacks.computeIfAbsent(event, (e) -> new ArrayList<>());
    }

    public void twice(Runnable action) {
        action.run();
        action.run();
    }

    public void onClick(Runnable action) {
        onEvent(Event.CLICK, action);
    }

    public void onKeyPress(Runnable action) {
        onEvent(Event.KEY, action);
    }

    public void afterDelay(int ticks, Runnable action) {
        onEvent(Event.TICK, new Runnable() {
            private int countdown = ticks;

            @Override
            public void run() {
                if (0 == --countdown) {
                    action.run();
                }
            }
        });
    }
}
