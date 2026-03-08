import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private SpecialistSchedule schedule;

    private Integer status;

    @Column(name = "type_of_session")
    private Integer typeOfSession;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}