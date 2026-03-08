@Entity
@Table(name = "patients")
@PrimaryKeyJoinColumn(name = "id")
public class Patient extends User {

    @Column(name = "ci_number")
    private String ciNumber;

    @Column(name = "selfie_verification")
    private String selfieVerification;

    @Column(name = "clinical_history")
    private String clinicalHistory;
}