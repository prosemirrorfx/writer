package prosemirrorfx.writer;

import java.nio.file.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import prosemirrorfx.view.Theme;
import prosemirrorfx.view.embed.MarkdownEditor;

/** Opens the writer on a file, reads it aloud and saves a snapshot every half second: args file outDir [dark]. */
public final class ReadTrial {
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
            stage.setScene(new Scene(new VBox(md.toolbar(), w.bar(), md.node()), 900, 500));
            stage.show();
            md.setCaret(1);
            w.read();
            Thread.ofPlatform().start(() -> {
                for (var i = 0; i < 12; i++) {
                    try { Thread.sleep(700); } catch (InterruptedException e) { return; }
                    var n = i;
                    Platform.runLater(() -> {
                        try {
                            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png", out.resolve("read-" + n + ".png").toFile());
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
                Platform.runLater(() -> { w.close(); Platform.exit(); });
            });
        });
    }
}
