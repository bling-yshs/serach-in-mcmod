package com.yshs.searchonmcmod;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.yshs.searchonmcmod.KeyBindings.SEARCH_ON_MCMOD_KEY;

@Mod(SearchOnMcmod.MOD_ID)
public class SearchOnMcmod {
    public static final String MOD_ID = "searchonmcmod";
    private static final Logger log = LogManager.getLogger();
    private static boolean keyDown = false;

    public SearchOnMcmod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    @SneakyThrows
    public void onRenderTooltipEvent(ItemTooltipEvent event) {
        if (keyDown == false) {
            return;
        }
        keyDown = false;
        // 1. 得到物品的描述ID
        String descriptionId = event.getItemStack().getItem().getDescriptionId();
        if (StringUtils.isBlank(descriptionId)) {
            return;
        }
        // 2. 转换为注册表名
        String registryName = SearchOnMcmod.convertDescriptionIdToRegistryName(descriptionId);
        // 3. 如果注册表名为空气，则不进行搜索
        if ("minecraft:air".equals(registryName)) {
            return;
        }
        // 4. 如果注册表明为空，但是物品的描述ID不为空，则进行搜索
        if (StringUtils.isBlank(registryName) && StringUtils.isNotBlank(descriptionId)) {
            MainUtil.openSearchPage(descriptionId);
            return;
        }
        // 5. 查找并得到物品在MCMOD中的ID
        String urlStr = String.format("https://api.mcmod.cn/getItem/?regname=%s", registryName);
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String mcmodItemID = in.readLine();
        in.close();
        connection.disconnect();
        log.info("mcmodItemID: {}", mcmodItemID);

        // 5. 如果mcmodItemID为0，则进行搜索
        if ("0".equals(mcmodItemID)) {
            // 得到物品的本地化名称
            String localizedName = event.getItemStack().getHoverName().getString();
            // 然后到https://search.mcmod.cn/s?key=%s去搜索
            MainUtil.openSearchPage(localizedName);
            return;
        }

        // 6. 打开MCMOD的物品页面
        String mcmodPageUrl = String.format("https://www.mcmod.cn/item/%s.html", mcmodItemID);
        log.info("mcmodPageUrl: {}", mcmodPageUrl);
        Util.getPlatform().openUri(mcmodPageUrl);

    }

    @SubscribeEvent
    public void onKeyPressed(GuiScreenEvent.KeyboardKeyPressedEvent.Post event) {
        int keyCode = event.getKeyCode();
        InputMappings.Input key = SEARCH_ON_MCMOD_KEY.getKey();
        if (keyCode == key.getValue() && keyDown == false) {
            keyDown = true;
            log.info("按键已按下，keyDown设置为true");
        }
    }

    @SubscribeEvent
    public void onKeyReleased(GuiScreenEvent.KeyboardKeyReleasedEvent.Post event) {
        int keyCode = event.getKeyCode();
        if (keyCode == SEARCH_ON_MCMOD_KEY.getKey().getValue()) {
            keyDown = false;
            log.info("按键已释放，keyDown设置为false");
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void setup(final FMLCommonSetupEvent event) {
            ClientRegistry.registerKeyBinding(SEARCH_ON_MCMOD_KEY);
        }

    }

    public static String convertDescriptionIdToRegistryName(@NonNull String descriptionId) {
        // 将输入字符串按"."分割
        String[] parts = descriptionId.split("\\.");

        // 返回格式化后的字符串
        if (parts.length >= 2) {
            return parts[1] + ":" + parts[2];
        } else {
            // 如果格式不符合预期，返回空字符串
            return "";
        }
    }
}
