package octopus.teamcity.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jetbrains.buildServer.agent.BuildProgressLogger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class OctopusBuildInformationWriter {

    private final BuildProgressLogger buildLogger;
    private final boolean verboseLogging;

    public OctopusBuildInformationWriter(final BuildProgressLogger buildLogger, final boolean verboseLogging) {

        this.buildLogger = buildLogger;
        this.verboseLogging = verboseLogging;
    }

    public void writeToFile(final OctopusBuildInformation buildInformation, final String dataFile) throws IOException {
        try {
            final Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            if (verboseLogging) {
                buildLogger.message("Serializing Octopus build information");
            }

            final String jsonData = gson.toJson(buildInformation);
            if (verboseLogging) {
                buildLogger.message("Serialized Octopus build information - " + jsonData);
            }

            OutputStreamWriter bw = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_16);
            bw.write(jsonData);
            bw.close();

            if (verboseLogging) {
                buildLogger.message("Wrote " + dataFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
            buildLogger.message("Error writing the octopus.buildinfo file");
            throw e;
        }
    }
}
