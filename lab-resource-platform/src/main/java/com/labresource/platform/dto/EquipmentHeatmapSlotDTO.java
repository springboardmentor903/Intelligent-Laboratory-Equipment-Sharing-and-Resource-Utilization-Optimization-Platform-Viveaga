
    package com.labresource.platform.dto;

    import java.math.BigDecimal;

    public class EquipmentHeatmapSlotDTO {

        private Integer availableMinutes;
        private Integer usedMinutes;
        private BigDecimal utilizationPercentage;

        public EquipmentHeatmapSlotDTO(
                Integer availableMinutes,
                Integer usedMinutes,
                BigDecimal utilizationPercentage) {

            this.availableMinutes = availableMinutes;
            this.usedMinutes = usedMinutes;
            this.utilizationPercentage = utilizationPercentage;
        }

        public Integer getAvailableMinutes() {
            return availableMinutes;
        }

        public Integer getUsedMinutes() {
            return usedMinutes;
        }

        public BigDecimal getUtilizationPercentage() {
            return utilizationPercentage;
        }
    }
