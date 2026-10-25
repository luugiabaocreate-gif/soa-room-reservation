package vn.edu.authservice.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Tên đăng nhập hoặc mật khẩu không đúng");
    }
}
