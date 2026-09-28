package org.nycfl.certificates.loader;

public record SplitPdfDescriptor(
    int startPage,
    int endPage,
    String event,
    int tournamentId
) {
    public String getOutputFile(final String filename) {
        return filename.replace(".pdf", " - " + event + ".pdf");
    }

    String getS3ObjectKey() {
        return "tournaments/" + tournamentId() + "/" + event() + ".pdf";
    }
}
