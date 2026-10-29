package prosemirrorfx.writer;

import java.util.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.input.*;
import javafx.util.Duration;
import prosemirrorfx.view.embed.MarkdownEditor;

/** Pastes several clipboard shapes into an editor, half a second apart, and prints the Markdown after each. */
public final class PasteTrial {
    public static void main(String[] args) {
        Platform.startup(() -> {
            var steps = new ArrayList<Runnable>();
            steps.add(() -> run("plain lines", Map.of(DataFormat.PLAIN_TEXT, "line one\r\nline two\r\n\r\nline four")));
            steps.add(() -> run("plain one line", Map.of(DataFormat.PLAIN_TEXT, "hello world")));
            steps.add(() -> run("html list", Map.of(DataFormat.PLAIN_TEXT, "a\nb", DataFormat.HTML, "<ul><li>a</li><li><b>b</b></li></ul>")));
            steps.add(() -> run("html word-like", Map.of(DataFormat.PLAIN_TEXT, "Title\nPara", DataFormat.HTML,
                    "<html><body><!--StartFragment--><h2>Title</h2><p class=MsoNormal>Para <i>it</i></p><!--EndFragment--></body></html>")));
            steps.add(Platform::exit);
            var t = new Timeline();
            for (var i = 0; i < steps.size(); i++) {
                var s = steps.get(i);
                t.getKeyFrames().add(new KeyFrame(Duration.millis(500 * (i + 1)), e -> s.run()));
            }
            t.play();
        });
    }

    static void run(String name, Map<DataFormat, Object> content) {
        var md = new MarkdownEditor();
        md.setMarkdown("Start here.\n");
        md.setCaret(6);
        var cb = Clipboard.getSystemClipboard();
        cb.setContent(content);
        System.err.println("hasString=" + cb.hasString() + " hasHtml=" + cb.hasHtml());
        try {
            var ok = md.paste();
            System.err.println("== " + name + " ok=" + ok + "\n" + md.getMarkdown());
        } catch (Throwable t) {
            System.err.println(name + " THREW " + t);
            t.printStackTrace();
        }
    }
}
