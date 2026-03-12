import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "specialist_schedule")
public class SpecialistSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    @Column(name = "day_of_week", columnDefinition = "tinyint")
    private Integer dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(columnDefinition = "tinyint")
    private Integer status;
}