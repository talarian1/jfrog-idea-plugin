package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Tal Arian
 */
public class IACScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("iac");
    private static final boolean RUN_WITH_CONFIG_FILE = true;
    
    public IACScannerExecutor(Log log, ServerConfig serverConfig) {
        this(log, serverConfig, null, true);
    }

    public IACScannerExecutor(Log log, ServerConfig serverConfig, String binaryDownloadUrl, boolean useJFrogReleases) {
        super(SourceCodeScanType.IAC, binaryDownloadUrl, log, serverConfig, useJFrogReleases);
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled, RUN_WITH_CONFIG_FILE);
    }

    @Override
    List<FileTreeNode> createSpecificFileIssueNodes(List<JFrogSecurityWarning> warnings) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : warnings) {
            // Create FileTreeNodes for files with found issues
            FileTreeNode fileNode = results.get(warning.getFilePath());
            if (fileNode == null) {
                fileNode = new FileTreeNode(warning.getFilePath());
                results.put(warning.getFilePath(), fileNode);
            }

            FileIssueNode issueNode = new FileIssueNode("Infrastructure as Code Vulnerability",
                    warning.getFilePath(), warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                    warning.getScannerSearchTarget(), warning.getLineSnippet(), warning.getReporter(), warning.getSeverity());
            fileNode.addIssue(issueNode);
        }
        return new ArrayList<>(results.values());
    }

    @Override
    public Feature getScannerFeatureName() {
        return Feature.INFRASTRUCTURE_AS_CODE;
    }

    @Override
    protected boolean isPackageTypeSupported(PackageManagerType packageType) {
        return true;
    }

}
