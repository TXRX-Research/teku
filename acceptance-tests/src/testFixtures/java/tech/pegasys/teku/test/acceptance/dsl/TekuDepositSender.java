/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.test.acceptance.dsl;

import static tech.pegasys.teku.util.config.Constants.MAX_EFFECTIVE_BALANCE;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.Network;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.async.Waiter;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.test.acceptance.dsl.tools.deposits.DepositGenerator;
import tech.pegasys.teku.test.acceptance.dsl.tools.deposits.DepositSenderService;
import tech.pegasys.teku.test.acceptance.dsl.tools.deposits.ValidatorKeyGenerator;
import tech.pegasys.teku.util.config.Eth1Address;

public class TekuDepositSender extends Node {
  private static final Logger LOG = LogManager.getLogger();

  public TekuDepositSender(final Network network) {
    super(network, TekuNode.TEKU_DOCKER_IMAGE, LOG);
  }

  public void sendValidatorDeposits(final BesuNode eth1Node, final int numberOfValidators)
      throws InterruptedException, ExecutionException, TimeoutException {
    sendValidatorDeposits(eth1Node, numberOfValidators, MAX_EFFECTIVE_BALANCE);
  }

  public void sendValidatorDeposits(
      final BesuNode eth1Node, final int numberOfValidators, long amount)
      throws InterruptedException, ExecutionException, TimeoutException {
    final Eth1Address eth1Address = Eth1Address.fromHexString(eth1Node.getDepositContractAddress());
    final Credentials eth1Credentials = Credentials.create(eth1Node.getRichBenefactorKey());
    final UInt64 depositAmount = UInt64.valueOf(amount);
    try (final DepositGenerator depositGenerator =
        new DepositGenerator(
            eth1Node.getExternalJsonRpcUrl(),
            eth1Address,
            eth1Credentials,
            numberOfValidators,
            depositAmount)) {
      final SafeFuture<Void> future = depositGenerator.generate();
      Waiter.waitFor(future, Duration.ofMinutes(2));
    }
  }

  public void sendValidatorDeposits(
      final BesuNode eth1Node,
      final List<ValidatorKeyGenerator.ValidatorKeys> validatorKeys,
      long amount)
      throws InterruptedException, ExecutionException, TimeoutException {
    final Eth1Address eth1Address = Eth1Address.fromHexString(eth1Node.getDepositContractAddress());
    final Credentials eth1Credentials = Credentials.create(eth1Node.getRichBenefactorKey());
    final UInt64 depositAmount = UInt64.valueOf(amount);
    final DepositSenderService depositSenderService =
        new DepositSenderService(
            eth1Node.getExternalJsonRpcUrl(), eth1Credentials, eth1Address, depositAmount);
    final List<SafeFuture<TransactionReceipt>> transactionReceipts =
        validatorKeys.stream().map(depositSenderService::sendDeposit).collect(Collectors.toList());
    final SafeFuture<Void> future =
        SafeFuture.allOf(transactionReceipts.toArray(SafeFuture[]::new));
    Waiter.waitFor(future, Duration.ofMinutes(2));
  }

  public List<ValidatorKeyGenerator.ValidatorKeys> generateValidatorKeys(int numberOfValidators) {
    final ValidatorKeyGenerator generator = new ValidatorKeyGenerator(numberOfValidators);
    return generator.generateKeysStream().collect(Collectors.toList());
  }
}
