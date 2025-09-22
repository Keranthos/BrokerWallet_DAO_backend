package com.brokerwallet.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 勋章查询结果DTO
 */
@Data
@Builder
public class MedalQueryResult {
    private String address;
    private Medals medals;

    @Data
    public static class Medals {
        private int gold;
        private int silver;
        private int bronze;
        private int total;
        
        public Medals(int gold, int silver, int bronze, int total) {
            this.gold = gold;
            this.silver = silver;
            this.bronze = bronze;
            this.total = total;
        }
    }
}
