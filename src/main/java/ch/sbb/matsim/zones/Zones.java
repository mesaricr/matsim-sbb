package ch.sbb.matsim.zones;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser
 */
public interface Zones {

    Id<Zones> getId();

    int size();

    Zone findZone(double x, double y);
}
