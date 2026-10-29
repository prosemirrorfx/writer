package prosemirrorfx.writer;

import speechfx.readaloud.*;

public final class EngineTrial {
    public static void main(String[] args) throws Exception {
        var v = args.length > 0 && args[0].equals("windows") ? ReadAloud.windows() : ReadAloud.all();
        System.err.println("voices " + v.voices().size() + " selected " + v.selected());
        var done = new java.util.concurrent.CountDownLatch(1);
        v.speak(args.length > 1 ? java.nio.file.Files.readString(java.nio.file.Path.of(args[1])).substring(2) : "The quick brown fox jumps.", 1.0, new SpeechListener() {
            @Override public void speaking(int from, int to, boolean est) { System.err.println("speaking " + from + "-" + to + " " + est); }
            @Override public void finished(boolean stopped) { System.err.println("finished " + stopped); done.countDown(); }
            @Override public void error(String m) { System.err.println("error " + m); done.countDown(); }
        });
        System.err.println("await " + done.await(40, java.util.concurrent.TimeUnit.SECONDS));
        v.close();
        System.exit(0);
    }
}
