package me.ujun;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;

public class Main extends JavaPlugin implements Listener {

    public Integer opLevel = 0;
    public Map<String, Integer> perPlayerLevel = new HashMap<String, Integer>();
    public Map<Integer, List<String>> disabledCommandCache = new HashMap<>();

    @Override
    public void onEnable() {
        // save default config if not saved yet
        getConfig().options().copyDefaults(true);
        saveConfig();

        // get current global OP permission level setting
        File f = new File("server.properties");
        opLevel = Integer.parseInt(getString("op-permission-level", f));

        // get per-player OP levels
        reloadOpsCache();
        loadCommandRestrictions();

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new Detect(this), this);

        Command cmd = new Command(this);
        getCommand("setoplevel").setExecutor(cmd);
        getCommand("reload-bukkitopfix").setExecutor(cmd);
        getCommand("reload-bukkitopfix").setTabCompleter(new CommandTabCompleter());
    }

    public void updateOpsJson() {
        try {
            String opsFileContent = readFile("ops.json", StandardCharsets.UTF_8);

            Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Map<String, Object>> ops = gson.fromJson(opsFileContent, listType);
            if (ops == null) ops = new ArrayList<>();

            Map<String, Map<String, Object>> byUuid = new HashMap<>();
            for (Map<String, Object> entry : ops) {
                Object u = entry.get("uuid");
                if (u instanceof String uuid && !uuid.isEmpty()) {
                    byUuid.put(uuid, entry);
                }
            }

            // perPlayerLevel과 비교하여 level만 갱신
            boolean changed = false;
            for (Map.Entry<String, Integer> e : perPlayerLevel.entrySet()) {
                String uuid = e.getKey();
                int desiredLevel = e.getValue();

                Map<String, Object> entry = byUuid.get(uuid);
                if (entry != null) {
                    int currentLevel = asInt(entry.get("level"));
                    if (currentLevel != desiredLevel) {
                        entry.put("level", desiredLevel);
                        changed = true;
                    }
                }
            }

            if (changed) {
                String pretty = gson.toJson(ops);
                Files.writeString(Path.of("ops.json"), pretty, StandardCharsets.UTF_8);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.updateCommands();
                }
            }

        } catch (IOException ex) {
            Bukkit.getLogger().warning("[BukkitOpFix] Failed to update ops.json.");
            ex.printStackTrace();
        }
    }

    private static int asInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (Exception ignored) {}
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public void reloadOpsCache() {
        String opsFileContent;
        perPlayerLevel = new HashMap<String, Integer>();
        try {
            opsFileContent = readFile("ops.json", StandardCharsets.UTF_8);
            List<Object> ops = new Gson().fromJson(opsFileContent, ArrayList.class);

            for (Object pair : ops) {
                Map<String, Object> values = (Map<String, Object>) pair;

                perPlayerLevel.put((String) values.get("uuid"), ((Double) values.get("level")).intValue());
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }

        } catch (IOException e1) {
            Bukkit.getLogger().warning("[BukkitOpFix] Failed to load data from ops.json file.");
            e1.printStackTrace();
        }
    }

    private String readFile(String path, Charset encoding) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static String getString(String s, File f)
    {
        Properties pr = new Properties();
        try
        {
            FileInputStream in = new FileInputStream(f);
            pr.load(in);
            String string = pr.getProperty(s);
            return string;
        }
        catch (IOException e)
        {

        }
        return "";
    }

    public void loadCommandRestrictions() {
        for (int i = 1; i <= 4; i++) {
            List<String> list = getConfig().getStringList("level" + i + "disabledCommands");
            disabledCommandCache.put(i, list);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }
    }
}