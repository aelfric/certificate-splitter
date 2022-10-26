package org.nycfl.certificates.loader;

import java.nio.file.Path;

public record SplitPdf(
    Path tempFile,
    String targetFileName
) {
}
