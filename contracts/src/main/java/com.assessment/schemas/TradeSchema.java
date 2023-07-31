package com.assessment.schemas;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class TradeSchema extends MappedSchema {

    public TradeSchema() {
        super(TradeSchema.class, 1, Arrays.asList(PersistentTrade.class));
    }

    @Entity
    @Table(name = "trade_state")
    public static class PersistentTrade extends PersistentState {
        @Column(name = "trade_id") private final UUID tradeId;
        @Column(name = "seller") private final String seller;
        @Column(name = "buyer") private final String buyer;
        @Column(name = "amount") private final int amount;
        @Column(name = "trade_date") private final Date tradeDate;
        @Column(name = "status") private final String status;
        @Column(name = "updated_by") private final String updatedBy;

        public PersistentTrade(UUID tradeId, String seller, String buyer, int amount,
                               Date tradeDate, String status, String updatedBy) {
            this.tradeId = tradeId;
            this.seller = seller;
            this.buyer = buyer;
            this.amount = amount;
            this.tradeDate = tradeDate;
            this.status = status;
            this.updatedBy = updatedBy;
        }

        public PersistentTrade(){
            this.tradeId = null;
            this.seller = null;
            this.buyer = null;
            this.amount = 0;
            this.tradeDate = null;
            this.status = null;
            this.updatedBy = null;
        }

        public UUID getTradeId() {
            return tradeId;
        }

        public String getSeller() {
            return seller;
        }

        public String getBuyer() {
            return buyer;
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
    }
}
