package pl.pawelkielb.xchat.client.config;

import pl.pawelkielb.xchat.data.Name;

import static java.util.Objects.requireNonNull;

public record ClientConfig(Name username, String serverHost, int serverPort) {
    public ClientConfig {
        requireNonNull(username);
        requireNonNull(serverHost);
        if (serverPort < 0) {
            throw new IllegalArgumentException("The server port must be a positive integer");
        }
    }

    public static ClientConfig defaults() {
        return new ClientConfig(Name.of("Guest"), "http://localhost", 8080);
    }
}
