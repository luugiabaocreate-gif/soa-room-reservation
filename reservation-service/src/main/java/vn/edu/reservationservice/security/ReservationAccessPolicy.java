package vn.edu.reservationservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.reservationservice.reservation.ReservationRequest;

@Component
public class ReservationAccessPolicy {

    public ReservationRequest enforceCreateOwnership(ReservationRequest requested) {
        AuthenticatedUser user = currentUser();

        if (user.hasRole("ADMIN")) {
            return requested;
        }

        if (!user.hasRole("STUDENT") || user.requesterId() == null || user.requesterId().isBlank()) {
            throw new AccessDeniedException("Tài khoản này không được tạo đăng ký thay cho người dùng khác");
        }

        // Do not trust requesterId/requesterName sent by the browser for a STUDENT.
        return new ReservationRequest(
                requested.roomId(),
                user.requesterId(),
                user.displayName(),
                requested.purpose(),
                requested.startTime(),
                requested.endTime(),
                requested.attendeeCount()
        );
    }

    public String effectiveRequesterIdForRead(String requestedRequesterId) {
        AuthenticatedUser user = currentUser();
        if (user.hasRole("STUDENT")) {
            if (user.requesterId() == null || user.requesterId().isBlank()) {
                throw new AccessDeniedException("JWT của STUDENT không có requesterId hợp lệ");
            }
            return user.requesterId();
        }
        return requestedRequesterId;
    }

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Không tìm thấy danh tính đã xác thực");
        }
        return user;
    }
}
