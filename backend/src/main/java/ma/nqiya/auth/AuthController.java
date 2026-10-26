package ma.nqiya.auth;

import jakarta.validation.Valid;
import ma.nqiya.auth.AuthDtos.AuthResponse;
import ma.nqiya.auth.AuthDtos.RequestCodeRequest;
import ma.nqiya.auth.AuthDtos.RequestCodeResponse;
import ma.nqiya.auth.AuthDtos.VerifyCodeRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/request-code")
    public RequestCodeResponse requestCode(@Valid @RequestBody RequestCodeRequest request) {
        return authService.requestCode(request.email());
    }

    @PostMapping("/verify")
    public AuthResponse verify(@Valid @RequestBody VerifyCodeRequest request) {
        return authService.verify(request.email(), request.code());
    }
}
