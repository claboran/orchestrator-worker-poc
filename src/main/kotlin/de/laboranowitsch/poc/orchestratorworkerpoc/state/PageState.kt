package de.laboranowitsch.poc.orchestratorworkerpoc.state

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "page_state")
class PageState(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PageStatus = PageStatus.CREATED,

    // store PageData as jsonb and map directly to the Kotlin data class using Hypersistence JsonType
    @Column(name = "data", columnDefinition = "jsonb")
    @Type(value = io.hypersistence.utils.hibernate.type.json.JsonType::class)
    var data: PageData? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,

    // Many pages belong to one job. EAGER from the many side, lazy is configured on JobState.pages
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_state_id", nullable = false)
    var jobState: JobState,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PageState
        // compare by id; if either id is null (shouldn't be) then fallback to identity
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
