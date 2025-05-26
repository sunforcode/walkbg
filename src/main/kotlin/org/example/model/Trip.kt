package org.example.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "trips")
data class Trip(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    val startDate: Instant? = null,
    
    val endDate: Instant? = null,
    
    val status: Int = 0,
    
    val participantCount: Int = 0,
    
    val organizerId: String? = null,
    
    val budget: Double? = null,
    
    val actualCost: Double? = null,
    
    @Column(columnDefinition = "TEXT")
    val notes: String? = null,
    
    val privacySetting: String? = null,
    
    val coverUrl: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_route_id")
    var primaryRoute: Route? = null,
    
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val tripRoutes: MutableList<TripRoute> = mutableListOf(),
    
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val participants: MutableList<Participant> = mutableListOf(),
    
    @OneToOne(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    var equipmentList: EquipmentList? = null,
    
    @OneToOne(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    var mealPlan: MealPlan? = null,
    
    @OneToOne(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    var waterPlan: WaterPlan? = null,
    
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val itinerary: MutableList<TripItinerary> = mutableListOf(),
    
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    val images: MutableList<TripImage> = mutableListOf()
) {
    fun addRoute(route: Route, isPrimary: Boolean = false) {
        tripRoutes.add(TripRoute(trip = this, route = route, isPrimary = isPrimary))
        if (isPrimary) {
            primaryRoute = route
        }
    }
    
    fun addParticipant(participant: Participant) {
        participants.add(participant)
        participant.trip = this
    }
    
    fun addItineraryItem(itineraryItem: TripItinerary) {
        itinerary.add(itineraryItem)
        itineraryItem.trip = this
    }
    
    fun addImage(url: String, isCover: Boolean = false, sequenceNumber: Int = images.size + 1) {
        images.add(TripImage(trip = this, imageUrl = url, isCover = isCover, sequenceNumber = sequenceNumber))
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Trip
        
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "Trip(id='$id', name='$name')"
    }
}