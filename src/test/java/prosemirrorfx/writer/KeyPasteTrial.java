package prosemirrorfx.writer;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.stage.Stage;
import prosemirrorfx.view.embed.MarkdownEditor;

/** Fires a real Ctrl+V key event at the editor with whatever is on the Windows clipboard now, and prints the result. */
public final class KeyPasteTrial {
    public static void main(String[] args) {
        Platform.startup(() -> {
            var cb = Clipboard.getSystemClipboard();
            System.err.println("formats=" + cb.getContentTypes() + "\nhtml=" + cb.getHtml() + "\nstring=" + cb.getString());
            var md = new MarkdownEditor();
            md.setMarkdown("Start here.\n");
            var stage = new Stage();
            stage.setScene(new Scene(new javafx.scene.layout.StackPane(md.node()), 600, 300));
            stage.show();
            md.setCaret(6);
            md.focus();
            try {
                Event.fireEvent(md.node(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.V, false, true, false, false));
            } catch (Throwable t) { t.printStackTrace(); }
            System.err.println("== after Ctrl+V\n" + md.getMarkdown());
            Platform.exit();
        });
    }
}
