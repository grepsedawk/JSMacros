package xyz.wagyourtail.jsmacros.client.api.helper.world;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.client.api.helper.TextHelper;
import xyz.wagyourtail.jsmacros.core.helpers.BaseHelper;

/**
 * @author Wagyourtail
 * @since 1.0.2
 */
@SuppressWarnings("unused")
public class PlayerListEntryHelper extends BaseHelper<PlayerInfo> {

    public PlayerListEntryHelper(PlayerInfo p) {
        super(p);
    }

    /**
     * @return
     * @since 1.1.9
     */
    @Nullable
    public String getUUID() {
        return base.getProfile().id().toString();
    }

    /**
     * @return
     * @since 1.0.2
     */
    @Nullable
    public String getName() {
        return base.getProfile().name();
    }

    /**
     * @return
     * @since 1.6.5
     */
    public int getPing() {
        return base.getLatency();
    }

    /**
     * @return null if unknown
     * @since 1.6.5
     */
    @DocletReplaceReturn("Gamemode")
    @Nullable
    public String getGamemode() {
        return base.getGameMode().getName();
    }

    /**
     * @return
     * @since 1.1.9
     */
    public TextHelper getDisplayText() {
        return TextHelper.wrap(base.getTabListDisplayName());
    }

    /**
     * @return
     * @since 1.8.2
     */
    public byte[] getPublicKey() {
        var session = base.getChatSession();
        return session == null ? null : session.profilePublicKey().data().key().getEncoded();
    }

    /**
     * @return {@code true} if the player has a cape enabled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCape() {
        return base.getSkin().cape() != null;
    }

    /**
     * A slim skin is an Alex skin, while the default one is Steve.
     *
     * @return {@code true} if the player has a slim skin, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasSlimModel() {
        return base.getSkin().model().equals(PlayerModelType.SLIM);
    }

    /**
     * @return the identifier of the player's skin texture or {@code null} if it's unknown.
     * @since 1.8.4
     */
    public String getSkinTexture() {
        return base.getSkin().body().toString();
    }

    /**
     * @since 1.9.0
     */
    @Nullable
    public String getSkinUrl() {
        return base.getSkin().body() instanceof ClientAsset.DownloadedTexture d ? d.url() : null;
    }

    /**
     * @return the identifier of the player's cape texture or {@code null} if it's unknown.
     * @since 1.8.4
     */
    @Nullable
    public String getCapeTexture() {
        return base.getSkin().cape() == null ? null : base.getSkin().cape().toString();
    }

    /**
     * @return the identifier of the player's elytra texture or {@code null} if it's unknown.
     * @since 1.8.4
     */
    @Nullable
    public String getElytraTexture() {
        return base.getSkin().elytra() == null ? null : base.getSkin().elytra().toString();
    }

    /**
     * @return the team of the player or {@code null} if the player is not in a team.
     * @since 1.8.4
     */
    @Nullable
    public TeamHelper getTeam() {
        return base.getTeam() == null ? null : new TeamHelper(base.getTeam());
    }

    @Override
    public String toString() {
        return String.format("PlayerListEntryHelper:{\"uuid\": \"%s\", \"name\": \"%s\"}", this.getUUID(), this.getName());
    }

}
