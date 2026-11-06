package com.stagesgraphs;

import java.io.*;
import java.util.*;

/** Local rider profiles, with inclusive lower boundaries for zones 2 through 5. */
public final class ZoneProfiles {
    public static final class Profile {
        public final String name;
        private final int[] starts;
        public Profile(String username, int[] boundaries) {
            name = username == null ? "" : username.trim();
            if (name.isEmpty() || name.length() > 32) throw new IllegalArgumentException("Enter a username (1–32 characters)");
            if (boundaries == null || boundaries.length != 4) throw new IllegalArgumentException("Enter all four zone starts");
            starts = boundaries.clone();
            int previous = 0;
            for (int value : starts) {
                if (value <= previous || value > 300) throw new IllegalArgumentException("Zone starts must increase, from 1 to 300");
                previous = value;
            }
        }
        public int boundary(int index) { return starts[index]; }
        public int zone(int heartRate) {
            int zone = 0;
            while (zone < starts.length && heartRate >= starts[zone]) zone++;
            return zone;
        }
    }
    private final LinkedHashMap<String, Profile> profiles = new LinkedHashMap<>();
    private String selected;
    private static String key(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    public List<Profile> all() { return new ArrayList<>(profiles.values()); }
    public Profile find(String name) { return name == null ? null : profiles.get(key(name)); }
    public Profile selected() { return profiles.get(selected); }
    public void save(Profile profile) {
        selected = key(profile.name);
        profiles.put(selected, profile);
    }
    public void select(String name) {
        if (find(name) == null) throw new IllegalArgumentException("Unknown user");
        selected = key(name);
    }
    public String encode() {
        Properties data = new Properties();
        data.setProperty("count", Integer.toString(profiles.size()));
        if (selected != null) data.setProperty("selected", selected);
        int i = 0;
        for (Profile profile : profiles.values()) {
            data.setProperty("name."+i, profile.name);
            data.setProperty("zones."+i, profile.starts[0]+","+profile.starts[1]+","+profile.starts[2]+","+profile.starts[3]);
            i++;
        }
        StringWriter writer = new StringWriter();
        try { data.store(writer, "Stages Graphs profiles"); }
        catch (IOException impossible) { throw new IllegalStateException(impossible); }
        return writer.toString();
    }
    public static ZoneProfiles decode(String encoded) {
        ZoneProfiles result = new ZoneProfiles();
        if (encoded == null || encoded.isEmpty()) return result;
        Properties data = new Properties();
        try {
            data.load(new StringReader(encoded));
            int count = Integer.parseInt(data.getProperty("count", "0"));
            if (count < 0 || count > 1000) return result;
            for (int i=0; i<count; i++) {
                try {
                    String[] fields = data.getProperty("zones."+i, "").split(",");
                    if (fields.length != 4) continue;
                    int[] starts = new int[4];
                    for (int j=0; j<4; j++) starts[j] = Integer.parseInt(fields[j]);
                    result.save(new Profile(data.getProperty("name."+i), starts));
                } catch (IllegalArgumentException invalidProfile) { /* Retain other valid profiles. */ }
            }
            String selected = data.getProperty("selected");
            if (result.find(selected) != null) result.select(selected);
        } catch (IOException | IllegalArgumentException invalidData) { return new ZoneProfiles(); }
        return result;
    }
}
