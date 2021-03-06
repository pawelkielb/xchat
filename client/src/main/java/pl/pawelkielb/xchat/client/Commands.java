package pl.pawelkielb.xchat.client;

import pl.pawelkielb.xchat.Exceptions;
import pl.pawelkielb.xchat.Logger;
import pl.pawelkielb.xchat.Observable;
import pl.pawelkielb.xchat.client.config.ChannelConfig;
import pl.pawelkielb.xchat.client.config.ClientConfig;
import pl.pawelkielb.xchat.client.exceptions.DisconnectedException;
import pl.pawelkielb.xchat.client.exceptions.ExceptionHandler;
import pl.pawelkielb.xchat.client.exceptions.FileWriteException;
import pl.pawelkielb.xchat.client.exceptions.NetworkException;
import pl.pawelkielb.xchat.data.Message;
import pl.pawelkielb.xchat.data.Name;

import java.net.ProtocolException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static pl.pawelkielb.xchat.Exceptions.throwAsUnchecked;


public abstract class Commands {
    public static void execute(String command,
                               List<String> args,
                               ClientConfig clientConfig,
                               ChannelConfig channelConfig,
                               Console console,
                               Database database,
                               Observable<Void> applicationExitEvent) {

        if (command.equals("init")) {
            if (clientConfig != null) {
                ExceptionHandler.onAlreadyInitialized();
            }

            ClientConfig defaultClientConfig = ClientConfig.defaults();
            try {
                database.saveClientConfig(defaultClientConfig);
                return;
            } catch (FileWriteException e) {
                ExceptionHandler.onCannotWriteFile(e);
            }
        }

        // other commands require client config
        if (clientConfig == null) {
            ExceptionHandler.onCannotFindClientConfig();
            throw new AssertionError();
        }

        Path logsPath;
        if (channelConfig == null) {
            logsPath = Paths.get("logs.txt");
        } else {
            logsPath = Paths.get("..", "logs.txt");
        }

        Logger logger = new FileLogger(logsPath, applicationExitEvent);

        var httpClient = Ktor.createClient(logger);
        applicationExitEvent.subscribe(event -> httpClient.close());

        var api = new XChatApi(
                httpClient,
                clientConfig.serverHost() + ":" + clientConfig.serverPort(),
                clientConfig.username()
        );
        Client client = new Client(database, api, clientConfig);

        switch (command) {
            case "create" -> {
                if (args.size() == 0) {
                    ExceptionHandler.onMissingArgument("Please provide a channel name");
                }

                Name channelName;
                try {
                    channelName = Name.of(args.get(0));
                } catch (IllegalArgumentException e) {
                    ExceptionHandler.onIllegalArgument("Illegal name was provided", e);
                    return;
                }

                var members = args
                        .subList(1, args.size())
                        .stream()
                        .filter(Name::isValid)
                        .map(Name::of)
                        .collect(Collectors.toSet());

                doNetwork(() -> client.createGroupChannel(channelName, members));
            }

            case "priv" -> {
                if (args.size() == 0) {
                    ExceptionHandler.onIllegalArgument("Please provide recipient's username");
                }

                Name recipient;
                try {
                    recipient = Name.of(args.get(0));
                } catch (IllegalArgumentException e) {
                    ExceptionHandler.onIllegalArgument("Invalid recipient", e);
                    return;
                }
                doNetwork(() -> client.createPrivateChannel(recipient));
            }

            case "send" -> {
                if (args.size() < 1) {
                    ExceptionHandler.onMissingArgument("Please provide a message");
                    return;
                }

                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                String message = String.join(" ", args);
                doNetwork(() -> client.sendMessage(channelConfig.id(), message));
            }

            case "read" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                int messageCount = 100;
                if (args.size() > 0) {
                    try {
                        messageCount = Integer.parseInt(args.get(0));
                    } catch (NumberFormatException e) {
                        ExceptionHandler.onIllegalArgument("The first argument has to be an integer");
                    }
                }

                if (messageCount < 1) {
                    return;
                }

                final int messageCountFinal = messageCount;
                doNetwork(() -> client.readMessages(channelConfig.id(), messageCountFinal)
                        .forEach(message -> printMessage(console, message)));
            }

            case "sync" -> doNetwork(client::sync);

            case "sendfile" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                if (args.size() == 0) {
                    ExceptionHandler.onMissingArgument("Please provide a path");
                }

                Path path = Paths.get(args.get(0));
                ProgressBar progressBar = new ProgressBar(console);

                doNetwork(() -> client.sendFile(channelConfig.id(), path, progressBar::update), e -> {
                    if (e instanceof NoSuchFileException) {
                        ExceptionHandler.onIllegalArgument("No such file", e);
                    }

                    if (e instanceof Client.NotFileException) {
                        ExceptionHandler.onIllegalArgument("Cannot send a directory");
                    }

                    throwAsUnchecked(e);
                });

                console.updateLine("");
            }

            case "download" -> {
                if (channelConfig == null) {
                    ExceptionHandler.onCommandNotUsedInChannelDirectory();
                    return;
                }

                if (args.size() == 0) {
                    ExceptionHandler.onMissingArgument("Please provide a file name");
                }

                Name fileName = Name.of(args.get(0));
                ProgressBar progressBar = new ProgressBar(console);

                try {
                    doNetwork(() -> client.downloadFile(channelConfig.id(), fileName, Paths.get("."), progressBar::update));
                } catch (NoSuchElementException e) {
                    ExceptionHandler.onIllegalArgument("No such file", e);
                }

                console.updateLine("");
            }

            default -> ExceptionHandler.onUnknownCommand(command);
        }
    }

    private static void printMessage(Console console, Message message) {
        console.println(String.format("%s: %s", message.getAuthor(), message.getContent()));
    }

    private static void doNetwork(Exceptions.Runnable_WithExceptions<Exception> runnable,
                                  Consumer<Exception> exceptionHandler) {

        try {
            runnable.run();
        } catch (ProtocolException e) {
            ExceptionHandler.onProtocolException(e);
        } catch (NetworkException e) {
            ExceptionHandler.onNetworkException(e);
        } catch (DisconnectedException e) {
            ExceptionHandler.onServerDisconnected(e);
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(e);
            } else {
                throwAsUnchecked(e);
            }
        }
    }

    private static void doNetwork(Exceptions.Runnable_WithExceptions<Exception> runnable) {
        doNetwork(runnable, null);
    }
}
