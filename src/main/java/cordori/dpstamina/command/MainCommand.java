package cordori.dpstamina.command;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    private static final DPStamina dps = DPStamina.getInstance();
    private List<String> filter(List<String> list, String latest) {
        if (list.isEmpty() || latest == null)
            return list;
        String ll = latest.toLowerCase();
        list.removeIf(k -> !k.toLowerCase().startsWith(ll));
        return list;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String latest = null;
        List<String> list = new ArrayList<>();
        if (args.length != 0) {
            latest = args[args.length - 1];
        }
        if (args.length == 1) {
            list.add("reload");
            list.add("help");
            list.add("group");
            list.add("give");
            list.add("take");
            list.add("set");
        } else if (args.length == 2) {
            String playerName = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(playerName)) {
                    list.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("group")) {
            String groupName = args[2].toLowerCase();
            for (String group : ConfigManager.groupKey) {
                if (group.toLowerCase().startsWith(groupName)) {
                    list.add(group);
                }
            }
        }
        return filter(list, latest);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(dps, () -> {
            if (args.length == 0) {
                sender.sendMessage(ConfigManager.prefix + "§6==============================");
                sender.sendMessage(ConfigManager.prefix + "§b reload - 重载配置配置");
                sender.sendMessage(ConfigManager.prefix + "§b help - 查看指令与描述");
                sender.sendMessage(ConfigManager.prefix + "§b group - 修改玩家体力组");
                sender.sendMessage(ConfigManager.prefix + "§b give - 给予玩家体力值");
                sender.sendMessage(ConfigManager.prefix + "§b take - 扣除玩家体力值");
                sender.sendMessage(ConfigManager.prefix + "§b set - 设置玩家体力值");
                sender.sendMessage(ConfigManager.prefix + "§6==============================");
                return;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                ConfigManager.reloadMyConfig();
                sender.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("reload"));
            }

            else if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ConfigManager.prefix + "§6==============================");
                sender.sendMessage(ConfigManager.prefix + "§b reload - 重载配置配置");
                sender.sendMessage(ConfigManager.prefix + "§b help - 查看指令与描述");
                sender.sendMessage(ConfigManager.prefix + "§b group - 修改玩家体力组");
                sender.sendMessage(ConfigManager.prefix + "§b give - 给予玩家体力值");
                sender.sendMessage(ConfigManager.prefix + "§b take - 扣除玩家体力值");
                sender.sendMessage(ConfigManager.prefix + "§b set - 设置玩家体力值");
                sender.sendMessage(ConfigManager.prefix + "§6==============================");
            }

            else if (args[0].equalsIgnoreCase("group")) {
                if (args.length <= 2) {
                    sender.sendMessage(ConfigManager.prefix + "§c参数不足捏~");
                }
                String playerName = args[1];
                Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    sender.sendMessage(ConfigManager.prefix + "§c该玩家不在线！");
                    return;
                }

                String group = args[2];
                if(!ConfigManager.groupKey.contains(group)) {
                    sender.sendMessage(ConfigManager.prefix + "§c很抱歉，你输入的体力组不正确呢~");
                    return;
                }

                PlayerData.HashMap.get(player).setStaminaGroup(group);

                double currentStamina = PlayerData.HashMap.get(player).getStamina();
                double limitStamina = StaminaGroup.HashMap.get(group).getLimit();
                if(currentStamina > limitStamina) {
                    PlayerData.HashMap.get(player).setStamina(limitStamina);
                }
                sender.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("group")
                        .replaceAll("%player%", playerName)
                        .replaceAll("%group%", group)
                );
            }

            else if (args[0].equalsIgnoreCase("set") ||
                    args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("take")) {

                if (args.length <= 2) {
                    sender.sendMessage(ConfigManager.prefix + "§c参数不足捏~");
                    return;
                }

                String playerName = args[1];
                Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    sender.sendMessage(ConfigManager.prefix + "§c该玩家不在线！");
                    return;
                }

                double number;
                try {
                    number = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ConfigManager.prefix + "§c参数错误，需要输入数字！");
                    return;
                }

                double stamina = PlayerData.HashMap.get(player).getStamina();
                String staminaGroup = PlayerData.HashMap.get(player).getStaminaGroup();
                double limit = StaminaGroup.HashMap.get(staminaGroup).getLimit();

                switch (args[0].toLowerCase()) {
                    case "set":
                        double num = Math.min(number, limit);
                        if(number < 0) num = 0;
                        String message1 = ConfigManager.messagesHashMap.get("set")
                                .replaceAll("%player%", playerName)
                                .replaceAll("%num%", String.valueOf(num)
                                );
                        modifyStamina(sender, player, num, message1);
                        break;

                    case "give":
                        double newStamina = Math.min(stamina + number, limit);
                        double ns;
                        if(stamina + number > limit) {
                            ns = limit - stamina;
                        } else {
                            ns = number;
                        }
                        String message2 = ConfigManager.messagesHashMap.get("give")
                                .replaceAll("%player%", playerName)
                                .replaceAll("%num%", String.valueOf(ns))
                                .replaceAll("%stamina%", String.valueOf(newStamina)
                                );
                        modifyStamina(sender, player, newStamina, message2);
                        break;

                    case "take":
                        double reducedStamina = Math.max(stamina - number, 0);
                        double rs;
                        if(stamina - number < 0) {
                            rs = stamina;
                        } else {
                            rs = number;
                        }
                        String message3 = ConfigManager.messagesHashMap.get("take")
                                .replaceAll("%player%", playerName)
                                .replaceAll("%num%", String.valueOf(rs))
                                .replaceAll("%stamina%", String.valueOf(reducedStamina)
                                );
                        modifyStamina(sender, player, reducedStamina, message3);
                        break;

                    default:
                        sender.sendMessage(ConfigManager.prefix + "§c无效的参数！");
                }
            }

            else {
                sender.sendMessage(ConfigManager.prefix + "§c无效的指令！");
            }
        });
        return false;
    }

    private void modifyStamina(CommandSender sender, Player player, double newStamina, String message) {
        PlayerData.HashMap.get(player).setStamina(newStamina);
        sender.sendMessage(ConfigManager.prefix + message);
    }
}
