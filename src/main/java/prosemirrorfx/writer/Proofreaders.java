package prosemirrorfx.writer;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

/** The proofreaders on the class path and in ~/prosemirrorfx/writer/plugins/*.jar, found once per process. */
final class Proofreaders {

    static final Path PLUGINS = Path.of(System.getProperty("user.home"), "prosemirrorfx", "writer", "plugins");
    static List<Proofreader> found;

    static synchronized List<Proofreader> all() {
        if (found != null) return found;
        var urls = new ArrayList<URL>();
        try (var jars = Files.newDirectoryStream(PLUGINS, "*.jar")) {
            for (var j : jars) urls.add(j.toUri().toURL());
        } catch (NoSuchFileException none) {
            // no plugins folder: class path only
        } catch (Exception e) {
            System.err.println("prosemirrorfx writer: could not list " + PLUGINS + ": " + e);
        }
        var loader = urls.isEmpty() ? Proofreaders.class.getClassLoader()
                : new URLClassLoader(urls.toArray(URL[]::new), Proofreaders.class.getClassLoader());
        var list = new ArrayList<Proofreader>();
        for (var it = ServiceLoader.load(Proofreader.class, loader).iterator(); ; ) {
            try {
                if (!it.hasNext()) break;
                list.add(it.next());
            } catch (ServiceConfigurationError e) {
                System.err.println("prosemirrorfx writer: a proofreader would not load: " + e);
            }
        }
        return found = List.copyOf(list);
    }

    private Proofreaders() {}
}
