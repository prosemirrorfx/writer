package prosemirrorfx.writer;

import com.atlassian.prosemirror.state.*;
import prosemirrorfx.core.editor.EditorModel;
import prosemirrorfx.markdown.*;

/** Feeds browser-style clipboard HTML through the paste reader and reports what comes out or what throws. */
public final class HtmlTrial {
    static final String[][] CASES = {
        {"chrome", "<meta charset='utf-8'><span style=\"color: rgb(32, 33, 36); font-family: Arial;\">Hello from <b>Chrome</b></span>"},
        {"gdocs", "<meta charset='utf-8'><b style=\"font-weight:normal;\" id=\"docs-internal-guid-1\"><p dir=\"ltr\" style=\"line-height:1.38;\"><span style=\"font-size:11pt;\">First para</span></p><ul style=\"margin-top:0;\"><li dir=\"ltr\" style=\"list-style-type:disc;\"><p dir=\"ltr\" role=\"presentation\"><span>bullet</span></p></li></ul><br></b>"},
        {"table", "<table><tr><td>a</td><td>b</td></tr></table>"},
        {"img", "<p>x<img src=\"data:image/png;base64,AAAA\">y</p>"},
        {"pre", "<pre><code>int x = 1;\nint y = 2;</code></pre>"},
        {"github", "<div class=\"markdown-body\"><h2 dir=\"auto\">Title</h2><p dir=\"auto\">Text with <code>code</code> and <a href=\"https://x\">link</a></p><blockquote><p>q</p></blockquote></div>"},
        {"nested-list", "<ol><li>one<ul><li>inner</li></ul></li><li>two</li></ol>"},
        {"wt", "<!DOCTYPE html><HTML><HEAD></HEAD><BODY><!--StartFragment--><DIV STYLE=\"display:inline;white-space:pre;font-family:'Cascadia Mono';font-size:12pt;\"><SPAN STYLE=\"color:#CCCCCC;\">line one<BR>line two<BR></SPAN></DIV><!--EndFragment--></BODY></HTML>"},
        {"vscode", "<meta charset='utf-8'><div style=\"color: #cccccc;background-color: #1f1f1f;white-space: pre;\"><div><span style=\"color: #569cd6;\">var</span><span> x = 1;</span></div><div><span>next</span></div></div>"},
        {"word", "<html xmlns:o=\"urn:schemas-microsoft-com:office:office\"><head><style><!-- p.MsoNormal {margin:0} --></style></head><body lang=EN-US><!--StartFragment--><p class=MsoNormal>Word <b>bold</b><o:p></o:p></p><p class=MsoListParagraph style=\"mso-list:l0 level1 lfo1\"><![if !supportLists]><span>1.<span>&nbsp;&nbsp; </span></span><![endif]>item</p><!--EndFragment--></body></html>"},
        {"empty-div", "<div></div>"},
        {"span-only", "<span>just text</span>"},
        {"br-only", "a<br>b"},
    };

    public static void main(String[] args) {
        for (var c : CASES) {
            try {
                var blocks = Html.read(c[1]);
                var m = new EditorModel(Lossless.read("Start here.\n"));
                var tr = m.state().getTr().setSelection(prosemirrorfx.core.pm.Pm.cursor(m.state().getDoc(), 6));
                tr.replaceSelection(Html.pasteSlice(blocks));
                m.dispatch(tr);
                System.out.println("== " + c[0] + " OK\n" + Markdown.write(m.state().getDoc()));
            } catch (Throwable t) {
                System.out.println("== " + c[0] + " THREW " + t);
                for (var e : t.getStackTrace()) if (e.getClassName().startsWith("prosemirrorfx")) { System.out.println("   at " + e); }
            }
        }
    }
}
