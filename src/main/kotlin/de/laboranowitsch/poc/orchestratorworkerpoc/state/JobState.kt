package de.laboranowitsch.poc.orchestratorworkerpoc.state

import jakarta.persistence.*
import java.time.OffsetDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "job_state")
data class JobState(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "job_id", nullable = false, unique = true)
    val jobId: String = "",

    @Column(nullable = false)
    var status: String = "",

    // Stored as JSONB in Postgres. Keep as a JSON string in the entity.
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(value = io.hypersistence.utils.hibernate.type.json.JsonType::class)
    var payload: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
)

