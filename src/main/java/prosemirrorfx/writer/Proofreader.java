package prosemirrorfx.writer;

import java.util.List;

/**
 * A spelling and grammar checker the writer can use. Found with ServiceLoader, on the class path or in a jar under
 * ~/prosemirrorfx/writer/plugins/ (META-INF/services/prosemirrorfx.writer.Proofreader names the class). The writer
 * calls check off the FX thread, only after the user asked for proofreading in that document, because the text may
 * leave the machine.
 */
public interface Proofreader {

    /** Shown to the user: "Grammarly". */
    String name();

    /** The issues in text, as character offsets into it. Paragraphs are separated by "\n". */
    List<Issue> check(String text) throws Exception;

    /** [begin, end) in the checked text; replacements best first, possibly none. */
    record Issue(int begin, int end, Kind kind, String title, List<String> replacements) {}

    enum Kind { SPELLING, GRAMMAR, STYLE }
}
