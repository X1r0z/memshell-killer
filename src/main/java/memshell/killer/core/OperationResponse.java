package memshell.killer.core;

import java.util.ArrayList;
import java.util.List;

public class OperationResponse {
    public boolean success;
    public Object data;
    public List<String> errors = new ArrayList<>();

    public static OperationResponse ok(Object data) {
        OperationResponse response = new OperationResponse();
        response.success = true;
        response.data = data;
        return response;
    }

    public static OperationResponse error(String error) {
        OperationResponse response = new OperationResponse();
        response.success = false;
        response.errors.add(error);
        return response;
    }
}
