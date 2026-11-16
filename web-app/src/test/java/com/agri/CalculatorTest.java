package com.agri;

import com.agri.service.Calculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    @Test
    void yieldPerAreaDividesQuantityByArea() {
        assertEquals(1801.08, Calculator.yieldPerArea(14408.6, 8.0), 0.001);
    }

    @Test
    void yieldPerAreaRejectsZeroArea() {
        assertThrows(IllegalArgumentException.class, () -> Calculator.yieldPerArea(100, 0));
    }

    @Test
    void revenueIsQuantityTimesPrice() {
        assertEquals(280967.70, Calculator.revenue(14408.6, 19.5), 0.001);
    }

    @Test
    void profitIsRevenueMinusCost() {
        assertEquals(268223.01, Calculator.profit(280967.70, 12744.69), 0.001);
    }

    @Test
    void profitCanBeNegative() {
        assertEquals(-500.0, Calculator.profit(1000.0, 1500.0), 0.001);
    }
}
