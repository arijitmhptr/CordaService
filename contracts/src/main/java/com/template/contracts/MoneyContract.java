package com.template.contracts;

import com.template.states.MoneyState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class MoneyContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.MoneyContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);
        if (cmd.getValue() instanceof Commands.Pay){
            MoneyState moneystate = tx.outputsOfType(MoneyState.class).get(0);
            requireThat(req ->
            {
                req.using("Payment should be positive", moneystate.getAmount() > 0);
                return null;
            });
        }
        else {
            throw new IllegalArgumentException("Command is not recognised");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Pay implements Commands {}
    }
}