import com.sun.jna.Native;

public final class CLibraryFunctions {

    private CLibrary cLibraryInstance;

    public CLibraryFunctions() {
        cLibraryInstance = (CLibrary)Native.loadLibrary("c", CLibrary.class);
    }

    public void printf(String format, Object... args) {
        cLibraryInstance.printf(format, args);
    }

    public int atoi(String data) {
        return cLibraryInstance.atoi(data);
    }
}
