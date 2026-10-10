package edu.cmu.cs214.booking.service;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.domain.WaitlistEntry;
import edu.cmu.cs214.booking.repo.BookingStore;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates booking and waitlist logic.
 */
public class BookingService {
    private final BookingStore store;
    private int nextBookingId;
    private int nextWaitlistSeq;

    public BookingService(BookingStore store) {
        this.store = store;
        this.nextBookingId = 1;
        this.nextWaitlistSeq = 1;
    }

    /**
     * Attempt to book a room for a user.
     * If the interval overlaps existing confirmed bookings for that room, add to waitlist.
     */
    public BookingResult book(Room room, User user, TimeInterval interval) {
        List<Booking> roomBookings = store.bookingsForRoom(room);
        boolean overlaps = roomBookings.stream()
                .anyMatch(b -> b.interval().overlaps(interval));

        if (!overlaps) {
            Booking booking = new Booking(Integer.toString(nextBookingId++), room, user, interval);
            store.addBooking(booking);
            return new BookingResult.Confirmed(booking);
        } else {
            int position = nextWaitlistSeq;
            WaitlistEntry entry = new WaitlistEntry(Integer.toString(nextWaitlistSeq++), room, user, interval, position);
            store.addWaitlistEntry(entry);
            return new BookingResult.Waitlisted(position);
        }
    }

    /**
     * Get confirmed bookings for a room.
     */
    public List<Booking> listBookings(Room room) {
        return store.bookingsForRoom(room);
    }

    /**
     * Cancel a booking by bookingId.
     * Remove the booking. Then scan waitlist (in seq order) and promote the FIRST waitlist entry
     * whose interval does not overlap any remaining confirmed bookings of this room.
     * Promote at most one entry.
     */
    public void cancelBooking(String bookingId) {
        Optional<Booking> bookingOpt = store.findBooking(bookingId);
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking canceledBooking = bookingOpt.get();
        Room targetRoom = canceledBooking.room();

        // Step1: remove the canceled booking from store
        store.removeBooking(bookingId);

        // Step2: get remaining confirmed bookings after deletion
        List<Booking> remainingBookings = store.bookingsForRoom(targetRoom);
        List<WaitlistEntry> waitlist = store.waitlistForRoom(targetRoom);

        // Step3: iterate waitlist in seq order, find first promotable entry
        for (WaitlistEntry entry : waitlist) {
            TimeInterval candidateInterval = entry.interval();
            boolean noOverlap = true;
            for (Booking b : remainingBookings) {
                if (b.interval().overlaps(candidateInterval)) {
                    noOverlap = false;
                    break;
                }
            }
            if (noOverlap) {
                // promote this entry
                store.removeWaitlistEntry(entry.id());
                Booking newBooking = new Booking(
                        Integer.toString(nextBookingId++),
                        targetRoom,
                        entry.user(),
                        candidateInterval
                );
                store.addBooking(newBooking);
                break; // promote at most one
            }
        }
    }
}
