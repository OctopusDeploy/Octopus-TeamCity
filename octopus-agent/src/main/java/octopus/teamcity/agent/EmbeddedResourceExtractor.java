/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class EmbeddedResourceExtractor {
  public void extractTo(String destinationPath) throws Exception {
    ensureDirectory(destinationPath, "1.0");
    extractFile("/resources/1/0/octo.exe", destinationPath + "/1.0/Octo.exe");
    extractFile("/resources/1/0/Octo.exe.config", destinationPath + "/1.0/Octo.exe.config");

    ensureDirectory(destinationPath, "2.0");
    extractFile("/resources/2/0/Octo.exe", destinationPath + "/2.0/Octo.exe");
    extractFile("/resources/2/0/Octo.exe.config", destinationPath + "/2.0/Octo.exe.config");

    ensureDirectory(destinationPath, "3.0");
    extractFile("/resources/3/0/octo.exe", destinationPath + "/3.0/octo.exe");
    extractFile("/resources/3/0/OctopusTools.portable.zip", destinationPath + "/3.0/Core.zip");
    unzip(destinationPath + "/3.0/Core.zip", destinationPath + "/3.0/Core");
  }

  public void extractCliTo(String destinationPath, String octopusVersion, String osFolder)
      throws Exception {
    String binaryArchive =
        getArchiveName(String.format("resources/newcli/%s/%s", octopusVersion, osFolder));
    extractTarGzResource(
        String.format("/resources/newcli/%s/%s/%s", octopusVersion, osFolder, binaryArchive),
        Paths.get(destinationPath));
  }

  private void extractFile(String resourceName, String destinationName) throws Exception {
    int attempts = 0;
    while (true) {
      attempts++;

      try {
        File file = new File(destinationName);
        if (file.exists()) {
          return;
        }

        InputStream is = getClass().getResourceAsStream(resourceName);
        OutputStream os = new FileOutputStream(destinationName, false);

        byte[] buffer = new byte[4096];
        int length;
        while ((length = is.read(buffer)) > 0) {
          os.write(buffer, 0, length);
        }

        os.close();
        is.close();
      } catch (Exception ex) {
        Thread.sleep(4000);
        if (attempts > 3) {
          throw ex;
        }
      }
    }
  }

  private void ensureDirectory(String destinationPath, String version) {
    File extractedTo = new File(destinationPath, version);
    if (extractedTo.exists()) {
      return;
    }

    if (!extractedTo.mkdirs())
      throw new RuntimeException("Unable to create temp output directory " + extractedTo);
  }

  private void unzip(String source, String destination) throws IOException {
    File destDir = new File(destination);
    if (!destDir.exists()) {
      destDir.mkdirs();
    }

    byte[] buffer = new byte[4096];
    FileInputStream fis = new FileInputStream(source);
    ZipInputStream zis = new ZipInputStream(fis);
    try {
      ZipEntry ze = zis.getNextEntry();
      while (ze != null) {
        File newFile = new File(destination + File.separator + ze.getName());
        if (ze.isDirectory()) {
          newFile.mkdirs();
        } else {
          new File(newFile.getParent()).mkdirs();
          FileOutputStream fos = new FileOutputStream(newFile);
          int len;
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
        }
        ze = zis.getNextEntry();
      }
    } finally {
      zis.closeEntry();
      zis.close();
      fis.close();
    }
  }

  public void extractTarGzResource(String resourcePath, Path destDir) throws IOException {
    Files.createDirectories(destDir);

    try (InputStream fi = getClass().getResourceAsStream(resourcePath)) {
      if (fi == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }

      try (BufferedInputStream bi = new BufferedInputStream(fi);
          GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
          TarArchiveInputStream tarIn = new TarArchiveInputStream(gzi)) {

        TarArchiveEntry entry;
        byte[] buffer = new byte[4096];

        while ((entry = tarIn.getNextTarEntry()) != null) {
          Path entryPath = destDir.resolve(entry.getName());

          if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Files.createDirectories(entryPath.getParent());

            try (OutputStream out = Files.newOutputStream(entryPath)) {
              int bytesRead;
              while ((bytesRead = tarIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
              }
            }
          }
        }
      }
    }
  }

  private String getArchiveName(String resourceFolder) {
    URL url = getClass().getClassLoader().getResource(resourceFolder);
    if (url == null) {
      throw new RuntimeException("Unable to find embedded resource folder: " + resourceFolder);
    }

    try {
      if ("file".equals(url.getProtocol())) {
        // Running from filesystem (IDE/development)
        Path folderPath = Paths.get(url.toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
          for (Path entry : stream) {
            String name = entry.getFileName().toString();
            if (name.endsWith(".tar.gz") || name.endsWith(".zip") || name.endsWith(".tgz")) {
              return name;
            }
          }
        }
      } else if ("jar".equals(url.getProtocol())) {
        // Running from JAR
        JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = jarConnection.getJarFile()) {
          String folderPrefix =
              resourceFolder.endsWith("/") ? resourceFolder : resourceFolder + "/";

          return jarFile.stream()
              .map(JarEntry::getName)
              .filter(name -> name.startsWith(folderPrefix))
              .filter(name -> !name.equals(folderPrefix)) // Skip the folder itself
              .filter(
                  name ->
                      name.endsWith(".tar.gz") || name.endsWith(".zip") || name.endsWith(".tgz"))
              .map(
                  name -> {
                    // Extract just the filename from the full path
                    String relativePath = name.substring(folderPrefix.length());
                    int slashIndex = relativePath.indexOf('/');
                    return slashIndex >= 0 ? relativePath.substring(0, slashIndex) : relativePath;
                  })
              .findFirst()
              .orElseThrow(() -> new RuntimeException("No archive found in: " + resourceFolder));
        }
      }
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException("Error reading resource folder: " + resourceFolder, e);
    }

    throw new RuntimeException("No archive found in folder: " + resourceFolder);
  }
}
