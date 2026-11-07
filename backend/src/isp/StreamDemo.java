package isp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small, runnable demonstration of the java.io stream classes used in the syllabus. */
public final class StreamDemo {
    private StreamDemo() { }

    public static void main(String[] args) throws IOException {
        Path file = Files.createTempFile("netwise-stream-demo-", ".dat");
        try {
            // FileOutputStream writes bytes; buffering reduces small write operations.
            try (DataOutputStream dataOut = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file.toFile())))) {
                dataOut.writeUTF("Netwise stream demo");
                dataOut.writeInt(42);
                dataOut.writeBoolean(true);
            }

            // FileInputStream reads bytes; buffered input wraps it, and DataInputStream
            // decodes the primitive values in the same order they were written.
            String label;
            int count;
            boolean active;
            try (DataInputStream dataIn = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(file.toFile())))) {
                label = dataIn.readUTF();
                count = dataIn.readInt();
                active = dataIn.readBoolean();
            }

            // PrintStream supplies print/println output; capture it so the demo can
            // show the exact formatted result using the console's PrintStream.
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            try (PrintStream report = new PrintStream(captured, true, "UTF-8")) {
                report.println("Label: " + label);
                report.println("Count: " + count);
                report.println("Active: " + active);
            }

            String output = captured.toString("UTF-8");
            if (!output.contains("Label: Netwise stream demo")
                    || !output.contains("Count: 42")
                    || !output.contains("Active: true")) {
                throw new IOException("Stream demo read-back check failed.");
            }
            System.out.print(output);
            System.out.println("PASS: buffered, data, file, and print streams worked.");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
