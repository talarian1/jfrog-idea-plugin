package com.jfrog.ide.idea.integration;

import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.ApplicabilityScannerExecutor;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ApplicabilityScannerIntegrationTests extends BaseIntegrationTest {
    private ApplicabilityScannerExecutor scanner;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new ApplicabilityScannerExecutor(Logger.getInstance(), serverConfig);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testApplicabilityScannerNpmProjectNotApplicable() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("npm");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot))
                .cves(List.of("CVE-2021-3918", "CVE-2021-3807"));
        var results = scanner.execute(input);
        assertEquals(2, results.size());
        // Expect all issues to be not applicable to this test project
        assertFalse(results.stream().anyMatch(JFrogSecurityWarning::isApplicable));
    }

    public void testApplicabilityScannerNpmProject() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("npm");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot))
                .cves(List.of("CVE-2022-25878"));
        List<JFrogSecurityWarning> results = scanner.execute(input);
        assertEquals(2, results.size());
        // Expect all issues to be applicable.
        assertTrue(results.stream().allMatch(JFrogSecurityWarning::isApplicable));
        // Expect specific indications
        assertEquals("protobuf.parse(p)", results.get(0).getLineSnippet());
        assertEquals(20, results.get(0).getLineStart());
        assertEquals(20, results.get(0).getLineEnd());
        assertEquals(0, results.get(0).getColStart());
        assertEquals(17, results.get(0).getColEnd());
        assertTrue(results.get(0).getFilePath().endsWith("index.js"));
    }

    public void testApplicabilityScannerPythonProjectNotApplicable() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("python");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot))
                .cves(List.of("CVE-2021-3918", "CVE-2019-15605"));
        var results = scanner.execute(input);
        assertEquals(2, results.size());
        // Expect all issues to be not applicable to this test project
        assertFalse(results.stream().anyMatch(JFrogSecurityWarning::isApplicable));
    }

    public void testApplicabilityScannerPythonProject() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("python");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot))
                .cves(List.of("CVE-2019-20907"));
        List<JFrogSecurityWarning> results = scanner.execute(input);
        assertEquals(1, results.size());
        // Expect specific indications
        assertTrue(results.get(0).isApplicable());
        assertEquals("tarfile.open(name)", results.get(0).getLineSnippet());
        assertEquals(16, results.get(0).getLineStart());
        assertEquals(16, results.get(0).getLineEnd());
        assertEquals(6, results.get(0).getColStart());
        assertEquals(24, results.get(0).getColEnd());
        assertTrue(results.get(0).getFilePath().endsWith("main.py"));
    }

    private String createTempProjectDir(String projectName) throws IOException {
        String tempProjectDir = getTempDir().createVirtualDir(projectName).toNioPath().toString();
        FileUtils.copyDirectory(getTestProjectPath().resolve(projectName).toFile(), new File(tempProjectDir));
        return tempProjectDir;
    }
}
