
import java.util.*;
import java.util.stream.Collectors;

/** ---- IOperation (interface) ---- */
interface IOperation {
    String getId();
    String getDescription();

    /** Nominal/expected duration of this operation (minutes). */
    double getNominalTimeMinutes();

    /** Actual realized duration (minutes). By default equal to nominal, but
     *  operations may override (e.g., use AGV speed or distance). */
    double getDurationMinutes();

    /** AGVs required to execute this operation. */
    List<AGV> getResources();

    /** Simple key-value storage to mimic setData/getData from the UML. */
    void setData(String key, Object value);
    Object getData(String key);
}

/** ---- A convenient base class to hold common bits for operations ---- */
abstract class BaseOperation implements IOperation {
    private final String id;
    private final String description;
    protected double nominalMinutes;
    protected final List<AGV> resources = new ArrayList<>();
    private final Map<String, Object> data = new HashMap<>();

    protected BaseOperation(String id, String description, double nominalMinutes, List<AGV> resources) {
        this.id = id;
        this.description = description;
        this.nominalMinutes = nominalMinutes;
        if (resources != null) this.resources.addAll(resources);
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public double getNominalTimeMinutes() { return nominalMinutes; }
    public List<AGV> getResources() { return Collections.unmodifiableList(resources); }

    public void setData(String key, Object value) { data.put(key, value); }
    public Object getData(String key) { return data.get(key); }
}

/** ---- Example operation type: Load/Unload that just takes time ---- */
class LoadUnloadOperation extends BaseOperation {
    public LoadUnloadOperation(String id, String description, double nominalMinutes, List<AGV> resources) {
        super(id, description, nominalMinutes, resources);
    }
    @Override public double getDurationMinutes() { return getNominalTimeMinutes(); }
}

/** ---- Example operation type: Transport using distance and AGV speed ----
 * Duration = (distance meters) / (AGV actual speed m/s) -> minutes
 */
class TransportOperation extends BaseOperation {
    private final double distanceMeters;

    public TransportOperation(String id, String description, double distanceMeters, AGV agv) {
        super(id, description, /* nominal as a hint */ 0.0, List.of(agv));
        this.distanceMeters = distanceMeters;
        setData("distance_m", distanceMeters);
    }

    @Override
    public double getDurationMinutes() {
        AGV agv = getResources().get(0);
        double speedMps = agv.getActSpeedMps();
        if (speedMps <= 0) return Double.POSITIVE_INFINITY;
        double minutes = (distanceMeters / speedMps) / 60.0;
        // add a simple fixed overhead to reflect start/stop (optional)
        minutes += 0.5;
        return minutes;
    }

    @Override
    public double getNominalTimeMinutes() {
        // For transport we compute from speed, not a fixed nominal
        return getDurationMinutes();
    }
}

/** ---- AGV entity ----
 * Minimal energy model: energy (kWh) = (duration_hours) * (consumption_kW)
 */
class AGV {
    private final String id;
    private double batteryLoadKWh;      // current battery charge (kWh), optional in this model
    private final double consumptionKw; // kW while operating (kWh per hour)
    private final double chargingTimeMin;
    private final String position;      // textual placeholder
    private final float maxSpeedMps;
    private float actSpeedMps;

    public AGV(String id,
               double batteryLoadKWh,
               double consumptionKw,
               double chargingTimeMin,
               String position,
               float maxSpeedMps,
               float actSpeedMps) {
        this.id = id;
        this.batteryLoadKWh = batteryLoadKWh;
        this.consumptionKw = consumptionKw;
        this.chargingTimeMin = chargingTimeMin;
        this.position = position;
        this.maxSpeedMps = maxSpeedMps;
        this.actSpeedMps = actSpeedMps;
    }

    public String getId() { return id; }
    public double getBatteryLoadKWh() { return batteryLoadKWh; }
    public void setBatteryLoadKWh(double batteryLoadKWh) { this.batteryLoadKWh = batteryLoadKWh; }
    public double getConsumptionKw() { return consumptionKw; }
    public double getChargingTimeMin() { return chargingTimeMin; }
    public String getPosition() { return position; }
    public float getMaxSpeedMps() { return maxSpeedMps; }
    public float getActSpeedMps() { return actSpeedMps; }
    public void setActSpeedMps(float s) { this.actSpeedMps = s; }

    /** Energy used over a duration (minutes). */
    public double energyForMinutes(double minutes) {
        return (minutes / 60.0) * consumptionKw;
    }

    @Override public String toString() { return "AGV(" + id + ")"; }
}

/** ---- IndustrialProcess ---- */
class IndustrialProcess {
    private final String id;
    private final List<IOperation> operations = new ArrayList<>();

    public IndustrialProcess(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public void addOperation(IOperation op) { operations.add(op); }

    public List<IOperation> getOperations() { return Collections.unmodifiableList(operations); }

    /** Total process duration (simple model: operations run sequentially). */
    public double getProcessDurationMinutes() {
        return operations.stream().mapToDouble(IOperation::getDurationMinutes).sum();
    }

    /** Distinct AGVs needed by the process. */
    public Set<AGV> getProcessResources() {
        return operations.stream()
                .flatMap(op -> op.getResources().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Total energy consumed by all AGVs across all operations (kWh). */
    public double getEnergyConsumptionKWh() {
        double total = 0.0;
        for (IOperation op : operations) {
            double minutes = op.getDurationMinutes();
            for (AGV agv : op.getResources()) {
                total += agv.energyForMinutes(minutes);
            }
        }
        return total;
    }

    /** Pretty print a short report. */
    public void printReport() {
        System.out.println("=== IndustrialProcess: " + id + " ===");
        for (IOperation op : operations) {
            String agvNames = op.getResources().stream().map(AGV::getId).collect(Collectors.joining(", "));
            System.out.printf(Locale.US, "- op[%s] %-18s duration=%.2f min  AGVs=[%s]%n",
                    op.getId(), op.getDescription(), op.getDurationMinutes(), agvNames);
        }
        System.out.printf(Locale.US, "Total duration: %.2f minutes%n", getProcessDurationMinutes());
        System.out.printf("AGVs required: %d -> %s%n", getProcessResources().size(),
                getProcessResources().stream().map(AGV::getId).collect(Collectors.joining(", ")));
        System.out.printf(Locale.US, "Energy consumption: %.3f kWh%n", getEnergyConsumptionKWh());
        System.out.println();
    }
}

/** ---- Minimal simulation driver ---- */
public class Main {
    public static void main(String[] args) {
        // Create a few AGVs
        AGV agvA = new AGV("AGV-A", 15.0, /*kW*/ 2.0, 30.0, "Dock-1", 2.0f, 1.2f);
        AGV agvB = new AGV("AGV-B", 15.0, /*kW*/ 1.8, 30.0, "Dock-2", 2.0f, 1.0f);

        // --- Process 1: Receive -> Transport -> Unload ---
        IndustrialProcess inbound = new IndustrialProcess("Inbound-Receiving");
        inbound.addOperation(new LoadUnloadOperation("OP-1", "Dock receive", 6.0, List.of(agvA)));
        inbound.addOperation(new TransportOperation("OP-2", "Move pallets to storage (120 m)", 120.0, agvA));
        inbound.addOperation(new LoadUnloadOperation("OP-3", "Putaway at rack", 4.0, List.of(agvA)));

        // --- Process 2: Pick -> Transport -> Pack (uses a different AGV) ---
        IndustrialProcess outbound = new IndustrialProcess("Outbound-Picking");
        outbound.addOperation(new LoadUnloadOperation("OP-4", "Pick at rack", 5.0, List.of(agvB)));
        outbound.addOperation(new TransportOperation("OP-5", "Move to packing (150 m)", 150.0, agvB));
        outbound.addOperation(new LoadUnloadOperation("OP-6", "Packing", 7.0, List.of(agvB)));

        // Print reports
        inbound.printReport();
        outbound.printReport();

        // Example: several processes together (batch)
        List<IndustrialProcess> batch = List.of(inbound, outbound);
        double totalMinutes = batch.stream().mapToDouble(IndustrialProcess::getProcessDurationMinutes).sum();
        double totalEnergy = batch.stream().mapToDouble(IndustrialProcess::getEnergyConsumptionKWh).sum();
        Set<AGV> allAgvs = batch.stream().flatMap(p -> p.getProcessResources().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        System.out.println("=== Batch Summary ===");
        System.out.printf(Locale.US, "Processes: %d | Total time: %.2f min | Energy: %.3f kWh | Distinct AGVs: %s%n",
                batch.size(), totalMinutes, totalEnergy,
                allAgvs.stream().map(AGV::getId).collect(Collectors.joining(", ")));
    }
}
