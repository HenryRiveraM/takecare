
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String names;

    @Column(name = "first_lastname")
    private String firstLastname;

    @Column(name = "second_lastname")
    private String secondLastname;

    @Column(name = "ci_document_img")
    private String ciDocumentImg;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    private Integer role;
    private Integer strikes;

    private Boolean status;

    @Column(name = "account_verified")
    private Integer accountVerified;
}