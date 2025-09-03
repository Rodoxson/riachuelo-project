package br.com.dio.storefront.service.impl;

import br.com.dio.storefront.dto.ProductInfoDTO;
import br.com.dio.storefront.dto.ProductsDetailsDTO;
import br.com.dio.storefront.entity.ProductEntity;
import br.com.dio.storefront.mapper.IProductMapper;
import br.com.dio.storefront.repository.ProductRepository;
import br.com.dio.storefront.service.IProductService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@AllArgsConstructor
@Service
public class ProductServiceImpl implements IProductService {

    private final ProductRepository repository;
    private final RestClient warehouseClient;
    private final IProductMapper mapper;

    @Override
    public ProductEntity save(ProductEntity productEntity) {
        return repository.save(productEntity);
    }

    @Override
    public void changeActivated(UUID id, boolean activate) {
        var entity = findProductById(id);
        entity.setActive(activate);
        repository.save(entity);

    }

    @Override
    public List<ProductEntity> findAllActive() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    public ProductInfoDTO findInfo(UUID id) {
        var entity = findProductById(id);
        var price = requestCurrentAmount(id);
        return mapper.toDTO(entity, price);
    }



    @Override
    public void purchase(UUID id) {
        purchaseWarehouse(id);

    }
    private ProductEntity findProductById(final UUID id) {
        return repository.findById(id).orElseThrow();
    }
    private BigDecimal requestCurrentAmount(UUID id) {
        var dto = warehouseClient.get()
                .uri("/products/"+ id)
                .retrieve()
                .body(ProductsDetailsDTO.class);
        return dto.price();
    }
    private void purchaseWarehouse(final UUID id) {

        var path = String.format("/products/%s/purchase", id);
        warehouseClient.post()
                .uri(path)
                .retrieve()
                .toBodilessEntity();

    }
}
