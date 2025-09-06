package br.com.dio.warehouse;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("products")
public class ProductController {

    private final AvailabilityPublisher publisher;

    @Autowired
    public ProductController(final AvailabilityPublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping("{id}")
    public ProductsDetailsDTO findById(@PathVariable final UUID id) {
        // Implementação simples para integração: devolve um produto "fake" com preço determinístico
        String name = "Product " + id.toString().substring(0, 8);
        BigDecimal price = priceFor(id);
        return new ProductsDetailsDTO(id, name, price);
    }

    @PostMapping("{id}/purchase")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purchase(@PathVariable final UUID id) {
        // Publica evento de indisponibilidade após a compra
        publisher.publishUnavailable(id);
    }

    private BigDecimal priceFor(UUID id) {
        // Gera um preço determinístico simples baseado no UUID, apenas para fins de integração
        long lsb = Math.abs(id.getLeastSignificantBits());
        long base = (lsb % 9000L) + 1000L; // 10.00 a 100.00 (em centavos multiplicado por 100)
        return BigDecimal.valueOf(base).movePointLeft(2);
    }
}

@Service
class AvailabilityPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String queueName;

    @Autowired
    public AvailabilityPublisher(final RabbitTemplate rabbitTemplate,
                                 @Value("${spring.rabbitmq.queue.product-change-availability}") final String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    public void publishAvailable(final UUID id) {
        rabbitTemplate.convertAndSend(queueName, new StockStatusMessage(id, "AVAIlABLE"));
    }

    public void publishUnavailable(final UUID id) {
        rabbitTemplate.convertAndSend(queueName, new StockStatusMessage(id, "UNAVAILABLE"));
    }
}
