@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_user_id")
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "reported_user_id")
    private User reported;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    private String reason;

    private Integer status;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
}