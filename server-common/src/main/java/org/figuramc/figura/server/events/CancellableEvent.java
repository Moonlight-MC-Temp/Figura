package org.figuramc.figura.server.events;

public abstract class CancellableEvent extends Event {
    private boolean cancelled;

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean canContinue() {
        return !cancelled;
    }
}
