package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sun.javafx.collections.ImmutableObservableList;
import com.template.contracts.PaymentContract;
import com.template.states.PaymentRequest;
import jdk.nashorn.internal.ir.annotations.Immutable;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

// ******************
// * Initiator flow *
// ******************

public class RequestFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class RequestFlowInitiator extends FlowLogic<SignedTransaction> {
        private final int amount;
        private final Party employee;

        public RequestFlowInitiator(int amount, Party employee) {
            this.amount = amount;
            this.employee = employee;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Initiator flow logic goes here.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party bank = getServiceHub().getNetworkMapCache().getPeerByLegalName(new
                    CordaX500Name("HDFC", "Kolkata", "IN"));
            PaymentRequest outputState = new PaymentRequest(amount, employee, Arrays.asList(getOurIdentity(), bank));

            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            CommandData command = new PaymentContract.Commands.Request();
            txBuilder.addCommand(command,getOurIdentity().getOwningKey(),bank.getOwningKey());
            txBuilder.addOutputState(outputState, PaymentContract.ID);
            txBuilder.verify(getServiceHub());

            FlowSession session = initiateFlow(bank);
            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));
            return subFlow(new FinalityFlow(stx,Arrays.asList(session)));
        }
    }

    @InitiatedBy(RequestFlowInitiator.class)
    public static class RequestFlowResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public RequestFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Responder flow logic goes here.
            SignedTransaction stx = subFlow(new SignTransactionFlow(counterpartySession){

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    if (! stx.getInputs().isEmpty()){
                        throw new IllegalArgumentException("Payment request should not have any inputs");
                    }
                }
            });
            subFlow(new ReceiveFinalityFlow(counterpartySession,stx.getId()));
            return null;
        }
    }
}