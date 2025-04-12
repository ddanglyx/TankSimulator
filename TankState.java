// javac TankState.java
// java TankState

import java.util.Map;

public class TankState {
    private float x, y, z, angle;
    private String color;


    public TankState(float x, float y, float z, float angle) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.angle = angle;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getAngle() { return angle; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public static TankState fromString(String stateString) {
        String[] parts = stateString.split(",");
        return new TankState(
            Float.parseFloat(parts[0]),
            Float.parseFloat(parts[1]),
            Float.parseFloat(parts[2]),
            Float.parseFloat(parts[3])
        );
    }

    public static String toBroadcastString(Map<String, TankState> tankStates) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, TankState> entry : tankStates.entrySet()) {
            sb.append(entry.getKey()).append(":")
              .append(entry.getValue().toString()).append(";");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z + "," + angle;
    }
    // Main method for testing
    public static void main(String[] args) {
        // Create a TankState instance
        TankState tank = new TankState(10.5f, 20.0f, 30.0f, 45.0f);
        System.out.println("TankState: " + tank);

        // Convert TankState to string and back
        String serialized = tank.toString();
        TankState deserialized = TankState.fromString(serialized);
        System.out.println("Deserialized TankState: " + deserialized);
    }
}