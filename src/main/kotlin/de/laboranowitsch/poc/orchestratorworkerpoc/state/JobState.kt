package de.laboranowitsch.poc.orchestratorworkerpoc.state

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "job_state")
data class JobState(
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "job_id", nullable = false, unique = true)
    val jobId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.CREATED,

    // Stored as JSONB in Postgres. Keep as a JSON string in the entity.
    @Column(columnDefinition = "jsonb")
    @Type(value = io.hypersistence.utils.hibernate.type.json.JsonType::class)
    var payload: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
)
