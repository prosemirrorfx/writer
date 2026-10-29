package prosemirrorfx.writer;

import java.util.Map;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import prosemirrorfx.view.ToolbarIcons;
import speechfx.dictation.controls.*;

import static speechfx.dictation.DictationState.*;

/**
 * One row under the editor's toolbar: Dictate / Stop / Mic / Speech, then Read / Stop / speed / Voice, then what is
 * happening. Every button wears its key, and the key and the button call the same method of the Writer.
 */
final class WriterBar extends HBox {

    static final Map<String, String> ICONS = ToolbarIcons.load(WriterBar.class, "icons.tsv");

    final Writer w;
    final Button dictate = button(), stopDictation = button(), mic = button(), speech = button();
    final Button read = button(), stopReading = button(), slower = button(), faster = button(), voice = button();
    final Label speed = new Label(), state = new Label();

    WriterBar(Writer w) {
        this.w = w;
        getStyleClass().addAll("pm-toolbar", "wr-bar");
        setAlignment(Pos.CENTER_LEFT);
        dictate.setOnAction(e -> w.dictate());
        stopDictation.setOnAction(e -> w.stopDictation());
        mic.setOnAction(e -> MicMenu.show(mic, w::useMicrophone));
        speech.setOnAction(e -> SpeechMenu.show(speech));
        read.setOnAction(e -> w.read());
        stopReading.setOnAction(e -> w.stopReading());
        slower.setOnAction(e -> w.slower());
        faster.setOnAction(e -> w.faster());
        voice.setOnAction(e -> w.voiceMenu(voice));
        icon(stopDictation, "stop", "Stop dictation");
        icon(mic, "mic", "Microphone");
        speech.setText("Speech");
        speech.setTooltip(new Tooltip("Speech service and key"));
        icon(stopReading, "stop", "Stop reading (Esc)");
        slower.setText("−");
        slower.setTooltip(new Tooltip("Slower"));
        faster.setText("+");
        faster.setTooltip(new Tooltip("Faster"));
        icon(voice, "record_voice_over", "Voice");
        speed.getStyleClass().add("wr-speed");
        state.getStyleClass().add("wr-state");
        var gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        getChildren().addAll(dictate, stopDictation, mic, speech, new Separator(Orientation.VERTICAL),
                read, stopReading, slower, speed, faster, voice, gap, state);
        refresh();
    }

    static Button button() {
        var b = new Button();
        b.setMnemonicParsing(false);
        b.setFocusTraversable(false);
        return b;
    }

    static void icon(Button b, String name, String tip) {
        var g = ToolbarIcons.draw(ICONS.get(name));
        b.setGraphic(g);
        if (g == null) b.setText(tip);
        b.setTooltip(new Tooltip(tip));
    }

    void refresh() {
        var d = w.dictation.state;
        var live = d == LISTENING || d == SWITCHING || d == RECONNECTING;
        dictate.setGraphic(ToolbarIcons.draw(ICONS.get(live ? "pause" : "fiber_manual_record")));
        dictate.setText(Writer.DICTATE_KEY + (live ? " Pause" : d == PAUSED ? " Resume" : " Dictate"));
        dictate.getStyleClass().remove("wr-recording");
        if (live) dictate.getStyleClass().add("wr-recording");
        dictate.setDisable(d == STARTING || d == FINISHING);
        stopDictation.setDisable(!w.dictation.active());
        var r = w.reader;
        read.setGraphic(ToolbarIcons.draw(ICONS.get(r.reading() && !r.paused() ? "pause" : "play_arrow")));
        read.setText(Writer.READ_KEY + (r.reading() ? r.paused() ? " Resume" : " Pause" : " Read"));
        stopReading.setDisable(!r.reading());
        speed.setText(String.format("%.1f×", r.rate));
        state.setText(w.describe());
    }
}
