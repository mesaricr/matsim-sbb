package ch.sbb.matsim.intermodal;

import ch.sbb.matsim.config.SBBIntermodalConfigGroup;
import ch.sbb.matsim.config.SBBIntermodalConfigGroup.SBBIntermodalModeParameterSet;
import ch.sbb.matsim.config.variables.SBBModes;
import ch.sbb.matsim.config.variables.Variables;
import ch.sbb.matsim.csv.CSVReader;
import ch.sbb.matsim.intermodal.analysis.IntermodalControlerListener;
import ch.sbb.matsim.intermodal.analysis.IntermodalTransferTimeAnalyser;
import ch.sbb.matsim.routing.network.SBBNetworkRoutingModule;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;


public class IntermodalModule extends AbstractModule {
    private final static Logger log = Logger.getLogger(IntermodalModule.class);

    public static void preparePopulation(Population population, URL csvPath) {
        try (CSVReader reader = new CSVReader(csvPath, ";")) {
            log.info(csvPath);

            Set<String> attributes = new HashSet<>(Arrays.asList(reader.getColumns()));
            if (!attributes.contains(Variables.PERSONID)) {
                throw new RuntimeException("CSV file does not contain a " + Variables.PERSONID + " field in header.");
            }
            Map<String, String> map;
            while ((map = reader.readLine()) != null) {
                final String personIdString = map.get(Variables.PERSONID);
                Id<Person> personId = Id.createPersonId(personIdString);
                Person person = population.getPersons().get(personId);
                if (person != null) {
                    for (String attribute : attributes) {
                        if (person.getAttributes().getAsMap().containsKey(attribute)) {
                            throw new RuntimeException("Attribute " + attribute + " already exists. Overwriting by CSV should not be intended.");
                        }
                        person.getAttributes().putAttribute(attribute, map.get(attribute));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void prepareIntermodalScenario(Scenario scenario) {
        SBBIntermodalConfigGroup configGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), SBBIntermodalConfigGroup.class);

        URL csvPath = configGroup.getAttributesCSVPathURL(scenario.getConfig().getContext());
        if (csvPath != null) {
            preparePopulation(scenario.getPopulation(), csvPath);
        }

        for (SBBIntermodalModeParameterSet mode : configGroup.getModeParameterSets()) {
            if (mode.isRoutedOnNetwork()) {
                SBBNetworkRoutingModule.addNetworkMode(scenario.getNetwork(), mode.getMode(), SBBModes.CAR);
                Set<String> routedModes = new HashSet<>(scenario.getConfig().plansCalcRoute().getNetworkModes());
                routedModes.add(mode.getMode());
                scenario.getConfig().plansCalcRoute().setNetworkModes(routedModes);
            }
            if (mode.isSimulatedOnNetwork()) {
                Set<String> mainModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
                mainModes.add(mode.getMode());
                scenario.getConfig().qsim().setMainModes(mainModes);
            }
        }
    }

    @Override
    public void install() {
        SBBIntermodalConfigGroup configGroup = ConfigUtils.addOrGetModule(this.getConfig(), SBBIntermodalConfigGroup.class);

        for (SBBIntermodalModeParameterSet mode : configGroup.getModeParameterSets()) {
			if (mode.isRoutedOnNetwork() && !mode.getMode().equals(SBBModes.CAR)) {
				addTravelTimeBinding(mode.getMode()).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(mode.getMode()).to(carTravelDisutilityFactoryKey());
			}
        }
        bind(IntermodalTransferTimeAnalyser.class).asEagerSingleton();
        addControlerListenerBinding().to(IntermodalControlerListener.class).asEagerSingleton();
        bind(RaptorIntermodalAccessEgress.class).to(SBBRaptorIntermodalAccessEgress.class).asEagerSingleton();
    }

}


