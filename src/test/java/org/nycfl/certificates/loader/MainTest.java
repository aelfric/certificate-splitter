package org.nycfl.certificates.loader;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
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

}