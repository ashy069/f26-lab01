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
 * Coordinates bookings and the waitlist. Enforces the core invariant: a room
 * never holds two confirmed bookings whose intervals overlap. Persistence is
 * delegated to a {@link BookingStore}.
 */
public class BookingService {
    private final BookingStore store;
    private int nextBookingSeq = 1;
    private int nextWaitlistSeq = 1;

    public BookingService(BookingStore store) {
        this.store = store;
    }

    /**
     * Attempts to book {@code room} for {@code user} over {@code interval}. If the
     * room is free over that interval the booking is confirmed; otherwise the user
     * is placed on the room's waitlist.
     */
    public BookingResult book(Room room, User user, TimeInterval interval) {
        for (Booking existing : store.bookingsForRoom(room)) {
            if (existing.interval().overlaps(interval)) {
                int position = store.waitlistForRoom(room).size() + 1;
                int seq = nextWaitlistSeq++;
                store.addWaitlistEntry(new WaitlistEntry("w" + seq, room, user, interval, seq));
                return new BookingResult.Waitlisted(position);
            }
        }
        Booking booking = new Booking("b" + nextBookingSeq++, room, user, interval);
        store.addBooking(booking);
        return new BookingResult.Confirmed(booking);
    }

    /** Returns the confirmed bookings for {@code room}. */
    public List<Booking> listBookings(Room room) {
        return store.bookingsForRoom(room);
    }

    /**
     * Cancels an existing booking.
     * If there is a waitlist entry for the same room AND same time interval,
     * promote the first matching waitlisted entry to a confirmed booking.
     * @param bookingId the id of booking to cancel
     */
    public void cancelBooking(String bookingId) {
        Optional<Booking> bookingOpt = store.findBooking(bookingId);
        // 找不到预约，直接结束
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking canceledBooking = bookingOpt.get();
        Room targetRoom = canceledBooking.room();
        TimeInterval targetInterval = canceledBooking.interval();

        // 1. 删除原预约
        store.deleteBooking(bookingId);

        // 2. 在该房间候补列表里，找到第一个相同时间段的候补
        List<WaitlistEntry> roomWaitlist = store.waitlistForRoom(targetRoom);
        for (WaitlistEntry entry : roomWaitlist) {
            if (entry.interval().equals(targetInterval)) {
                // 找到匹配的第一条候补
                store.removeWaitlistEntry(entry.id());
                Booking newConfirmedBooking = new Booking(
                        "b" + nextBookingSeq++,
                        targetRoom,
                        entry.user(),
                        targetIpackage edu.cmu.cs214.booking.service;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.domain.WaitlistEntry;
import edu.cmu.cs214.booking.repo.BookingStore;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates bookings and the waitlist. Enforces the core invariant: a room
 * never holds two confirmed bookings whose intervals overlap. Persistence is
 * delegated to a {@link BookingStore}.
 */
public class BookingService {
    private final BookingStore store;
    private int nextBookingSeq = 1;
    private int nextWaitlistSeq = 1;

    public BookingService(BookingStore store) {
        this.store = store;
    }

    /**
     * Attempts to book {@code room} for {@code user} over {@code interval}. If the
     * room is free over that interval the booking is confirmed; otherwise the user
     * is placed on the room's waitlist.
     */
    public BookingResult book(Room room, User user, TimeInterval interval) {
        for (Booking existing : store.bookingsForRoom(room)) {
            if (existing.interval().overlaps(interval)) {
                int position = store.waitlistForRoom(room).size() + 1;
                int seq = nextWaitlistSeq++;
                store.addWaitlistEntry(new WaitlistEntry("w" + seq, room, user, interval, seq));
                return new BookingResult.Waitlisted(position);
            }
        }
        Booking booking = new Booking("b" + nextBookingSeq++, room, user, interval);
        store.addBooking(booking);
        return new BookingResult.Confirmed(booking);
    }

    /** Returns the confirmed bookings for {@code room}. */
    public List<Booking> listBookings(Room room) {
        return store.bookingsForRoom(room);
    }

    /**
     * Cancels an existing booking.
     * If there is a waitlist entry for the same room AND same time interval,
     * promote the first matching waitlisted entry to a confirmed booking.
     * @param bookingId the id of booking to cancel
     */
    public void cancelBooking(String bookingId) {
        Optional<Booking> bookingOpt = store.findBooking(bookingId);
        // 找不到预约，直接结束
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking canceledBooking = bookingOpt.get();
        Room targetRoom = canceledBooking.room();
        TimeInterval targetInterval = canceledBooking.interval();

        // 1. 删除原预约
        store.deleteBooking(bookingId);

        // 2. 在该房间候补列表里，找到第一个【相同时间段】的候补
        List<WaitlistEntry> roomWaitlist = store.waitlistForRoom(targetRoom);
        for (WaitlistEntry entry : roomWaitlist) {
            if (entry.interval().equals(targetInterval)) {
                // 找到匹配的第一条候补
                store.removeWaitlistEntry(entry.id());
                Booking newConfirmedBooking = new Booking(
                        "b" + nextBookingSeq++,
                        targetRoom,
                        entry.user(),
                        targetInterval
                );
                store.addBooking(newConfirmedBooking);
                break; // 只提升第一个，提升完退出
            }
        }
    }
}
nterval
                );
                store.addBooking(newConfirmedBooking);
                break; // 只提升第一个，提升完退出
            }
        }
    }
}
