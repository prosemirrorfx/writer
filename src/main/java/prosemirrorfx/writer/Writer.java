package prosemirrorfx.writer;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import prosemirrorfx.view.embed.MarkdownEditor;
import speechfx.dictation.controls.*;

import static speechfx.dictation.DictationState.*;

/**
 * The Markdown editor as a small word processor: dictation while editing (grey words at the caret, finished text
 * inserted there while the user keeps typing) and read aloud from the caret or the selection, the word and its line
 * highlighted, speed and voice adjustable. Keys, voices and microphones come from the shared speechfx settings; this
 * class holds none of them. The host puts bar() under the editor's toolbar and binds the keys to the methods here.
 */
public final class Writer {

    public static final String DICTATE_KEY = "F3", READ_KEY = "F9";

    final MarkdownEditor md;
    final Dictate dictation;
    final Reader reader;
    WriterBar bar;
    Runnable stopSpeechWatch;

    public Writer(MarkdownEditor md) {
        this.md = md;
        dictation = new Dictate(md.view(), this::problem);
        reader = new Reader(md.view(), this::problem);
        dictation.changed = this::refresh;
        reader.changed = this::refresh;
    }

    /** The row of dictation and read-aloud controls, made on first use. FX thread. */
    public Node bar() {
        if (bar == null) {
            bar = new WriterBar(this);
            bar.getStylesheets().add(Writer.class.getResource("writer.css").toExternalForm());
        }
        return bar;
    }

    /** Starts dictation, or pauses and resumes it. */
    public void dictate() {
        if (dictation.state == IDLE || dictation.state == STOPPED || dictation.state == ERROR) watchSpeechChoice();
        dictation.toggle();
        md.focus();
    }

    public void stopDictation() { dictation.stop(); md.focus(); }

    /** Starts reading from the caret (or the selection), or pauses and resumes it. */
    public void read() { reader.toggle(); md.focus(); }

    public void stopReading() { reader.stop(); md.focus(); }

    public void slower() { reader.rate(reader.rate - 0.1); }

    public void faster() { reader.rate(reader.rate + 0.1); }

    /** Stops reading, else dictation; false when neither was running (so the key can go on to others). */
    public boolean stop() {
        if (reader.reading()) { stopReading(); return true; }
        if (dictation.active()) { stopDictation(); return true; }
        return false;
    }

    public boolean busy() { return reader.reading() || dictation.active(); }

    /** What is happening, for a status bar: "Listening · Deepgram Flux", "Reading 1.2×", or empty. */
    public String describe() {
        var d = dictation.state;
        var listening = switch (d) {
            case STARTING -> "Starting dictation…";
            case LISTENING, SWITCHING -> "Listening · " + SpeechMenu.providerName();
            case PAUSED -> "Dictation paused";
            case RECONNECTING -> "Reconnecting…";
            case FINISHING -> "Finishing…";
            default -> "";
        };
        var reading = reader.reading() ? (reader.paused() ? "Reading paused" : "Reading") + String.format(" %.1f×", reader.rate) : "";
        return listening.isEmpty() ? reading : reading.isEmpty() ? listening : listening + "   " + reading;
    }

    void useMicrophone(String name) {
        dictation.switchMic(name);
    }

    void watchSpeechChoice() {
        if (stopSpeechWatch == null) stopSpeechWatch = SpeechMenu.onChange(() -> Platform.runLater(dictation::restart));
    }

    void voiceMenu(Node anchor) {
        reader.voice().thenAccept(v -> {
            var names = v.voices();
            Platform.runLater(() -> {
                var menu = new ContextMenu();
                var current = v.selected();
                for (var n : names) {
                    var item = new CheckMenuItem(n);
                    item.setMnemonicParsing(false);
                    item.setSelected(n.equals(current));
                    item.setOnAction(e -> reader.select(n));
                    menu.getItems().add(item);
                }
                menu.show(anchor, Side.BOTTOM, 0, 0);
            });
        });
    }

    void refresh() { if (bar != null) Platform.runLater(bar::refresh); }

    void problem(String message) {
        var owner = md.node().getScene() == null ? null : md.node().getScene().getWindow();
        Platform.runLater(() -> Dialogs.message(owner, "Writer", message, Dialogs.Kind.WARNING));
    }

    /** Stops whatever is running and lets the voice engine go. */
    public void close() {
        reader.close();
        dictation.stop();
        if (stopSpeechWatch != null) stopSpeechWatch.run();
    }
}
