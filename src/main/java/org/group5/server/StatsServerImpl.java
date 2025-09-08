package org.group5.server;

import org.group5.api.StatsResult;
import org.group5.api.StatsServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;

public class StatsServerImpl implements StatsServer {

    private final String datasetResource; // e.g., "/dataset/exercise_1_dataset.csv"

    public StatsServerImpl(String datasetResource) {
        this.datasetResource = datasetResource;
    }

    @Override
    public StatsResult getPopulationofCountry(String countryName) throws RemoteException {
        long start = System.nanoTime();
        long sum = 0;

        // NAIVE implementation per spec: read & parse the CSV for every request
        try (InputStream in = getClass().getResourceAsStream(datasetResource);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line; boolean skipHeader = true;
            while ((line = br.readLine()) != null) {
                if (skipHeader) { skipHeader = false; continue; }
                // simple CSV split (dataset values are plain; adjust if quoted)
                // Columns (per spec sample): id,name,country_code,country_name_en,population,timezone,coords
                String[] t = line.split(";", -1); // adjust delimiter if your CSV uses ';' or ',' accordingly
                if (t.length < 5) continue;
                String country = t[3].trim();         // "Country name EN"
                String popStr  = t[4].trim();         // "Population"
                if (country.equalsIgnoreCase(countryName) && !popStr.isEmpty()) {
                    try { sum += Long.parseLong(popStr); } catch (NumberFormatException ignore) { }
                }
            }
        } catch (Exception e) {
            throw new RemoteException("Failed to read dataset " + datasetResource, e);
        }

        long execMs = (System.nanoTime() - start) / 1_000_000;
        return new StatsResult(sum, execMs);
    }
}
