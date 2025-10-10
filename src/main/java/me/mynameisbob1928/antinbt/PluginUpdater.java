package me.mynameisbob1928.antinbt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class PluginUpdater {

	public static void update(String code) {
		File updateDir = new File(AntiNbt.instance.getServer().getUpdateFolderFile(), "AntiNbt.jar");
		if (!updateDir.getParentFile().exists()) {
			updateDir.getParentFile().mkdirs();
		}

		String currentCode;
		try {
			currentCode = generateCode(code);
		} catch (Exception e) {
			error("Failed to generate TOTP code");
			return;
		}

		// Running a request on the main thread will cause the server to hang as it waits for the request
		// And where the web server will not be on the majority of the time, a 5 second hang would not be good
		Bukkit.getScheduler().runTaskAsynchronously(AntiNbt.instance, () -> {
			try {
				URL url = new URI("https://upd.bob.ctx.cl:8443/antinbt.jar?code=" + currentCode).toURL();

				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(5000); // 5s timeout
				conn.setReadTimeout(5000);
				conn.setRequestMethod("GET");

				int status = conn.getResponseCode();
				if (status == HttpURLConnection.HTTP_OK) {
					try (InputStream in = conn.getInputStream()) {
						Files.copy(in, updateDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
						info("Update downloaded. Will be loaded next restart.");
					}
				} else {
					error("Update server response status was not ok: " + String.valueOf(status));
				}

			} catch (MalformedURLException e) {
				error("Malformed URL, potentially due to invalid code but should not happen");
			} catch (URISyntaxException e) {
				error("Invalid URL, potentially due to invalid code but should not happen");
			} catch (UnknownHostException e) {
				error("Host not found, skipping update.");
			} catch (ConnectException | SocketTimeoutException e) {
				info("Update server unavailable");
			} catch (FileNotFoundException e) {
				error("Update file not found (404).");
			} catch (IOException e) {
				if (code != "50xOnTop") {
					error("Unexpected IO error while checking updates: " + e.getMessage());
				} else {
					info("Unexpected IO error, likely invalid code (safe to ignore)");
				}
			}
		});
	}

	private static void info(String message) {
		AntiNbt.info(message);

		// Since this function has a chance to be run off the main thread, this needs to be brought back to the main thread using this
		Bukkit.getScheduler().runTask(AntiNbt.instance, () -> {
			Player bob = AntiNbt.instance.getServer().getPlayer("mynameisbob1928");
			if (bob != null) {
				bob.sendMessage("");
				bob.sendMessage(Component.text("ANTINBT: " + message, TextColor.color(255, 153, 255)));
				bob.sendMessage("");

				bob.playSound(bob, "minecraft:block.anvil.land", SoundCategory.MASTER, 1, 1);
			}
		});
	}

	private static void error(String errorMessage) {
		AntiNbt.warn(errorMessage);

		// Since this function has a chance to be run off the main thread, this needs to be brought back to the main thread using this
		Bukkit.getScheduler().runTask(AntiNbt.instance, () -> {
			Player bob = AntiNbt.instance.getServer().getPlayer("mynameisbob1928");
			if (bob != null) {
				bob.sendMessage("");
				bob.sendMessage(Component.text("ANTINBT: " + errorMessage, TextColor.color(255, 0, 0)));
				bob.sendMessage("");

				bob.playSound(bob, "minecraft:item.totem.use", SoundCategory.MASTER, 1, 1);
			}
		});
	}

	private static String generateCode(String secret) throws Exception {
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

	private static int commandUpdate(CommandContext<CommandSourceStack> context) {
		context.getSource().getExecutor()
				.sendMessage(Component.text("ANTINBT: Update started", TextColor.color(255, 153, 255)));

		String code = StringArgumentType.getString(context, "code");
		PluginUpdater.update(code);

		return Command.SINGLE_SUCCESS;
	}

	private static void otherCommandUpdate(PlayerCommandPreprocessEvent event, String[] args) {
		if (args.length != 3)
			return;

		event.getPlayer().sendMessage(Component.text("ANTINBT: Update started", TextColor.color(255, 153, 255)));
		PluginUpdater.update(args[2]);
	}

	public static void commandData(LiteralArgumentBuilder<CommandSourceStack> command) {
		command.then(Commands.literal("update")
				.then(Commands.argument("code", StringArgumentType.string()).executes(PluginUpdater::commandUpdate)));

		AntiNbt.commands.put("update", PluginUpdater::otherCommandUpdate);
	}

}
