package com.assessment.contracts;

import co.paralleluniverse.fibers.Suspendable;
import com.assessment.Const;
import com.assessment.states.TradeState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import javax.validation.constraints.NotNull;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

public class TradeContract implements Contract {
    public static final String ID = "com.assessment.contracts.TradeContract";


    public static class Create implements CommandData {}
    public static class Settle implements CommandData {}
    public static class Close implements CommandData {}


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if(tx.getCommands().size() != 1){
            throw new IllegalArgumentException("Transaction must have one command");
        }

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();

        CommandData commandType = command.getValue();
        if(commandType instanceof TradeContract.Create){
            // Input-Output state constraints
            if(tx.getInputStates().size() != 0)
                throw new IllegalArgumentException("New state of trade must have zero input");
            if(tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("New State of trade must have one output");

            // Content constraints (state conditions)
            ContractState outputState = tx.getOutput(0);
            if(!(outputState instanceof TradeState))
                throw new IllegalArgumentException("Output state must be of trade state");
            TradeState tradeState = (TradeState) outputState;
            if(tradeState.getToParty().equals(tradeState.getFromParty()))
                throw new IllegalArgumentException("Buyer and seller should not be same");
            if(tradeState.getAmount() < 1)
                throw new IllegalArgumentException("Trade amount should grater than 0");

            // Required signers constraints.
            List<PublicKey> expectedSigner = Arrays.asList(tradeState.getFromParty().getOwningKey(), tradeState.getToParty().getOwningKey());

            if(requiredSigners.size() != 2)
                throw new IllegalArgumentException("Two signers are required");


            if(!(requiredSigners.equals(expectedSigner)))
                throw new IllegalArgumentException("Both signer must sign");

        } else if (commandType instanceof TradeContract.Settle) {

            // Input-Output state constraints
            if(tx.getInputStates().size() != 1)
                throw new IllegalArgumentException("Settle state should have one input");
            if(tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("Settle state should have one output");

            // Content constraints (state conditions)
            ContractState outPutState = tx.getOutput(0);
            if(!(outPutState instanceof TradeState))
                throw new IllegalArgumentException("Output state must be of type trade state");

            TradeState outPutTradeState = (TradeState) outPutState;
            if(!outPutTradeState.getStatus().equalsIgnoreCase(Const.SETTLED))
                throw new IllegalArgumentException("Trade status must settle");

            List<PublicKey> expectedSigner = Arrays.asList(outPutTradeState.getToParty().getOwningKey());
            if(!(requiredSigners.equals(expectedSigner)))
                throw new IllegalArgumentException("Current party must sign");

        } else if(commandType instanceof TradeContract.Close) {
            ContractState inputState = tx.getInput(0);
            if(!(inputState instanceof TradeState))
                throw new IllegalArgumentException("Input state must be of type trade state");
            TradeState inputTradeState = (TradeState) inputState;

            List<PublicKey> expectedSigner = Arrays.asList(inputTradeState.getToParty().getOwningKey());
            if(!(requiredSigners.equals(expectedSigner)))
                throw new IllegalArgumentException("Current party must sign");
        }else {
            throw new IllegalArgumentException("Command type not recognized");
        }
    }
}
