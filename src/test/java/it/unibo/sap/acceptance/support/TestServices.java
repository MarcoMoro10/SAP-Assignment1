package it.unibo.sap.acceptance.support;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import it.unibo.sap.account.application.AccountServiceImpl;
import it.unibo.sap.account.infrastructure.AccountServiceController;
import it.unibo.sap.account.infrastructure.FileBasedAccountRepository;
import it.unibo.sap.delivery.application.DeliveryServiceImpl;
import it.unibo.sap.delivery.application.DroneEventHandler;
import it.unibo.sap.delivery.infrastructure.DeliveryServiceController;
import it.unibo.sap.delivery.infrastructure.FileBasedDeliveryRepository;
import it.unibo.sap.delivery.infrastructure.FleetMonitoringController;
import it.unibo.sap.delivery.infrastructure.GeocodingService;
import it.unibo.sap.delivery.infrastructure.InMemoryTrackingSessionRegistry;
import it.unibo.sap.delivery.infrastructure.VertxSchedulerVerticle;
import it.unibo.sap.delivery.infrastructure.VertxTrackingSessionEventObserver;
import it.unibo.sap.delivery.infrastructure.fleet.DroneEventHandlerSink;
import it.unibo.sap.delivery.infrastructure.fleet.FleetModule;
import it.unibo.sap.delivery.infrastructure.fleet.FleetSeeder;
import it.unibo.sap.delivery.infrastructure.fleet.InMemoryDroneRepository;
import it.unibo.sap.session.application.SessionService;
import it.unibo.sap.session.application.SessionServiceImpl;
import it.unibo.sap.session.infrastructure.AccountServiceProxy;
import it.unibo.sap.session.infrastructure.DeliveryServiceProxy;
import it.unibo.sap.session.infrastructure.InMemorySessionRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class TestServices {

    // Test ports, distinct from production (8080/8082/8083) to avoid clashes.
    private static final String HOST = "localhost";
    private static final int ACCOUNT_PORT = 9080;
    private static final int DELIVERY_PORT = 9082;
    private static final int FLEET_PORT = 9083;
    private static final double DRONE_SPEED = 0.01;

    private static TestServices instance;

    private final Vertx vertx;
    private final WebClient webClient;
    private final SessionService sessionService;

    private TestServices() {
        this.vertx = Vertx.vertx();
        this.webClient = WebClient.create(vertx);

        startAccountService();
        startDeliveryService();

        // session-service wired with REAL proxies pointing at the test ports
        final var accountProxy = new AccountServiceProxy(webClient, HOST, ACCOUNT_PORT);
        final var deliveryProxy = new DeliveryServiceProxy(webClient, HOST, DELIVERY_PORT, FLEET_PORT);
        this.sessionService = new SessionServiceImpl(
                accountProxy, deliveryProxy, new InMemorySessionRepository());
    }

    public static synchronized TestServices get() {
        if (instance == null) {
            instance = new TestServices();
        }
        return instance;
    }

    public SessionService sessionService() {
        return sessionService;
    }

    public WebClient webClient() {
        return webClient;
    }

    public String host() {
        return HOST;
    }

    public int accountPort() {
        return ACCOUNT_PORT;
    }

    private void startAccountService() {
        final var repository = new FileBasedAccountRepository(tempFile("accounts-test"));
        final var service = new AccountServiceImpl(repository);
        deployAndWait(new AccountServiceController(service, ACCOUNT_PORT));
    }

    private void startDeliveryService() {
        final var deliveryRepository = new FileBasedDeliveryRepository(tempFile("deliveries-test"));
        final var trackingRegistry = new InMemoryTrackingSessionRegistry();
        final var geocoding = new GeocodingService();
        final var trackingObserver = new VertxTrackingSessionEventObserver(vertx.eventBus());

        final var droneRepository = new InMemoryDroneRepository();
        final var fleetModule = new FleetModule(droneRepository, DRONE_SPEED);

        final var deliveryService = new DeliveryServiceImpl(
                deliveryRepository, fleetModule, geocoding, trackingRegistry);

        final var droneEventHandler = new DroneEventHandler(
                deliveryRepository, trackingRegistry, trackingObserver, fleetModule, DRONE_SPEED);
        fleetModule.setTelemetrySink(new DroneEventHandlerSink(droneEventHandler));

        FleetSeeder.seed(droneRepository);

        deployAndWait(new DeliveryServiceController(deliveryService, DELIVERY_PORT));
        deployAndWait(new FleetMonitoringController(deliveryService, FLEET_PORT));
        deployAndWait(new VertxSchedulerVerticle(deliveryService));
    }

    private void deployAndWait(final io.vertx.core.Verticle verticle) {
        final CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(verticle, ar -> latch.countDown());
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Verticle deploy timed out: " + verticle);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String tempFile(final String prefix) {
        try {
            final Path dir = Files.createTempDirectory(prefix);
            return dir.resolve(prefix + "-" + UUID.randomUUID() + ".json").toString();
        } catch (final Exception e) {
            throw new IllegalStateException("Cannot create temp data file", e);
        }
    }
}
