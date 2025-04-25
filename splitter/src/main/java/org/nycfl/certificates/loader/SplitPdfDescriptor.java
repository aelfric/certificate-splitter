package org.nycfl.certificates.loader;

public record SplitPdfDescriptor(
    int startPage,
    int endPage,
    String event
) {
    public String getOutputFile(final String filename) {
        return filename.replace(".pdf", " - " + event + ".pdf");
    }
}
