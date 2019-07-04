import com.google.common.base.Strings;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/*
 * Esedd parser using libesedb.
 * https://github.com/libyal/libesedb/wiki/Development
 */
public class EsedbParser {

    static void printError(String function, int result, PointerByReference errorPointer) {
        System.out.println("Função: " + function);
        System.out.println("Resultado: " + result);
        System.out.println("Erro: " + errorPointer.getValue().getString(0));
        System.out.println("=============================");
        EsedbLibrary.INSTANCE.libesedb_error_free(errorPointer);
        
    }

    static String convertLDAPTimeToString(long nanoseconds) {
        /* Convert LDAP Timestamp to human readable date
         * https://www.epochconverter.com/ldap
         * http://goliferay.blogspot.com/2015/11/convert-18-digit-ldap-timestamps-to.html
         */
        long mills = (nanoseconds / 10000000);
        long unix = (((1970 - 1601) * 365) - 3 + Math.round((1970 - 1601) / 4)) * 86400L;
        long timeStamp = mills - unix;
        Date date = new Date(timeStamp * 1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // give a timezone reference for formating (see comment at the bottom
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    public static void main(String[] args) {

        String filename = "/home/herrmann/Documents/BrowsersArtifacts/Edge/WebCacheV01.dat";
        String table = "";
        Boolean info = false;
        int contagemAbertura = 0;
        int contagemFechamentos = 0;

        EsedbCli cmd = new EsedbCli(args);
        CommandLine cmdLine = null;

        try {
            cmdLine = cmd.parse();
        } catch (ParseException ex) {
            System.err.println("Erro: " + ex);
            System.exit(1);
        }

        if (cmdLine.hasOption("f")) {
            filename = cmdLine.getOptionValue("f");
            if (!new File(filename).exists()) {
                System.err.println("Arquivo não existe!");
                System.exit(0);
            }
        } else {
            cmd.printHelp();
            System.exit(0);
        }

        if (cmdLine.hasOption("t")) {
            table = cmdLine.getOptionValue("t");
        }

        if (cmdLine.hasOption("i")) {
            info = true;
        }

        System.out.println("Versão da biblioteca: " + EsedbLibrary.INSTANCE.libesedb_get_version());
        System.out.println("=============================");

        /*
         * Variables used by file functions
         */

        PointerByReference filePointerReference = new PointerByReference();
        PointerByReference columnPointerReference = new PointerByReference();
        PointerByReference errorPointer = new PointerByReference();
        IntByReference numberOfTables = new IntByReference();
        int numColumns;
        int numTables;

        /* Table Container_n columns names
         * 0 - EntryId
         * 1 - ContainerId
         * 2 - CacheId
         * 3 - UrlHash
         * 4 - SecureDirectory
         * 5 - FileSize
         * 6 - Type
         * 7 - Flags
         * 8 - AccessCount
         * 9 - SyncTime
         * 10- CreationTime
         * 11- ExpiryTime
         * 12 - ModifiedTime
         * 13 - AccessedTime
         * 14 - PostCheckTime
         * 15 - SyncCount
         * 16 - ExemptionDelta
         * 17 - Url
         * 18 - Filename
         * 19 - FileExtension
         * 20 - RequestHeaders
         * 21 - ResponseHeaders
         * 22 - RedirectUrl
         * 23 - Group
         * 24 - ExtraData
         */

        int[] recordColumnsValues = new int[]{0, 5, 8, 10, 12, 13, 17, 18};

        int accessFlags = 1;
        int columnFlags = 1;
        int result = 0;

        System.out.println("Nome do arquivo: " + filename);
        System.out.println("=============================");

        result = EsedbLibrary.INSTANCE.libesedb_file_initialize(filePointerReference, errorPointer);
        if (result < 0) printError("File Initialize", result, errorPointer);

        result = EsedbLibrary.INSTANCE.libesedb_check_file_signature(filename, errorPointer);
        if (result < 0) printError("Check File Signature", result, errorPointer);
        if (result == 0) {
            System.out.println("File does not contains an ESEDB");
            System.out.println("=============================");
            System.exit(0);
        }

        result = EsedbLibrary.INSTANCE.libesedb_file_open(filePointerReference.getValue(), filename, accessFlags, errorPointer);
        if (result < 0) printError("File Open", result, errorPointer);
        contagemAbertura++;

        result = EsedbLibrary.INSTANCE.libesedb_file_get_number_of_tables(filePointerReference.getValue(), numberOfTables, errorPointer);
        if (result < 0) printError("File Get Number of Tables", result, errorPointer);
        numTables = numberOfTables.getValue();
        System.out.println("Number of tables: " + numTables);
        System.out.println("=============================");

        for (int tables = 0; tables < numTables; tables++) {

            /*
             * Variables used by table functions
             */
            PointerByReference tablePointerReference = new PointerByReference();

            IntByReference tableName = new IntByReference();
            IntByReference tableNameSize = new IntByReference();

            IntByReference numberOfColumns = new IntByReference();
            LongByReference numberOfRecords = new LongByReference();

            IntByReference columnType = new IntByReference();
            IntByReference columnName = new IntByReference();
            IntByReference columnNameSize = new IntByReference();

            long numRecords;

            if (info) {
                System.out.println();
                System.out.println("-----------------------------");
                System.out.println("-------- TABLE INFO ---------");
                System.out.println("-----------------------------");
                System.out.println();
            }

            result = EsedbLibrary.INSTANCE.libesedb_file_get_table(filePointerReference.getValue(), tables, tablePointerReference, errorPointer);
            if (result < 0) printError("File Get Table", result, errorPointer);
            contagemAbertura++;

            result = EsedbLibrary.INSTANCE.libesedb_table_get_utf8_name_size(tablePointerReference.getValue(), tableNameSize, errorPointer);
            if (result < 0) printError("Table Get UTF8 Name Size", result, errorPointer);
//            System.out.println("Table name size: " + tableNameSize.getValue());

            result = EsedbLibrary.INSTANCE.libesedb_table_get_utf8_name(tablePointerReference.getValue(), tableName, tableNameSize.getValue(), errorPointer);
            if (result < 0) printError("Table Get UTF8 Name", result, errorPointer);
            String tableNameString = tableName.getPointer().getString(0);
            if (info) System.out.println("Table name: " + tableNameString);

            result = EsedbLibrary.INSTANCE.libesedb_table_get_number_of_columns(tablePointerReference.getValue(), numberOfColumns, columnFlags, errorPointer);
            if (result < 0) printError("Table Get Number of Columns", result, errorPointer);
            if (info) System.out.println("Number of columns: " + numberOfColumns.getValue());

            result = EsedbLibrary.INSTANCE.libesedb_table_get_number_of_records(tablePointerReference.getValue(), numberOfRecords, errorPointer);
            if (result < 0) printError("Table Get Number of Records", result, errorPointer);
            numRecords = numberOfRecords.getValue();
            if (info) System.out.println("Number of records (rows): " + numRecords);

            if (Strings.isNullOrEmpty(table)) {
                table = "Container_";
            }

            if (tableNameString.contains(table)) {

                System.out.println();
                System.out.println("-----------------------------");
                System.out.println("-------- TABLE DATA ---------");
                System.out.println("-----------------------------");
                System.out.println();


                for (int i = 0; i < numRecords; i++) {

                    /*
                     * Variables used by record functions
                     */
                    PointerByReference recordPointerReference = new PointerByReference();
                    IntByReference recordNumberOfValues = new IntByReference();

                    /*
                     * Variables used by value functions
                     */
                    IntByReference recordValueDataSize = new IntByReference();
                    IntByReference recordValueData32 = new IntByReference();
                    IntByReference valueDataFlags = new IntByReference();
                    LongByReference recordValueData = new LongByReference();
//                    ShortByReference recordValueDataShort = new ShortByReference();
//                    ByteByReference recordValueDataByte = new ByteByReference();
//                    IntByReference recordValueDataInt = new IntByReference();
                    Memory recordValueDataUrl = new Memory(3072);
                    Memory recordValueDataFilename = new Memory(1024);

                    /* Valor dos campos da tabela */
                    long entryId = 0;
                    long fileSize = 0;
                    long accessCount = 0;
                    String modifiedTime = "";
                    String accessedTime = "";
                    String creationTime = "";
                    String url = "";
                    String file = "";

//                    System.out.println("Entrou tabela com valores. " + (i + 1) + " de " + numRecords);

                    result = EsedbLibrary.INSTANCE.libesedb_table_get_record(tablePointerReference.getValue(), i, recordPointerReference, errorPointer);
                    if (result < 0) printError("Table Get Record", result, errorPointer);
                    contagemAbertura++;

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_number_of_values(recordPointerReference.getValue(), recordNumberOfValues, errorPointer);
                    if (result < 0) printError("Record Get Number of Values", result, errorPointer);
//                    System.out.println("Record number of Values: " + recordNumberOfValues.getValue());
//                    System.out.println();

//                    for (int recordValueEntry : recordColumnsValues) {

                    /*
                     * The column types
                     *
                     *   enum LIBESEDB_COLUMN_TYPES
                     *   {
                     *       LIBESEDB_COLUMN_TYPE_NULL			= 0,
                     *       LIBESEDB_COLUMN_TYPE_BOOLEAN			= 1,
                     *       LIBESEDB_COLUMN_TYPE_INTEGER_8BIT_UNSIGNED	= 2,
                     *       LIBESEDB_COLUMN_TYPE_INTEGER_16BIT_SIGNED	= 3,
                     *       LIBESEDB_COLUMN_TYPE_INTEGER_32BIT_SIGNED	= 4,
                     *       LIBESEDB_COLUMN_TYPE_CURRENCY			= 5,
                     *       LIBESEDB_COLUMN_TYPE_FLOAT_32BIT		= 6,
                     *       LIBESEDB_COLUMN_TYPE_DOUBLE_64BIT		= 7,
                     *       LIBESEDB_COLUMN_TYPE_DATE_TIME			= 8,
                     *       LIBESEDB_COLUMN_TYPE_BINARY_DATA		= 9,
                     *       LIBESEDB_COLUMN_TYPE_TEXT			= 10,
                     *       LIBESEDB_COLUMN_TYPE_LARGE_BINARY_DATA		= 11,
                     *       === Url, Filename ===
                     *       LIBESEDB_COLUMN_TYPE_LARGE_TEXT			= 12,
                     *       LIBESEDB_COLUMN_TYPE_SUPER_LARGE_VALUE		= 13,
                     *       === AccessCount ===
                     *       LIBESEDB_COLUMN_TYPE_INTEGER_32BIT_UNSIGNED	= 14,
                     *       === EntryId, FileSize, CreationTime, ModifiedTime, AccessedTime ===
                     *       LIBESEDB_COLUMN_TYPE_INTEGER_64BIT_SIGNED	= 15,
                     *       LIBESEDB_COLUMN_TYPE_GUID			= 16,
                     *       LIBESEDB_COLUMN_TYPE_INTEGER_16BIT_UNSIGNED	= 17
                     *   };
                     */

                    /*
                     * Get values of interest
                     */

                    /* Integer 64bit signed */
                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_64bit(recordPointerReference.getValue(), 0, recordValueData, errorPointer);
                    if (result < 0) printError("Record Get EntryId Data", result, errorPointer);
                    entryId = recordValueData.getValue();

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_64bit(recordPointerReference.getValue(), 5, recordValueData, errorPointer);
                    if (result < 0) printError("Record Get FileSize Data", result, errorPointer);
                    fileSize = recordValueData.getValue();

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_32bit(recordPointerReference.getValue(), 8, recordValueData32, errorPointer);
                    if (result < 0) printError("Record Get AccessCount Data", result, errorPointer);
                    accessCount = recordValueData.getValue();

                    /* LDAP Timestamp
                     * The 18-digit Active Directory timestamps, also named 'Windows NT time format' and 'Win32 FILETIME or SYSTEMTIME'.
                     * These are used in Microsoft Active Directory for pwdLastSet, accountExpires, LastLogon, LastLogonTimestamp and LastPwdSet.
                     * The timestamp is the number of 100-nanoseconds intervals (1 nanosecond = one billionth of a second) since Jan 1, 1601 UTC.
                     */
                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_64bit(recordPointerReference.getValue(), 10, recordValueData, errorPointer);
                    if (result < 0) printError("Record Get CreationTime Data", result, errorPointer);
                    creationTime = convertLDAPTimeToString(recordValueData.getValue());

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_64bit(recordPointerReference.getValue(), 12, recordValueData, errorPointer);
                    if (result < 0) printError("Record Get ModifiedTime Data", result, errorPointer);
                    modifiedTime = convertLDAPTimeToString(recordValueData.getValue());

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_64bit(recordPointerReference.getValue(), 13, recordValueData, errorPointer);
                    if (result < 0) printError("Record Get AccessedTime Data", result, errorPointer);
                    accessedTime = convertLDAPTimeToString(recordValueData.getValue());

                    /* Large Text */
                    result = EsedbLibrary.INSTANCE.libesedb_record_get_column_type(recordPointerReference.getValue(), 17, columnType, errorPointer);
                    if (result < 0) printError("Record Get Column Type", result, errorPointer);

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_data_flags(recordPointerReference.getValue(), 17, valueDataFlags, errorPointer);
                    if (result < 0) printError("Record Get Value Data Flags", result, errorPointer);

                    if (valueDataFlags.getValue() == 1) {
                        result = EsedbLibrary.INSTANCE.libesedb_record_get_value_utf8_string_size(recordPointerReference.getValue(), 17, recordValueDataSize, errorPointer);
                        if (result < 0) printError("Record Get URL UTF8 String Size", result, errorPointer);

                        if ((recordValueDataSize.getValue() > 0) && (result == 1)) {
                            result = EsedbLibrary.INSTANCE.libesedb_record_get_value_utf8_string(recordPointerReference.getValue(), 17, recordValueDataUrl, recordValueDataSize.getValue(), errorPointer);
                            if (result < 0) printError("Record Get URL UTF8 String", result, errorPointer);
                            url = recordValueDataUrl.getString(0);
                        }
                    }

                    result = EsedbLibrary.INSTANCE.libesedb_record_get_value_utf8_string_size(recordPointerReference.getValue(), 18, recordValueDataSize, errorPointer);
                    if (result < 0) printError("Record Get FileName UTF8 String Size", result, errorPointer);
                    if ((recordValueDataSize.getValue() > 0) && (result == 1)) {
                        result = EsedbLibrary.INSTANCE.libesedb_record_get_value_utf8_string(recordPointerReference.getValue(), 18, recordValueDataFilename, recordValueDataSize.getValue(), errorPointer);
                        if (result < 0) printError("Record Get FileName UTF8 String", result, errorPointer);
                        file = recordValueDataFilename.getString(0);
                    }

                    /* Imprime valores dos campos da tabela */
                    System.out.format("%4s %10s %10s %20s %30s %30s %30s %40s",
                            "EntryId", "FileSize", "AccessCount", "CreationTime", "ModifiedTime", "AccessedTime", "Filename", "Url");
                    System.out.println();
                    System.out.format("%4d %10d %10d %30s %30s %30s %35s %50s",
                                        entryId, fileSize, accessCount, creationTime, modifiedTime, accessedTime, file, url);
                    System.out.println();

                    result = EsedbLibrary.INSTANCE.libesedb_record_free(recordPointerReference, errorPointer);
                    if (result < 0) printError("Record Free", result, errorPointer);
                    contagemFechamentos++;
                }
            }

            result = EsedbLibrary.INSTANCE.libesedb_table_free(tablePointerReference, errorPointer);
            if (result < 0) printError("Table Free", result, errorPointer);
            contagemFechamentos++;

        }



        System.out.println();
        System.out.println("=============================");

        System.out.println("Fechará arquivo" + filename);
        result = EsedbLibrary.INSTANCE.libesedb_file_close(filePointerReference.getValue(), errorPointer);
        if (result < 0) printError("File Close", result, errorPointer);
        System.out.println("Fechou arquivo" + filename);

        System.out.println("Liberará arquivo" + filename);
        result = EsedbLibrary.INSTANCE.libesedb_file_free(filePointerReference, errorPointer);
        if (result < 0) printError("File Free", result, errorPointer);
        contagemFechamentos++;
        System.out.println("Liberou arquivo" + filename);

        System.out.println("=============================");

        System.out.println();
        System.out.println("=============================");
        System.out.println("Aberturas: " + (contagemAbertura));
        System.out.println("Fechamentos: " + contagemFechamentos);
        System.out.println("=============================");
    }
}
