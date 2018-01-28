/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.preparation;

import ch.sbb.matsim.RunSBB;
import ch.sbb.matsim.config.PostProcessingConfigGroup;
import ch.sbb.matsim.config.SBBBehaviorGroupsConfigGroup;
import ch.sbb.matsim.config.SBBPopulationSamplerConfigGroup;
import ch.sbb.matsim.config.SBBTransitConfigGroup;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class XLSXScoringParser {

    static private final String[] MODES = new String[] {"car", "ride", "pt", "transit_walk", "egress_walk", "access_walk", "walk", "bike"};
    static private final String SCORING_SHEET = "ScoringParams";
    static private final String MATSIM_PARAMS_LABEL = "MATSim Param Name";
    static private final String GENERAL_PARAMS_LABEL = "general";
    private static final String UTL_OF_LINE_SWITCH = "utilityOfLineSwitch" ;
    private static final String WAITING_PT  = "waitingPt";
    private static final String EARLY_DEPARTURE = "earlyDeparture";
    private static final String LATE_ARRIVAL = "lateArrival";
    private static final String PERFORMING = "performing";
    private static final String WAITING  = "waiting";
    private static final String MARGINAL_UTL_OF_MONEY = "marginalUtilityOfMoney" ;
    private static final String CONSTANT = "constant";
    private static final String MARGINAL_UTILITY_OF_TRAVELING = "marginalUtilityOfTraveling_util_hr";
    private static final String MARGINAL_UTILITY_OF_DISTANCE = "marginalUtilityOfDistance_util_m";
    private static final String MONETARY_DISTANCE_RATE = "monetaryDistanceRate";

    static private final String[] GENERAL_PARAMS_ARRAY = new String[] {UTL_OF_LINE_SWITCH, WAITING_PT, EARLY_DEPARTURE, LATE_ARRIVAL, WAITING, PERFORMING, MARGINAL_UTL_OF_MONEY};
    static private final String[] MODE_PARAMS_ARRAY = new String[] {CONSTANT, MARGINAL_UTILITY_OF_DISTANCE, MARGINAL_UTILITY_OF_TRAVELING, MONETARY_DISTANCE_RATE};
    static private final Set<String> GENERAL_PARAMS = new HashSet<>(Arrays.asList(GENERAL_PARAMS_ARRAY));
    static private final Set<String> MODE_PARAMS = new HashSet<>(Arrays.asList(MODE_PARAMS_ARRAY));

    static private final String DUMMY_GROUP_SHEET = "DummyGroupForScoringOnlyDefault";
    static private final String DUMMY_GROUP_NAME = "DummyDistanceCorrection";
    static private final String SEASON_TICKET_SHEET = "Abobesitz";
    static private final String SEASON_TICKET_NAME = "Abobesitz";
    static private final String CAR_AVAIL_SHEET = "PW_Verf";
    static private final String CAR_AVAIL_NAME = "PW Verfuegbarkeit";
    static private final String LAND_USE_SHEET = "Raumtyp";
    static private final String LAND_USE_NAME = "Raumtyp";

    static private final Map<String, String> BEHAVIOR_GROUP_SHEETS = new HashMap<>();
    static {
        BEHAVIOR_GROUP_SHEETS.put(DUMMY_GROUP_NAME, DUMMY_GROUP_SHEET);
        BEHAVIOR_GROUP_SHEETS.put(SEASON_TICKET_NAME, SEASON_TICKET_SHEET);
        BEHAVIOR_GROUP_SHEETS.put(CAR_AVAIL_NAME, CAR_AVAIL_SHEET);
        BEHAVIOR_GROUP_SHEETS.put(LAND_USE_NAME, LAND_USE_SHEET);
    }

    static private final String DUMMY_GROUP_PERSON_ATTRIBUTE = "subpopulation";
    static private final String SEASON_TICKET_PERSON_ATTRIBUTE = "season_ticket";
    static private final String CAR_AVAIL_PERSON_ATTRIBUTE = "availability: car";
    static private final String LAND_USE_PERSON_ATTRIBUTE = "raumtyp";

    static private final Map<String, String> BEHAVIOR_GROUP_PERSON_ATTRIBUTES = new HashMap<>();
    static {
        BEHAVIOR_GROUP_PERSON_ATTRIBUTES.put(DUMMY_GROUP_NAME, DUMMY_GROUP_PERSON_ATTRIBUTE);
        BEHAVIOR_GROUP_PERSON_ATTRIBUTES.put(SEASON_TICKET_NAME, SEASON_TICKET_PERSON_ATTRIBUTE);
        BEHAVIOR_GROUP_PERSON_ATTRIBUTES.put(CAR_AVAIL_NAME, CAR_AVAIL_PERSON_ATTRIBUTE);
        BEHAVIOR_GROUP_PERSON_ATTRIBUTES.put(LAND_USE_NAME, LAND_USE_PERSON_ATTRIBUTE);
    }

    static private final String[] DUMMY_GROUP_ATTRIBUTE_VALUES = new String[] {"dummy"};
    static private final String[] SEASON_TICKET_ATTRIBUTE_VALUES = new String[] {"none", "Generalabo", "Halbtaxabo"};
    static private final String[] CAR_AVAIL_ATTRIBUTE_VALUES = new String[] {"always", "never", "by arrengement"};
    static private final String[] LAND_USE_ATTRIBUTE_VALUES = new String[] {"1", "2", "3", "4"};

    static private final Map<String, Set<String>> BEHAVIOR_GROUP_ATTRIBUTE_VALUES = new HashMap<>();
    static {
        BEHAVIOR_GROUP_ATTRIBUTE_VALUES.put(DUMMY_GROUP_NAME, new HashSet<>(Arrays.asList(DUMMY_GROUP_ATTRIBUTE_VALUES)));
        BEHAVIOR_GROUP_ATTRIBUTE_VALUES.put(SEASON_TICKET_NAME, new HashSet<>(Arrays.asList(SEASON_TICKET_ATTRIBUTE_VALUES)));
        BEHAVIOR_GROUP_ATTRIBUTE_VALUES.put(CAR_AVAIL_NAME, new HashSet<>(Arrays.asList(CAR_AVAIL_ATTRIBUTE_VALUES)));
        BEHAVIOR_GROUP_ATTRIBUTE_VALUES.put(LAND_USE_NAME, new HashSet<>(Arrays.asList(LAND_USE_ATTRIBUTE_VALUES)));
    }

    private static Logger log = Logger.getLogger(RunSBB.class);

    public static void parseXLSXWorkbook(Workbook workbook, Config config) {
        PlanCalcScoreConfigGroup planCalcScore = config.planCalcScore();
        SBBBehaviorGroupsConfigGroup behaviorGroupConfigGroup = (SBBBehaviorGroupsConfigGroup) config.getModules().get(SBBBehaviorGroupsConfigGroup.GROUP_NAME);

        Sheet scoringParamsSheet = workbook.getSheet(SCORING_SHEET);

        if (scoringParamsSheet != null) {
            parseScoringParamsSheet(scoringParamsSheet, planCalcScore);
            log.info("parsed general acoring parameters sheet: " + SCORING_SHEET);
        }

        for (Map.Entry<String, String> entry : BEHAVIOR_GROUP_SHEETS.entrySet()) {
            Sheet behaviorGroupParamsSheet = workbook.getSheet(entry.getValue());

            if (behaviorGroupParamsSheet != null) {
                parseBehaviorGroupParamsSheet(entry.getKey(), behaviorGroupParamsSheet, behaviorGroupConfigGroup);
                log.info("parsed behaviorGroup scoring parameters sheet: " + entry.getValue());
            }
        }
    }

    protected static void parseScoringParamsSheet(Sheet scoringParamsSheet, PlanCalcScoreConfigGroup planCalcScore) {
        Map<Integer, PlanCalcScoreConfigGroup.ModeParams> modeParamsConfig = new TreeMap<>();
        Set<String> modes = new TreeSet<>();
        Integer generalParamsCol = null;

        for (Row row : scoringParamsSheet) {
            Cell firstCell = row.getCell(0);

            if ((firstCell != null) && (firstCell.getCellTypeEnum() == CellType.STRING)) {
                String rowLabel = firstCell.getStringCellValue();

                if (rowLabel.equals(MATSIM_PARAMS_LABEL)) {
                    int lastColumn = row.getLastCellNum();

                    for (int col = 1; col < lastColumn; col++) {
                        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        if ((cell != null) && (cell.getCellTypeEnum() == CellType.STRING)) {
                            String mode = cell.getStringCellValue();

                            if (mode.equals(GENERAL_PARAMS_LABEL)) {
                                generalParamsCol = col;
                            } else if (Arrays.asList(MODES).contains(mode)) {
                                PlanCalcScoreConfigGroup.ModeParams modeParams = planCalcScore.getOrCreateModeParams(mode);
                                modeParamsConfig.put(col, modeParams);
                                modes.add(mode);
                            }
                        }
                    }

                    log.info("found parameters for modes: " + modes.toString());
                } else if (MODE_PARAMS.contains(rowLabel)) {
                    for (Map.Entry<Integer, PlanCalcScoreConfigGroup.ModeParams> entry : modeParamsConfig.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        PlanCalcScoreConfigGroup.ModeParams modeParams = entry.getValue();

                        if ((cell.getCellTypeEnum() == CellType.NUMERIC) || (cell.getCellTypeEnum() == CellType.FORMULA)) {
                            switch (rowLabel) {
                                case CONSTANT:
                                    modeParams.setConstant(cell.getNumericCellValue());
                                    break;
                                case MARGINAL_UTILITY_OF_DISTANCE:
                                    modeParams.setMarginalUtilityOfDistance(cell.getNumericCellValue());
                                    break;
                                case MARGINAL_UTILITY_OF_TRAVELING:
                                    modeParams.setMarginalUtilityOfTraveling(cell.getNumericCellValue());
                                    break;
                                case MONETARY_DISTANCE_RATE:
                                    modeParams.setMonetaryDistanceRate(cell.getNumericCellValue());
                                    break;
                            }
                        }
                    }
                } else if (GENERAL_PARAMS.contains(rowLabel)) {
                    Cell cell = row.getCell(generalParamsCol);

                    if ((cell.getCellTypeEnum() == CellType.NUMERIC) || (cell.getCellTypeEnum() == CellType.FORMULA)) {
                        planCalcScore.addParam(rowLabel, String.valueOf(cell.getNumericCellValue()));
                    }
                }
            }
        }
    }

    protected static void parseBehaviorGroupParamsSheet(String behaviorGroupName, Sheet behaviorGroupParamsSheet, SBBBehaviorGroupsConfigGroup behaviorGroupsConfigGroup) {
        Map<Integer, String> modes = new TreeMap<>();
        final Set<String> ATTRIBUTE_VALUES = BEHAVIOR_GROUP_ATTRIBUTE_VALUES.get(behaviorGroupName);
        final String PERSON_ATTRIBUTE_KEY = BEHAVIOR_GROUP_PERSON_ATTRIBUTES.get(behaviorGroupName);
        Map<String, Map<String, SBBBehaviorGroupsConfigGroup.ModeCorrection>> modeCorrections = new HashMap<>();

        SBBBehaviorGroupsConfigGroup.BehaviorGroupParams behaviorGroupParams = new SBBBehaviorGroupsConfigGroup.BehaviorGroupParams();
        behaviorGroupParams.setBehaviorGroupName(behaviorGroupName);
        behaviorGroupParams.setPersonAttribute(PERSON_ATTRIBUTE_KEY);

        for (Row row : behaviorGroupParamsSheet) {
            Cell firstCell = row.getCell(0);

            if (firstCell != null) {
                String rowLabel = null;

                if (firstCell.getCellTypeEnum() == CellType.STRING) {
                    rowLabel = firstCell.getStringCellValue();
                } else if (firstCell.getCellTypeEnum() == CellType.NUMERIC) {
                    rowLabel = String.valueOf((int) firstCell.getNumericCellValue());
                } else {
                    continue;
                }

                if (rowLabel.equals(PERSON_ATTRIBUTE_KEY)) {
                    int lastColumn = row.getLastCellNum();

                    for (int col = 1; col < lastColumn; col++) {
                        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        if ((cell != null) && (cell.getCellTypeEnum() == CellType.STRING)) {
                            String mode = cell.getStringCellValue();

                            if ((Arrays.asList(MODES).contains(mode)) && (!modes.containsValue(mode))) {
                                modes.put(col, mode);
                            }
                        }
                    }
                } else if (ATTRIBUTE_VALUES.contains(rowLabel)) {
                    if (!modeCorrections.containsKey(rowLabel)) {
                        modeCorrections.put(rowLabel, new HashMap<String, SBBBehaviorGroupsConfigGroup.ModeCorrection>());

                        for (Map.Entry<Integer, String> modeEntry : modes.entrySet()) {
                            SBBBehaviorGroupsConfigGroup.ModeCorrection modeCorrection = new SBBBehaviorGroupsConfigGroup.ModeCorrection();
                            modeCorrection.setMode(modeEntry.getValue());

                            modeCorrections.get(rowLabel).put(modeEntry.getValue(), modeCorrection);
                        }
                    }

                    Cell secondCell = row.getCell(1);

                    if ((secondCell != null) && (secondCell.getCellTypeEnum() == CellType.STRING)) {
                        String parameterLabel = secondCell.getStringCellValue();

                        if (MODE_PARAMS.contains(parameterLabel)) {
                            for (Map.Entry<Integer, String> entry : modes.entrySet()) {
                                SBBBehaviorGroupsConfigGroup.ModeCorrection modeCorrection = modeCorrections.get(rowLabel).get(entry.getValue());
                                Cell cell = row.getCell(entry.getKey());

                                if ((cell.getCellTypeEnum() == CellType.NUMERIC) || (cell.getCellTypeEnum() == CellType.FORMULA)) {
                                    switch (parameterLabel) {
                                        case CONSTANT:
                                            modeCorrection.setConstant(cell.getNumericCellValue());
                                            break;
                                        case MARGINAL_UTILITY_OF_DISTANCE:
                                            modeCorrection.setMargUtilOfDistance(cell.getNumericCellValue());
                                            break;
                                        case MARGINAL_UTILITY_OF_TRAVELING:
                                            modeCorrection.setMargUtilOfTime(cell.getNumericCellValue());
                                            break;
                                        case MONETARY_DISTANCE_RATE:
                                            modeCorrection.setDistanceRate(cell.getNumericCellValue());
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Map<String, SBBBehaviorGroupsConfigGroup.ModeCorrection>> modeCorrectionsEntry : modeCorrections.entrySet()) {
            String personAttributeValue = modeCorrectionsEntry.getKey();
            Map<String, SBBBehaviorGroupsConfigGroup.ModeCorrection> modeCorrectionsPerMode = modeCorrectionsEntry.getValue();

            SBBBehaviorGroupsConfigGroup.PersonGroupTypes types = new SBBBehaviorGroupsConfigGroup.PersonGroupTypes();
            types.setPersonGroupType(personAttributeValue);

            for (SBBBehaviorGroupsConfigGroup.ModeCorrection modeCorrection : modeCorrectionsPerMode.values()) {
                if (modeCorrection.isSet()) {
                    types.addModeCorrection(modeCorrection);
                    log.info("adding modeCorrection for " + PERSON_ATTRIBUTE_KEY + "/" + personAttributeValue + " for mode " + modeCorrection.getMode());
                }
            }

            if (!types.getModeCorrectionParams().isEmpty()) {
                behaviorGroupParams.addPersonGroupType(types);
            }
        }

        behaviorGroupParams.setBehaviorTypes(modeCorrections.keySet());
        behaviorGroupsConfigGroup.addBehaviorGroupParams(behaviorGroupParams);
    }
}
