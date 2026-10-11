package lado.com.lado_manufacturing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public ResponseEntity<?> getInventory(
            @AuthenticationPrincipal CompanyUserDetails user) {

        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please log in to view inventory."));
        }

        try {
            return ResponseEntity.ok(
                    inventoryRepository.findAllInventory(user.getCompanyId()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to load inventory."));
        }
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Map<String, Object>> adjustStock(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CompanyUserDetails user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getCompanyId() == null) {
            response.put("success", false);
            response.put("message", "Please log in to adjust stock.");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        try {
            Object productIdValue = request.get("productId");
            Object typeValue = request.get("type");
            Object quantityValue = request.get("quantity");
            Object reasonValue = request.get("reason");
            Object notesValue = request.get("notes");

            if (productIdValue == null
                    || typeValue == null
                    || quantityValue == null
                    || reasonValue == null) {

                response.put("success", false);
                response.put("message",
                        "Product, adjustment type, quantity, and reason are required.");

                return ResponseEntity.badRequest().body(response);
            }

            Long productId = Long.valueOf(productIdValue.toString());
            String type = typeValue.toString().trim().toUpperCase();
            BigDecimal quantity =
                    new BigDecimal(quantityValue.toString());
            String reason = reasonValue.toString().trim();
            String notes = notesValue == null
                    ? ""
                    : notesValue.toString().trim();

            if (productId <= 0) {
                response.put("success", false);
                response.put("message", "Invalid product ID.");

                return ResponseEntity.badRequest().body(response);
            }

            if (!type.equals("IN") && !type.equals("OUT")) {
                response.put("success", false);
                response.put("message",
                        "Adjustment type must be IN or OUT.");

                return ResponseEntity.badRequest().body(response);
            }

            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                response.put("success", false);
                response.put("message",
                        "Quantity must be greater than zero.");

                return ResponseEntity.badRequest().body(response);
            }

            if (reason.isBlank()) {
                response.put("success", false);
                response.put("message", "Please provide a reason.");

                return ResponseEntity.badRequest().body(response);
            }

            Long companyId = user.getCompanyId();

            // Verify that this product belongs to the logged-in company.
            if (!inventoryRepository.productExistsForCompany(
                    companyId, productId)) {

                response.put("success", false);
                response.put("message",
                        "Product not found for your company.");

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }

            BigDecimal delta = type.equals("IN")
                    ? quantity
                    : quantity.negate();

            String transactionNotes = "Reason: " + reason;

            if (!notes.isBlank()) {
                transactionNotes += " | Notes: " + notes;
            }

            inventoryRepository.applyStockAdjustment(
                    companyId,
                    productId,
                    delta,
                    "ADJUSTMENT",
                    transactionNotes
            );

            response.put("success", true);
            response.put("message",
                    "Stock adjustment completed successfully.");

            return ResponseEntity.ok(response);

        } catch (NumberFormatException exception) {
            response.put("success", false);
            response.put("message",
                    "Product ID and quantity must be valid numbers.");

            return ResponseEntity.badRequest().body(response);

        } catch (Exception exception) {
            exception.printStackTrace();

            response.put("success", false);
            response.put("message",
                    "Stock adjustment failed. Check the server logs for details.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
