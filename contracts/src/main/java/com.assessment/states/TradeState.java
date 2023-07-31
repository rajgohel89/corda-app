package com.assessment.states;

import com.assessment.contracts.TradeContract;
import com.assessment.schemas.TradeSchema;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@BelongsToContract(TradeContract.class)
public class TradeState implements LinearState, QueryableState {

    private UniqueIdentifier linearId;
    private Party fromParty;
    private Party toParty;
    private int amount;
    private Date tradeDate;
    private String status;
    private String updatedBy;

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof TradeSchema) {
            return new TradeSchema.PersistentTrade(this.getLinearId().getId(),
                    this.fromParty.getName().toString(),
                    this.toParty.getName().toString(),
                    this.getAmount(),
                    this.getTradeDate(),
                    this.getStatus(),
                    this.getUpdatedBy());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new TradeSchema());
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        // Returning list of party which will be a part of state.
        return Arrays.asList(fromParty, toParty);
    }

    public TradeState(UniqueIdentifier linearId, Party fromParty, Party toParty, int amount, Date tradeDate, String status, String updatedBy) {
        this.linearId = linearId;
        this.fromParty = fromParty;
        this.toParty = toParty;
        this.amount = amount;
        this.tradeDate = tradeDate;
        this.status = status;
        this.updatedBy = updatedBy;
    }

    public Party getFromParty() {
        return fromParty;
    }

    public Party getToParty() {
        return toParty;
    }

    public int getAmount() {
        return amount;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public String getStatus() {
        return status;
    }

    public String getUpdatedBy() { return updatedBy; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
