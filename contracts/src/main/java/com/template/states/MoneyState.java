package com.template.states;

import com.template.contracts.MoneyContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(MoneyContract.class)
public class MoneyState implements ContractState {
    private final int amount;
    private final Party receiver;

    public int getAmount() {
        return amount;
    }

    public Party getReceiver() {
        return receiver;
    }

    public MoneyState(int amount, Party receiver) {
        this.amount = amount;
        this.receiver = receiver;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Collections.singletonList(receiver);
    }
}