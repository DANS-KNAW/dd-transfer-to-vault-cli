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

package nl.knaw.dans.transfercli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.AbstractCommandLineApp;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.lib.util.PicocliVersionProvider;
import nl.knaw.dans.transfercli.client.ApiClient;
import nl.knaw.dans.transfercli.client.DefaultApi;
import nl.knaw.dans.transfercli.command.FlushWorkToVault;
import nl.knaw.dans.transfercli.config.DdTransferToVaultCliConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.stream.Collectors;

@Command(name = "transfer",
         mixinStandardHelpOptions = true,
         versionProvider = PicocliVersionProvider.class,
         description = "CLI for dd-transfer-to-vault")
@Slf4j
public class DdTransferToVaultCli extends AbstractCommandLineApp<DdTransferToVaultCliConfig> implements Context {
    public static void main(String[] args) throws Exception {
        new DdTransferToVaultCli().run(args);
    }

    @Getter
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, DefaultApi> pipelines;

    @Option(names = { "-p", "--pipeline" },
            description = "The pipeline (dd-transfer-to-vault) instance to execute the command on.")
    @Setter
    private String pipeline;

    @Override
    public DefaultApi getApi() {
        if (pipelines == null) {
            throw new IllegalStateException("getApi() called before initialization.");
        }

        if (this.pipeline == null) {
            System.err.println("No instance specified. Use -i or --instance option.");
            throw new IllegalArgumentException("No instance specified.");
        }

        var api = pipelines.get(this.pipeline);
        if (api == null) {
            System.err.println("No instance found for " + this.pipeline);
            throw new IllegalArgumentException("No instance found for " + this.pipeline);
        }
        return api;
    }

    public String getName() {
        return "CLI for dd-transfer-to-vault";
    }

    @Override
    public void configureCommandLine(CommandLine commandLine, DdTransferToVaultCliConfig config) {
        log.debug("Configuring command line");
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.pipelines = config.getPipelines().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new ClientProxyBuilder<ApiClient, DefaultApi>()
                .apiClient(new ApiClient())
                .basePath(e.getValue().getUrl())
                .httpClient(e.getValue().getHttpClient())
                .defaultApiCtor(DefaultApi::new)
                .build()));

        commandLine.addSubcommand(new FlushWorkToVault(this));
    }
}
