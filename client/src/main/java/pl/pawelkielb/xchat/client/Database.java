package pl.pawelkielb.xchat.client;

import pl.pawelkielb.xchat.client.config.ChannelConfig;
import pl.pawelkielb.xchat.client.config.ClientConfig;
import pl.pawelkielb.xchat.client.exceptions.FileReadException;
import pl.pawelkielb.xchat.client.exceptions.FileWriteException;
import pl.pawelkielb.xchat.data.Name;
import pl.pawelkielb.xchat.utils.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;


/**
 * Allows saving and retrieving a client-related data.
 */
public class Database {
    public static final String clientConfigFileName = "xchat.properties";
    public static final String channelProperties = "channel.properties";
    public static final String cacheFileName = "cache.json";

    private final Path rootDirectory;

    public Database(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public static class InvalidConfigException extends RuntimeException {
        public InvalidConfigException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidConfigException(String message) {
            super(message);
        }
    }

    /**
     * @param path a path to the config
     * @return The channel config. Will be a null if the config was not found.
     * @throws InvalidConfigException if the config contains errors
     * @throws FileReadException      if there was an error while reading the file
     */
    public static ChannelConfig readChannelConfig(Path path) {
        Properties properties = readProperties(path);
        if (properties == null) {
            return null;
        }

        String channelIdString = properties.getProperty("id");

        if (channelIdString == null) {
            throw new InvalidConfigException("Channel id must be provided");
        }

        UUID channelId = UUID.fromString(channelIdString);

        return new ChannelConfig(channelId);
    }

    /**
     * @return A client config. Might be null if client config was not found in the database.
     * @throws InvalidConfigException if the config contains errors
     * @throws FileReadException      if there was an error while reading some file
     */
    public ClientConfig loadClientConfig() {
        Path path = rootDirectory.resolve(clientConfigFileName);
        Properties properties;
        properties = readProperties(path);
        if (properties == null) {
            return null;
        }
        Name username;
        try {
            username = Name.of(properties.getProperty("username"));
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigException("Invalid username", e);
        } catch (NullPointerException e) {
            throw new InvalidConfigException("Username must be provided");
        }
        var serverHost = properties.getProperty("server_host");

        if (serverHost == null) {
            throw new InvalidConfigException("Server host must be provided");
        }

        int serverPort;
        try {
            serverPort = Integer.parseInt(properties.getProperty("server_port"));
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("Invalid server port", e);
        }

        return new ClientConfig(username, serverHost, serverPort);
    }

    /**
     * Saves a client config. Will override if it's already saved.
     *
     * @param clientConfig a client config to save
     * @throws FileReadException  if there was an error while reading some file
     * @throws FileWriteException if there was an error while writing some file
     */
    public void saveClientConfig(ClientConfig clientConfig) {
        Properties properties = new Properties();
        properties.setProperty("username", clientConfig.username().value());
        properties.setProperty("server_host", clientConfig.serverHost());
        properties.setProperty("server_port", String.valueOf(clientConfig.serverPort()));
        Path path = rootDirectory.resolve(clientConfigFileName);

        writeProperties(path, properties);
    }

    /**
     * Saves a channel. Will override if it's already saved.
     *
     * @param name          a name of the channel
     * @param channelConfig a config of the channel
     * @throws FileReadException  if there was an error while reading some file
     * @throws FileWriteException if there was an error while writing some file
     */
    public void saveChannel(Name name, ChannelConfig channelConfig) {
        String directoryName = sanitizeAsPath(name.value());
        Path directoryPath;
        Path configPath;
        while (true) {
            directoryPath = rootDirectory.resolve(directoryName);
            configPath = directoryPath.resolve(channelProperties);
            try {
                Files.createDirectory(directoryPath);
                break;
            } catch (FileAlreadyExistsException e) {
                ChannelConfig existingConfig;
                try {
                    existingConfig = readChannelConfig(configPath);
                } catch (InvalidConfigException e1) {
                    continue;
                }
                if (existingConfig != null && existingConfig.id().equals(channelConfig.id())) {
                    break;
                }
                directoryName = StringUtils.increment(directoryName);
            } catch (IOException e) {
                throw new FileWriteException(directoryPath, e);
            }
        }

        Properties properties = new Properties();
        properties.setProperty("id", channelConfig.id().toString());

        writeProperties(configPath, properties);
    }

    public Cache loadCache() {
        var path = rootDirectory.resolve(cacheFileName);
        var cache = CacheKt.loadCache(path.toFile());
        if (cache == null) {
            cache = new Cache();
            saveCache(cache);
        }

        return cache;
    }

    public void saveCache(Cache cache) {
        var path = rootDirectory.resolve(cacheFileName);
        CacheKt.saveCache(path.toFile(), cache);
    }

    private static String sanitizeAsPath(String string) {
        return string.replaceAll("[^\\da-zA-z ]", "");
    }

    private static Properties readProperties(Path path) {
        Properties properties = new Properties();
        try (var clientPropertiesReader = Files.newBufferedReader(path)) {
            properties.load(clientPropertiesReader);
            return properties;
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new FileReadException(path, e);
        }
    }

    private static void writeProperties(Path path, Properties properties) {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, null);
        } catch (IOException e) {
            throw new FileWriteException(path, e);
        }
    }
}
