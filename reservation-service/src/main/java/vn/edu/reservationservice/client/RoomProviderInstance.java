package vn.edu.reservationservice.client;

/**
 * Non-sensitive instance metadata returned by Room Service's existing public
 * information endpoint. It is used solely to demonstrate TH7 discovery and
 * client-side load-balancing; it carries no Room or Reservation business data.
 */
public record RoomProviderInstance(
        String service,
        String serviceId,
        String instanceId,
        String port,
        String status
) {
}
