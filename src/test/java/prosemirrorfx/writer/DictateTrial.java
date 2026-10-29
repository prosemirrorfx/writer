package prosemirrorfx.writer;

import java.nio.file.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import prosemirrorfx.view.embed.MarkdownEditor;

/** Simulates dictation in the middle of a paragraph: interim, then final; snapshots each. args file outDir. */
public final class DictateTrial {
    public static void main(String[] args) throws Exception {
        var text = Files.readString(Path.of(args[0]));
        var out = Path.of(args[1]);
        Files.createDirectories(out);
        Platform.startup(() -> {
            var md = new MarkdownEditor();
            md.setMarkdown(text);
            var w = new Writer(md);
            var stage = new Stage();
            stage.setScene(new Scene(new VBox(md.toolbar(), w.bar(), md.node()), 900, 400));
            stage.show();
            md.setCaret(md.view().sourceMap().docPositionAtOffset(text.indexOf(" fox")));
            md.focus();
            md.view().setInterimAtCaret("and very sleepy");
            later(() -> {
                shot(stage, out.resolve("dict-interim.png"));
                md.view().insertCommitted("and very sleepy");
                later(() -> {
                    shot(stage, out.resolve("dict-final.png"));
                    System.err.println("MD: " + md.getMarkdown().lines().skip(2).findFirst().orElse(""));
                    Platform.exit();
                });
            });
        });
    }

    static void later(Runnable r) {
        var p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(600));
        p.setOnFinished(e -> r.run());
        p.play();
    }

    static void shot(Stage stage, Path file) {
        try { javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png", file.toFile()); } catch (Exception e) { e.printStackTrace(); }
    }
}
