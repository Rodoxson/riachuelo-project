package br.com.dio.storefront.service;

import br.com.dio.storefront.dto.ProductInfoDTO;
import br.com.dio.storefront.entity.ProductEntity;

import java.util.List;
import java.util.UUID;

public interface IProductService {

    ProductEntity save(final ProductEntity productEntity);
    void changeActivated(final UUID id, final boolean activate);
    List<ProductEntity> findAllActive();
    ProductInfoDTO findInfo(final UUID id);
    void purchase(final UUID id);
}
