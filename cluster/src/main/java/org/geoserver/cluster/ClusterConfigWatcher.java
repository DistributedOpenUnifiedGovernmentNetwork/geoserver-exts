package org.geoserver.cluster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;

public class ClusterConfigWatcher {

    static Logger LOGGER = Logging.getLogger("org.geoserver.cluster");

    ClusterConfig config;
    PropertyFileWatcher watcher;
    
    protected ClusterConfig getNewClusterConfig() {
        return new ClusterConfig();
    }

    public ClusterConfigWatcher(File file) {
        watcher = new PropertyFileWatcher(file) {
            @Override
            protected Properties parseFileContents(InputStream in) throws IOException {
                ClusterConfig config = getNewClusterConfig();
                config.putAll(super.parseFileContents(in));
                return config;
            }
        };
    }

    public ClusterConfig get() {
        if (watcher.isStale()) {
            try {
                config = (ClusterConfig) watcher.read();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading " + watcher.getFile().getPath(), e);
            }
        }
        return config;
    }
}
