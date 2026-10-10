package rw.ac.auca.scoutpro_27202.messaging;

// Builds a small JSON object of plain values for the audit log.
// Entities are not serialized whole: they contain loops (an athlete points at a sport
// that is not needed in the audit row).
public final class Snapshots {

    private Snapshots() {}

    public static String of(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Snapshots.of needs key, value, key, value");
        }
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(escape(String.valueOf(pairs[i]))).append("\":");
            Object value = pairs[i + 1];
            if (value == null) {
                json.append("null");
            } else {
                json.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        json.append('}');
        return json.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
