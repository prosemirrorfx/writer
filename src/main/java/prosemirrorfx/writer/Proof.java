package prosemirrorfx.writer;

import com.atlassian.prosemirror.model.Node;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import prosemirrorfx.view.DocView;

import static prosemirrorfx.core.schema.EditorSchema.*;

/**
 * Spelling and grammar, underlined in the text. Off until the user asks in this document (the text goes to the
 * checker's service), then checked again a moment after each pause in typing. Next goes to the following issue and
 * offers its fixes: Enter applies the chosen one, Esc leaves it.
 */
final class Proof {

    final DocView view;
    final Consumer<String> problem;
    Runnable changed = () -> {};
    boolean on, checking;
    int issues;
    final PauseTransition idle = new PauseTransition(Duration.seconds(2));
    Popup fixes;

    Proof(DocView view, Consumer<String> problem) {
        this.view = view;
        this.problem = problem;
        idle.setOnFinished(e -> check());
        view.model().onChange((old, now) -> {
            if (on && old.getDoc() != now.getDoc()) idle.playFromStart();
        });
    }

    static Proofreader reader() { var all = Proofreaders.all(); return all.isEmpty() ? null : all.getFirst(); }

    boolean available() { return reader() != null; }

    /** Turns proofreading on, or off and clears the underlines. */
    void toggle() {
        on = !on;
        if (on) check();
        else { idle.stop(); view.setMarks(List.of()); issues = 0; }
        changed.run();
    }

    /** The first press turns proofreading on; after that, the next issue after the caret, with its fixes. */
    void next() {
        if (!on) { if (available()) toggle(); else problem.accept(NONE); return; }
        var marks = view.marks();
        if (marks.isEmpty()) return;
        var sel = view.model().state().getSelection();
        var at = sel.getEmpty() ? sel.getHead() : sel.getFrom() + 1;
        var m = marks.stream().filter(k -> k.from() >= at).findFirst().orElse(marks.getFirst());
        view.selectRange(m.from(), m.to());
        // the caret is drawn on the next pulse; place the fixes under it after that
        var later = new PauseTransition(Duration.millis(60));
        later.setOnFinished(e -> offer(m));
        later.play();
    }

    static final String NONE = "No spelling and grammar checker is installed. Put its jar in " + Proofreaders.PLUGINS + ".";

    void check() {
        var r = reader();
        if (r == null) return;
        var doc = view.model().state().getDoc();
        var text = Text.of(doc);
        checking = true;
        changed.run();
        CompletableFuture.supplyAsync(() -> {
            try {
                return r.check(text.chars.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((found, err) -> Platform.runLater(() -> {
            checking = false;
            if (!on) return;
            if (err != null) { problem.accept(r.name() + " could not check the text: " + Dictate.rootMessage(err)); on = false; changed.run(); return; }
            // edited while the check ran: the idle timer is already counting down to a fresh one
            if (view.model().state().getDoc() != doc) { changed.run(); return; }
            var marks = new ArrayList<DocView.Mark>();
            for (var i : found) {
                if (i.begin() < 0 || i.end() > text.chars.length() || i.end() <= i.begin() || text.inCode(i.begin(), i.end())) continue;
                marks.add(new DocView.Mark(text.pos[i.begin()], text.pos[i.end() - 1] + 1, i.kind().name().toLowerCase(Locale.ROOT), i));
            }
            marks.sort(Comparator.comparingInt(DocView.Mark::from));
            view.setMarks(marks);
            issues = marks.size();
            changed.run();
        }));
    }

    void offer(DocView.Mark m) {
        if (fixes != null) fixes.hide();
        var issue = (Proofreader.Issue) m.data();
        var list = new ListView<String>();
        list.getItems().addAll(issue.replacements().stream().limit(6).toList());
        list.getItems().add(IGNORE);
        list.setPrefHeight(Math.min(7, list.getItems().size()) * 26 + 4);
        list.setPrefWidth(260);
        list.getSelectionModel().selectFirst();
        var title = new Label(issue.title() == null ? "" : issue.title().replaceAll("<[^>]+>", ""));
        title.getStyleClass().add("wr-fix-title");
        var box = new VBox(title, list);
        box.getStyleClass().add("wr-fixes");
        box.getStylesheets().add(Writer.class.getResource("writer.css").toExternalForm());
        var t = view.theme();
        box.setStyle("-fx-background-color: " + t.colour("panel") + "; -fx-border-color: " + t.colour("border") + ";");
        title.setStyle("-fx-text-fill: " + t.colour("text") + ";");
        var popup = new Popup();
        popup.getContent().add(box);
        popup.setAutoHide(true);
        Runnable apply = () -> {
            var pick = list.getSelectionModel().getSelectedItem();
            popup.hide();
            if (pick == null) return;
            if (pick.equals(IGNORE)) view.setMarks(view.marks().stream().filter(k -> k.data() != issue).toList());
            else {
                var now = view.marks().stream().filter(k -> k.data() == issue).findFirst();
                now.ifPresent(k -> view.model().dispatch(view.model().state().getTr().insertText(pick, k.from(), k.to())));
            }
            issues = view.marks().size();
            changed.run();
            view.requestFocus();
        };
        list.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { apply.run(); e.consume(); }
            else if (e.getCode() == KeyCode.ESCAPE) { popup.hide(); view.requestFocus(); e.consume(); }
        });
        list.setOnMouseClicked(e -> apply.run());
        var at = view.caretOnScreen();
        var window = view.getScene() == null ? null : view.getScene().getWindow();
        if (window == null) return;
        if (at == null) popup.show(window); else popup.show(window, at.getMinX(), at.getMaxY() + 4);
        list.requestFocus();
        fixes = popup;
    }

    static final String IGNORE = "Ignore";

    String describe() {
        if (!on) return "";
        if (checking) return "Checking…";
        return issues == 0 ? "No issues" : issues == 1 ? "1 issue" : issues + " issues";
    }

    void close() { idle.stop(); if (fixes != null) fixes.hide(); }

    /**
     * The document as plain text for a checker: paragraphs joined by "\n", code blocks left out, and for each
     * character the document position it came from. Inline code is noted so issues inside it can be dropped.
     */
    static final class Text {
        final StringBuilder chars = new StringBuilder();
        int[] pos = new int[256];
        final BitSet code = new BitSet();

        static Text of(Node doc) {
            var t = new Text();
            doc.descendants((n, start, parent, i) -> {
                if (!n.isTextblock()) return true;
                if (Boolean.TRUE.equals(n.getAttrs().get("verbatim"))) return false;
                if (!t.chars.isEmpty()) t.add('\n', start, false);
                var p = start + 1;
                for (var c : n.getContent().getContent()) {
                    if (c.isText()) {
                        var inCode = c.getMarks().stream().anyMatch(k -> k.getType() == CODE);
                        var s = c.getText();
                        for (var k = 0; k < s.length(); k++) t.add(s.charAt(k), p + k, inCode);
                    } else t.add(c.getType() == HARD_BREAK ? '\n' : ' ', p, false);
                    p += c.getNodeSize();
                }
                return false;
            });
            return t;
        }

        void add(char c, int at, boolean inCode) {
            var i = chars.length();
            if (i == pos.length) pos = Arrays.copyOf(pos, i * 2);
            pos[i] = at;
            if (inCode) code.set(i);
            chars.append(c);
        }

        boolean inCode(int from, int to) {
            var next = code.nextSetBit(from);
            return next >= 0 && next < to;
        }
    }
}
