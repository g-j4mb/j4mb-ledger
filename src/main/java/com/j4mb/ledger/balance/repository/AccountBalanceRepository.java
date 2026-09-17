package com.j4mb.ledger.balance.repository;
import com.j4mb.ledger.balance.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    Optional<AccountBalance> findByAccountIdAndFiscalPeriodIdAndCurrencyCode(UUID accountId, UUID periodId, String currency);
    List<AccountBalance> findByFiscalPeriodId(UUID periodId);

    /**
     * Atomically applies a net debit/credit delta only if the resulting balance stays
     * within the overdraft limit. Returns 0 affected rows if the row doesn't exist yet
     * or the limit would be breached — the caller must distinguish the two.
     */
    @Modifying
    @Query("""
            UPDATE AccountBalance b
               SET b.periodDebit    = b.periodDebit + :netDebit,
                   b.periodCredit   = b.periodCredit + :netCredit,
                   b.closingDebit   = b.openingDebit + b.periodDebit + :netDebit,
                   b.closingCredit  = b.openingCredit + b.periodCredit + :netCredit,
                   b.updatedBy      = :updatedBy,
                   b.lastRebuiltUtc = :now,
                   b.version        = b.version + 1
             WHERE b.accountId = :accountId
               AND b.fiscalPeriodId = :fiscalPeriodId
               AND b.currencyCode = :currencyCode
               AND (b.openingDebit + b.periodDebit + :netDebit) - (b.openingCredit + b.periodCredit + :netCredit) >= :minNet
            """)
    int applyDeltaIfWithinLimit(@Param("accountId") UUID accountId,
                                 @Param("fiscalPeriodId") UUID fiscalPeriodId,
                                 @Param("currencyCode") String currencyCode,
                                 @Param("netDebit") BigDecimal netDebit,
                                 @Param("netCredit") BigDecimal netCredit,
                                 @Param("minNet") BigDecimal minNet,
                                 @Param("updatedBy") String updatedBy,
                                 @Param("now") Instant now);

    /** Same as {@link #applyDeltaIfWithinLimit} but unconditional — for accounts that allow a negative balance. */
    @Modifying
    @Query("""
            UPDATE AccountBalance b
               SET b.periodDebit    = b.periodDebit + :netDebit,
                   b.periodCredit   = b.periodCredit + :netCredit,
                   b.closingDebit   = b.openingDebit + b.periodDebit + :netDebit,
                   b.closingCredit  = b.openingCredit + b.periodCredit + :netCredit,
                   b.updatedBy      = :updatedBy,
                   b.lastRebuiltUtc = :now,
                   b.version        = b.version + 1
             WHERE b.accountId = :accountId
               AND b.fiscalPeriodId = :fiscalPeriodId
               AND b.currencyCode = :currencyCode
            """)
    int applyDelta(@Param("accountId") UUID accountId,
                    @Param("fiscalPeriodId") UUID fiscalPeriodId,
                    @Param("currencyCode") String currencyCode,
                    @Param("netDebit") BigDecimal netDebit,
                    @Param("netCredit") BigDecimal netCredit,
                    @Param("updatedBy") String updatedBy,
                    @Param("now") Instant now);
}
