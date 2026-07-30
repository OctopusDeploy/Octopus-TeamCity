package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

class EmbeddedResourceExtractorTest {

  @Test
  void extractTarGzResource_readsFromClasspathResource() throws Exception {
    // locate classpath root
    URL root = this.getClass().getClassLoader().getResource("");
    if (root == null) {
      throw new IllegalStateException("Unable to locate test classpath root");
    }

    Path resourceRoot = Paths.get(root.toURI());
    Path folder = resourceRoot.resolve("test-extract");
    Files.createDirectories(folder);

    Path archivePath = folder.resolve("archive-cls.tar.gz");

    byte[] content = "hello-from-classpath".getBytes(StandardCharsets.UTF_8);
    try (FileOutputStream fos = new FileOutputStream(archivePath.toFile());
        GZIPOutputStream gos = new GZIPOutputStream(fos);
        TarArchiveOutputStream taos = new TarArchiveOutputStream(gos)) {

      TarArchiveEntry entry = new TarArchiveEntry("dir/hello.txt");
      entry.setSize(content.length);
      taos.putArchiveEntry(entry);
      taos.write(content);
      taos.closeArchiveEntry();
      taos.finish();
    }

    EmbeddedResourceExtractor extractor = new EmbeddedResourceExtractor();
    Path destDir = Files.createTempDirectory("extract-target-cls-");

    // resourcePath must start with '/'
    String resourcePath = "/test-extract/archive-cls.tar.gz";
    extractor.extractTarGzResource(resourcePath, destDir);

    Path extracted = destDir.resolve("dir").resolve("hello.txt");
    assertThat(Files.exists(extracted)).isTrue();
    String read = new String(Files.readAllBytes(extracted), StandardCharsets.UTF_8);
    assertThat(read).isEqualTo("hello-from-classpath");
  }

  @Test
  void extractZipResource_readsFromClasspathResource() throws Exception {
    // locate classpath root
    URL root = this.getClass().getClassLoader().getResource("");
    if (root == null) {
      throw new IllegalStateException("Unable to locate test classpath root");
    }

    Path resourceRoot = Paths.get(root.toURI());
    Path folder = resourceRoot.resolve("test-extract");
    Files.createDirectories(folder);

    Path archivePath = folder.resolve("archive-cls.zip");

    byte[] content = "hello-from-classpath".getBytes(StandardCharsets.UTF_8);
    try (FileOutputStream fos = new FileOutputStream(archivePath.toFile());
        ZipOutputStream zos = new ZipOutputStream(fos)) {

      ZipEntry entry = new ZipEntry("dir/hello.txt");
      zos.putNextEntry(entry);
      zos.write(content);
      zos.closeEntry();
    }

    EmbeddedResourceExtractor extractor = new EmbeddedResourceExtractor();
    Path destDir = Files.createTempDirectory("extract-target-cls-zip-");

    // resourcePath must start with '/'
    String resourcePath = "/test-extract/archive-cls.zip";
    extractor.extractZipResource(resourcePath, destDir);

    Path extracted = destDir.resolve("dir").resolve("hello.txt");
    assertThat(Files.exists(extracted)).isTrue();
    String read = new String(Files.readAllBytes(extracted), StandardCharsets.UTF_8);
    assertThat(read).isEqualTo("hello-from-classpath");
  }
}
