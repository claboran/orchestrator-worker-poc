package de.laboranowitsch.poc.orchestratorworkerpoc.entity

import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageState
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "job_state")
class JobState(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.CREATED,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    var updatedAt: OffsetDateTime? = null,

    @OneToMany(mappedBy = "jobState", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var pages: MutableList<PageState> = mutableListOf(),
) {

    fun addPage(page: PageState): PageState {
        page.jobState = this
        pages.add(page)
        return page
    }

    fun addPage(data: PageData): PageState {
        val p = PageState(data = data, jobState = this)
        pages.add(p)
        return p
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as JobState
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }
}
