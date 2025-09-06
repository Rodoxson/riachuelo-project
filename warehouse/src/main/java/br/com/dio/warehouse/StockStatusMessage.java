package br.com.dio.warehouse;

import java.util.UUID;

public record StockStatusMessage(UUID id, String status) {
    public boolean active(){
        return status.equals("AVAIlABLE");
    }
}
