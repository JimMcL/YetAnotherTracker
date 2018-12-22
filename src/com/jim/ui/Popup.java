package com.jim.ui;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;

import java.util.function.Consumer;

/** Abstract base class for popup windows. */
abstract class Popup {
    protected Consumer<Popup> okListener;
    protected Consumer<Popup> cancelListener;

    protected void close(Event event) {
        EventTarget target = event.getTarget();
        Window w = null;
        // Seems weird that it is so hard to close a window from an event
        if (target instanceof Window)
            w = (Window) target;
        else if (target instanceof Scene)
            w = ((Scene) target).getWindow();
        else if (target instanceof Node)
            w = ((Node) target).getScene().getWindow();
        w.hide();
    }

    /** Called on copy key press. */
    protected void doCopy() {
    }

    public void setOkListener(Consumer<Popup> okListener) {
        this.okListener = okListener;
    }

    public void setCancelListener(Consumer<Popup> cancelListener) {
        this.cancelListener = cancelListener;
    }

    // ==========================================================================
    // UI Actions

    public void doOK(Event event) {
        if (okListener != null)
            okListener.accept(this);
        close(event);
    }

    public void doCancel(Event event) {
        if (cancelListener != null)
            cancelListener.accept(this);
        close(event);
    }

    public void onKeyPressed(KeyEvent event) {
        final KeyCodeCombination keyCodeCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
        if (event.getCode() == KeyCode.ESCAPE) {
            doCancel(event);
            event.consume();
        } else if (keyCodeCopy.match(event)) {
            doCopy();
            event.consume();
        }
    }
}
