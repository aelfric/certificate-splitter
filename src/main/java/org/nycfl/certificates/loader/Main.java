
package org.nycfl.certificates.loader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "split-certs",
        description = "splits up a certificates PDF and uploads it to AWS",
        version = "certificates-splitter 0.0.3",
        mixinStandardHelpOptions = true,
        subcommands = GenerateCompletion.class
)
public class Main implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Parameters(index = "0", description = "The file to split")
    File sourceFile;

    @Parameters(index = "1", description = "the tournament ID")
    int tournamentId;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        String filename = sourceFile.getName();
        List<SplitPdfDescriptor> splitDescriptors = getSplitDescriptors(tournamentId);

        List<SplitPdf> files = splitFiles(sourceFile, filename, splitDescriptors);

        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build()) {

            String bucket = "nycfl-certs";
            for (SplitPdf file : files) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(file.targetFileName())
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                log.info("Uploading {} to S3", file);
                s3.putObject(putObjectRequest, RequestBody.fromFile(file.tempFile()));
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to upload to S3", e);
            return -1;
        }
    }

    List<SplitPdf> splitFiles(File input,
                              String filename,
                              List<SplitPdfDescriptor> splitDescriptors)
            throws IOException {
        try (PDDocument document = Loader.loadPDF((input))) {
            PageExtractor extractor = new PageExtractor(document);

            List<Optional<SplitPdf>> files = splitDescriptors
                    .stream()
                    .map(descriptor -> splitFile(filename, document, extractor, descriptor))
                    .toList();

            if (files.stream().allMatch(Optional::isPresent)) {
                return files
                        .stream()
                        .<SplitPdf>mapMulti(Optional::ifPresent)
                        .toList();
            } else {
                throw new IllegalArgumentException("One or more files failed...giving up.");
            }
        }

    }

    private Optional<SplitPdf> splitFile(String filename, PDDocument document, PageExtractor extractor,
                                         SplitPdfDescriptor descriptor) {
        try {
            String outputFileName = descriptor.getOutputFile(filename);
            Path outputFilePath = Files.createTempFile("", outputFileName);

            if (outputFileName.length() > 255) {
                document.close();
                log.error("This filename will be too long [{}]...giving up", outputFileName);
                return Optional.empty();
            }
            log.info("Splitting out {}", descriptor);
            extractor.setStartPage(descriptor.startPage());
            extractor.setEndPage(descriptor.endPage());
            PDDocument extract = extractor.extract();

            extract.save(outputFilePath.toFile());
            extract.close();
            log.info("Writing to {}", outputFilePath);
            return Optional.of(
                    new SplitPdf(
                            outputFilePath,
                            outputFileName
                    )
            );
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    public static List<SplitPdfDescriptor> getSplitDescriptors(int tournamentId) throws IOException,
            URISyntaxException {
        InputStream body = new URI(
            "https://forensics.frankriccobono.com/certs/tournaments/%d/certificates/index"
                .formatted(tournamentId)
        )
            .toURL()
            .openStream();

        try (
                CSVParser
                        records =
                        CSVParser.parse(body,
                                StandardCharsets.UTF_8,
                                CSVFormat.DEFAULT
                                        .builder()
                                        .setHeader()
                                        .setSkipHeaderRecord(true)
                                        .get()
                        )
        ) {
            return records
                    .stream()
                    .map(Main::getSplitPdfDescriptor)
                    .toList();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private static SplitPdfDescriptor getSplitPdfDescriptor(CSVRecord row) {
        String event = row.get("event");
        int startPage = Integer.parseInt(row.get("startPage"));
        int endPage = Integer.parseInt(row.get("endPage"));

        return new SplitPdfDescriptor(
                startPage,
                endPage,
                event
        );
    }

}
