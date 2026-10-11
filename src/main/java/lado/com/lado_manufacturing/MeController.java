package lado.com.lado_manufacturing;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal CompanyUserDetails user) {
        return Map.of(
                "userId", user.getUserId(),
                "companyId", user.getCompanyId(),
                "companyName", user.getCompanyName(),
                "fullName", user.getFullName(),
                "username", user.getUsername(),
                "role", user.getRoleName());
    }
}
