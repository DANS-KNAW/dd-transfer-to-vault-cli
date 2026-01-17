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
import nl.knaw.dans.transfercli.DdTransferToVaultCli;
import nl.knaw.dans.transfercli.client.ApiException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "flush-work-to-vault",
         mixinStandardHelpOptions = true,
         description = "Flush the staged batch to the data vault")
@RequiredArgsConstructor
public class FlushWorkToVault implements Callable<Integer> {
    private final Context context;

    @Override
    public Integer call() {
        try {
            var statusMessage = context.getApi().sendToVaultFlushPost();
            System.err.println("Flush job submitted: " + statusMessage.getMessage());
            return 0;
        }
        catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
