# prosemirrorfx writer

The prosemirrorfx Markdown editor as a small word processor you can embed in any JavaFX window:

- **Dictation while editing.** Words still being recognised show grey at the caret; finished text is inserted there while you keep typing.
- **Read aloud.** Reads from the caret, or just the selection, highlighting the word being spoken and its line. Speed and voice can be changed while it reads.

Speech comes from [speechfx](https://github.com/speechfx): `speechfx-dictation` with its controls (microphone, speech service, keys) and `speechfx-readaloud` (Windows voices, Deepgram voices). Keys and settings live in speechfx's own settings folder; this module holds none.

```java
var md = new MarkdownEditor();
var writer = new Writer(md);
var root = new VBox(md.toolbar(), writer.bar(), md.node());
// bind keys to writer.dictate(), writer.read(), writer.stop()
```

`Writer.DICTATE_KEY` and `Writer.READ_KEY` name the keys the bar advertises (F3, F9).

Licence: Apache 2.0.
