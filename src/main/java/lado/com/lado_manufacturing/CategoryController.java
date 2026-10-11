package lado.com.lado_manufacturing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Every method uses the logged-in user's company id, so one company can never see another's data. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    public record CategoryRequest(String name, String description) {}
    public record CategoryDto(Long id, String name, String description) {}

    private final CategoryRepository repo;

    public CategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    private static CategoryDto toDto(Category c) {
        return new CategoryDto(c.getCategoryId(), c.getCategoryName(), c.getDescription());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String validate(String name, String description) {
        if (name.isEmpty()) return "Name is required.";
        if (name.length() > 100) return "Name must be at most 100 characters.";
        if (description.length() > 500) return "Description must be at most 500 characters.";
        return null;
    }

    @GetMapping
    public List<CategoryDto> list(@AuthenticationPrincipal CompanyUserDetails user) {
        return repo.findByCompanyIdOrderByCategoryNameAsc(user.getCompanyId())
                .stream().map(CategoryController::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal CompanyUserDetails user,
                                    @RequestBody CategoryRequest req) {
        String name = clean(req.name());
        String description = clean(req.description());
        String problem = validate(name, description);
        if (problem != null) return error(HttpStatus.BAD_REQUEST, problem);

        if (repo.existsByCompanyIdAndCategoryNameIgnoreCase(user.getCompanyId(), name)) {
            return error(HttpStatus.CONFLICT, "A category named \"" + name + "\" already exists.");
        }
        Category c = new Category();
        c.setCompanyId(user.getCompanyId());
        c.setCategoryName(name);
        c.setDescription(description.isEmpty() ? null : description);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(repo.save(c)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal CompanyUserDetails user,
                                    @PathVariable Long id,
                                    @RequestBody CategoryRequest req) {
        Optional<Category> found = repo.findByCategoryIdAndCompanyId(id, user.getCompanyId());
        if (found.isEmpty()) return error(HttpStatus.NOT_FOUND, "Category not found.");

        String name = clean(req.name());
        String description = clean(req.description());
        String problem = validate(name, description);
        if (problem != null) return error(HttpStatus.BAD_REQUEST, problem);

        boolean duplicate = repo.findByCompanyIdAndCategoryNameIgnoreCase(user.getCompanyId(), name)
                .filter(other -> !other.getCategoryId().equals(id)).isPresent();
        if (duplicate) {
            return error(HttpStatus.CONFLICT, "A category named \"" + name + "\" already exists.");
        }
        Category c = found.get();
        c.setCategoryName(name);
        c.setDescription(description.isEmpty() ? null : description);
        return ResponseEntity.ok(toDto(repo.save(c)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal CompanyUserDetails user,
                                    @PathVariable Long id) {
        Optional<Category> found = repo.findByCategoryIdAndCompanyId(id, user.getCompanyId());
        if (found.isEmpty()) return error(HttpStatus.NOT_FOUND, "Category not found.");
        try {
            repo.delete(found.get());
        } catch (DataAccessException e) {
            return error(HttpStatus.CONFLICT,
                    "This category is used by products and cannot be deleted.");
        }
        return ResponseEntity.noContent().build();
    }
}
