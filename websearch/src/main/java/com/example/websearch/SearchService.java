package com.example.websearch;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SearchService {

    private ZooKeeper zooKeeper;
    private String leaderAddress;
    private ServerSocket serverSocket; // ServerSocket مشترك

    public SearchService() {
        connectToZooKeeper();
        watchLeaderNode();
        startServerSocketListener(); // بدء تشغيل listener عند إنشاء الخدمة
    }

    private void connectToZooKeeper() {
        String zookeeperAddress = "192.168.184.129:2181";
        try {
            zooKeeper = new ZooKeeper(zookeeperAddress, 3000, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("Connected to ZooKeeper");
                }
            });
        } catch (Exception e) {
            System.err.println("Error connecting to ZooKeeper: " + e.getMessage());
        }
    }

    private void watchLeaderNode() {
        String leaderNodePath = "/coordinator/leader";
        try {
            Stat stat = zooKeeper.exists(leaderNodePath, this::leaderNodeWatcher);
            if (stat != null) {
                byte[] leaderData = zooKeeper.getData(
                        leaderNodePath, this::leaderNodeWatcher, null);
                leaderAddress = new String(leaderData);
                System.out.println("Leader address updated: " + leaderAddress);
            } else {
                System.out.println("Leader node does not exist.");
                Thread.sleep(1000);
            }
        } catch (KeeperException | InterruptedException e) {
            System.err.println("Error watching leader node: " + e.getMessage());
        }
    }

    private void leaderNodeWatcher(WatchedEvent event) {
        if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
            System.out.println("Leader node deleted, attempting to find new leader...");
            watchLeaderNode();
        } else if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
            System.out.println("Leader node data changed, updating leader address...");
            watchLeaderNode();
        }
    }

    private void startServerSocketListener() {
        try {
            serverSocket = new ServerSocket(8082); // فتح ServerSocket مرة واحدة
            System.out.println("Web Server is waiting for results from Coordinator...");

            // تشغيل خيط لمعالجة الاتصالات
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept(); // استقبال اتصال
                        // تشغيل خيط لكل طلب
                        new Thread(() -> handleClientRequest(clientSocket)).start();
                    } catch (IOException e) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Error starting ServerSocket: " + e.getMessage());
        }
    }

    private void handleClientRequest(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            String resultsString = (String) in.readObject();
            System.out.println("Results received from Coordinator: " + resultsString);

            Map<String, Double> results = convertStringToMap(resultsString);
            WebSearchResult.setResults(results);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling Coordinator results: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    public Map<String, Double> convertStringToMap(String responseString) {
        Map<String, Double> map = new LinkedHashMap<>();
        responseString = responseString.substring(1, responseString.length() - 1);
        String[] entries = responseString.split(", ");

        for (String entry : entries) {
            String[] keyValue = entry.split("=");
            String key = keyValue[0].trim();
            Double value = Double.parseDouble(keyValue[1].trim());
            map.put(key, value);
        }

        return map;
    }

    public SearchResponse sendQueryToLeader(String query) {
        if (leaderAddress == null) {
            return new SearchResponse(Map.of("Error", 0.0));
        }

        String[] addressParts = leaderAddress.split(":");
        String leaderIp = addressParts[0];
        int leaderPort = Integer.parseInt(addressParts[1]);

        try (Socket socket = new Socket(leaderIp, leaderPort)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Sending query to leader at: " + leaderIp + ":" + leaderPort);
            out.writeObject(query);

            return new SearchResponse(Map.of("Status", 1.0));
        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResponse(Map.of("Error", 0.0));
        }
    }
}


//    public SearchResponse sendQueryToLeader(String query) {
//        if (leaderAddress == null) {
//            return new SearchResponse(Map.of("Error", 0.0));
//        }
//
//        String[] addressParts = leaderAddress.split(":");
//        String leaderIp = addressParts[0];
//        int leaderPort = Integer.parseInt(addressParts[1]);
//
//        try (Socket socket = new Socket(leaderIp, leaderPort)) {
//            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//            System.out.println("Sending query to leader at: " + leaderIp + ":" + leaderPort);
//            out.writeObject(query);
//
//            new Thread(() -> {
//                try (ServerSocket serverSocket = new ServerSocket(8082)) {
//                    System.out.println("Web Server is waiting for results from Coordinator...");
//                    while (true) {
//                        try (Socket clientSocket = serverSocket.accept()) {
//                            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
//                            String resultsString = (String) in.readObject(); // استقبال النتائج
//                            System.out.println("Results received from Coordinator: " + resultsString);
//
//                            // معالجة النتائج (تحويل السلسلة النصية إلى Map إذا لزم الأمر)
//                            Map<String, Double> results = convertStringToMap(resultsString);
//                            System.out.println("Processed Results: " + results);
//                        } catch (IOException | ClassNotFoundException e) {
//                            System.err.println("Error handling Coordinator results: " + e.getMessage());
//                        }
//                    }
//                } catch (IOException e) {
//                    System.err.println("Error starting Coordinator listener: " + e.getMessage());
//                }
//            }).start();
//
//
////            System.out.println("+++++++");
////            Thread.sleep(10000);
////
////            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
////            System.out.println("-----");
////            String responseString = (String) in.readObject(); // استقبال السلسلة النصية
////            System.out.println("Received response from coordinator: " + responseString);
////
////            Map<String, Double> responseMap = convertStringToMap(responseString); // تحويل السلسلة النصية إلى Map
////            return new SearchResponse(responseMap);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new SearchResponse(Map.of("Error", 0.0));
//        }
//    }

//}
