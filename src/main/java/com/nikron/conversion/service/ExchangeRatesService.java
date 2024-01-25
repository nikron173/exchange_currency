package com.nikron.conversion.service;

import com.nikron.conversion.dto.ExchangeRatesDto;
import com.nikron.conversion.dto.ExchangeRequestDto;
import com.nikron.conversion.dto.ExchangeResponseDto;
import com.nikron.conversion.exception.BadRequestException;
import com.nikron.conversion.exception.NotFoundException;
import com.nikron.conversion.model.Currency;
import com.nikron.conversion.model.ExchangeRates;
import com.nikron.conversion.repository.CurrencyRepository;
import com.nikron.conversion.repository.ExchangeRatesRepository;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ExchangeRatesService {

    private final ExchangeRatesRepository exchangeRatesRepository = ExchangeRatesRepository.getInstanceRepository();
    private final CurrencyRepository currencyRepository = CurrencyRepository.getInstanceRepository();

    private static final ExchangeRatesService INSTANCE_SERVICE = new ExchangeRatesService();

    private ExchangeRatesService() {
    }

    public static ExchangeRatesService getInstanceService() {
        return INSTANCE_SERVICE;
    }

    public Optional<ExchangeRates> findById(long id) {
        return exchangeRatesRepository.findById(id);
    }

    public Optional<ExchangeRates> findByCode(String code) {
        Optional<ExchangeRates> exchangeRates;
        if ((exchangeRates = exchangeRatesRepository.findByCode(code)).isPresent()) {
            return exchangeRates;
        }
        if ((exchangeRates = exchangeRatesRepository.findByCodeRevert(code)).isPresent()) {
            return exchangeRates;
        }
        return Optional.empty();
    }

    public Optional<List<ExchangeRates>> findAll() {
        return exchangeRatesRepository.findAll();
    }

    public Optional<ExchangeRates> change(Long id, BigDecimal rate) {
        Optional<ExchangeRates> optionalExchangeRates = exchangeRatesRepository.findById(id);
        if (optionalExchangeRates.isEmpty()) {
            throw new NotFoundException("Не найден обменник с id " + id, HttpServletResponse.SC_BAD_REQUEST);
        }
        ExchangeRates exchangeRates = optionalExchangeRates.get();
        exchangeRates.setRate(rate);
        return exchangeRatesRepository.change(id, exchangeRates);
    }

    public Optional<ExchangeRates> change(String code, BigDecimal rate) {
        Optional<ExchangeRates> optionalExchangeRates = exchangeRatesRepository.findByCode(code);
        if (optionalExchangeRates.isEmpty()) {
            throw new NotFoundException("Не найден обменник с id " + code, HttpServletResponse.SC_BAD_REQUEST);
        }
        ExchangeRates exchangeRates = optionalExchangeRates.get();
        exchangeRates.setRate(rate);
        return exchangeRatesRepository.change(exchangeRates.getId(), exchangeRates);
    }

    public void delete(Long id) {
        exchangeRatesRepository.delete(id);
    }

    public Optional<ExchangeRates> addExchangeRates(ExchangeRatesDto dto) {
        //return repository.save(exchangeRates).get();
        Optional<Currency> baseCurrency = currencyRepository.findByCode(dto.getBaseCurrencyCode());
        Optional<Currency> targetCurrency = currencyRepository.findByCode(dto.getTargetCurrencyCode());
        if (baseCurrency.isEmpty()) {
            throw new BadRequestException("Не найдет code " + dto.getBaseCurrencyCode(),
                    HttpServletResponse.SC_BAD_REQUEST);
        }
        if (targetCurrency.isEmpty()) {
            throw new BadRequestException("Не найдет code " + dto.getTargetCurrencyCode(),
                    HttpServletResponse.SC_BAD_REQUEST);
        }
        ExchangeRates exchangeRates = new ExchangeRates();
        exchangeRates.setBaseCurrency(baseCurrency.get());
        exchangeRates.setTargetCurrency(targetCurrency.get());
        exchangeRates.setRate(dto.getRate());
        return exchangeRatesRepository.save(exchangeRates);
    }

    public ExchangeResponseDto exchange(ExchangeRequestDto dto) {
        Optional<ExchangeRates> exchangeRates = findByCode(dto.getBaseCurrencyCode() + dto.getTargetCurrencyCode());
        if (exchangeRates.isEmpty()) {
            throw new BadRequestException("Не найдет обменник валюты " + dto.getBaseCurrencyCode() + " => "
                    + dto.getTargetCurrencyCode(), HttpServletResponse.SC_BAD_REQUEST);
        }
        ExchangeResponseDto dtoResponse = new ExchangeResponseDto();
        dtoResponse.setBaseCurrency(exchangeRates.get().getBaseCurrency());
        dtoResponse.setTargetCurrency(exchangeRates.get().getTargetCurrency());
        dtoResponse.setRate(exchangeRates.get().getRate());
        dtoResponse.setAmount(dto.getAmount());
        dtoResponse.setConvertedAmount(exchangeRates.get().getRate().multiply(dto.getAmount()));
        return dtoResponse;
    }
}
