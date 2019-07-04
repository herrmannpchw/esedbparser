import org.apache.commons.cli.*;
import com.google.inject.Inject;

public class EsedbCli extends DefaultParser {

    private final String[] args;

    @Inject
    public EsedbCli(String... args) {
        super();
        this.args = args;

        options = new Options();
        options.addOption("f", "file", true, "file ESEDB to parse web history.");
        options.addOption("i", "info", false, "show ESEDB file info.");
        options.addOption("t", "table", true, "specify table to list.");
    }

    public String[] getArgs() {
        return this.args;
    }

    public Options getOptions() {
        return options;
    }

    public CommandLine parse() throws ParseException {
        return parse(options, args);
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        formatter.printHelp( "esedbParser", options );
    }
}
