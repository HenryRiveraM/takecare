import jakarta.persistence.*;

@Entity
@Table(name = "specialties")
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToMany(mappedBy = "specialties")
    private Set<Specialist> specialists;
}