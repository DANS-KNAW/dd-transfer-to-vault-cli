/*
 * Copyright (C) 2026 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.transfercli.command;

import nl.knaw.dans.transfercli.Context;
import nl.knaw.dans.transfercli.config.DdTransferToVaultCliConfig;
import nl.knaw.dans.transfercli.config.DirectoriesConfig;
import nl.knaw.dans.transfercli.config.TransferToVaultConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferStatusTest {
    private final Path testDir = Path.of("target/test/" + getClass().getSimpleName());
    private Context context;
    private DdTransferToVaultCliConfig config;
    private TransferToVaultConfig pipelineConfig;
    private DirectoriesConfig directories;

    @BeforeEach
    void setUp() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
        Files.createDirectories(testDir);

        context = Mockito.mock(Context.class);
        config = Mockito.mock(DdTransferToVaultCliConfig.class);
        pipelineConfig = Mockito.mock(TransferToVaultConfig.class);
        directories = new DirectoriesConfig();

        Mockito.when(context.getConfig()).thenReturn(config);
        Mockito.when(config.getPipelines()).thenReturn(Map.of("test-pipeline", pipelineConfig));
        Mockito.when(config.getDirectories()).thenReturn(directories);

        Path collectInboxes = testDir.resolve("collect-inboxes");
        Path workingSpaceBaseDir = testDir.resolve("working-space");
        Path dataVaultBatchRoot = testDir.resolve("data-vault-batches");
        Path dataVaultRoot = testDir.resolve("data-vault-root");

        Files.createDirectories(collectInboxes);
        Files.createDirectories(workingSpaceBaseDir);
        Files.createDirectories(dataVaultBatchRoot);
        Files.createDirectories(dataVaultRoot);

        directories.setCollectInboxes(collectInboxes);
        directories.setWorkingSpaceBaseDir(workingSpaceBaseDir);
        directories.setDataVaultBatchRoot(dataVaultBatchRoot);
        directories.setDataVaultRoot(dataVaultRoot);
    }

    @Test
    void call_should_throw_IllegalArgumentException_when_pipeline_not_specified() {
        Mockito.when(context.getPipeline()).thenReturn(null);
        TransferStatus command = new TransferStatus(context);

        assertThatThrownBy(command::call)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No pipeline specified");
    }

    @Test
    void call_should_throw_IllegalArgumentException_when_pipeline_config_missing() {
        Mockito.when(context.getPipeline()).thenReturn("non-existent");
        TransferStatus command = new TransferStatus(context);

        assertThatThrownBy(command::call)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No configuration found for pipeline: non-existent");
    }

    @Test
    void call_should_print_status_report_for_valid_pipeline() throws Exception {
        Mockito.when(context.getPipeline()).thenReturn("test-pipeline");

        // Create pipeline-specific directories
        Path pipelineWorkingDir = directories.getWorkingSpaceBaseDir().resolve("test-pipeline");
        Files.createDirectories(pipelineWorkingDir.resolve("extract-metadata/inbox"));
        Files.createFile(pipelineWorkingDir.resolve("extract-metadata/inbox/test.zip"));

        // Data vault batches
        Path pipelineDataVaultBatchRoot = directories.getDataVaultBatchRoot().resolve("test-pipeline");
        Path inbox = pipelineDataVaultBatchRoot.resolve("inbox");
        Path batch1 = inbox.resolve("batch1");
        Files.createDirectories(batch1);
        Files.createDirectories(batch1.resolve("item1"));

        TransferStatus command = new TransferStatus(context);

        PrintStream oldOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            Integer result = command.call();
            assertThat(result).isEqualTo(0);
            String output = outContent.toString();
            assertThat(output).contains("status test-pipeline at");
            assertThat(output).contains("* dd-transfer-to-vault:");
            assertThat(output).contains("transfer inbox");
            assertThat(output).contains("extract metadata inbox        : 1 items");
            assertThat(output).contains("* dd-data-vault:");
            assertThat(output).contains("batch1                   1");
            assertThat(output).contains("0 bytes");
            assertThat(output).contains("---");
        } finally {
            System.setOut(oldOut);
        }
    }
}
