package com.j4mb.ledger.account.service;

import com.j4mb.ledger.account.domain.Account;
import com.j4mb.ledger.account.repository.AccountRepository;
import com.j4mb.ledger.coa.domain.AccountType;
import com.j4mb.ledger.coa.domain.CoaNode;
import com.j4mb.ledger.coa.domain.NormalBalance;
import com.j4mb.ledger.coa.repository.CoaNodeRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock CoaNodeRepository coaNodeRepository;
    @Mock UserContext        userContext;

    @InjectMocks AccountService service;

    @Test
    void shouldCreateAccount_whenCoaNodeIsPostable() {
        // Given
        UUID coaNodeId = UUID.randomUUID();
        CoaNode node = CoaNode.create(null, "1000", "Cash", "ASSET.CASH", 1,
                AccountType.ASSET, NormalBalance.DEBIT, 1, "test");
        node.markPostable(true);

        when(coaNodeRepository.findById(coaNodeId)).thenReturn(Optional.of(node));
        when(accountRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userContext.getUserId()).thenReturn("test");

        // When
        Account created = service.create(coaNodeId, "ACC-001", "Cash Account", "MYR");

        // Then
        assertThat(created.getAccountNumber()).isEqualTo("ACC-001");
        assertThat(created.getName()).isEqualTo("Cash Account");
        assertThat(created.getCurrencyCode()).isEqualTo("MYR");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void shouldThrow_whenCoaNodeNotFound() {
        // Given
        UUID coaNodeId = UUID.randomUUID();
        when(coaNodeRepository.findById(coaNodeId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.create(coaNodeId, "ACC-001", "Cash Account", "MYR"))
                .isInstanceOf(LedgerException.class)
                .hasMessageContaining(coaNodeId.toString());
    }

    @Test
    void shouldThrow_whenCoaNodeNotPostable() {
        // Given
        UUID coaNodeId = UUID.randomUUID();
        CoaNode node = CoaNode.create(null, "1000", "Cash", "ASSET.CASH", 1,
                AccountType.ASSET, NormalBalance.DEBIT, 1, "test");
        node.markPostable(false);

        when(coaNodeRepository.findById(coaNodeId)).thenReturn(Optional.of(node));

        // When / Then
        assertThatThrownBy(() -> service.create(coaNodeId, "ACC-001", "Cash Account", "MYR"))
                .isInstanceOf(LedgerException.class);
    }

    @Test
    void shouldThrow_whenAccountNumberAlreadyExists() {
        // Given
        UUID coaNodeId = UUID.randomUUID();
        CoaNode node = CoaNode.create(null, "1000", "Cash", "ASSET.CASH", 1,
                AccountType.ASSET, NormalBalance.DEBIT, 1, "test");
        node.markPostable(true);

        when(coaNodeRepository.findById(coaNodeId)).thenReturn(Optional.of(node));
        when(accountRepository.findByAccountNumber("ACC-001"))
                .thenReturn(Optional.of(Account.create(coaNodeId, "ACC-001", "Existing", "MYR", "test")));

        // When / Then
        assertThatThrownBy(() -> service.create(coaNodeId, "ACC-001", "Cash Account", "MYR"))
                .isInstanceOf(LedgerException.class)
                .hasMessageContaining("ACC-001");
    }

    @Test
    void shouldFreezeAccount() {
        // Given
        UUID accountId = UUID.randomUUID();
        Account account = Account.create(UUID.randomUUID(), "ACC-001", "Cash Account", "MYR", "test");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // When
        service.freeze(accountId);

        // Then
        assertThat(account.isFrozen()).isTrue();
    }

    @Test
    void shouldThrow_whenAccountNotFound() {
        // Given
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.findById(accountId))
                .isInstanceOf(LedgerException.class)
                .hasMessageContaining(accountId.toString());
    }
}
