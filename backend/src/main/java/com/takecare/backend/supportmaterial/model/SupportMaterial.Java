import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_materials")
public class SupportMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    private String title;

    private String description;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}