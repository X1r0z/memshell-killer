package memshell.killer.route;

import java.util.List;

public interface RouteHandler {
    String type();

    List<RouteEntry> dump() throws Exception;

    RemoveResult remove(String className) throws Exception;
}
