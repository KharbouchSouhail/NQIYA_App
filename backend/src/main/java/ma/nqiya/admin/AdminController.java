package ma.nqiya.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.nqiya.admin.AdminService.ActionLog;
import ma.nqiya.admin.AdminService.AdminSpot;
import ma.nqiya.admin.AdminService.AdminUser;
import ma.nqiya.admin.AdminService.Stats;
import ma.nqiya.common.web.PageResponse;
import ma.nqiya.security.CurrentUser;
import ma.nqiya.user.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Restricted to ROLE_ADMIN by the security filter chain. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final CurrentUser currentUser;

    public AdminController(AdminService adminService, CurrentUser currentUser) {
        this.adminService = adminService;
        this.currentUser = currentUser;
    }

    public record ReasonRequest(@Size(max = 200) String reason) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    @GetMapping("/stats")
    public Stats stats() {
        return adminService.stats();
    }

    @GetMapping("/spots")
    public PageResponse<AdminSpot> spots(@RequestParam(defaultValue = "0") int page) {
        return adminService.spots(page);
    }

    @PostMapping("/spots/{id}/close")
    public ResponseEntity<Void> close(@PathVariable Long id, @RequestBody(required = false) ReasonRequest body) {
        adminService.setSpotClosed(currentUser.id(), id, true, body == null ? null : body.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/spots/{id}/reopen")
    public ResponseEntity<Void> reopen(@PathVariable Long id, @RequestBody(required = false) ReasonRequest body) {
        adminService.setSpotClosed(currentUser.id(), id, false, body == null ? null : body.reason());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/spots/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminService.deleteSpot(currentUser.id(), id, reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public PageResponse<AdminUser> users(@RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page) {
        return adminService.users(q, page);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> setRole(@PathVariable Long id, @RequestBody @jakarta.validation.Valid RoleRequest body) {
        adminService.setRole(currentUser.id(), id, body.role());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/log")
    public PageResponse<ActionLog> log(@RequestParam(defaultValue = "0") int page) {
        return adminService.log(page);
    }
}
