package es.ferchau.weather.infrastructure.aemet;

public class AemetGatewayException extends RuntimeException {
    public AemetGatewayException(String message) {
        super(message);
    }

    public AemetGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
