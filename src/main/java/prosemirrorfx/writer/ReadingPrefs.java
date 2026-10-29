package prosemirrorfx.writer;

import java.nio.file.*;
import java.util.regex.Pattern;

/** Reading speed and voice, remembered between runs in ~/prosemirrorfx/writer/reading.json. */
final class ReadingPrefs {

    static final Path FILE = Path.of(System.getProperty("user.home"), "prosemirrorfx", "writer", "reading.json");
    static final Pattern RATE = Pattern.compile("\"rate\"\\s*:\\s*([0-9.]+)"), VOICE = Pattern.compile("\"voice\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    static double rate = 1.0;
    static String voice;

    static {
        try {
            var s = Files.readString(FILE);
            var r = RATE.matcher(s);
            if (r.find()) rate = Double.parseDouble(r.group(1));
            var v = VOICE.matcher(s);
            if (v.find()) voice = v.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (Exception ignored) {
            // first run, or an unreadable file: defaults
        }
    }

    static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            var v = voice == null ? "null" : "\"" + voice.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            Files.writeString(FILE, "{\"rate\": " + rate + ", \"voice\": " + v + "}\n");
        } catch (Exception e) {
            System.err.println("prosemirrorfx writer: could not save reading preferences: " + e);
        }
    }

    /** A Windows voice takes seconds to start, so a saved one is worth starting before the first read. */
    static boolean savedVoiceIsSlowToStart() { return voice != null && !voice.startsWith("Deepgram"); }

    private ReadingPrefs() {}
}
