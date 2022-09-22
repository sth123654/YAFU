package pro.javacard.fido2.cli;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.win32.StdCallLibrary;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class CommandLineInterface {
    static final String VERSION = "0.1";
    protected static OptionParser parser = new OptionParser();

    // Generic options
    protected static OptionSpec<Void> OPT_VERSION = parser.acceptsAll(Arrays.asList("V", "version"), "Show program version");
    protected static OptionSpec<Void> OPT_DEBUG = parser.acceptsAll(Arrays.asList("debug"), "Show wire traces");
    protected static OptionSpec<Void> OPT_VERBOSE = parser.acceptsAll(Arrays.asList("v", "verbose"), "Show CBOR messages");

    protected static OptionSpec<Void> OPT_LIST_CREDENTIALS = parser.acceptsAll(Arrays.asList("l", "list-credentials"), "List credentials (pre)");
    protected static OptionSpec<Void> OPT_LIST_FINGERPRINTS = parser.acceptsAll(Arrays.asList("F", "list-fingerprints"), "List fingerprints (pre, WIP)");
    protected static OptionSpec<String> OPT_DELETE = parser.acceptsAll(Arrays.asList("D", "delete"), "Delete credential (pre)").withRequiredArg().describedAs("user@domain|credential");

    protected static OptionSpec<Void> OPT_WINK = parser.acceptsAll(Arrays.asList("W", "wink"), "Wink ;)");

    protected static OptionSpec<Void> OPT_HELP = parser.acceptsAll(Arrays.asList("h", "help"), "Show information about the program").forHelp();
    protected static OptionSpec<String> OPT_NFC = parser.acceptsAll(Arrays.asList("N", "nfc"), "Use specific NFC reader").withOptionalArg().describedAs("reader");

    protected static OptionSpec<String> OPT_TCP = parser.acceptsAll(Arrays.asList("T", "tcp"), "Use APDU over TCP (test)").withOptionalArg().describedAs("host:port");

    protected static OptionSpec<String> OPT_USB = parser.acceptsAll(Arrays.asList("U", "usb"), "Use specific USB HID device").withOptionalArg().describedAs("device");
    protected static OptionSpec<String> OPT_CHANGE_PIN = parser.acceptsAll(Arrays.asList("change-pin"), "Set new PIN (FIDO2)").withRequiredArg().describedAs("new PIN");
    protected static OptionSpec<String> OPT_PIN = parser.acceptsAll(Arrays.asList("p", "pin"), "Use PIN (FIDO2)").withOptionalArg().describedAs("PIN");

    protected static OptionSpec<Void> OPT_U2F = parser.acceptsAll(Arrays.asList("1", "u2f"), "Force use of U2F");

    // CTAP2/CTAP2 commands
    protected static OptionSpec<Void> OPT_GET_INFO = parser.acceptsAll(Arrays.asList("i", "info"), "Get info (FIDO2)");
    protected static OptionSpec<String> OPT_REGISTER = parser.acceptsAll(Arrays.asList("r", "register"), "Make credential / register").withRequiredArg().describedAs("[user@]domain");

    protected static OptionSpec<Void> OPT_RK = parser.acceptsAll(Arrays.asList("rk", "discoverable"), "Discoverable (FIDO2)");
    protected static OptionSpec<String> OPT_HMAC_SECRET = parser.acceptsAll(Arrays.asList("hmac-secret"), "Use hmac-secret (FIDO2)").withOptionalArg().describedAs("hex");
    protected static OptionSpec<Integer> OPT_PROTECT = parser.acceptsAll(Arrays.asList("protect"), "Use credProtect (FIDO2)").withRequiredArg().ofType(Integer.class);
    protected static OptionSpec<String> OPT_UID = parser.accepts("uid", "User identifier").withRequiredArg().describedAs("uid (hex)"); // FIXME: hex

    protected static OptionSpec<String> OPT_PUBKEY = parser.acceptsAll(Arrays.asList("pubkey"), "Credential public key").withRequiredArg().describedAs("hex/file");

    protected static OptionSpec<String> OPT_AUTHENTICATE = parser.acceptsAll(Arrays.asList("a", "authenticate"), "Get assertion / authenticate").withRequiredArg().describedAs("[user@]domain");

    protected static OptionSpec<String> OPT_CREDENTIAL = parser.acceptsAll(Arrays.asList("c", "credential"), "CredentialID").withRequiredArg().describedAs("hex/file"); // FIXME: hex
    protected static OptionSpec<Void> OPT_NO_UP = parser.acceptsAll(Arrays.asList("no-up", "no-presence"), "Do not require UP (touch)");
    protected static OptionSpec<Void> OPT_UV = parser.acceptsAll(Arrays.asList("uv", "verification"), "Do UV (PIN/biometrics)");

    // X-FIDO commands
    protected static OptionSpec<String> OPT_X_AUTH = parser.acceptsAll(Arrays.asList("x-auth"), "Use admin secret (X-FIDO)").withRequiredArg().describedAs("secret");

    protected static OptionSpec<Void> OPT_X_INFO = parser.acceptsAll(Arrays.asList("x-info"), "Show token info (X-FIDO)");

    protected static OptionSpec<String> OPT_X_CREATE = parser.acceptsAll(Arrays.asList("x-create"), "Create something (X-FIDO)").withRequiredArg().describedAs("type");
    protected static OptionSpec<String> OPT_X_READ = parser.accepts("x-read", "Read something (X-FIDO)").withRequiredArg().describedAs("type");
    protected static OptionSpec<String> OPT_X_UPDATE = parser.acceptsAll(Arrays.asList("x-update"), "Update something (X-FIDO)").withRequiredArg().describedAs("type");
    protected static OptionSpec<String> OPT_X_DELETE = parser.acceptsAll(Arrays.asList("x-delete"), "Delete something (X-FIDO)").withRequiredArg().describedAs("type");
    protected static OptionSpec<String> OPT_X_LIST = parser.acceptsAll(Arrays.asList("x-list"), "List things (X-FIDO)").withRequiredArg().describedAs("type");

    // Options to disable/block things
    protected static OptionSpec<Boolean> OPT_X_DISABLED = parser.acceptsAll(Arrays.asList("disabled"), "Disable it (X-FIDO)").withRequiredArg().describedAs("true/false").ofType(Boolean.class);
    protected static OptionSpec<Boolean> OPT_X_BLOCKED = parser.acceptsAll(Arrays.asList("blocked"), "Block it (X-FIDO)").withRequiredArg().describedAs("true/false").ofType(Boolean.class);

    protected static <V> Optional<V> optional(OptionSet args, OptionSpec<V> v) {
        return args.hasArgument(v) ? Optional.ofNullable(args.valueOf(v)) : Optional.empty();
    }

    protected static OptionSet parseArguments(String[] argv) throws IOException {
        OptionSet args = null;

        // Parse arguments
        try {
            args = parser.parse(argv);
        } catch (OptionException e) {
            parser.printHelpOn(System.err);
            System.err.println();
            if (e.getCause() != null) {
                System.err.println(e.getMessage() + ": " + e.getCause().getMessage());
            } else {
                System.err.println(e.getMessage());
            }
            exitWith(1);
            throw new RuntimeException("Never reached but makes spotbugs happy."); // FIXME
        }

        if (args.nonOptionArguments().size() > 0) {
            System.err.println();
            System.err.println("Invalid non-option arguments: " + args.nonOptionArguments().stream().map(e -> e.toString()).collect(Collectors.joining(" ")));
            System.err.println("Try " + argv[0] + " --help");
            exitWith(1);
        }

        if (args.has(OPT_HELP) || args.specs().size() == 0) {
            parser.printHelpOn(System.out);
            if (Platform.isWindows()) {
                System.out.println();
                System.out.println("NB! This tool must be run as an Administrator on Windows!");
            }
            exitWith(0);
        }

        return args;
    }


    static void exitWith(int code) {
        if (Platform.isWindows()) {
            // If run via wrapper and uac was triggered to open a new window, then PROMPT is not present
            if (System.getenv().containsKey("FIDO_EXE_WRAPPER") && !System.getenv().containsKey("PROMPT")) {
                System.console().readLine("Press ENTER to close this window ...");
            }
        }
        System.exit(code);
    }

    static boolean requiresPIN(OptionSet options) {
        return options.has(OPT_CHANGE_PIN) || options.has(OPT_LIST_CREDENTIALS) || options.has(OPT_LIST_FINGERPRINTS) || options.has(OPT_DELETE) || options.has(OPT_REGISTER);
    }


    // See https://stackoverflow.com/questions/18631597/java-on-windows-test-if-a-java-application-is-run-as-an-elevated-process-with
    interface Shell32 extends StdCallLibrary {
        boolean IsUserAnAdmin() throws LastErrorException;
    }

    static final Shell32 INSTANCE = Platform.isWindows() ? Native.load("shell32", Shell32.class) : null;

    static boolean isUserWindowsAdmin() {
        return INSTANCE != null && INSTANCE.IsUserAnAdmin();
    }
}
