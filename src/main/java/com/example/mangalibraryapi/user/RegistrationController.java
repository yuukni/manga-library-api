package  com.example.mangalibraryapi.user;

import com.example.mangalibraryapi.user.dto.RegistrationForm;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final UserService userService;

    // @Autowired
    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationForm registrationForm) {
        try {
            User registeredUser = userService.registerUser(registrationForm);

            // Console debug: prints the ID and username in the terminal
            System.out.println("DEBUG - Registered user ID: " + registeredUser.getId());
            System.out.println("DEBUG - Registered username: " + registeredUser.getUsername());

            // If the ID is null, we return a clear message
            if (registeredUser.getId() == null) {
                return ResponseEntity.ok("Registered! But the ID is NULL. Username: " + registeredUser.getUsername());
            }

            return ResponseEntity.ok("Successfully registered! ID: " + registeredUser.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}