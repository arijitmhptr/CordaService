package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.MoneyContract;
import com.template.states.MoneyState;
import com.template.states.PaymentRequest;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// ******************
// * Responder flow *
// ******************
public class MoneyFlow {

    @InitiatingFlow
    @StartableByService
    public static class MoneyFlowInitiator extends FlowLogic<SignedTransaction> {

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Responder flow logic goes here.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            List<StateAndRef<PaymentRequest>> stateList = getServiceHub().getVaultService().queryBy(PaymentRequest.class).getStates();
            PaymentRequest vaultList = stateList.get(stateList.size() - 1).getState().getData();
            MoneyState outputState = new MoneyState(vaultList.getAmount(),vaultList.getEmployee());

            TransactionBuilder txbuilder = new TransactionBuilder(notary);
            MoneyContract.Commands.Pay command = new MoneyContract.Commands.Pay();
            txbuilder.addCommand(command,getOurIdentity().getOwningKey(),vaultList.getEmployee().getOwningKey());
            txbuilder.addOutputState(outputState, MoneyContract.ID);
                txbuilder.verify(getServiceHub());

            FlowSession session = initiateFlow(vaultList.getEmployee());
            SignedTransaction stx = getServiceHub().signInitialTransaction(txbuilder);
            SignedTransaction ptx = subFlow(new CollectSignaturesFlow(stx, Arrays.asList(session)));
            return subFlow(new FinalityFlow(ptx,Arrays.asList(session)));
        }
    }

    @InitiatedBy(com.template.flows.MoneyFlow.MoneyFlowInitiator.class)
    public static class MoneyFlowResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public MoneyFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Responder flow logic goes here.
            SignedTransaction stx = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                if (!counterpartySession.getCounterparty().equals(getServiceHub().getNetworkMapCache().getPeerByLegalName(new CordaX500Name("HDFC", "Kolkata", "IN"))))
                    {
                        throw new IllegalArgumentException("Only bank node can send payment");
                    }
                }
            });
            subFlow(new ReceiveFinalityFlow(counterpartySession,stx.getId()));
            return null;
        }
    }
}