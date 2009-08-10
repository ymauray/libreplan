package org.navalplanner.business.advance.entities;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.navalplanner.business.common.BaseEntity;
import org.navalplanner.business.orders.entities.OrderElement;

public class AdvanceAssigment extends BaseEntity {

    private boolean reportGlobalAdvance;

    private OrderElement orderElement;

    private AdvanceType advanceType;

    private Set<AdvanceMeasurement> advanceMeasurements;

    public AdvanceAssigment(boolean reportGlobalAdvance, BigDecimal maxValue) {
        this.reportGlobalAdvance = reportGlobalAdvance;
    }

    public void setReportGlobalAdvance(boolean reportGlobalAdvance) {
        this.reportGlobalAdvance = reportGlobalAdvance;
    }

    public boolean getReportGlobalAdvance() {
        return this.reportGlobalAdvance;
    }

    public void setOrderElement(OrderElement orderElement) {
        this.orderElement = orderElement;
    }

    public OrderElement getOrderElement() {
        return this.orderElement;
    }

    public void setAdvanceType(AdvanceType advanceType) {
        this.advanceType = advanceType;
    }

    public AdvanceType getAdvanceType() {
        return this.advanceType;
    }

    public void setAdvanceMeasurements(Set<AdvanceMeasurement> advanceMeasurements) {
        this.advanceMeasurements = advanceMeasurements;
    }

    public Set<AdvanceMeasurement> getAdvanceMeasurements() {
        return new HashSet<AdvanceMeasurement>(this.advanceMeasurements);
    }
}
