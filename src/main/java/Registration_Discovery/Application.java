package Registration_Discovery;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

public class Application implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final String address = "192.168.184.129:2181";
    private static final int SESSION_TIMEOUT = 3000;
    //private static final int DEFAULT_PORT = 8080;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        System.out.println("Enter Port!!!");
        Scanner scanner = new Scanner(System.in);
        int currentServerPort = scanner.nextInt();
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZookeeper();

        logger.info("Connected");

        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);

        OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, currentServerPort);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
        application.run();
        application.close();
    }

    public ZooKeeper connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(address, SESSION_TIMEOUT, this);
        return zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    private void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    logger.debug("Successfully connected to Zookeeper");
                } else if (watchedEvent.getState() == Watcher.Event.KeeperState.Disconnected) {
                    synchronized (zooKeeper) {
                        logger.debug("Disconnected from Zookeeper");
                        zooKeeper.notifyAll();
                    }
                } else if (watchedEvent.getState() == Watcher.Event.KeeperState.Closed) {
                    logger.debug("Closed Successfully");
                }
                break;
        }
    }

}
