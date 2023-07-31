package com.assessment.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.assessment.Const;
import com.assessment.contracts.TradeContract;
import com.assessment.states.TradeState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TradeFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<UUID> {
        private Party toParty;
        private int amount;

        public Initiator(Party toParty, int amount) {
            this.toParty = toParty;
            this.amount = amount;
        }


        @Override
        @Suspendable
        public UUID call() throws FlowException {
            // We retrieve the notary identity from the network map.

            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party fromParty = getOurIdentity();

            //          Creating new state with uniqueIndentifier and current date;
            Date transactionDate = new Date();
            UniqueIdentifier uuid = new UniqueIdentifier();
            TradeState outPutTradeState = new
                    TradeState(uuid, fromParty,
                    toParty, amount, transactionDate, Const.SUBMITTED, null);
            TradeContract.Create createTradeCommand = new TradeContract.Create();

            List<PublicKey> requiredSigners = Arrays.asList(fromParty.getOwningKey(), toParty.getOwningKey());

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(outPutTradeState, TradeContract.ID)
                    .addCommand(createTradeCommand, requiredSigners);

            // Signing the transaction.
            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            txBuilder.verify(getServiceHub());

            // Creating a session with the other party.
            FlowSession toPartySession = initiateFlow(toParty);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    signedTx, Arrays.asList(toPartySession), CollectSignaturesFlow.tracker()));

            SignedTransaction notarisedTransaction = subFlow(new FinalityFlow(fullySignedTx, toPartySession));
            StateAndRef oldStateref =  getServiceHub().toStateAndRef(new StateRef(notarisedTransaction.getId(),0));
            TradeState oldTradeState = (TradeState) oldStateref.getState().getData();

            return oldTradeState.getLinearId().getId();
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {
        private final FlowSession toPartySession;

        public Acceptor(FlowSession toPartySession) {
            this.toPartySession = toPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartySession) {
                    super(otherPartySession);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    TradeState tradeState = (TradeState) output;
                    if(tradeState.getAmount() > 1000)
                        throw new IllegalArgumentException("Trade amount should be less than 1000");
                }
            }


            SecureHash expectedTxId = subFlow(new SignTxFlow(toPartySession)).getId();

            return subFlow(new ReceiveFinalityFlow(toPartySession, expectedTxId));
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Settle extends FlowLogic<UUID> {

        private UUID tradeId;

        public Settle(UUID tradeId) {
            this.tradeId = tradeId;
        }

        @Suspendable
        @Override
        public UUID call() throws FlowException {
            List<StateAndRef<TradeState>> tradeStateStateAndRefs = getServiceHub().getVaultService().queryBy(TradeState.class).getStates();

            StateAndRef<TradeState> inputTradeStateAndRef = tradeStateStateAndRefs
                    .stream().filter(tradeStateAndRef -> {
                        TradeState tradeState = tradeStateAndRef.getState().getData();
                        return tradeState.getLinearId().getId().equals(tradeId);

                    }).findAny().orElseThrow(() -> new IllegalArgumentException("The trade state not available"));
            TradeState inputTradeState = inputTradeStateAndRef.getState().getData();

            Party toParty = getOurIdentity();

            // We use the notary used by the input state.
            Party notary = inputTradeStateAndRef.getState().getNotary();

            TradeState outputTradeState = new TradeState(inputTradeState.getLinearId(), inputTradeState.getFromParty(), inputTradeState.getToParty(), inputTradeState.getAmount(), inputTradeState.getTradeDate(), Const.SETTLED, toParty.getName().toString());

            // We build a transaction using a `TransactionBuilder`.
            TransactionBuilder txBuilder = new TransactionBuilder();
            TradeContract.Settle settleCommand = new TradeContract.Settle();

            txBuilder.setNotary(notary);
            txBuilder.addInputState(inputTradeStateAndRef);
            txBuilder.addOutputState(outputTradeState, TradeContract.ID);
            txBuilder.addCommand(settleCommand, toParty.getOwningKey());
            txBuilder.verify(getServiceHub());

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession fromPartySession = initiateFlow(inputTradeState.getFromParty());

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    signedTx, Arrays.asList(fromPartySession), CollectSignaturesFlow.tracker()));

            SignedTransaction notraizedtransaction = subFlow(new FinalityFlow(signedTx, fromPartySession));
            StateAndRef oldStateref =  getServiceHub().toStateAndRef(new StateRef(notraizedtransaction.getId(),0));
            TradeState oldTradeState = (TradeState) oldStateref.getState().getData();

            return oldTradeState.getLinearId().getId();
        }
    }

    @InitiatedBy(Settle.class)
    public static class SettleFlowResponder extends FlowLogic<Void> {

        private final FlowSession otherPartySession;

        public SettleFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {

            SignedTransaction ledgerTransaction = subFlow(new ReceiveFinalityFlow(otherPartySession));
            System.out.println("ledger transaction = " + ledgerTransaction);
            return null;
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class Close extends FlowLogic<Void> {
        private UUID tradeId;

        public Close(UUID tradeId) {
            this.tradeId = tradeId;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            List<StateAndRef<TradeState>> tradeStateStateAndRefs = getServiceHub().getVaultService().queryBy(TradeState.class).getStates();

            StateAndRef<TradeState> inputTradeStateAndRef = tradeStateStateAndRefs
                    .stream().filter(tradeStateAndRef -> {
                        TradeState tradeState = tradeStateAndRef.getState().getData();
                        return tradeState.getLinearId().getId().equals(tradeId);
                    }).findAny().orElseThrow(() -> new IllegalArgumentException("The trade state not available"));
            TradeState inputTradeState = inputTradeStateAndRef.getState().getData();

            Party notary = inputTradeStateAndRef.getState().getNotary();

            TransactionBuilder txBuilder = new TransactionBuilder();
            TradeContract.Close closeTradeCommand = new TradeContract.Close();

            txBuilder.setNotary(notary);
            txBuilder.addInputState(inputTradeStateAndRef);
            txBuilder.addCommand(closeTradeCommand, getOurIdentity().getOwningKey());
            txBuilder.verify(getServiceHub());

            SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
            FlowSession fromPartySession = initiateFlow(inputTradeState.getFromParty());
            subFlow(new FinalityFlow(signedTx, fromPartySession));
            return null;
        }
    }

    @InitiatedBy(Close.class)
    @Suspendable
    public static class CloseFlowResponder extends FlowLogic<Void> {

        private final FlowSession otherPartySession;

        public CloseFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            subFlow(new ReceiveFinalityFlow(otherPartySession));
            return null;
        }
    }
}