package Registration_Discovery;

import org.apache.zookeeper.KeeperException;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final int port;
    private ServerSocket serverSocket; // Declare ServerSocket as a class-level variable

    public OnElectionAction(ServiceRegistry serviceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    @Override
    public void onElectedToBeLeader() {
        try {
            String address = InetAddress.getLocalHost().getHostAddress() + ":" + port;
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.registerToClusterAsCoordinator(address);
            serviceRegistry.registerForUpdates();

            Coordinator coordinator = new Coordinator(serviceRegistry);

            // Start a thread to handle WebServer queries
            new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(port); // Open ServerSocket once
                    System.out.println("Coordinator is waiting for queries from WebServer on port " + port + "...");

                    while (true) {
                        Socket clientSocket = serverSocket.accept(); // Accept incoming connection
                        // Start a new thread to handle each request
                        new Thread(() -> handleWebServerRequest(clientSocket, coordinator)).start();
                    }
                } catch (IOException e) {
                    System.err.println("Error starting WebServer listener: " + e.getMessage());
                } finally {
                    closeServerSocket();
                }
            }).start();

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleWebServerRequest(Socket clientSocket, Coordinator coordinator) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            String query = (String) in.readObject(); // Read query from WebServer
            System.out.println("Query received from WebServer: " + query);

            // Process the query
            coordinator.start(query);

            // Optionally, send results back to Web Server
            // coordinator.sendResultsToWebServer(coordinator.getResults());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling WebServer query: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("ServerSocket closed successfully.");
            } catch (IOException e) {
                System.err.println("Failed to close the ServerSocket: " + e.getMessage());
            }
        }
    }

    @Override
    public void onWorker() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            String currentServerAddress = String.format("%s:%s", ipAddress, port);
            serviceRegistry.registerToCluster(currentServerAddress);
            String leaderAddress = serviceRegistry.getLeaderAddress();
            Worker worker = new Worker(port);
            worker.start();

            if (leaderAddress != null) {
                System.out.println("Leader Address: " + leaderAddress);
            } else {
                System.out.println("No leader found.");
            }
            System.out.println("I am a Worker. Monitoring for updates...");
        } catch (InterruptedException | KeeperException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
