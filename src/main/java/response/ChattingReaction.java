package org.github.Dutap01.bot.response;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.github.Dutap01.bot.music.PlayerManager;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;
import java.util.List;
import java.util.ArrayList;

public class ChattingReaction extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String messageContent = event.getMessage().getContentRaw();

        if (messageContent.startsWith("!ping")) {
            event.getChannel().sendMessage("Pong!").queue();
        } else if (messageContent.startsWith("!join")) {
            joinVoiceChannel(event);
        } else if (messageContent.startsWith("!play ")) {
            String trackUrl = messageContent.substring("!play ".length()).trim();
            playMusic(event, trackUrl);
        } else if (messageContent.equals("!pause")) {
            pauseMusic(event);
        } else if (messageContent.equals("!skip")) {
            skipMusic(event);
        } else if (messageContent.equals("!list")) {
            displayQueue(event);
        } else if (messageContent.equals("!nowplaying")) {
            displayNowPlaying(event);
        } else if (messageContent.startsWith("!promote ")) {
            if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel() || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
                event.getChannel().sendMessage("음악을 승격하려면 먼저 보이스 채널에 접속하고 봇이 연결되어 있어야 합니다.").queue();
                return;
            }
            String[] parts = messageContent.split(" ");
            if (parts.length < 2) {
                event.getChannel().sendMessage("`!promote [곡 번호]` 형식으로 입력해주세요.").queue();
                return;
            }
            try {
                int trackIndex = Integer.parseInt(parts[1]);
                PlayerManager playerManager = PlayerManager.getINSTANCE();
                if (playerManager.promoteTrack(event.getGuild(), trackIndex)) {
                    event.getChannel().sendMessage(trackIndex + "번 곡을 다음 곡으로 끌어올렸습니다!").queue();
                    displayNowPlaying(event);
                } else {
                    event.getChannel().sendMessage("유효하지 않은 곡 번호이거나 큐가 비어 있습니다.").queue();
                }
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("곡 번호는 숫자로 입력해주세요.").queue();
            }
        }
    }

    private void joinVoiceChannel(MessageReceivedEvent event) {
        if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("보이스 채널 입장 오류");
            builder.setDescription("먼저 보이스 채널에 접속해야 합니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }

        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isConnected()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("보이스 채널 입장 오류");
            builder.setDescription("봇이 이미 다른 보이스 채널에 연결되어 있습니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }

        AudioChannel userChannel = event.getMember().getVoiceState().getChannel();
        if (userChannel instanceof VoiceChannel) {
            audioManager.openAudioConnection((VoiceChannel) userChannel);
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.GREEN);
            builder.setTitle("보이스 채널 입장 성공");
            builder.setDescription("`" + userChannel.getName() + "` 채널에 연결되었습니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("보이스 채널 입장 오류");
            builder.setDescription("유효한 보이스 채널이 아닙니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
        }
    }

    private void playMusic(MessageReceivedEvent event, String trackUrl) {
        if (event.getMember() == null || event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("음악 재생 오류");
            builder.setDescription("음악을 재생하려면 보이스 채널에 소속되어 있어야 합니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }
        if (event.getGuild().getSelfMember() == null || event.getGuild().getSelfMember().getVoiceState() == null || !event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            final AudioManager audioManager = event.getGuild().getAudioManager();
            final AudioChannel memberChannel = event.getMember().getVoiceState().getChannel();

            if (memberChannel instanceof VoiceChannel) {
                audioManager.openAudioConnection((VoiceChannel) memberChannel);
            } else {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.RED);
                builder.setTitle("음악 재생 오류");
                builder.setDescription("연결할 수 있는 유효한 보이스 채널이 아닙니다. (예: 스테이지 채널)");
                event.getChannel().sendMessageEmbeds(builder.build()).queue();
                return;
            }
        }
        PlayerManager.getINSTANCE().loadAndPlay(event.getChannel(), trackUrl, event.getMember());
    }

    private void pauseMusic(MessageReceivedEvent event) {
        if (event.getGuild() == null) return;
        if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("일시정지 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }

        PlayerManager.getINSTANCE().pauseTrack(event.getGuild());
        displayNowPlaying(event);
    }

    private void skipMusic(MessageReceivedEvent event) {
        if (event.getGuild() == null) return;
        if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("스킵 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }
        PlayerManager.getINSTANCE().skipTrack(event.getGuild());
        displayNowPlaying(event);
    }

    private void displayNowPlaying(MessageReceivedEvent event) {
        if (event.getGuild() == null) return;
        if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("정보 표시 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }

        PlayerManager playerManager = PlayerManager.getINSTANCE();
        EmbedBuilder embed = playerManager.createNowPlayingEmbed(event.getGuild());

        String buttonLabel = playerManager.isPaused(event.getGuild()) ? "▶️" : "⏸️";

        event.getChannel().sendMessageEmbeds(embed.build())
                .addActionRow(
                        Button.secondary("music:previous", "⏮️"),
                        Button.primary("music:togglepause", buttonLabel),
                        Button.secondary("music:skip", "⏭️"),
                        Button.secondary("music:showqueue", "📜 재생 목록")
                )
                .queue();
    }

    private void displayQueue(MessageReceivedEvent event) {
        if (event.getGuild() == null) return;
        if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle("재생 목록 오류");
            builder.setDescription("봇이 보이스 채널에 연결되어 있지 않습니다.");
            event.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }
        PlayerManager.getINSTANCE().sendQueueEmbed(event.getChannel(), event.getGuild(), 1);
    }
}