package br.com.dio.storefront.dto;

import java.util.UUID;

public record StockStatusMessage(UUID id, String status) {

    public boolean active(){
        return status.equals("AVAIlABLE");
    }
}
