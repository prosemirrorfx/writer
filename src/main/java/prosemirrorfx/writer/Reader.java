package prosemirrorfx.writer;

import com.atlassian.prosemirror.transform.Mapping;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;
import prosemirrorfx.markdown.SourceMap;
import prosemirrorfx.view.DocView;
import speechfx.readaloud.*;

/**
 * Reads the document aloud from the caret, or just the selection, highlighting the word being spoken and its line.
 * The Markdown text is what is spoken (the engine silences its punctuation), so the engine's offsets map back to
 * the document through the source map taken when reading began, then through every edit made since, so the
 * highlight stays on the right word while the user corrects the text. Speed and voice are remembered.
 */
final class Reader implements SpeechListener {

    static final double MIN_RATE = 0.5, MAX_RATE = 3.0;

    final DocView view;
    final Consumer<String> problem;
    static CompletableFuture<Voice> shared;
    SpeechHandle handle;
    SourceMap map;
    Mapping since;
    int base, end, lastFrom;
    double rate = ReadingPrefs.rate;
    Runnable changed = () -> {};

    Reader(DocView view, Consumer<String> problem) {
        this.view = view;
        this.problem = problem;
        view.model().onChange((old, now) -> {
            if (since != null && old.getDoc() != now.getDoc()) since.appendMapping(view.model().lastTransaction().getMapping());
        });
    }

    /** One voice engine for every editor in the process, started on first use, on the voice chosen last time. */
    static synchronized CompletableFuture<Voice> voice() {
        if (shared == null) shared = CompletableFuture.supplyAsync(() -> {
            var v = ReadAloud.all();
            if (ReadingPrefs.voice != null && v.voices().contains(ReadingPrefs.voice)) v.select(ReadingPrefs.voice);
            return v;
        });
        return shared;
    }

    boolean reading() { return handle != null && !handle.done(); }

    boolean paused() { return reading() && handle.paused(); }

    void toggle() {
        if (!reading()) start();
        else if (handle.paused()) handle.resume();
        else handle.pause();
        changed.run();
    }

    void pauseIfReading() { if (reading() && !handle.paused()) toggle(); }

    void start() {
        map = view.sourceMap();
        since = new Mapping(new ArrayList<>(), new ArrayList<>(), 0, 0);
        var sel = view.model().state().getSelection();
        base = map.offset(sel.getFrom());
        end = sel.getEmpty() ? map.text().length() : map.offset(sel.getTo());
        if (base >= end) base = 0;
        speakFrom(base);
    }

    void speakFrom(int offset) {
        lastFrom = offset;
        var text = map.text().substring(offset, end);
        var from = offset;
        voice().whenComplete((v, err) -> Platform.runLater(() -> {
            if (err != null) { problem.accept("Read aloud could not start: " + Dictate.rootMessage(err)); return; }
            base = from;
            handle = v.speak(text, rate, this);
            changed.run();
        }));
    }

    void stop() {
        var h = handle;
        handle = null;
        if (h != null) h.stop();
        view.setReading(-1, -1, false);
        changed.run();
    }

    void rate(double r) {
        rate = Math.round(Math.max(MIN_RATE, Math.min(MAX_RATE, r)) * 10) / 10.0;
        if (handle != null) handle.rate(rate);
        ReadingPrefs.rate = rate;
        ReadingPrefs.save();
        changed.run();
    }

    List<String> voices() { return voice().join().voices(); }

    /** A new voice carries on from the word being read. */
    void select(String name) {
        voice().thenAccept(v -> Platform.runLater(() -> {
            var wasReading = reading() && !handle.paused();
            var at = lastFrom;
            if (handle != null) { handle.stop(); handle = null; }
            v.select(name);
            ReadingPrefs.voice = name;
            ReadingPrefs.save();
            if (wasReading) speakFrom(at);
            changed.run();
        }));
    }

    void close() {
        stop();
    }

    @Override public void speaking(int from, int to, boolean estimated) {
        Platform.runLater(() -> {
            if (handle == null || map == null) return;
            lastFrom = base + from;
            var size = view.model().state().getDoc().getContent().getSize();
            var a = Math.min(size, since.map(map.docPositionAtOffset(base + from), 1));
            var b = Math.min(size, since.map(map.docPositionAtOffset(base + to), -1));
            view.setReading(a, Math.max(a, b), estimated);
        });
    }

    @Override public void finished(boolean stopped) {
        Platform.runLater(() -> {
            if (stopped) return;
            handle = null;
            view.setReading(-1, -1, false);
            changed.run();
        });
    }

    @Override public void error(String message) { Platform.runLater(() -> { problem.accept("Read aloud: " + message); stop(); }); }
}
