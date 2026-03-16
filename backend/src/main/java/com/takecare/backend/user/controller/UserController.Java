@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegisterDTO dto){
        userService.registerPatient(dto);
        return ResponseEntity.ok("Patient registered");
    }

    @PostMapping("/register/specialist")
    public ResponseEntity<?> registerSpecialist(@Valid @RequestBody SpecialistRegisterDTO dto){
        userService.registerSpecialist(dto);
        return ResponseEntity.ok("Specialist registered");
    }

}