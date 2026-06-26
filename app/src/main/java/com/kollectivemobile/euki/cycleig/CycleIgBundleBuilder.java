package com.kollectivemobile.euki.cycleig;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.utils.Constants;

import java.util.Date;
import java.util.List;

public class CycleIgBundleBuilder {
    private static final String CYCLE_CANONICAL = "https://cycle.fhir.me";
    private static final String CYCLE_CODE_SYSTEM = CYCLE_CANONICAL + "/CodeSystem/cycle";
    private static final String EUKI_CODE_SYSTEM = "https://github.com/Euki-Inc/Euki-Android/CodeSystem/euki-cycle-ig";
    private static final String SNOMED = "http://snomed.info/sct";
    private static final String OBSERVATION_CATEGORY = "http://terminology.hl7.org/CodeSystem/observation-category";
    private static final String BUNDLE_PROFILE = CYCLE_CANONICAL + "/StructureDefinition/period-tracking-bundle";
    private static final String FACT_PROFILE = CYCLE_CANONICAL + "/StructureDefinition/period-tracking-fact";
    private static final String MENSTRUAL_BLEEDING_PROFILE = CYCLE_CANONICAL + "/StructureDefinition/menstrual-bleeding";
    private static final String MENSTRUAL_FLOW_PROFILE = CYCLE_CANONICAL + "/StructureDefinition/menstrual-flow";
    private static final String SYMPTOM_PROFILE = CYCLE_CANONICAL + "/StructureDefinition/symptom";

    private int observationIndex = 0;
    private JsonArray entries = new JsonArray();

    public String build(CycleIgSnapshot snapshot) {
        observationIndex = 0;
        entries = new JsonArray();

        for (CalendarItem item : snapshot.getItems()) {
            addBleedingFacts(item);
            if (snapshot.getScope().includeEmotions()) {
                addEmotionFacts(item);
            }
            if (snapshot.getScope().includeBody()) {
                addBodyFacts(item);
            }
            if (snapshot.getScope().includeNotes()) {
                addNoteFact(item);
            }
        }

        if (observationIndex == 0) {
            throw new IllegalArgumentException("SMART Link requires at least one recorded fact.");
        }

        JsonObject bundle = new JsonObject();
        bundle.addProperty("resourceType", "Bundle");
        bundle.addProperty("id", "euki-cycle-ig-export");
        JsonObject meta = new JsonObject();
        JsonArray profiles = new JsonArray();
        profiles.add(BUNDLE_PROFILE);
        meta.add("profile", profiles);
        bundle.add("meta", meta);
        bundle.addProperty("type", "collection");
        bundle.addProperty("timestamp", new Date().toInstant().toString());
        bundle.add("entry", entries);
        return new GsonBuilder().setPrettyPrinting().create().toJson(bundle);
    }

    private void addBleedingFacts(CalendarItem item) {
        if (!CycleIgSnapshotFactory.hasBleedingValue(item)) {
            return;
        }

        String date = CycleIgDates.formatDate(item.getDate());
        boolean menstrualBleeding = item.getIncludeCycleSummary();
        JsonObject bleeding = baseObservation(MENSTRUAL_BLEEDING_PROFILE, cycleCode("menstrual-bleeding", "Menstrual bleeding"), date);
        bleeding.addProperty("valueBoolean", menstrualBleeding);
        addObservation(bleeding);

        if (menstrualBleeding && item.getBleedingSize() != null) {
            Flow flow = flowFor(item.getBleedingSize());
            JsonObject flowObservation = baseObservation(MENSTRUAL_FLOW_PROFILE, cycleCode("menstrual-flow", "Patient-reported menstrual flow category"), date);
            flowObservation.add("valueCodeableConcept", codeableConcept(CYCLE_CODE_SYSTEM, flow.code, flow.display, flow.display.toLowerCase()));
            addObservation(flowObservation);
        }

        addCounterFacts(item, item.getBleedingProductsCounter(), Constants.BleedingProducts.values, "bleeding-product", "Bleeding product", date);
        addCounterFacts(item, item.getBleedingClotsCounter(), Constants.BleedingClots.values, "bleeding-clot", "Bleeding clot", date);
    }

    private void addCounterFacts(CalendarItem item, List<Integer> counters, Object[] values, String code, String display, String date) {
        if (counters == null) {
            return;
        }

        for (int index = 0; index < counters.size() && index < values.length; index++) {
            Integer count = counters.get(index);
            if (count == null || count <= 0) {
                continue;
            }

            String valueCode = code + "-" + (index + 1);
            String valueDisplay = values[index].toString().toLowerCase().replace('_', ' ');
            JsonObject observation = baseObservation(FACT_PROFILE, appCode(code, display), date);
            JsonObject value = codeableConcept(EUKI_CODE_SYSTEM, valueCode, valueDisplay, valueDisplay);
            value.addProperty("text", valueDisplay + " x" + count);
            observation.add("valueCodeableConcept", value);
            addObservation(observation);
        }
    }

    private void addEmotionFacts(CalendarItem item) {
        if (item.getEmotions() == null) {
            return;
        }

        for (Constants.Emotions emotion : item.getEmotions()) {
            addSymptom(item, "emotion." + emotion.name().toLowerCase(), label(emotion.name()), null, null);
        }
    }

    private void addBodyFacts(CalendarItem item) {
        if (item.getBody() == null) {
            return;
        }

        for (Constants.Body body : item.getBody()) {
            StandardCoding coding = standardBodyCoding(body);
            addSymptom(item, "body." + body.name().toLowerCase(), label(body.name()), coding, body == Constants.Body.SEVEREPAIN ? "Severe pain" : null);
        }
    }

    private void addNoteFact(CalendarItem item) {
        if (!item.hasNote()) {
            return;
        }

        JsonObject observation = baseObservation(FACT_PROFILE, appCode("daily-note", "Daily note"), CycleIgDates.formatDate(item.getDate()));
        observation.addProperty("valueString", item.getNote());
        addObservation(observation);
    }

    private void addSymptom(CalendarItem item, String code, String display, StandardCoding standardCoding, String text) {
        JsonObject observation = baseObservation(SYMPTOM_PROFILE, cycleCode("symptom", "Symptom"), CycleIgDates.formatDate(item.getDate()));
        JsonObject value = new JsonObject();
        JsonArray coding = new JsonArray();
        if (standardCoding != null) {
            coding.add(coding(standardCoding.system, standardCoding.code, standardCoding.display));
        }
        coding.add(coding(EUKI_CODE_SYSTEM, code, display));
        value.add("coding", coding);
        value.addProperty("text", text == null ? display : text);
        observation.add("valueCodeableConcept", value);
        addObservation(observation);
    }

    private JsonObject baseObservation(String profile, JsonObject code, String date) {
        JsonObject observation = new JsonObject();
        observation.addProperty("resourceType", "Observation");
        observation.addProperty("id", nextId());
        observation.addProperty("status", "final");
        JsonObject meta = new JsonObject();
        JsonArray profiles = new JsonArray();
        profiles.add(profile);
        meta.add("profile", profiles);
        observation.add("meta", meta);
        observation.add("category", surveyCategory());
        observation.add("code", code);
        observation.addProperty("effectiveDateTime", date);
        return observation;
    }

    private void addObservation(JsonObject observation) {
        JsonObject entry = new JsonObject();
        entry.addProperty("fullUrl", "urn:euki:cycle-ig:" + observation.get("id").getAsString());
        entry.add("resource", observation);
        entries.add(entry);
    }

    private String nextId() {
        observationIndex++;
        return String.format("obs-%04d", observationIndex);
    }

    private static JsonArray surveyCategory() {
        JsonArray category = new JsonArray();
        JsonObject item = new JsonObject();
        JsonArray coding = new JsonArray();
        coding.add(coding(OBSERVATION_CATEGORY, "survey", "Survey"));
        item.add("coding", coding);
        category.add(item);
        return category;
    }

    private static JsonObject cycleCode(String code, String display) {
        return codeableConcept(CYCLE_CODE_SYSTEM, code, display, display);
    }

    private static JsonObject appCode(String code, String display) {
        return codeableConcept(EUKI_CODE_SYSTEM, code, display, display);
    }

    private static JsonObject codeableConcept(String system, String code, String display, String text) {
        JsonObject concept = new JsonObject();
        JsonArray coding = new JsonArray();
        coding.add(coding(system, code, display));
        concept.add("coding", coding);
        concept.addProperty("text", text);
        return concept;
    }

    private static JsonObject coding(String system, String code, String display) {
        JsonObject coding = new JsonObject();
        coding.addProperty("system", system);
        coding.addProperty("code", code);
        coding.addProperty("display", display);
        return coding;
    }

    private static Flow flowFor(Constants.BleedingSize bleedingSize) {
        return switch (bleedingSize) {
            case SPOTING -> new Flow("flow-spotting", "Spotting");
            case LIGHT -> new Flow("flow-light", "Light");
            case MEDIUM -> new Flow("flow-moderate", "Moderate");
            case HEAVY -> new Flow("flow-heavy", "Heavy");
        };
    }

    private static StandardCoding standardBodyCoding(Constants.Body body) {
        return switch (body) {
            case CRAMPS -> new StandardCoding(SNOMED, "431416001", "Menstrual cramp");
            case HEADACHE -> new StandardCoding(SNOMED, "25064002", "Headache");
            case FATIGUE -> new StandardCoding(SNOMED, "84229001", "Fatigue");
            case NAUSEAS -> new StandardCoding(SNOMED, "422587007", "Nausea");
            case TENDERBREASTS -> new StandardCoding(SNOMED, "55222007", "Breast tenderness");
            case FEVER -> new StandardCoding(SNOMED, "386661006", "Fever");
            case ACNE -> new StandardCoding(SNOMED, "11381005", "Acne");
            default -> null;
        };
    }

    private static String label(String name) {
        return name.toLowerCase().replace('_', ' ');
    }

    private static class Flow {
        final String code;
        final String display;

        Flow(String code, String display) {
            this.code = code;
            this.display = display;
        }
    }

    private static class StandardCoding {
        final String system;
        final String code;
        final String display;

        StandardCoding(String system, String code, String display) {
            this.system = system;
            this.code = code;
            this.display = display;
        }
    }
}
