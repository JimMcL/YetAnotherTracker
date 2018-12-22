package com.jim.tracking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by jim_m on 13-May-17.
 */
class CSVWriter {
    private static final char QUOTE = '"';
    private static final char LINE_SEP = '\n';
    private transient Writer writer;
    private String commentPrefix = "# ";

    CSVWriter() {
    }

    CSVWriter(Writer writer) {
        this.writer = new BufferedWriter(writer);
    }

    public void setCommentPrefix(String commentPrefix) {
        this.commentPrefix = commentPrefix;
    }

    public void writeComment(String comment) throws IOException {
        for (String line : comment.split("\n")) {
            writer.append(commentPrefix).append(line).append(LINE_SEP);
        }
    }

    void writeHeaders(String[] headers) throws IOException {
        writeValues(headers);
    }

    void writeValues(Object[] values) throws IOException {
        if (writer != null) {
            StringBuilder buf = new StringBuilder();
            String sep = "";
            for (Object value : values) {
                buf.append(sep);
                appendValue(buf, value);
                sep = ",";
            }
            buf.append(LINE_SEP);
            writer.write(buf.toString());
        }
    }

    void close() throws IOException {
        if (writer != null)
            writer.close();
        writer = null;
    }

    private void appendValue(StringBuilder buf, Object value) {
        boolean quote = value instanceof String;
        if (quote)
            buf.append(QUOTE);
        buf.append(value.toString());
        if (quote)
            buf.append(QUOTE);
    }
}
