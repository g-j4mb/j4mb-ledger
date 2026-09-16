package com.j4mb.ledger.currency.service;

import com.j4mb.ledger.currency.domain.ExchangeRate;
import com.j4mb.ledger.currency.repository.ExchangeRateRepository;
import com.j4mb.ledger.shared.exception.LedgerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public ExchangeRate findById(UUID id) {
        return exchangeRateRepository.findById(id)
                .orElseThrow(() -> new LedgerException("Exchange rate not found: " + id));
    }

    public List<ExchangeRate> listAll() {
        return exchangeRateRepository.findAll();
    }

    @Transactional
    public ExchangeRate update(UUID id, BigDecimal rate, String source) {
        ExchangeRate er = findById(id);
        if (rate != null) {
            if (rate.compareTo(BigDecimal.ZERO) <= 0)
                throw new LedgerException("Exchange rate must be positive");
            er.updateRate(rate);
        }
        if (source != null) er.updateSource(source);
        return exchangeRateRepository.save(er);
    }

    @Transactional
    public void delete(UUID id) {
        findById(id); // throws if not found
        exchangeRateRepository.deleteById(id);
    }
}
