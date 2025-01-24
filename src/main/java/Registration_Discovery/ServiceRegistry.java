package Registration_Discovery;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private static final String REGISTRY_ZNODE = "/service_registry";
    private static final String COORDINATOR_ZNODE = "/coordinator";
    private final ZooKeeper zooKeeper;
    private String currentZnode = null;
    private static List<String> allServiceAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        createZnodeIfNotExists(REGISTRY_ZNODE);
        createZnodeIfNotExists(COORDINATOR_ZNODE);
    }

    private void createZnodeIfNotExists(String znodePath) {
        try {
            if (zooKeeper.exists(znodePath, false) == null) {
                zooKeeper.create(znodePath, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Error creating znode {}: {}", znodePath, e.getMessage());
        }
    }

    public void registerToCluster(String metadata) throws KeeperException, InterruptedException {
        if (this.currentZnode != null) {
            logger.debug("Already registered to service registry");
            return;
        }
        this.currentZnode = zooKeeper.create(REGISTRY_ZNODE + "/n_", metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.debug("Registered to service registry with znode: {}", currentZnode);
        zooKeeper.getChildren(REGISTRY_ZNODE, this);
    }


    public void registerToClusterAsCoordinator(String metadata) throws KeeperException, InterruptedException {

        if (zooKeeper.exists(COORDINATOR_ZNODE, false) != null) {

            List<String> children = zooKeeper.getChildren(COORDINATOR_ZNODE, false);
            for (String child : children) {
                zooKeeper.delete(COORDINATOR_ZNODE + "/" + child, -1);
            }
        }

        zooKeeper.create(COORDINATOR_ZNODE + "/leader", metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        logger.debug("Registered as leader with address: {}", metadata);
    }

    public String getLeaderAddress() {
        try {
            List<String> leaderNodes = zooKeeper.getChildren(COORDINATOR_ZNODE, false);
            if (!leaderNodes.isEmpty()) {
                String leaderNodePath = COORDINATOR_ZNODE + "/" + leaderNodes.get(0);
                byte[] leaderData = zooKeeper.getData(leaderNodePath, false, null);
                return new String(leaderData);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Error getting leader address: {}", e.getMessage());
        }
        return null;
    }


    public void unregisterFromCluster() {
        try {
            if (currentZnode != null && zooKeeper.exists(currentZnode, false) != null) {
                zooKeeper.delete(currentZnode, -1);
                logger.debug("Unregistered from cluster, removed znode: {}", currentZnode);
            }
        } catch (KeeperException | InterruptedException e) {
            logger.error("Error unregistering from cluster: {}", e.getMessage());
        }
    }

    public void registerForUpdates() {
        try {
            updateAddresses();
        } catch (KeeperException | InterruptedException e) {
            logger.error("Error registering for updates: {}", e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void updateAddresses() throws KeeperException, InterruptedException, UnknownHostException {
        List<String> workerZnodes = zooKeeper.getChildren(REGISTRY_ZNODE, this);

        List<String> addresses = new ArrayList<>(workerZnodes.size());

        for (String workerZnode : workerZnodes) {
            String workerFullPath = REGISTRY_ZNODE + "/" + workerZnode;
            Stat stat = zooKeeper.exists(workerFullPath, false);
            if (stat == null) {
                continue;
            }

            byte[] addressBytes = zooKeeper.getData(workerFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }

        List<String> newAddresses = new ArrayList<>(addresses);
        List<String> oldAddresses = this.allServiceAddresses != null ? new ArrayList<>(this.allServiceAddresses) : new ArrayList<>();
        newAddresses.removeAll(oldAddresses);
        oldAddresses.removeAll(addresses);

        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        logger.info("Updated service addresses: {}", this.allServiceAddresses);

        for (String newAddress : newAddresses) {
            logger.info("New worker detected: {}", newAddress);
        }
        for (String oldAddress : oldAddresses) {
            logger.info("Removed worker: {}", oldAddress);
        }


        String leaderAddress = getLeaderAddress();
        if (leaderAddress != null && leaderAddress.equals(InetAddress.getLocalHost().getHostAddress())) {
            System.out.println("I am the Leader.");
            System.out.println("Worker addresses: " + this.allServiceAddresses);
            System.out.println("Total workers: " + this.allServiceAddresses.size());
        }
    }
    public static List<String> getWorkerAddresses() {
        return allServiceAddresses != null ? allServiceAddresses : Collections.emptyList();
    }

    public List<String> getRegisteredServices() {
        return allServiceAddresses;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged && event.getPath().equals(REGISTRY_ZNODE)) {
            try {
                updateAddresses();
            } catch (KeeperException | InterruptedException e) {
                logger.error("Error processing event: {}", e.getMessage());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
