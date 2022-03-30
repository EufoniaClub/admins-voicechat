package de.maxhenkel.voicechat.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.voice.common.PingPacket;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import de.maxhenkel.voicechat.voice.server.ClientConnection;
import de.maxhenkel.voicechat.voice.server.Group;
import de.maxhenkel.voicechat.voice.server.PingManager;
import de.maxhenkel.voicechat.voice.server.Server;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandAlias("voicechat|vc")
@CommandPermission("voicechat.command")
public class VoiceChatCommand extends BaseCommand {

    private final String prefix;
    private final String helpCommand;

    public VoiceChatCommand() {
        this.prefix = "§0[§3Voicechat§0] §r";
        this.helpCommand = "help";
    }

    @Default
    @Subcommand("help")
    @CatchUnknown
    public void onHelp(CommandSender sender) {
        sender.sendMessage(this.helpCommand);
    }

    @Subcommand("speaker")
    @CommandCompletion("on|off @nothing @players")
    @Syntax("<on|off> [distance] [playerName]")
    public void onSpeaker(Player player, String onOrOff, @Default("-1") Double distance, @Optional String playerName) {
        if (onOrOff.equalsIgnoreCase("off")) {
            Player target = player;
            if (playerName != null) {
                Player pp = Bukkit.getPlayer(playerName);
                if (pp == null) {
                    player.sendMessage(this.prefix + "§cPlayer not found.");
                } else {
                    target = pp;
                }
            }

            if (Voicechat.VOICE_RESTRICTIONS.isSpeaker(target)) {
                Voicechat.VOICE_RESTRICTIONS.removeSpeaker(player.getUniqueId());
                target.sendMessage(this.prefix + "§aSpeaker mode disabled.");
                if (target != player) {
                    player.sendMessage(this.prefix + "§aSpeaker mode disabled for §e" + target.getName() + "§a.");
                }
            } else {
                if (target == player) {
                    player.sendMessage(this.prefix + "§cSpeaker mode is already disabled.");
                } else {
                    player.sendMessage(this.prefix + "§cSpeaker mode is already disabled for " + target.getName() + ".");
                }
            }
            return;
        }

        if (!onOrOff.equalsIgnoreCase("on")) {
            player.sendMessage(this.prefix + "§cInvalid argument.");
            return;
        }

        Player target = player;
        if (playerName != null) {
            Player pp = Bukkit.getPlayer(playerName);
            if (pp == null) {
                player.sendMessage(this.prefix + "§cPlayer not found.");
            } else {
                target = pp;
            }
        }

        Voicechat.VOICE_RESTRICTIONS.addSpeaker(target.getUniqueId(), distance);

        target.sendMessage(this.prefix + "§aSpeaker mode enabled." + (distance > 0 ? "§7 (Distance: " + distance + ")" : ""));
        if (target != player) {
            player.sendMessage(this.prefix + "§aSpeaker mode enabled for " + target.getName() + (distance > 0 ? "§7 (Distance: " + distance + ")" : ""));
        }
    }

    @Subcommand("mute")
    @CommandCompletion("@players")
    @Syntax("<playerName>")
    public void mute(Player player, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(this.prefix + "§cPlayer not found.");
            return;
        }

        if (Voicechat.VOICE_RESTRICTIONS.isPlayerMuted(target.getUniqueId())) {
            player.sendMessage(this.prefix + "§cPlayer §e" + target.getName() + "§c is already muted.");
            return;
        }

        Voicechat.VOICE_RESTRICTIONS.addMutedPlayer(target.getUniqueId());
        player.sendMessage(this.prefix + "§aMuted §e" + target.getName() + "§a.");
    }

    @Subcommand("unmute")
    @CommandCompletion("@players")
    @Syntax("<playerName>")
    public void unmute(Player player, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(this.prefix + "§cPlayer not found.");
            return;
        }

        if (!Voicechat.VOICE_RESTRICTIONS.isPlayerMuted(target.getUniqueId())) {
            player.sendMessage(this.prefix + "§cPlayer §e" + target.getName() + "§c is already unmuted.");
            return;
        }

        Voicechat.VOICE_RESTRICTIONS.removeMutedPlayer(target.getUniqueId());
        player.sendMessage(this.prefix + "§aUnmuted §e" + target.getName() + "§a.");
    }

    @Subcommand("globalmute")
    @CommandCompletion("on|off")
    @Syntax("<on|off>")
    public void globalmute(Player player, String onOrOff) {
        if (onOrOff.equalsIgnoreCase("off")) {
            if (!Voicechat.VOICE_RESTRICTIONS.isAllMuted()) {
                player.sendMessage(this.prefix + "§cGlobal mute is already disabled.");
                return;
            }

            Voicechat.VOICE_RESTRICTIONS.setAllMuted(false);
            player.sendMessage(this.prefix + "§aGlobal mute disabled.");
            return;
        }

        if (!onOrOff.equalsIgnoreCase("on")) {
            player.sendMessage(this.prefix + "§cInvalid argument.");
            return;
        }

        if (Voicechat.VOICE_RESTRICTIONS.isAllMuted()) {
            player.sendMessage(this.prefix + "§cGlobal mute is already enabled.");
            return;
        }

        Voicechat.VOICE_RESTRICTIONS.setAllMuted(true);
        player.sendMessage(this.prefix + "§aGlobal mute enabled.");
    }

    @Subcommand("whisper")
    @CommandCompletion("@players")
    @Syntax("<playerName>")
    public void whisper(Player player, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(this.prefix + "§cPlayer not found.");
            return;
        }

        if (Voicechat.VOICE_RESTRICTIONS.isWhispering(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(this.prefix + "§cYou're already whispering §e" + target.getName() + "§c.");
            return;
        }

        Voicechat.VOICE_RESTRICTIONS.addWhisperer(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(this.prefix + "§aYou're now whispering §e" + target.getName() + "§a.");
    }

    @Subcommand("unwhisper")
    @CommandCompletion("@players")
    @Syntax("<playerName>")
    public void unwhisper(Player player, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(this.prefix + "§cPlayer not found.");
            return;
        }

        if (!Voicechat.VOICE_RESTRICTIONS.isWhispering(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(this.prefix + "§cYou're already not whispering §e" + target.getName() + "§c.");
            return;
        }

        Voicechat.VOICE_RESTRICTIONS.removeWhisperer(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(this.prefix + "§aYou're no longer whispering §e" + target.getName() + "§a.");
    }

    @Subcommand("list whisper")
    public void whisperlist(Player player) {
        List<Player> players = Voicechat.VOICE_RESTRICTIONS.getPlayerWhispers(player.getUniqueId()).stream().map(Bukkit::getPlayer).toList();

        if (players.isEmpty()) {
            player.sendMessage(this.prefix + "§cYou're not whispering anyone.");
        } else {
            player.sendMessage(this.prefix + "§aYou're whispering: §e" + players.stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
    }

    @Subcommand("list muted")
    public void mutedlist(Player player) {
        List<Player> players = Voicechat.VOICE_RESTRICTIONS.getMutedPlayers().stream().map(Bukkit::getPlayer).toList();

        if (players.isEmpty()) {
            player.sendMessage(this.prefix + "§cThere are no muted players.");
        } else {
            player.sendMessage(this.prefix + "§aMuted players: §e" + players.stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
    }

    @Subcommand("list speakers")
    public void speaklist(Player player) {
        List<Player> players = Voicechat.VOICE_RESTRICTIONS.getSpeakers().stream().map(Bukkit::getPlayer).toList();

        if (players.isEmpty()) {
            player.sendMessage(this.prefix + "§cThere are no speakers.");
        } else {
            player.sendMessage(this.prefix + "§aSpeakers: §e" + players.stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
    }
}