package prosemirrorfx.writer;

import java.util.*;
import javafx.application.Platform;
import javafx.scene.input.*;
import prosemirrorfx.view.embed.MarkdownEditor;

/** Copies a range inside the editor and pastes it elsewhere, for several shapes; restores the user's clipboard after. */
public final class CopyPasteTrial {
    static final String DOC = "# Head\n\nFirst paragraph here.\n\n- item one\n- item two\n\n1. num one\n2. num two\n\nLast paragraph.\n";

    public static void main(String[] args) {
        Platform.startup(() -> {
            var cb = Clipboard.getSystemClipboard();
            var saved = new HashMap<DataFormat, Object>();
            for (var f : cb.getContentTypes()) { try { var v = cb.getContent(f); if (v != null) saved.put(f, v); } catch (Throwable ignored) {} }
            var cases = List.of(
                new String[]{"word in para -> para", "paragraph", "Last"},
                new String[]{"whole para -> para", "First paragraph here.", "Last"},
                new String[]{"list items -> para", "item one\n- item two", "Last"},
                new String[]{"heading -> para", "Head", "Last"},
                new String[]{"para+list -> list item", "here.\n\n- item one", "num two"},
                new String[]{"numbered -> bullet", "num one\n2. num two", "item two"});
            var t = new javafx.animation.Timeline();
            var editors = new ArrayList<MarkdownEditor>();
            for (var i = 0; i < cases.size(); i++) {
                var c = cases.get(i);
                t.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(700 * i + 100), e -> editors.add(copy(c[1]))));
                t.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(700 * i + 500), e -> paste(c[0], editors.getLast(), c[2])));
            }
            t.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(700 * cases.size() + 200), e -> { cb.setContent(saved); Platform.exit(); }));
            t.play();
        });
    }

    static MarkdownEditor copy(String source) {
        var md = new MarkdownEditor();
        md.setMarkdown(DOC);
        var map = md.sourceMap();
        var a = DOC.indexOf(source);
        md.selectRange(map.docPositionAtOffset(a), map.docPositionAtOffset(a + source.length()));
        md.copy();
        return md;
    }

    static void paste(String name, MarkdownEditor md, String target) {
        md.setCaret(md.sourceMap().docPositionAtOffset(DOC.indexOf(target)));
        try {
            var ok = md.paste();
            System.err.println("== " + name + " ok=" + ok + "\n" + md.getMarkdown());
        } catch (Throwable t) {
            System.err.println("== " + name + " THREW " + t);
            for (var e : t.getStackTrace()) if (e.getClassName().startsWith("prosemirrorfx")) System.err.println("   at " + e);
        }
    }
}
