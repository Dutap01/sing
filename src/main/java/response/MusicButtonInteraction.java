package org.github.Dutap01.bot.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.github.Dutap01.bot.music.PlayerManager;

public class MusicButtonInteraction extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (event.getGuild() == null) {
            event.deferReply(true).setContent("이 명령어는 서버(길드)에서만 사용할 수 있습니다.").queue();
            return;
        }

        PlayerManager playerManager = PlayerManager.getINSTANCE();
        event.deferReply(true).queue(hook -> {
            String replyMessage = "";
            boolean updateNowPlayingMessage = true;

            if (componentId.startsWith("music:promote:")) {
                String[] parts = componentId.split(":");
                if (parts.length == 3) {
                    try {
                        int trackIndex = Integer.parseInt(parts[2]);
                        if (playerManager.promoteTrack(event.getGuild(), trackIndex)) {
                            replyMessage = trackIndex + "번 곡을 다음 곡으로 끌어올렸습니다!";
                            playerManager.sendQueueEmbed(event.getChannel(), event.getGuild(), 1);
                            updateNowPlayingMessage = false;
                        } else {
                            replyMessage = "유효하지 않은 곡 번호이거나 큐가 비어 있습니다.";
                        }
                    } catch (NumberFormatException e) {
                        replyMessage = "잘못된 곡 번호입니다.";
                    }
                } else {
                    replyMessage = "알 수 없는 버튼 상호작용입니다.";
                }
            } else if (componentId.startsWith("music:queue:next_page:") || componentId.startsWith("music:queue:prev_page:")) {
                String[] parts = componentId.split(":");
                if (parts.length == 4) {
                    try {
                        int targetPage = Integer.parseInt(parts[3]);
                        playerManager.sendQueueEmbed(event.getChannel(), event.getGuild(), targetPage);
                        replyMessage = targetPage + "페이지로 이동합니다.";
                        updateNowPlayingMessage = false;
                    } catch (NumberFormatException e) {
                        replyMessage = "잘못된 페이지 번호입니다.";
                    }
                } else {
                    replyMessage = "알 수 없는 페이지 버튼입니다.";
                }
            }
            else {
                switch (componentId) {
                    case "music:togglepause":
                        playerManager.pauseTrack(event.getGuild());
                        if (playerManager.isPaused(event.getGuild())) {
                            replyMessage = "음악을 일시정지했습니다.";
                        } else {
                            replyMessage = "음악 재생을 재개했습니다.";
                        }
                        break;

                    case "music:skip":
                        playerManager.skipTrack(event.getGuild());
                        replyMessage = "다음 곡으로 스킵했습니다.";
                        break;

                    case "music:previous":
                        replyMessage = "이전 곡 기능은 아직 구현되지 않았습니다.";
                        break;

                    case "music:showqueue":
                        playerManager.sendQueueEmbed(event.getChannel(), event.getGuild(), 1);
                        replyMessage = "재생 목록을 표시합니다.";
                        updateNowPlayingMessage = false;
                        break;

                    default:
                        replyMessage = "알 수 없는 버튼 상호작용입니다.";
                        break;
                }
            }
            hook.editOriginal(replyMessage).queue();
            if (updateNowPlayingMessage) {
                EmbedBuilder updatedEmbed = playerManager.createNowPlayingEmbed(event.getGuild());
                String newButtonLabel = playerManager.isPaused(event.getGuild()) ? "▶️" : "⏸️";

                event.getMessage().editMessageEmbeds(updatedEmbed.build())
                        .setActionRow(
                                Button.secondary("music:previous", "⏮️"),
                                Button.primary("music:togglepause", newButtonLabel),
                                Button.secondary("music:skip", "⏭️"),
                                Button.secondary("music:showqueue", "📜 재생 목록")
                        ).queue();
            }
        });
    }
}