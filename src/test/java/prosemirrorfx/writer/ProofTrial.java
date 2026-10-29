package prosemirrorfx.writer;

import java.nio.file.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import prosemirrorfx.view.Theme;
import prosemirrorfx.view.embed.MarkdownEditor;

/**
 * Proofreads a file with whatever proofreader is on the class path: turns it on, snapshots the underlines, opens the
 * first issue's fixes and applies the first, types above the rest to see them follow. args: file outDir [dark].
 */
public final class ProofTrial {
    public static void main(String[] args) throws Exception {
        var text = Files.readString(Path.of(args[0]));
        var out = Path.of(args[1]);
        Files.createDirectories(out);
        Platform.startup(() -> {
            var md = new MarkdownEditor();
            md.setMarkdown(text);
            if (args.length > 2) md.view().setTheme(Theme.DARK);
            var w = new Writer(md);
            var stage = new Stage();
            stage.setScene(new Scene(new VBox(md.toolbar(), w.bar(), md.node()), 900, 520));
            stage.show();
            md.setCaret(1);
            w.nextIssue();
            Thread.ofPlatform().start(() -> {
                try {
                    waitFor(() -> !w.proof.checking && !w.proof.describe().isEmpty(), 60);
                    Thread.sleep(300);
                    fx(() -> { say(w, "after check"); shot(stage.getScene(), out.resolve("proof-1-marks.png")); });
                    fx(w::nextIssue);
                    Thread.sleep(600);
                    fx(() -> {
                        var p = w.proof.fixes;
                        System.out.println("popup shown=" + (p != null && p.isShowing()));
                        if (p != null) shot(p.getScene(), out.resolve("proof-2-fixes.png"));
                        var list = p.getScene().getRoot().lookup(".list-view");
                        Event.fireEvent(list, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
                    });
                    Thread.sleep(300);
                    fx(() -> say(w, "after applying the first fix"));
                    fx(() -> md.view().model().dispatch(md.view().model().state().getTr().insertText("Typed above. ", 1, 1)));
                    Thread.sleep(300);
                    fx(() -> { say(w, "after typing above"); shot(stage.getScene(), out.resolve("proof-3-moved.png")); });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> { w.close(); Platform.exit(); });
            });
        });
    }

    static void say(Writer w, String when) {
        var doc = w.md.view().model().state().getDoc();
        System.out.println("-- " + when + ": " + w.describe());
        for (var m : w.md.view().marks()) System.out.println("   " + m.kind() + " [" + doc.textBetween(m.from(), m.to(), "\n", "\n") + "] -> " + ((Proofreader.Issue) m.data()).replacements());
    }

    static void waitFor(java.util.function.BooleanSupplier ok, int seconds) throws InterruptedException {
        for (var i = 0; i < seconds * 10; i++) {
            var done = new boolean[1];
            fx(() -> done[0] = ok.getAsBoolean());
            if (done[0]) return;
            Thread.sleep(100);
        }
    }

    static void fx(Runnable r) throws InterruptedException {
        var latch = new java.util.concurrent.CountDownLatch(1);
        Platform.runLater(() -> { try { r.run(); } finally { latch.countDown(); } });
        latch.await();
    }

    static void shot(Scene s, Path to) {
        try {
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(s.snapshot(null), null), "png", to.toFile());
        } catch (Exception e) { e.printStackTrace(); }
    }
}
