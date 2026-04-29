package com.wpw.pim.repository.pricing;

import com.wpw.pim.domain.pricing.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    List<Currency> findByIsActiveTrueOrderByCode();
}
