package com.eventticketing.event.servlet;

import com.eventticketing.shared.model.Event;
import com.eventticketing.shared.model.TicketType;
import com.eventticketing.shared.model.Seat;
import com.eventticketing.event.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {
    private static final Logger logger = LoggerFactory.getLogger(EventResource.class);
    private final EventService eventService;

    public EventResource() {
        this.eventService = new EventService();
    }

    @GET
    public Response getAllEvents(
            @QueryParam("category") String category,
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        try {
            logger.info("Getting all events - category: {}, status: {}, page: {}, size: {}", 
                       category, status, page, size);
            
            List<Event> events = eventService.getAllEvents(category, status, page, size);
            return Response.ok(events).build();
        } catch (Exception e) {
            logger.error("Error getting events", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve events\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getEventById(@PathParam("id") Long id) {
        try {
            logger.info("Getting event by ID: {}", id);
            
            Event event = eventService.getEventById(id);
            if (event == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Event not found\"}")
                        .build();
            }
            
            return Response.ok(event).build();
        } catch (Exception e) {
            logger.error("Error getting event by ID: {}", id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve event\"}")
                    .build();
        }
    }

    @POST
    public Response createEvent(Event event) {
        try {
            logger.info("Creating new event: {}", event.getTitle());
            
            Event createdEvent = eventService.createEvent(event);
            return Response.status(Response.Status.CREATED)
                    .entity(createdEvent)
                    .build();
        } catch (Exception e) {
            logger.error("Error creating event", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create event\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateEvent(@PathParam("id") Long id, Event event) {
        try {
            logger.info("Updating event ID: {}", id);
            
            event.setId(id);
            Event updatedEvent = eventService.updateEvent(event);
            if (updatedEvent == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Event not found\"}")
                        .build();
            }
            
            return Response.ok(updatedEvent).build();
        } catch (Exception e) {
            logger.error("Error updating event ID: {}", id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to update event\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEvent(@PathParam("id") Long id) {
        try {
            logger.info("Deleting event ID: {}", id);
            
            boolean deleted = eventService.deleteEvent(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Event not found\"}")
                        .build();
            }
            
            return Response.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting event ID: {}", id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to delete event\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}/ticket-types")
    public Response getTicketTypesByEventId(@PathParam("id") Long eventId) {
        try {
            logger.info("Getting ticket types for event ID: {}", eventId);
            
            List<TicketType> ticketTypes = eventService.getTicketTypesByEventId(eventId);
            return Response.ok(ticketTypes).build();
        } catch (Exception e) {
            logger.error("Error getting ticket types for event ID: {}", eventId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve ticket types\"}")
                    .build();
        }
    }

    @POST
    @Path("/{id}/ticket-types")
    public Response createTicketType(@PathParam("id") Long eventId, TicketType ticketType) {
        try {
            logger.info("Creating ticket type for event ID: {}", eventId);
            
            ticketType.setEventId(eventId);
            TicketType createdTicketType = eventService.createTicketType(ticketType);
            return Response.status(Response.Status.CREATED)
                    .entity(createdTicketType)
                    .build();
        } catch (Exception e) {
            logger.error("Error creating ticket type for event ID: {}", eventId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create ticket type\"}")
                    .build();
        }
    }

    @GET
    @Path("/{id}/seats")
    public Response getAvailableSeats(@PathParam("id") Long eventId) {
        try {
            logger.info("Getting available seats for event ID: {}", eventId);
            
            List<Seat> seats = eventService.getAvailableSeats(eventId);
            return Response.ok(seats).build();
        } catch (Exception e) {
            logger.error("Error getting available seats for event ID: {}", eventId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to retrieve available seats\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}/seats/{seatId}/reserve")
    public Response reserveSeat(@PathParam("id") Long eventId, @PathParam("seatId") Long seatId) {
        try {
            logger.info("Reserving seat {} for event ID: {}", seatId, eventId);
            
            boolean reserved = eventService.reserveSeat(eventId, seatId);
            if (!reserved) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Seat not available or already reserved\"}")
                        .build();
            }
            
            return Response.ok("{\"message\": \"Seat reserved successfully\"}").build();
        } catch (Exception e) {
            logger.error("Error reserving seat {} for event ID: {}", seatId, eventId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to reserve seat\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}/seats/{seatId}/release")
    public Response releaseSeat(@PathParam("id") Long eventId, @PathParam("seatId") Long seatId) {
        try {
            logger.info("Releasing seat {} for event ID: {}", seatId, eventId);
            
            boolean released = eventService.releaseSeat(eventId, seatId);
            if (!released) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Seat not found or not reserved\"}")
                        .build();
            }
            
            return Response.ok("{\"message\": \"Seat released successfully\"}").build();
        } catch (Exception e) {
            logger.error("Error releasing seat {} for event ID: {}", seatId, eventId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to release seat\"}")
                    .build();
        }
    }

    @PATCH
    @Path("/{id}/capacity")
    public Response updateEventCapacity(@PathParam("id") Long eventId, Map<String, Object> requestData) {
        try {
            logger.info("Updating capacity for event ID: {}", eventId);
            
            Integer bookedSeats = (Integer) requestData.get("bookedSeats");
            if (bookedSeats == null || bookedSeats <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Invalid booked seats count\"}")
                        .build();
            }
            
            boolean updated = eventService.updateEventCapacity(eventId, bookedSeats);
            if (!updated) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Event not found\"}")
                        .build();
            }
            
            return Response.ok("{\"message\": \"Event capacity updated successfully\"}").build();
        } catch (Exception e) {
            logger.error("Error updating capacity for event ID: {}", eventId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to update event capacity\"}")
                    .build();
        }
    }

    @GET
    @Path("/search")
    public Response searchEvents(
            @QueryParam("q") String query,
            @QueryParam("category") String category,
            @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        try {
            logger.info("Searching events - query: {}, category: {}, dateFrom: {}, dateTo: {}", 
                       query, category, dateFrom, dateTo);
            
            List<Event> events = eventService.searchEvents(query, category, dateFrom, dateTo, page, size);
            return Response.ok(events).build();
        } catch (Exception e) {
            logger.error("Error searching events", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to search events\"}")
                    .build();
        }
    }
}
