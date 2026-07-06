import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class NativeHost {

    // Raw stdin/stdout captured BEFORE we redirect System.out/System.err.
    // All Native Messaging framing goes through these, so nothing else the JVM
    // or Swing might print can ever corrupt Chrome's pipe.
    private static InputStream  chromeIn;
    private static OutputStream chromeOut;

    public static void main(String[] args) {
        // Capture the real OS-level stdin/stdout (fd 0 and fd 1) first.
        chromeIn  = new FileInputStream(FileDescriptor.in);
        chromeOut = new FileOutputStream(FileDescriptor.out);

        // Route every other stream to the log file. After this point a stray
        // System.out.println or a JVM/Swing warning can never reach Chrome.
        redirectStandardStreams();

        try {
            String message = readMessage();
            if (message == null) {
                sendMessage(error("No message received"));
                halt(0);
            }
            String action = extractString(message, "action");
            if (action == null) action = "open";

            switch (action) {
                case "browse": handleBrowse();                             break;
                case "open":   handleOpen(extractString(message, "path")); break;
                default:       sendMessage(error("Unknown action: " + action));
            }
        } catch (Exception e) {
            try { sendMessage(error(e.getMessage())); } catch (Exception ignored) {}
        }
        // halt() terminates immediately without running AWT/Swing shutdown
        // hooks, which can otherwise deadlock and leave the process (and
        // Chrome's pipe handle) alive.
        halt(0);
    }

    private static void halt(int code) {
        try { chromeOut.flush(); } catch (Exception ignored) {}
        Runtime.getRuntime().halt(code);
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private static void handleBrowse() throws Exception {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        final String[] result = {null};

        SwingUtilities.invokeAndWait(() -> {
            // Invisible owner frame keeps the dialog on top of all windows
            JFrame owner = new JFrame();
            owner.setUndecorated(true);
            owner.setAlwaysOnTop(true);
            owner.setLocationRelativeTo(null);
            owner.setVisible(true);

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Folder");
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
                result[0] = chooser.getSelectedFile().getAbsolutePath();
            }
            owner.dispose();
        });

        if (result[0] != null) {
            sendMessage("{\"success\":true,\"path\":\"" + escapeJson(result[0]) + "\"}");
        } else {
            sendMessage("{\"success\":false,\"message\":\"cancelled\"}");
        }
    }

    private static void handleOpen(String path) throws Exception {
        if (path == null || path.isBlank()) {
            sendMessage(error("No path provided"));
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            sendMessage(error("Path does not exist: " + path));
            return;
        }
        Runtime.getRuntime().exec(new String[]{"explorer", file.getAbsolutePath()});
        sendMessage("{\"success\":true,\"message\":\"Opened: " + escapeJson(path) + "\"}");
    }

    // ── Native Messaging protocol (4-byte LE length prefix) ──────────────────

    private static String readMessage() throws IOException {
        byte[] lenBytes = chromeIn.readNBytes(4);
        if (lenBytes.length < 4) return null;
        int length = ByteBuffer.wrap(lenBytes).order(ByteOrder.nativeOrder()).getInt();
        byte[] data = chromeIn.readNBytes(length);
        return new String(data, StandardCharsets.UTF_8);
    }

    private static void sendMessage(String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        byte[] len  = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(data.length).array();
        chromeOut.write(len);
        chromeOut.write(data);
        chromeOut.flush();
    }

    // ── Minimal JSON helpers (no external deps) ───────────────────────────────

    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        i += needle.length();
        while (i < json.length() && json.charAt(i) != ':') i++;
        i++;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    default:  sb.append(next);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String error(String msg) {
        return "{\"success\":false,\"message\":\"" + escapeJson(msg == null ? "unknown" : msg) + "\"}";
    }

    // ── Route System.out/System.err to a log so they never hit Chrome's pipe ──

    private static void redirectStandardStreams() {
        try {
            String jar = NativeHost.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            File log = new File(new File(jar).getParent(), "native_host.log");
            PrintStream logStream = new PrintStream(new FileOutputStream(log, true), true);
            System.setOut(logStream);
            System.setErr(logStream);
        } catch (Exception ignored) {}
    }
}
