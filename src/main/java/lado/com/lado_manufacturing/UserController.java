package lado.com.lado_manufacturing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record UserDto(Long id, String username, String fullName, String email,
                          Long roleId, String role, String status) {}
    public record UserRequest(String username, String fullName, String email,
                              String password, Long roleId, String status) {}
    public record RoleDto(Long id, String name) {}

    private boolean isAdmin(CompanyUserDetails actor) {
        return actor != null && "ADMIN".equalsIgnoreCase(actor.getRoleName());
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Only an administrator can manage users."));
    }

    private UserDto toDto(AppUser user) {
        String roleName = roleRepository.findById(user.getRoleId())
                .map(Role::getRoleName).orElse("UNKNOWN");
        return new UserDto(user.getUserId(), user.getUsername(), user.getFullName(),
                user.getEmail(), user.getRoleId(), roleName,
                "Y".equalsIgnoreCase(user.getIsActive()) ? "Active" : "Inactive");
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal CompanyUserDetails actor) {
        if (!isAdmin(actor)) return forbidden();
        List<UserDto> users = userRepository.findAll().stream()
                .filter(u -> actor.getCompanyId().equals(u.getCompanyId()))
                .map(this::toDto).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> roles(@AuthenticationPrincipal CompanyUserDetails actor) {
        if (!isAdmin(actor)) return forbidden();
        List<RoleDto> roles = roleRepository.findAll().stream()
                .filter(r -> actor.getCompanyId().equals(r.getCompanyId()))
                .map(r -> new RoleDto(r.getRoleId(), r.getRoleName())).toList();
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal CompanyUserDetails actor,
                                    @RequestBody UserRequest request) {
        if (!isAdmin(actor)) return forbidden();
        if (request.username() == null || request.username().isBlank()
                || request.fullName() == null || request.fullName().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.roleId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Username, full name, password and role are required."));
        }
        String username = request.username().trim();
        if (userRepository.findByCompanyIdAndUsername(actor.getCompanyId(), username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "That username already exists for this company."));
        }
        Optional<Role> role = roleRepository.findById(request.roleId())
                .filter(r -> actor.getCompanyId().equals(r.getCompanyId()));
        if (role.isEmpty()) return ResponseEntity.badRequest()
                .body(Map.of("message", "Choose a valid role for this company."));

        AppUser user = new AppUser();
        user.setCompanyId(actor.getCompanyId());
        user.setUsername(username);
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email() == null ? null : request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoleId(role.get().getRoleId());
        user.setIsActive("Inactive".equalsIgnoreCase(request.status()) ? "N" : "Y");
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(userRepository.save(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal CompanyUserDetails actor,
                                    @PathVariable Long id, @RequestBody UserRequest request) {
        if (!isAdmin(actor)) return forbidden();
        Optional<AppUser> found = userRepository.findById(id)
                .filter(u -> actor.getCompanyId().equals(u.getCompanyId()));
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        AppUser user = found.get();

        if (request.fullName() == null || request.fullName().isBlank() || request.roleId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Full name and role are required."));
        }
        Optional<Role> role = roleRepository.findById(request.roleId())
                .filter(r -> actor.getCompanyId().equals(r.getCompanyId()));
        if (role.isEmpty()) return ResponseEntity.badRequest()
                .body(Map.of("message", "Choose a valid role for this company."));

        user.setFullName(request.fullName().trim());
        user.setEmail(request.email() == null ? null : request.email().trim());
        user.setRoleId(role.get().getRoleId());
        user.setIsActive("Inactive".equalsIgnoreCase(request.status()) ? "N" : "Y");
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return ResponseEntity.ok(toDto(userRepository.save(user)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@AuthenticationPrincipal CompanyUserDetails actor,
                                          @PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!isAdmin(actor)) return forbidden();
        Optional<AppUser> found = userRepository.findById(id)
                .filter(u -> actor.getCompanyId().equals(u.getCompanyId()));
        if (found.isEmpty()) return ResponseEntity.notFound().build();
        AppUser user = found.get();
        String status = body.getOrDefault("status", "");
        if (!"Active".equalsIgnoreCase(status) && !"Inactive".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status must be Active or Inactive."));
        }
        if (actor.getUserId().equals(user.getUserId()) && "Inactive".equalsIgnoreCase(status)) {
            return ResponseEntity.badRequest().body(Map.of("message", "You cannot deactivate your own account."));
        }
        user.setIsActive("Active".equalsIgnoreCase(status) ? "Y" : "N");
        return ResponseEntity.ok(toDto(userRepository.save(user)));
    }
}
