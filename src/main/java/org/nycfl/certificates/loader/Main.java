
package org.nycfl.certificates.loader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.multipdf.PageExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
  name = "split-certs",
  description = "splits up a certificates PDF and uploads it to AWS",
  version = "certificates-splitter 0.0.2",
  mixinStandardHelpOptions = true
)
public class Main implements Callable<Integer>{
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Parameters(index = "0", description = "The file to split")
    String sourceFile;

    @Parameters(index = "1", description = "the tournament ID")
    int tournamentId;

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        File input = new File(sourceFile);
        String filename = input.getName();
        List<SplitPdfDescriptor> splitDescriptors = getSplitDescriptors(tournamentId);

        PDDocument document = PDDocument.load(input);
        PageExtractor extractor = new PageExtractor(document);

        List<SplitPdf> files = new LinkedList<>();
        for (SplitPdfDescriptor descriptor : splitDescriptors) {
            String outputFile = descriptor.getOutputFile(filename);
            Path tempFile = Files.createTempFile("", outputFile);

            if (outputFile.length() > 255) {
                document.close();
                throw new IllegalArgumentException("This filename will be too long [" + outputFile + "]...giving up");
            }
            log.info("Splitting out {}", descriptor);
            extractor.setStartPage(descriptor.startPage());
            extractor.setEndPage(descriptor.endPage());
            PDDocument extract = extractor.extract();

            extract.save(tempFile.toFile());
            extract.close();
            log.info("Writing to {}", tempFile);
            files.add(
                new SplitPdf(
                    tempFile,
                    outputFile
                )
            );
        }
        document.close();

        S3Client s3 = S3Client.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.US_EAST_1)
            .build();

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
    }

    public static List<SplitPdfDescriptor> getSplitDescriptors(int tournamentId) throws IOException,
        URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
            .newBuilder()
            .uri(
                new URI(
                    "https://forensics.frankriccobono.com/certs/tournaments/%d/certificates/index"
                        .formatted(tournamentId)
                )
            )
            .build();
        HttpResponse<String> send = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        try (
            CSVParser
                records =
                CSVParser.parse(send.body(),
                    CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                )
        ) {
            return records
                .stream()
                .map(Main::getSplitPdfDescriptor)
                .toList();
        } catch (IOException e) {
            e.printStackTrace();
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
