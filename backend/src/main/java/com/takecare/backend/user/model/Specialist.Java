import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "specialists")
@PrimaryKeyJoinColumn(name = "id")
public class Specialist extends User {

    private String biography;

    @Column(name = "certification_img")
    private String certificationImg;

    @Column(name = "office_ubi")
    private String officeUbi;

    @Column(name = "session_cost")
    private BigDecimal sessionCost;

    @Column(name = "reputation_average")
    private BigDecimal reputationAverage;

    @ManyToMany
    @JoinTable(
            name = "specialist_specialties",
            joinColumns = @JoinColumn(name = "specialist_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private Set<Specialty> specialties;
}