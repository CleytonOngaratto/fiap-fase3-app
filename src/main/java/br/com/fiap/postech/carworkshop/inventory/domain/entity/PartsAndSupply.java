package br.com.fiap.postech.carworkshop.inventory.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartsAndSupply {
    private Long id;
    private String code;
    private String manufacturer;
    private String description;
    private BigDecimal price;
    private TypeProductEnum type;
    private Integer quantity;

    public void consumeStock(int quantity) {
        if (this.quantity == null || this.quantity < quantity) {
            throw new br.com.fiap.postech.carworkshop.shared.domain.exception.StockException(
                    "Insufficient stock for part: " + this.description);
        }
        this.quantity -= quantity;
    }
}
