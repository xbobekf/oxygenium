// Copyright 2018 The Oxygenium Authors
// This file is part of the oxygenium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.oxygenium.flow.validation

import scala.collection.mutable

import akka.util.ByteString

import org.oxygenium.flow.core.{BlockFlow, BlockFlowGroupView, FlowUtils}
import org.oxygenium.io.IOResult
import org.oxygenium.protocol.{ALPH, Hash, PublicKey, SignatureSchema}
import org.oxygenium.protocol.config.{GroupConfig, NetworkConfig}
import org.oxygenium.protocol.model._
import org.oxygenium.protocol.vm.{InvalidSignature => _, OutOfGas => VMOutOfGas, _}
import org.oxygenium.protocol.vm.StatefulVM.TxScriptExecution
import org.oxygenium.util.{AVector, EitherF, TimeStamp, U256}

// scalastyle:off number.of.methods file.size.limit
trait TxValidation {
  import ValidationStatus._

  implicit def groupConfig: GroupConfig
  implicit def networkConfig: NetworkConfig
  implicit def logConfig: LogConfig

  private def validateTxTemplate(
      tx: TransactionTemplate,
      chainIndex: ChainIndex,
      groupView: BlockFlowGroupView[WorldState.Cached],
      blockEnv: BlockEnv
  ): TxValidationResult[Unit] = {
    tx.unsigned.scriptOpt match {
      case Some(script) =>
        validateScriptTxTemplate(tx, script, chainIndex, groupView, blockEnv)
      case None =>
        validateNonScriptTxTemplate(tx, chainIndex, groupView, blockEnv)
    }
  }

  private def validateScriptTxTemplate(
      tx: TransactionTemplate,
      script: StatefulScript,
      chainIndex: ChainIndex,
      groupView: BlockFlowGroupView[WorldState.Cached],
      blockEnv: BlockEnv
  ): TxValidationResult[Unit] = {
    for {
      preOutputs <- fromGetPreOutputs(groupView.getPreAssetOutputs(tx.unsigned.inputs))
      // the tx might fail afterwards
      failedTx <- FlowUtils
        .convertFailedScriptTx(preOutputs, tx, script)
        .toRight(Right(InvalidRemainingBalancesForFailedScriptTx))
      _ <- checkStateless(
        chainIndex,
        failedTx,
        checkDoubleSpending = true,
        blockEnv.getHardFork(),
        isCoinbase = false
      )
      _ <- checkStatefulExceptTxScript(failedTx, blockEnv, preOutputs.as[TxOutput], None)
      // the tx should succeed
      _ <- validateSuccessfulScriptTxTemplate(
        tx,
        script,
        chainIndex,
        groupView,
        blockEnv,
        preOutputs
      )
    } yield ()
  }

  def validateSuccessfulScriptTxTemplate(
      tx: TransactionTemplate,
      script: StatefulScript,
      chainIndex: ChainIndex,
      groupView: BlockFlowGroupView[WorldState.Cached],
      blockEnv: BlockEnv,
      preOutputs: AVector[AssetOutput]
  ): TxValidationResult[Transaction] = {
    val stagingWorldState = groupView.worldState.staging()
    val scriptBaseGas     = GasCall.scriptBaseGas(script.bytes.length)
    for {
      gasRemaining0 <- fromOption(tx.unsigned.gasAmount.sub(scriptBaseGas), OutOfGas)
      exeResult <- fromExeResult(
        StatefulVM.runTxScript(
          stagingWorldState,
          blockEnv,
          tx,
          preOutputs,
          script,
          gasRemaining0
        ),
        TxScriptExeFailed.apply
      )
      exeGas       = gasRemaining0.subUnsafe(exeResult.gasBox)
      successfulTx = FlowUtils.convertSuccessfulTx(tx, exeResult)
      _ <- checkStateless(
        chainIndex,
        successfulTx,
        checkDoubleSpending = false,
        blockEnv.getHardFork(),
        isCoinbase = false
      ) // checked already
      gasRemaining1 <- checkStatefulExceptTxScript(
        successfulTx,
        blockEnv,
        preOutputs.as[TxOutput] ++ exeResult.contractPrevOutputs,
        None
      )
      gasRemaining2 <- fromOption(gasRemaining1.sub(scriptBaseGas), OutOfGas)
      _             <- fromOption(gasRemaining2.sub(exeGas), TxScriptExeFailed(VMOutOfGas))
    } yield {
      stagingWorldState.commit()
      successfulTx
    }
  }

  private def validateNonScriptTxTemplate(
      tx: TransactionTemplate,
      chainIndex: ChainIndex,
      groupView: BlockFlowGroupView[WorldState.Cached],
      blockEnv: BlockEnv
  ): TxValidationResult[Unit] = {
    assume(tx.unsigned.scriptOpt.isEmpty)
    val fullTx = FlowUtils.convertNonScriptTx(tx)
    for {
      _ <- checkStateless(
        chainIndex,
        fullTx,
        checkDoubleSpending = true,
        blockEnv.getHardFork(),
        isCoinbase = false
      )
      preOutputs <- fromGetPreOutputs(groupView.getPreAssetOutputs(tx.unsigned.inputs))
      _ <- checkStateful(
        chainIndex,
        fullTx,
        groupView.worldState,
        preOutputs.as[TxOutput],
        None,
        blockEnv
      )
    } yield ()
  }

  def validateMempoolTxTemplate(
      tx: TransactionTemplate,
      flow: BlockFlow
  ): TxValidationResult[Unit] = {
    for {
      chainIndex <- getChainIndex(tx)
      blockEnv   <- from(flow.getDryrunBlockEnv(chainIndex))
      groupView  <- from(flow.getMutableGroupViewIncludePool(chainIndex.from))
      _          <- validateTxTemplate(tx, chainIndex, groupView, blockEnv)
    } yield ()
  }

  def validateTxOnlyForTest(
      tx: Transaction,
      flow: BlockFlow,
      hardForkOpt: Option[HardFork]
  ): TxValidationResult[Unit] = {
    for {
      chainIndex <- getChainIndex(tx)
      bestDeps = flow.getBestDeps(chainIndex.from)
      groupView <- from(flow.getMutableGroupView(chainIndex.from, bestDeps))
      blockEnv  <- from(flow.getDryrunBlockEnv(chainIndex))
      _ <- validateTx(
        tx,
        chainIndex,
        groupView,
        hardForkOpt.map(hardFork => blockEnv.copy(hardFork = hardFork)).getOrElse(blockEnv),
        None,
        checkDoubleSpending = true
      )
    } yield ()
  }

  private[validation] def validateTx(
      tx: Transaction,
      chainIndex: ChainIndex,
      groupView: BlockFlowGroupView[WorldState.Cached],
      blockEnv: BlockEnv,
      coinbaseNetReward: Option[U256],
      checkDoubleSpending: Boolean // for block txs, this has been checked in block validation
  ): TxValidationResult[Unit] = {
    for {
      _ <- checkStateless(
        chainIndex,
        tx,
        checkDoubleSpending,
        blockEnv.getHardFork(),
        isCoinbase = coinbaseNetReward.nonEmpty
      )
      preOutputs <- fromGetPreOutputs(groupView.getPreOutputs(tx, blockEnv.newOutputRefCache))
      _ <- checkStateful(
        chainIndex,
        tx,
        groupView.worldState,
        preOutputs,
        coinbaseNetReward,
        blockEnv
      )
    } yield ()
  }

  protected def fromGetPreOutputs[Output <: TxOutput](
      getPreOutputs: IOResult[Option[AVector[Output]]]
  ): TxValidationResult[AVector[Output]] = {
    getPreOutputs match {
      case Right(Some(outputs)) => Right(outputs)
      case Right(None)          => Left(Right(NonExistInput))
      case Left(error)          => Left(Left(error))
    }
  }

  protected[validation] def checkBlockTx(
      chainIndex: ChainIndex,
      tx: Transaction,
      groupView: BlockFlowGroupView[WorldState.Cached],
      blockEnv: BlockEnv,
      coinbaseNetReward: Option[U256]
  ): TxValidationResult[Unit] = {
    for {
      _ <- validateTx(
        tx,
        chainIndex,
        groupView,
        blockEnv,
        coinbaseNetReward,
        // checkDoubleSpending is false as it has been checked in block validation
        checkDoubleSpending = false
      )
    } yield ()
  }

  protected[validation] def checkStateless(
      chainIndex: ChainIndex,
      tx: Transaction,
      checkDoubleSpending: Boolean,
      hardFork: HardFork,
      isCoinbase: Boolean
  ): TxValidationResult[Unit] = {
    for {
      _ <- checkVersion(tx)
      _ <- checkNetworkId(tx)
      _ <- checkInputNum(tx, chainIndex.isIntraGroup)
      _ <- checkOutputNum(tx, chainIndex.isIntraGroup)
      _ <- checkScriptSigNum(tx, chainIndex.isIntraGroup)
      _ <- checkGasBound(tx, isCoinbase, hardFork)
      _ <- checkOutputStats(tx, hardFork)
      _ <- checkChainIndex(tx, chainIndex)
      _ <- checkUniqueInputs(tx, checkDoubleSpending)
    } yield ()
  }
  protected[validation] def checkStateful(
      chainIndex: ChainIndex,
      tx: Transaction,
      worldState: WorldState.Cached,
      preOutputs: AVector[TxOutput],
      coinbaseNetReward: Option[U256],
      blockEnv: BlockEnv
  ): TxValidationResult[Unit] = {
    for {
      gasRemaining <- checkStatefulExceptTxScript(tx, blockEnv, preOutputs, coinbaseNetReward)
      preAssetOutputs = getPrevAssetOutputs(preOutputs, tx)
      _ <- checkTxScript(chainIndex, tx, gasRemaining, worldState, preAssetOutputs, blockEnv)
    } yield ()
  }
  protected[validation] def checkStatefulExceptTxScript(
      tx: Transaction,
      blockEnv: BlockEnv,
      preOutputs: AVector[TxOutput],
      coinbaseNetReward: Option[U256]
  ): TxValidationResult[GasBox] = {
    for {
      _            <- checkLockTime(preOutputs, blockEnv.timeStamp)
      _            <- checkAlphBalance(tx, preOutputs, coinbaseNetReward)
      _            <- checkTokenBalance(tx, preOutputs)
      gasRemaining <- checkGasAndWitnesses(tx, preOutputs, blockEnv, coinbaseNetReward.isDefined)
    } yield gasRemaining
  }

  protected def getPrevAssetOutputs(
      prevOutputs: AVector[TxOutput],
      tx: Transaction
  ): AVector[AssetOutput] = {
    prevOutputs.take(tx.unsigned.inputs.length).asUnsafe[AssetOutput]
  }

  // format off for the sake of reading and checking rules
  // format: off
  protected[validation] def checkVersion(tx: Transaction): TxValidationResult[Unit]
  protected[validation] def checkNetworkId(tx: Transaction): TxValidationResult[Unit]
  protected[validation] def checkInputNum(tx: Transaction, isIntraGroup: Boolean): TxValidationResult[Unit]
  protected[validation] def checkOutputNum(tx: Transaction, isIntraGroup: Boolean): TxValidationResult[Unit]
  protected[validation] def checkScriptSigNum(tx: Transaction, isIntraGroup: Boolean): TxValidationResult[Unit]
  protected[validation] def checkGasBound(tx: TransactionAbstract, isCoinbase: Boolean, hardFork: HardFork): TxValidationResult[Unit]
  protected[validation] def checkOutputStats(tx: Transaction, hardFork: HardFork): TxValidationResult[U256]
  protected[validation] def getChainIndex(tx: TransactionAbstract): TxValidationResult[ChainIndex]
  protected[validation] def checkChainIndex(tx: Transaction, expected: ChainIndex): TxValidationResult[Unit]
  protected[validation] def checkUniqueInputs(tx: Transaction, checkDoubleSpending: Boolean): TxValidationResult[Unit]

  protected[validation] def checkLockTime(preOutputs: AVector[TxOutput], headerTs: TimeStamp): TxValidationResult[Unit]
  protected[validation] def checkAlphBalance(tx: Transaction, preOutputs: AVector[TxOutput], coinbaseNetReward: Option[U256]): TxValidationResult[Unit]
  protected[validation] def checkTokenBalance(tx: Transaction, preOutputs: AVector[TxOutput]): TxValidationResult[Unit]
  def checkGasAndWitnesses(tx: Transaction, preOutputs: AVector[TxOutput], blockEnv: BlockEnv, isCoinbase: Boolean): TxValidationResult[GasBox]
  protected[validation] def checkTxScript(
      chainIndex: ChainIndex,
      tx: Transaction,
      gasRemaining: GasBox,
      worldState: WorldState.Cached,
      preOutputs: AVector[AssetOutput],
      blockEnv: BlockEnv): TxValidationResult[GasBox]
  // format: on
}

// Note: only non-coinbase transactions are validated here
object TxValidation {
  import ValidationStatus._

  def build(implicit
      groupConfig: GroupConfig,
      networkConfig: NetworkConfig,
      logConfig: LogConfig
  ): TxValidation =
    new Impl()

  // scalastyle:off number.of.methods
  class Impl(implicit
      val groupConfig: GroupConfig,
      val networkConfig: NetworkConfig,
      val logConfig: LogConfig
  ) extends TxValidation {
    protected[validation] def checkVersion(tx: Transaction): TxValidationResult[Unit] = {
      if (tx.unsigned.version == DefaultTxVersion) {
        validTx(())
      } else {
        invalidTx(InvalidTxVersion)
      }
    }

    protected[validation] def checkNetworkId(tx: Transaction): TxValidationResult[Unit] = {
      if (tx.unsigned.networkId == networkConfig.networkId) {
        validTx(())
      } else {
        invalidTx(InvalidNetworkId)
      }
    }

    protected[validation] def checkInputNum(
        tx: Transaction,
        isIntraGroup: Boolean
    ): TxValidationResult[Unit] = {
      if (isIntraGroup) checkIntraGroupInputNum(tx) else checkInterGroupInputNum(tx)
    }
    protected[validation] def checkIntraGroupInputNum(tx: Transaction): TxValidationResult[Unit] = {
      checkInputNumCommon(tx.inputsLength)
    }
    protected[validation] def checkInterGroupInputNum(tx: Transaction): TxValidationResult[Unit] = {
      if (tx.contractInputs.nonEmpty) {
        invalidTx(ContractInputForInterGroupTx)
      } else {
        checkInputNumCommon(tx.unsigned.inputs.length)
      }
    }
    protected[validation] def checkInputNumCommon(inputNum: Int): TxValidationResult[Unit] = {
      // inputNum can be 0 due to coinbase tx
      if (inputNum > ALPH.MaxTxInputNum) {
        invalidTx(TooManyInputs)
      } else {
        validTx(())
      }
    }

    protected[validation] def checkOutputNum(
        tx: Transaction,
        isIntraGroup: Boolean
    ): TxValidationResult[Unit] = {
      if (isIntraGroup) checkIntraGroupOutputNum(tx) else checkInterGroupOutputNum(tx)
    }
    @inline protected[validation] def checkIntraGroupOutputNum(
        tx: Transaction
    ): TxValidationResult[Unit] = {
      checkOutputNumCommon(tx.outputsLength)
    }
    @inline protected[validation] def checkInterGroupOutputNum(
        tx: Transaction
    ): TxValidationResult[Unit] = {
      if (tx.generatedOutputs.nonEmpty) {
        invalidTx(GeneratedOutputForInterGroupTx)
      } else {
        checkOutputNumCommon(tx.unsigned.fixedOutputs.length)
      }
    }
    @inline protected[validation] def checkOutputNumCommon(
        outputNum: Int
    ): TxValidationResult[Unit] = {
      if (outputNum == 0) {
        invalidTx(NoOutputs)
      } else if (outputNum > ALPH.MaxTxOutputNum) {
        invalidTx(TooManyOutputs)
      } else {
        validTx(())
      }
    }

    protected[validation] def checkScriptSigNum(
        tx: Transaction,
        isIntraGroup: Boolean
    ): TxValidationResult[Unit] = {
      if (isIntraGroup) checkIntraGroupScriptSigNum(tx) else checkInterGroupScriptSigNum(tx)
    }
    @inline protected[validation] def checkIntraGroupScriptSigNum(
        tx: Transaction
    ): TxValidationResult[Unit] = {
      if (tx.scriptSignatures.length > ALPH.MaxScriptSigNum) {
        invalidTx(TooManyScriptSignatures)
      } else {
        validTx(())
      }
    }
    @inline protected[validation] def checkInterGroupScriptSigNum(
        tx: Transaction
    ): TxValidationResult[Unit] = {
      if (tx.scriptSignatures.length != 0) {
        invalidTx(UnexpectedScriptSignatures)
      } else {
        validTx(())
      }
    }

    protected[validation] def checkGasBound(
        tx: TransactionAbstract,
        isCoinbase: Boolean,
        hardFork: HardFork
    ): TxValidationResult[Unit] = {
      if (!GasBox.validate(tx.unsigned.gasAmount, hardFork)) {
        invalidTx(InvalidStartGas)
      } else if (!GasPrice.validate(tx.unsigned.gasPrice, isCoinbase, hardFork)) {
        invalidTx(InvalidGasPrice)
      } else {
        validTx(())
      }
    }

    protected[validation] def checkOutputStats(
        tx: Transaction,
        hardFork: HardFork
    ): TxValidationResult[U256] = {
      for {
        _      <- checkEachOutputStats(tx, hardFork)
        amount <- checkAlphOutputAmount(tx)
      } yield amount
    }

    protected[validation] def checkEachOutputStats(
        tx: Transaction,
        hardFork: HardFork
    ): TxValidationResult[Unit] = {
      for {
        _ <- tx.unsigned.fixedOutputs.foreachE(checkEachOutputStat(_, hardFork))
        _ <- tx.generatedOutputs.foreachE(checkEachOutputStat(_, hardFork))
      } yield ()
    }

    protected[validation] def checkEachOutputStat(
        output: TxOutput,
        hardFork: HardFork
    ): TxValidationResult[Unit] = {
      for {
        _ <- checkOutputAmount(output, hardFork)
        _ <- checkP2MPKStat(output, hardFork)
        _ <- checkOutputDataState(output)
      } yield ()
    }

    @inline private def checkOutputAmount(
        output: TxOutput,
        hardFork: HardFork
    ): TxValidationResult[Unit] = {
      if (hardFork.isLemanEnabled()) {
        checkOutputAmountLeman(output)
      } else {
        checkOutputAmountGenesis(output)
      }
    }

    @inline private def checkOutputAmountLeman(
        output: TxOutput
    ): TxValidationResult[Unit] = {
      val numTokenBound =
        if (output.isAsset) maxTokenPerAssetUtxo else maxTokenPerContractUtxo
      val validated = output.amount >= dustUtxoAmount &&
        output.tokens.length <= numTokenBound &&
        output.tokens.forall(_._2.nonZero) &&
        // If the asset output contains token, it has to contains exact dust amount of ALPH
        (output.isContract || output.tokens.isEmpty || output.amount == dustUtxoAmount)
      if (validated) Right(()) else invalidTx(InvalidOutputStats)
    }

    @inline private def checkOutputAmountGenesis(
        output: TxOutput
    ): TxValidationResult[Unit] = {
      val validated = output.amount >= deprecatedDustUtxoAmount &&
        output.tokens.length <= deprecatedMaxTokenPerUtxo &&
        output.tokens.forall(_._2.nonZero)
      if (validated) Right(()) else invalidTx(InvalidOutputStats)
    }

    @inline private def checkP2MPKStat(
        output: TxOutput,
        hardFork: HardFork
    ): TxValidationResult[Unit] = {
      if (hardFork.isLemanEnabled()) {
        output.lockupScript match {
          case LockupScript.P2MPKH(pkHashes, _) =>
            if (pkHashes.length > ALPH.MaxKeysInP2MPK) {
              invalidTx(TooManyKeysInMultisig)
            } else {
              Right(())
            }
          case _ => Right(())
        }
      } else {
        Right(())
      }
    }

    @inline private def checkOutputDataState(
        output: TxOutput
    ): TxValidationResult[Unit] = {
      output match {
        case output: AssetOutput =>
          if (output.additionalData.length > ALPH.MaxOutputDataSize) {
            invalidTx(OutputDataSizeExceeded)
          } else {
            Right(())
          }
        case _ => Right(())
      }
    }

    protected[validation] def checkAlphOutputAmount(tx: Transaction): TxValidationResult[U256] = {
      tx.attoAlphAmountInOutputs match {
        case Some(total) => validTx(total)
        case None        => invalidTx(BalanceOverFlow)
      }
    }

    protected[validation] def checkChainIndex(
        tx: Transaction,
        expected: ChainIndex
    ): TxValidationResult[Unit] = {
      val ok1 = tx.unsigned.inputs.forall(_.fromGroup == expected.from)
      val ok2 = tx.unsigned.fixedOutputs.forall(output => expected.relateTo(output.toGroup))
      val ok3 = tx.generatedOutputs.forall {
        case output: AssetOutput => expected.relateTo(output.toGroup)
        case _                   => true
      }
      // when the transaction in for intra group, the fixedOutputs might be empty
      val ok4 = expected.isIntraGroup || tx.unsigned.fixedOutputs.exists(_.toGroup == expected.to)
      if (!ok1) {
        invalidTx(InvalidInputGroupIndex)
      } else if ((!ok2) || (!ok3) || (!ok4)) {
        invalidTx(InvalidOutputGroupIndex)
      } else {
        validTx(())
      }
    }

    @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
    protected[validation] def getChainIndex(
        tx: TransactionAbstract
    ): TxValidationResult[ChainIndex] = {
      val inputIndexes = tx.unsigned.inputs.map(_.fromGroup).toSet
      if (inputIndexes.size != 1) {
        invalidTx(InvalidInputGroupIndex)
      } else {
        val fromIndex = inputIndexes.head
        val outputIndexes =
          (0 until tx.outputsLength).view
            .map(index => getToGroup(tx.getOutput(index), fromIndex))
            .filter(_ != fromIndex)
            .toSet
        outputIndexes.size match {
          case 0 => validTx(ChainIndex(fromIndex, fromIndex))
          case 1 => validTx(ChainIndex(fromIndex, outputIndexes.head))
          case _ => invalidTx(InvalidOutputGroupIndex)
        }
      }
    }

    private def getToGroup(output: TxOutput, fromIndex: GroupIndex): GroupIndex =
      output match {
        case o: AssetOutput    => o.toGroup
        case _: ContractOutput => fromIndex
      }

    protected[validation] def checkUniqueInputs(
        tx: Transaction,
        checkDoubleSpending: Boolean
    ): TxValidationResult[Unit] = {
      if (checkDoubleSpending) {
        val utxoUsed = scala.collection.mutable.Set.empty[TxOutputRef]
        tx.unsigned.inputs.foreachE { input =>
          if (utxoUsed.contains(input.outputRef)) {
            invalidTx(TxDoubleSpending)
          } else {
            utxoUsed += input.outputRef
            validTx(())
          }
        }
      } else {
        validTx(())
      }
    }

    protected[validation] def checkLockTime(
        preOutputs: AVector[TxOutput],
        headerTs: TimeStamp
    ): TxValidationResult[Unit] = {
      if (preOutputs.forall(checkLockTime(_, headerTs))) {
        validTx(())
      } else {
        invalidTx(TimeLockedTx)
      }
    }

    @inline private def checkLockTime(output: TxOutput, headerTs: TimeStamp): Boolean =
      output match {
        case o: AssetOutput if o.lockTime > headerTs => false
        case _                                       => true
      }

    protected[validation] def checkAlphBalance(
        tx: Transaction,
        preOutputs: AVector[TxOutput],
        coinbaseNetReward: Option[U256]
    ): TxValidationResult[Unit] = {
      val inputSum = preOutputs.fold(coinbaseNetReward.getOrElse(U256.Zero))(_ addUnsafe _.amount)
      val result = for {
        outputSum <- tx.attoAlphAmountInOutputs
        allOutSum <- outputSum.add(tx.gasFeeUnsafe) // safe after gas bound check
      } yield allOutSum == inputSum
      result match {
        case Some(true)  => validTx(())
        case Some(false) => invalidTx(InvalidAlphBalance)
        case None        => invalidTx(BalanceOverFlow)
      }
    }

    protected[validation] def checkTokenBalance(
        tx: Transaction,
        preOutputs: AVector[TxOutput]
    ): TxValidationResult[Unit] = {
      for {
        inputBalances  <- computeTokenBalances(preOutputs)
        outputBalances <- computeTokenBalances(tx.allOutputs)
        _ <- {
          val ok = {
            if (tx.isEntryMethodPayable && tx.scriptExecutionOk) {
              // Token balance is validated in VM execution, but let's double check here
              outputBalances.forall { case (tokenId, balance) =>
                // either new token or no inflation
                !inputBalances.contains(tokenId) || inputBalances(tokenId) >= balance
              }
            } else {
              outputBalances.forall { case (tokenId, balance) =>
                inputBalances.get(tokenId) == Option(balance)
              }
            }
          }
          if (ok) validTx(()) else invalidTx(InvalidTokenBalance)
        }
      } yield ()
    }

    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    protected[validation] def computeTokenBalances(
        outputs: AVector[TxOutput]
    ): TxValidationResult[mutable.Map[TokenId, U256]] =
      try {
        val balances = mutable.Map.empty[TokenId, U256]
        outputs.foreach { output =>
          output.tokens.foreach { case (tokenId, amount) =>
            val total = balances.getOrElse(tokenId, U256.Zero)
            balances.put(tokenId, total.add(amount).get)
          }
        }
        Right(balances)
      } catch {
        case _: NoSuchElementException => Left(Right(BalanceOverFlow))
      }

    def checkGasAndWitnesses(
        tx: Transaction,
        preOutputs: AVector[TxOutput],
        blockEnv: BlockEnv,
        isCoinbase: Boolean
    ): TxValidationResult[GasBox] = {
      for {
        gasRemaining0 <- checkBasicGas(tx, tx.unsigned.gasAmount)
        gasRemaining1 <- checkWitnesses(tx, preOutputs, blockEnv, gasRemaining0, isCoinbase)
      } yield gasRemaining1
    }

    protected[validation] def checkBasicGas(
        tx: Transaction,
        gasRemaining: GasBox
    ): TxValidationResult[GasBox] = {
      val inputGas      = GasSchedule.txInputBaseGas.mulUnsafe(tx.unsigned.inputs.length)
      val outputGas     = GasSchedule.txOutputBaseGas.mulUnsafe(tx.unsigned.fixedOutputs.length)
      val totalBasicGas = GasSchedule.txBaseGas.addUnsafe(inputGas).addUnsafe(outputGas)
      gasRemaining.sub(totalBasicGas).toRight(Right(OutOfGas))
    }

    @inline private[validation] def sameUnlockScriptAsPrevious(
        unlockScript: UnlockScript,
        previousUnlockScript: UnlockScript,
        hardFork: HardFork
    ): Boolean = {
      if (hardFork.isLemanEnabled()) {
        // Make `SameAsPrevious` optional to keep backward compatibility
        unlockScript == UnlockScript.SameAsPrevious || unlockScript == previousUnlockScript
      } else {
        unlockScript == previousUnlockScript
      }
    }

    protected[validation] def checkWitnesses(
        tx: Transaction,
        preOutputs: AVector[TxOutput],
        blockEnv: BlockEnv,
        gasRemaining: GasBox,
        isCoinbase: Boolean
    ): TxValidationResult[GasBox] = {
      assume(tx.unsigned.inputs.length <= preOutputs.length)
      val signatures = Stack.popOnly(tx.inputSignatures.reverse)
      val txEnv      = TxEnv(tx, getPrevAssetOutputs(preOutputs, tx), signatures)
      val inputs     = tx.unsigned.inputs
      for {
        remaining <- EitherF.foldTry(inputs.indices, gasRemaining) { case (gasRemaining, idx) =>
          val unlockScript = inputs(idx).unlockScript
          if (
            idx > 0 &&
            preOutputs(idx).lockupScript == preOutputs(idx - 1).lockupScript &&
            sameUnlockScriptAsPrevious(
              unlockScript,
              inputs(idx - 1).unlockScript,
              blockEnv.getHardFork()
            )
          ) {
            validTx(gasRemaining)
          } else {
            checkLockupScript(
              blockEnv,
              txEnv,
              gasRemaining,
              preOutputs(idx).lockupScript,
              unlockScript,
              isCoinbase
            )
          }
        }
        _ <- if (signatures.isEmpty) validTx(()) else invalidTx(TooManyInputSignatures)
      } yield remaining
    }

    protected[validation] def checkLockupScript(
        blockEnv: BlockEnv,
        txEnv: TxEnv,
        gasRemaining: GasBox,
        lockupScript: LockupScript,
        unlockScript: UnlockScript,
        isCoinbase: Boolean
    ): TxValidationResult[GasBox] = {
      (lockupScript, unlockScript) match {
        case (lock: LockupScript.P2PKH, unlock: UnlockScript.P2PKH) =>
          checkP2pkh(txEnv, txEnv.txId.bytes, gasRemaining, lock, unlock.publicKey)
        case (lock: LockupScript.P2MPKH, unlock: UnlockScript.P2MPKH) =>
          checkP2mpkh(txEnv, gasRemaining, lock, unlock)
        case (lock: LockupScript.P2SH, unlock: UnlockScript.P2SH) =>
          checkP2SH(blockEnv, txEnv, gasRemaining, lock, unlock)
        case (lock: LockupScript.P2PKH, unlock: UnlockScript.PoLW) if isCoinbase =>
          assume(blockEnv.getHardFork() == HardFork.Rhone)
          val addressTo = txEnv.fixedOutputs(0).lockupScript
          val preImage  = UnlockScript.PoLW.buildPreImage(lock, addressTo)
          checkP2pkh(txEnv, preImage, gasRemaining, lock, unlock.publicKey)
        case _ =>
          invalidTx(InvalidUnlockScriptType)
      }
    }

    protected[validation] def checkP2pkh(
        txEnv: TxEnv,
        preImage: ByteString,
        gasRemaining: GasBox,
        lock: LockupScript.P2PKH,
        publicKey: PublicKey
    ): TxValidationResult[GasBox] = {
      if (Hash.hash(publicKey.bytes) != lock.pkHash) {
        invalidTx(InvalidPublicKeyHash)
      } else {
        checkSignature(txEnv, preImage, gasRemaining, publicKey)
      }
    }

    private def checkSignature(
        txEnv: TxEnv,
        preImage: ByteString,
        gasRemaining: GasBox,
        publicKey: PublicKey
    ): TxValidationResult[GasBox] = {
      txEnv.signatures.pop() match {
        case Right(signature) =>
          if (!SignatureSchema.verify(preImage, signature, publicKey)) {
            invalidTx(InvalidSignature)
          } else {
            fromOption(gasRemaining.sub(GasSchedule.p2pkUnlockGas), OutOfGas)
          }
        case Left(_) => invalidTx(NotEnoughSignature)
      }
    }

    protected[validation] def checkP2mpkh(
        txEnv: TxEnv,
        gasRemaining: GasBox,
        lock: LockupScript.P2MPKH,
        unlock: UnlockScript.P2MPKH
    ): TxValidationResult[GasBox] = {
      if (unlock.indexedPublicKeys.length != lock.m) {
        invalidTx(InvalidNumberOfPublicKey)
      } else if (!UnlockScript.validateP2mpkh(unlock)) {
        invalidTx(InvalidP2mpkhUnlockScript)
      } else {
        unlock.indexedPublicKeys
          .foldE(gasRemaining) { case (gasBox, (publicKey, index)) =>
            lock.pkHashes.get(index) match {
              case Some(pkHash) =>
                if (Hash.hash(publicKey.bytes) != pkHash) {
                  invalidTx(InvalidPublicKeyHash)
                } else {
                  checkSignature(txEnv, txEnv.txId.bytes, gasBox, publicKey)
                }
              case None =>
                invalidTx(InvalidP2mpkhUnlockScript)
            }
          }
      }
    }

    @inline protected[validation] def checkP2SH(
        blockEnv: BlockEnv,
        txEnv: TxEnv,
        gasRemaining: GasBox,
        lock: LockupScript.P2SH,
        unlock: UnlockScript.P2SH
    ): TxValidationResult[GasBox] = {
      checkUnlockScript(
        blockEnv,
        txEnv,
        gasRemaining,
        lock.scriptHash,
        unlock.script,
        unlock.params
      )
    }

    protected[validation] def checkUnlockScript(
        blockEnv: BlockEnv,
        txEnv: TxEnv,
        gasRemaining: GasBox,
        expectedScriptHash: Hash,
        script: StatelessScript,
        params: AVector[Val]
    ): TxValidationResult[GasBox] = {
      if (script.hash != expectedScriptHash) {
        invalidTx(InvalidScriptHash)
      } else {
        fromExeResult(
          for {
            remaining0 <- VM.checkCodeSize(gasRemaining, script.bytes, blockEnv.getHardFork())
            remaining1 <- remaining0.use(GasHash.gas(script.bytes.length))
            exeResult  <- StatelessVM.runAssetScript(blockEnv, txEnv, remaining1, script, params)
          } yield exeResult.gasRemaining,
          UnlockScriptExeFailed.apply
        )
      }
    }

    protected[validation] def checkTxScript(
        chainIndex: ChainIndex,
        tx: Transaction,
        gasRemaining: GasBox,
        worldState: WorldState.Cached,
        preAssetOutputs: AVector[AssetOutput],
        blockEnv: BlockEnv
    ): TxValidationResult[GasBox] = {
      if (chainIndex.isIntraGroup) {
        tx.unsigned.scriptOpt match {
          case Some(script) =>
            val stagingWorldState = worldState.staging()
            executeTxScript(
              tx,
              script,
              gasRemaining,
              stagingWorldState,
              preAssetOutputs,
              blockEnv
            ) match {
              case Right(TxScriptExecution(remaining, contractInputs, _, generatedOutputs)) =>
                if (contractInputs != tx.contractInputs) {
                  invalidTx(InvalidContractInputs)
                } else if (generatedOutputs != tx.generatedOutputs) {
                  invalidTx(InvalidGeneratedOutputs)
                } else {
                  stagingWorldState.commit()
                  checkScriptExeFlag(tx, true, remaining)
                }
              case Left(Right(_: BreakingInstr)) =>
                // This case should already be filtered by mempool and block assembly, double check here
                invalidTx(UsingBreakingInstrs)
              case Left(Right(_)) =>
                checkScriptExeFlag(tx, false, GasBox.zero)
              case Left(Left(ioFalure)) => Left(Left(ioFalure.error))
            }
          case None => checkScriptExeFlag(tx, true, gasRemaining)
        }
      } else {
        if (tx.unsigned.scriptOpt.nonEmpty) {
          invalidTx(UnexpectedTxScript)
        } else {
          checkScriptExeFlag(tx, true, gasRemaining)
        }
      }
    }

    private def checkScriptExeFlag[T](
        tx: Transaction,
        expectedScriptExeOk: Boolean,
        result: T
    ): TxValidationResult[T] = {
      if (tx.scriptExecutionOk == expectedScriptExeOk) {
        validTx(result)
      } else {
        invalidTx(InvalidScriptExecutionFlag)
      }
    }

    private def executeTxScript(
        tx: TransactionAbstract,
        script: StatefulScript,
        gasRemaining: GasBox,
        worldState: WorldState.Staging,
        preAssetOutputs: AVector[AssetOutput],
        blockEnv: BlockEnv
    ): ExeResult[StatefulVM.TxScriptExecution] = {
      for {
        remaining <- VM.checkCodeSize(gasRemaining, script.bytes, blockEnv.getHardFork())
        result <-
          StatefulVM.runTxScript(
            worldState,
            blockEnv,
            tx,
            preAssetOutputs,
            script,
            remaining
          )
      } yield result
    }
  }
  // scalastyle:on number.of.methods
}
