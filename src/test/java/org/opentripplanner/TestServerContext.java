package org.opentripplanner;

import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;

import io.micrometer.core.instrument.Metrics;
import java.util.List;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;

public class TestServerContext {

  private TestServerContext() {}

  /** Create a context for unit testing, using the default RouteRequest. */
  public static OtpServerRequestContext createServerContext(
    Graph graph,
    TransitModel transitModel
  ) {
    transitModel.index();
    final RouterConfig routerConfig = RouterConfig.DEFAULT;
    var transitService = new DefaultTransitService(transitModel);
    DefaultServerRequestContext context = DefaultServerRequestContext.create(
      routerConfig.transitTuningConfig(),
      routerConfig.routingRequestDefaults(),
      new RaptorConfig<>(routerConfig.transitTuningConfig()),
      graph,
      new DefaultTransitService(transitModel),
      Metrics.globalRegistry,
      routerConfig.vectorTileLayers(),
      createWorldEnvelopeService(),
      createRealtimeVehicleService(transitService),
      createVehicleRentalService(),
      routerConfig.flexConfig(),
      List.of(),
      new DefaultStopConsolidationService(new DefaultStopConsolidationRepository(), transitModel),
      null
    );
    creatTransitLayerForRaptor(transitModel, routerConfig.transitTuningConfig());
    return context;
  }

  /** Static factory method to create a service for test purposes. */
  public static WorldEnvelopeService createWorldEnvelopeService() {
    return new DefaultWorldEnvelopeService(new DefaultWorldEnvelopeRepository());
  }

  public static RealtimeVehicleService createRealtimeVehicleService(TransitService transitService) {
    return new DefaultRealtimeVehicleService(transitService);
  }

  public static VehicleRentalService createVehicleRentalService() {
    return new DefaultVehicleRentalService();
  }
}
