/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.analysis.travelcomponents;

import ch.sbb.matsim.config.variables.SBBActivities;
import ch.sbb.matsim.config.variables.SBBModes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;

public class Trip extends TravelComponent {

	private final Config config;
	private double walkSpeed;
	private Activity fromAct;
	private Activity toAct;
	private TravelledLeg firstRailLeg;
	private TravelledLeg lastRailLeg;
	private List<TravelledLeg> legs = new ArrayList<>();
	private String visumTripId;

	Trip(Config config) {
		super(config);
		this.config = config;
		this.walkSpeed = config.plansCalcRoute().getModeRoutingParams().get(SBBModes.WALK_FOR_ANALYSIS).getTeleportedModeSpeed();
	}

	public TravelledLeg addLeg() {
		TravelledLeg leg = new TravelledLeg(this.config);
		this.legs.add(leg);
		return leg;
	}

	public void setVisumTripId(String visumTripId) {
		this.visumTripId = visumTripId;
	}

	public String getVisumTripId() {
		return visumTripId;
	}

	public List<TravelledLeg> getLegs() {
		return this.legs;
	}

	public TravelledLeg getFirstLeg() {
		return this.legs.get(0);
	}

	public TravelledLeg getLastLeg() {
		return this.legs.get(this.legs.size() - 1);
	}

	public String toString() {
		return String.format("TRIP: start: %6.0f end: %6.0f dur: %6.0f invehDist: %6.0f walkDist: %6.0f \n %s",
				getStartTime(), getEndTime(), getDuration(), getInVehDistance(), getWalkDistance(),
				legs.toString());
	}

	public double getInVehDistance() {
		if (getMainMode().equals(SBBModes.WALK_FOR_ANALYSIS)) {
			return 0;
		}
		return this.legs.stream().mapToDouble(TravelledLeg::getDistance).sum();
	}

	private double getWalkDistance() {
		if (getMainMode().equals(SBBModes.WALK_FOR_ANALYSIS)) {
			return walkSpeed * getDuration();
		}
		return 0;
	}

	public double getDistance() {
		return getInVehDistance() + getWalkDistance();
	}

	public double getInVehTime() {
		if (getMainMode().equals(SBBModes.WALK_FOR_ANALYSIS)) {
			return 0;
		}
		return this.legs.stream().mapToDouble(TravelledLeg::getDuration).sum();
	}

	public String getMainMode() {
		// get main mode according to hierarchical order
		TravelledLeg leg = Collections.min(this.legs, Comparator.comparing(TravelledLeg::getModeHierarchy));
		if (leg.getModeHierarchy() != SBBModes.DEFAULT_MODE_HIERARCHY) {
			if (leg.isPtLeg()) {
				return SBBModes.PT;
			}
			String mainMode = leg.getMode();
			if (mainMode.equals(SBBModes.PT_FALLBACK_MODE) || mainMode.equals(SBBModes.WALK_MAIN_MAINMODE)) {
				return SBBModes.WALK_FOR_ANALYSIS;
			}
			return mainMode;
		} else {
			// fallback solution -> get main mode according to longest distance
			return Collections.max(this.legs, Comparator.comparing(TravelledLeg::getDistance)).getMode();
		}
	}

	public Activity getFromAct() {
		return fromAct;
	}

	public void setFromAct(Activity fromAct) {
		this.fromAct = fromAct;
	}

	public Activity getToAct() {
		return toAct;
	}

	public void setToAct(Activity toAct) {
		this.toAct = toAct;
	}

	public String getToActType() {
		String typeLong = this.toAct.getType();
		String type = typeLong.split("_")[0];
		return SBBActivities.matsimActs2abmActs.get(type);
	}

	public List<TravelledLeg> getAccessLegs() {
		List<TravelledLeg> accessLegs = new ArrayList<>();
		if (!isRailJourney()) {
			return accessLegs;
		}
		for (TravelledLeg leg : this.legs) {
			if (leg.isRailLeg()) {
				this.firstRailLeg = leg;
				break;
			}
			if (leg.getDistance() > 0) {
				accessLegs.add(leg);
			}
		}
		return accessLegs;
	}

	public List<TravelledLeg> getEgressLegs() {
		List<TravelledLeg> egressLegs = new ArrayList<>();
		TravelledLeg leg;
		if (!isRailJourney()) {
			return egressLegs;
		}
		for (int i = this.legs.size() - 1; i >= 0; i--) {
			leg = this.legs.get(i);
			if (leg.isRailLeg()) {
				this.lastRailLeg = leg;
				break;
			}
			if (leg.getDistance() > 0) {
				egressLegs.add(0, leg);
			}
		}
		return egressLegs;
	}

	public boolean isRailJourney() {
		return this.legs.stream().anyMatch(TravelledLeg::isRailLeg);
	}

	public String getAccessToRailMode(List<TravelledLeg> accessLegs) {
		if (accessLegs == null || accessLegs.isEmpty()) {
			return "";
		} else if (accessLegs.size() > 1) {
			Set<String> modes = new HashSet<>();
			for (TravelledLeg leg : accessLegs) {
				modes.add(leg.getMode());
			}
			if (modes.contains(SBBModes.ACCESS_EGRESS_WALK)) {
				if (modes.size() > 1) {
					modes.remove(SBBModes.ACCESS_EGRESS_WALK);
				}
			}
			if (modes.size() == 0) {
				return "";
			} else if (modes.size() == 1) {
				return new ArrayList<>(modes).get(0);
			} else {
				return String.join(",", new ArrayList<>(modes));
			}
		} else {
			return accessLegs.get(0).getMode();
		}
	}

	public String getEgressFromRailMode(List<TravelledLeg> egressLegs) {
		if (egressLegs == null || egressLegs.isEmpty()) {
			return "";
		} else if (egressLegs.size() > 1) {
			Set<String> modes = new HashSet<>();
			for (TravelledLeg leg : egressLegs) {
				modes.add(leg.getMode());
			}
			if (modes.contains(SBBModes.ACCESS_EGRESS_WALK)) {
				if (modes.size() > 1) {
					modes.remove(SBBModes.ACCESS_EGRESS_WALK);
				}
			}
			if (modes.size() == 0) {
				return "";
			} else if (modes.size() == 1) {
				return new ArrayList<>(modes).get(0);
			} else {
				return String.join(",", new ArrayList<>(modes));
			}
		} else {
			return egressLegs.get(0).getMode();
		}
	}

	public double getAccessToRailDist(List<TravelledLeg> accessLegs) {
		if (accessLegs == null) {
			return 0;
		}
		return accessLegs.stream().mapToDouble(TravelledLeg::getDistance).sum();
	}

	public double getEgressFromRailDist(List<TravelledLeg> egressLegs) {
		if (egressLegs == null) {
			return 0;
		}
		return egressLegs.stream().mapToDouble(TravelledLeg::getDistance).sum();
	}

	public Id getFirstRailBoardingStop() {
		if (this.firstRailLeg == null) {
			return null;
		}
		return this.firstRailLeg.getBoardingStop();
	}

	public Id getLastRailAlightingStop() {
		if (this.lastRailLeg == null) {
			return null;
		}
		return this.lastRailLeg.getAlightingStop();
	}
}