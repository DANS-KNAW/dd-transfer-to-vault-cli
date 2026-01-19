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

import lombok.RequiredArgsConstructor;
import nl.knaw.dans.transfercli.Context;
import nl.knaw.dans.transfercli.config.TransferToVaultConfig;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "status",
         mixinStandardHelpOptions = true,
         description = "Prints a status report for a transfer pipeline.")
@RequiredArgsConstructor
public class TransferStatus implements Callable<Integer> {
    private final Context context;

    @Option(names = { "-a", "--all-batches" },
            description = "Show all batches, including completed ones.")
    private boolean allBatches = false;

    @Override
    public Integer call() {
        String pipelineName = context.getPipeline();
        if (pipelineName == null) {
            throw new IllegalArgumentException("No pipeline specified. Use -p or --pipeline option.");
        }

        TransferToVaultConfig config = context.getConfig().getPipelines().get(pipelineName);
        if (config == null) {
            throw new IllegalArgumentException("No configuration found for pipeline: " + pipelineName);
        }

        var directories = context.getConfig().getDirectories();
        Path collectInbox = (config.isVaas() && directories.getVaasCollectInboxes() != null ?
            directories.getVaasCollectInboxes() : directories.getCollectInboxes()).resolve(pipelineName);
        Path workingSpaceBaseDir = directories.getWorkingSpaceBaseDir().resolve(pipelineName);
        Path dataVaultBatchRoot = directories.getDataVaultBatchRoot().resolve(pipelineName);
        Path dataVaultRoot = directories.getDataVaultRoot().resolve(pipelineName);

        System.out.printf("status %s at %s%n", pipelineName, ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss zzz yyyy")));
        System.out.println();
        System.out.println("* dd-transfer-to-vault:");
        statusLine(collectInbox, "transfer inbox", "*.zip");
        statusLine(workingSpaceBaseDir.resolve("extract-metadata/inbox"), "extract metadata inbox");
        statusLine(workingSpaceBaseDir.resolve("extract-metadata/outbox/failed"), "extract metadata failed", "*.zip");
        statusLine(workingSpaceBaseDir.resolve("extract-metadata/outbox/rejected"), "extract metadata rejected", "*.zip");
        statusLine(workingSpaceBaseDir.resolve("send-to-vault/inbox"), "send to vault inbox", "*.zip");
        statusLine(workingSpaceBaseDir.resolve("send-to-vault/outbox/processed"), "send to vault processed", "*.zip");
        statusLine(workingSpaceBaseDir.resolve("send-to-vault/outbox/failed"), "send to vault failed", "*.zip");
        statusLine(workingSpaceBaseDir.resolve("send-to-vault/work"), "send to vault work");
        System.out.println();

        System.out.println("* dd-data-vault:");
        statusLine(dataVaultRoot.resolve("staging"), "staged layers");
        statusLine(dataVaultRoot.resolve("archive"), "archived layers");

        Path ddDataVaultInbox = dataVaultBatchRoot.resolve("inbox");
        statusLine(ddDataVaultInbox, "data vault inbox batches");

        printDataVaultBatches(dataVaultBatchRoot);

        System.out.println("---");
        System.out.println();

        return 0;
    }

    private void statusLine(Path dir, String label) {
        statusLine(dir, label, null);
    }

    private void statusLine(Path dir, String label, String filter) {
        long count = 0;
        String size = "0 bytes";
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                Stream<Path> filteredStream = stream;
                if (filter != null && filter.equals("*.zip")) {
                    filteredStream = filteredStream.filter(p -> p.getFileName().toString().endsWith(".zip"));
                }
                count = filteredStream.count();
                size = FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(dir.toFile()));
            }
            catch (IOException | IllegalArgumentException e) {
                // Ignore or handle
            }
        }
        System.out.printf("%-30s: %d items (%s)%n", label, count, size);
    }

    private void printDataVaultBatches(Path dataVaultBatchRoot) {
        Path inboxBase = dataVaultBatchRoot.resolve("inbox");
        Path outboxBase = dataVaultBatchRoot.resolve("outbox");

        System.out.printf("%-20s %6s %-12s %10s %-12s %7s %-11s%n", "BATCH", "INBOX", "(SIZE)", "PROCESSED", "(SIZE)", "FAILED", "(SIZE)");

        if (Files.exists(inboxBase)) {
            try (Stream<Path> stream = Files.list(inboxBase)) {
                stream.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(batch -> {
                        String batchName = batch.getFileName().toString();
                        long inboxCount = 0;
                        String inboxSize = "0 bytes";
                        try (Stream<Path> s = Files.list(batch)) {
                            inboxCount = s.filter(Files::isDirectory).count();
                            inboxSize = FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(batch.toFile()));
                        }
                        catch (IOException | IllegalArgumentException ignored) {
                        }

                        Path processedDir = outboxBase.resolve(batchName).resolve("processed");
                        Path failedDir = outboxBase.resolve(batchName).resolve("failed");

                        long processedCount = 0;
                        String processedSize = "0 bytes";
                        if (Files.exists(processedDir)) {
                            try (Stream<Path> s = Files.list(processedDir)) {
                                processedCount = s.filter(Files::isDirectory).count();
                                processedSize = FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(processedDir.toFile()));
                            }
                            catch (IOException | IllegalArgumentException ignored) {
                            }
                        }

                        long failedCount = 0;
                        String failedSize = "0 bytes";
                        if (Files.exists(failedDir)) {
                            try (Stream<Path> s = Files.list(failedDir)) {
                                failedCount = s.filter(Files::isDirectory).count();
                                failedSize = FileUtils.byteCountToDisplaySize(FileUtils.sizeOfDirectory(failedDir.toFile()));
                            }
                            catch (IOException | IllegalArgumentException ignored) {
                            }
                        }

                        if (allBatches || inboxCount > 0 || failedCount > 0) {
                            System.out.printf("%-20s %6d (%-11s %10d (%-11s %7d (%s%n",
                                batchName,
                                inboxCount, inboxSize + ")",
                                processedCount, processedSize + ")",
                                failedCount, failedSize + ")");
                        }
                    });
            }
            catch (IOException ignored) {
            }
        }
    }
}
