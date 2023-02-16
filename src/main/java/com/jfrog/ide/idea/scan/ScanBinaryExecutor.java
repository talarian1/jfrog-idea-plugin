package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createAnonymousAccessArtifactoryManagerBuilder;
import static com.jfrog.ide.common.utils.Utils.createMapper;
import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static com.jfrog.ide.common.utils.XrayConnectionUtils.createXrayClientBuilder;
import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;
import static com.jfrog.ide.idea.utils.Utils.getFileChecksumFromServer;

/**
 * @author Tal Arian
 */
public abstract class ScanBinaryExecutor {
    final String scanType;
    protected List<String> supportedLanguages;
    private static final Path BINARIES_DIR = HOME_PATH.resolve("dependencies").resolve("jfrog-security");
    private static Path BINARY_TARGET_PATH;
    private static final String MINIMAL_XRAY_VERSION_SUPPORTED_FOR_ENTITLEMENT = "3.66.0";
    private static final int UPDATE_INTERVAL = 1;
    private static LocalDateTime nextUpdateCheck;
    private Log log;
    private boolean notSupportedOS;
    private static final String ENV_PLATFORM = "JF_PLATFORM_URL";
    private static final String ENV_USER = "JF_USER";
    private static final String ENV_PASSWORD = "JF_PASS";
    private static final String ENV_ACCESS_TOKEN = "JF_TOKEN";
    private static final String ENV_HTTP_PROXY = "HTTP_PROXY";
    private static final String ENV_HTTPS_PROXY = "HTTPS_PROXY";
    private static final int USER_NOT_ENTITLED = 31;
    private static final String JFROG_RELEASES = "https://releases.jfrog.io/artifactory/";
    private static String osDistribution;
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;


    ScanBinaryExecutor(String scanType, String binaryName) {
        this.scanType = scanType;
        BINARY_TARGET_PATH = BINARIES_DIR.resolve(binaryName);
        commandExecutor = new CommandExecutor(BINARY_TARGET_PATH.toString(), creatEnvWithCredentials());
        log = Logger.getInstance();
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        artifactoryManagerBuilder = createAnonymousAccessArtifactoryManagerBuilder(JFROG_RELEASES, server.getProxyConfForTargetUrl(JFROG_RELEASES), log);
        try {
            osDistribution = getOSAndArc();
        } catch (IOException e) {
            log.info(e.getMessage());
            notSupportedOS = true;
        }
    }

    public static String getOsDistribution() {
        return osDistribution;
    }

    abstract List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException, URISyntaxException;

    abstract String getBinaryDownloadURL();

    abstract Feature getScannerFeatureName();

    protected List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, List<String> args) throws IOException, InterruptedException, URISyntaxException {
        if (notSupportedOS || !shouldExecute()) {
            return List.of();
        }
        CommandExecutor commandExecutor = new CommandExecutor(BINARY_TARGET_PATH.toString(), creatEnvWithCredentials());
        updateBinaryIfNeeded();
        Path outputTempDir = null;
        Path inputFile = null;
        try {
            outputTempDir = Files.createTempDirectory("");
            Path outputFilePath = Files.createTempFile(outputTempDir, "", ".sarif");
            inputFileBuilder.output(outputFilePath.toString());
            inputFileBuilder.scanType(scanType);
            inputFile = createTempRunInputFile(new ScansConfig(List.of(inputFileBuilder.Build())));
            args = new ArrayList<>(args);
            args.add(inputFile.toString());

            Logger log = Logger.getInstance();
            // Execute the external process
            CommandResults commandResults = commandExecutor.exeCommand(outputTempDir.toFile(), args, null, log);
            if (commandResults.getExitValue() == USER_NOT_ENTITLED) {
                log.debug("User not entitled for advance security scan");
                return List.of();
            }
            if (!commandResults.isOk()) {
                throw new IOException(commandResults.getErr());
            }
            return parseOutputSarif(outputFilePath);
        } finally {
            if (outputTempDir != null) {
                FileUtils.deleteQuietly(outputTempDir.toFile());
            }
            if (inputFile != null) {
                FileUtils.deleteQuietly(inputFile.toFile());
            }
        }
    }

    private void updateBinaryIfNeeded() throws IOException, URISyntaxException, InterruptedException {
        if (!Files.exists(BINARY_TARGET_PATH)) {
            downloadBinary();
            return;
        }
        if (nextUpdateCheck == null || LocalDateTime.now().isAfter(nextUpdateCheck)) {
            var currentTime = LocalDateTime.now();
            nextUpdateCheck = LocalDateTime.of(currentTime.getYear(), currentTime.getMonth(), currentTime.getDayOfMonth() + UPDATE_INTERVAL, currentTime.getHour(), currentTime.getMinute(), currentTime.getSecond());
            // Check for new version of the binary
            try (FileInputStream binaryFile = new FileInputStream(BINARY_TARGET_PATH.toFile())) {
                String latestBinaryChecksum = getFileChecksumFromServer(JFROG_RELEASES + getBinaryDownloadURL());
                String currentBinaryCheckSum = DigestUtils.sha256Hex(binaryFile).toString();
                if (!latestBinaryChecksum.equals(currentBinaryCheckSum)) {
                    downloadBinary();
                }
            }
        }
    }


    protected boolean shouldExecute() {
        ServerConfig server = GlobalSettings.getInstance().getServerConfig();
        try (Xray xrayClient = createXrayClientBuilder(server, log).build()) {
            try {
                if (xrayClient.system().version().isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED_FOR_ENTITLEMENT)) {
                    return false;
                }
            } catch (IOException e) {
                log.error("Couldn't connect to JFrog Xray. Please check your credentials.", e);
                return false;
            }
            return xrayClient.entitlements().isEntitled(getScannerFeatureName());
        }
    }

    protected List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    protected List<JFrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException {
        Output output = getOutputObj(outputFile);
        List<JFrogSecurityWarning> warnings = new ArrayList<>();
        output.getRuns().forEach(run -> run.getResults().forEach(result -> warnings.add(new JFrogSecurityWarning(result))));
        return warnings;
    }

    protected Output getOutputObj(Path outputFile) throws IOException {
        ObjectMapper om = createMapper();
        return om.readValue(outputFile.toFile(), Output.class);
    }

    protected void downloadBinary() throws IOException {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            String downloadUrl = getBinaryDownloadURL();
            File downloadBinary = artifactoryManager.downloadToFile(downloadUrl, BINARY_TARGET_PATH.toString());
            if (downloadBinary == null) {
                throw new IOException("An empty response received from Artifactory.");
            }
            downloadBinary.setExecutable(true);
        }
    }

    Path createTempRunInputFile(ScansConfig scanInput) throws IOException {
        ObjectMapper om = createYAMLMapper();
        Path tempDir = Files.createTempDirectory("");
        Path inputPath = Files.createTempFile(tempDir, "", ".yaml");
        om.writeValue(inputPath.toFile(), scanInput);
        return inputPath;
    }

    private Map<String, String> creatEnvWithCredentials() {
        Map<String, String> env = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
        ServerConfigImpl serverConfig = GlobalSettings.getInstance().getServerConfig();
        if (serverConfig.isXrayConfigured()) {
            env.put(ENV_PLATFORM, serverConfig.getUrl());
            if (StringUtils.isNotEmpty(serverConfig.getAccessToken())) {
                env.put(ENV_ACCESS_TOKEN, serverConfig.getAccessToken());
            } else {
                env.put(ENV_USER, serverConfig.getUsername());
                env.put(ENV_PASSWORD, serverConfig.getPassword());
            }

            ProxyConfiguration proxyConfiguration = serverConfig.getProxyConfForTargetUrl(serverConfig.getUrl());
            if (proxyConfiguration != null) {
                String proxyUrl = proxyConfiguration.host + ":" + proxyConfiguration.port;
                if (StringUtils.isNoneBlank(proxyConfiguration.username, proxyConfiguration.password)) {
                    proxyUrl = proxyConfiguration.username + ":" + proxyConfiguration.password + "@" + proxyUrl;
                }
                env.put(ENV_HTTP_PROXY, "http://" + proxyUrl);
                env.put(ENV_HTTPS_PROXY, "https://" + proxyUrl);
            }
        }
        return env;
    }

    private static String getOSAndArc() throws IOException {
        String arch = SystemUtils.OS_ARCH;
        // Windows
        if (SystemUtils.IS_OS_WINDOWS) {
            return "windows-amd64";
        }
        // Mac
        if (SystemUtils.IS_OS_MAC) {
            if (arch == "arm64") {
                return "mac-arm64";
            } else {
                return "mac-amd64";
            }
        }
        // Linux
        if (SystemUtils.IS_OS_LINUX) {
            switch (arch) {
                case "i386":
                case "i486":
                case "i586":
                case "i686":
                case "i786":
                case "x86":
                    return "linux-386";
                case "amd64":
                case "x86_64":
                case "x64":
                    return "linux-amd64";
                case "arm":
                case "armv7l":
                    return "linux-arm";
                case "aarch64":
                    return "linux-arm64";
                case "s390x":
                case "ppc64":
                case "ppc64le":
                    return "linux-" + arch;
            }
        }
        throw new IOException(String.format("Unsupported OS: %s-%s", SystemUtils.OS_NAME, arch));
    }
}
