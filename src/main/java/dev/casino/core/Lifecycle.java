package dev.casino.core;

/** Implemented by managers that need plugin lifecycle hooks. */
public interface Lifecycle {
    void onEnable();
    void onDisable();
}
