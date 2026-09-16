package com.j4mb.ledger.currency.service;

import com.j4mb.ledger.currency.domain.Currency;
import com.j4mb.ledger.currency.repository.CurrencyRepository;
import com.j4mb.ledger.shared.context.UserContext;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final UserContext         userContext;

    CurrencyService(CurrencyRepository currencyRepository, UserContext userContext) {
        this.currencyRepository = currencyRepository;
        this.userContext         = userContext;
    }

    public List<Currency> listAll() {
        return currencyRepository.findAll();
    }

    public Currency findByCode(String code) {
        return currencyRepository.findByCurrencyCode(code)
                .orElseThrow(() -> new LedgerException("Currency not found: " + code));
    }

    public Currency findBaseCurrency() {
        return currencyRepository.findByBaseCurrencyTrue()
                .orElseThrow(() -> new LedgerException("No base currency configured"));
    }

    public Currency findById(UUID id) {
        return currencyRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Currency not found: " + id));
    }

    @Transactional
    public Currency update(UUID id, String currencyName, Boolean active, Integer decimalPlaces) {
        Currency currency = findById(id);
        currency.updateName(currencyName);
        if (active != null) currency.setActive(active);
        if (decimalPlaces != null) currency.updateDecimalPlaces(decimalPlaces);
        return currencyRepository.save(currency);
    }

    @Transactional
    public Currency create(String code, String name, boolean isBase, int decimalPlaces) {
        if (currencyRepository.findByCurrencyCode(code).isPresent()) {
            throw new LedgerException("Currency already exists: " + code);
        }
        if (isBase && currencyRepository.findByBaseCurrencyTrue().isPresent()) {
            throw new LedgerException("A base currency is already configured");
        }
        return currencyRepository.save(
                Currency.create(code.toUpperCase(), name, isBase, decimalPlaces, userContext.getUserId()));
    }
}
