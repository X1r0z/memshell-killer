package memshell.killer.route;

import memshell.killer.core.DumpResult;
import memshell.killer.core.RemoveResult;

import java.util.List;

public interface RouteHandler {
    String type();

    List<DumpResult> dump() throws Exception;

    RemoveResult remove(String className) throws Exception;
}
