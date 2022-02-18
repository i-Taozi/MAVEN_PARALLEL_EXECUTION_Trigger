/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package org.atmosphere.plugin.rmi;

import org.atmosphere.cpr.AtmosphereConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * <p>
 * This manager is in charge of sending all broadcast messages to a set of known peers (thanks to its
 * {@link RMIPeerManager#sendAll(String, Object)} method) and to bind a {@link RMIBroadcastService} (thanks to
 * its {@link RMIPeerManager#server(String, RMIBroadcastService, AtmosphereConfig) method}).
 * </p>
 *
 * <p>
 * The manager is a singleton. To be instantiated successfully the first time its {@link RMIPeerManager#getInstance()}
 * method is called, a property file at {@link RMIPeerManager#RMI_PROPERTIES_LOCATION this} location must exists
 * in the classpath. Both RMI server port and peers location must be declared. The port is associated to the key
 * {@link RMIPeerManager#RMI_SERVER_PORT_PROPERTY} and each peer location will be associated to a key starting by
 * {@link RMIPeerManager#PEER_PROPERTY_PREFIX this} prefix. Sample of properties file content :
 *  <pre>
 *      rmi.server.port=4000
 *      rmi.peer.server1=com.my.company.server1:4000
 *      rmi.peer.server2=com.my.company.server2:4000
 *  </pre>
 * </p>
 *
 * <p>
 * NOTE : the properties file should not contains the peer's URL of the server this manager is running on.
 * </p>
 *
 * <p>
 * Moreover, any value declared in the properties file could be overridden with a system property. Each system property
 * must starts with {@link RMIPeerManager#SYSTEM_PROPERTY_PREFIX this} prefix followed by the property key as declared in
 * the properties file. For instance, if I want to override the value of the property 'rmi.peer.server1' declared in the
 * properties file, I will run my server with '-D org.atmosphere.rmi.peer.server1=my.overridden.host:port' in the command
 * line.
 * </p>
 *
 * <p>
 * TODO : Should be enhanced to discover peers automatically thanks to multicast
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 1.1.1
 */
public class RMIPeerManager {

    /**
     * Singleton.
     */
    private static RMIPeerManager instance;

    /**
     * Expected path for the properties file defining all the peers.
     */
    private static final String RMI_PROPERTIES_LOCATION = "/org/atmosphere/plugin/rmi/rmi.properties";

    /**
     * Required server port.
     */
    private static final String RMI_SERVER_PORT_PROPERTY = "rmi.server.port";

    /**
     * Required prefix for all keys referencing a peer's URL in the properties file.
     */
    private static final String PEER_PROPERTY_PREFIX = "rmi.peer.";

    /**
     * Required prefix for properties to be overridden through system properties.
     */
    private static final String SYSTEM_PROPERTY_PREFIX = "org.atmosphere.";

    /**
     * Server port system property.
     */
    private static final String RMI_SERVER_PORT_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + RMI_SERVER_PORT_PROPERTY;

    /**
     * Prefix for all system properties referencing a peer's URL.
     */
    private static final String PEER_SYSTEM_PROPERTY_PREFIX = SYSTEM_PROPERTY_PREFIX + PEER_PROPERTY_PREFIX;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(RMIPeerManager.class);

    /**
     * All the discovered peers.
     */
    private List<Peer> peers;

    /**
     * Registry to use when creating the server.
     */
    private Registry registry;

    /**
     * The port to use when creating the registry.
     */
    private int serverPort;

    /**
     * <p>
     * Builds a new instance by loading properties from {@link RMIPeerManager#RMI_PROPERTIES_LOCATION}.
     * </p>
     *
     * <p>
     * If the properties file is not found in the classpath or could not be read successfully,
     * then an {@code IllegalStateException} will be thrown.
     * </p>
     *
     * <p>
     * If one property does not refer to a valid URL, then an {@code IllegalArgumentException} will be thrown.
     * </p>
     */
    private RMIPeerManager() {
        final Properties properties = new Properties();

        logger.info("Looking for '{}' file in the classpath", RMI_PROPERTIES_LOCATION);
        final InputStream peerProperties = getClass().getResourceAsStream(RMI_PROPERTIES_LOCATION);
        if (peerProperties != null) {
            try {
                logger.info("Loading '{}' file from classpath", RMI_PROPERTIES_LOCATION);
                properties.load(peerProperties);
            } catch (final IOException ioe) {
                logger.error("Unable to load '" + RMI_PROPERTIES_LOCATION + "' file from the classpath", ioe);
            }
        }

        peers = new ArrayList<Peer>();

        discoverServerPort(properties);
        discoverPeers(properties);
    }

    /**
     * <p>
     * Discovers the port to bind when creating the RMI server in the given {@code Properties} object.
     * </p>
     *
     * <p>
     * If the given port is not a valid integer, then an {@code IllegalArgumentException} will be thrown.
     * </p>
     *
     * @param properties the properties containing the RMI port.
     */
    private void discoverServerPort(final Properties properties) {
        String portValue = properties.getProperty(RMI_SERVER_PORT_PROPERTY);

        // Looking for system property
        final String sysPropertyValue = System.getProperty(RMI_SERVER_PORT_SYSTEM_PROPERTY);

        if (sysPropertyValue != null) {
            logger.info("System property '{}' set. Overriding value '{}' with '{}'",
                    new Object[] { RMI_SERVER_PORT_SYSTEM_PROPERTY, portValue, sysPropertyValue, });
            portValue = sysPropertyValue;
        }

        if (portValue == null) {
            throw new IllegalArgumentException(RMI_SERVER_PORT_PROPERTY + " property's value is null. Must be a valid integer");
        }

        try {
            serverPort = Integer.parseInt(portValue);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(RMI_SERVER_PORT_PROPERTY + " property's value is not an integer : " + portValue, nfe);
        }
    }

    /**
     * <p>
     * Discovers all the peer name to authority (host:port) mappings defined in the given {@code Properties} object and/or the system properties.
     * </p>
     *
     * If the same peer name occurs in both the given {@code Properties} object and the system properties,
     * the system properties authority overrides the given {@code Properties} authority.
     *
     * @param properties Properties containing peer name to authority mappings
     *
     * @throws IllegalArgumentException If any peer authority (host:port) is malformed 
     */
    private void discoverPeers(final Properties properties) {
        logger.info("Discovering RMI peers");

        final Map<String, String> sysPropPeerAuthorityByPeerName = new HashMap<String, String>();

        // Add peers from system properties
        for (final Entry<Object, Object> sysProp : System.getProperties().entrySet()) {
            final String sysPropKey = sysProp.getKey().toString();
            if (sysPropKey.startsWith(PEER_SYSTEM_PROPERTY_PREFIX)) {
                final String peerName       = sysPropKey.substring(PEER_SYSTEM_PROPERTY_PREFIX.length());
                final String peerAuthority  = sysProp.getValue().toString();
                addPeer(peerName, peerAuthority);
                sysPropPeerAuthorityByPeerName.put(peerName, peerAuthority);
                logger.info("Added peer '{}' with authority '{}' from system properties", peerName, peerAuthority);
            }
        }

        // Add peers from properties that weren't overridden by system properties
        for (final Entry<Object, Object> property : properties.entrySet()) {
            final String propertyKey = property.getKey().toString();
            if (propertyKey.startsWith(PEER_PROPERTY_PREFIX)) {
                final String peerName             = propertyKey.substring(PEER_PROPERTY_PREFIX.length());
                final String peerAuthority        = property.getValue().toString();
                final String sysPropPeerAuthority = sysPropPeerAuthorityByPeerName.get(peerName);
                if (sysPropPeerAuthority == null) {
                    addPeer(peerName, peerAuthority);
                    logger.info("Added peer '{}' with authority '{}' from properties", peerName, peerAuthority);
                }
                else if (! sysPropPeerAuthority.equals(peerAuthority)) {
                    logger.info(
                        "Peer '{}' with authority '{}' from system properties overrode authority '{}'",
                        new Object[] {peerName, sysPropPeerAuthority, peerAuthority}
                    );
                }
            }
        }
    }

    private void addPeer(final String peerName, final String peerAuthority) {
        try {
            peers.add(new Peer("rmi://" + peerAuthority + '/' + RMIBroadcastService.class.getSimpleName() + '/'));
        } catch (final MalformedURLException mue) {
            throw new IllegalArgumentException(
                "Value for peer '" + peerName + "' must be a valid host name and port (e.g., foo:40001). Invalid value: " + peerAuthority
            );
        }
    }

    /**
     * <p>
     * Gets the unique instance of {@link RMIPeerManager}.
     * </p>
     *
     * <p>
     * If this is the first time the method is called, the singleton will be instantiated here.
     * </p>
     *
     * @return the unique instance
     */
    public synchronized static RMIPeerManager getInstance() {
        if (instance == null) {
            instance = new RMIPeerManager();
        }

        return instance;
    }

    /**
     * <p>
     * Sends the given message to the broadcaster identified by the given ID belonging to all the registered peers.
     * </p>
     *
     * @param broadcasterId the broadcaster ID
     * @param message the message to be sent
     */
    public synchronized void sendAll(final String broadcasterId, final Object message) {
        logger.info("Sending message to {} known RMI peers", peers.size());

        for (Peer peer : peers) {
            peer.send(broadcasterId, message, 1);
        }
    }

    /**
     * <p>
     * Creates a service by binding the given service for the given broadcaster ID.
     * </p>
     *  @param broadcasterId the broadcaster ID
     * @param service       the service to be bound
     * @param config        the atmosphere config
     */
    public synchronized void server(final String broadcasterId, final RMIBroadcastService service, AtmosphereConfig config) {
        try {
            if (registry == null) {
                logger.info("Creating registry with port {}", serverPort);
                registry = LocateRegistry.createRegistry(serverPort);
                if (config != null) {
                    config.shutdownHook(new AtmosphereConfig.ShutdownHook() {
                        @Override
                        public void shutdown() {
                            for (Thread t : Thread.getAllStackTraces().keySet()) {
                                if ("RMI Reaper".equals(t.getName())) {
                                    t.interrupt();
                                }
                            }
                        }
                    });
                }
            }

            logger.info("Rebinding {}", RMIBroadcastService.class.getSimpleName());

            final String url = RMIBroadcastService.class.getSimpleName() + "/" + broadcasterId;
            logger.info("URL : {}", url);
            registry.rebind(url, service);
        } catch (RemoteException re) {
            logger.error("Unable to create the RMI server. Won't receive message to broadcast from other peers", re);
        }
    }

    /**
     * <p>
     * Internal class which represents a peer handling connections where messages should be broadcast.
     * </p>
     *
     * <p>
     * Ths class encapsulates a {@link RMIBroadcastService} retrieved remotely to send the messages. If the
     * connection could not be established because the service is not already reachable, the messages are lost.
     * When a new message is sent, the {@link Peer} tries to reconnect and if it succeeds, it sends it.
     * </p>
     *
     */
    private class Peer {

        /**
         * The remote URL.
         */
        String url;

        /**
         * <p>
         * Creates a new instance. If the connection could not be established because of a non reachable remote
         * service, the object will try to reconnect when a message will be sent.
         * </p>
         *
         * @param peerUrl the remote service URL
         * @throws MalformedURLException if the URL is not correct
         */
        Peer(final String peerUrl) throws MalformedURLException {
            logger.info("Connecting to peer at {}", peerUrl);

            url = peerUrl;

            // Just connect to detect a MalformedURLException exception
            connect("");
        }

        /**
         * <p>
         * Tries to establish a connection by looking up the remote service dedicated to the specified broadcaster ID.
         * </p>
         *
         * @param broadcasterId the broadcaster ID
         * @return the remote interface, {@code }
         * @throws MalformedURLException if the {@link Peer#url} is not correct
         */
        RMIBroadcastService connect(final String broadcasterId) throws MalformedURLException {
            try {
                logger.info("Trying to connect to {}", url);
                return (RMIBroadcastService) Naming.lookup(url + broadcasterId);
            } catch (RemoteException re) {
                logger.warn("Could not reach the remote host with the url {}. Reason is '{}'. Will try later",
                        new Object[] { url, re.getMessage() }, re);
            } catch (NotBoundException nbe) {
                logger.warn("{} for url {} not currently bound. Reason is {}. Will try later",
                        new Object[] { RMIBroadcastService.class.getSimpleName(), url, nbe.getMessage() }, nbe);
            }

            return null;
        }

        /**
         * <p>
         * Sends the given message to the broadcaster identified by the specified ID.
         * </p>
         *
         * <p>
         * Its first tries to connect. Once the connection is established, the given message is sent.
         * If the connection fails or if an error occurs, the method will retries a specified number of times.
         * </p>
         *
         * @param broadcasterId the broadcaster ID
         * @param message the message to send
         * @param retry how many times the method will retry if an error occurs
         */
        synchronized void send(final String broadcasterId, final Object message, final int retry) {
            try {
                RMIBroadcastService service;

                if ((service = connect(broadcasterId)) != null) {
                    logger.debug("Sending message '{}' to peer at url {}", new Object[] { message, url, });

                    if (retry > 0) {
                        try {
                            service.send(message);
                        } catch(Exception e) {
                            logger.warn("Send operation failed {}. Retrying...", e.getMessage());
                            send(broadcasterId, message, retry - 1);
                        }
                    } else {
                        service.send(message);
                    }
                }
            } catch (MalformedURLException mue) {
                // Should never occurs since the URL has been validated when the class was instantiated
                throw new IllegalStateException(mue);
            } catch (RemoteException re) {
                logger.warn("Failed to send message to peer '{}'", url, re);
            }
        }
    }
}
