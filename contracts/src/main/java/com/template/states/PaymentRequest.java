package com.template.states;

import com.template.contracts.PaymentContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(PaymentContract.class)
public class PaymentRequest implements ContractState {
    private final int amount;
    private final Party employee;
    private final List<AbstractParty> participants;
    public PaymentRequest(int amount, Party employee, List<AbstractParty> participants) {
        this.amount = amount;
        this.employee = employee;
        this.participants = participants;
    }

    public int getAmount() {
        return amount;
    }

    public Party getEmployee() {
        return employee;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}