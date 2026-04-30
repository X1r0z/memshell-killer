package memshell.killer.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Jsons {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private Jsons() {
    }
}
