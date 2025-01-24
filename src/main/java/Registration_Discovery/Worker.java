package Registration_Discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

public class Worker {
    private int port; // Port on which the worker listens for incoming connections
    private final String documentsPath; // Path to the folder containing documents
    private String receivedQuery; // Query received from the client
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    public Worker(int port) {
        this.port = port;

        this.documentsPath = "D:\\Fifth year\\DS\\Ranem_Search_Engine\\Distributed_text_search_system\\src\\main\\resources\\documents" + port;
    }

    // Start the worker server to listen for incoming client connections
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker listening on port " + port);

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("Client connected");

                    // Handle the client in a separate thread
                    handleClient(socket);
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle client requests
    private void handleClient(Socket socket) {
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

            // Receive the search query from the client
            receiveQuery(inputStream);

            // Perform the search operation in the documents
            List<DocumentTermsInfo> searchResults = searchDocuments();

            // Send the results back to the client
            sendResults(outputStream, searchResults);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error processing request: " + e.getMessage());
        }
    }

    // Receive the query from the client
    private void receiveQuery(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        receivedQuery = (String) inputStream.readObject(); // Read the query as a string
        System.out.println("Received query: " + receivedQuery);
    }

    // Perform the search operation in the documents
    private List<DocumentTermsInfo> searchDocuments() {
        System.out.println("Searching documents in path: " + documentsPath);

        File folder = new File(documentsPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt")); // Get all .txt files in the folder

        List<DocumentTermsInfo> results = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                System.out.println("Processing file: " + file.getName());
                DocumentTermsInfo documentInfo = processFile(file); // Process each file
                results.add(documentInfo);
            }
        } else {
            System.out.println("No files found in directory: " + documentsPath);
        }

        return results;
    }

    // Process a single file and calculate term frequencies (TF)
    private DocumentTermsInfo processFile(File file) {
        DocumentTermsInfo documentInfo = new DocumentTermsInfo(file.getName()); // Create a new document info object

        try {
            String content = new String(Files.readAllBytes(file.toPath())); // Read the file content

            // Split the content into words and calculate total word count in the document
            String[] words = content.split("\\s+");
            int totalWords = words.length;

            // Initialize the map to store term frequencies
            HashMap<String, Double> termFrequencies = new HashMap<>();

            // Calculate the term frequencies for the query words
            for (String queryWord : receivedQuery.split("\\s+")) {
                queryWord = queryWord.trim(); // Remove any surrounding spaces
                // Count occurrences of the query word in the document
                double wordCountInDocument = countWordOccurrences(content, queryWord);
                // Calculate Term Frequency (TF)
                double tf = calculateTermFrequency(wordCountInDocument, totalWords);

                // Log the results for each term
//                logger.info("(Term: {}, Frequency: {}, Total Document Words: {}, Term Frequency (Percentage): {})",
//                        queryWord, wordCountInDocument, totalWords, tf);

                // Store the term frequency in the map
                termFrequencies.put(queryWord, tf);
            }

            // Set the term frequencies in the document info object
            documentInfo.setTermFrequency(termFrequencies);

        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }

        return documentInfo;
    }

    // Count the occurrences of a word in the document (case insensitive)
    private double countWordOccurrences(String fileContent, String word) {
        String[] documentWords = fileContent.split("\\s+");
        double count = 0;
        for (String documentWord : documentWords) {
            if (documentWord.toLowerCase().contains(word.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    // Calculate Term Frequency (TF) for a word
    private double calculateTermFrequency(double wordCountInDocument, double documentWordsCount) {
        double TF = 0.0;
        if (documentWordsCount != 0) {
            TF = wordCountInDocument / documentWordsCount;
        }
        return TF;
    }



    // Send the search results back to the coordinator
    private void sendResults(ObjectOutputStream outputStream, List<DocumentTermsInfo> results) throws IOException {
        System.out.println("Sending results to coordinator...");
        outputStream.writeObject(results); // Write the list of document information to the output stream
        outputStream.flush(); // Ensure all data is sent
        System.out.println("Results sent successfully: " + results);
    }
}
