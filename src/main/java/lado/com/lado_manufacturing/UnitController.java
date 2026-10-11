package lado.com.lado_manufacturing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    public record UnitRequest(String name, String symbol) {}
    public record UnitDto(Long id, String name, String symbol) {}

    private final UnitRepository repo;

    public UnitController(UnitRepository repo) {
        this.repo = repo;
    }

    private static UnitDto toDto(Unit u) {
        return new UnitDto(u.getUnitId(), u.getUnitName(), u.getUnitSymbol());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String validate(String name, String symbol) {
        if (name.isEmpty()) return "Name is required.";
        if (name.length() > 50) return "Name must be at most 50 characters.";
        if (symbol.length() > 20) return "Symbol must be at most 20 characters.";
        return null;
    }

    @GetMapping
    public List<UnitDto> list(@AuthenticationPrincipal CompanyUserDetails user) {
        return repo.findByCompanyIdOrderByUnitNameAsc(user.getCompanyId())
                .stream().map(UnitController::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal CompanyUserDetails user,
                                    @RequestBody UnitRequest req) {
        String name = clean(req.name());
        String symbol = clean(req.symbol());
        String problem = validate(name, symbol);
        if (problem != null) return error(HttpStatus.BAD_REQUEST, problem);

        if (repo.existsByCompanyIdAndUnitNameIgnoreCase(user.getCompanyId(), name)) {
            return error(HttpStatus.CONFLICT, "A unit named \"" + name + "\" already exists.");
        }
        Unit u = new Unit();
        u.setCompanyId(user.getCompanyId());
        u.setUnitName(name);
        u.setUnitSymbol(symbol.isEmpty() ? null : symbol);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(repo.save(u)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal CompanyUserDetails user,
                                    @PathVariable Long id,
                                    @RequestBody UnitRequest req) {
        Optional<Unit> found = repo.findByUnitIdAndCompanyId(id, user.getCompanyId());
        if (found.isEmpty()) return error(HttpStatus.NOT_FOUND, "Unit not found.");

        String name = clean(req.name());
        String symbol = clean(req.symbol());
        String problem = validate(name, symbol);
        if (problem != null) return error(HttpStatus.BAD_REQUEST, problem);

        boolean duplicate = repo.findByCompanyIdAndUnitNameIgnoreCase(user.getCompanyId(), name)
                .filter(other -> !other.getUnitId().equals(id)).isPresent();
        if (duplicate) {
            return error(HttpStatus.CONFLICT, "A unit named \"" + name + "\" already exists.");
        }
        Unit u = found.get();
        u.setUnitName(name);
        u.setUnitSymbol(symbol.isEmpty() ? null : symbol);
        return ResponseEntity.ok(toDto(repo.save(u)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal CompanyUserDetails user,
                                    @PathVariable Long id) {
        Optional<Unit> found = repo.findByUnitIdAndCompanyId(id, user.getCompanyId());
        if (found.isEmpty()) return error(HttpStatus.NOT_FOUND, "Unit not found.");
        try {
            repo.delete(found.get());
        } catch (DataAccessException e) {
            return error(HttpStatus.CONFLICT,
                    "This unit is used by products and cannot be deleted.");
        }
        return ResponseEntity.noContent().build();
    }
}
