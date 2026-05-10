package memshell.killer.core;

import java.util.ArrayList;
import java.util.List;

public class CommandResponse {
    public boolean success;
    public Object data;
    public List<String> errors = new ArrayList<>();

    public static CommandResponse ok(Object data) {
        CommandResponse response = new CommandResponse();
        response.success = true;
        response.data = data;
        return response;
    }

    public static CommandResponse error(String error) {
        CommandResponse response = new CommandResponse();
        response.success = false;
        response.errors.add(error);
        return response;
    }
}
