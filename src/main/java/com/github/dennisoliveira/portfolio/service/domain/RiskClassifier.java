package com.github.dennisoliveira.portfolio.service.domain;

import com.github.dennisoliveira.portfolio.domain.Risk;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class RiskClassifier {

    public Risk classify(BigDecimal budget, LocalDate start, LocalDate expectedEnd) {
        long months = ChronoUnit.MONTHS.between(start, expectedEnd);
        boolean low  = budget.compareTo(new BigDecimal("100000")) <= 0 && months <= 3;
        boolean high = budget.compareTo(new BigDecimal("500000")) > 0 || months > 6;
        if (low)  return Risk.BAIXO;
        if (high) return Risk.ALTO;
        return Risk.MEDIO;
    }
}