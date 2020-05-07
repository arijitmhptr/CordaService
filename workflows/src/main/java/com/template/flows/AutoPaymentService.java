package com.template.flows;

import com.template.states.PaymentRequest;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializationToken;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@CordaService
public class AutoPaymentService extends SingletonSerializeAsToken {
    private final static Logger log = LoggerFactory.getLogger(AutoPaymentService.class);
    private final static Executor executor = Executors.newFixedThreadPool(8);
    private final AppServiceHub serviceHub;

    public AutoPaymentService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        directPayment();
        log.info("Tracking new payment request");
    }
    private void directPayment(){
        Party identity = ourIdentity();
        serviceHub.getVaultService().trackBy(PaymentRequest.class).getUpdates().subscribe(
                update -> {
                    update.getProduced().forEach(
                            message -> {
                                TransactionState<PaymentRequest> state = message.getState();
                                if (identity.equals(
                                        serviceHub.getNetworkMapCache().getPeerByLegalName(new CordaX500Name("HDFC", "Kolkata", "IN"))
                                )) {
                                    executor.execute(() -> {
                                        log.info("Directing to message " + state);
                                        serviceHub.startFlow(new MoneyFlow.MoneyFlowInitiator()); // START FLOW HERE
                                    });
                                }
                            }
                    );
                }
        );
    }
    private Party ourIdentity() {
        return serviceHub.getMyInfo().getLegalIdentities().get(0);
    }
}
