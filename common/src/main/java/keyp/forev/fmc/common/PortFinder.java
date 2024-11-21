package keyp.forev.fmc.common;

import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class PortFinder {
    public static int foundPort;
    private final Logger logger;
    private final int startPort;
    private final int endPort;
    @Inject
    public PortFinder(Logger logger) {
        this.logger = logger;
        this.startPort = 6000;
        this.endPort = 6999;
    }

    public CompletableFuture<Integer> findAvailablePortAsync(Connection conn) throws SQLException {
        return CompletableFuture.supplyAsync(() -> {
            List<Integer> portRange = IntStream.rangeClosed(startPort, endPort).boxed().collect(Collectors.toList());
            try {
	            List<Integer> usingPorts = getUsingPorts(conn);
	            portRange.removeAll(usingPorts);
	            for (int port : portRange) {
	                try (ServerSocket serverSocket = new ServerSocket(port)) {
	                    serverSocket.close();
	                    return port;
	                } catch (Exception e) {
	                    logger.info("Port {} is already in use, so skip and tried next one", port);
	                	}
	            	}
				} catch (SQLException e) {
					logger.error("An Exception error occurred: {}", e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) {
						logger.error(element.toString());
					}
				}
	          throw new RuntimeException("No available port found in the range " + startPort + " to " + endPort);
        });
    }
    
    private List<Integer> getUsingPorts(Connection conn) throws SQLException {
    	String sql = "SELECT socketport FROM status;";
    	List<Integer> usingPorts = new ArrayList<>();
		try (PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				Object portObj = rs.getObject("socketport");
				if (portObj instanceof Integer) {
                usingPorts.add((Integer) portObj);
            	}
			}
		} catch (Exception e) {
			logger.error("An Exception error occurred: {}", e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		return usingPorts;
    }
}