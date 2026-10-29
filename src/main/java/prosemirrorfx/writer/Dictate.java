package prosemirrorfx.writer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;
import prosemirrorfx.view.DocView;
import speechfx.dictation.*;

import static speechfx.dictation.DictationState.*;

/**
 * Dictation into the editor while the user edits: words still being recognised show grey at the caret, finished
 * text goes in at the caret. Start, pause and resume share one key; stop ends the session and keeps what was said.
 */
final class Dictate implements DictationListener {

    final DocView view;
    final Consumer<String> problem;
    Dictation session;
    volatile DictationState state = IDLE;
    Runnable changed = () -> {};

    Dictate(DocView view, Consumer<String> problem) {
        this.view = view;
        this.problem = problem;
    }

    boolean active() { return state != IDLE && state != STOPPED && state != ERROR; }

    void toggle() {
        if (session == null) start();
        else if (session.paused()) { session.resume(); set(LISTENING); }
        else { session.pause(); set(PAUSED); view.setInterimAtCaret(""); }
    }

    void start() {
        set(STARTING);
        CompletableFuture.supplyAsync(() -> {
            try {
                return Dictation.start(this);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }).whenComplete((d, err) -> Platform.runLater(() -> {
            if (err != null) { set(ERROR); problem.accept("Dictation could not start: " + rootMessage(err)); return; }
            session = d;
            if (state == STARTING) set(LISTENING);
        }));
    }

    CompletableFuture<Void> stop() {
        var s = session;
        session = null;
        view.setInterimAtCaret("");
        if (s == null) { if (state != ERROR) set(IDLE); return CompletableFuture.completedFuture(null); }
        set(FINISHING);
        return s.stop().handle((text, err) -> null).thenRun(() -> Platform.runLater(() -> { view.setInterimAtCaret(""); set(IDLE); }));
    }

    /** Another microphone: a running session moves to it without a gap; otherwise the next start uses it. */
    void switchMic(String name) {
        var s = session;
        if (s == null) { Microphones.choose(name); return; }
        try {
            s.switchMic(name);
        } catch (Exception e) {
            problem.accept("Could not switch to " + name + ": " + rootMessage(e));
        }
    }

    /** A different speech service: a running session moves to it by starting again. */
    void restart() {
        if (session == null) return;
        stop().thenRun(() -> Platform.runLater(this::start));
    }

    String provider() { var s = session; return s == null ? "" : s.provider(); }

    long elapsedMs() { var s = session; return s == null ? 0 : s.elapsedMs(); }

    void set(DictationState s) {
        state = s;
        changed.run();
    }

    static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    @Override public void onFinalText(String text) { view.insertCommitted(text); }

    @Override public void onInterimText(String text) { if (session != null && !session.paused()) view.setInterimAtCaret(text); }

    @Override public void onState(DictationState s, String detail) {
        Platform.runLater(() -> {
            if (session == null || (state == PAUSED && s == LISTENING)) return;
            if (s == ERROR && detail != null) problem.accept("Dictation: " + detail);
            set(s);
        });
    }
}
