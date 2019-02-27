/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim;


import ch.sbb.matsim.analysis.SBBPostProcessingOutputHandler;
import ch.sbb.matsim.config.*;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.preparation.PopulationSampler.SBBPopulationSampler;
import ch.sbb.matsim.routing.access.AccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import ch.sbb.matsim.scoring.SBBScoringFunctionFactory;
import ch.sbb.matsim.vehicles.ParkingCostVehicleTracker;
import ch.sbb.matsim.vehicles.CreateVehiclesFromType;
import ch.sbb.matsim.zones.ZonesCollections;
import ch.sbb.matsim.zones.ZonesListConfigGroup;
import ch.sbb.matsim.zones.ZonesModule;
import com.google.inject.Provides;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * @author denism
 *
 */
public class RunSBB {

    private final static Logger log = Logger.getLogger(RunSBB.class);

    public static void main(String[] args) {
        System.setProperty("matsim.preferLocalDtds", "true");

        final String configFile = args[0];
        log.info(configFile);
        final Config config = buildConfig(configFile);

        if(args.length > 1)
            config.controler().setOutputDirectory(args[1]);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // vehicle types
        new CreateVehiclesFromType(scenario.getPopulation(), scenario.getVehicles(), "vehicleType", "car").createVehicles();
        scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);

        // controler
        Controler controler = new Controler(scenario);

        SBBPopulationSamplerConfigGroup samplerConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), SBBPopulationSamplerConfigGroup.class);
        if(samplerConfig.getDoSample()){
            SBBPopulationSampler sbbPopulationSampler = new SBBPopulationSampler();
            sbbPopulationSampler.sample(scenario.getPopulation(), samplerConfig.getFraction());
        }

        ScoringFunctionFactory scoringFunctionFactory = new SBBScoringFunctionFactory(scenario);
        controler.setScoringFunctionFactory(scoringFunctionFactory);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addControlerListenerBinding().to(SBBPostProcessingOutputHandler.class);
            }
        });

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("ride").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());

                install(new SBBTransitModule());
                install(new SwissRailRaptorModule());
                install(new ZonesModule());

                addEventHandlerBinding().to(ParkingCostVehicleTracker.class);
            }

            @Provides
            QSimComponentsConfig provideQSimComponentsConfig() {
                QSimComponentsConfig components = new QSimComponentsConfig();
                new StandardQSimComponentConfigurator(config).configure(components);
                SBBTransitEngineQSimModule.configure(components);
                return components;
            }
        });

        controler.addOverridingModule(new AccessEgress(scenario));

        controler.run();
    }

    public static Config buildConfig(String filepath) {
        return ConfigUtils.loadConfig(filepath, new PostProcessingConfigGroup(), new SBBTransitConfigGroup(),
                new SBBBehaviorGroupsConfigGroup(), new SBBPopulationSamplerConfigGroup(), new SwissRailRaptorConfigGroup(),
                new ZonesListConfigGroup());
    }
}
