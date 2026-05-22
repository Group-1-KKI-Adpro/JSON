package id.ac.ui.cs.advprog.kki.json.order.service;

import id.ac.ui.cs.advprog.kki.json.order.model.CartItem;
import id.ac.ui.cs.advprog.kki.json.order.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public CartItem addToCart(
            Long userId,
            Long catalogItemId,
            Integer quantity
    ) {

        CartItem item = new CartItem();

        item.setUserId(userId);
        item.setCatalogItemId(catalogItemId);
        item.setQuantity(quantity);

        return cartRepository.save(item);
    }

    public List<CartItem> getCart(Long userId) {
        return cartRepository.findByUserId(userId);
    }
}