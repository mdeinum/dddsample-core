package se.citerus.dddsample.application.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import se.citerus.dddsample.application.BookingService;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.Itinerary;
import se.citerus.dddsample.domain.model.cargo.RouteSpecification;
import se.citerus.dddsample.domain.model.cargo.TrackingId;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.service.RoutingService;

public class BookingServiceImpl implements BookingService {

  private final CargoRepository cargoRepository;
  private final LocationRepository locationRepository;
  private final RoutingService routingService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public BookingServiceImpl(final CargoRepository cargoRepository,
                            final LocationRepository locationRepository,
                            final RoutingService routingService) {
    this.cargoRepository = cargoRepository;
    this.locationRepository = locationRepository;
    this.routingService = routingService;
  }

  @Override
  @Transactional
  public TrackingId bookNewCargo(final UnLocode originUnLocode,
                                 final UnLocode destinationUnLocode,
                                 final Date arrivalDeadline) {
    // TODO modeling this as a cargo factory might be suitable
    final TrackingId trackingId = cargoRepository.nextTrackingId();
    final Location origin = locationRepository.find(originUnLocode);
    final Location destination = locationRepository.find(destinationUnLocode);
    final RouteSpecification routeSpecification = new RouteSpecification(origin, destination, arrivalDeadline);

    final Cargo cargo = new Cargo(trackingId, routeSpecification);

    cargoRepository.store(cargo);
    logger.info("Booked new cargo with tracking id {}", cargo.trackingId().idString());

    return cargo.trackingId();
  }

  @Override
  @Transactional
  public List<Itinerary> requestPossibleRoutesForCargo(final TrackingId trackingId) {
    final Cargo cargo = cargoRepository.find(trackingId);

    if (cargo == null) {
      return Collections.emptyList();
    }

    return routingService.fetchRoutesForSpecification(cargo.routeSpecification());
  }

  @Override
  @Transactional
  public void assignCargoToRoute(final Itinerary itinerary, final TrackingId trackingId) {
    final Cargo cargo = cargoRepository.find(trackingId);
    if (cargo == null) {
      throw new IllegalArgumentException("Can't assign itinerary to non-existing cargo " + trackingId);
    }

    cargo.assignToRoute(itinerary);
    cargoRepository.store(cargo);

    logger.info("Assigned cargo {} to new route", trackingId);
  }

  @Override
  @Transactional
  public void changeDestination(final TrackingId trackingId, final UnLocode unLocode) {
    final Cargo cargo = cargoRepository.find(trackingId);
    final Location newDestination = locationRepository.find(unLocode);

    final RouteSpecification routeSpecification = new RouteSpecification(
      cargo.origin(), newDestination, cargo.routeSpecification().arrivalDeadline()
    );
    cargo.specifyNewRoute(routeSpecification);

    cargoRepository.store(cargo);
    logger.info("Changed destination for cargo {} to {}" ,trackingId, routeSpecification.destination());
  }

}
