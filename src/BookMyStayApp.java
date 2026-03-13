import java.util.*;

class Reservation {
    private String guestName;
    private String roomType;

    public Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getRoomType() {
        return roomType;
    }
}

class BookingRequestQueue {

    private Queue<Reservation> requestQueue = new LinkedList<>();

    public synchronized void addRequest(Reservation reservation) {
        requestQueue.offer(reservation);
    }

    public synchronized Reservation getNextRequest() {
        return requestQueue.poll();
    }

    public synchronized boolean hasPendingRequests() {
        return !requestQueue.isEmpty();
    }
}

class RoomInventory {

    private Map<String, Integer> roomAvailability = new HashMap<>();

    public RoomInventory() {
        roomAvailability.put("Single", 5);
        roomAvailability.put("Double", 3);
        roomAvailability.put("Suite", 2);
    }

    public synchronized Map<String, Integer> getRoomAvailability() {
        return roomAvailability;
    }

    public synchronized void updateAvailability(String roomType, int count) {
        roomAvailability.put(roomType, count);
    }
}

class RoomAllocationService {

    private Map<String, Integer> roomCounters = new HashMap<>();

    public synchronized void allocateRoom(Reservation reservation, RoomInventory inventory) {

        String roomType = reservation.getRoomType();
        Map<String, Integer> availability = inventory.getRoomAvailability();

        if (availability.get(roomType) <= 0) {
            System.out.println("No rooms available for " + roomType);
            return;
        }

        int nextId = roomCounters.getOrDefault(roomType, 0) + 1;
        roomCounters.put(roomType, nextId);

        String roomId = roomType + "-" + nextId;

        inventory.updateAvailability(roomType, availability.get(roomType) - 1);

        System.out.println("Booking confirmed for Guest: "
                + reservation.getGuestName()
                + ", Room ID: " + roomId);
    }
}

class ConcurrentBookingProcessor implements Runnable {

    private BookingRequestQueue bookingQueue;
    private RoomInventory inventory;
    private RoomAllocationService allocationService;

    public ConcurrentBookingProcessor(
            BookingRequestQueue bookingQueue,
            RoomInventory inventory,
            RoomAllocationService allocationService) {

        this.bookingQueue = bookingQueue;
        this.inventory = inventory;
        this.allocationService = allocationService;
    }

    @Override
    public void run() {

        while (bookingQueue.hasPendingRequests()) {

            Reservation reservation;

            synchronized (bookingQueue) {
                reservation = bookingQueue.getNextRequest();
            }

            if (reservation != null) {
                allocationService.allocateRoom(reservation, inventory);
            }
        }
    }
}

public class BookMyStayApp {

    public static void main(String[] args) {

        System.out.println("Concurrent Booking Simulation");

        RoomInventory inventory = new RoomInventory();
        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        RoomAllocationService allocationService = new RoomAllocationService();

        bookingQueue.addRequest(new Reservation("Abhi", "Single"));
        bookingQueue.addRequest(new Reservation("Vanmathi", "Double"));
        bookingQueue.addRequest(new Reservation("Kural", "Suite"));
        bookingQueue.addRequest(new Reservation("Subha", "Single"));

        Thread t1 = new Thread(
                new ConcurrentBookingProcessor(
                        bookingQueue, inventory, allocationService
                )
        );

        Thread t2 = new Thread(
                new ConcurrentBookingProcessor(
                        bookingQueue, inventory, allocationService
                )
        );

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Thread execution interrupted.");
        }

        System.out.println("\nRemaining Inventory:");

        Map<String, Integer> availability = inventory.getRoomAvailability();

        System.out.println("Single: " + availability.get("Single"));
        System.out.println("Double: " + availability.get("Double"));
        System.out.println("Suite: " + availability.get("Suite"));
    }
}