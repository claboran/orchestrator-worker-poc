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
class JobState(
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

    // Bi-directional one-to-many to PageState. Lazy on the one side (JobState.pages).
    @OneToMany(mappedBy = "jobState", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var pages: MutableList<PageState> = mutableListOf(),
) {

    // Convenience: add an existing PageState and keep both sides in sync
    fun addPage(page: PageState): PageState {
        page.jobState = this
        pages.add(page)
        return page
    }

    // Convenience: create a PageState from PageData and add it
    fun addPage(data: PageData): PageState {
        val p = PageState(data = data, jobState = this)
        pages.add(p)
        return p
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as JobState
        // If either id is null, they are not equal (transient)
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }
}
