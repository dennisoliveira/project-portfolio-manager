package com.github.dennisoliveira.portfolio.service.domain;

import com.github.dennisoliveira.portfolio.domain.Risk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RiskClassifierTest {

    private final RiskClassifier classifier = new RiskClassifier();

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    @DisplayName("BAIXO: orçamento ≤ 100.000 e prazo ≤ 3 meses")
    void lowRisk_budgetAtMost100k_andDurationUpTo3Months() {
        var start = d(2025, 1, 15);
        var endExactly3Months = start.plusMonths(3);
        var endSameMonth      = start;

        assertThat(classifier.classify(new BigDecimal("100000"), start, endExactly3Months))
                .isEqualTo(Risk.BAIXO);
        assertThat(classifier.classify(new BigDecimal("50000"), start, endSameMonth))
                .isEqualTo(Risk.BAIXO);
    }

    @Test
    @DisplayName("ALTO: orçamento > 500.000 (independente do prazo)")
    void highRisk_byBudgetOnly() {
        var start = d(2025, 1, 15);
        var end3m = start.plusMonths(3);

        assertThat(classifier.classify(new BigDecimal("500001"), start, end3m))
                .isEqualTo(Risk.ALTO);
        assertThat(classifier.classify(new BigDecimal("750000"), start, start))
                .isEqualTo(Risk.ALTO);
    }

    @Test
    @DisplayName("ALTO: prazo > 6 meses (independente do orçamento)")
    void highRisk_byTimeOnly() {
        var start = d(2025, 1, 15);
        var end7m = start.plusMonths(7);

        assertThat(classifier.classify(new BigDecimal("100000"), start, end7m))
                .isEqualTo(Risk.ALTO);
        assertThat(classifier.classify(new BigDecimal("1"), start, end7m))
                .isEqualTo(Risk.ALTO);
    }

    @Test
    @DisplayName("MÉDIO: orçamento entre 100.001 e 500.000 com prazo ≤ 3 meses")
    void mediumRisk_byBudgetMiddleRange() {
        var start = d(2025, 1, 15);
        var end3m = start.plusMonths(3);

        assertThat(classifier.classify(new BigDecimal("100001"), start, end3m))
                .isEqualTo(Risk.MEDIO);
        assertThat(classifier.classify(new BigDecimal("500000"), start, end3m))
                .isEqualTo(Risk.MEDIO);
    }

    @Test
    @DisplayName("MÉDIO: prazo entre >3 e ≤6 meses quando não for ALTO/BAIXO")
    void mediumRisk_byTimeBetween3And6Months() {
        var start = d(2025, 1, 15);
        var end4m = start.plusMonths(4);
        var end6m = start.plusMonths(6);

        // orçamento baixo, mas prazo > 3 meses => não é BAIXO; também não é ALTO (≤6 meses) => MÉDIO
        assertThat(classifier.classify(new BigDecimal("100000"), start, end4m))
                .isEqualTo(Risk.MEDIO);

        // limite superior de tempo (6m) também entra em MÉDIO se orçamento não for > 500k
        assertThat(classifier.classify(new BigDecimal("100000"), start, end6m))
                .isEqualTo(Risk.MEDIO);
    }

    @Test
    @DisplayName("Bordas exatas: 100.000, 100.001, 500.000, 500.001 e 3/6 meses")
    void edges_budgetAndTimeBoundaries() {
        var start = d(2025, 1, 15);
        var end3m = start.plusMonths(3);
        var end6m = start.plusMonths(6);

        // 100.000 + 3m => BAIXO
        assertThat(classifier.classify(new BigDecimal("100000"), start, end3m))
                .isEqualTo(Risk.BAIXO);

        // 100.001 + 3m => MÉDIO (orçamento médio)
        assertThat(classifier.classify(new BigDecimal("100001"), start, end3m))
                .isEqualTo(Risk.MEDIO);

        // 500.000 + 6m => MÉDIO (não é >500k e não é >6m)
        assertThat(classifier.classify(new BigDecimal("500000"), start, end6m))
                .isEqualTo(Risk.MEDIO);

        // 500.001 + 3m => ALTO (orçamento alto)
        assertThat(classifier.classify(new BigDecimal("500001"), start, end3m))
                .isEqualTo(Risk.ALTO);
    }
}