package br.com.dio.storefront.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
public class ProductEntity {
    @Id
    private UUID id;
    private String name;
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProductEntity that)) return false;
        return active == that.active && Objects.equals(id, that.id)
                                     && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, active);
    }
}
