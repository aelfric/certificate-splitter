package org.nycfl.certificates.loader;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void canGetDescriptors() throws IOException, URISyntaxException, InterruptedException {
        List<SplitPdfDescriptor> splitDescriptors = Main.getSplitDescriptors(30300);
        assertThat(splitDescriptors).hasSize(10);
        assertThat(splitDescriptors.get(0).startPage()).isEqualTo(1);
    }

    @Test
    void testSplitFiles() throws URISyntaxException, IOException {
        List<SplitPdfDescriptor> descriptors = List.of(new SplitPdfDescriptor(1, 5, "POI"));
        File file = new File(ClassLoader.getSystemResource("test.pdf").toURI());
        Main main = new Main();
        List<SplitPdf> splitPdfs = main.splitFiles(file, "test.pdf", descriptors);
        assertThat(splitPdfs).hasSize(1);
        assertThat(splitPdfs.get(0).tempFile()).exists();
    }

    @Test
    void canParseArguments(){
        Main main = new Main();
        CommandLine cmd = new CommandLine(main);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int execute = cmd.execute("-h");
        assertThat(execute).isZero();
        assertThat(sw).hasToString(
                """
                        Usage: split-certs [-hV] <sourceFile> <tournamentId>
                        splits up a certificates PDF and uploads it to AWS
                              <sourceFile>     The file to split
                              <tournamentId>   the tournament ID
                          -h, --help           Show this help message and exit.
                          -V, --version        Print version information and exit.
                        """
        );
    }

}